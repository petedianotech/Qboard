package com.example.keyboard

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class KeyboardPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)

    private val _themeColor = MutableStateFlow(prefs.getInt("themeColor", 0)) // 0: Auto, 1: Dark, 2: Light
    val themeColor: StateFlow<Int> = _themeColor

    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean("vibrationEnabled", true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean("soundEnabled", false))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled

    private val _autoCapsEnabled = MutableStateFlow(prefs.getBoolean("autoCapsEnabled", true))
    val autoCapsEnabled: StateFlow<Boolean> = _autoCapsEnabled

    private val _keyboardLayout = MutableStateFlow(prefs.getInt("keyboardLayout", 0)) // 0: QWERTY, 1: AZERTY, 2: QWERTZ
    val keyboardLayout: StateFlow<Int> = _keyboardLayout

    private val _predictionEnabled = MutableStateFlow(prefs.getBoolean("predictionEnabled", true))
    val predictionEnabled: StateFlow<Boolean> = _predictionEnabled

    private val _cerebrasApiKey = MutableStateFlow(prefs.getString("cerebrasApiKey", "") ?: "")
    val cerebrasApiKey: StateFlow<String> = _cerebrasApiKey

    private val _aiMood = MutableStateFlow(prefs.getInt("aiMood", 0)) // 0: Normal, 1: Professional, 2: Casual, 3: Friendly
    val aiMood: StateFlow<Int> = _aiMood

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "themeColor" -> _themeColor.value = prefs.getInt(key, 0)
            "vibrationEnabled" -> _vibrationEnabled.value = prefs.getBoolean(key, true)
            "soundEnabled" -> _soundEnabled.value = prefs.getBoolean(key, false)
            "autoCapsEnabled" -> _autoCapsEnabled.value = prefs.getBoolean(key, true)
            "keyboardLayout" -> _keyboardLayout.value = prefs.getInt(key, 0)
            "predictionEnabled" -> _predictionEnabled.value = prefs.getBoolean(key, true)
            "cerebrasApiKey" -> _cerebrasApiKey.value = prefs.getString(key, "") ?: ""
            "aiMood" -> _aiMood.value = prefs.getInt(key, 0)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setThemeColor(theme: Int) = prefs.edit().putInt("themeColor", theme).apply()
    fun setVibrationEnabled(enabled: Boolean) = prefs.edit().putBoolean("vibrationEnabled", enabled).apply()
    fun setSoundEnabled(enabled: Boolean) = prefs.edit().putBoolean("soundEnabled", enabled).apply()
    fun setAutoCapsEnabled(enabled: Boolean) = prefs.edit().putBoolean("autoCapsEnabled", enabled).apply()
    fun setKeyboardLayout(layout: Int) = prefs.edit().putInt("keyboardLayout", layout).apply()
    fun setPredictionEnabled(enabled: Boolean) = prefs.edit().putBoolean("predictionEnabled", enabled).apply()
    fun setCerebrasApiKey(key: String) = prefs.edit().putString("cerebrasApiKey", key).apply()
    fun setAiMood(mood: Int) = prefs.edit().putInt("aiMood", mood).apply()

    companion object {
        @Volatile
        private var INSTANCE: KeyboardPreferences? = null

        fun getInstance(context: Context): KeyboardPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KeyboardPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
