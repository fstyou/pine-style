/*
Pine Style
Copyright (C) 2026 fstu
This file is part of Pine Style.
Pine Style is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Pine Style is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with Pine Style. If not, see <https://www.gnu.org/licenses/>.
 */

package cc.fstu.pinestyle.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// 设置数据存储库
// 使用DataStore持久化存储应用设置
class SettingsRepository(private val context : Context) {
    
    companion object {
        private val Context.dataStore : DataStore<Preferences> by preferencesDataStore(name = "settings")
        
        // 设置键
        private val CAMERA_TYPE_KEY = stringPreferencesKey("camera_type")
        private val THEME_TYPE_KEY = stringPreferencesKey("theme_type")
        private val DEVICE_KEY = stringPreferencesKey("device")
    }
    
    // 获取应用设置
    val appSettings : Flow<AppSettings> = context.dataStore.data
        .map { preferences ->
            val camera = try {
                Camera.valueOf(preferences[CAMERA_TYPE_KEY] ?: "FRONT")
            }
            catch (_ : IllegalArgumentException) {
                Camera.FRONT
            }
            val themeType = try {
                ThemeType.valueOf(preferences[THEME_TYPE_KEY] ?: "FOLLOW_SYSTEM")
            }
            catch (_ : IllegalArgumentException) {
                ThemeType.FOLLOW_SYSTEM
            }
            val device = try {
                Device.valueOf(preferences[DEVICE_KEY] ?: "CPU")
            }
            catch (_ : IllegalArgumentException) {
                Device.CPU
            }
            
            AppSettings(
                camera = camera,
                themeType = themeType,
                device = device
                       )
        }
    
    // 更新摄像头类型
    suspend fun updateCameraType(camera : Camera) {
        context.dataStore.edit { preferences ->
            preferences[CAMERA_TYPE_KEY] = camera.name
        }
    }
    
    // 更新运算设备
    suspend fun updateDevice(device : Device) {
        context.dataStore.edit { preferences ->
            preferences[DEVICE_KEY] = device.name
        }
    }
    
    // 更新主题类型
    suspend fun updateThemeType(themeType : ThemeType) {
        context.dataStore.edit { preferences ->
            preferences[THEME_TYPE_KEY] = themeType.name
        }
    }
    
    // 重置为默认设置
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    // 测试DataStore是否工作
    suspend fun testDataStore() {
        try {
            val settings = appSettings.first()
            // 测试写入（测试摄像头类型）
            updateCameraType(if (settings.camera == Camera.FRONT) Camera.BACK else Camera.FRONT)
            appSettings.first() // 读取验证
            // 恢复原值
            updateCameraType(settings.camera)
        }
        catch (e : Exception) {
            android.util.Log.e("SettingsRepository", "DataStore测试失败", e)
        }
    }
}
