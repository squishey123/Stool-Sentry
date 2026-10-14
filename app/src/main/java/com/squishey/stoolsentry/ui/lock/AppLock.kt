package com.squishey.stoolsentry.ui.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide lock state. The UI shows the lock screen while [locked] is true and the
 * user has turned app lock on. Leaving the app locks it again.
 */
class AppLock {
    private val lockedState = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = lockedState.asStateFlow()

    private var skipNextStop = false

    fun unlock() {
        lockedState.value = false
    }

    /** Call before opening a system picker so returning from it does not lock the app. */
    fun allowExternalActivity() {
        skipNextStop = true
    }

    fun onResume() {
        skipNextStop = false
    }

    fun onStop(changingConfigurations: Boolean) {
        if (changingConfigurations) return
        if (skipNextStop) {
            skipNextStop = false
            return
        }
        lockedState.value = true
    }
}

object Biometrics {
    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String?,
        onSuccess: () -> Unit,
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { if (subtitle != null) setSubtitle(subtitle) }
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }
}
