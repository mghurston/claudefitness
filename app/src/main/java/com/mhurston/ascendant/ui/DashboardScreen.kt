package com.mhurston.ascendant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
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
                Text("ASCENDANT", style = MaterialTheme.typography.headlineMedium, color = ManaPurple)
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

        ExerciseRow("Push-ups", today.pushups, { videoFor = "pushups" }) { onAddReps(ExerciseKind.PUSHUPS, it) }
        ExerciseRow("Squats", today.squats, { videoFor = "squats" }) { onAddReps(ExerciseKind.SQUATS, it) }
        ExerciseRow("Leg Lifts", today.legLifts, { videoFor = "leglifts" }) { onAddReps(ExerciseKind.LEG_LIFTS, it) }
        ExerciseRow("Calf Raises", today.calfRaises, { videoFor = "calfraises" }) { onAddReps(ExerciseKind.CALF_RAISES, it) }
        ExerciseRow("Curls", today.curls, { videoFor = "curls" }) { onAddReps(ExerciseKind.CURLS, it) }
        WalkingRow(today.miles, { videoFor = "walking" }, onAddMiles)

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AddBtn("+10") { onAdd(10) }
                AddBtn("+25") { onAdd(25) }
                AddBtn("+100") { onAdd(100) }
                AddBtn("−10") { onAdd(-10) }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AddBtn("+0.5") { onAdd(0.5) }
                AddBtn("+1.0") { onAdd(1.0) }
                AddBtn("−0.5") { onAdd(-0.5) }
            }
        }
    }
}

@Composable
private fun AddBtn(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraCyan)
    ) { Text(label) }
}
