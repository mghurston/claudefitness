package com.mhurston.ascendant.ui

import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.mhurston.ascendant.R
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mhurston.ascendant.domain.Progression
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mhurston.ascendant.data.Exporter
import com.mhurston.ascendant.ui.theme.AuraCyan
import com.mhurston.ascendant.ui.theme.CrimsonRed
import com.mhurston.ascendant.ui.theme.ManaPurple
import com.mhurston.ascendant.ui.theme.SuccessGreen
import com.mhurston.ascendant.ui.theme.TextDim
import com.mhurston.ascendant.ui.theme.XpGold
import kotlin.math.roundToInt

@Composable
fun CharacterScreen(
    state: UiState,
    onImportJson: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    /** Builds the full backup JSON (days + profile + settings) — see AppViewModel.buildBackupJson. */
    onExportJson: () -> String = { "" },
    avatar: com.mhurston.ascendant.domain.Avatar = com.mhurston.ascendant.domain.Avatar.MALE,
    onSetAvatar: (com.mhurston.ascendant.domain.Avatar) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = state.character
    val s = c.stats
    var showTiers by remember { mutableStateOf(false) }
    if (showTiers) RankTiersDialog(currentLevel = c.level) { showTiers = false }
    var showAttrInfo by remember { mutableStateOf(false) }
    if (showAttrInfo) AttributesInfoDialog { showAttrInfo = false }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        ScreenTitle("Ascendant")
        ScreenSubtitle("Progress sheet")
        Spacer(Modifier.height(16.dp))

        // Hero portrait — choose your character; the aura border follows your rank.
        val portrait = when (avatar) {
            com.mhurston.ascendant.domain.Avatar.FEMALE -> R.drawable.hero_portrait_female
            com.mhurston.ascendant.domain.Avatar.MALE_BLACK -> R.drawable.hero_portrait_male_black
            com.mhurston.ascendant.domain.Avatar.FEMALE_BLACK -> R.drawable.hero_portrait_female_black
            else -> R.drawable.hero_portrait
        }
        // Portrait shares the row with today's status rings, so the focus isn't all on the
        // picture — your goals sit right beside your hero. The row sizes to the rings column
        // (taller with 3 rings than 2), and the portrait fills that height — cropping the
        // image taller via ContentScale.Crop so there's never dead space beside the rings.
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {
            RankPortrait(portrait = portrait, rank = c.rank, level = c.level,
                modifier = Modifier.weight(0.95f).fillMaxHeight())
            Spacer(Modifier.width(16.dp))
            StatusRings(state, modifier = Modifier.weight(1.05f))
        }
        Spacer(Modifier.height(12.dp))
        Caption("Choose your character")
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            com.mhurston.ascendant.domain.Avatar.entries.forEach { a ->
                AvatarChoice(a.label, avatar == a, Modifier.weight(1f)) { onSetAvatar(a) }
            }
        }
        Spacer(Modifier.height(16.dp))

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
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showTiers = true }) {
                        Text(c.title, style = MaterialTheme.typography.titleLarge, color = AuraCyan)
                        Spacer(Modifier.width(6.dp))
                        // Tap to see the full rank/title ladder — not documented anywhere else.
                        Text("ⓘ", style = MaterialTheme.typography.titleLarge, color = ManaPurple)
                    }
                    Caption("${c.totalXp} XP" +
                        if (c.idlePenaltyXp > 0) "  (−${c.idlePenaltyXp} idle)" else "")
                }
                RankBadge(c.rank, c.level)
            }
        }

        Spacer(Modifier.height(20.dp))
        // Tap to see how each attribute is earned — same ⓘ pattern as the rank-tiers dialog.
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { showAttrInfo = true }) {
            SectionHeader("Attributes")
            Spacer(Modifier.width(6.dp))
            Text("ⓘ", style = MaterialTheme.typography.titleLarge, color = ManaPurple)
        }
        // One distinct color per stat (Style Guide §2.2) so bars are tellable at a glance.
        LabeledBar("STRENGTH", s.strength, statBarMax(s.strength), CrimsonRed)
        LabeledBar("ENDURANCE", s.endurance, statBarMax(s.endurance), SuccessGreen)
        LabeledBar("AGILITY", s.agility, statBarMax(s.agility), AuraCyan)
        LabeledBar("DISCIPLINE", s.discipline, statBarMax(s.discipline), ManaPurple)
        LabeledBar("CONSISTENCY", s.consistency, statBarMax(s.consistency), XpGold)

        Spacer(Modifier.height(20.dp))
        SectionHeader("Records")
        Spacer(Modifier.height(8.dp))
        InfoRow("Current strength streak", "${c.strengthStreak} days")
        InfoRow("Longest strength streak", "${c.longestStrengthStreak} days")
        InfoRow("Activity streak", "${c.activityStreak} days")
        InfoRow("Days trained", "${c.daysTrained}")
        InfoRow("Lifetime strength reps", "${c.totalStrengthReps}")
        InfoRow("Lifetime miles", "${c.totalMiles.roundToInt()}")

        Spacer(Modifier.height(20.dp))
        AboutSection()

        Spacer(Modifier.height(20.dp))
        ExportSection(state, onImportJson, onExportJson)
        Spacer(Modifier.height(24.dp))
    }
}

