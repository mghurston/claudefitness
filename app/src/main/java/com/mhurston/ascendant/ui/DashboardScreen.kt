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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    onAddCoreVariant: (String, Int) -> Unit = { _, _ -> },
    onAddCardioMinutes: (String, Int) -> Unit = { _, _ -> },
    onAddOneOff: (String, Int) -> Unit = { _, _ -> },
    onRemoveOneOff: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = state.character
    val today = state.today
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakChip("🔥 Strength", c.strengthStreak, Modifier.weight(1f))
            StreakChip("⚡ Activity", c.activityStreak, Modifier.weight(1f))
            StreakChip("★ Perfect", c.perfectStreak, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        QuestSection(state)

        Spacer(Modifier.height(20.dp))
        Text("Today's Training", style = MaterialTheme.typography.titleLarge, color = AuraCyan)
        Text("100 is the goal — every rep and mile burns calories, and calories are XP. " +
            "Tap a group to log it.",
            style = MaterialTheme.typography.labelMedium, color = TextDim)
        Spacer(Modifier.height(4.dp))

        val upperReps = today.pushTotal() + today.curls
        CollapsibleSection("Upper Body", "$upperReps / 200") {
            VariantGoalSection(
                name = "Push-ups",
                variants = com.mhurston.ascendant.domain.PushExercise.entries.map { it.id to it.label },
                breakdown = today.pushBreakdown(),
                total = today.pushTotal(),
                onVideos = { videoFor = "pushups" },
                onAddVariant = onAddPushVariant
            )
            ExerciseRow("Curls", today.curls, { videoFor = "curls" }) { onAddReps(ExerciseKind.CURLS, it) }
        }
        CollapsibleSection("Core", "${today.coreTotal()} / 100") {
            VariantGoalSection(
                name = "Core",
                variants = com.mhurston.ascendant.domain.CoreExercise.entries.map { it.id to it.label },
                breakdown = today.coreBreakdown(),
                total = today.coreTotal(),
                onVideos = { videoFor = "leglifts" },
                onAddVariant = onAddCoreVariant
            )
        }
        val lowerReps = today.squats + today.calfRaises
        CollapsibleSection("Lower Body", "$lowerReps / 200") {
            ExerciseRow("Squats", today.squats, { videoFor = "squats" }) { onAddReps(ExerciseKind.SQUATS, it) }
            ExerciseRow("Calf Raises", today.calfRaises, { videoFor = "calfraises" }) { onAddReps(ExerciseKind.CALF_RAISES, it) }
        }
        CollapsibleSection("Cardio", "%.1f / 5.0 mi".format(today.walkMiles)) {
            WalkingRow(
                treadmillMiles = today.miles,
                trackedMiles = today.trackedMiles,
                steps = today.passiveSteps,
                onVideos = { videoFor = "walking" },
                onAdd = onAddMiles
            )
            val cardioMin = com.mhurston.ascendant.data.WorkoutDayEntity.decodeCustomReps(today.cardioMinutes)
            com.mhurston.ascendant.domain.CardioActivity.entries.forEach { act ->
                CardioMinutesRow(
                    label = act.label,
                    minutes = cardioMin[act.id] ?: 0,
                    kcalPerMin = act.met * 3.5 * state.profile.weightKg / 200.0,
                    onAdd = { onAddCardioMinutes(act.id, it) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        ExtrasSection(
            customExercises = state.customExercises,
            todayReps = com.mhurston.ascendant.data.WorkoutDayEntity.decodeCustomReps(today.customReps),
            oneOffs = com.mhurston.ascendant.data.WorkoutDayEntity.decodeOneOffs(today.oneOffs),
            weightKg = state.profile.weightKg,
            onAddReps = onAddCustomReps,
            onAddExercise = onAddCustomExercise,
            onRemoveExercise = onRemoveCustomExercise,
            onAddOneOff = onAddOneOff,
            onRemoveOneOff = onRemoveOneOff
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
    val dailyDone = daily.count { it.done }
    val weeklyDone = weekly.count { it.done }
    val allDailyClear = daily.isNotEmpty() && daily.all { it.done }

    CollapsibleSection(
        title = "Daily Quests",
        summary = if (allDailyClear) "ALL CLEAR ✓ +100 XP" else "$dailyDone / ${daily.size}",
        defaultExpanded = false
    ) {
        Text("Bonus goals layered on today's training — hit them for extra XP.",
            style = MaterialTheme.typography.labelMedium, color = TextDim)
        Spacer(Modifier.height(6.dp))
        daily.forEach { QuestRow(it) }
    }
    CollapsibleSection(
        title = "Weekly Quests",
        summary = "$weeklyDone / ${weekly.size}",
        defaultExpanded = false
    ) {
        weekly.forEach { QuestRow(it) }
    }
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
            Text(q.desc, style = MaterialTheme.typography.labelMedium, color = TextDim)
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
                    Spacer(Modifier.width(8.dp))
                    FormVideoChip(onVideos)
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

/** A single rep goal satisfied by any of several equivalent exercises (e.g. Push-ups, or Core).
 *  Each variant has its own controls; all reps sum 1:1 toward the single target in the header. */
@Composable
private fun VariantGoalSection(
    name: String,
    variants: List<Pair<String, String>>, // id to label
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
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    FormVideoChip(onVideos)
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
            variants.forEach { (id, label) ->
                val reps = breakdown[id] ?: 0
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodyMedium,
                        color = if (reps > 0) MaterialTheme.colorScheme.onSurface else TextDim)
                    Text("$reps", color = if (reps > 0) AuraCyan else TextDim,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                RepControls(current = reps, onAdd = { onAddVariant(id, it) })
            }
        }
    }
}

/** Time-based cardio (bike/swim): logged in minutes, shown with its calorie/XP estimate.
 *  Does not count toward the walking-miles goal — it's its own calorie earner. */
@Composable
private fun CardioMinutesRow(label: String, minutes: Int, kcalPerMin: Double, onAdd: (Int) -> Unit) {
    val kcal = (kcalPerMin * minutes).roundToInt()
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (minutes > 0) Text("≈$kcal XP  ", color = XpGold,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("$minutes min", color = if (minutes > 0) AuraCyan else TextDim,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            MinuteControls(minutes = minutes, onAdd = onAdd)
        }
    }
}

/** +15 / −15 minute steps, a free-text minutes entry, and a reset. */
@Composable
private fun MinuteControls(minutes: Int, onAdd: (Int) -> Unit) {
    var custom by remember { mutableStateOf("") }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AddBtn("+15 min", Modifier.weight(1f)) { onAdd(15) }
            AddBtn("−15 min", Modifier.weight(1f)) { onAdd(-15) }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberField(custom, { custom = it }, "Minutes", modifier = Modifier.weight(1.4f))
            AddBtn("Add", Modifier.weight(1f)) {
                custom.toIntOrNull()?.let { if (it != 0) onAdd(it) }
                custom = ""
            }
            AddBtn("Reset", Modifier.weight(1f), contentColor = DangerRed) {
                if (minutes != 0) onAdd(-minutes)
            }
        }
    }
}

/** Walking = cumulative total of two sources toward the 5-mile goal:
 *  - "Tracked" — estimated from Health Connect steps (~2000 steps/mi), read-only.
 *  - "Treadmill / manual" — logged by hand for walking done off-phone (e.g. a treadmill).
 *  Only the manual part is editable; the header shows the combined total + goal. */
@Composable
private fun WalkingRow(
    treadmillMiles: Double,
    trackedMiles: Double,
    steps: Int,
    onVideos: () -> Unit,
    onAdd: (Double) -> Unit
) {
    val total = treadmillMiles + trackedMiles
    val over = total - Progression.MILE_TARGET
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Walking", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    FormVideoChip(onVideos)
                }
                Row {
                    if (over > 0) Text("OVERDRIVE +${"%.1f".format(over)}mi  ", color = XpGold,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("${"%.1f".format(total)} / 5.0 mi",
                        color = if (total >= Progression.MILE_TARGET) AuraCyan else TextDim,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            ProgressTrack(
                fraction = (total / Progression.MILE_TARGET).coerceIn(0.0, 1.0).toFloat(),
                color = if (over > 0) XpGold else AuraCyan
            )

            // Breakdown: tracked (steps) appears once anything has synced.
            if (steps > 0) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Tracked (steps)", style = MaterialTheme.typography.bodyMedium)
                    Text("${"%.1f".format(trackedMiles)} mi  ·  ${"%,d".format(steps)} steps",
                        color = AuraCyan, fontWeight = FontWeight.Bold)
                }
                Text("Auto from Health Connect — no phone needed for the treadmill, add that below.",
                    style = MaterialTheme.typography.labelMedium, color = TextDim)
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Treadmill / manual", style = MaterialTheme.typography.bodyMedium,
                    color = if (treadmillMiles > 0) MaterialTheme.colorScheme.onSurface else TextDim)
                Text("${"%.1f".format(treadmillMiles)} mi",
                    color = if (treadmillMiles > 0) AuraCyan else TextDim, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            MileControls(miles = treadmillMiles, onAdd = onAdd)
        }
    }
}

/** A tappable group header that expands/collapses its content. Keeps the Train tab short:
 *  body-part groups are collapsed by default with a one-line summary, tap to log. */
@Composable
private fun CollapsibleSection(
    title: String,
    summary: String = "",
    defaultExpanded: Boolean = false,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable(key = title) { mutableStateOf(defaultExpanded) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (expanded) "▾" else "▸", color = AuraCyan, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = AuraCyan,
                modifier = Modifier.weight(1f))
            if (summary.isNotEmpty()) Text(summary, style = MaterialTheme.typography.labelMedium,
                color = TextDim, fontWeight = FontWeight.Bold)
            action?.let { Spacer(Modifier.width(12.dp)); it() }
        }
        if (expanded) content()
    }
}

