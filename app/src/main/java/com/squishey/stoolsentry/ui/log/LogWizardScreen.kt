package com.squishey.stoolsentry.ui.log

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.domain.BristolType
import com.squishey.stoolsentry.domain.LogValidator
import com.squishey.stoolsentry.domain.StoolColor
import com.squishey.stoolsentry.domain.StoolSize
import com.squishey.stoolsentry.domain.Symptoms
import com.squishey.stoolsentry.domain.WizardState
import com.squishey.stoolsentry.domain.WizardStep
import com.squishey.stoolsentry.ui.components.BristolGlyph
import com.squishey.stoolsentry.ui.components.CardDivider
import com.squishey.stoolsentry.ui.components.CheckBadge
import com.squishey.stoolsentry.ui.components.GoldButton
import com.squishey.stoolsentry.ui.components.GoldGradient
import com.squishey.stoolsentry.ui.components.IconAction
import com.squishey.stoolsentry.ui.components.QuietButton
import com.squishey.stoolsentry.ui.components.ScreenBody
import com.squishey.stoolsentry.ui.components.SectionLabel
import com.squishey.stoolsentry.ui.components.ThroneCard
import com.squishey.stoolsentry.ui.components.rememberSkeletonFormatter
import com.squishey.stoolsentry.ui.components.rememberTimeFormatter
import com.squishey.stoolsentry.ui.theme.Throne
import java.time.LocalDate
import java.time.LocalTime

private val STEP_COUNT = WizardStep.entries.size
private const val MILLIS_PER_DAY = 86_400_000L

@Composable
fun LogWizardScreen(
    draft: WizardState,
    saving: Boolean,
    editing: Boolean,
    isInFuture: Boolean,
    canSave: Boolean,
    today: LocalDate,
    onSelectType: (BristolType) -> Unit,
    onSelectSize: (StoolSize) -> Unit,
    onSelectColor: (StoolColor) -> Unit,
    onSymptoms: (Symptoms) -> Unit,
    onNotes: (String) -> Unit,
    onDateTime: (LocalDate, LocalTime) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            WizardBar(
                step = draft.step,
                canAdvance = draft.canAdvance(),
                canSave = canSave,
                saving = saving,
                onBack = onBack,
                onNext = onNext,
                onSave = onSave,
            )
        },
    ) { padding ->
        ScreenBody(Modifier.padding(padding)) {
            AnimatedContent(
                targetState = draft.step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val enter = slideInHorizontally(tween(300)) { w -> if (forward) w / 4 else -w / 4 } +
                        fadeIn(tween(240))
                    val exit = slideOutHorizontally(tween(300)) { w -> if (forward) -w / 4 else w / 4 } +
                        fadeOut(tween(140))
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                label = "wizardStep",
            ) { step ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                ) {
                    StepHeader(step = step, editing = editing)
                    Spacer(Modifier.height(16.dp))
                    when (step) {
                        WizardStep.TYPE -> TypeStep(draft.bristolType, onSelectType)
                        WizardStep.SIZE -> SizeStep(draft.size, onSelectSize)
                        WizardStep.COLOR -> ColorStep(draft.color, onSelectColor)
                        WizardStep.NOTES -> DetailsStep(
                            draft = draft,
                            isInFuture = isInFuture,
                            today = today,
                            onSymptoms = onSymptoms,
                            onNotes = onNotes,
                            onDateTime = onDateTime,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(step: WizardStep, editing: Boolean) {
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val (title, subtitle) = when (step) {
        WizardStep.TYPE -> R.string.wizard_type_title to R.string.wizard_type_subtitle
        WizardStep.SIZE -> R.string.wizard_size_title to R.string.wizard_size_subtitle
        WizardStep.COLOR -> R.string.wizard_color_title to R.string.wizard_color_subtitle
        WizardStep.NOTES -> R.string.wizard_details_title to R.string.wizard_details_subtitle
    }
    val stepNumber = step.ordinal + 1
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            SectionLabel(
                if (editing) {
                    stringResource(R.string.wizard_step_editing, stepNumber, STEP_COUNT)
                } else {
                    stringResource(R.string.wizard_step, stepNumber, STEP_COUNT)
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(title), style = MaterialTheme.typography.headlineLarge, color = Throne.Porcelain)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(subtitle), style = MaterialTheme.typography.bodyMedium, color = Throne.TextMuted)
        }
        if (step == WizardStep.TYPE) {
            IconAction(
                R.drawable.ic_info,
                stringResource(R.string.wizard_bristol_info),
                onClick = { showInfo = true },
                tint = Throne.GoldLight,
            )
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.action_ok)) }
            },
            title = { Text(stringResource(R.string.wizard_bristol_info_title)) },
            text = { Text(stringResource(R.string.wizard_bristol_info_body)) },
            containerColor = Throne.CardRaised,
            titleContentColor = Throne.Porcelain,
            textContentColor = Throne.TextMuted,
        )
    }
}

