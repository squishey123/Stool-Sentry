package com.squishey.stoolsentry.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.squishey.stoolsentry.BuildConfig
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.domain.UserSettings
import com.squishey.stoolsentry.ui.components.CardDivider
import com.squishey.stoolsentry.ui.components.ScreenBody
import com.squishey.stoolsentry.ui.components.SectionLabel
import com.squishey.stoolsentry.ui.components.ThroneCard
import com.squishey.stoolsentry.ui.components.ThroneTopBar
import com.squishey.stoolsentry.ui.components.currentLocale
import com.squishey.stoolsentry.ui.theme.Throne
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun SettingsScreen(
    settings: UserSettings,
    lockAvailable: Boolean,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onLockChange: (Boolean) -> Unit,
    onWeekStart: (DayOfWeek) -> Unit,
    onAutoExportEnabled: (Boolean) -> Unit = {},
    onPickAutoExportFolder: () -> Unit = {},
    onLicense: () -> Unit = {},
    encrypted: Boolean = false,
    onEnableEncryption: () -> Unit = {},
    onChangePassphrase: () -> Unit = {},
    onDisableEncryption: () -> Unit = {},
    onExportReadable: () -> Unit = {},
) {
    var confirmExport by rememberSaveable { mutableStateOf(false) }
    var confirmReadable by rememberSaveable { mutableStateOf(false) }
    var confirmAutoExport by rememberSaveable { mutableStateOf(false) }
    var pickWeekStart by rememberSaveable { mutableStateOf(false) }
    var showDisclaimer by rememberSaveable { mutableStateOf(false) }
    val locale = currentLocale()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { ThroneTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
    ) { padding ->
        ScreenBody(Modifier.padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp),
            ) {
                Section(stringResource(R.string.settings_section_data)) {
                    SettingsRow(
                        icon = R.drawable.ic_download,
                        title = stringResource(R.string.settings_export),
                        body = stringResource(
                            if (encrypted) R.string.settings_export_body_encrypted else R.string.settings_export_body,
                        ),
                        onClick = { confirmExport = true },
                    )
                    if (encrypted) {
                        CardDivider(Modifier.padding(start = 64.dp))
                        SettingsRow(
                            icon = R.drawable.ic_download,
                            title = stringResource(R.string.settings_export_readable),
                            body = stringResource(R.string.settings_export_readable_body),
                            onClick = { confirmReadable = true },
                        )
                    }
                    CardDivider(Modifier.padding(start = 64.dp))
                    AutoExportRow(
                        enabled = settings.autoExportEnabled,
                        body = stringResource(
                            if (encrypted) {
                                R.string.settings_auto_export_body_encrypted
                            } else {
                                R.string.settings_auto_export_body
                            },
                        ),
                        onChange = { enable ->
                            if (enable) confirmAutoExport = true else onAutoExportEnabled(false)
                        },
                    )
                    if (settings.autoExportEnabled) {
                        CardDivider(Modifier.padding(start = 64.dp))
                        SettingsRow(
                            icon = R.drawable.ic_folder,
                            title = stringResource(R.string.settings_auto_export_folder),
                            body = settings.autoExportFolderName
                                ?: stringResource(R.string.settings_auto_export_choose),
                            onClick = onPickAutoExportFolder,
                        )
                    }
                    CardDivider(Modifier.padding(start = 64.dp))
                    SettingsRow(
                        icon = R.drawable.ic_upload,
                        title = stringResource(R.string.settings_import),
                        body = stringResource(R.string.settings_import_body),
                        onClick = onImport,
                    )
                }
                if (settings.autoExportEnabled) {
                    Note(
                        stringResource(
                            if (encrypted) R.string.settings_auto_export_note_encrypted else R.string.settings_auto_export_note,
                        ),
                    )
                }
                Note(
                    stringResource(
                        if (encrypted) R.string.settings_backup_note_encrypted else R.string.settings_backup_note,
                    ),
                )

                Section(stringResource(R.string.settings_section_privacy)) {
                    if (encrypted) {
                        SettingsRow(
                            icon = R.drawable.ic_shield,
                            title = stringResource(R.string.settings_encrypt_on),
                            body = stringResource(R.string.settings_encrypt_on_body),
                        )
                        CardDivider(Modifier.padding(start = 64.dp))
                        SettingsRow(
                            icon = R.drawable.ic_lock,
                            title = stringResource(R.string.settings_encrypt_change),
                            body = stringResource(R.string.settings_encrypt_change_body),
                            onClick = onChangePassphrase,
                        )
                        CardDivider(Modifier.padding(start = 64.dp))
                        SettingsRow(
                            icon = R.drawable.ic_lock,
                            title = stringResource(R.string.settings_encrypt_off),
                            body = stringResource(R.string.settings_encrypt_off_body),
                            onClick = onDisableEncryption,
                        )
                    } else {
                        SettingsRow(
                            icon = R.drawable.ic_shield,
                            title = stringResource(R.string.settings_encrypt),
                            body = stringResource(R.string.settings_encrypt_body),
                            onClick = onEnableEncryption,
                        )
                    }
                    CardDivider(Modifier.padding(start = 64.dp))
                    LockRow(
                        enabled = settings.appLockEnabled,
                        available = lockAvailable,
                        onChange = onLockChange,
                    )
                }
                Note(
                    stringResource(
                        if (encrypted) R.string.settings_lock_note_encrypted else R.string.settings_lock_note,
                    ),
                )

                Section(stringResource(R.string.settings_section_calendar)) {
                    SettingsRow(
                        icon = R.drawable.ic_calendar,
                        title = stringResource(R.string.settings_week_start),
                        body = settings.weekStart.getDisplayName(TextStyle.FULL, locale),
                        onClick = { pickWeekStart = true },
                    )
                }

                Section(stringResource(R.string.settings_section_about)) {
                    SettingsRow(
                        icon = R.drawable.ic_info,
                        title = stringResource(R.string.settings_disclaimer),
                        body = stringResource(R.string.settings_disclaimer_body),
                        onClick = { showDisclaimer = true },
                    )
                    CardDivider(Modifier.padding(start = 64.dp))
                    SettingsRow(
                        icon = R.drawable.ic_crown,
                        title = stringResource(R.string.settings_free),
                        body = stringResource(R.string.settings_free_body),
                    )
                    CardDivider(Modifier.padding(start = 64.dp))
                    SettingsRow(
                        icon = R.drawable.ic_shield,
                        title = stringResource(R.string.settings_license),
                        body = stringResource(R.string.settings_license_value),
                        onClick = onLicense,
                        trailing = {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = Throne.TextFaint,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    CardDivider(Modifier.padding(start = 64.dp))
                    SettingsRow(
                        icon = R.drawable.ic_settings,
                        title = stringResource(R.string.settings_version),
                        body = BuildConfig.VERSION_NAME,
                    )
                }
            }
        }
    }

    if (confirmAutoExport) {
        val hasFolder = !settings.autoExportTreeUri.isNullOrBlank()
        ThroneDialog(
            title = stringResource(R.string.settings_auto_export_dialog_title),
            body = stringResource(
                if (encrypted) {
                    R.string.settings_auto_export_dialog_body_encrypted
                } else {
                    R.string.settings_auto_export_dialog_body
                },
            ),
            confirm = stringResource(
                if (hasFolder) R.string.settings_auto_export_confirm else R.string.settings_auto_export_choose,
            ),
            onConfirm = {
                confirmAutoExport = false
                if (hasFolder) onAutoExportEnabled(true) else onPickAutoExportFolder()
            },
            onDismiss = { confirmAutoExport = false },
        )
    }
    if (confirmExport) {
        ThroneDialog(
            title = stringResource(
                if (encrypted) R.string.settings_export_dialog_title_encrypted else R.string.settings_export_dialog_title,
            ),
            body = stringResource(
                if (encrypted) R.string.settings_export_dialog_body_encrypted else R.string.settings_export_dialog_body,
            ),
            confirm = stringResource(R.string.settings_export_confirm),
            onConfirm = {
                confirmExport = false
                onExport()
            },
            onDismiss = { confirmExport = false },
        )
    }
    if (confirmReadable) {
        ThroneDialog(
            title = stringResource(R.string.settings_export_dialog_title),
            body = stringResource(R.string.settings_export_dialog_body),
            confirm = stringResource(R.string.settings_export_confirm),
            onConfirm = {
                confirmReadable = false
                onExportReadable()
            },
            onDismiss = { confirmReadable = false },
        )
    }
    if (showDisclaimer) {
        ThroneDialog(
            title = stringResource(R.string.disclaimer_title),
            body = stringResource(R.string.disclaimer_body),
            confirm = stringResource(R.string.action_ok),
            onConfirm = { showDisclaimer = false },
            onDismiss = { showDisclaimer = false },
            showCancel = false,
        )
    }
    if (pickWeekStart) {
        WeekStartDialog(
            current = settings.weekStart,
            onPick = {
                pickWeekStart = false
                onWeekStart(it)
            },
            onDismiss = { pickWeekStart = false },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(20.dp))
    SectionLabel(title, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp))
    ThroneCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
        content = content,
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Throne.TextFaint,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val click = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(click)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Throne.CardRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Throne.Gold,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Throne.Porcelain)
            Spacer(Modifier.height(2.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = Throne.TextMuted)
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun AutoExportRow(enabled: Boolean, body: String, onChange: (Boolean) -> Unit) {
    SettingsRow(
        icon = R.drawable.ic_download,
        title = stringResource(R.string.settings_auto_export),
        body = body,
        modifier = Modifier.toggleable(
            value = enabled,
            role = Role.Switch,
            onValueChange = onChange,
        ),
        trailing = {
            Switch(
                checked = enabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Throne.OnGold,
                    checkedTrackColor = Throne.Gold,
                    uncheckedThumbColor = Throne.TextMuted,
                    uncheckedTrackColor = Throne.CardRaised,
                    uncheckedBorderColor = Throne.Outline,
                ),
            )
        },
    )
}

