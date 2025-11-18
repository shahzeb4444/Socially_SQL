package com.teamsx.i230610_i230040

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso Test Case 1: User Login Workflow
 * Critical Flow: User enters credentials and logs into the app
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class UserLoginTest {

    // Your actual login activity name
    @get:Rule
    val activityRule = ActivityScenarioRule(mainlogin::class.java)

    @Test
    fun testCompleteLoginWorkflow() {
        // Wait for activity to load
        Thread.sleep(1000)

        // Step 1: Enter email in username field
        onView(withId(R.id.usernamefield))
            .perform(clearText(), typeText("test@example.com"), closeSoftKeyboard())

        // Step 2: Enter password
        onView(withId(R.id.passwordfield))
            .perform(clearText(), typeText("password123"), closeSoftKeyboard())

        // Step 3: Click login button
        onView(withId(R.id.loginbutton))
            .perform(click())

        // Step 4: Wait for Firebase authentication
        Thread.sleep(4000)

        // Step 5: Verify user reached login_splash screen
        // Replace R.id.splashLayout with any unique view ID from your login_splash activity
        onView(withId(R.id.switchaccounts))
            .check(matches(isDisplayed()))
    }
}