/** A small, obvious tappable pill for opening the form-video dialog. */
@Composable
private fun FormVideoChip(onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, AuraCyan.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text("▶ Videos", color = AuraCyan, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold)
    }
}

/** Extras: pinned recurring custom exercises (rep-based) + per-day one-off activities
 *  (calorie-based). One-offs live only on today; pinning promotes one to a daily option. */
@Composable
private fun ExtrasSection(
    customExercises: List<com.mhurston.ascendant.domain.CustomExercise>,
    todayReps: Map<String, Int>,
    oneOffs: List<com.mhurston.ascendant.domain.OneOff>,
    weightKg: Double,
    onAddReps: (String, Int) -> Unit,
    onAddExercise: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onAddOneOff: (String, Int) -> Unit,
    onRemoveOneOff: (Int) -> Unit
) {
    var showOneOff by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<com.mhurston.ascendant.domain.CustomExercise?>(null) }

    if (showOneOff) {
        OneOffDialog(
            onAdd = { name, kcal, pin ->
                onAddOneOff(name, kcal)
                if (pin) onAddExercise(name)
                showOneOff = false
            },
            onDismiss = { showOneOff = false }
        )
    }

    removing?.let { ex ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Unpin \"${ex.name}\"?") },
            text = { Text("Removes it from your daily options. Days you already logged it keep " +
                "their entry (visible in the Log), and the XP you earned stays.") },
            confirmButton = {
                TextButton(onClick = { onRemoveExercise(ex.id); removing = null }) {
                    Text("Unpin", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } }
        )
    }

    CollapsibleSection(
        title = "Extra Work",
        summary = "${oneOffs.size + customExercises.size}",
        action = {
            Text("＋ One-off", color = ManaPurple, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showOneOff = true })
        }
    ) {
    Text("One-offs (a run, a class) are logged to today only. Pin one to make it a daily option.",
        style = MaterialTheme.typography.labelMedium, color = TextDim)
    Spacer(Modifier.height(8.dp))

    // Today's one-off activities — each lives only on this day.
    oneOffs.forEachIndexed { i, o ->
        Card(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(o.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("+${o.kcal} XP", color = XpGold, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold)
                Text("  ✕", color = DangerRed, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { onRemoveOneOff(i) })
            }
        }
    }

    // Pinned recurring custom exercises — rep-based, available every day.
    customExercises.forEach { ex ->
        val reps = todayReps[ex.id] ?: 0
        val kcal = com.mhurston.ascendant.domain.Calories.strengthKcal(weightKg, reps).roundToInt()
        Card(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                        Text("  📌", style = MaterialTheme.typography.labelMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (reps > 0) Text("≈$kcal XP  ", color = XpGold,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("$reps", color = if (reps > 0) AuraCyan else TextDim,
                            fontWeight = FontWeight.Bold)
                        Text("  ✕", color = DangerRed, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clickable { removing = ex })
                    }
                }
                Spacer(Modifier.height(8.dp))
                RepControls(current = reps, onAdd = { onAddReps(ex.id, it) })
            }
        }
    }

    if (oneOffs.isEmpty() && customExercises.isEmpty()) {
        Text("Nothing extra yet. Tap ＋ One-off to log something like a marathon or a yoga class.",
            style = MaterialTheme.typography.bodyLarge, color = TextDim)
    }
    }
}

/** Name + calorie entry for a one-off activity, with an optional "pin as daily option". */
@Composable
internal fun OneOffDialog(onAdd: (String, Int, Boolean) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log a one-off") },
        text = {
            Column {
                Text("A single activity for today — e.g. \"Marathon\" or \"Spin class\". " +
                    "Your calorie estimate becomes XP (1 kcal = 1 XP).",
                    style = MaterialTheme.typography.labelMedium, color = TextDim)
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40); nameError = false },
                    singleLine = true,
                    isError = nameError,
                    label = { Text("Activity name (required)") },
                    supportingText = if (nameError) {
                        { Text("Enter a name to save this activity", color = DangerRed) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = kcal,
                    onValueChange = { v -> kcal = v.filter { it.isDigit() }.take(5) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Calories burned (estimate)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { pin = !pin }) {
                    Text(if (pin) "☑" else "☐", color = ManaPurple,
                        style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(8.dp))
                    Text("Also pin as a daily option (recurring)",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) nameError = true
                    else onAdd(name.trim(), kcal.toIntOrNull() ?: 0, pin)
                }
            ) { Text("Add", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
