package com.mhurston.ascendant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mhurston.ascendant.data.WorkoutDayEntity
import com.mhurston.ascendant.ui.theme.AuraCyan
import com.mhurston.ascendant.ui.theme.ManaPurple
import com.mhurston.ascendant.ui.theme.TextDim
import com.mhurston.ascendant.ui.theme.XpGold
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CalendarScreen(
    state: UiState,
    onAddReps: (String, ExerciseKind, Int) -> Unit,
    onAddMiles: (String, Double) -> Unit,
    onResetDay: (String) -> Unit,
    onSetNotes: (String, String) -> Unit = { _, _ -> },
    onSetConsumed: (String, Int) -> Unit = { _, _ -> },
    onSetWeight: (String, Double) -> Unit = { _, _ -> },
    onAddCustomReps: (String, String, Int) -> Unit = { _, _, _ -> },
    onAddCustomExercise: (String) -> Unit = {},
    onAddPushVariant: (String, String, Int) -> Unit = { _, _, _ -> },
    onAddCoreVariant: (String, String, Int) -> Unit = { _, _, _ -> },
    onAddCardioMinutes: (String, String, Int) -> Unit = { _, _, _ -> },
    onAddOneOff: (String, com.mhurston.ascendant.domain.OneOff) -> Unit = { _, _ -> },
    onUpdateOneOff: (String, Int, com.mhurston.ascendant.domain.OneOff) -> Unit = { _, _, _ -> },
    onRemoveOneOff: (String, Int) -> Unit = { _, _ -> },
    unitSystem: com.mhurston.ascendant.domain.UnitSystem = com.mhurston.ascendant.domain.UnitSystem.IMPERIAL,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    val byDate: Map<String, WorkoutDayEntity> = remember(state.days) {
        state.days.associateBy { it.date }
    }

    selected?.let { date ->
        // Effective (carried-forward) energy values for this day: the most recent weigh-in / intake
        // logged on or before it. Weight falls back to the profile weight before any weigh-in.
        // These show on every day after an entry until the next one — "carry forward unless changed".
        val dateStr = date.toString()
        val sortedDays = state.days.sortedBy { it.date }
        val carriedWeightKg = sortedDays.lastOrNull { it.date <= dateStr && it.weightKg > 0.0 }
            ?.weightKg ?: state.profile.weightKg
        // >= 0 because an explicit 0 is a logged fasting day; -1 is the "not logged" sentinel.
        // The carried value is what the day's diet XP actually used, so the editor shows it.
        val carriedConsumed = sortedDays.lastOrNull { it.date <= dateStr && it.caloriesConsumed >= 0 }
            ?.caloriesConsumed ?: -1
        val ent = byDate[dateStr]
        // Day's gross activity burn, computed with the carried-forward weight so the number
        // matches the day's XP/energy math (weightFor uses day.weightKg when > 0).
        val dayForBurn = (ent ?: WorkoutDayEntity(date = dateStr)).toDayData()
            .copy(weightKg = carriedWeightKg)
        val caloriesBurned =
            com.mhurston.ascendant.domain.Calories.activityBurn(state.profile, dayForBurn).roundToInt()
        DayEditorDialog(
            date = date,
            entity = ent,
            completion = state.derivedByDate[date]?.completion ?: 0.0,
            xp = state.derivedByDate[date]?.xp ?: 0L,
            caloriesBurned = caloriesBurned,
            carriedWeightKg = carriedWeightKg,
            weightLoggedToday = (ent?.weightKg ?: 0.0) > 0.0,
            carriedConsumed = carriedConsumed,
            consumedLoggedToday = (ent?.caloriesConsumed ?: -1) >= 0,
            onAddReps = { kind, delta -> onAddReps(date.toString(), kind, delta) },
            onAddMiles = { delta -> onAddMiles(date.toString(), delta) },
            onReset = { onResetDay(date.toString()) },
            onSetNotes = { n -> onSetNotes(date.toString(), n) },
            onSetConsumed = { v -> onSetConsumed(date.toString(), v) },
            onSetWeight = { kg -> onSetWeight(date.toString(), kg) },
            customExercises = state.allCustomExercises,
            onAddCustomReps = { id, delta -> onAddCustomReps(date.toString(), id, delta) },
            onAddCustomExercise = onAddCustomExercise,
            onAddPushVariant = { id, delta -> onAddPushVariant(date.toString(), id, delta) },
            onAddCoreVariant = { id, delta -> onAddCoreVariant(date.toString(), id, delta) },
            onAddCardioMinutes = { id, delta -> onAddCardioMinutes(date.toString(), id, delta) },
            weightKg = state.profile.weightKg,
            unitSystem = unitSystem,
            onAddOneOff = { oneOff -> onAddOneOff(date.toString(), oneOff) },
            onUpdateOneOff = { idx, oneOff -> onUpdateOneOff(date.toString(), idx, oneOff) },
            onRemoveOneOff = { idx -> onRemoveOneOff(date.toString(), idx) },
            onDismiss = { selected = null }
        )
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        ScreenTitle("Workout Log")
        ScreenSubtitle("${state.days.count { it.date <= today.toString() }} days recorded · tap any day to view or fix it")
        Spacer(Modifier.height(16.dp))

        // Month header with navigation
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            NavBtn("‹") { month = month.minusMonths(1) }
            SectionHeader("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}")
            NavBtn("›") { month = month.plusMonths(1) }
        }
        Spacer(Modifier.height(12.dp))

        // Weekday header (Sun..Sat)
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Caption(it)
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        MonthGrid(
            month = month,
            today = today,
            derivedByDate = state.derivedByDate,
            onClick = { selected = it }
        )

        Spacer(Modifier.height(16.dp))
        LegendRow()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    derivedByDate: Map<LocalDate, com.mhurston.ascendant.domain.DayDerived>,
    onClick: (LocalDate) -> Unit
) {
    val first = month.atDay(1)
    // Sunday-first week: ISO Monday=1..Sunday=7 → Sun=0, Mon=1, … Sat=6 leading blanks.
    val leading = first.dayOfWeek.value % 7
    val daysInMonth = month.lengthOfMonth()
    val cells = leading + daysInMonth
    val rows = (cells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 7) {
                    val cellIndex = r * 7 + col
                    val dayNum = cellIndex - leading + 1
                    if (dayNum in 1..daysInMonth) {
                        val date = month.atDay(dayNum)
                        DayCell(
                            date = date,
                            isFuture = date.isAfter(today),
                            completion = derivedByDate[date]?.completion ?: 0.0,
                            isToday = date == today,
                            modifier = Modifier.weight(1f),
                            onClick = { onClick(date) }
                        )
                    } else {
                        Box(Modifier.weight(1f).aspectRatio(1f)) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isFuture: Boolean,
    completion: Double,
    isToday: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val pct = (completion * 100).roundToInt()
    val fill = if (isFuture) Color(0xFF15151F) else completionColor(completion)
    val darkText = !isFuture && completion >= 0.6
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .then(if (isToday) Modifier.border(2.dp, AuraCyan, RoundedCornerShape(10.dp)) else Modifier)
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.labelMedium,
                color = if (darkText) Color.Black.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (!isFuture && pct > 0) {
                Text("$pct%", style = MaterialTheme.typography.labelMedium,
                    color = if (darkText) Color.Black.copy(alpha = 0.7f) else TextDim)
            }
        }
    }
}

