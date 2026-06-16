package com.mhurston.ascendant.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mhurston.ascendant.domain.Progression
import com.mhurston.ascendant.ui.theme.AuraCyan
import com.mhurston.ascendant.ui.theme.DangerRed
import com.mhurston.ascendant.ui.theme.ManaPurple
import com.mhurston.ascendant.ui.theme.TextDim
import com.mhurston.ascendant.ui.theme.XpGold
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    state: UiState,
    onAddReps: (ExerciseKind, Int) -> Unit,
    onAddMiles: (Double) -> Unit,
    onQuickLog: () -> Unit,
    onResetToday: () -> Unit,
    onToggleFavVideo: (String) -> Unit,
    onAddUserVideo: (String, String, String) -> Unit,
    onSetMood: (Int) -> Unit = {},
    onSetNotes: (String) -> Unit = {},
    onAddCustomReps: (String, Int) -> Unit = { _, _ -> },
    onAddCustomExercise: (String) -> Unit = {},
    onRemoveCustomExercise: (String) -> Unit = {},
    onAddPushVariant: (String, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val c = state.character
    val today = state.today
    val completionPct = (state.todayDerived.completion * 100).roundToInt()
    var confirmReset by remember { mutableStateOf(false) }
    var videoFor by remember { mutableStateOf<String?>(null) }

    videoFor?.let { key ->
        VideoDialog(
            exerciseKey = key,
            state = state,
            onToggleFav = onToggleFavVideo,
            onAddUrl = onAddUserVideo,
            onDismiss = { videoFor = null }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset today?") },
            text = { Text("This clears everything you logged today back to zero. " +
                "Your level, history, and other days are unaffected.") },
            confirmButton = {
                TextButton(onClick = { onResetToday(); confirmReset = false }) {
                    Text("Reset", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("TRAINING", style = MaterialTheme.typography.headlineMedium, color = ManaPurple)
                Text(c.title, style = MaterialTheme.typography.labelMedium, color = TextDim)
            }
            RankBadge(c.rank, c.level)
        }

        Spacer(Modifier.height(16.dp))
        XpBar(into = c.xpIntoLevel, forNext = c.xpForNextLevel, level = c.level)

        if (c.idlePenaltyXp > 0) {
            Spacer(Modifier.height(12.dp))
            DecayBanner(idleDays = c.idleDays, penalty = c.idlePenaltyXp)
        }

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            CompletionRing(
                completion = state.todayDerived.completion,
                centerLabel = "$completionPct%",
                centerSub = "today  ·  +${state.todayDerived.xp} XP"
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakChip("🔥 Strength", c.strengthStreak, Modifier.weight(1f))
            StreakChip("⚡ Activity", c.activityStreak, Modifier.weight(1f))
            StreakChip("★ Perfect", c.perfectStreak, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        QuestSection(state)

        Spacer(Modifier.height(20.dp))
        Text("Today's Training", style = MaterialTheme.typography.titleLarge, color = AuraCyan)
        Text("100 is the goal — going over earns bonus XP (Overdrive).",
            style = MaterialTheme.typography.labelMedium, color = TextDim)
        Spacer(Modifier.height(8.dp))

        PushUpsSection(
            breakdown = today.pushBreakdown(),
            total = today.pushTotal(),
            onVideos = { videoFor = "pushups" },
            onAddVariant = onAddPushVariant
        )
        ExerciseRow("Squats", today.squats, { videoFor = "squats" }) { onAddReps(ExerciseKind.SQUATS, it) }
        ExerciseRow("Leg Lifts", today.legLifts, { videoFor = "leglifts" }) { onAddReps(ExerciseKind.LEG_LIFTS, it) }
        ExerciseRow("Calf Raises", today.calfRaises, { videoFor = "calfraises" }) { onAddReps(ExerciseKind.CALF_RAISES, it) }
        ExerciseRow("Curls", today.curls, { videoFor = "curls" }) { onAddReps(ExerciseKind.CURLS, it) }
        WalkingRow(today.miles, { videoFor = "walking" }, onAddMiles)

        Spacer(Modifier.height(20.dp))
        CustomExerciseSection(
            customExercises = state.customExercises,
            todayReps = com.mhurston.ascendant.data.WorkoutDayEntity.decodeCustomReps(today.customReps),
            onAddReps = onAddCustomReps,
            onAddExercise = onAddCustomExercise,
            onRemoveExercise = onRemoveCustomExercise
        )

        Spacer(Modifier.height(20.dp))
        Text("Today's Journal", style = MaterialTheme.typography.titleLarge, color = AuraCyan)
        Spacer(Modifier.height(8.dp))
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            JournalSection(
                dateKey = today.date,
                mood = today.mood,
                notes = today.notes,
                onMood = onSetMood,
                onNotes = onSetNotes,
                modifier = Modifier.padding(14.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onQuickLog,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ManaPurple)
        ) { Text("⚡ Quick-Log Full Day", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { confirmReset = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
        ) { Text("Reset Today") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuestSection(state: UiState) {
    val daily = state.quests.filter { it.cadence == com.mhurston.ascendant.domain.Cadence.DAILY }
    val weekly = state.quests.filter { it.cadence == com.mhurston.ascendant.domain.Cadence.WEEKLY }
    val allDailyClear = daily.isNotEmpty() && daily.all { it.done }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Quests", style = MaterialTheme.typography.titleLarge, color = AuraCyan)
        if (allDailyClear) Text("ALL CLEAR ✓ +100 XP", color = XpGold,
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(8.dp))
    daily.forEach { QuestRow(it) }
    Spacer(Modifier.height(6.dp))
    Text("This Week", style = MaterialTheme.typography.labelMedium, color = TextDim)
    weekly.forEach { QuestRow(it) }
}

@Composable
private fun QuestRow(q: com.mhurston.ascendant.domain.Quest) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text((if (q.done) "✓ " else "") + q.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (q.done) XpGold else MaterialTheme.colorScheme.onSurface)
                Text("+${q.xpReward}", style = MaterialTheme.typography.labelMedium,
                    color = if (q.done) XpGold else TextDim, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            ProgressTrack(fraction = q.progress, color = if (q.done) XpGold else AuraCyan)
            Text("${q.current} / ${q.target}", style = MaterialTheme.typography.labelMedium, color = TextDim)
        }
    }
}

@Composable
private fun DecayBanner(idleDays: Int, penalty: Long) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.14f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("⚠ Inactivity decay active", color = DangerRed, fontWeight = FontWeight.Bold)
            Text("$idleDays idle day(s) → −$penalty XP. Log anything today to stop the bleed.",
                style = MaterialTheme.typography.labelMedium, color = TextDim)
        }
    }
}

@Composable
private fun StreakChip(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$value", style = MaterialTheme.typography.titleLarge, color = XpGold,
                fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextDim)
        }
    }
}

@Composable
private fun ExerciseRow(name: String, current: Int, onVideos: () -> Unit, onAdd: (Int) -> Unit) {
    val target = Progression.REP_TARGET
    val over = current - target
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    Text("  ▶ form", color = AuraCyan,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable(onClick = onVideos))
                }
                Row {
                    if (over > 0) Text("OVERDRIVE +$over  ", color = XpGold,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("$current / $target", color = if (current >= target) AuraCyan else TextDim,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            ProgressTrack(
                fraction = (current.toFloat() / target).coerceIn(0f, 1f),
                color = if (over > 0) XpGold else ManaPurple
            )
            Spacer(Modifier.height(8.dp))
            RepControls(current = current, onAdd = onAdd)
        }
    }
}

/** Push-ups goal, satisfied by any of the equivalent exercises. Each variant has its own
 *  controls; all reps sum 1:1 toward the single rep target shown in the header. */
@Composable
private fun PushUpsSection(
    breakdown: Map<String, Int>,
    total: Int,
    onVideos: () -> Unit,
    onAddVariant: (String, Int) -> Unit
) {
    val target = Progression.REP_TARGET
    val over = total - target
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Push-ups", style = MaterialTheme.typography.bodyLarge)
                    Text("  ▶ form", color = AuraCyan,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable(onClick = onVideos))
                }
                Row {
                    if (over > 0) Text("OVERDRIVE +$over  ", color = XpGold,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("$total / $target", color = if (total >= target) AuraCyan else TextDim,
                        fontWeight = FontWeight.Bold)
                }
            }
            Text("Log any of these — they all count toward the goal.",
                style = MaterialTheme.typography.labelMedium, color = TextDim)
            Spacer(Modifier.height(6.dp))
            ProgressTrack(
                fraction = (total.toFloat() / target).coerceIn(0f, 1f),
                color = if (over > 0) XpGold else ManaPurple
            )
            com.mhurston.ascendant.domain.PushExercise.entries.forEach { v ->
                val reps = breakdown[v.id] ?: 0
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(v.label, style = MaterialTheme.typography.bodyMedium,
                        color = if (reps > 0) MaterialTheme.colorScheme.onSurface else TextDim)
                    Text("$reps", color = if (reps > 0) AuraCyan else TextDim,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                RepControls(current = reps, onAdd = { onAddVariant(v.id, it) })
            }
        }
    }
}

