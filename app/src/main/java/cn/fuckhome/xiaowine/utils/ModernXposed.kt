package cn.fuckhome.xiaowine.utils
import android.content.Context
import android.content.res.Resources
import io.github.libxposed.api.XposedModule
object ModernXposed {
    lateinit var module: XposedModule
    lateinit var classLoader: ClassLoader
    lateinit var appContext: Context
    lateinit var moduleRes: Resources
}