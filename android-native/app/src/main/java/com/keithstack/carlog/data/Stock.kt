package com.keithstack.carlog.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class StockRange(val name: String, val lines: String, val ranges: List<IntRange>)

data class Station(val name: String, val lat: Double, val lon: Double)

val STOCK: List<StockRange> = listOf(
    StockRange("1972 Stock", "Bakerloo line", listOf(3200..3299, 3300..3399, 3500..3599, 4200..4399, 4500..4599)),
    StockRange("1973 Stock", "Piccadilly line", listOf(100..699, 800..899)),
    StockRange("1992 Stock", "Central line", listOf(91001..91399, 92001..92399, 93001..93399)),
    StockRange("1992 Stock", "Waterloo & City line", listOf(65500..65599, 67500..67599)),
    StockRange("1995 Stock", "Northern line", listOf(51501..51699, 52501..52699, 53501..53699)),
    StockRange("1996 Stock", "Jubilee line", listOf(96001..96799)),
    StockRange("2009 Stock", "Victoria line", listOf(11001..18099)),
    StockRange("S Stock", "Metropolitan, District, Circle, H&C", listOf(21001..28199)),
)

val STATIONS: List<Station> = listOf(
    Station("Oxford Circus", 51.5152, -0.1415),
    Station("Bond Street", 51.5142, -0.1494),
    Station("Marble Arch", 51.5136, -0.1586),
    Station("Tottenham Court Road", 51.5165, -0.1310),
    Station("Holborn", 51.5174, -0.1200),
    Station("Chancery Lane", 51.5185, -0.1111),
    Station("St Paul’s", 51.5146, -0.0973),
    Station("Bank", 51.5133, -0.0886),
    Station("Liverpool Street", 51.5178, -0.0823),
    Station("Moorgate", 51.5186, -0.0886),
    Station("Aldgate", 51.5143, -0.0755),
    Station("Tower Hill", 51.5098, -0.0766),
    Station("London Bridge", 51.5052, -0.0864),
    Station("Waterloo", 51.5036, -0.1143),
    Station("Embankment", 51.5074, -0.1223),
    Station("Westminster", 51.5010, -0.1254),
    Station("Victoria", 51.4965, -0.1447),
    Station("Green Park", 51.5067, -0.1428),
    Station("Piccadilly Circus", 51.5098, -0.1342),
    Station("Leicester Square", 51.5113, -0.1281),
    Station("Covent Garden", 51.5129, -0.1243),
    Station("King’s Cross St. Pancras", 51.5308, -0.1238),
    Station("Euston", 51.5282, -0.1337),
    Station("Warren Street", 51.5247, -0.1384),
    Station("Baker Street", 51.5226, -0.1571),
    Station("Paddington", 51.5154, -0.1755),
    Station("Notting Hill Gate", 51.5094, -0.1967),
    Station("South Kensington", 51.4941, -0.1738),
    Station("Earl’s Court", 51.4920, -0.1934),
    Station("Hammersmith", 51.4936, -0.2251),
    Station("Camden Town", 51.5392, -0.1426),
    Station("Angel", 51.5322, -0.1058),
    Station("Old Street", 51.5263, -0.0873),
    Station("Stratford", 51.5416, -0.0033),
    Station("Canary Wharf", 51.5035, -0.0187),
    Station("North Greenwich", 51.5005, 0.0039),
    Station("Brixton", 51.4627, -0.1145),
    Station("Stockwell", 51.4723, -0.1228),
    Station("Elephant & Castle", 51.4943, -0.1001),
    Station("Finsbury Park", 51.5642, -0.1065),
    Station("Wembley Park", 51.5635, -0.2795),
    Station("Heathrow Terminals 2 & 3", 51.4713, -0.4524),
    Station("Walthamstow Central", 51.5830, -0.0197),
    Station("Morden", 51.4022, -0.1948),
    Station("Edgware", 51.6137, -0.2750),
    Station("High Barnet", 51.6505, -0.1941),
    Station("Richmond", 51.4633, -0.3014),
    Station("Uxbridge", 51.5463, -0.4786),
    Station("Upminster", 51.5590, 0.2510),
    Station("Ealing Broadway", 51.5152, -0.3017),
    Station("Epping", 51.6937, 0.1139),
    Station("Cockfosters", 51.6517, -0.1496),
    Station("Harrow-on-the-Hill", 51.5793, -0.3366),
    Station("Wimbledon", 51.4214, -0.2064),
)

/** Exact stock match for a fully-typed car number. */
fun exactStock(carNumber: Int): StockRange? =
    STOCK.find { stock -> stock.ranges.any { carNumber in it } }

/** Stock entries whose range could still be reached by typing more digits after [prefix]. */
fun prefixMatches(prefix: String): List<StockRange> {
    if (prefix.isEmpty()) return emptyList()
    val p = prefix.toLong()
    return STOCK.filter { stock ->
        stock.ranges.any { range ->
            (prefix.length..5).any { totalLen ->
                val mult = Math.pow(10.0, (totalLen - prefix.length).toDouble()).toLong()
                val lo = p * mult
                val hi = lo + mult - 1
                lo <= range.last && hi >= range.first
            }
        }
    }
}

fun formatCoords(lat: Double, lon: Double): String {
    val latStr = "%.4f".format(lat)
    val lonStr = "%.4f".format(kotlin.math.abs(lon))
    val hemi = if (lon < 0) "W" else "E"
    return "$latStr° N, $lonStr° $hemi"
}

/** Great-circle distance in kilometres (haversine). */
fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    fun rad(x: Double) = x * Math.PI / 180
    val dLat = rad(lat2 - lat1)
    val dLon = rad(lon2 - lon1)
    val h = sin(dLat / 2).pow(2) + cos(rad(lat1)) * cos(rad(lat2)) * sin(dLon / 2).pow(2)
    return 2 * r * asin(sqrt(h))
}

/** Nearest station within 1.5km, or null if nothing is close enough. */
fun nearestStation(lat: Double, lon: Double): String? {
    var best: String? = null
    var bestDist = Double.MAX_VALUE
    for (station in STATIONS) {
        val d = distanceKm(lat, lon, station.lat, station.lon)
        if (d < bestDist) {
            bestDist = d
            best = station.name
        }
    }
    return if (bestDist <= 1.5) best else null
}

fun stockShort(car: String): String {
    val n = car.toIntOrNull() ?: return "Unknown stock"
    val s = exactStock(n) ?: return "Unknown stock"
    return "${s.name} · ${s.lines.replace(" line", "")}"
}
