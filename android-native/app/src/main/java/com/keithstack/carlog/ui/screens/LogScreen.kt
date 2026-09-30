package com.keithstack.carlog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keithstack.carlog.data.KeypadOrder
import com.keithstack.carlog.data.LogStyle
import com.keithstack.carlog.data.Sighting
import com.keithstack.carlog.data.stockShort
import com.keithstack.carlog.location.LocationStatus
import com.keithstack.carlog.ui.CarLogUiState
import com.keithstack.carlog.ui.CarLogViewModel
import com.keithstack.carlog.ui.HintTone
import com.keithstack.carlog.ui.describeInput
import com.keithstack.carlog.ui.formatAgo
import com.keithstack.carlog.ui.formatHm
import com.keithstack.carlog.ui.placeText
import com.keithstack.carlog.ui.theme.Accent100
import com.keithstack.carlog.ui.theme.Accent700
import com.keithstack.carlog.ui.theme.ColorAccent
import com.keithstack.carlog.ui.theme.ColorDivider
import com.keithstack.carlog.ui.theme.ColorSurface
import com.keithstack.carlog.ui.theme.ColorText
import com.keithstack.carlog.ui.theme.Neutral100
import com.keithstack.carlog.ui.theme.Neutral400
import com.keithstack.carlog.ui.theme.Neutral500
import com.keithstack.carlog.ui.theme.Neutral700
import com.keithstack.carlog.ui.theme.Neutral800
import java.text.SimpleDateFormat
import java.util.Locale

private fun keyOrder(order: KeypadOrder): List<String> =
    if (order == KeypadOrder.Calculator) listOf("7", "8", "9", "4", "5", "6", "1", "2", "3")
    else listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")

private fun entriesByCar(entries: List<Sighting>): Map<String, List<Sighting>> =
    entries.groupBy { it.car }.mapValues { (_, list) -> list.sortedByDescending { it.ts } }

private fun toneColor(tone: HintTone): Color = when (tone) {
    HintTone.Neutral700 -> Neutral700
    HintTone.Neutral800 -> Neutral800
    HintTone.Accent700 -> Accent700
}

@Composable
fun LogScreen(state: CarLogUiState, viewModel: CarLogViewModel) {
    val today = countLoggedToday(state.entries, state.now)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (state.settings.style == LogStyle.Ledger) {
                    Text(weekdayName(state.now), style = MaterialTheme.typography.labelSmall, color = ColorAccent)
                }
                Text(
                    text = if (state.settings.style == LogStyle.Ledger) longDate(state.now) else "Log a car",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Box(
                modifier = Modifier.background(Neutral100, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("$today today", style = MaterialTheme.typography.labelMedium, color = Neutral800)
            }
        }

        when (state.settings.style) {
            LogStyle.Keypad -> KeypadContent(state, viewModel)
            LogStyle.Ledger -> LedgerContent(state, viewModel)
            LogStyle.Plate -> PlateContent(state, viewModel)
        }
    }
}

