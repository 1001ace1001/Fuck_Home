package cn.fuckhome.xiaowine.config

import android.content.SharedPreferences
import android.view.Gravity
import cn.fuckhome.xiaowine.utils.ConfigUtils

class Config {
    private val config: ConfigUtils
    constructor(sharedPreferences: SharedPreferences) { config = ConfigUtils(sharedPreferences) }
    constructor(preferencesProvider: () -> SharedPreferences?) { config = ConfigUtils(preferencesProvider) }
    fun update() = config.update()
    fun setColor(str: String) = config.put("Color", str)
    fun getColor(): String = config.optString("Color", "")
    fun setBgColor(str: String) = config.put("BgColor", str)
    fun getBgColor(): String = config.optString("BgColor", "#00000000")
    fun setBgCorners(i: Int) = config.put("BgCorners", i)
    fun getBgCorners(): Int = config.optInt("BgCorners", 30)
    fun setGravity(i: Int) = config.put("Gravity", i)
    fun getGravity(): Int = config.optInt("Gravity", Gravity.START)
    fun setUnit(b: Boolean) = config.put("Unit", b)
    fun getUnit(): Boolean = config.optBoolean("Unit", true)
    fun getString(key: String, def: String = ""): String = config.optString(key, def)
    fun getBoolean(key: String, def: Boolean = false): Boolean = config.optBoolean(key, def)
    fun getInt(key: String, def: Int = 0): Int = config.optInt(key, def)
    fun setValue(key: String, value: Any) = config.put(key, value)
    fun clear() = config.clearConfig()
}
