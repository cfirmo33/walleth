package org.walleth

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.espresso.matcher.ViewMatchers.Visibility.*
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.MainActivity
import org.walleth.data.ETH_IN_WEI
import java.math.BigInteger

class TheMainActivity {

    @get:Rule
    var rule = TruleskActivityRule(MainActivity::class.java)

    @Test
    fun behavesCorrectlyWhenBalanceIsZero() {
        TestApp.balanceProvider.setBalance(TestApp.keyStore.getCurrentAddress(),42,BigInteger("0"))

        onView(allOf(isDescendantOfA(withId(R.id.value_view)),withId(R.id.current_eth)))
                .check(matches(withText("0")))

        onView(withId(R.id.send_container)).check(matches(withEffectiveVisibility(INVISIBLE)))
        onView(withId(R.id.empty_view)).check(matches(withEffectiveVisibility(VISIBLE)))

        onView(withId(R.id.transaction_recycler_in)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.transaction_recycler_out)).check(matches(withEffectiveVisibility(GONE)))

        onView(withId(R.id.fab)).check(matches(withEffectiveVisibility(GONE)))

        rule.screenShot("balance_zero")
    }

    @Test
    fun behavesCorrectlyWhenBalanceIsOne() {
        TestApp.balanceProvider.setBalance(TestApp.keyStore.getCurrentAddress(),42, ETH_IN_WEI)

        onView(allOf(isDescendantOfA(withId(R.id.value_view)),withId(R.id.current_eth)))
                .check(matches(withText("1")))

        onView(withId(R.id.send_container)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.empty_view)).check(matches(withEffectiveVisibility(GONE)))

        onView(withId(R.id.transaction_recycler_in)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.transaction_recycler_out)).check(matches(withEffectiveVisibility(VISIBLE)))

        onView(withId(R.id.fab)).check(matches(withEffectiveVisibility(VISIBLE)))

        rule.screenShot("balance_one")
    }

}
