package com.mhurston.ascendant.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mhurston.ascendant.ui.theme.ManaPurple

private enum class Tab(val label: String, val glyph: String) {
    CHARACTER("Hero", "🛡"),
    DASHBOARD("Train", "🏋"),
    TROPHIES("Trophies", "🏆"),
    HISTORY("Log", "📜"),
    ENERGY("Energy", "🔥")
}

@Composable
fun AscendantApp(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val reminderOn by vm.reminderEnabled.collectAsState()
    val unitSystem by vm.unitSystem.collectAsState()
    val avatar by vm.avatar.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = Tab.entries

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(t.glyph) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ManaPurple,
                            selectedTextColor = ManaPurple,
                            indicatorColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }
        }
    ) { padding ->
        val content = Modifier.padding(padding)
        when (tabs[tab]) {
            Tab.DASHBOARD -> DashboardScreen(
                state = state,
                onAddReps = vm::addReps,
                onAddMiles = vm::addMiles,
                onQuickLog = vm::quickLogFullDay,
                onResetToday = vm::resetToday,
                onToggleFavVideo = vm::toggleFavoriteVideo,
                onAddUserVideo = vm::addUserVideo,
                modifier = content
            )
            Tab.CHARACTER -> CharacterScreen(
                state = state,
                onImportJson = vm::importBackupJson,
                avatar = avatar,
                onSetAvatar = vm::setAvatar,
                modifier = content
            )
            Tab.TROPHIES -> AchievementsScreen(state, content)
            Tab.HISTORY -> CalendarScreen(
                state = state,
                onAddReps = vm::addRepsForDate,
                onAddMiles = vm::addMilesForDate,
                onResetDay = vm::resetDay,
                modifier = content
            )
            Tab.ENERGY -> EnergyScreen(
                state = state,
                onSaveProfile = vm::saveProfile,
                onSetConsumed = vm::setConsumed,
                onResetGoalStart = vm::resetGoalStart,
                unitSystem = unitSystem,
                onSetUnit = vm::setUnitSystem,
                reminderEnabled = reminderOn,
                onSetReminder = vm::setReminderEnabled,
                modifier = content
            )
        }
    }
}
