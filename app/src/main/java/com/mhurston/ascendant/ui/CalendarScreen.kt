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
            onDismiss = { selected = null }
        )
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Workout Log", style = MaterialTheme.typography.headlineMedium, color = ManaPurple)
        Text("${state.days.count { it.date <= today.toString() }} days recorded · tap any day to view or fix it",
            style = MaterialTheme.typography.labelMedium, color = TextDim)
        Spacer(Modifier.height(16.dp))

        // Month header with navigation
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            NavBtn("‹") { month = month.minusMonths(1) }
            Text(
                "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                style = MaterialTheme.typography.titleLarge, color = AuraCyan,
                fontWeight = FontWeight.Bold
            )
            NavBtn("›") { month = month.plusMonths(1) }
        }
        Spacer(Modifier.height(12.dp))

        // Weekday header (Mon..Sun)
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
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
    // ISO: Monday=1..Sunday=7 → leading blanks before the 1st.
    val leading = first.dayOfWeek.value - 1
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
    onDismiss: () -> Unit
) {
    var confirmReset by remember { mutableStateOf(false) }
    var showAddCustom by remember { mutableStateOf(false) }
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
                Spacer(Modifier.height(4.dp))
                EditRow("Squats", e.squats) { onAddReps(ExerciseKind.SQUATS, it) }
                EditRow("Leg Lifts", e.legLifts) { onAddReps(ExerciseKind.LEG_LIFTS, it) }
                EditRow("Calf Raises", e.calfRaises) { onAddReps(ExerciseKind.CALF_RAISES, it) }
                EditRow("Curls", e.curls) { onAddReps(ExerciseKind.CURLS, it) }
                MilesEditRow(e.miles, onAddMiles)

                Spacer(Modifier.height(8.dp))
                // Custom exercises: add a new one (created globally) or log reps right here.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Custom", style = MaterialTheme.typography.labelMedium, color = TextDim)
                    Text("＋ Add", color = ManaPurple, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showAddCustom = true })
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
