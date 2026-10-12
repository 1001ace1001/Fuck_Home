package cn.fuckhome.xiaowine.utils

import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.Executors

/** Private UI preferences are authoritative; the service mirrors them for hooks. */
object ConfigSharing {
    private const val TAG = "FuckHomeConfigSharing"
    private val lock = Any()
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "FuckHome-ConfigSync").apply { isDaemon = true }
    }
    private var localPreferences: SharedPreferences? = null
    private var registered = false
    private val services = LinkedHashMap<XposedService, SharedPreferences?>()
    // SharedPreferences listeners must be retained strongly.
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> sync() }
    private val serviceListener = object : XposedServiceHelper.OnServiceListener {
        override fun onServiceBind(service: XposedService) {
            synchronized(lock) { services[service] = null }
            sync()
        }
        override fun onServiceDied(service: XposedService) {
            synchronized(lock) { services.remove(service) }
        }
    }
    fun start(preferences: SharedPreferences) {
        val register = synchronized(lock) {
            if (localPreferences !== preferences) {
                localPreferences?.unregisterOnSharedPreferenceChangeListener(preferenceListener)
                localPreferences = preferences
                preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
            }
            if (registered) false else { registered = true; true }
        }
        // The library requires exactly one registration per app process.
        if (register) XposedServiceHelper.registerListener(serviceListener)
        sync()
    }
    /** Full snapshot, retried on bind, edit and resume; no UI-thread Binder writes. */
    fun sync() {
        worker.execute {
            synchronized(lock) {
                val local = localPreferences ?: return@execute
                // Snapshot at execution time avoids stale queued writes.
                val snapshot = local.all
                for (service in services.keys.toList()) {
                    try {
                        val remote = services[service]
                            ?: service.getRemotePreferences(Utils.CONFIG_GROUP).also { services[service] = it }
                        if (remote.all == snapshot) continue
                        val editor = remote.edit().clear()
                        snapshot.forEach { (key, value) -> ConfigUtils.putValue(editor, key, value) }
                        if (!editor.commit()) {
                            services[service] = null
                            Log.w(TAG, "Remote preference commit failed; local settings retained")
                        }
                    } catch (e: Exception) {
                        services[service] = null
                        Log.w(TAG, "Cannot mirror settings; retry on bind, edit or resume", e)
                    }
                }
            }
        }
    }
}
