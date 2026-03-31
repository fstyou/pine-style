/*
Pine Style
Copyright (C) 2026 fstu
This file is part of Pine Style.
Pine Style is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Pine Style is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with Pine Style. If not, see <https://www.gnu.org/licenses/>.
 */

package cc.fstu.pinestyle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.fstu.pinestyle.R
import cc.fstu.pinestyle.data.AppSettings
import cc.fstu.pinestyle.data.Camera
import cc.fstu.pinestyle.data.Device
import cc.fstu.pinestyle.data.ThemeType
import cc.fstu.pinestyle.ui.shared.BottomNavigationBar
import kotlinx.coroutines.launch

// 设置页面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings : AppSettings,
    onSettingsUpdate : (AppSettings) -> Unit,
    onResetToDefaults : () -> Unit,
    onNavigateToHome : () -> Unit,
    onNavigateToStats : () -> Unit,
    onNavigateToHelp : () -> Unit,
                  ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
                     )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "settings",
                onHomeClick = onNavigateToHome,
                onStatsClick = onNavigateToStats,
                onSettingsClick = { },
                onHelpClick = onNavigateToHelp
                               )
        }
            ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
              ) {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSection(
                title = stringResource(R.string.camera_settings),
                icon = Icons.Filled.Camera,
                iconDescription = stringResource(R.string.icon_camera_settings)
                           ) {
                CameraTypeSelector(
                    selectedCamera = settings.camera,
                    onCameraTypeChanged = { newCameraType ->
                        onSettingsUpdate(settings.copy(camera = newCameraType))
                    }
                                  )
            }
            // 运算设备设置
            SettingsSection(
                title = stringResource(R.string.device_settings),
                icon = Icons.Filled.DeviceHub,
                iconDescription = stringResource(R.string.icon_device_settings)
                           ) {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                DeviceSelector(
                    selectedDevice = settings.device,
                    onDeviceChanged = { newDevice ->
                        onSettingsUpdate(settings.copy(device = newDevice))
                        if (newDevice != Device.CPU) {
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val moveNet = cc.fstu.pinestyle.ml.MoveNet.create(context, newDevice)
                                    moveNet.close()
                                }
                                catch (e : Throwable) {
                                    android.util.Log.e("SettingsScreen", "设备 $newDevice 测试失败", e)
                                }
                            }
                        }
                    }
                              )
            }
            // 主题设置
            SettingsSection(
                title = stringResource(R.string.theme_settings),
                icon = Icons.Filled.Palette,
                iconDescription = stringResource(R.string.icon_theme_settings)
                           ) {
                ThemeTypeSelector(
                    selectedThemeType = settings.themeType,
                    onThemeTypeChanged = { newThemeType ->
                        onSettingsUpdate(settings.copy(themeType = newThemeType))
                    }
                                 )
            }
            Spacer(modifier = Modifier.height(32.dp))
            // 重置按钮
            Button(
                onClick = { onResetToDefaults() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                  ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.icon_reset),
                    modifier = Modifier.size(24.dp)
                    )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reset_defaults))
            }
        }
    }
}

// 设置部分容器
@Composable
fun SettingsSection(
    title : String,
    icon : androidx.compose.ui.graphics.vector.ImageVector,
    iconDescription : String,
    content : @Composable () -> Unit,
                   ) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
                                        )
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
              ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
               ) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconDescription,
                    tint = MaterialTheme.colorScheme.primary
                    )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                                                                     ),
                    color = MaterialTheme.colorScheme.onSurface
                    )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

// 摄像头类型选择器
@Composable
fun CameraTypeSelector(
    selectedCamera : Camera,
    onCameraTypeChanged : (Camera) -> Unit,
                      ) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
        CameraTypeOption(
            title = stringResource(R.string.front_camera),
            isSelected = selectedCamera == Camera.FRONT,
            onClick = { onCameraTypeChanged(Camera.FRONT) }
                        )
        
        CameraTypeOption(
            title = stringResource(R.string.back_camera),
            isSelected = selectedCamera == Camera.BACK,
            onClick = { onCameraTypeChanged(Camera.BACK) }
                        )
    }
}

// 摄像头类型选项
@Composable
fun CameraTypeOption(
    title : String,
    isSelected : Boolean,
    onClick : () -> Unit,
                    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
       ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            }
            else {
                MaterialTheme.colorScheme.onSurface
            }
            )
        
        RadioButton(
            selected = isSelected,
            onClick = onClick
                   )
    }
}

// 运算设备选择器
@Composable
fun DeviceSelector(
    selectedDevice : Device,
    onDeviceChanged : (Device) -> Unit,
                  ) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
        DeviceOption(
            title = stringResource(R.string.device_cpu),
            description = stringResource(R.string.device_description_cpu),
            isSelected = selectedDevice == Device.CPU,
            onClick = { onDeviceChanged(Device.CPU) }
                    )
        DeviceOption(
            title = stringResource(R.string.device_gpu),
            description = stringResource(R.string.device_description_gpu),
            isSelected = selectedDevice == Device.GPU,
            onClick = { onDeviceChanged(Device.GPU) }
                    )
        DeviceOption(
            title = stringResource(R.string.device_nnapi),
            description = stringResource(R.string.device_description_nnapi),
            isSelected = selectedDevice == Device.NNAPI,
            onClick = { onDeviceChanged(Device.NNAPI) }
                    )
        Text(
            text = stringResource(R.string.device_fallback_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )
    }
}

// 运算设备选项
@Composable
fun DeviceOption(
    title : String,
    description : String,
    isSelected : Boolean,
    onClick : () -> Unit,
                ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
          ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
           ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    }
                    else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick
                       )
        }
    }
}

// 主题类型选择器
@Composable
fun ThemeTypeSelector(
    selectedThemeType : ThemeType,
    onThemeTypeChanged : (ThemeType) -> Unit,
                     ) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
        ThemeTypeOption(
            title = stringResource(R.string.follow_system),
            isSelected = selectedThemeType == ThemeType.FOLLOW_SYSTEM,
            onClick = { onThemeTypeChanged(ThemeType.FOLLOW_SYSTEM) }
                       )
        
        ThemeTypeOption(
            title = stringResource(R.string.light_theme),
            isSelected = selectedThemeType == ThemeType.LIGHT,
            onClick = { onThemeTypeChanged(ThemeType.LIGHT) }
                       )
        
        ThemeTypeOption(
            title = stringResource(R.string.dark_theme),
            isSelected = selectedThemeType == ThemeType.DARK,
            onClick = { onThemeTypeChanged(ThemeType.DARK) }
                       )
    }
}

// 主题类型选项
@Composable
fun ThemeTypeOption(
    title : String,
    isSelected : Boolean,
    onClick : () -> Unit,
                   ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
       ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            }
            else {
                MaterialTheme.colorScheme.onSurface
            }
            )
        
        RadioButton(
            selected = isSelected,
            onClick = onClick
                   )
    }
}
