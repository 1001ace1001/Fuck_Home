/*
 * StatusBarLyric
 * Copyright (C) 2021-2022 fkj@fkj233.cn
 * https://github.com/577fkj/StatusBarLyric
 *
 * This software is free opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by 577fkj.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and eula along with this software. If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/577fkj/StatusBarLyric/blob/main/LICENSE>.
 */
package cn.fuckhome.xiaowine.utils

import android.content.SharedPreferences

class ConfigUtils {
    private val preferencesProvider: () -> SharedPreferences?
    private val writablePreferences: SharedPreferences?
    constructor(sharedPreferences: SharedPreferences) {
        preferencesProvider = { sharedPreferences }
        writablePreferences = sharedPreferences
    }
    constructor(provider: () -> SharedPreferences?) {
        preferencesProvider = provider
        writablePreferences = null
    }
    // API 102 remote preferences update themselves; resolve provider on every read.
    fun update() = Unit
    fun put(key: String?, any: Any) {
        requireNotNull(key) { "Preference key must not be null" }
        val prefs = writablePreferences ?: error("Hook configuration is read-only")
        val editor = prefs.edit()
        putValue(editor, key, any)
        editor.apply()
        ConfigSharing.sync()
    }
    fun optInt(key: String, i: Int): Int = runCatching { preferencesProvider()?.getInt(key, i) ?: i }.getOrDefault(i)
    fun optBoolean(key: String, bool: Boolean): Boolean = runCatching { preferencesProvider()?.getBoolean(key, bool) ?: bool }.getOrDefault(bool)
    fun optString(key: String, str: String): String = runCatching { preferencesProvider()?.getString(key, str) ?: str }.getOrDefault(str)
    fun optFloat(key: String, f: Float): Float = runCatching { preferencesProvider()?.getFloat(key, f) ?: f }.getOrDefault(f)
    fun clearConfig() {
        val prefs = writablePreferences ?: error("Hook configuration is read-only")
        prefs.edit().clear().apply()
        // Pre-Android 11 may not notify listeners on clear().
        ConfigSharing.sync()
    }
    companion object {
        internal fun putValue(editor: SharedPreferences.Editor, key: String, value: Any?) {
            when (value) {
                null -> editor.remove(key)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> {
                    require(value.all { it is String }) { "Only string sets are supported" }
                    editor.putStringSet(key, value.map { it as String }.toSet())
                }
                else -> error("Unsupported preference value: ${value.javaClass.name}")
            }
        }
    }
}
