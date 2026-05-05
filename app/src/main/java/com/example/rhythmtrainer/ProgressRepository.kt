import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("rhythm_trainer")

class ProgressRepository(private val context: Context) {

    // Ключи
    private val completedLevelsKey = stringSetPreferencesKey("completed_levels")
    private val totalScoreKey = intPreferencesKey("total_score")
    private val calibrationOffsetKey = intPreferencesKey("calibration_offset")
    private val soundEnabledKey = intPreferencesKey("sound_enabled") // 1/0
    private val vibrationEnabledKey = intPreferencesKey("vibration_enabled")

    // Сохранить пройденный уровень
    suspend fun saveCompletedLevel(levelId: String) {
        context.dataStore.edit { prefs ->
            val currentSet = prefs[completedLevelsKey]?.toMutableSet() ?: mutableSetOf()
            currentSet.add(levelId)
            prefs[completedLevelsKey] = currentSet
        }
    }

    // Получить список пройденных уровней
    fun getCompletedLevels(): Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[completedLevelsKey] ?: emptySet() }

    // Добавить очки к общему счёту
    suspend fun addToTotalScore(addedScore: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[totalScoreKey] ?: 0
            prefs[totalScoreKey] = current + addedScore
        }
    }

    // Получить общий счёт
    fun getTotalScore(): Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[totalScoreKey] ?: 0 }

    // Сохранить настройки
    suspend fun saveSettings(calibrationOffset: Int, soundEnabled: Boolean, vibrationEnabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[calibrationOffsetKey] = calibrationOffset
            prefs[soundEnabledKey] = if (soundEnabled) 1 else 0
            prefs[vibrationEnabledKey] = if (vibrationEnabled) 1 else 0
        }
    }

    // Загрузить настройки (отдельные Flow или один объект)
    fun getSettings(): Flow<Triple<Int, Boolean, Boolean>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[calibrationOffsetKey] ?: 0,
            (prefs[soundEnabledKey] ?: 1) == 1,
            (prefs[vibrationEnabledKey] ?: 1) == 1
        )
    }
}