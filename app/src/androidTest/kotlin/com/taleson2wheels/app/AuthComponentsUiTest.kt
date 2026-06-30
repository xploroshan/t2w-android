package com.taleson2wheels.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.taleson2wheels.app.ui.auth.AuthErrorText
import com.taleson2wheels.app.ui.auth.AuthPrimaryButton
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented Compose UI tests for the shared auth components. Runs on a device
 * or emulator (`connectedDebugAndroidTest`); not part of the JVM unit-test CI job.
 */
class AuthComponentsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun errorText_isShown_whenMessagePresent() {
        composeRule.setContent {
            MaterialTheme { AuthErrorText(message = "Invalid credentials") }
        }
        composeRule.onNodeWithText("Invalid credentials").assertIsDisplayed()
    }

    @Test
    fun primaryButton_invokesCallback_whenEnabledAndClicked() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                AuthPrimaryButton(text = "Sign in", enabled = true, loading = false, onClick = { clicked = true })
            }
        }
        composeRule.onNodeWithText("Sign in").performClick()
        assertTrue(clicked)
    }

    @Test
    fun primaryButton_doesNotInvoke_whenDisabled() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                AuthPrimaryButton(text = "Sign in", enabled = false, loading = false, onClick = { clicked = true })
            }
        }
        composeRule.onNodeWithText("Sign in").performClick()
        assertFalse(clicked)
    }
}