@Composable
private fun TypeStep(selected: BristolType?, onSelect: (BristolType) -> Unit) {
    val rows = listOf(
        listOf(BristolType.TYPE_1, BristolType.TYPE_2, BristolType.TYPE_3),
        listOf(BristolType.TYPE_4, BristolType.TYPE_5, BristolType.TYPE_6),
    )
    Column(
        Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { type ->
                    TypeCard(
                        type = type,
                        selected = type == selected,
                        onClick = { onSelect(type) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.weight(1f))
            TypeCard(
                type = BristolType.TYPE_7,
                selected = selected == BristolType.TYPE_7,
                onClick = { onSelect(BristolType.TYPE_7) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TypeCard(
    type: BristolType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        ThroneCard(
            modifier = Modifier.fillMaxSize(),
            onClick = onClick,
            selected = selected,
            selectable = true,
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
        ) {
            BristolGlyph(
                type = type,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = type.label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) Throne.GoldLight else Throne.Porcelain,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = type.description,
                style = MaterialTheme.typography.bodySmall,
                color = Throne.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SelectionBadge(selected, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun SelectionBadge(selected: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = selected,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier.padding(6.dp),
    ) {
        CheckBadge()
    }
}

@Composable
private fun SizeStep(selected: StoolSize?, onSelect: (StoolSize) -> Unit) {
    Column(
        Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StoolSize.entries.forEachIndexed { index, size ->
            val isSelected = size == selected
            ThroneCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(size) },
                selected = isSelected,
                selectable = true,
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SizeMeter(level = index, selected = isSelected)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = size.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) Throne.GoldLight else Throne.Porcelain,
                        modifier = Modifier.weight(1f),
                    )
                    RadioMark(isSelected)
                }
            }
        }
    }
}

@Composable
private fun SizeMeter(level: Int, selected: Boolean) {
    val count = StoolSize.entries.size
    Row(
        Modifier.height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (i in 0 until count) {
            val filled = i <= level
            val color by animateColorAsState(
                when {
                    filled && selected -> Throne.Gold
                    filled -> Throne.GoldDim
                    else -> Throne.CardHighest
                },
                label = "meter",
            )
            Box(
                Modifier
                    .width(5.dp)
                    .height((8 + i * 3.6f).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun RadioMark(selected: Boolean) {
    val ring by animateColorAsState(if (selected) Throne.Gold else Throne.Outline, label = "radioRing")
    val dot by animateDpAsState(if (selected) 12.dp else 0.dp, label = "radioDot")
    Box(
        Modifier
            .size(24.dp)
            .border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(dot)
                .clip(CircleShape)
                .background(GoldGradient),
        )
    }
}

@Composable
private fun ColorStep(selected: StoolColor?, onSelect: (StoolColor) -> Unit) {
    Column(
        Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StoolColor.entries.chunked(3).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { color ->
                    ColorCell(
                        color = color,
                        selected = color == selected,
                        onClick = { onSelect(color) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorCell(
    color: StoolColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swatch = Color(color.swatchArgb)
    ThroneCard(
        modifier = modifier,
        onClick = onClick,
        selected = selected,
        selectable = true,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(46.dp)
                .clip(CircleShape)
                .background(swatch)
                .border(1.dp, Throne.Porcelain.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val checkScale by animateFloatAsState(if (selected) 1f else 0f, label = "colorCheck")
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = if (swatch.luminance() > 0.4f) Throne.OnGold else Throne.Porcelain,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = checkScale
                    },
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = color.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Throne.GoldLight else Throne.Porcelain,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsStep(
    draft: WizardState,
    isInFuture: Boolean,
    today: LocalDate,
    onSymptoms: (Symptoms) -> Unit,
    onNotes: (String) -> Unit,
    onDateTime: (LocalDate, LocalTime) -> Unit,
) {
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    val dateFormatter = rememberSkeletonFormatter("EEEMMMdy")
    val timeFormatter = rememberTimeFormatter()
    val time = draft.localTime()

    SectionLabel(stringResource(R.string.wizard_when))
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PickerPill(
            icon = R.drawable.ic_calendar,
            text = draft.localDate.format(dateFormatter),
            actionLabel = stringResource(R.string.wizard_pick_date),
            onClick = { showDate = true },
            modifier = Modifier.weight(1.4f),
        )
        PickerPill(
            icon = R.drawable.ic_clock,
            text = time.format(timeFormatter),
            actionLabel = stringResource(R.string.wizard_pick_time),
            onClick = { showTime = true },
            modifier = Modifier.weight(1f),
        )
    }
    AnimatedVisibility(visible = isInFuture) {
        Text(
            text = stringResource(R.string.wizard_future_error),
            style = MaterialTheme.typography.bodySmall,
            color = Throne.Error,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
    }

    Spacer(Modifier.height(24.dp))
    SectionLabel(stringResource(R.string.wizard_symptoms))
    Spacer(Modifier.height(10.dp))
    SymptomsCard(draft.symptoms, onSymptoms)

    Spacer(Modifier.height(24.dp))
    SectionLabel(stringResource(R.string.wizard_notes))
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = draft.notes,
        onValueChange = { onNotes(it.take(LogValidator.MAX_NOTES)) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        placeholder = {
            Text(stringResource(R.string.wizard_notes_placeholder), color = Throne.TextFaint)
        },
        supportingText = {
            Text(
                text = stringResource(R.string.wizard_notes_counter, draft.notes.length, LogValidator.MAX_NOTES),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Throne.Gold,
            unfocusedBorderColor = Throne.OutlineSoft,
            focusedContainerColor = Throne.Card,
            unfocusedContainerColor = Throne.Card,
            cursorColor = Throne.Gold,
            focusedTextColor = Throne.Porcelain,
            unfocusedTextColor = Throne.Porcelain,
            focusedSupportingTextColor = Throne.TextMuted,
            unfocusedSupportingTextColor = Throne.TextFaint,
        ),
    )

    if (showDate) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = draft.localDate.toEpochDay() * MILLIS_PER_DAY,
            selectableDates = remember(today) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        !LocalDate.ofEpochDay(Math.floorDiv(utcTimeMillis, MILLIS_PER_DAY)).isAfter(today)

                    override fun isSelectableYear(year: Int): Boolean = year <= today.year
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { millis ->
                            onDateTime(LocalDate.ofEpochDay(Math.floorDiv(millis, MILLIS_PER_DAY)), time)
                        }
                        showDate = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            colors = DatePickerDefaults.colors(containerColor = Throne.CardRaised),
        ) {
            DatePicker(
                state = dateState,
                colors = DatePickerDefaults.colors(containerColor = Throne.CardRaised),
            )
        }
    }

    if (showTime) {
        val timeState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateTime(draft.localDate, LocalTime.of(timeState.hour, timeState.minute))
                        showTime = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
            containerColor = Throne.CardRaised,
        )
    }
}

@Composable
private fun PickerPill(
    icon: Int,
    text: String,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ThroneCard(
        modifier = modifier,
        onClick = onClick,
        onClickLabel = actionLabel,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Throne.Gold,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.titleSmall, color = Throne.Porcelain, maxLines = 1)
        }
    }
}

@Composable
private fun SymptomsCard(symptoms: Symptoms, onChange: (Symptoms) -> Unit) {
    val rows = listOf(
        Triple(R.string.symptom_urgent, symptoms.urgent) { v: Boolean -> symptoms.copy(urgent = v) },
        Triple(R.string.symptom_blood, symptoms.blood) { v: Boolean -> symptoms.copy(blood = v) },
        Triple(R.string.symptom_painful, symptoms.painful) { v: Boolean -> symptoms.copy(painful = v) },
        Triple(R.string.symptom_straining, symptoms.straining) { v: Boolean -> symptoms.copy(straining = v) },
        Triple(R.string.symptom_incomplete, symptoms.incomplete) { v: Boolean -> symptoms.copy(incomplete = v) },
        Triple(R.string.symptom_mucus, symptoms.mucus) { v: Boolean -> symptoms.copy(mucus = v) },
    )
    ThroneCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        rows.forEachIndexed { index, (label, value, update) ->
            YesNoRow(
                label = stringResource(label),
                value = value,
                onChange = { onChange(update(it)) },
            )
            if (index < rows.lastIndex) CardDivider()
        }
    }
}

@Composable
private fun YesNoRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Throne.Porcelain,
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier
                .clip(CircleShape)
                .background(Throne.Espresso.copy(alpha = 0.6f))
                .border(1.dp, Throne.OutlineSoft, CircleShape)
                .padding(3.dp)
                .selectableGroup(),
        ) {
            Segment(
                text = stringResource(R.string.answer_no),
                description = "$label, ${stringResource(R.string.answer_no)}",
                selected = !value,
                selectedBackground = Throne.CardHighest,
                selectedContent = Throne.Porcelain,
                onClick = { onChange(false) },
            )
            Segment(
                text = stringResource(R.string.answer_yes),
                description = "$label, ${stringResource(R.string.answer_yes)}",
                selected = value,
                selectedBackground = Throne.Gold,
                selectedContent = Throne.OnGold,
                onClick = { onChange(true) },
            )
        }
    }
}

@Composable
private fun Segment(
    text: String,
    description: String,
    selected: Boolean,
    selectedBackground: Color,
    selectedContent: Color,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        if (selected) selectedBackground else Color.Transparent,
        label = "segmentBackground",
    )
    val foreground by animateColorAsState(
        if (selected) selectedContent else Throne.TextFaint,
        label = "segmentForeground",
    )
    Box(
        Modifier
            .widthIn(min = 60.dp)
            .height(42.dp)
            .clip(CircleShape)
            .background(background)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = foreground)
    }
}

@Composable
private fun WizardBar(
    step: WizardStep,
    canAdvance: Boolean,
    canSave: Boolean,
    saving: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Throne.Espresso)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        CardDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuietButton(
                text = stringResource(if (step == WizardStep.TYPE) R.string.action_cancel else R.string.action_back),
                onClick = onBack,
                enabled = !saving,
            )
            ProgressDots(
                current = step.ordinal,
                modifier = Modifier.weight(1f),
            )
            if (step == WizardStep.NOTES) {
                GoldButton(
                    text = stringResource(R.string.action_done),
                    onClick = onSave,
                    enabled = canSave,
                    loading = saving,
                    icon = R.drawable.ic_check,
                )
            } else {
                GoldButton(
                    text = stringResource(R.string.action_next),
                    onClick = onNext,
                    enabled = canAdvance,
                )
            }
        }
    }
}

@Composable
private fun ProgressDots(current: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.wizard_progress, current + 1, STEP_COUNT)
    Row(
        modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until STEP_COUNT) {
            val width by animateDpAsState(if (i == current) 22.dp else 8.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (i <= current) Throne.Gold else Throne.Outline,
                label = "dotColor",
            )
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
