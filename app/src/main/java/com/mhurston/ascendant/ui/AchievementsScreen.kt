package com.mhurston.ascendant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mhurston.ascendant.domain.AchStatus
import com.mhurston.ascendant.domain.Rarity
import com.mhurston.ascendant.ui.theme.AuraCyan
import com.mhurston.ascendant.ui.theme.ManaPurple
import com.mhurston.ascendant.ui.theme.TextDim
import com.mhurston.ascendant.ui.theme.XpGold

fun rarityColor(r: Rarity): Color = when (r) {
    Rarity.COMMON -> Color(0xFF9AA0A6)
    Rarity.UNCOMMON -> Color(0xFF3DDC84)
    Rarity.RARE -> AuraCyan
    Rarity.EPIC -> ManaPurple
    Rarity.LEGENDARY -> XpGold
    Rarity.MYTHIC -> Color(0xFFFF2D55)
}

@Composable
fun AchievementsScreen(state: UiState, modifier: Modifier = Modifier) {
    val achievements = state.achievements
    val unlocked = achievements.count { it.unlocked }
    val bonusXp = com.mhurston.ascendant.domain.Achievements.unlockedXp(achievements)
    // Unlocked first, then by progress descending.
    val sorted = achievements.sortedWith(
        compareByDescending<AchStatus> { it.unlocked }
            .thenByDescending { it.current.toFloat() / it.target.coerceAtLeast(1) }
    )

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Text("Achievements", style = MaterialTheme.typography.headlineMedium, color = ManaPurple)
        Text("$unlocked / ${achievements.size} unlocked · +$bonusXp bonus XP earned",
            style = MaterialTheme.typography.labelMedium, color = TextDim)
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(sorted, key = { it.def.id }) { AchievementRow(it) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private val EarnedGreen = Color(0xFF3DDC84)

@Composable
private fun AchievementRow(st: AchStatus) {
    val color = rarityColor(st.def.rarity)
    val locked = !st.unlocked
    val hiddenLocked = locked && st.def.hidden
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            // Earned rows glow faintly with their rarity tint so they clearly stand apart.
            containerColor = if (locked) MaterialTheme.colorScheme.surface
            else color.copy(alpha = 0.10f)
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Rarity medallion — earned medallions get a filled tint + colored ring.
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (locked) Color(0xFF22223A) else color.copy(alpha = 0.30f))
                    .then(
                        if (locked) Modifier
                        else Modifier.border(1.5.dp, color, RoundedCornerShape(10.dp))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (hiddenLocked) "?" else if (st.unlocked) "★" else "🔒",
                    color = if (locked) TextDim else color, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (hiddenLocked) "???" else st.def.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (locked) TextDim else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (hiddenLocked) "Hidden — keep training to discover it." else st.def.desc,
                    style = MaterialTheme.typography.labelMedium, color = TextDim
                )
                if (!st.unlocked && !st.def.hidden && st.target > 1) {
                    Spacer(Modifier.height(6.dp))
                    ProgressTrack(fraction = st.current.toFloat() / st.target, color = color)
                    Text("${st.current} / ${st.target}", style = MaterialTheme.typography.labelMedium,
                        color = TextDim)
                }
            }
            Spacer(Modifier.size(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (st.unlocked) {
                    Text("✓ EARNED", style = MaterialTheme.typography.labelMedium,
                        color = EarnedGreen, fontWeight = FontWeight.Bold)
                }
                Text(st.def.rarity.name, style = MaterialTheme.typography.labelMedium, color = color,
                    fontWeight = FontWeight.Bold)
                Text("+${st.def.rarity.xp}", style = MaterialTheme.typography.labelMedium, color = TextDim)
            }
        }
    }
}
