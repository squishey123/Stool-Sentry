package com.squishey.stoolsentry.ui.encrypt

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.data.VaultPhase
import com.squishey.stoolsentry.domain.ExportCrypto
import com.squishey.stoolsentry.ui.components.GoldButton
import com.squishey.stoolsentry.ui.components.ScreenBody
import com.squishey.stoolsentry.ui.components.SectionLabel
import com.squishey.stoolsentry.ui.components.ThroneTopBar
import com.squishey.stoolsentry.ui.theme.Throne

@Composable
fun SecureWindow() {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

@Composable
fun EncryptionOfferScreen(
    onSkip: () -> Unit,
    onEnable: (String) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    val activity = LocalActivity.current
    BackHandler {
        if (creating) creating = false else activity?.moveTaskToBack(true)
    }
    if (creating) {
        PassphraseForm(
            mode = PassphraseMode.Create,
            onBack = { creating = false },
            onSubmit = onEnable,
        )
    } else {
        ScreenBody(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(48.dp))
                SectionLabel(stringResource(R.string.encrypt_offer_eyebrow))
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.encrypt_offer_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = Throne.Porcelain,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.encrypt_offer_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Throne.TextMuted,
                )
                Spacer(Modifier.height(28.dp))
                GoldButton(
                    text = stringResource(R.string.encrypt_offer_enable),
                    onClick = { creating = true },
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.encrypt_offer_skip), color = Throne.Gold)
                }
            }
        }
    }
}

@Composable
fun UnlockScreen(
    failed: Boolean,
    busy: Boolean,
    onSubmit: (String) -> Unit,
) {
    val activity = LocalActivity.current
    BackHandler { activity?.moveTaskToBack(true) }
    PassphraseForm(
        mode = PassphraseMode.Unlock,
        failed = failed,
        busy = busy,
        onSubmit = onSubmit,
    )
}