@Composable
private fun WalkingRow(miles: Double, onVideos: () -> Unit, onAdd: (Double) -> Unit) {
    val over = miles - Progression.MILE_TARGET
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Walking", style = MaterialTheme.typography.bodyLarge)
                    Text("  ▶ form", color = AuraCyan,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable(onClick = onVideos))
                }
                Row {
                    if (over > 0) Text("OVERDRIVE +${"%.1f".format(over)}mi  ", color = XpGold,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("${"%.1f".format(miles)} / 5.0 mi",
                        color = if (miles >= Progression.MILE_TARGET) AuraCyan else TextDim,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            ProgressTrack(
                fraction = (miles / Progression.MILE_TARGET).coerceIn(0.0, 1.0).toFloat(),
                color = if (over > 0) XpGold else AuraCyan
            )
            Spacer(Modifier.height(8.dp))
            MileControls(miles = miles, onAdd = onAdd)
        }
    }
}

@Composable
private fun CustomExerciseSection(
    customExercises: List<com.mhurston.ascendant.domain.CustomExercise>,
    todayReps: Map<String, Int>,
    onAddReps: (String, Int) -> Unit,
    onAddExercise: (String) -> Unit,
    onRemoveExercise: (String) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<com.mhurston.ascendant.domain.CustomExercise?>(null) }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
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
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = { onAddExercise(name.trim()); showAdd = false }
                ) { Text("Add", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }

    removing?.let { ex ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove \"${ex.name}\"?") },
            text = { Text("Removes it from today's options. Days you already logged it keep " +
                "their entry (visible in the Log), and the XP you earned stays.") },
            confirmButton = {
                TextButton(onClick = { onRemoveExercise(ex.id); removing = null }) {
                    Text("Remove", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } }
        )
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text("Custom Exercises", style = MaterialTheme.typography.titleLarge, color = AuraCyan)
        Text("＋ Add", color = ManaPurple, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showAdd = true })
    }
    Text("Side work that earns bonus XP — your tuned core stays untouched.",
        style = MaterialTheme.typography.labelMedium, color = TextDim)
    Spacer(Modifier.height(8.dp))

    if (customExercises.isEmpty()) {
        Text("No custom exercises yet. Tap ＋ Add to track extras like pull-ups or planks.",
            style = MaterialTheme.typography.bodyLarge, color = TextDim)
    } else {
        customExercises.forEach { ex ->
            val reps = todayReps[ex.id] ?: 0
            val bonus = Progression.customBonusXp(
                com.mhurston.ascendant.domain.DayData(java.time.LocalDate.now(), customReps = mapOf(ex.id to reps))
            )
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (reps > 0) Text("+$bonus XP  ", color = XpGold,
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("$reps", color = if (reps > 0) AuraCyan else TextDim,
                                fontWeight = FontWeight.Bold)
                            Text("  ✕", color = DangerRed, style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.clickable { removing = ex })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    RepControls(
                        current = reps,
                        onAdd = { onAddReps(ex.id, it) }
                    )
                }
            }
        }
    }
}

