package com.squishey.stoolsentry

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.squishey.stoolsentry.ui.StoolSentryRoot
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** FragmentActivity because BiometricPrompt needs one for the app lock. */
class MainActivity : FragmentActivity() {
    private val container: AppContainer
        get() = (application as StoolSentryApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.CREATED) {
                    container.settings.settings
                        .map { it.appLockEnabled }
                        .distinctUntilChanged()
                        .collect { lockEnabled -> setRecentsScreenshotEnabled(!lockEnabled) }
                }
            }
        }

        setContent {
            StoolSentryRoot(container)
        }
    }

    override fun onResume() {
        super.onResume()
        container.appLock.onResume()
    }

    override fun onStop() {
        super.onStop()
        container.appLock.onStop(isChangingConfigurations)
    }
}
