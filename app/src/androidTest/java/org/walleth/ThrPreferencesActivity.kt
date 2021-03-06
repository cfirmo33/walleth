package org.walleth

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.PreferenceActivity

class ThrPreferencesActivity {

    @get:Rule
    var rule = TruleskActivityRule(PreferenceActivity::class.java)

    @Test
    fun preferencesShow() {
        onView(withText(R.string.day_or_night_summary)).check(matches(isDisplayed()))

        rule.screenShot("preferences")
    }

}
