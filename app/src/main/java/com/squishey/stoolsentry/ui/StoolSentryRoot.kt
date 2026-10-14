package com.squishey.stoolsentry.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.squishey.stoolsentry.AppContainer
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.domain.UserSettings
import com.squishey.stoolsentry.ui.components.throneBackground
import com.squishey.stoolsentry.ui.lock.Biometrics
import com.squishey.stoolsentry.ui.lock.LockScreen
import com.squishey.stoolsentry.ui.nav.AppNavHost
import com.squishey.stoolsentry.ui.theme.StoolSentryTheme
import com.squishey.stoolsentry.ui.theme.Throne
import com.squishey.stoolsentry.ui.welcome.WelcomeScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun StoolSentryRoot(container: AppContainer) {
    val settingsFlow = remember(container) { container.settings.settings.map<UserSettings, UserSettings?> { it } }
    val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
    val locked by container.appLock.locked.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current as? FragmentActivity

    val showMessage: (String) -> Unit = remember(snackbar, scope) {
        { message ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(message)
            }
        }
    }

    StoolSentryTheme {
        Box(
            Modifier
                .fillMaxSize()
                .throneBackground(),
        ) {
            val current = settings
            when {
                current == null -> Unit
                !current.disclaimerAccepted -> WelcomeScreen(
                    onAccept = { scope.launch { container.settings.setDisclaimerAccepted(true) } },
                )
                else -> {
                    val showLock = current.appLockEnabled && locked
                    Box(if (showLock) Modifier.clearAndSetSemantics { } else Modifier) {
                        AppNavHost(container = container, showMessage = showMessage)
                    }
                    AnimatedVisibility(
                        visible = showLock,
                        enter = fadeIn(tween(120)),
                        exit = fadeOut(tween(320)),
                    ) {
                        val unlockTitle = stringResource(R.string.lock_prompt_title)
                        val unlockSubtitle = stringResource(R.string.lock_prompt_subtitle)
                        val prompt = {
                            if (activity != null) {
                                Biometrics.authenticate(activity, unlockTitle, unlockSubtitle) {
                                    container.appLock.unlock()
                                }
                            }
                        }
                        var autoPrompted by remember { mutableStateOf(false) }
                        LifecycleResumeEffect(Unit) {
                            if (!autoPrompted) {
                                autoPrompted = true
                                prompt()
                            }
                            onPauseOrDispose { }
                        }
                        BackHandler { activity?.moveTaskToBack(true) }
                        LockScreen(onUnlock = prompt)
                    }
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 88.dp),
            ) { data -> ThroneSnackbar(data) }
        }
    }
}

@Composable
private fun ThroneSnackbar(data: SnackbarData) {
    Surface(
        shape = CircleShape,
        color = Throne.CardHighest,
        contentColor = Throne.Porcelain,
        shadowElevation = 10.dp,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 480.dp),
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Throne.Gold,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(data.visuals.message, style = MaterialTheme.typography.labelLarge)
        }
    }
}
