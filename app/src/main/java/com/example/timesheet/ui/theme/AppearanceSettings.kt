package com.example.timesheet.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val label: String) {
    AUTO("Auto"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class TextSize(val label: String, val scale: Float) {
    STANDARD("Standard", 1.0f),
    LARGE("Large", 1.2f),
    EXTRA_LARGE("Biggest", 1.4f)
}

/**
 * Look-and-feel choices. Backed by Compose state so the whole app restyles the moment one changes,
 * and saved in SharedPreferences. Call [load] before setContent.
 */
object AppearanceSettings {
    private const val FILE = "settings"

    var themeMode by mutableStateOf(ThemeMode.AUTO)
        private set

    // Big by default: this app is for people who don't want to hunt for their glasses.
    var textSize by mutableStateOf(TextSize.LARGE)
        private set

    fun load(context: Context) {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        themeMode = ThemeMode.entries.firstOrNull { it.name == p.getString("theme_mode", null) } ?: ThemeMode.AUTO
        textSize = TextSize.entries.firstOrNull { it.name == p.getString("text_size", null) } ?: TextSize.LARGE
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        themeMode = mode
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("theme_mode", mode.name).apply()
    }

    fun setTextSize(context: Context, size: TextSize) {
        textSize = size
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("text_size", size.name).apply()
    }
}
