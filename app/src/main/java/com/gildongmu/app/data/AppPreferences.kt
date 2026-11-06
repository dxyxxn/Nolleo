package com.gildongmu.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gildongmu.app.core.SourceMode
import com.gildongmu.app.core.log.AppLog
import com.gildongmu.app.core.speech.AnswerInput
import com.gildongmu.app.core.speech.SpeechMode
import com.gildongmu.app.domain.drowsy.DriverBaseline
import com.gildongmu.app.domain.engine.SessionSnapshot
import com.gildongmu.app.domain.model.GameChoice
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gildongmu")

/** 앱 설정 (SettingsRepo) */
data class AppSettings(
    val demoMode: Boolean = false,
    val carMode: SourceMode = SourceMode.AUTO,
    val tripMode: SourceMode = SourceMode.AUTO,
    val locationMode: SourceMode = SourceMode.AUTO,
    val speechMode: SpeechMode = SpeechMode.AUTO,
    val answerInput: AnswerInput = AnswerInput.MIC,
    val simTimeScale: Float = 30f,
    val lastPresetId: String? = null,
    val lastDriverId: Int = 1,
    val lastPassengerCount: Int = 1,
    val lastChoice: GameChoice = GameChoice.ALTERNATE,
)

/**
 * DataStore(Preferences) — 설정(SettingsRepo), 운전자별 졸음 기준값(ProfileRepo), 진행 중 세션 저장본(SessionStore).
 * 네트워크·외부 저장 없음. allowBackup=false 로 백업에서도 제외된다.
 */
class AppPreferences(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val DEMO = booleanPreferencesKey("demo_mode")
        val CAR = stringPreferencesKey("car_mode")
        val TRIP = stringPreferencesKey("trip_mode")
        val LOCATION = stringPreferencesKey("location_mode")
        val SPEECH = stringPreferencesKey("speech_mode")
        val ANSWER = stringPreferencesKey("answer_input")
        val SCALE = floatPreferencesKey("sim_time_scale")
        val PRESET = stringPreferencesKey("last_preset")
        val DRIVER = intPreferencesKey("last_driver")
        val PASSENGERS = intPreferencesKey("last_passengers")
        val CHOICE = stringPreferencesKey("last_choice")
        val SESSION = stringPreferencesKey("session_snapshot")
        fun baseline(driverId: Int) = stringPreferencesKey("baseline_$driverId")
    }

    suspend fun loadSettings(): AppSettings = runCatching {
        val p = store.data.first()
        AppSettings(
            demoMode = p[Keys.DEMO] ?: false,
            carMode = p[Keys.CAR].toEnum(SourceMode.AUTO),
            tripMode = p[Keys.TRIP].toEnum(SourceMode.AUTO),
            locationMode = p[Keys.LOCATION].toEnum(SourceMode.AUTO),
            speechMode = p[Keys.SPEECH].toEnum(SpeechMode.AUTO),
            answerInput = p[Keys.ANSWER].toEnum(AnswerInput.MIC),
            simTimeScale = p[Keys.SCALE] ?: 30f,
            lastPresetId = p[Keys.PRESET],
            lastDriverId = p[Keys.DRIVER] ?: 1,
            lastPassengerCount = p[Keys.PASSENGERS] ?: 1,
            lastChoice = p[Keys.CHOICE].toEnum(GameChoice.ALTERNATE),
        )
    }.onFailure { AppLog.w(TAG, "settings read failed", it) }.getOrDefault(AppSettings())

    suspend fun saveSettings(s: AppSettings) {
        runCatching {
            store.edit { p ->
                p[Keys.DEMO] = s.demoMode
                p[Keys.CAR] = s.carMode.name
                p[Keys.TRIP] = s.tripMode.name
                p[Keys.LOCATION] = s.locationMode.name
                p[Keys.SPEECH] = s.speechMode.name
                p[Keys.ANSWER] = s.answerInput.name
                p[Keys.SCALE] = s.simTimeScale
                s.lastPresetId?.let { p[Keys.PRESET] = it }
                p[Keys.DRIVER] = s.lastDriverId
                p[Keys.PASSENGERS] = s.lastPassengerCount
                p[Keys.CHOICE] = s.lastChoice.name
            }
        }.onFailure { AppLog.w(TAG, "settings write failed", it) }
    }

    suspend fun loadBaseline(driverId: Int): DriverBaseline? = runCatching {
        SnapshotCodec.decodeBaseline(store.data.first()[Keys.baseline(driverId)])
    }.getOrNull()

    suspend fun saveBaseline(driverId: Int, baseline: DriverBaseline?) {
        runCatching {
            store.edit { p ->
                if (baseline == null) p.remove(Keys.baseline(driverId)) else p[Keys.baseline(driverId)] = SnapshotCodec.encodeBaseline(baseline)
            }
        }
    }

    suspend fun loadSession(): SessionSnapshot? = runCatching {
        store.data.first()[Keys.SESSION]?.let { SnapshotCodec.decode(it) }
    }.getOrNull()

    suspend fun saveSession(snapshot: SessionSnapshot?) {
        runCatching {
            store.edit { p -> if (snapshot == null) p.remove(Keys.SESSION) else p[Keys.SESSION] = SnapshotCodec.encode(snapshot) }
        }.onFailure { AppLog.w(TAG, "session write failed", it) }
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        this?.let { name -> runCatching { enumValueOf<E>(name) }.getOrNull() } ?: default

    companion object {
        private const val TAG = "Prefs"
    }
}
