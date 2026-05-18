package org.caexplorer.data

actual object AppSettings {
    private val map = mutableMapOf<String, Any>()

    actual fun getString(key: String, default: String): String = map[key] as? String ?: default
    actual fun putString(key: String, value: String) { map[key] = value }
    actual fun getInt(key: String, default: Int): Int = map[key] as? Int ?: default
    actual fun putInt(key: String, value: Int) { map[key] = value }
    actual fun getBoolean(key: String, default: Boolean): Boolean = map[key] as? Boolean ?: default
    actual fun putBoolean(key: String, value: Boolean) { map[key] = value }
}