// Gold is the top tier (100% = the reward color); cyan marks a strong 60%+ day.
private fun completionColor(c: Double): Color = when {
    c >= 1.0 -> XpGold
    c >= 0.6 -> AuraCyan
    c > 0.0 -> ManaPurple.copy(alpha = 0.55f)
    else -> com.mhurston.ascendant.ui.theme.TrackDark
}

@Composable
private fun LegendRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendDot(completionColor(0.0), "none")
        LegendDot(completionColor(0.4), "partial")
        LegendDot(completionColor(0.7), "60%+")
        LegendDot(completionColor(1.0), "100%")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.height(14.dp).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(color))
        Caption(label)
    }
}

@Composable
private fun NavBtn(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraCyan)) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun DayEditorDialog(
    date: LocalDate,
    entity: WorkoutDayEntity?,
    completion: Double,
    xp: Long,
    caloriesBurned: Int = 0,
    onAddReps: (ExerciseKind, Int) -> Unit,
    onAddMiles: (Double) -> Unit,
    onReset: () -> Unit,
    onSetNotes: (String) -> Unit,
    onSetConsumed: (Int) -> Unit = {},
    onSetWeight: (Double) -> Unit = {},
    carriedWeightKg: Double = 0.0,
    weightLoggedToday: Boolean = false,
    carriedConsumed: Int = -1,
    consumedLoggedToday: Boolean = false,
    customExercises: List<com.mhurston.ascendant.domain.CustomExercise> = emptyList(),
    onAddCustomReps: (String, Int) -> Unit = { _, _ -> },
    onAddCustomExercise: (String) -> Unit = {},
    onAddPushVariant: (String, Int) -> Unit = { _, _ -> },
    onAddCoreVariant: (String, Int) -> Unit = { _, _ -> },
    onAddCardioMinutes: (String, Int) -> Unit = { _, _ -> },
    weightKg: Double = 0.0,
    unitSystem: com.mhurston.ascendant.domain.UnitSystem = com.mhurston.ascendant.domain.UnitSystem.IMPERIAL,
    onAddOneOff: (com.mhurston.ascendant.domain.OneOff) -> Unit = {},
    onUpdateOneOff: (Int, com.mhurston.ascendant.domain.OneOff) -> Unit = { _, _ -> },
    onRemoveOneOff: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    var confirmReset by remember { mutableStateOf(false) }
    var showAddCustom by remember { mutableStateOf(false) }
    var showOneOff by remember { mutableStateOf(false) }
    var editingOneOff by remember { mutableStateOf<Pair<Int, com.mhurston.ascendant.domain.OneOff>?>(null) }
    val e = entity ?: WorkoutDayEntity(date = date.toString())
    val pct = (completion * 100).roundToInt()

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset ${date}?") },
            text = { Text("Clears everything logged on this day back to zero. " +
                "Steps synced from Health Connect are kept.") },
            confirmButton = {
                TextButton(onClick = { onReset(); confirmReset = false }) {
                    Text("Reset", color = com.mhurston.ascendant.ui.theme.DangerRed,
                        fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }

    if (showAddCustom) {
        AddCustomExerciseDialog(
            onAdd = { name -> onAddCustomExercise(name); showAddCustom = false },
            onDismiss = { showAddCustom = false }
        )
    }

    if (showOneOff) {
        OneOffDialog(
            weightKg = weightKg,
            unitSystem = unitSystem,
            onAdd = { oneOff, pin ->
                onAddOneOff(oneOff)
                if (pin) onAddCustomExercise(oneOff.name)
                showOneOff = false
            },
            onDismiss = { showOneOff = false }
        )
    }

    editingOneOff?.let { (index, existing) ->
        OneOffDialog(
            weightKg = weightKg,
            unitSystem = unitSystem,
            initial = existing,
            onAdd = { updated, _ ->
                onUpdateOneOff(index, updated)
                editingOneOff = null
            },
            onDismiss = { editingOneOff = null }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(date.toString(), fontWeight = FontWeight.Bold)
                // XP can be negative (a calorie surplus docks it), so sign it explicitly
                // rather than always prefixing "+".
                val xpLabel = if (xp < 0) "$xp" else "+$xp"
                Caption("$pct% · $xpLabel XP")
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val pushBreak = e.pushBreakdown()
                Caption("Push-ups — ${e.pushTotal()} / 100  (any of these count)")
                com.mhurston.ascendant.domain.PushExercise.entries.forEach { v ->
                    EditRow(v.label, pushBreak[v.id] ?: 0) { onAddPushVariant(v.id, it) }
                }
                Spacer(Modifier.height(8.dp))
                val coreBreak = e.coreBreakdown()
                Caption("Core — ${e.coreTotal()} / 100  (any of these count)")
                com.mhurston.ascendant.domain.CoreExercise.entries.forEach { v ->
                    EditRow(v.label, coreBreak[v.id] ?: 0) { onAddCoreVariant(v.id, it) }
                }
                Spacer(Modifier.height(4.dp))
                EditRow("Squats", e.squats) { onAddReps(ExerciseKind.SQUATS, it) }
                EditRow("Calf Raises", e.calfRaises) { onAddReps(ExerciseKind.CALF_RAISES, it) }
                EditRow("Curls", e.curls) { onAddReps(ExerciseKind.CURLS, it) }
                // Walking: tracked steps from Health Connect are read-only history; the
                // treadmill/manual miles below are the hand-editable part. Combined total
                // matches the dashboard's walking card.
                if (e.passiveSteps > 0) {
                    TrackedWalkRow(steps = e.passiveSteps, trackedMiles = e.trackedMiles)
                }
                MilesEditRow(e.miles, onAddMiles)
                if (e.passiveSteps > 0) {
                    ReadOnlyRow("Total walking", "${"%.1f".format(Locale.US, e.walkMiles)} mi")
                }
                val cardioMin = WorkoutDayEntity.decodeCustomReps(e.cardioMinutes)
                com.mhurston.ascendant.domain.CardioActivity.entries.forEach { act ->
                    EditRow("${act.label} (min)", cardioMin[act.id] ?: 0) { onAddCardioMinutes(act.id, it) }
                }

                Spacer(Modifier.height(8.dp))
                // Energy: fix a past day's intake, or record/correct a weigh-in. Each value carries
                // forward to later days until the next entry, so days after an entry SHOW it (marked
                // "carried") rather than reading blank — editing here re-anchors from this day on.
                Caption("Energy")
                // Calories burned by activity this day (gross) — read-only, computed from the
                // logged work + tracked steps, so history shows the full picture.
                ReadOnlyRow("Calories burned", "$caloriesBurned kcal")
                val metric = unitSystem == com.mhurston.ascendant.domain.UnitSystem.METRIC
                val wUnit = if (metric) "kg" else "lb"
                val shownWeight = if (carriedWeightKg > 0.0) {
                    val w = if (metric) carriedWeightKg
                        else com.mhurston.ascendant.domain.Units.kgToLbs(carriedWeightKg)
                    if (w == Math.floor(w)) w.toInt().toString()
                    else String.format(java.util.Locale.US, "%.1f", w)
                } else ""
                SetValueRow(
                    "Weigh-in ($wUnit)", shownWeight,
                    inherited = !weightLoggedToday, decimal = true
                ) { entered ->
                    val v = entered.toDoubleOrNull() ?: 0.0
                    val kg = when {
                        v <= 0.0 -> 0.0
                        metric -> v
                        else -> com.mhurston.ascendant.domain.Units.lbsToKg(v)
                    }
                    onSetWeight(kg)
                }
                SetValueRow(
                    "Calories eaten (0 = fasted)",
                    if (carriedConsumed >= 0) carriedConsumed.toString() else "",
                    inherited = !consumedLoggedToday, decimal = false
                ) { entered -> onSetConsumed(entered.toIntOrNull() ?: -1) }

                Spacer(Modifier.height(8.dp))
                // One-off activities — logged to THIS day only, kept in history forever.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Caption("One-offs (this day only)")
                    AddLink("One-off") { showOneOff = true }
                }
                WorkoutDayEntity.decodeOneOffs(e.oneOffs).forEachIndexed { i, o ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { editingOneOff = i to o }) {
                            BodyText(o.name)
                            val label = o.metricsLabel(unitSystem)
                            if (label.isNotEmpty()) {
                                Caption(label)
                            }
                        }
                        Text("+${o.kcal} XP", color = XpGold, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                        EditIcon { editingOneOff = i to o }
                        RemoveIcon { onRemoveOneOff(i) }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // Pinned recurring custom exercises — rep-based, available every day.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Caption("Pinned 📌")
                    AddLink("Exercise") { showAddCustom = true }
                }
                val customReps = WorkoutDayEntity.decodeCustomReps(e.customReps)
                // Active exercises are always editable here; archived ones appear only on
                // days they were actually logged, so old entries stay visible after removal.
                customExercises
                    .filter { !it.archived || (customReps[it.id] ?: 0) > 0 }
                    .forEach { ex ->
                        EditRow(ex.name, customReps[ex.id] ?: 0) { onAddCustomReps(ex.id, it) }
                    }
                Spacer(Modifier.height(12.dp))
                JournalSection(
                    dateKey = date.toString(),
                    notes = e.notes,
                    onNotes = onSetNotes
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { confirmReset = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = com.mhurston.ascendant.ui.theme.DangerRed)
                ) { Text("Reset this day") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

/** Name-entry dialog for creating a custom exercise from a logged day. Mirrors the
 *  dashboard's add flow; the new exercise is created globally and then loggable here. */
@Composable
private fun AddCustomExerciseDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add custom exercise") },
        text = {
            Column {
                Caption("Their reps burn calories (= XP) but don't change your completion % or stats.")
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    singleLine = true,
                    label = { Text("Name (e.g. Pull-ups, Plank sec)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onAdd(name.trim()) }) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditRow(name: String, value: Int, onAdd: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        // Flexible so long names (e.g. "Standing Dumbbell Chest Fly") wrap instead of
        // pushing the rep buttons off-screen.
        Column(Modifier.weight(1f)) {
            BodyText(name)
            Caption("$value", color = AuraCyan)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallBtn("−10") { onAdd(-10) }
            SmallBtn("+10") { onAdd(10) }
            SmallBtn("+25") { onAdd(25) }
        }
    }
}

/** A set-to-a-value row (vs. the +/− delta rows): a labelled numeric field + Set button, for
 *  values you correct outright rather than nudge — a weigh-in or a day's calories. [current] is the
 *  effective value (explicit on this day, or carried forward from an earlier one); [inherited]
 *  marks it as carried so the user knows it isn't anchored here yet. Setting it anchors it to this
 *  day; an empty entry clears it back to its "not logged" sentinel (0 for weight, -1 for calories
 *  — a typed 0 for calories is a real fasting entry), so the carried-forward value applies again. */
@Composable
private fun SetValueRow(
    label: String,
    current: String,
    inherited: Boolean,
    decimal: Boolean,
    onSet: (String) -> Unit
) {
    var text by remember(current) { mutableStateOf(current) }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BodyText(label)
            val sub = when {
                current.isBlank() -> "not logged"
                inherited -> "$current · carried forward"
                else -> "$current · logged"
            }
            Caption(sub, color = AuraCyan)
        }
        androidx.compose.material3.OutlinedTextField(
            value = text,
            onValueChange = { v ->
                text = v.filter { it.isDigit() || (decimal && it == '.') }.take(6)
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = if (decimal) androidx.compose.ui.text.input.KeyboardType.Decimal
                    else androidx.compose.ui.text.input.KeyboardType.Number
            ),
            modifier = Modifier.width(96.dp)
        )
        SmallBtn("Set") { onSet(text) }
    }
}

/** Read-only tracked walking from Health Connect steps — mirrors the dashboard's
 *  "Tracked (steps)" line so history shows steps and step-estimated miles, not just manual. */
@Composable
private fun TrackedWalkRow(steps: Int, trackedMiles: Double) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column {
            BodyText("Tracked (steps)")
            Caption("auto from Health Connect")
        }
        Text("${"%.1f".format(Locale.US, trackedMiles)} mi  ·  ${"%,d".format(Locale.US, steps)} steps",
            style = MaterialTheme.typography.labelMedium, color = AuraCyan, fontWeight = FontWeight.Bold)
    }
}

/** A labelled read-only value row (no controls) — for derived history that isn't hand-editable,
 *  e.g. tracked total or a day's calories burned. */
@Composable
private fun ReadOnlyRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        BodyText(label)
        Text(value, style = MaterialTheme.typography.labelMedium, color = AuraCyan, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MilesEditRow(miles: Double, onAdd: (Double) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column {
            BodyText("Treadmill / manual")
            Caption("${"%.1f".format(Locale.US, miles)} mi", color = AuraCyan)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallBtn("−0.5") { onAdd(-0.5) }
            SmallBtn("+0.5") { onAdd(0.5) }
            SmallBtn("+1") { onAdd(1.0) }
        }
    }
}

@Composable
private fun SmallBtn(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraCyan)
    ) { Text(label, style = MaterialTheme.typography.labelMedium) }
}
