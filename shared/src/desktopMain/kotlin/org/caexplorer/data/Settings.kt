package org.caexplorer.data

import java.util.prefs.Preferences

actual object AppSettings {
    private val prefs = Preferences.userNodeForPackage(AppSettings::class.java)

    actual fun getString(key: String, default: String): String = prefs.get(key, default)
    actual fun putString(key: String, value: String) = prefs.put(key, value)
    actual fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    actual fun putInt(key: String, value: Int) = prefs.putInt(key, value)
    actual fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    actual fun putBoolean(key: String, value: Boolean) = prefs.putBoolean(key, value)
}
