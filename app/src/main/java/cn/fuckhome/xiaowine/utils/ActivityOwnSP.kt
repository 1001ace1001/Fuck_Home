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

import android.content.Context
import android.content.SharedPreferences
import cn.fuckhome.xiaowine.config.Config

object ActivityOwnSP {
    lateinit var ownSP: SharedPreferences
        private set
    val ownSPConfig by lazy { Config(ownSP) }
    const val version = 2
    @Synchronized
    fun initialize(context: Context) {
        if (!::ownSP.isInitialized) ownSP = Utils.getSP(context, Utils.CONFIG_GROUP)
        ConfigSharing.start(ownSP)
    }
    fun set(key: String, any: Any) {
        val editor = ownSP.edit()
        ConfigUtils.putValue(editor, key, any)
        editor.apply()
        ConfigSharing.sync()
    }
}