/** The rank/title ladder — the level at which each rank and its title unlock. This progression
 *  isn't surfaced anywhere else, so the ⓘ next to the current title opens it. The row matching the
 *  hero's current level is highlighted. */
@Composable
private fun RankTiersDialog(currentLevel: Int, onDismiss: () -> Unit) {
    // Driven by Progression so the dialog can never drift from the real thresholds.
    val tierLevels = listOf(1, 5, 10, 20, 35, 50, 75, 100)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rank & title tiers") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Caption("Level up by earning XP (1 calorie = 1 XP). Each tier unlocks a new rank and title:")
                Spacer(Modifier.height(12.dp))
                tierLevels.forEach { lvl ->
                    val rank = Progression.rankForLevel(lvl)
                    val title = Progression.titleForLevel(lvl)
                    val isCurrent = currentLevel >= lvl &&
                        (tierLevels.firstOrNull { it > lvl }?.let { currentLevel < it } ?: true)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Lv $lvl",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCurrent) XpGold else TextDim,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            "${rank.label} · $title",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isCurrent) XpGold else AuraCyan,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it", fontWeight = FontWeight.Bold) } }
    )
}

/** How each attribute is earned. The formulas live in Progression.rebuild — this ⓘ dialog is
 *  the only place the app explains them, so keep the text in sync if the formulas change. */
@Composable
private fun AttributesInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How attributes grow") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AttrInfo("STRENGTH", CrimsonRed,
                    "Lifetime pushups, squats & curls.",
                    "√(reps ÷ 50) — point 1 at 50 reps, point 10 at 5,000.")
                AttrInfo("ENDURANCE", SuccessGreen,
                    "Lifetime manually-logged walking miles. Tracked steps earn XP but not END.",
                    "√(miles × 4) — point 1 at ¼ mile, point 10 at 25 miles.")
                AttrInfo("AGILITY", AuraCyan,
                    "Lifetime leg lifts & calf raises.",
                    "√(reps ÷ 60) — point 1 at 60 reps, point 10 at 6,000.")
                AttrInfo("DISCIPLINE", ManaPurple,
                    "Days where you hit at least 80% of the daily goals.",
                    "1 point per qualifying day.")
                AttrInfo("CONSISTENCY", XpGold,
                    "Strength streaks — the only attribute that can drop (when a streak dies).",
                    "Longest streak ever + half your current streak.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it", fontWeight = FontWeight.Bold) } }
    )
}

/** One attribute entry in the info dialog: colored name, what feeds it, then the formula. */
@Composable
private fun AttrInfo(name: String, color: Color, source: String, formula: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(name, style = MaterialTheme.typography.labelLarge, color = color,
            fontWeight = FontWeight.Bold)
        BodyText(source)
        Caption(formula)
    }
}

/** Today's status at a glance — Goals completion, Active Burn, and (when passive sync is on)
 *  passively-tracked Steps. Stacked vertically so they sit beside the hero portrait. */
