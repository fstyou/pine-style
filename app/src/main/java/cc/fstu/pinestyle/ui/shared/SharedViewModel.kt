/*
Pine Style
Copyright (C) 2026 fstu
This file is part of Pine Style.
Pine Style is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Pine Style is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with Pine Style. If not, see <https://www.gnu.org/licenses/>.
 */

package cc.fstu.pinestyle.ui.shared

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.fstu.pinestyle.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 管理应用级别的状态
class SharedViewModel(context : Context) : ViewModel() {
    private val settingsRepository = SettingsRepository(context)
    
    // 应用设置
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings : StateFlow<AppSettings> = _appSettings.asStateFlow()
    
    init {
        // 从存储加载设置
        viewModelScope.launch {
            settingsRepository.appSettings.collect { settings ->
                _appSettings.value = settings
            }
        }
    }
    
    // 更新摄像头类型
    fun updateCameraType(camera : Camera) {
        viewModelScope.launch {
            settingsRepository.updateCameraType(camera)
        }
    }
    
    // 更新运算设备
    fun updateDevice(device : Device) {
        viewModelScope.launch {
            settingsRepository.updateDevice(device)
        }
    }
    
    // 更新主题类型
    fun updateThemeType(themeType : ThemeType) {
        viewModelScope.launch {
            settingsRepository.updateThemeType(themeType)
        }
    }
    
    // 重置为默认设置
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
        }
    }
    
    // 获取当前主题类型
    fun getCurrentThemeType() : ThemeType {
        return appSettings.value.themeType
    }
    
    // 测试DataStore功能
    fun testDataStore() {
        viewModelScope.launch {
            settingsRepository.testDataStore()
        }
    }
}
