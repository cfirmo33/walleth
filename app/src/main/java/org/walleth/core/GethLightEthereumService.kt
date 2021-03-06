package org.walleth.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.SystemClock
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.ethereum.geth.*
import org.walleth.data.BalanceProvider
import org.walleth.data.WallethAddress
import org.walleth.data.config.Settings
import org.walleth.data.keystore.GethBackedWallethKeyStore
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.syncprogress.WallethSyncProgress
import org.walleth.data.transactions.Transaction
import org.walleth.data.transactions.TransactionProvider
import org.walleth.data.transactions.TransactionSource
import org.walleth.ui.ChangeObserver
import java.io.File
import java.math.BigInteger


class GethLightEthereumService : Service() {

    val binder by lazy { Binder() }
    override fun onBind(intent: Intent) = binder

    val ethereumContext = Context()

    val lazyKodein = LazyKodein(appKodein)

    val balanceProvider: BalanceProvider by lazyKodein.instance()
    val transactionProvider: TransactionProvider by lazyKodein.instance()
    val syncProgress: SyncProgressProvider by lazyKodein.instance()
    val keyStore: WallethKeyStore by lazyKodein.instance()
    val settings: Settings by lazyKodein.instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by lazyKodein.instance()
    private val path by lazy { File(baseContext.filesDir, ".ethereum_rb").absolutePath }

    val ethereumNode by lazy {
        Geth.newNode(path, NodeConfig().apply {
            val bootNodes = Enodes()

            val network = networkDefinitionProvider.networkDefinition

            network.bootNodes.forEach {
                bootNodes.append(Enode(it))
            }

            bootstrapNodes = bootNodes
            ethereumGenesis = network.genesis
            ethereumNetworkID = 4
            ethereumNetStats = settings.getStatsName() + ":Respect my authoritah!@stats.rinkeby.io"
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        try {
            ethereumNode.start()
        } catch (e: Exception) {
            // TODO better handling - unfortunately ethereumNode does not have one isStarted method which would come handy here
        }

        ethereumNode.ethereumClient.subscribeNewHead(ethereumContext, object : NewHeadHandler {
            override fun onNewHead(p0: Header) {
                val address = keyStore.getCurrentAddress().toGethAddr()
                val balance = ethereumNode.ethereumClient.getBalanceAt(ethereumContext, address, p0.number)
                balanceProvider.setBalance(WallethAddress(address.hex), p0.number, BigInteger(balance.string()))
            }

            override fun onError(p0: String?) {}

        }, 16)

        transactionProvider.registerChangeObserver(object : ChangeObserver {
            override fun observeChange() {
                transactionProvider.getAllTransactions().forEach {
                    if (it.ref == TransactionSource.WALLETH) {
                        executeTransaction(it)
                    }
                }
            }

        })
        Thread({

            while (true) {

                val ethereumSyncProgress = ethereumNode.ethereumClient.syncProgress(ethereumContext)

                if (ethereumSyncProgress != null) {
                    val newSyncProgress = WallethSyncProgress(true, ethereumSyncProgress.currentBlock, ethereumSyncProgress.highestBlock)
                    syncProgress.setSyncProgress(newSyncProgress)
                } else {
                    syncProgress.setSyncProgress(WallethSyncProgress())
                }

                SystemClock.sleep(1000)
            }
        }).start()

        return START_STICKY
    }


    private fun executeTransaction(transaction: Transaction) {
        transaction.ref = TransactionSource.WALLETH_PROCESSED

        try {
            val client = ethereumNode.ethereumClient
            val nonceAt = client.getNonceAt(ethereumContext, transaction.from.toGethAddr(), -1)

            val gasPrice = client.suggestGasPrice(ethereumContext)

            val gasLimit = BigInt(21_000)

            val newTransaction = Geth.newTransaction(nonceAt, transaction.to.toGethAddr(), BigInt(transaction.value.toLong()), gasLimit, gasPrice, ByteArray(0))

            newTransaction.hashCode()

            val gethKeystore = (keyStore as GethBackedWallethKeyStore).keyStore
            val accounts = gethKeystore.accounts
            val index = (0..(accounts.size() - 1)).first { accounts.get(0).address.hex.toUpperCase() == transaction.from.hex }
            gethKeystore.unlock(accounts.get(index), "default")

            val signHash = gethKeystore.signHash(transaction.from.toGethAddr(), newTransaction.sigHash.bytes)
            val transactionWithSignature = newTransaction.withSignature(signHash)

            transaction.sigHash = newTransaction.sigHash.hex
            transaction.txHash = newTransaction.hash.hex

            client.sendTransaction(ethereumContext, transactionWithSignature)
        } catch (e: Exception) {
            transaction.error = e.message
        }
    }

}
