/*
Pine Style
Copyright (C) 2026 fstu
This file is part of Pine Style.
Pine Style is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Pine Style is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with Pine Style. If not, see <https://www.gnu.org/licenses/>.
 */

package cc.fstu.pinestyle.data

// 应用设置数据类
data class AppSettings(
    // 摄像头类型，默认前置
    val camera : Camera = Camera.FRONT,
    // 主题类型，默认跟随系统
    val themeType : ThemeType = ThemeType.FOLLOW_SYSTEM,
    // 运算设备，默认CPU
    val device : Device = Device.CPU,
                      )
