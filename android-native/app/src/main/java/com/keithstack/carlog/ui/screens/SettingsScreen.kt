package com.keithstack.carlog.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.keithstack.carlog.data.KeypadOrder
import com.keithstack.carlog.data.LogStyle
import com.keithstack.carlog.ui.APP_VERSION
import com.keithstack.carlog.ui.CarLogUiState
import com.keithstack.carlog.ui.CarLogViewModel
import com.keithstack.carlog.ui.theme.Accent700
import com.keithstack.carlog.ui.theme.ColorAccent
import com.keithstack.carlog.ui.theme.ColorDivider
import com.keithstack.carlog.ui.theme.ColorText
import com.keithstack.carlog.ui.theme.Neutral100
import com.keithstack.carlog.ui.theme.Neutral700

@Composable
fun SettingsScreen(state: CarLogUiState, viewModel: CarLogViewModel, onSignInWithGoogle: () -> Unit = {}) {
    val context = LocalContext.current
    val uniqueCars = state.entries.map { it.car }.distinct().size

    LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 18.dp))
        }

        item { SectionHeading("Account") }
        item {
            val account = state.account
            if (account != null && !account.isAnonymous) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.displayName ?: account.email ?: "Signed in", style = MaterialTheme.typography.bodyMedium)
                        account.email?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Neutral700) }
                    }
                    OutlinedButton(onClick = viewModel::signOutGoogle) { Text("Sign out") }
                }
            } else {
                Text(
                    "Signed in anonymously on this device only. Sign in with Google to keep your log if you reinstall or switch phones.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Button(onClick = onSignInWithGoogle, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    Text("Sign in with Google")
                }
            }
            state.signInError?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = Neutral700, modifier = Modifier.padding(bottom = 14.dp))
            }
            SectionDivider()
        }

        item { SectionHeading("Log screen") }
        item {
            StyleOption("Keypad", "Large display and keypad, time and location shown above", state.settings.style == LogStyle.Keypad) { viewModel.setStyle(LogStyle.Keypad) }
            StyleOption("Ledger", "Today's cars as a logbook page, with a Log key beside the digits", state.settings.style == LogStyle.Ledger) { viewModel.setStyle(LogStyle.Ledger) }
            StyleOption("Plate", "The number shown large in a frame, with round keys", state.settings.style == LogStyle.Plate) { viewModel.setStyle(LogStyle.Plate) }
        }

        item { SectionHeading("Entry") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SegmentedRow("Keypad order", listOf("1 2 3" to (state.settings.order == KeypadOrder.Phone), "7 8 9" to (state.settings.order == KeypadOrder.Calculator))) { index ->
                    viewModel.setOrder(if (index == 0) KeypadOrder.Phone else KeypadOrder.Calculator)
                }
                SegmentedRow("Stock hints", listOf("On" to state.settings.hints, "Off" to !state.settings.hints)) { index -> viewModel.setHints(index == 0) }
                SegmentedRow("Vibrate on keys", listOf("On" to state.settings.vibrate, "Off" to !state.settings.vibrate)) { index -> viewModel.setVibrate(index == 0) }
            }
            SectionDivider(topPadding = 12.dp)
        }

        item { SectionHeading("Location") }
        item {
            Text(
                "There's no GPS signal below ground, so each sighting uses the last position the phone got on the surface, with its age saved alongside it.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Location status", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = viewModel::refreshLocationFix) { Text("Get fix") }
            }
            SectionDivider(topPadding = 12.dp)
        }

        item { SectionHeading("Data") }
        item {
            Text(
                "${state.entries.size} ${if (state.entries.size == 1) "sighting" else "sightings"} · $uniqueCars ${if (uniqueCars == 1) "car" else "cars"}, backed up to your account.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val uri = viewModel.exportCsv()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export CSV"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Export CSV") }
                OutlinedButton(onClick = viewModel::addSample, modifier = Modifier.fillMaxWidth()) { Text("Add sample sightings") }
                OutlinedButton(onClick = viewModel::askWipe, modifier = Modifier.fillMaxWidth()) { Text("Delete all…") }
            }
            SectionDivider(topPadding = 14.dp)
        }

        item { SectionHeading("Updates") }
        item {
            Text("Version $APP_VERSION", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
            if (state.update.available) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Version ${state.update.version} is available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Accent700,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = {
                        if (viewModel.apkInstaller.canInstallUnknownApps()) {
                            viewModel.downloadUpdate()
                        } else {
                            context.startActivity(viewModel.apkInstaller.requestInstallPermissionIntent())
                        }
                    }) { Text("Download") }
                }
            }
            state.update.error?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = Neutral700, modifier = Modifier.padding(bottom = 8.dp))
            }
            OutlinedButton(onClick = viewModel::checkForUpdate) {
                Text(if (state.update.checking) "Checking…" else "Check for updates")
            }
        }

        item {
            Text(
                "Stock type is worked out from approximate number ranges and may be wrong for some cars.",
                style = MaterialTheme.typography.labelSmall,
                color = Neutral700,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Neutral700,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun SectionDivider(topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = topPadding).height(1.dp).background(ColorDivider))
}

@Composable
private fun StyleOption(name: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = onClick, colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = ColorAccent))
        Column(modifier = Modifier.padding(start = 4.dp, top = 10.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.labelSmall, color = Neutral700, modifier = Modifier.padding(top = 2.dp))
        }
    }
    SectionDivider()
}

/** A bordered pill container with a filled sub-pill for the selected option — reads as one cohesive toggle. */
@Composable
private fun SegmentedRow(label: String, options: List<Pair<String, Boolean>>, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .border(1.dp, ColorDivider, RoundedCornerShape(8.dp))
                .padding(3.dp),
        ) {
            options.forEachIndexed { index, (optLabel, selected) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) ColorAccent else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        optLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) Neutral100 else ColorText,
                    )
                }
            }
        }
    }
}
