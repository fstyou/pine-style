/*
Pine Style
Copyright (C) 2026 fstu
This file is part of Pine Style.
Pine Style is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Pine Style is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with Pine Style. If not, see <https://www.gnu.org/licenses/>.
 */

package cc.fstu.pinestyle.data

// 检测状态枚举
// 定义连续帧检测的状态
enum class DetectionState {
    // 正常状态
    NORMAL,
    
    // 警戒状态（连续30帧不健康姿势）
    WARNING,
    
    // 警报状态
    ALERT
}
