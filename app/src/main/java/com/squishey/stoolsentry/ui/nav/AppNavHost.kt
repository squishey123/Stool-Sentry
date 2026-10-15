package com.squishey.stoolsentry.ui.nav

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.squishey.stoolsentry.AppContainer
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.domain.BowelLog
import com.squishey.stoolsentry.domain.ExportCrypto
import com.squishey.stoolsentry.ui.encrypt.BackupPassphraseDialog
import com.squishey.stoolsentry.ui.encrypt.PassphraseForm
import com.squishey.stoolsentry.ui.encrypt.PassphraseMode
import com.squishey.stoolsentry.domain.WizardStep
import com.squishey.stoolsentry.ui.calendar.CalendarScreen
import com.squishey.stoolsentry.ui.calendar.CalendarViewModel
import com.squishey.stoolsentry.ui.day.DayScreen
import com.squishey.stoolsentry.ui.day.DayViewModel
import com.squishey.stoolsentry.ui.lock.Biometrics
import com.squishey.stoolsentry.ui.log.LogWizardScreen
import com.squishey.stoolsentry.ui.log.LogWizardViewModel
import com.squishey.stoolsentry.ui.log.WizardUiState
import com.squishey.stoolsentry.ui.settings.LicenseScreen
import com.squishey.stoolsentry.ui.settings.SettingsScreen
import com.squishey.stoolsentry.ui.settings.SettingsViewModel
import com.squishey.stoolsentry.ui.summary.SummaryScreen
import com.squishey.stoolsentry.ui.summary.SummaryViewModel
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AUTO_ADVANCE_MS = 260L
private const val MAX_IMPORT_BYTES = 10 * 1024 * 1024