@Composable
fun WorkingScreen(kind: VaultPhase.Kind) {
    val activity = LocalActivity.current
    BackHandler { activity?.moveTaskToBack(true) }
    val message = when (kind) {
        VaultPhase.Kind.Enable -> R.string.encrypt_working
        VaultPhase.Kind.Disable -> R.string.encrypt_working_off
        VaultPhase.Kind.Change -> R.string.encrypt_working_change
    }
    ScreenBody {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator(color = Throne.Gold)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.titleMedium,
                color = Throne.Porcelain,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

enum class PassphraseMode { Create, Unlock, Change, Disable }

@Composable
fun PassphraseForm(
    mode: PassphraseMode,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    matchesCurrent: (String) -> Boolean = { false },
    failed: Boolean = false,
    busy: Boolean = false,
) {
    SecureWindow()
    var current by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    val shownError = error ?: if (failed) R.string.encrypt_wrong else null
    val title = when (mode) {
        PassphraseMode.Create -> R.string.encrypt_create_title
        PassphraseMode.Unlock -> R.string.encrypt_unlock_title
        PassphraseMode.Change -> R.string.encrypt_change_title
        PassphraseMode.Disable -> R.string.encrypt_disable_title
    }
    val action = when (mode) {
        PassphraseMode.Create -> R.string.encrypt_create_action
        PassphraseMode.Unlock -> R.string.encrypt_unlock_action
        PassphraseMode.Change -> R.string.encrypt_change_action
        PassphraseMode.Disable -> R.string.encrypt_disable_action
    }

    fun submit() {
        when (mode) {
            PassphraseMode.Unlock, PassphraseMode.Disable -> {
                if (ExportCrypto.normalize(first).isEmpty()) {
                    error = R.string.encrypt_blank
                    return
                }
                if (mode == PassphraseMode.Disable && !matchesCurrent(first)) {
                    error = R.string.encrypt_wrong
                    return
                }
                onSubmit(ExportCrypto.normalize(first))
            }
            PassphraseMode.Create, PassphraseMode.Change -> {
                val created = ExportCrypto.normalize(first)
                val confirmed = ExportCrypto.normalize(second)
                if (mode == PassphraseMode.Change && !matchesCurrent(current)) {
                    error = R.string.encrypt_wrong
                    return
                }
                if (created.isEmpty()) {
                    error = R.string.encrypt_blank
                    return
                }
                if (created != confirmed) {
                    error = R.string.encrypt_mismatch
                    return
                }
                onSubmit(created)
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        if (onBack != null || mode != PassphraseMode.Unlock) {
            ThroneTopBar(
                title = stringResource(title),
                onBack = onBack,
            )
        }
        ScreenBody(
            Modifier
                .weight(1f)
                .then(if (mode == PassphraseMode.Unlock) Modifier.statusBarsPadding() else Modifier),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                if (mode == PassphraseMode.Unlock) {
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Throne.Porcelain,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        when (mode) {
                            PassphraseMode.Create -> R.string.encrypt_create_body
                            PassphraseMode.Unlock -> R.string.encrypt_unlock_body
                            PassphraseMode.Change -> R.string.encrypt_change_body
                            PassphraseMode.Disable -> R.string.encrypt_disable_body
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Throne.TextMuted,
                )
                if (mode == PassphraseMode.Create || mode == PassphraseMode.Change) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.encrypt_suggestion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Throne.TextMuted,
                    )
                }
                if (mode == PassphraseMode.Create) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.encrypt_forgotten),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Throne.TextMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.encrypt_cloud_history),
                        style = MaterialTheme.typography.bodySmall,
                        color = Throne.TextFaint,
                    )
                }
                Spacer(Modifier.height(20.dp))
                if (mode == PassphraseMode.Change) {
                    SecretField(
                        value = current,
                        onValueChange = {
                            current = it
                            error = null
                        },
                        label = stringResource(R.string.encrypt_current),
                        imeAction = ImeAction.Next,
                        contentType = ContentType.Password,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                SecretField(
                    value = first,
                    onValueChange = {
                        first = it
                        error = null
                    },
                    label = stringResource(
                        if (mode == PassphraseMode.Change) R.string.encrypt_new else R.string.encrypt_passphrase,
                    ),
                    imeAction = if (mode == PassphraseMode.Create || mode == PassphraseMode.Change) {
                        ImeAction.Next
                    } else {
                        ImeAction.Done
                    },
                    onDone = if (mode == PassphraseMode.Unlock || mode == PassphraseMode.Disable) {
                        { submit() }
                    } else {
                        null
                    },
                    contentType = if (mode == PassphraseMode.Create || mode == PassphraseMode.Change) {
                        ContentType.NewPassword
                    } else {
                        ContentType.Password
                    },
                )
                if (mode == PassphraseMode.Create || mode == PassphraseMode.Change) {
                    Spacer(Modifier.height(12.dp))
                    SecretField(
                        value = second,
                        onValueChange = {
                            second = it
                            error = null
                        },
                        label = stringResource(R.string.encrypt_confirm),
                        imeAction = ImeAction.Done,
                        onDone = { submit() },
                        contentType = ContentType.NewPassword,
                    )
                }
                if (shownError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(shownError),
                        style = MaterialTheme.typography.bodySmall,
                        color = Throne.Error,
                    )
                }
                Spacer(Modifier.height(24.dp))
                GoldButton(
                    text = stringResource(action),
                    onClick = { submit() },
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                    loading = busy,
                    enabled = !busy,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun BackupPassphraseDialog(
    failed: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SecureWindow()
    var passphrase by remember { mutableStateOf("") }
    var blank by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_passphrase_title)) },
        text = {
            Column {
                Text(stringResource(R.string.import_passphrase_body))
                Spacer(Modifier.height(12.dp))
                SecretField(
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        blank = false
                    },
                    label = stringResource(R.string.encrypt_passphrase),
                    imeAction = ImeAction.Done,
                    onDone = {
                        if (ExportCrypto.normalize(passphrase).isEmpty()) blank = true
                        else onSubmit(ExportCrypto.normalize(passphrase))
                    },
                    contentType = ContentType.Password,
                )
                if (blank || failed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(if (blank) R.string.encrypt_blank else R.string.snack_import_bad_passphrase),
                        style = MaterialTheme.typography.bodySmall,
                        color = Throne.Error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (ExportCrypto.normalize(passphrase).isEmpty()) blank = true
                    else onSubmit(ExportCrypto.normalize(passphrase))
                },
            ) { Text(stringResource(R.string.settings_import)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        containerColor = Throne.CardRaised,
        titleContentColor = Throne.Porcelain,
        textContentColor = Throne.TextMuted,
    )
}

@Composable
private fun SecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    contentType: ContentType,
    onDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentType = contentType },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone?.invoke() },
            onNext = { },
        ),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Throne.Gold,
            unfocusedBorderColor = Throne.OutlineSoft,
            focusedContainerColor = Throne.Card,
            unfocusedContainerColor = Throne.Card,
            cursorColor = Throne.Gold,
            focusedTextColor = Throne.Porcelain,
            unfocusedTextColor = Throne.Porcelain,
            focusedLabelColor = Throne.Gold,
            unfocusedLabelColor = Throne.TextMuted,
        ),
    )
}
