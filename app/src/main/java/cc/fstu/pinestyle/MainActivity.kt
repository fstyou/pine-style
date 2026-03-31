/*
Pine Style
Copyright (C) 2026 fstu
Pine Style is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Pine Style is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with Pine Style. If not, see <https://www.gnu.org/licenses/>.
 */

package cc.fstu.pinestyle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cc.fstu.pinestyle.ui.help.HelpScreen
import cc.fstu.pinestyle.ui.home.HomeScreen
import cc.fstu.pinestyle.ui.home.MainViewModel
import cc.fstu.pinestyle.ui.settings.SettingsScreen
import cc.fstu.pinestyle.ui.shared.SharedViewModel
import cc.fstu.pinestyle.ui.stats.StatisticsScreen
import cc.fstu.pinestyle.ui.stats.StatisticsViewModel
import cc.fstu.pinestyle.ui.theme.GreenPineTheme

class MainActivity : ComponentActivity() {
    // 摄像头权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
                                                                     ) { isGranted : Boolean ->
        Log.d("MainActivity", "摄像头权限请求结果: $isGranted")
        recreate()
    }
    
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "应用启动完成")
        setContent {
            // 在Composable中实时检查权限状态
            val hasCameraPermission = checkCameraPermission()
            val sharedViewModel : SharedViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass : Class<T>) : T {
                        if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
                            return SharedViewModel(this@MainActivity) as T
                        }
                        throw IllegalArgumentException("Unknown ViewModel class")
                    }
                }
                                                             )
            val appSettings by sharedViewModel.appSettings.collectAsState()
            GreenPineTheme(
                themeType = appSettings.themeType
                          ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                       ) {
                    MainApp(
                        sharedViewModel = sharedViewModel,
                        hasCameraPermission = hasCameraPermission,
                        onRequestCameraPermission = { requestCameraPermission() }
                           )
                }
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    // 检查摄像头权限
    private fun checkCameraPermission() : Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
                                                             ) == PackageManager.PERMISSION_GRANTED
        Log.d("MainActivity", "检查摄像头权限: $hasPermission")
        return hasPermission
    }
    
    // 请求摄像头权限
    private fun requestCameraPermission() {
        Log.d("MainActivity", "请求摄像头权限")
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
}

// 主应用组件
@Composable
fun MainApp(
    sharedViewModel : SharedViewModel,
    hasCameraPermission : Boolean,
    onRequestCameraPermission : () -> Unit,
           ) {
    val navController = rememberNavController()
    val appSettings by sharedViewModel.appSettings.collectAsState()
    // 创建主页ViewModel
    val homeViewModel : MainViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass : Class<T>) : T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    return MainViewModel(sharedViewModel) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
                                                 )
    NavHost(
        navController = navController,
        startDestination = "home"
           ) {
        composable("home") {
            val confirmedPosture by homeViewModel.confirmedPosture.collectAsState()
            val detectionState by homeViewModel.detectionState.collectAsState()
            // 获取上下文和生命周期所有者
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            // 设置ViewModel的上下文和生命周期所有者
            LaunchedEffect(context, lifecycleOwner) {
                homeViewModel.setContextAndLifecycleOwner(context, lifecycleOwner)
            }
            // 监听摄像头类型变化
            val currentAppSettings by sharedViewModel.appSettings.collectAsState()
            LaunchedEffect(currentAppSettings.camera) {
                Log.d("MainActivity", "摄像头类型变化: ${currentAppSettings.camera}")
                homeViewModel.updateCameraType(currentAppSettings.camera)
            }
            // 监听设备类型变化
            LaunchedEffect(currentAppSettings.device) {
                Log.d("MainActivity", "设备类型变化: ${currentAppSettings.device}")
            }
            HomeScreen(
                viewModel = homeViewModel,
                currentPosture = confirmedPosture,
                hasCameraPermission = hasCameraPermission,
                detectionState = detectionState,
                onRequestCameraPermission = onRequestCameraPermission,
                onNavigateToStats = { navController.navigate("stats") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHelp = { navController.navigate("help") }
                      )
        }
        composable("stats") {
            val statsViewModel : StatisticsViewModel = viewModel()
            val currentSession by statsViewModel.currentSession.collectAsState()
            // 使用LaunchedEffect在进入页面时获取一次数据
            LaunchedEffect(Unit) {
                Log.d("MainActivity", "统计页面 - 进入页面，获取统计数据")
                // 获取当前统计数据快照
                val snapshot = homeViewModel.getStatisticsSnapshot()
                Log.d("MainActivity", "统计页面 - 获取到数据快照: $snapshot")
                // 更新统计ViewModel
                statsViewModel.updateFromMainViewModel(
                    goodDuration = snapshot["goodDuration"] ?: 0,
                    totalDetectionDuration = snapshot["totalDetectionDuration"] ?: 0,
                    crossLegDuration = snapshot["crossLegDuration"] ?: 0,
                    hunchbackDuration = snapshot["hunchbackDuration"] ?: 0,
                    noPersonDuration = snapshot["noPersonDuration"] ?: 0
                                                      )
            }
            StatisticsScreen(
                statistics = currentSession,
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHelp = { navController.navigate("help") },
                onUpdateData = {
                    // 获取最新数据并更新
                    val snapshot = homeViewModel.getStatisticsSnapshot()
                    Log.d("MainActivity", "统计页面 - 手动更新数据: $snapshot")
                    
                    statsViewModel.triggerDataUpdate(
                        goodDuration = snapshot["goodDuration"] ?: 0,
                        totalDetectionDuration = snapshot["totalDetectionDuration"] ?: 0,
                        crossLegDuration = snapshot["crossLegDuration"] ?: 0,
                        hunchbackDuration = snapshot["hunchbackDuration"] ?: 0,
                        noPersonDuration = snapshot["noPersonDuration"] ?: 0
                                                    )
                }
                            )
        }
        composable("settings") {
            SettingsScreen(
                settings = appSettings,
                onSettingsUpdate = { newSettings ->
                    Log.d("MainActivity", "设置更新: $newSettings")
                    // 直接更新设置，不暂停摄像头
                    sharedViewModel.updateCameraType(newSettings.camera)
                    sharedViewModel.updateThemeType(newSettings.themeType)
                    sharedViewModel.updateDevice(newSettings.device)
                },
                onResetToDefaults = {
                    Log.d("MainActivity", "重置到默认设置")
                    sharedViewModel.resetToDefaults()
                },
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToStats = { navController.navigate("stats") },
                onNavigateToHelp = { navController.navigate("help") }
                          )
        }
        composable("help") {
            HelpScreen(
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToStats = { navController.navigate("stats") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHelp = { }
                      )
        }
    }
}