@Composable
fun AppNavHost(
    container: AppContainer,
    showMessage: (String) -> Unit,
) {
    val nav = rememberNavController()
    val factory = container.viewModelFactory
    val resources = LocalResources.current

    NavHost(
        navController = nav,
        startDestination = RoutePatterns.CALENDAR,
        enterTransition = {
            fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 8 }
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = {
            fadeOut(tween(180)) + slideOutHorizontally(tween(320)) { it / 8 }
        },
    ) {
        composable(RoutePatterns.CALENDAR) {
            val vm: CalendarViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()
            var hour by remember { mutableIntStateOf(LocalTime.now().hour) }
            LifecycleResumeEffect(vm) {
                vm.refreshToday()
                hour = LocalTime.now().hour
                onPauseOrDispose { }
            }
            CalendarScreen(
                state = state,
                hourOfDay = hour,
                onSelectDay = dropUnlessResumedWith { date ->
                    vm.select(date)
                    nav.navigate(RoutePatterns.day(date))
                },
                onPreviousMonth = vm::showPreviousMonth,
                onNextMonth = vm::showNextMonth,
                onToday = vm::jumpToToday,
                onLog = dropUnlessResumed {
                    val preset = state.selectedDate.takeIf { it != state.today }
                    nav.navigate(RoutePatterns.wizard(presetDate = preset))
                },
                onSummary = dropUnlessResumed { nav.navigate(RoutePatterns.SUMMARY) },
                onSettings = dropUnlessResumed { nav.navigate(RoutePatterns.SETTINGS) },
            )
        }

        composable(
            route = RoutePatterns.WIZARD,
            arguments = listOf(
                navArgument(RoutePatterns.ARG_LOG_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(RoutePatterns.ARG_DATE) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
            enterTransition = { fadeIn(tween(260)) + slideInVertically(tween(360)) { it / 10 } },
            exitTransition = { fadeOut(tween(180)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically(tween(320)) { it / 10 } },
        ) { entry ->
            val vm: LogWizardViewModel = viewModel(factory = factory)
            val editing = (entry.arguments?.getLong(RoutePatterns.ARG_LOG_ID) ?: -1L) > 0L
            val ui by vm.uiState.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            var advanceJob by remember { mutableStateOf<Job?>(null) }
            val close = dropUnlessResumed { nav.popBackStack() }

            fun autoAdvance(from: WizardStep) {
                advanceJob?.cancel()
                advanceJob = scope.launch {
                    delay(AUTO_ADVANCE_MS)
                    val current = vm.uiState.value
                    if (current is WizardUiState.Ready && current.draft.step == from) vm.next()
                }
            }

            val onSaved by rememberUpdatedState { log: BowelLog ->
                finishWizard(nav, factory, log, editing)
                showMessage(resources.getString(if (editing) R.string.snack_updated else R.string.snack_logged))
            }
            LaunchedEffect(vm) {
                vm.saved.collect { onSaved(it) }
            }

            // Re-checks "is this time in the future" as the clock moves on.
            val clockTick by produceState(0) {
                while (true) {
                    delay(30_000)
                    value += 1
                }
            }

            val ready = ui as? WizardUiState.Ready
            BackHandler(enabled = ready?.saving != true) {
                advanceJob?.cancel()
                if (vm.back()) close()
            }
            if (ready != null) {
                val isInFuture = remember(ready.draft, clockTick) { vm.isInFuture(ready.draft) }
                val canSave = remember(ready.draft, clockTick) { vm.canSave(ready.draft) }
                val today = remember(clockTick) { vm.today() }
                LogWizardScreen(
                    draft = ready.draft,
                    saving = ready.saving,
                    editing = editing,
                    isInFuture = isInFuture,
                    canSave = canSave,
                    today = today,
                    onSelectType = {
                        vm.selectType(it)
                        autoAdvance(WizardStep.TYPE)
                    },
                    onSelectSize = {
                        vm.selectSize(it)
                        autoAdvance(WizardStep.SIZE)
                    },
                    onSelectColor = {
                        vm.selectColor(it)
                        autoAdvance(WizardStep.COLOR)
                    },
                    onSymptoms = vm::setSymptoms,
                    onNotes = vm::setNotes,
                    onDateTime = vm::setDateTime,
                    onNext = {
                        advanceJob?.cancel()
                        vm.next()
                    },
                    onBack = {
                        advanceJob?.cancel()
                        if (vm.back()) close()
                    },
                    onSave = vm::save,
                )
            }
        }

        composable(
            route = RoutePatterns.DAY,
            arguments = listOf(navArgument(RoutePatterns.ARG_DATE) { type = NavType.StringType }),
        ) {
            val vm: DayViewModel = viewModel(factory = factory)
            val logs by vm.loadedLogs.collectAsStateWithLifecycle()
            DayScreen(
                date = vm.date,
                today = container.clock.today(),
                logs = logs,
                onBack = dropUnlessResumed { nav.popBackStack() },
                onEdit = dropUnlessResumedWith { id -> nav.navigate(RoutePatterns.wizard(logId = id)) },
                onAdd = dropUnlessResumed { nav.navigate(RoutePatterns.wizard(presetDate = vm.date)) },
                onDelete = { id ->
                    vm.delete(id)
                    showMessage(resources.getString(R.string.snack_deleted))
                },
            )
        }

        composable(RoutePatterns.SUMMARY) {
            val vm: SummaryViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()
            LifecycleResumeEffect(vm) {
                vm.refreshToday()
                onPauseOrDispose { }
            }
            SummaryScreen(
                state = state,
                onBack = dropUnlessResumed { nav.popBackStack() },
                onSelectPeriod = vm::selectPeriod,
            )
        }

        composable(RoutePatterns.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = factory)
            SettingsRoute(
                vm = vm,
                container = container,
                onBack = dropUnlessResumed { nav.popBackStack() },
                onLicense = dropUnlessResumed { nav.navigate(RoutePatterns.LICENSE) },
                onNavigateEncrypt = dropUnlessResumedWith { mode: String ->
                    nav.navigate(RoutePatterns.encrypt(mode))
                },
                showMessage = showMessage,
            )
        }

        composable(RoutePatterns.LICENSE) {
            LicenseScreen(onBack = dropUnlessResumed { nav.popBackStack() })
        }

        composable(
            RoutePatterns.ENCRYPT,
            arguments = listOf(navArgument(RoutePatterns.ARG_MODE) { type = NavType.StringType }),
        ) { entry ->
            val mode = entry.arguments?.getString(RoutePatterns.ARG_MODE)
            EncryptionRoute(
                mode = mode,
                container = container,
                onBack = dropUnlessResumed { nav.popBackStack() },
            )
        }
    }
}

/** Like [dropUnlessResumed] for callbacks that take an argument. */
@Composable
private fun <T> dropUnlessResumedWith(block: (T) -> Unit): (T) -> Unit {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(block)
    return remember(owner) {
        { value: T ->
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                latest(value)
            }
        }
    }
}

private fun finishWizard(
    nav: NavHostController,
    factory: ViewModelProvider.Factory,
    log: BowelLog,
    editing: Boolean,
) {
    val calendarEntry = nav.getBackStackEntry(RoutePatterns.CALENDAR)
    ViewModelProvider(calendarEntry.viewModelStore, factory, calendarEntry.defaultViewModelCreationExtras)[
        CalendarViewModel::class.java,
    ].focus(log.localDate)

    if (!editing) {
        nav.popBackStack(RoutePatterns.CALENDAR, inclusive = false)
        return
    }
    val previous = nav.previousBackStackEntry
    val returnsToSameDay = previous?.destination?.route == RoutePatterns.DAY &&
        previous.arguments?.getString(RoutePatterns.ARG_DATE) == log.localDate.toString()
    if (returnsToSameDay) {
        nav.popBackStack()
    } else {
        nav.navigate(RoutePatterns.day(log.localDate)) {
            popUpTo(RoutePatterns.CALENDAR)
        }
    }
}

@Composable
private fun SettingsRoute(
    vm: SettingsViewModel,
    container: AppContainer,
    onBack: () -> Unit,
    onLicense: () -> Unit,
    onNavigateEncrypt: (String) -> Unit,
    showMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current as? FragmentActivity
    val settings by vm.settings.collectAsStateWithLifecycle()
    val encrypted by vm.encrypted.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var lockAvailable by remember { mutableStateOf(Biometrics.isAvailable(context)) }
    LifecycleResumeEffect(Unit) {
        lockAvailable = Biometrics.isAvailable(context)
        onPauseOrDispose { }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = vm.useAutoExportFolder(uri)
            showMessage(
                resources.getString(
                    if (ok) {
                        R.string.snack_auto_export_on
                    } else if (encrypted) {
                        R.string.snack_auto_export_failed_encrypted
                    } else {
                        R.string.snack_auto_export_failed
                    },
                ),
            )
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { writeExport(context, uri, vm.exportCsv().toByteArray(Charsets.UTF_8), showMessage, resources) }
    }

    val encryptedExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = runCatching {
                val csv = vm.exportCsv()
                container.vault.seal(csv) ?: error("Encryption is off")
            }.getOrNull()
            if (bytes == null) {
                showMessage(resources.getString(R.string.snack_export_failed))
                return@launch
            }
            writeExport(context, uri, bytes, showMessage, resources)
        }
    }

    var pendingImport by remember { mutableStateOf<ByteArray?>(null) }
    var importPassphraseFailed by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { readLimitedBytes(it) }
                }
            }.getOrNull()
            if (bytes == null) {
                showMessage(resources.getString(R.string.snack_import_failed))
                return@launch
            }
            val text = container.vault.readBackup(bytes)
            if (text == null && ExportCrypto.looksEncrypted(bytes)) {
                importPassphraseFailed = false
                pendingImport = bytes
                return@launch
            }
            if (text == null) {
                showMessage(resources.getString(R.string.snack_import_failed))
                return@launch
            }
            showImportResult(vm.importCsv(text), showMessage, resources)
        }
    }

    SettingsScreen(
        settings = settings,
        lockAvailable = lockAvailable,
        encrypted = encrypted,
        onBack = onBack,
        onExport = {
            container.appLock.allowExternalActivity()
            val name = resources.getString(
                if (encrypted) R.string.export_file_name_encrypted else R.string.export_file_name,
                container.clock.today().toString(),
            )
            if (encrypted) encryptedExportLauncher.launch(name) else exportLauncher.launch(name)
        },
        onExportReadable = {
            container.appLock.allowExternalActivity()
            exportLauncher.launch(
                resources.getString(R.string.export_file_name, container.clock.today().toString()),
            )
        },
        onImport = {
            container.appLock.allowExternalActivity()
            importLauncher.launch(
                arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream"),
            )
        },
        onAutoExportEnabled = { enable ->
            if (!enable) {
                vm.setAutoExportEnabled(false)
            } else {
                scope.launch {
                    val ok = vm.enableAutoExport()
                    showMessage(
                        resources.getString(
                            if (ok) {
                                R.string.snack_auto_export_on
                            } else if (encrypted) {
                                R.string.snack_auto_export_failed_encrypted
                            } else {
                                R.string.snack_auto_export_failed
                            },
                        ),
                    )
                }
            }
        },
        onPickAutoExportFolder = {
            container.appLock.allowExternalActivity()
            val initial = settings.autoExportTreeUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            folderLauncher.launch(initial)
        },
        onLicense = onLicense,
        onEnableEncryption = { onNavigateEncrypt("create") },
        onChangePassphrase = { onNavigateEncrypt("change") },
        onDisableEncryption = { onNavigateEncrypt("disable") },
        onLockChange = { enable ->
            if (!enable) {
                vm.setAppLockEnabled(false)
            } else if (activity != null && lockAvailable) {
                Biometrics.authenticate(
                    activity = activity,
                    title = resources.getString(R.string.lock_enable_prompt_title),
                    subtitle = resources.getString(R.string.lock_prompt_subtitle),
                ) {
                    container.appLock.unlock()
                    vm.setAppLockEnabled(true)
                }
            }
        },
        onWeekStart = vm::setWeekStart,
    )

    val pending = pendingImport
    if (pending != null) {
        BackupPassphraseDialog(
            failed = importPassphraseFailed,
            onDismiss = { pendingImport = null },
            onSubmit = { passphrase ->
                scope.launch {
                    val text = container.vault.readBackup(pending, passphrase)
                    if (text == null) {
                        importPassphraseFailed = true
                    } else {
                        pendingImport = null
                        showImportResult(vm.importCsv(text), showMessage, resources)
                    }
                }
            },
        )
    }
}

