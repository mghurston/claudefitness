package com.mhurston.ascendant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.mhurston.ascendant.R
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mhurston.ascendant.data.Exporter
import com.mhurston.ascendant.ui.theme.AuraCyan
import com.mhurston.ascendant.ui.theme.ManaPurple
import com.mhurston.ascendant.ui.theme.TextDim
import com.mhurston.ascendant.ui.theme.XpGold
import kotlin.math.roundToInt

@Composable
fun CharacterScreen(
    state: UiState,
    onImportJson: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    avatar: com.mhurston.ascendant.domain.Avatar = com.mhurston.ascendant.domain.Avatar.MALE,
    onSetAvatar: (com.mhurston.ascendant.domain.Avatar) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = state.character
    val s = c.stats
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("ASCENDANT", style = MaterialTheme.typography.headlineMedium, color = ManaPurple)
        Text("PROGRESS SHEET", style = MaterialTheme.typography.labelMedium, color = TextDim)
        Spacer(Modifier.height(16.dp))

        // Hero portrait — choose your character; the aura border follows your rank.
        val portrait = when (avatar) {
            com.mhurston.ascendant.domain.Avatar.FEMALE -> R.drawable.hero_portrait_female
            com.mhurston.ascendant.domain.Avatar.MALE_BLACK -> R.drawable.hero_portrait_male_black
            com.mhurston.ascendant.domain.Avatar.FEMALE_BLACK -> R.drawable.hero_portrait_female_black
            else -> R.drawable.hero_portrait
        }
        RankPortrait(portrait = portrait, rank = c.rank, level = c.level)
        Spacer(Modifier.height(10.dp))
        Text("Choose your character", style = MaterialTheme.typography.labelMedium, color = TextDim)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            com.mhurston.ascendant.domain.Avatar.entries.forEach { a ->
                AvatarChoice(a.label, avatar == a, Modifier.weight(1f)) { onSetAvatar(a) }
            }
        }
        Spacer(Modifier.height(8.dp))

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Level ${c.level}", style = MaterialTheme.typography.headlineMedium,
                        color = XpGold, fontWeight = FontWeight.Bold)
                    Text(c.title, style = MaterialTheme.typography.titleLarge, color = AuraCyan)
                    Text("${c.totalXp} XP" +
                        if (c.idlePenaltyXp > 0) "  (−${c.idlePenaltyXp} idle)" else "",
                        style = MaterialTheme.typography.labelMedium, color = TextDim)
                }
                RankBadge(c.rank, c.level)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Attributes", style = MaterialTheme.typography.titleLarge, color = AuraCyan)
        LabeledBar("STRENGTH", s.strength, statBarMax(s.strength), ManaPurple)
        LabeledBar("ENDURANCE", s.endurance, statBarMax(s.endurance), AuraCyan)
        LabeledBar("AGILITY", s.agility, statBarMax(s.agility), XpGold)
        LabeledBar("DISCIPLINE", s.discipline, statBarMax(s.discipline), ManaPurple)
        LabeledBar("CONSISTENCY", s.consistency, statBarMax(s.consistency), AuraCyan)

        Spacer(Modifier.height(20.dp))
        Text("Records", style = MaterialTheme.typography.titleLarge, color = AuraCyan)
        Spacer(Modifier.height(8.dp))
        InfoRow("Current strength streak", "${c.strengthStreak} days")
        InfoRow("Longest strength streak", "${c.longestStrengthStreak} days")
        InfoRow("Activity streak", "${c.activityStreak} days")
        InfoRow("Days trained", "${c.daysTrained}")
        InfoRow("Lifetime strength reps", "${c.totalStrengthReps}")
        InfoRow("Lifetime miles", "${c.totalMiles.roundToInt()}")

        Spacer(Modifier.height(20.dp))
        ExportSection(state, onImportJson)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ExportSection(state: UiState, onImportJson: (String, (Boolean, String) -> Unit) -> Unit) {
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val text = runCatching {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            }.getOrNull()
            if (text != null) {
                onImportJson(text) { _, msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            } else {
                android.widget.Toast.makeText(context, "Couldn't read file", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                os.write(Exporter.toCsv(state.days).toByteArray())
            }
        }
    }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                val now = java.time.LocalDateTime.now().toString()
                os.write(Exporter.toJson(state.days, state.profile, now).toByteArray())
            }
        }
    }

    Text("Backup & Export", style = MaterialTheme.typography.titleLarge, color = AuraCyan)
    Text("Your data is yours — export anytime. CSV matches the original spreadsheet columns.",
        style = MaterialTheme.typography.labelMedium, color = TextDim)
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { csvLauncher.launch("ascendant_export.csv") },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = ManaPurple)
    ) { Text("Export CSV (spreadsheet format)", fontWeight = FontWeight.Bold) }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { jsonLauncher.launch("ascendant_backup.json") },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
    ) { Text("Export JSON backup (everything)") }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
    ) { Text("Restore from JSON backup") }
}

@Composable
private fun AvatarChoice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) ManaPurple else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else TextDim
        )
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** Hero portrait whose aura ring thickens and brightens as the rank climbs. */
@Composable
private fun RankPortrait(portrait: Int, rank: com.mhurston.ascendant.domain.Rank, level: Int) {
    val aura = rankAuraColor(rank)
    val tier = rank.ordinal // 0 (E) .. 7 (National-Level)
    val ring = (2 + tier).dp
    val glow = ring + 8.dp
    Box(Modifier.fillMaxWidth().aspectRatio(0.85f)) {
        // Outer aura glow — thicker and brighter the higher the rank.
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(26.dp))
                .border(glow, aura.copy(alpha = 0.10f + 0.045f * tier), RoundedCornerShape(26.dp))
        )
        Image(
            painter = painterResource(portrait),
            contentDescription = "Hero portrait",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(20.dp))
                .border(ring, aura, RoundedCornerShape(20.dp))
        )
        // Rank emblem, top-left.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(androidx.compose.ui.graphics.Color(0xCC0E0E16))
                .border(1.dp, aura, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                if (rank == com.mhurston.ascendant.domain.Rank.NATIONAL) "★" else rank.label,
                color = aura, fontWeight = FontWeight.Black,
                fontFamily = com.mhurston.ascendant.ui.theme.Orbitron,
                style = MaterialTheme.typography.titleLarge
            )
        }
        // Tier caption, bottom-center.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(androidx.compose.ui.graphics.Color(0xCC0E0E16))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                "RANK ${rank.label}  ·  LV $level",
                color = TextDim, style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun rankAuraColor(rank: com.mhurston.ascendant.domain.Rank) = when (rank) {
    com.mhurston.ascendant.domain.Rank.E -> androidx.compose.ui.graphics.Color(0xFF8A8A99)
    com.mhurston.ascendant.domain.Rank.D -> androidx.compose.ui.graphics.Color(0xFF59C36A)
    com.mhurston.ascendant.domain.Rank.C -> AuraCyan
    com.mhurston.ascendant.domain.Rank.B -> androidx.compose.ui.graphics.Color(0xFF4D9BFF)
    com.mhurston.ascendant.domain.Rank.A -> ManaPurple
    com.mhurston.ascendant.domain.Rank.S -> XpGold
    com.mhurston.ascendant.domain.Rank.SS -> androidx.compose.ui.graphics.Color(0xFFFF8A3D)
    com.mhurston.ascendant.domain.Rank.NATIONAL -> androidx.compose.ui.graphics.Color(0xFFFF5470)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextDim)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
