package com.keithstack.carlog.ui

import com.keithstack.carlog.data.Sighting
import com.keithstack.carlog.data.StockRange
import com.keithstack.carlog.data.exactStock
import com.keithstack.carlog.data.prefixMatches

enum class HintTone { Neutral700, Neutral800, Accent700 }
data class HintResult(val text: String, val tone: HintTone)

/** Direct port of the original app's `describe()` — live hint text as the user types a car number. */
fun describeInput(input: String, entriesByCar: Map<String, List<Sighting>>, withHints: Boolean, now: Long): HintResult {
    if (input.isEmpty()) return HintResult("Type the number painted on the car end", HintTone.Neutral700)

    val seen = entriesByCar[input].orEmpty()
    val seenTxt = if (seen.isNotEmpty()) {
        val times = if (seen.size == 1) "once" else "${seen.size} times"
        "seen $times, last ${formatAgo(now, seen[0].ts)}"
    } else {
        "first sighting"
    }
    fun cap(t: String) = t.replaceFirstChar { it.uppercase() }

    val inputNum = input.toLongOrNull()
    val ex: StockRange? = if (input.length >= 3 && inputNum != null) exactStock(inputNum.toInt()) else null
    val longer = if (input.length < 5 && inputNum != null) {
        prefixMatches(input).filter { it != ex && it.ranges.any { r -> r.last >= inputNum * 10 } }
    } else emptyList()

    if (ex != null) {
        var hint = if (withHints) "${ex.name} · ${ex.lines} · $seenTxt" else cap(seenTxt)
        if (withHints && longer.isNotEmpty()) hint += ". Keep typing for ${longer[0].name}."
        val tone = if (seen.isNotEmpty()) HintTone.Neutral800 else HintTone.Accent700
        return HintResult(hint, tone)
    }
    if (!withHints) {
        val text = if (input.length >= 3) cap(seenTxt) else ""
        return HintResult(text, HintTone.Neutral700)
    }
    val matches = prefixMatches(input)
    if (matches.isEmpty()) {
        val text = if (input.length >= 3) "Not in the fleet list. You can still log it." else "No stock numbers start with $input"
        return HintResult(text, HintTone.Accent700)
    }
    val names = matches.map { "${it.name} (${it.lines.replace(" line", "")})" }.distinct()
    val text = if (names.size <= 2) "Could be " + names.joinToString(" or ") else "${names.size} stock types match. Keep typing."
    return HintResult(text, HintTone.Neutral700)
}
