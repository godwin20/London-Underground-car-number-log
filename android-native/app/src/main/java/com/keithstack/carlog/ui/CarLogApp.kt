package com.keithstack.carlog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keithstack.carlog.ui.screens.DetailScreen
import com.keithstack.carlog.ui.screens.HistoryScreen
import com.keithstack.carlog.ui.screens.LogScreen
import com.keithstack.carlog.ui.screens.SettingsScreen
import com.keithstack.carlog.ui.theme.Accent300
import com.keithstack.carlog.ui.theme.ColorAccent
import com.keithstack.carlog.ui.theme.ColorBg
import com.keithstack.carlog.ui.theme.Neutral700
import com.keithstack.carlog.ui.theme.Neutral900

@Composable
fun CarLogApp(viewModel: CarLogViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(ColorBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (state.screen) {
                    Screen.Log -> LogScreen(state = state, viewModel = viewModel)
                    Screen.History -> HistoryScreen(state = state, viewModel = viewModel)
                    Screen.Detail -> DetailScreen(state = state, viewModel = viewModel)
                    Screen.Settings -> SettingsScreen(state = state, viewModel = viewModel)
                }
            }
            if (state.screen != Screen.Detail) {
                BottomNav(current = state.screen, onSelect = viewModel::go)
            }
        }

        state.snack?.let { snack ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = if (state.screen == Screen.Detail) 96.dp else 88.dp)
                    .fillMaxWidth()
                    .background(Neutral900, shape = MaterialTheme.shapes.medium)
                    .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(snack.text, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (snack.car != null && state.screen == Screen.Log) {
                        TextButton(onClick = viewModel::snackOpen) { Text("View", color = Color.White) }
                    }
                    TextButton(onClick = viewModel::undo) { Text("Undo", color = Accent300) }
                }
            }
        }

        if (state.wipeOpen) {
            AlertDialog(
                onDismissRequest = viewModel::closeWipe,
                title = { Text("Delete all sightings?") },
                text = { Text("This removes every logged sighting on this device and account. This can't be undone.") },
                confirmButton = { TextButton(onClick = viewModel::wipe) { Text("Delete all") } },
                dismissButton = { TextButton(onClick = viewModel::closeWipe) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun BottomNav(current: Screen, onSelect: (Screen) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(ColorBg)
            .navigationBarsPadding(),
    ) {
        NavTab(Modifier.weight(1f), "Log", Icons.AutoMirrored.Filled.List, current == Screen.Log) { onSelect(Screen.Log) }
        NavTab(Modifier.weight(1f), "History", Icons.Filled.History, current == Screen.History) { onSelect(Screen.History) }
        NavTab(Modifier.weight(1f), "Settings", Icons.Filled.Settings, current == Screen.Settings) { onSelect(Screen.Settings) }
    }
}

@Composable
private fun NavTab(modifier: Modifier, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) ColorAccent else Neutral700
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(width = 60.dp, height = 30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        }
        Text(label, color = color, style = MaterialTheme.typography.labelMedium)
    }
}
