package org.caexplorer.data

expect object AppSettings {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

object SettingsKeys {
    const val GRID_WIDTH = "grid_width"
    const val GRID_HEIGHT = "grid_height"
    const val GRID_DEPTH = "grid_depth"
    const val DARK_THEME = "dark_theme"
    const val LAST_RULE_INDEX = "last_rule_index"
    const val LAST_RULE_NAME = "last_rule_name"
    const val LAST_RULE_PROPERTIES = "last_rule_properties"
    const val LAST_COLOR_SCHEME = "last_color_scheme"
    const val SPEED_INDEX = "speed_index"
    const val LATTICE_TYPE = "lattice_type"
    const val INIT_PATTERN = "init_pattern"
    const val APP_PALETTE = "app_palette"
}
