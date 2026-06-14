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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mhurston.ascendant.domain.Calories
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.Sex
import com.mhurston.ascendant.domain.UnitSystem
import com.mhurston.ascendant.domain.Units
import com.mhurston.ascendant.ui.theme.AuraCyan
import com.mhurston.ascendant.ui.theme.DangerRed
import com.mhurston.ascendant.ui.theme.ManaPurple
import com.mhurston.ascendant.ui.theme.TextDim
import com.mhurston.ascendant.ui.theme.XpGold
import kotlin.math.roundToInt

@Composable
fun EnergyScreen(
    state: UiState,
    onSaveProfile: (Profile) -> Unit,
    onSetConsumed: (Int) -> Unit,
    onResetGoalStart: (Double) -> Unit,
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    onSetUnit: (UnitSystem) -> Unit = {},
    reminderEnabled: Boolean = false,
    onSetReminder: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val p = state.profile
    val imperial = unitSystem == UnitSystem.IMPERIAL

    var sex by remember(p) { mutableStateOf(p.sex) }
    var age by remember(p) { mutableStateOf(p.age.toString()) }
    // Weight/goal fields are in the active display unit; height for imperial uses ft+in.
    var weightStr by remember(p, unitSystem) {
        mutableStateOf(if (imperial) wholeStr(Units.kgToLbs(p.weightKg)) else trimNum(p.weightKg))
    }
    var goalStr by remember(p, unitSystem) {
        mutableStateOf(
            if (p.goalWeightKg <= 0) ""
            else if (imperial) wholeStr(Units.kgToLbs(p.goalWeightKg)) else trimNum(p.goalWeightKg)
        )
    }
    var heightCmStr by remember(p, unitSystem) { mutableStateOf(trimNum(p.heightCm)) }
    val (ft0, in0) = Units.cmToFeetInches(p.heightCm)
    var feetStr by remember(p, unitSystem) { mutableStateOf(ft0.toString()) }
    var inchStr by remember(p, unitSystem) { mutableStateOf(in0.toString()) }
    var consumed by remember(state.today.date) { mutableStateOf(state.today.caloriesConsumed.toString()) }

    // Convert the display fields back to canonical metric for storage + calorie math.
    val weightKg = if (imperial) Units.lbsToKg(weightStr.toDoubleOrNull() ?: 0.0) else (weightStr.toDoubleOrNull() ?: 0.0)
    val goalKg = if ((goalStr.toDoubleOrNull() ?: 0.0) <= 0) 0.0
        else if (imperial) Units.lbsToKg(goalStr.toDouble()) else goalStr.toDouble()
    val heightCm = if (imperial)
        Units.feetInchesToCm(feetStr.toIntOrNull() ?: 0, inchStr.toIntOrNull() ?: 0)
        else (heightCmStr.toDoubleOrNull() ?: 0.0)

    val liveProfile = Profile(
        sex = sex,
        age = age.toIntOrNull() ?: 0,
        heightCm = heightCm,
        weightKg = weightKg,
        goalWeightKg = goalKg,
        startWeightKg = p.startWeightKg
    )
    val est = Calories.estimate(liveProfile, state.today.toDayData(), consumed.toIntOrNull() ?: 0)

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Energy Balance", style = MaterialTheme.typography.headlineMedium, color = ManaPurple)
        Text("Uses today's logged activity", style = MaterialTheme.typography.labelMedium, color = TextDim)
        Spacer(Modifier.height(12.dp))

        // Units toggle
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Toggle("US (lb/ft)", imperial) { onSetUnit(UnitSystem.IMPERIAL) }
            Toggle("Metric (kg/cm)", !imperial) { onSetUnit(UnitSystem.METRIC) }
        }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Toggle("Male", sex == Sex.MALE) { sex = Sex.MALE }
            Toggle("Female", sex == Sex.FEMALE) { sex = Sex.FEMALE }
        }
        Spacer(Modifier.height(8.dp))

        NumField("Age (years)", age) { age = it }
        if (imperial) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box1(Modifier.weight(1f)) { NumField("Height (ft)", feetStr) { feetStr = it } }
                Box1(Modifier.weight(1f)) { NumField("Height (in)", inchStr) { inchStr = it } }
            }
            NumField("Weight (lb)", weightStr) { weightStr = it }
            NumField("Goal weight (lb)", goalStr) { goalStr = it }
        } else {
            NumField("Height (cm)", heightCmStr) { heightCmStr = it }
            NumField("Weight (kg)", weightStr) { weightStr = it }
            NumField("Goal weight (kg)", goalStr) { goalStr = it }
        }
        NumField("Calories consumed today", consumed) { consumed = it }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                onSaveProfile(liveProfile)
                onSetConsumed(consumed.toIntOrNull() ?: 0)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ManaPurple)
        ) { Text("Save", fontWeight = FontWeight.Bold) }

        if (p.goalWeightKg > 0 && p.startWeightKg > 0) {
            Spacer(Modifier.height(20.dp))
            WeightGoalCard(p, imperial, onResetGoalStart)
        }

        Spacer(Modifier.height(20.dp))
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp)) {
                Stat("Calories burned", "${est.totalBurn} kcal", XpGold)
                Stat("  • resting (BMR)", "${est.bmr} kcal", TextDim)
                Stat("  • activity", "${est.activityBurn} kcal", TextDim)
                Spacer(Modifier.height(8.dp))
                Stat("Calories consumed", "${est.consumed} kcal", AuraCyan)
                Spacer(Modifier.height(8.dp))
                val surplus = est.net > 0
                Stat(
                    if (surplus) "Surplus (net)" else "Deficit (net)",
                    "${if (surplus) "+" else ""}${est.net} kcal",
                    if (surplus) DangerRed else AuraCyan
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Estimates only. BMR via Mifflin–St Jeor; walking burn scales with your " +
                "body weight and miles. Activity is pulled from what you logged today.",
            style = MaterialTheme.typography.labelMedium, color = TextDim
        )

        Spacer(Modifier.height(20.dp))
        ReminderCard(reminderEnabled, onSetReminder)
        Spacer(Modifier.height(24.dp))
    }
}

