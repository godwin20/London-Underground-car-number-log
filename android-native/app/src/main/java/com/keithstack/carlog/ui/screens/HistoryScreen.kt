package com.keithstack.carlog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keithstack.carlog.data.Sighting
import com.keithstack.carlog.data.stockShort
import com.keithstack.carlog.ui.CarLogUiState
import com.keithstack.carlog.ui.CarLogViewModel
import com.keithstack.carlog.ui.formatDayLabel
import com.keithstack.carlog.ui.formatHm
import com.keithstack.carlog.ui.placeText
import com.keithstack.carlog.ui.theme.ColorDivider
import com.keithstack.carlog.ui.theme.Neutral700

@Composable
fun HistoryScreen(state: CarLogUiState, viewModel: CarLogViewModel) {
    val query = state.query.trim()
    val filtered = state.entries.filter { query.isEmpty() || it.car.contains(query) }.sortedByDescending { it.ts }
    val uniqueCars = state.entries.map { it.car }.distinct().size

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("History", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${state.entries.size} ${if (state.entries.size == 1) "sighting" else "sightings"} · $uniqueCars ${if (uniqueCars == 1) "car" else "cars"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Neutral700,
                )
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Find a car number") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorDivider))

        if (state.entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No cars logged yet", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Cars you log appear here, grouped by day. Everything is stored on this phone and account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Neutral700,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
                androidx.compose.material3.OutlinedButton(onClick = viewModel::addSample) { Text("Add sample sightings") }
            }
            return
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No sightings of $query yet.", style = MaterialTheme.typography.bodyMedium, color = Neutral700, textAlign = TextAlign.Center)
            }
            return
        }

        val groups: List<Pair<String, List<Sighting>>> = run {
            val result = mutableListOf<Pair<String, MutableList<Sighting>>>()
            filtered.forEach { e ->
                val label = formatDayLabel(state.now, e.ts)
                if (result.isEmpty() || result.last().first != label) {
                    result.add(label to mutableListOf(e))
                } else {
                    result.last().second.add(e)
                }
            }
            result
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            groups.forEach { (label, items) ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Neutral700)
                        Text(
                            if (items.size == 1) "1 car" else "${items.size} cars",
                            style = MaterialTheme.typography.labelSmall,
                            color = Neutral700,
                        )
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorDivider))
                }
                items(items) { e ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openDetail(e.car) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(e.car, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stockShort(e.car), style = MaterialTheme.typography.bodySmall)
                            Text(placeText(e), style = MaterialTheme.typography.labelSmall, color = Neutral700, maxLines = 1)
                        }
                        Text(formatHm(e.ts), style = MaterialTheme.typography.labelSmall, color = Neutral700)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorDivider))
                }
            }
        }
    }
}