/** Recording controls: +10 / −10, a free-text custom entry, and a reset. */
@Composable
private fun RepControls(current: Int, onAdd: (Int) -> Unit) {
    var custom by remember { mutableStateOf("") }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AddBtn("+10", Modifier.weight(1f)) { onAdd(10) }
            AddBtn("−10", Modifier.weight(1f)) { onAdd(-10) }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberField(custom, { custom = it }, "Enter reps", modifier = Modifier.weight(1.4f))
            AddBtn("Add", Modifier.weight(1f)) {
                custom.toIntOrNull()?.let { if (it != 0) onAdd(it) }
                custom = ""
            }
            AddBtn("Reset", Modifier.weight(1f), contentColor = DangerRed) {
                if (current != 0) onAdd(-current)
            }
        }
    }
}

/** Walking variant: decimal entry for miles. */
@Composable
private fun MileControls(miles: Double, onAdd: (Double) -> Unit) {
    var custom by remember { mutableStateOf("") }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AddBtn("+0.5", Modifier.weight(1f)) { onAdd(0.5) }
            AddBtn("−0.5", Modifier.weight(1f)) { onAdd(-0.5) }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberField(custom, { custom = it }, "Miles", decimal = true, modifier = Modifier.weight(1.4f))
            AddBtn("Add", Modifier.weight(1f)) {
                custom.toDoubleOrNull()?.let { if (it != 0.0) onAdd(it) }
                custom = ""
            }
            AddBtn("Reset", Modifier.weight(1f), contentColor = DangerRed) {
                if (miles != 0.0) onAdd(-miles)
            }
        }
    }
}

/** Compact bordered numeric entry — type any value (e.g. "12") and tap Add. */
@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(input.filter { it.isDigit() || (decimal && it == '.') }.take(6))
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = AuraCyan),
        cursorBrush = SolidColor(AuraCyan),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, TextDim.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = TextDim, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                }
                inner()
            }
        }
    )
}

@Composable
private fun AddBtn(
    label: String,
    modifier: Modifier = Modifier,
    contentColor: Color = AuraCyan,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
    ) { Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge) }
}
