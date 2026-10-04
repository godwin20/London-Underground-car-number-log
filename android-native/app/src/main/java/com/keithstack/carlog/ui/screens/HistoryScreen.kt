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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.keithstack.carlog.data.Sighting
import com.keithstack.carlog.data.stockShort
import com.keithstack.carlog.ui.CarLogUiState
import com.keithstack.carlog.ui.CarLogViewModel
import com.keithstack.carlog.ui.formatDayLabel
import com.keithstack.carlog.ui.formatFullDate
import com.keithstack.carlog.ui.formatHm
import com.keithstack.carlog.ui.placeText
import com.keithstack.carlog.ui.theme.ColorAccent
import com.keithstack.carlog.ui.theme.ColorDivider
import com.keithstack.carlog.ui.theme.ColorText
import com.keithstack.carlog.ui.theme.Neutral100
import com.keithstack.carlog.ui.theme.Neutral700

@Composable
fun HistoryScreen(state: CarLogUiState, viewModel: CarLogViewModel) {
    val query = state.query.trim()
    val filtered = state.entries.filter { query.isEmpty() || it.car.contains(query) }
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
            if (state.entries.isNotEmpty()) {
                HistoryModeToggle(
                    byCar = state.historyByCar,
                    onChange = viewModel::setHistoryByCar,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
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

        if (state.historyByCar) {
            CarGroupedList(filtered, viewModel)
        } else {
            DayGroupedList(filtered, state.now, viewModel)
        }
    }
}

@Composable
private fun DayGroupedList(filtered: List<Sighting>, now: Long, viewModel: CarLogViewModel) {
    val sorted = filtered.sortedByDescending { it.ts }
    val groups: List<Pair<String, List<Sighting>>> = run {
        val result = mutableListOf<Pair<String, MutableList<Sighting>>>()
        sorted.forEach { e ->
            val label = formatDayLabel(now, e.ts)
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

/** Groups sightings by car number so each car's full history reads as its own section, newest car first. */
@Composable
private fun CarGroupedList(filtered: List<Sighting>, viewModel: CarLogViewModel) {
    val groups: List<Pair<String, List<Sighting>>> = filtered
        .groupBy { it.car }
        .mapValues { (_, list) -> list.sortedByDescending { it.ts } }
        .toList()
        .sortedByDescending { (_, list) -> list.first().ts }

    val previewCount = 3
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        groups.forEach { (car, sightings) ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openDetail(car) }
                        .padding(top = 16.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(car, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 12.dp))
                    Text(stockShort(car), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(
                        if (sightings.size == 1) "1 sighting" else "${sightings.size} sightings",
                        style = MaterialTheme.typography.labelSmall,
                        color = Neutral700,
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorDivider))
            }
            items(sightings.take(previewCount)) { e ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openDetail(car) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${formatFullDate(e.ts)} · ${formatHm(e.ts)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(placeText(e), style = MaterialTheme.typography.labelSmall, color = Neutral700, maxLines = 1)
                }
            }
            if (sightings.size > previewCount) {
                item {
                    Text(
                        "+ ${sightings.size - previewCount} more — tap to view all",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorAccent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openDetail(car) }
                            .padding(bottom = 8.dp),
                    )
                }
            }
        }
    }
}

/** A bordered pill container with a filled sub-pill for the selected option — mirrors the Settings toggle. */
@Composable
private fun HistoryModeToggle(byCar: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.border(1.dp, ColorDivider, RoundedCornerShape(8.dp)).padding(3.dp),
    ) {
        listOf("By day" to false, "By car" to true).forEach { (label, value) ->
            val selected = byCar == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) ColorAccent else Color.Transparent)
                    .clickable { onChange(value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = if (selected) Neutral100 else ColorText)
            }
        }
    }
}