@Composable
private fun StatusRings(state: UiState, modifier: Modifier = Modifier) {
    val completionPct = (state.todayDerived.completion * 100).roundToInt()
    // Read the carried-forward day the XP engine scored, so burn kcal == burn XP exactly.
    val burn = com.mhurston.ascendant.domain.Calories
        .activityBurn(state.profile, state.todayData).roundToInt()
    val burnTarget = com.mhurston.ascendant.domain.Calories.dailyBurnTarget(state.profile)
    val steps = state.today.passiveSteps
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RingStat(
            completion = state.todayDerived.completion,
            label = "✓ Goals",
            centerLabel = "$completionPct%",
            centerSub = (if (state.todayDerived.xp < 0) "${state.todayDerived.xp}"
                else "+${state.todayDerived.xp}") + " XP",
            color = AuraCyan
        )
        RingStat(
            completion = if (burnTarget > 0) burn.toDouble() / burnTarget else 0.0,
            label = "🔥 Burn",
            centerLabel = "$burn",
            centerSub = "/ $burnTarget kcal",
            color = XpGold
        )
        if (steps > 0) {
            val goal = com.mhurston.ascendant.domain.Calories.PASSIVE_STEP_GOAL
            RingStat(
                completion = if (goal > 0) steps.toDouble() / goal else 0.0,
                label = "👟 Steps",
                centerLabel = if (steps >= 1000)
                    "%.1fk".format(java.util.Locale.US, steps / 1000.0) else "$steps",
                centerSub = "≈%.1f mi".format(java.util.Locale.US, state.today.trackedMiles),
                color = AuraCyan
            )
        }
    }
}

/** One compact ring with its label to the right — the vertical building block beside the portrait. */
@Composable
private fun RingStat(
    completion: Double,
    label: String,
    centerLabel: String,
    centerSub: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CompletionRing(
            completion = completion,
            size = 84.dp,
            centerLabel = centerLabel,
            centerSub = centerSub,
            centerColor = color
        )
        Spacer(Modifier.width(12.dp))
        BodyText(label, color = TextDim)
    }
}

/** About the app: a one-line description plus the author's website + Linktree.
 *  Links open in an in-app Custom Tab (same pattern as the form-video links). */
@Composable
private fun AboutSection() {
    val context = LocalContext.current
    fun open(url: String) {
        try {
            CustomTabsIntent.Builder().setShowTitle(true).build()
                .launchUrl(context, android.net.Uri.parse(url))
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) {
                android.widget.Toast.makeText(
                    context, "No app to open this link", android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    SectionHeader("About")
    Caption("ASCENDANT turns your daily workouts into an anime RPG — every rep and mile " +
        "becomes XP, levels, and ranks built from your real training.")
    Spacer(Modifier.height(10.dp))
    Button(
        onClick = { open("https://www.michaelghurston.com") },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
    ) { Text("🌐 michaelghurston.com", color = AuraCyan) }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { open("https://linktr.ee/mghurston") },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
    ) { Text("🔗 linktr.ee/mghurston", color = ManaPurple) }
}

@Composable
private fun ExportSection(
    state: UiState,
    onImportJson: (String, (Boolean, String) -> Unit) -> Unit,
    onExportJson: () -> String
) {
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
                os.write(onExportJson().toByteArray())
            }
        }
    }

    SectionHeader("Backup & Export")
    Caption("Your data is yours — export anytime. CSV matches the original spreadsheet columns.")
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
private fun RankPortrait(portrait: Int, rank: com.mhurston.ascendant.domain.Rank, level: Int,
    modifier: Modifier = Modifier) {
    val aura = rankAuraColor(rank)
    val tier = rank.ordinal // 0 (E) .. 7 (National-Level)
    val ring = (2 + tier).dp
    val glow = ring + 8.dp
    // Height comes from the caller (it fills the rings-row height); the image crops to fit.
    Box(modifier) {
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
                .background(com.mhurston.ascendant.ui.theme.ScrimDark)
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
                .background(com.mhurston.ascendant.ui.theme.ScrimDark)
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
        BodyText(label, color = TextDim)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
