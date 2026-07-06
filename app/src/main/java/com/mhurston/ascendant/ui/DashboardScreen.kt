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
    onSetNotes: (String) -> Unit = {},
    onAddCustomReps: (String, Int) -> Unit = { _, _ -> },
    onAddCustomExercise: (String) -> Unit = {},
    onRemoveCustomExercise: (String) -> Unit = {},
    onAddPushVariant: (String, Int) -> Unit = { _, _ -> },
    onAddCoreVariant: (String, Int) -> Unit = { _, _ -> },
    onAddCardioMinutes: (String, Int) -> Unit = { _, _ -> },
    onAddOneOff: (com.mhurston.ascendant.domain.OneOff) -> Unit = {},
    onUpdateOneOff: (Int, com.mhurston.ascendant.domain.OneOff) -> Unit = { _, _ -> },
    onRemoveOneOff: (Int) -> Unit = {},
    unitSystem: com.mhurston.ascendant.domain.UnitSystem = com.mhurston.ascendant.domain.UnitSystem.IMPERIAL,
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
                "Steps synced from Health Connect are kept. " +
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
                ScreenTitle("Training")
                ScreenSubtitle(c.title)
            }
            RankBadge(c.rank, c.level)
        }

        Spacer(Modifier.height(16.dp))
        XpBar(into = c.xpIntoLevel, forNext = c.xpForNextLevel, level = c.level)

        // Only nudge about the live, still-growing trailing gap — past (interior) decay is already
        // locked in and unstoppable, so showing "log today to stop it" for that would be misleading.
        if (c.trailingPenaltyXp > 0) {
            Spacer(Modifier.height(12.dp))
            // Rate is derived from the actual charge, so per-day × days always equals the total.
            DecayBanner(
                idleDays = c.trailingChargedDays,
                penalty = c.trailingPenaltyXp,
                perDay = c.trailingPenaltyXp / c.trailingChargedDays.coerceAtLeast(1)
            )
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
        SectionHeader("Today's Training")
        Caption("100 is the goal — every rep and mile burns calories, and calories are XP. " +
            "Tap a group to log it.")
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
        CollapsibleSection("Cardio", "%.1f / 5.0 mi".format(java.util.Locale.US, today.walkMiles)) {
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
            unitSystem = unitSystem,
            onAddReps = onAddCustomReps,
            onAddExercise = onAddCustomExercise,
            onRemoveExercise = onRemoveCustomExercise,
            onAddOneOff = onAddOneOff,
            onUpdateOneOff = onUpdateOneOff,
            onRemoveOneOff = onRemoveOneOff
        )

        Spacer(Modifier.height(20.dp))
        SectionHeader("Today's Journal")
        Spacer(Modifier.height(8.dp))
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            JournalSection(
                dateKey = today.date,
                notes = today.notes,
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
        summary = if (allDailyClear) "ALL CLEAR ✓" else "$dailyDone / ${daily.size}",
        defaultExpanded = false
    ) {
        Caption("Goals layered on today's training. XP comes from calories — these are badges.")
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
                if (q.done) Text("DONE", style = MaterialTheme.typography.labelMedium,
                    color = XpGold, fontWeight = FontWeight.Bold)
            }
            Caption(q.desc)
            Spacer(Modifier.height(6.dp))
            ProgressTrack(fraction = q.progress, color = if (q.done) XpGold else AuraCyan)
            Caption("${q.current} / ${q.target}")
        }
    }
}

