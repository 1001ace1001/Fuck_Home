package cn.fuckhome.xiaowine.hook

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.util.Log
import cn.fuckhome.xiaowine.hook.module.add.Info
import cn.fuckhome.xiaowine.hook.module.modify.*
import cn.fuckhome.xiaowine.utils.ModernXposed
import cn.fuckhome.xiaowine.utils.Utils
import cn.fuckhome.xiaowine.utils.Utils.XConfig
import cn.fuckhome.xiaowine.utils.hookBeforeMethod
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.*

val homeList = arrayOf("com.miui.home", "com.mi.android.globallauncher")

class MainHook : XposedModule() {
    private var initialized = false
    override fun onModuleLoaded(param: ModuleLoadedParam) {
        ModernXposed.module = this
        runCatching { Utils.installRemotePreferences(getRemotePreferences(Utils.CONFIG_GROUP)) }
            .onFailure { log(Log.ERROR, "Fuck_Home", "Remote preferences unavailable", it) }
    }
    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage || !XConfig.getBoolean("MainSwitch")) return
        ModernXposed.classLoader = param.classLoader
        when (param.packageName) {
            in homeList -> Application::class.java.hookBeforeMethod("attach", Context::class.java) {
                if (initialized) return@hookBeforeMethod
                initialized = true
                val context = it.args[0] as Context
                ModernXposed.appContext = context
                runCatching {
                    val assets = AssetManager::class.java.getDeclaredConstructor().newInstance()
                    val cookie = AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
                        .invoke(assets, moduleApplicationInfo.sourceDir) as Int
                    check(cookie != 0) { "Cannot load module resources" }
                    @Suppress("DEPRECATION")
                    val resources = Resources(assets, context.resources.displayMetrics, context.resources.configuration)
                    ModernXposed.moduleRes = resources
                    installHomeHooks()
                }.onFailure { error -> log(Log.ERROR, "Fuck_Home", "Launcher initialization failed", error) }
            }
            "com.android.systemui" -> if (XConfig.getBoolean("RemoveSmallWindowRestriction")) {
                runCatching { RemoveSmallWindowRestriction3.init() }
                    .onFailure { log(Log.ERROR, "Fuck_Home", "System UI hook failed", it) }
            }
        }
    }
    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        ModernXposed.classLoader = param.classLoader
        if (XConfig.getBoolean("MainSwitch") && XConfig.getBoolean("RemoveSmallWindowRestriction")) {
            runCatching { RemoveSmallWindowRestriction1.init() }
                .onFailure { log(Log.ERROR, "Fuck_Home", "System server hook failed", it) }
        }
    }
    private fun installHomeHooks() {
        val hooks = listOf<Pair<String, BaseHook>>(
            "Pad" to UnlockPad, "Shortcuts" to ShortcutItemCount,
            "UnlockGrids" to UnlockGrids, "HideAppName" to HideAppName,
            "HideAppIcon" to HideAppIcon, "UnlockHotseat" to UnlockHotseatIcon,
            "ShortcutSmallWindow" to ShortcutSmallWindow, "HideSmallWindow" to HideSmallWindow,
            "RemoveSmallWindowRestriction" to RemoveSmallWindowRestriction2,
            "UnlockNavType" to UnlockNavType
        )
        for ((key, hook) in hooks) if (XConfig.getBoolean(key)) {
            runCatching { hook.init() }.onFailure { log(Log.ERROR, "Fuck_Home", "Hook failed: $key", it) }
        }
        for (hook in listOf(Info, HideStatusBarWhenEnterResents)) {
            runCatching { hook.init() }.onFailure { log(Log.ERROR, "Fuck_Home", "Information hook failed", it) }
        }
    }
}