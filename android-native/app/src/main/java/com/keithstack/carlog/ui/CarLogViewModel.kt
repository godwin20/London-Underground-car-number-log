package com.keithstack.carlog.ui

import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keithstack.carlog.auth.AuthRepository
import com.keithstack.carlog.data.CarNotesRepository
import com.keithstack.carlog.data.KeypadOrder
import com.keithstack.carlog.data.LogStyle
import com.keithstack.carlog.data.STATIONS
import com.keithstack.carlog.data.Sighting
import com.keithstack.carlog.data.SettingsRepository
import com.keithstack.carlog.data.SightingsRepository
import com.keithstack.carlog.data.exactStock
import com.keithstack.carlog.data.nearestStation
import com.keithstack.carlog.location.LocationStatus
import com.keithstack.carlog.location.LocationTracker
import com.keithstack.carlog.update.ApkInstaller
import com.keithstack.carlog.update.UpdateCheckResult
import com.keithstack.carlog.update.UpdateChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

const val APP_VERSION = "1.9"

class CarLogViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val sightingsRepository = SightingsRepository()
    private val carNotesRepository = CarNotesRepository()
    private val settingsRepository = SettingsRepository(application)
    private val locationTracker = LocationTracker(application)
    val apkInstaller = ApkInstaller(application)

    private val _state = MutableStateFlow(CarLogUiState())
    val state: StateFlow<CarLogUiState> = _state.asStateFlow()

    private var uid: String? = null
    private var snackJob: Job? = null
    private var sightingsJob: Job? = null
    private var carNotesJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            locationTracker.status.collect { status ->
                _state.update { it.copy(locationStatus = status) }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                _state.update { it.copy(now = System.currentTimeMillis()) }
            }
        }
        viewModelScope.launch {
            authRepository.accountState.collect { account ->
                _state.update { it.copy(account = account) }
            }
        }
        viewModelScope.launch {
            val resolvedUid = authRepository.ensureSignedIn()
            subscribeToSightings(resolvedUid)
        }
        checkForUpdate()
    }

    private fun subscribeToSightings(resolvedUid: String) {
        uid = resolvedUid
        sightingsJob?.cancel()
        sightingsJob = viewModelScope.launch {
            sightingsRepository.observeSightings(resolvedUid).collect { entries ->
                _state.update { it.copy(entries = entries) }
            }
        }
        carNotesJob?.cancel()
        carNotesJob = viewModelScope.launch {
            carNotesRepository.observeNotes(resolvedUid).collect { notes ->
                _state.update { it.copy(carNotes = notes) }
            }
        }
    }

    fun setCarNote(car: String, text: String) {
        viewModelScope.launch {
            val resolvedUid = uid ?: authRepository.ensureSignedIn().also { uid = it }
            carNotesRepository.setNote(resolvedUid, car, text)
        }
    }

    // ---- Google Sign-In ----

    fun handleGoogleSignInToken(idToken: String?) {
        if (idToken == null) {
            _state.update { it.copy(signInError = "Google sign-in didn't return a token") }
            return
        }
        viewModelScope.launch {
            val result = authRepository.linkGoogleIdToken(idToken)
            result.onSuccess {
                subscribeToSightings(authRepository.ensureSignedIn())
                _state.update { it.copy(signInError = null) }
            }.onFailure { e ->
                _state.update { it.copy(signInError = e.message ?: "Google sign-in failed") }
            }
        }
    }

    fun signOutGoogle() {
        authRepository.signOut()
        viewModelScope.launch { subscribeToSightings(authRepository.ensureSignedIn()) }
    }

    fun startLocationTracking() = locationTracker.start()
    fun refreshLocationFix() = locationTracker.refresh()

    override fun onCleared() {
        super.onCleared()
        locationTracker.stop()
    }

    // ---- keypad / logging ----

    fun press(key: String) {
        buzz(8)
        _state.update { s ->
            val cur = s.input
            val next = when {
                key == "back" -> cur.dropLast(1)
                key == "clear" -> ""
                cur.length >= 5 || (cur.isEmpty() && key == "0") -> cur
                else -> cur + key
            }
            s.copy(input = next)
        }
    }

    fun clearInput() = press("clear")
    fun backspace() = press("back")

    fun logCar() {
        val s = _state.value
        val car = s.input
        if (car.length < 3) return
        val fix = (s.locationStatus as? LocationStatus.Available)?.fix
        val sighting = Sighting(
            id = "e" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().take(4),
            car = car,
            ts = System.currentTimeMillis(),
            lat = fix?.lat,
            lon = fix?.lon,
            acc = fix?.accuracyMeters,
            fixAt = fix?.atMillis,
            near = fix?.let { nearestStation(it.lat, it.lon) },
        )
        buzz(25)
        viewModelScope.launch {
            val resolvedUid = uid ?: authRepository.ensureSignedIn().also { uid = it }
            sightingsRepository.addSighting(resolvedUid, sighting)
        }
        _state.update { it.copy(input = "") }
        showSnack(SnackState(text = "Car $car logged", car = car, removeId = sighting.id))
    }

    // ---- snackbar ----

    private fun showSnack(snack: SnackState) {
        snackJob?.cancel()
        _state.update { it.copy(snack = snack) }
        snackJob = viewModelScope.launch {
            delay(5_000)
            _state.update { it.copy(snack = null) }
        }
    }

    fun dismissSnack() {
        snackJob?.cancel()
        _state.update { it.copy(snack = null) }
    }

    fun snackOpen() {
        val car = _state.value.snack?.car
        dismissSnack()
        if (car != null) openDetail(car)
    }

    fun undo() {
        val snack = _state.value.snack ?: return
        snackJob?.cancel()
        _state.update { it.copy(snack = null) }
        viewModelScope.launch {
            val resolvedUid = uid ?: authRepository.ensureSignedIn().also { uid = it }
            when {
                snack.removeId != null -> {
                    sightingsRepository.deleteSighting(resolvedUid, snack.removeId)
                    _state.update { it.copy(input = snack.car ?: it.input) }
                }
                snack.restore != null -> sightingsRepository.restoreSighting(resolvedUid, snack.restore)
            }
        }
    }

    // ---- navigation ----

    fun go(screen: Screen) {
        _state.update { it.copy(screen = screen) }
    }

    fun openDetail(car: String) {
        _state.update { it.copy(previousScreen = it.screen, screen = Screen.Detail, detailCar = car) }
    }

    fun goBack() {
        _state.update { it.copy(screen = it.previousScreen, detailCar = null) }
    }

    fun logAgainFromDetail() {
        val car = _state.value.detailCar ?: return
        _state.update { it.copy(input = car.take(5), screen = Screen.Log, detailCar = null) }
    }

    fun deleteSighting(id: String) {
        val deleted = _state.value.entries.find { it.id == id } ?: return
        viewModelScope.launch {
            val resolvedUid = uid ?: authRepository.ensureSignedIn().also { uid = it }
            sightingsRepository.deleteSighting(resolvedUid, id)
        }
        showSnack(SnackState(text = "Sighting of ${deleted.car} deleted", restore = deleted))
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query.filter { c -> c.isDigit() }.take(5)) }
    }

    fun setHistoryByCar(byCar: Boolean) {
        _state.update { it.copy(historyByCar = byCar) }
    }

    // ---- settings ----

    fun setStyle(style: LogStyle) = viewModelScope.launch { settingsRepository.setStyle(style) }
    fun setOrder(order: KeypadOrder) = viewModelScope.launch { settingsRepository.setOrder(order) }
    fun setHints(enabled: Boolean) = viewModelScope.launch { settingsRepository.setHints(enabled) }
    fun setVibrate(enabled: Boolean) = viewModelScope.launch { settingsRepository.setVibrate(enabled) }

    fun addSample() {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        val day = 86_400_000L
        data class Row(val car: String, val agoMs: Long, val station: String)
        val rows = listOf(
            Row("91123", 9 * day + 200 * minute, "Holborn"),
            Row("91123", 3 * day + 50 * minute, "Bank"),
            Row("96042", day + 30 * minute, "Baker Street"),
            Row("96243", day + 29 * minute, "Baker Street"),
            Row("21087", 12 * day, "Baker Street"),
            Row("3242", 20 * day, "Oxford Circus"),
            Row("11034", 5 * day, "Brixton"),
            Row("51523", 30 * day, "Camden Town"),
            Row("51523", 95 * minute, "Camden Town"),
            Row("52523", 94 * minute, "Camden Town"),
        )
        val samples = rows.mapIndexed { i, row ->
            val station = STATIONS.find { it.name == row.station }
            Sighting(
                id = "x$now$i",
                car = row.car,
                ts = now - row.agoMs,
                lat = station?.lat,
                lon = station?.lon,
                acc = 12,
                fixAt = now - row.agoMs - (3 + i) * minute,
                near = row.station,
                sample = true,
            )
        }
        viewModelScope.launch {
            val resolvedUid = uid ?: authRepository.ensureSignedIn().also { uid = it }
            sightingsRepository.addSample(resolvedUid, samples)
        }
    }

    fun askWipe() = _state.update { it.copy(wipeOpen = true) }
    fun closeWipe() = _state.update { it.copy(wipeOpen = false) }
    fun wipe() {
        _state.update { it.copy(wipeOpen = false) }
        viewModelScope.launch {
            val resolvedUid = uid ?: authRepository.ensureSignedIn().also { uid = it }
            sightingsRepository.wipeAll(resolvedUid)
        }
    }

    // ---- CSV export ----

    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK)

    /** Writes the CSV to the app's cache dir and returns a shareable content:// URI. */
    fun exportCsv(): Uri {
        fun quote(v: String?): String {
            if (v == null) return ""
            return if (Regex("[\",\n]").containsMatchIn(v)) "\"" + v.replace("\"", "\"\"") + "\"" else v
        }
        val lines = mutableListOf("car,stock,date,time,latitude,longitude,accuracy_m,fix_age_min,near_station")
        _state.value.entries.sortedBy { it.ts }.forEach { e ->
            val stock = e.car.toIntOrNull()?.let { exactStock(it) }
            val fixAgeMin = e.fixAt?.let { Math.round((e.ts - it) / 60_000.0).toString() } ?: ""
            val row = listOf(
                e.car,
                stock?.name ?: "",
                csvDateFormat.format(e.ts),
                formatHm(e.ts),
                e.lat?.let { "%.5f".format(it) },
                e.lon?.let { "%.5f".format(it) },
                e.acc?.toString(),
                fixAgeMin,
                e.near,
            )
            lines.add(row.joinToString(",") { quote(it) })
        }
        val context = getApplication<Application>()
        val fileName = "car-log-${csvDateFormat.format(System.currentTimeMillis())}.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(lines.joinToString("\n"))
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // ---- updates ----

    fun checkForUpdate() {
        _state.update { it.copy(update = it.update.copy(checking = true, error = null)) }
        viewModelScope.launch {
            when (val result = UpdateChecker.checkForUpdate(APP_VERSION)) {
                is UpdateCheckResult.Available -> _state.update {
                    it.copy(update = UpdateUiState(checking = false, available = true, version = result.info.version, downloadUrl = result.info.downloadUrl))
                }
                is UpdateCheckResult.UpToDate -> _state.update {
                    it.copy(update = UpdateUiState(checking = false, available = false))
                }
                is UpdateCheckResult.Error -> _state.update {
                    it.copy(update = it.update.copy(checking = false, error = result.message))
                }
            }
        }
    }

    fun downloadUpdate() {
        val update = _state.value.update
        val url = update.downloadUrl ?: return
        apkInstaller.downloadAndInstall(url, update.version ?: "update")
    }

    // ---- vibration ----

    private fun buzz(durationMs: Long) {
        if (!_state.value.settings.vibrate) return
        val context = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}
