package org.walleth

import org.walleth.data.SimpleObserveable
import org.walleth.data.WallethAddress
import org.walleth.data.keystore.WallethKeyStore

class TestKeyStore : SimpleObserveable(), WallethKeyStore {

    private var currentAddressVar = WallethAddress("0xfdf1210fc262c73d0436236a0e07be419babbbc4")

    override fun setCurrentAddress(address: WallethAddress) {
        currentAddressVar = address
    }

    override fun getCurrentAddress() = currentAddressVar
    override fun importKey(json: String, importPassword: String, newPassword: String) = WallethAddress("OxABCD43")
    override fun exportCurrentKey(unlockPassword: String, exportPassword: String) = "export_key" + unlockPassword + exportPassword
}