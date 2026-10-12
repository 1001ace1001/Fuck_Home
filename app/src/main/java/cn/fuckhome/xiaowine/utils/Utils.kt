package cn.fuckhome.xiaowine.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.SystemClock
import android.text.format.Formatter
import android.util.Log
import android.widget.TextView
import cn.fuckhome.xiaowine.config.Config
import cn.fuckhome.xiaowine.hook.module.add.Info

object Utils {
    const val CONFIG_GROUP = "Fuck_Home_Config"
    @Volatile private var remotePreferences: SharedPreferences? = null
    // No target-private preference fallback, and no cached initial null backend.
    val XConfig: Config by lazy { Config { remotePreferences } }
    fun installRemotePreferences(preferences: SharedPreferences) { remotePreferences = preferences }
    fun getPref(key: String?): SharedPreferences? = if (key == CONFIG_GROUP) remotePreferences else null
    fun getSP(context: Context, key: String?): SharedPreferences =
        context.applicationContext.createDeviceProtectedStorageContext()
            .getSharedPreferences(requireNotNull(key), Context.MODE_PRIVATE)
    fun viewColor(view: TextView, info: MemoryUtils?) {
        if (info.isNotNull() && XConfig.getBoolean("Warning") && info!!.percentValue < Info.threshold) {
            view.setTextColor(Color.RED)
        } else if (XConfig.getColor().isEmpty()) {
            view.setTextColor(Info.textColors)
        } else {
            view.setTextColor(Color.parseColor(XConfig.getColor()))
        }
    }
    object Uptime {
        fun get(): String {
            val time = SystemClock.elapsedRealtime() / 1000
            var temp: Int
            val sb = StringBuffer()
            if (time > 3600) {
                temp = (time / 3600).toInt()
                sb.append(if (time / 3600 < 10) "0$temp:" else "$temp:")
                temp = (time % 3600 / 60).toInt()
                changeSeconds(time, temp, sb)
            } else {
                temp = (time % 3600 / 60).toInt()
                changeSeconds(time, temp, sb)
            }
            return sb.toString()
        }
        private fun changeSeconds(seconds: Long, temp: Int, sb: StringBuffer) {
            var temps = temp
            sb.append(if (temps < 10) "0$temps:" else "$temps:")
            temps = (seconds % 3600 % 60).toInt()
            sb.append(if (temps < 10) "0$temps" else "" + temps)
        }
    }
    fun catchNoClass(callback: () -> Unit) {
        runCatching { callback() }.exceptionOrNull()?.let {
            Log.i(LogUtils.TAG, "${callback.javaClass.simpleName}错误", it)
        }
    }
    fun Any.formatSize(): String = Formatter.formatFileSize(ModernXposed.appContext, this as Long)
    fun Any?.isNull(callback: () -> Unit) { if (this == null) callback() }
    fun Any?.isNotNull(callback: () -> Unit) { if (this != null) callback() }
    fun Any?.isNull() = this == null
    fun Any?.isNotNull() = this != null
}
