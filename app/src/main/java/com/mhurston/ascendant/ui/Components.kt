package com.mhurston.ascendant.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mhurston.ascendant.domain.Rank
import com.mhurston.ascendant.ui.theme.AuraCyan
import com.mhurston.ascendant.ui.theme.ManaPurple
import com.mhurston.ascendant.ui.theme.TextDim
import com.mhurston.ascendant.ui.theme.XpGold
import kotlin.math.max

// Mood scale 1..5, lowest → highest. Index 0 unused (0 means "unset").
val MOOD_EMOJI = listOf("😫", "😕", "😐", "🙂", "🔥")
val MOOD_LABEL = listOf("Drained", "Rough", "Okay", "Good", "Unstoppable")

/**
 * Journal block: a 1..5 mood selector plus a free-text note. Tapping the already
 * selected mood clears it (sends 0). Notes are kept in local state (keyed by [dateKey])
 * so per-keystroke persistence doesn't clobber the cursor.
 */
@Composable
fun JournalSection(
    dateKey: String,
    mood: Int,
    notes: String,
    onMood: (Int) -> Unit,
    onNotes: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text("How did it feel?", style = MaterialTheme.typography.labelMedium, color = TextDim)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..5) {
                val selected = mood == i
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AuraCyan.copy(alpha = 0.20f) else Color(0xFF1B1B2A))
                        .border(
                            1.dp,
                            if (selected) AuraCyan else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onMood(if (selected) 0 else i) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(MOOD_EMOJI[i - 1], style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        if (mood in 1..5) {
            Spacer(Modifier.height(4.dp))
            Text(MOOD_LABEL[mood - 1], style = MaterialTheme.typography.labelMedium, color = AuraCyan)
        }
        Spacer(Modifier.height(10.dp))
        var text by remember(dateKey) { mutableStateOf(notes) }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(500); onNotes(text) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notes (optional)") },
            placeholder = { Text("PRs, how you felt, what you changed…") },
            minLines = 2,
            maxLines = 4
        )
    }
}

@Composable
fun LabeledBar(
    label: String,
    value: Int,
    maxValue: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("$value", style = MaterialTheme.typography.labelMedium, color = color,
                fontWeight = FontWeight.Bold)
        }
        ProgressTrack(
            fraction = if (maxValue <= 0) 0f else (value.toFloat() / maxValue).coerceIn(0f, 1f),
            color = color
        )
    }
}

@Composable
fun ProgressTrack(fraction: Float, color: Color, height: Dp = 10.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF22223A))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f))))
        )
    }
}

@Composable
fun XpBar(into: Long, forNext: Long, level: Int) {
    val frac = if (forNext <= 0) 0f else (into.toFloat() / forNext).coerceIn(0f, 1f)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("LV $level", style = MaterialTheme.typography.labelMedium, color = XpGold,
                fontWeight = FontWeight.Bold)
            Text("$into / $forNext XP", style = MaterialTheme.typography.labelMedium, color = TextDim)
        }
        ProgressTrack(fraction = frac, color = XpGold, height = 12.dp)
    }
}

/** Circular completion ring drawn on a Canvas. completion can exceed 1.0 (overdrive). */
@Composable
fun CompletionRing(
    completion: Double,
    size: Dp = 180.dp,
    centerLabel: String,
    centerSub: String
) {
    val frac = completion.coerceIn(0.0, 1.0).toFloat()
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val stroke = 22f
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            // Track
            drawArc(
                color = Color(0xFF22223A),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                brush = Brush.sweepGradient(listOf(ManaPurple, AuraCyan, XpGold, ManaPurple)),
                startAngle = -90f,
                sweepAngle = 360f * frac,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerLabel, style = MaterialTheme.typography.headlineMedium, color = AuraCyan)
            Text(centerSub, style = MaterialTheme.typography.labelMedium, color = TextDim)
        }
    }
}

@Composable
fun RankBadge(rank: Rank, level: Int) {
    val color = when (rank) {
        Rank.E -> Color(0xFF8A8A99)
        Rank.D -> Color(0xFF59C36A)
        Rank.C -> AuraCyan
        Rank.B -> Color(0xFF4D9BFF)
        Rank.A -> ManaPurple
        Rank.S -> XpGold
        Rank.SS -> Color(0xFFFF8A3D)
        Rank.NATIONAL -> Color(0xFFFF5470)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("RANK", style = MaterialTheme.typography.labelMedium, color = TextDim)
            Text(rank.label, color = color, fontWeight = FontWeight.Black,
                fontFamily = com.mhurston.ascendant.ui.theme.Orbitron,
                style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Suggests a sensible bar max for a stat so the bars look meaningful. */
fun statBarMax(value: Int): Int = max(10, ((value / 10) + 1) * 10)