@Composable
private fun LockRow(enabled: Boolean, available: Boolean, onChange: (Boolean) -> Unit) {
    val canToggle = available || enabled
    SettingsRow(
        icon = R.drawable.ic_lock,
        title = stringResource(R.string.settings_lock),
        body = stringResource(if (available) R.string.settings_lock_body else R.string.settings_lock_unavailable),
        modifier = Modifier.toggleable(
            value = enabled,
            enabled = canToggle,
            role = Role.Switch,
            onValueChange = onChange,
        ),
        trailing = {
            Switch(
                checked = enabled,
                onCheckedChange = null,
                enabled = canToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Throne.OnGold,
                    checkedTrackColor = Throne.Gold,
                    uncheckedThumbColor = Throne.TextMuted,
                    uncheckedTrackColor = Throne.CardRaised,
                    uncheckedBorderColor = Throne.Outline,
                ),
            )
        },
    )
}

@Composable
private fun ThroneDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showCancel: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = if (showCancel) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
        } else {
            null
        },
        containerColor = Throne.CardRaised,
        titleContentColor = Throne.Porcelain,
        textContentColor = Throne.TextMuted,
    )
}

@Composable
private fun WeekStartDialog(
    current: DayOfWeek,
    onPick: (DayOfWeek) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_week_start)) },
        text = {
            Column(Modifier.selectableGroup()) {
                DayOfWeek.entries.forEach { day ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .selectable(selected = day == current, role = Role.RadioButton) { onPick(day) }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = day == current,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Throne.Gold,
                                unselectedColor = Throne.Outline,
                            ),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = day.getDisplayName(TextStyle.FULL, locale),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Throne.Porcelain,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        containerColor = Throne.CardRaised,
        titleContentColor = Throne.Porcelain,
    )
}