private fun trimNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

private fun wholeStr(v: Double): String = v.roundToInt().toString()

@Composable
private fun Box1(modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier) { content() }
}

@Composable
private fun ReminderCard(enabled: Boolean, onSet: (Boolean) -> Unit) {
    val context = LocalContext.current
    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> onSet(granted) }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Daily reminder", style = MaterialTheme.typography.titleLarge, color = AuraCyan)
                    Text("A 6 PM nudge — stronger on Wed/Fri/Sat. Local only, no internet.",
                        style = MaterialTheme.typography.labelMedium, color = TextDim)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { want ->
                        if (want) {
                            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                                !com.mhurston.ascendant.notify.Reminders.hasPermission(context)) {
                                permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else onSet(true)
                        } else onSet(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun WeightGoalCard(p: Profile, imperial: Boolean, onResetGoalStart: (Double) -> Unit) {
    fun disp(kg: Double): String =
        if (imperial) wholeStr(Units.kgToLbs(kg)) else trimNum(kg)
    val unit = if (imperial) "lb" else "kg"
    val losing = p.startWeightKg >= p.goalWeightKg
    val toGoKg = kotlin.math.abs(p.kgToGoal)
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Weight Goal", style = MaterialTheme.typography.titleLarge, color = AuraCyan)
                Text("${(p.goalProgress * 100).toInt()}%", color = XpGold, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            ProgressTrack(fraction = p.goalProgress, color = XpGold, height = 14.dp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("start ${disp(p.startWeightKg)}", style = MaterialTheme.typography.labelMedium, color = TextDim)
                Text("now ${disp(p.weightKg)} $unit", style = MaterialTheme.typography.labelMedium,
                    color = AuraCyan, fontWeight = FontWeight.Bold)
                Text("goal ${disp(p.goalWeightKg)}", style = MaterialTheme.typography.labelMedium, color = TextDim)
            }
            Spacer(Modifier.height(10.dp))
            val msg = when {
                p.goalReached -> "🎉 Goal reached — set a new one!"
                losing -> "${disp(toGoKg)} $unit to lose to hit your goal."
                else -> "${disp(toGoKg)} $unit to gain to hit your goal."
            }
            Text(msg, style = MaterialTheme.typography.bodyLarge,
                color = if (p.goalReached) XpGold else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text("↺ Set current weight as new start",
                style = MaterialTheme.typography.labelMedium, color = ManaPurple,
                modifier = Modifier.clickable { onResetGoalStart(p.weightKg) })
        }
    }
}

@Composable
private fun Toggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) ManaPurple else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else TextDim
        )
    ) { Text(label) }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onChange(new.filter { it.isDigit() || it == '.' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun Stat(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}
