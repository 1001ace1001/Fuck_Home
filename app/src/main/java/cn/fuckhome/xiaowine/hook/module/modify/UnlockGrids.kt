package cn.fuckhome.xiaowine.hook.module.modify

import cn.fuckhome.xiaowine.R
import cn.fuckhome.xiaowine.hook.BaseHook
import cn.fuckhome.xiaowine.utils.LogUtils
import cn.fuckhome.xiaowine.utils.Utils
import cn.fuckhome.xiaowine.utils.ModernXposed
import cn.fuckhome.xiaowine.utils.findMethod
import cn.fuckhome.xiaowine.utils.hookBefore

object UnlockGrids : BaseHook() {
    override fun init() {
        Utils.catchNoClass {
            LogUtils.i(ModernXposed.moduleRes.getString(R.string.UnlockGrids))
            findMethod("com.miui.home.launcher.DeviceConfig") { name == "getHotseatMaxCount" }.hookBefore { it.result = 15 }
        }
    }
}