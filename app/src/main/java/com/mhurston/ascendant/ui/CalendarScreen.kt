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
    onSetMood: (String, Int) -> Unit = { _, _ -> },
    onSetNotes: (String, String) -> Unit = { _, _ -> },
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
        DayEditorDialog(
            date = date,
            entity = byDate[date.toString()],
            completion = state.derivedByDate[date]?.completion ?: 0.0,
            xp = state.derivedByDate[date]?.xp ?: 0L,
            onAddReps = { kind, delta -> onAddReps(date.toString(), kind, delta) },
            onAddMiles = { delta -> onAddMiles(date.toString(), delta) },
            onReset = { onResetDay(date.toString()) },
            onSetMood = { m -> onSetMood(date.toString(), m) },
            onSetNotes = { n -> onSetNotes(date.toString(), n) },
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
                    Text(it, style = MaterialTheme.typography.labelMedium, color = TextDim)
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

private fun completionColor(c: Double): Color = when {
    c >= 1.0 -> AuraCyan
    c >= 0.6 -> XpGold
    c > 0.0 -> ManaPurple.copy(alpha = 0.55f)
    else -> Color(0xFF22223A)
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
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextDim)
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
    onAddReps: (ExerciseKind, Int) -> Unit,
    onAddMiles: (Double) -> Unit,
    onReset: () -> Unit,
    onSetMood: (Int) -> Unit,
    onSetNotes: (String) -> Unit,
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
            text = { Text("Clears everything logged on this day back to zero.") },
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
                Text("$pct% · +$xp XP", style = MaterialTheme.typography.labelMedium, color = TextDim)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val pushBreak = e.pushBreakdown()
                Text("Push-ups — ${e.pushTotal()} / 100  (any of these count)",
                    style = MaterialTheme.typography.labelMedium, color = TextDim)
                com.mhurston.ascendant.domain.PushExercise.entries.forEach { v ->
                    EditRow(v.label, pushBreak[v.id] ?: 0) { onAddPushVariant(v.id, it) }
                }
                Spacer(Modifier.height(8.dp))
                val coreBreak = e.coreBreakdown()
                Text("Core — ${e.coreTotal()} / 100  (any of these count)",
                    style = MaterialTheme.typography.labelMedium, color = TextDim)
                com.mhurston.ascendant.domain.CoreExercise.entries.forEach { v ->
                    EditRow(v.label, coreBreak[v.id] ?: 0) { onAddCoreVariant(v.id, it) }
                }
                Spacer(Modifier.height(4.dp))
                EditRow("Squats", e.squats) { onAddReps(ExerciseKind.SQUATS, it) }
                EditRow("Calf Raises", e.calfRaises) { onAddReps(ExerciseKind.CALF_RAISES, it) }
                EditRow("Curls", e.curls) { onAddReps(ExerciseKind.CURLS, it) }
                MilesEditRow(e.miles, onAddMiles)
                val cardioMin = WorkoutDayEntity.decodeCustomReps(e.cardioMinutes)
                com.mhurston.ascendant.domain.CardioActivity.entries.forEach { act ->
                    EditRow("${act.label} (min)", cardioMin[act.id] ?: 0) { onAddCardioMinutes(act.id, it) }
                }

                Spacer(Modifier.height(8.dp))
                // One-off activities — logged to THIS day only, kept in history forever.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("One-offs (this day only)", style = MaterialTheme.typography.labelMedium, color = TextDim)
                    AddLink("One-off") { showOneOff = true }
                }
                WorkoutDayEntity.decodeOneOffs(e.oneOffs).forEachIndexed { i, o ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { editingOneOff = i to o }) {
                            Text(o.name, style = MaterialTheme.typography.bodyLarge)
                            val label = o.metricsLabel(unitSystem)
                            if (label.isNotEmpty()) {
                                Text(label, style = MaterialTheme.typography.labelMedium, color = TextDim)
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
                    Text("Pinned 📌", style = MaterialTheme.typography.labelMedium, color = TextDim)
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
                    mood = e.mood,
                    notes = e.notes,
                    onMood = onSetMood,
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
                Text("These earn bonus XP but don't change your completion % or stats.",
                    style = MaterialTheme.typography.labelMedium, color = TextDim)
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
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text("$value", style = MaterialTheme.typography.labelMedium, color = AuraCyan)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallBtn("−10") { onAdd(-10) }
            SmallBtn("+10") { onAdd(10) }
            SmallBtn("+25") { onAdd(25) }
        }
    }
}

@Composable
private fun MilesEditRow(miles: Double, onAdd: (Double) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Walking", style = MaterialTheme.typography.bodyLarge)
            Text("${"%.1f".format(miles)} mi", style = MaterialTheme.typography.labelMedium, color = AuraCyan)
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
