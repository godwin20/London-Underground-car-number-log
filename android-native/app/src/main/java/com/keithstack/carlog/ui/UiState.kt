package com.keithstack.carlog.ui

import com.keithstack.carlog.auth.AccountInfo
import com.keithstack.carlog.data.AppSettings
import com.keithstack.carlog.data.Sighting
import com.keithstack.carlog.location.LocationStatus

enum class Screen { Log, History, Detail, Settings }

data class SnackState(
    val text: String,
    val car: String? = null,
    val removeId: String? = null,
    val restore: Sighting? = null,
)

data class UpdateUiState(
    val checking: Boolean = false,
    val available: Boolean = false,
    val version: String? = null,
    val downloadUrl: String? = null,
    val error: String? = null,
)

data class CarLogUiState(
    val entries: List<Sighting> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val locationStatus: LocationStatus = LocationStatus.Idle,
    val now: Long = System.currentTimeMillis(),
    val screen: Screen = Screen.Log,
    val previousScreen: Screen = Screen.Log,
    val detailCar: String? = null,
    val input: String = "",
    val query: String = "",
    val historyByCar: Boolean = false,
    val snack: SnackState? = null,
    val wipeOpen: Boolean = false,
    val update: UpdateUiState = UpdateUiState(),
    val exportCsvUri: String? = null,
    val account: AccountInfo? = null,
    val signInError: String? = null,
)