@Composable
private fun EncryptionRoute(
    mode: String?,
    container: AppContainer,
    onBack: () -> Unit,
) {
    val passphraseMode = when (mode) {
        "change" -> PassphraseMode.Change
        "disable" -> PassphraseMode.Disable
        else -> PassphraseMode.Create
    }
    PassphraseForm(
        mode = passphraseMode,
        onBack = onBack,
        matchesCurrent = container.vault::matches,
        onSubmit = { passphrase ->
            when (passphraseMode) {
                PassphraseMode.Create -> container.vault.beginEnable(passphrase)
                PassphraseMode.Change -> container.vault.beginChange(passphrase)
                PassphraseMode.Disable -> container.vault.beginDisable(passphrase)
                PassphraseMode.Unlock -> Unit
            }
        },
    )
}

private suspend fun writeExport(
    context: android.content.Context,
    uri: Uri,
    bytes: ByteArray,
    showMessage: (String) -> Unit,
    resources: android.content.res.Resources,
) {
    val ok = runCatching {
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("No output stream")
            stream.use { it.write(bytes) }
        }
    }.isSuccess
    showMessage(resources.getString(if (ok) R.string.snack_export_done else R.string.snack_export_failed))
}

private suspend fun showImportResult(
    result: com.squishey.stoolsentry.domain.ImportResult,
    showMessage: (String) -> Unit,
    resources: android.content.res.Resources,
) {
    val message = when {
        result.imported == 0 -> resources.getString(R.string.snack_import_nothing)
        result.skipped == 0 -> resources.getQuantityString(
            R.plurals.snack_imported,
            result.imported,
            result.imported,
        )
        else -> resources.getQuantityString(
            R.plurals.snack_imported_skipped,
            result.skipped,
            result.imported,
            result.skipped,
        )
    }
    showMessage(message)
}

private fun readLimitedBytes(input: InputStream): ByteArray? {
    val buffer = ByteArray(8 * 1024)
    val output = ByteArrayOutputStream()
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        output.write(buffer, 0, read)
        if (output.size() > MAX_IMPORT_BYTES) return null
    }
    return output.toByteArray()
}
