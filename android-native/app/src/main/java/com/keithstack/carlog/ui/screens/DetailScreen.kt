package com.keithstack.carlog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keithstack.carlog.data.exactStock
import com.keithstack.carlog.ui.CarLogUiState
import com.keithstack.carlog.ui.CarLogViewModel
import com.keithstack.carlog.ui.formatFullDate
import com.keithstack.carlog.ui.formatHm
import com.keithstack.carlog.ui.placeText
import com.keithstack.carlog.ui.theme.ColorAccent
import com.keithstack.carlog.ui.theme.ColorDivider
import com.keithstack.carlog.ui.theme.Neutral700

@Composable
fun DetailScreen(state: CarLogUiState, viewModel: CarLogViewModel) {
    val car = state.detailCar ?: return
    val seen = state.entries.filter { it.car == car }.sortedByDescending { it.ts }
    val stock = car.toIntOrNull()?.let { exactStock(it) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::goBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Car detail", style = MaterialTheme.typography.headlineSmall)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            Text((stock?.name ?: "Unknown stock").uppercase(), style = MaterialTheme.typography.labelSmall, color = ColorAccent)
            Text(car, style = MaterialTheme.typography.headlineLarge, fontSize = 64.sp)
            Text(stock?.lines ?: "Not in the fleet list", style = MaterialTheme.typography.bodyMedium)

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${seen.size}", style = MaterialTheme.typography.headlineLarge)
                    Text(if (seen.size == 1) "sighting" else "sightings", style = MaterialTheme.typography.labelSmall, color = Neutral700)
                }
                Column(modifier = Modifier.weight(1f)) {
                    val first = seen.lastOrNull()?.let { formatFullDate(it.ts) } ?: "—"
                    Text(first, style = MaterialTheme.typography.titleLarge)
                    Text("first seen", style = MaterialTheme.typography.labelSmall, color = Neutral700)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorDivider))
            Text("SIGHTINGS", style = MaterialTheme.typography.labelSmall, color = Neutral700, modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(seen) { e ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(formatFullDate(e.ts), style = MaterialTheme.typography.bodyMedium)
                                Text(formatHm(e.ts), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(placeText(e), style = MaterialTheme.typography.labelSmall, color = Neutral700)
                                val fixAge = e.fixAt?.let { "fix ${Math.round((e.ts - it) / 60_000.0)} min old" } ?: ""
                                Text(fixAge, style = MaterialTheme.typography.labelSmall, color = Neutral700)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteSighting(e.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete sighting", tint = Neutral700)
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorDivider))
                }
            }
        }
        Button(
            onClick = viewModel::logAgainFromDetail,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text("Log $car again", fontSize = 18.sp)
        }
    }
}