@Composable
private fun DecayBanner(idleDays: Int, penalty: Long, perDay: Long) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.14f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("⚠ You lose XP every day you don't train", color = DangerRed,
                fontWeight = FontWeight.Bold)
            Caption("A skipped day costs your daily burn target (−$perDay XP); a partial day " +
                "costs whatever's left of it. $idleDays skipped day(s) → −$penalty XP, gone for " +
                "good. Today isn't counted until midnight — log anything to stop the loss.")
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
            StatValue("$value")
            Caption(label)
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
                    BodyText(name)
                    Spacer(Modifier.width(8.dp))
                    FormVideoChip(onVideos)
                }
                Row {
                    if (over > 0) Text("OVERDRIVE +$over  ", color = XpGold,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("$current / $target", color = if (current >= target) XpGold else TextDim,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            ProgressTrack(
                fraction = (current.toFloat() / target).coerceIn(0f, 1f),
                color = if (current >= target) XpGold else ManaPurple
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
                    BodyText(name)
                    Spacer(Modifier.width(8.dp))
                    FormVideoChip(onVideos)
                }
                Row {
                    if (over > 0) Text("OVERDRIVE +$over  ", color = XpGold,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("$total / $target", color = if (total >= target) XpGold else TextDim,
                        fontWeight = FontWeight.Bold)
                }
            }
            Caption("Log any of these — they all count toward the goal.")
            Spacer(Modifier.height(6.dp))
            ProgressTrack(
                fraction = (total.toFloat() / target).coerceIn(0f, 1f),
                color = if (total >= target) XpGold else ManaPurple
            )
            variants.forEach { (id, label) ->
                val reps = breakdown[id] ?: 0
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    BodyText(label, color = if (reps > 0) MaterialTheme.colorScheme.onSurface else TextDim)
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
                BodyText(label)
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
                    BodyText("Walking")
                    Spacer(Modifier.width(8.dp))
                    FormVideoChip(onVideos)
                }
                Row {
                    if (over > 0) Text("OVERDRIVE +${"%.1f".format(java.util.Locale.US, over)}mi  ",
                        color = XpGold,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("${"%.1f".format(java.util.Locale.US, total)} / 5.0 mi",
                        color = if (total >= Progression.MILE_TARGET) XpGold else TextDim,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            ProgressTrack(
                fraction = (total / Progression.MILE_TARGET).coerceIn(0.0, 1.0).toFloat(),
                color = if (total >= Progression.MILE_TARGET) XpGold else AuraCyan
            )

            // Breakdown: tracked (steps) appears once anything has synced.
            if (steps > 0) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    BodyText("Tracked (steps)")
                    Text("${"%.1f".format(java.util.Locale.US, trackedMiles)} mi  ·  " +
                        "${"%,d".format(java.util.Locale.US, steps)} steps",
                        color = AuraCyan, fontWeight = FontWeight.Bold)
                }
                Caption("Auto from Health Connect.")
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                BodyText("Treadmill / manual",
                    color = if (treadmillMiles > 0) MaterialTheme.colorScheme.onSurface else TextDim)
                Text("${"%.1f".format(java.util.Locale.US, treadmillMiles)} mi",
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
            SectionHeader(title, modifier = Modifier.weight(1f))
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
    unitSystem: com.mhurston.ascendant.domain.UnitSystem,
    onAddReps: (String, Int) -> Unit,
    onAddExercise: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onAddOneOff: (com.mhurston.ascendant.domain.OneOff) -> Unit,
    onUpdateOneOff: (Int, com.mhurston.ascendant.domain.OneOff) -> Unit,
    onRemoveOneOff: (Int) -> Unit
) {
    var showOneOff by remember { mutableStateOf(false) }
    var editingOneOff by remember { mutableStateOf<Pair<Int, com.mhurston.ascendant.domain.OneOff>?>(null) }
    var removing by remember { mutableStateOf<com.mhurston.ascendant.domain.CustomExercise?>(null) }

    if (showOneOff) {
        OneOffDialog(
            weightKg = weightKg,
            unitSystem = unitSystem,
            onAdd = { oneOff, pin ->
                onAddOneOff(oneOff)
                if (pin) onAddExercise(oneOff.name)
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
        action = { AddLink("One-off") { showOneOff = true } }
    ) {
    Caption("One-offs (a run, a class) are logged to today only. Pin one to make it a daily option.")
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
                        BodyText(ex.name)
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
        BodyText("Nothing extra yet. Tap ＋ One-off to log something like a marathon or a yoga class.", color = TextDim)
    }
    }
}

/** Free-form one-off entry: name + any of reps / distance / calories. Reps and distance drive a
 *  live calorie estimate (same strength & walk/run/bike models the rest of the app uses); leave
 *  Calories blank to bank that estimate, or type a number to override it (e.g. a hard bike ride
 *  the moderate-pace estimate underrates). Calories are the XP source (1 kcal = 1 XP).
 *
 *  Pass [initial] to edit an existing entry: the fields are pre-filled (Calories shows the value
 *  it already banked), the title/button switch to "Edit"/"Save", and the pin option is hidden
 *  (pinning only applies to brand-new one-offs). */
@Composable
internal fun OneOffDialog(
    weightKg: Double,
    unitSystem: com.mhurston.ascendant.domain.UnitSystem = com.mhurston.ascendant.domain.UnitSystem.IMPERIAL,
    initial: com.mhurston.ascendant.domain.OneOff? = null,
    onAdd: (com.mhurston.ascendant.domain.OneOff, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val editing = initial != null
    val metric = unitSystem == com.mhurston.ascendant.domain.UnitSystem.METRIC
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var repsText by remember {
        mutableStateOf(initial?.reps?.takeIf { it > 0 }?.toString() ?: "")
    }
    // distText is in the user's units (mi or km); we convert to stored miles below.
    var distText by remember {
        mutableStateOf(
            initial?.distanceMi?.takeIf { it > 0.0 }?.let {
                val shown = if (metric) com.mhurston.ascendant.domain.Units.milesToKm(it) else it
                if (shown == Math.floor(shown)) shown.toInt().toString()
                else String.format(java.util.Locale.US, "%.2f", shown).trimEnd('0').trimEnd('.')
            } ?: ""
        )
    }
    // For an existing entry the stored kcal is the value it banked; surface it as the (editable)
    // override so the user sees and can change exactly what it earns.
    var kcalText by remember {
        mutableStateOf(initial?.kcal?.takeIf { it > 0 }?.toString() ?: "")
    }
    // Re-open with the activity the entry was logged as, so "use estimate" after an edit
    // recomputes with the right model (a run edited later must not re-estimate as a walk).
    var activity by remember {
        mutableStateOf(com.mhurston.ascendant.domain.DistanceActivity.forId(initial?.activityId ?: ""))
    }
    var pin by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    val reps = repsText.toIntOrNull() ?: 0
    val enteredDist = distText.toDoubleOrNull() ?: 0.0
    val miles = if (metric) com.mhurston.ascendant.domain.Units.kmToMiles(enteredDist) else enteredDist
    val estimate = Math.round(
        com.mhurston.ascendant.domain.Calories.strengthKcal(weightKg, reps) +
            com.mhurston.ascendant.domain.Calories.distanceKcal(weightKg, miles, activity.kcalPerKgPerMile)
    ).toInt()
    val manualKcal = kcalText.toIntOrNull()
    val finalKcal = manualKcal ?: estimate
    val hasMetrics = reps > 0 || miles > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing) "Edit one-off" else "Log a one-off") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Caption("A single activity for today — a bike ride, a lifting set, a class. " +
                    "Fill any fields; calories become XP (1 kcal = 1 XP).")
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
                    value = repsText,
                    onValueChange = { v -> repsText = v.filter { it.isDigit() }.take(5) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Reps (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = distText,
                    onValueChange = { v ->
                        // digits + a single decimal point
                        val cleaned = v.filter { it.isDigit() || it == '.' }
                        if (cleaned.count { it == '.' } <= 1) distText = cleaned.take(6)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("Distance in ${if (metric) "kilometers" else "miles"} (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (miles > 0.0) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.mhurston.ascendant.domain.DistanceActivity.entries.forEach { a ->
                            val selected = a == activity
                            Text(
                                a.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) ManaPurple else TextDim,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { activity = a }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = kcalText,
                    onValueChange = { v -> kcalText = v.filter { it.isDigit() }.take(5) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Calories — blank = use estimate") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (finalKcal <= 0 && manualKcal == null) {
                    Text("Enter reps, distance, or calories to earn XP",
                        color = TextDim, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                } else {
                    Text("Will bank +$finalKcal XP" + if (manualKcal != null) "  ·  manual" else "",
                        color = XpGold, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                    // Live estimate from the current reps/distance — updates as you edit. When a
                    // manual calorie value is overriding it, offer a one-tap way to adopt it.
                    if (manualKcal != null && hasMetrics && estimate > 0 && estimate != manualKcal) {
                        Spacer(Modifier.height(4.dp))
                        Text("↻ Use estimate from reps/distance: +$estimate XP",
                            color = ManaPurple, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clickable { kcalText = "" })
                    }
                }
                if (!editing) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { pin = !pin }) {
                        Text(if (pin) "☑" else "☐", color = ManaPurple,
                            style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        BodyText("Also pin as a daily option (recurring)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) nameError = true
                    else onAdd(
                        com.mhurston.ascendant.domain.OneOff(
                            name.trim(), finalKcal, miles, reps,
                            activityId = if (miles > 0.0) activity.name else ""
                        ),
                        pin
                    )
                }
            ) { Text(if (editing) "Save" else "Add", fontWeight = FontWeight.Bold) }
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
    ) { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
}
