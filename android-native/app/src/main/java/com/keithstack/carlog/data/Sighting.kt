package com.keithstack.carlog.data

data class Sighting(
    val id: String = "",
    val car: String = "",
    val ts: Long = 0L,
    val lat: Double? = null,
    val lon: Double? = null,
    val acc: Int? = null,
    val fixAt: Long? = null,
    val near: String? = null,
    val sample: Boolean = false,
)
