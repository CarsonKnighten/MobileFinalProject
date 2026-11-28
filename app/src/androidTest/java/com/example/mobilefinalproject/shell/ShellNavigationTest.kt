package com.example.mobilefinalproject.shell

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mobilefinalproject.MainActivity
import com.example.mobilefinalproject.R
import org.hamcrest.Matcher
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ShellNavigationTest {
    @Test
    fun launcherShowsEmailAndPasswordBeforeDashboard() {
        AppSession.signOut()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launch = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        ActivityScenario.launch<LoginActivity>(launch).use {
            onView(withId(R.id.open_strength_training)).check(doesNotExist())
            onView(withId(R.id.login_email)).check(matches(isDisplayed()))
            onView(withId(R.id.login_password)).check(matches(isDisplayed()))
            onView(withId(R.id.switch_auth_mode)).check(matches(withText("Create account")))
            assertNull(AppSession.account)
        }
    }

    @Test
    fun registersSignsOutAndSignsInWithEmailAndPassword() {
        AppSession.signOut()
        val email = "test-${UUID.randomUUID()}@example.com"
        val password = "Test workout password!"
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.switch_auth_mode)).perform(click())
            fill(R.id.login_email, email)
            fill(R.id.login_password, password)
            fill(R.id.confirm_password, password)
            onView(withId(R.id.sign_in_button)).perform(click())
            awaitDisplayed(withText("Signed in as $email"))
            onView(withId(R.id.sign_out_button)).perform(click())

            awaitDisplayed(withId(R.id.login_email))
            fill(R.id.login_email, email)
            fill(R.id.login_password, "A wrong password")
            onView(withId(R.id.sign_in_button)).perform(click())
            awaitDisplayed(withText("The email or password is incorrect."))
            assertNull(AppSession.account)

            fill(R.id.login_password, password)
            onView(withId(R.id.sign_in_button)).perform(click())
            awaitDisplayed(withText("Signed in as $email"))
            onView(withId(R.id.sign_out_button)).perform(click())
            awaitDisplayed(withId(R.id.login_email))
        }
    }

    @Test
    fun dashboardOpensBothFeaturesAndEachCanReturn() {
        AppSession.signIn(testAccount)
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.open_strength_training)).perform(click())
            onView(withId(R.id.strength_content)).check(matches(isDisplayed()))
            pressBack()
            onView(withId(R.id.open_calorie_tracking)).perform(click())
            onView(withId(R.id.button_save_weight_log)).check(matches(isDisplayed()))
            onView(withContentDescription("Navigate up")).perform(click())
            onView(withId(R.id.open_strength_training)).check(matches(isDisplayed()))
            it.recreate()
            onView(withId(R.id.open_calorie_tracking)).check(matches(isDisplayed()))
        }
        assertNull(AppSession.account)
    }

    private fun fill(id: Int, value: String) {
        onView(withId(id)).perform(replaceText(value), closeSoftKeyboard())
    }

    private fun awaitDisplayed(matcher: Matcher<android.view.View>) {
        val deadline = SystemClock.elapsedRealtime() + 7000
        while (true) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (error: NoMatchingViewException) {
                if (SystemClock.elapsedRealtime() >= deadline) throw error
                SystemClock.sleep(75)
            }
        }
    }
}