private fun countLoggedToday(entries: List<Sighting>, now: Long): Int {
    val start = java.util.Calendar.getInstance().apply {
        timeInMillis = now
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    return entries.count { it.ts >= start }
}

private val weekdayFormat = SimpleDateFormat("EEEE", Locale.UK)
private val longDateFormat = SimpleDateFormat("EEEE d MMMM", Locale.UK)
private fun weekdayName(ts: Long) = weekdayFormat.format(ts)
private fun longDate(ts: Long) = longDateFormat.format(ts)

@Composable
private fun LocationRow(state: CarLogUiState, viewModel: CarLogViewModel, modifier: Modifier = Modifier) {
    val (title, sub) = when (val status = state.locationStatus) {
        is LocationStatus.Available -> "Last known position" to
            "${"%.4f".format(status.fix.lat)}, ${"%.4f".format(status.fix.lon)} · ±${status.fix.accuracyMeters} m · ${formatAgo(state.now, status.fix.atMillis)}"
        LocationStatus.Locating -> "Finding location…" to "Waiting for GPS"
        LocationStatus.Idle -> "Finding location…" to "Waiting for GPS"
        is LocationStatus.Error -> status.message to "Tap to try again. Sightings save without location."
    }
    val iconColor = if (state.locationStatus is LocationStatus.Available) Accent700 else Neutral500
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { viewModel.refreshLocationFix() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = Neutral700, maxLines = 1)
        }
        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Neutral500, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun KeypadContent(state: CarLogUiState, viewModel: CarLogViewModel) {
    val hint = describeInput(state.input, entriesByCar(state.entries), state.settings.hints, state.now)
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.border(1.dp, ColorDivider)) {
            LocationRow(state, viewModel, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0 until 5) {
                    val ch = state.input.getOrNull(i)?.toString() ?: ""
                    val lineColor = when {
                        i == state.input.length -> ColorAccent
                        ch.isNotEmpty() -> ColorText
                        else -> ColorDivider
                    }
                    Box(
                        modifier = Modifier.size(width = 50.dp, height = 70.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Column {
                            Text(ch, fontSize = 56.sp, fontFamily = MaterialTheme.typography.headlineLarge.fontFamily, textAlign = TextAlign.Center, modifier = Modifier.size(width = 50.dp, height = 62.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(lineColor))
                        }
                    }
                }
            }
            Text(
                hint.text,
                style = MaterialTheme.typography.bodySmall,
                color = toneColor(hint.tone),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        val keys = keyOrder(state.settings.order) + listOf("clear", "0", "back")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(keys) { key ->
                OutlinedButton(
                    onClick = { viewModel.press(key) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.3f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (key == "back") Color.Transparent else ColorDivider),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorText),
                ) {
                    when {
                        key == "back" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                        key == "clear" -> Text("Clear", fontSize = 13.sp)
                        else -> Text(key, fontSize = 28.sp, fontFamily = MaterialTheme.typography.headlineMedium.fontFamily)
                    }
                }
            }
        }

        val canLog = state.input.length >= 3
        Button(
            onClick = viewModel::logCar,
            enabled = canLog,
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (canLog) Accent100 else Color.Transparent, contentColor = if (canLog) Accent700 else Neutral500),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (canLog) ColorAccent else ColorDivider),
        ) {
            Text(if (canLog) "Log car ${state.input}" else "Log car", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LedgerContent(state: CarLogUiState, viewModel: CarLogViewModel) {
    val hint = describeInput(state.input, entriesByCar(state.entries), state.settings.hints, state.now)
    val todayStart = java.util.Calendar.getInstance().apply {
        timeInMillis = state.now
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val today = state.entries.filter { it.ts >= todayStart }.sortedBy { it.ts }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text("TIME", modifier = Modifier.width(48.dp), style = MaterialTheme.typography.labelSmall, color = Neutral700)
            Text("CAR", modifier = Modifier.width(88.dp), style = MaterialTheme.typography.labelSmall, color = Neutral700)
            Text("STOCK · NEAR", style = MaterialTheme.typography.labelSmall, color = Neutral700)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorDivider))
        if (today.isEmpty()) {
            Text("Nothing logged today yet.", style = MaterialTheme.typography.bodySmall, color = Neutral700, modifier = Modifier.padding(vertical = 16.dp))
        }
        today.takeLast(6).forEach { e ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.openDetail(e.car) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(formatHm(e.ts), modifier = Modifier.width(48.dp), style = MaterialTheme.typography.labelSmall, color = Neutral700)
                Text(e.car, modifier = Modifier.width(88.dp), style = MaterialTheme.typography.headlineSmall)
                Column {
                    Text(stockShort(e.car), style = MaterialTheme.typography.bodySmall)
                    Text(placeText(e), style = MaterialTheme.typography.labelSmall, color = Neutral700, maxLines = 1)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatHm(state.now), modifier = Modifier.width(48.dp), style = MaterialTheme.typography.labelSmall, color = Accent700)
            Text(state.input, modifier = Modifier.width(88.dp), style = MaterialTheme.typography.headlineMedium)
            Text(hint.text.ifEmpty { "Type a car number" }, style = MaterialTheme.typography.labelSmall, color = toneColor(hint.tone))
        }
        LocationRow(state, viewModel)

        val keyRows = keyOrder(state.settings.order).chunked(3)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(modifier = Modifier.weight(3f)) {
                keyRows.forEach { rowKeys ->
                    Row {
                        rowKeys.forEach { key ->
                            OutlinedButton(
                                onClick = { viewModel.press(key) },
                                modifier = Modifier.weight(1f).aspectRatio(1.6f),
                                shape = RoundedCornerShape(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorText),
                            ) { Text(key, fontSize = 24.sp, fontFamily = MaterialTheme.typography.headlineMedium.fontFamily) }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.press("0") },
                    modifier = Modifier.fillMaxWidth().aspectRatio(4.8f),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorText),
                ) { Text("0", fontSize = 24.sp, fontFamily = MaterialTheme.typography.headlineMedium.fontFamily) }
            }
            Column(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = viewModel::backspace, modifier = Modifier.fillMaxWidth().aspectRatio(1.1f), shape = RoundedCornerShape(0.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                }
                OutlinedButton(onClick = viewModel::clearInput, modifier = Modifier.fillMaxWidth().aspectRatio(1.1f), shape = RoundedCornerShape(0.dp)) {
                    Text("Clear", fontSize = 12.sp)
                }
                val canLog = state.input.length >= 3
                Button(
                    onClick = viewModel::logCar,
                    enabled = canLog,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.1f),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = if (canLog) Accent700 else Neutral500),
                ) { Icon(Icons.Filled.Check, contentDescription = "Log") }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PlateContent(state: CarLogUiState, viewModel: CarLogViewModel) {
    val hint = describeInput(state.input, entriesByCar(state.entries), state.settings.hints, state.now)
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(6.dp, ColorSurface)
                .border(width = 1.dp, color = ColorDivider)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CAR NUMBER", style = MaterialTheme.typography.labelSmall, color = ColorAccent)
                Text("${state.input.length} / 5", style = MaterialTheme.typography.labelSmall, color = Neutral700)
            }
            Box(modifier = Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
                Text(
                    state.input.ifEmpty { "—" },
                    fontSize = 72.sp,
                    fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
                    color = if (state.input.isEmpty()) Neutral400 else ColorText,
                )
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorDivider))
            Text(
                hint.text,
                style = MaterialTheme.typography.bodySmall,
                color = toneColor(hint.tone),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatHm(state.now), style = MaterialTheme.typography.labelMedium, color = Neutral800)
            Row(
                modifier = Modifier
                    .background(Accent100, RoundedCornerShape(4.dp))
                    .clickable { viewModel.refreshLocationFix() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val locLabel = when (val s = state.locationStatus) {
                    is LocationStatus.Available -> formatAgo(state.now, s.fix.atMillis)
                    else -> "No location"
                }
                Text(locLabel, style = MaterialTheme.typography.labelSmall, color = Accent700)
            }
            Box(modifier = Modifier.weight(1f))
            OutlinedButton(onClick = viewModel::clearInput) { Text("Clear") }
        }
        val keys = keyOrder(state.settings.order) + listOf("back", "0", "log")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(keys) { key ->
                val canLog = state.input.length >= 3
                val enabled = key != "log" || canLog
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(1.dp, if (key == "back") Color.Transparent else if (key == "log" && canLog) ColorAccent else ColorDivider, CircleShape)
                        .clickable(enabled = enabled) { if (key == "log") viewModel.logCar() else viewModel.press(key) },
                    contentAlignment = Alignment.Center,
                ) {
                    when (key) {
                        "back" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                        "log" -> Icon(Icons.Filled.Check, contentDescription = "Log", tint = if (canLog) Accent700 else Neutral500)
                        else -> Text(key, fontSize = 28.sp, fontFamily = MaterialTheme.typography.headlineMedium.fontFamily)
                    }
                }
            }
        }
    }
}

