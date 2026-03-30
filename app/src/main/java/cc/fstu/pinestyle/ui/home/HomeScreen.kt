package cc.fstu.pinestyle.ui.home

import android.view.SurfaceView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cc.fstu.pinestyle.R
import cc.fstu.pinestyle.data.DetectionState
import cc.fstu.pinestyle.data.PostureType
import cc.fstu.pinestyle.ui.shared.BottomNavigationBar

// 主页屏幕
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel : MainViewModel,
    currentPosture : PostureType,
    hasCameraPermission : Boolean,
    detectionState : DetectionState,
    onRequestCameraPermission : () -> Unit,
    onNavigateToStats : () -> Unit,
    onNavigateToSettings : () -> Unit,
    onNavigateToHelp : () -> Unit,
              ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) }
                     )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "home",
                onHomeClick = { },
                onStatsClick = onNavigateToStats,
                onSettingsClick = onNavigateToSettings,
                onHelpClick = onNavigateToHelp
                               )
        }
            ) { paddingValues ->
        HomeScreenContent(
            viewModel = viewModel,
            currentPosture = currentPosture,
            hasCameraPermission = hasCameraPermission,
            detectionState = detectionState,
            onRequestCameraPermission = onRequestCameraPermission,
            modifier = Modifier.padding(paddingValues)
                         )
    }
}

// 主页内容组件
@Composable
fun HomeScreenContent(
    viewModel : MainViewModel,
    currentPosture : PostureType,
    hasCameraPermission : Boolean,
    detectionState : DetectionState,
    onRequestCameraPermission : () -> Unit,
    modifier : Modifier = Modifier,
                     ) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
        Spacer(modifier = Modifier.height(8.dp))
        // 摄像头权限提示
        if (! hasCameraPermission) {
            CameraPermissionCard(
                onRequestPermission = onRequestCameraPermission
                                )
        }
        else {
            // 有摄像头权限时显示坐姿状态卡片
            PostureStatusCard(
                currentPosture = currentPosture,
                detectionState = detectionState
                             )
            // 摄像头预览
            CameraPreview(
                viewModel = viewModel
                         )
        }
    }
}

// 坐姿状态卡片
@Composable
fun PostureStatusCard(
    currentPosture : PostureType,
    detectionState : DetectionState,
                     ) {
    val (postureTitle, postureColor) = when (currentPosture) {
        PostureType.GOOD -> Pair(
            stringResource(R.string.posture_good),
            MaterialTheme.colorScheme.primary
                                )
        
        PostureType.CROSS_LEG -> Pair(
            stringResource(R.string.posture_cross_leg),
            MaterialTheme.colorScheme.secondary
                                     )
        
        PostureType.HUNCHBACK -> Pair(
            stringResource(R.string.posture_hunchback),
            MaterialTheme.colorScheme.error
                                     )
        
        PostureType.NO_PERSON -> Pair(
            stringResource(R.string.posture_no_person),
            MaterialTheme.colorScheme.onSurfaceVariant
                                     )
    }
    val (statusIcon, statusHint, statusColor) = when (detectionState) {
        DetectionState.NORMAL -> Triple(
            Icons.Filled.CheckCircle,
            stringResource(R.string.detection_normal),
            MaterialTheme.colorScheme.primary
                                       )
        
        DetectionState.WARNING -> Triple(
            Icons.Filled.Warning,
            stringResource(R.string.warning_state),
            MaterialTheme.colorScheme.secondary
                                        )
        
        DetectionState.ALERT -> Triple(
            Icons.Filled.Error,
            stringResource(R.string.alert_state),
            MaterialTheme.colorScheme.error
                                      )
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = postureColor.copy(alpha = 0.1f)
                                        )
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
              ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
               ) {
                Text(
                    text = stringResource(R.string.current_posture),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                if (currentPosture != PostureType.NO_PERSON) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                       ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = statusHint,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                            )
                        
                        Text(
                            text = statusHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                            )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = postureTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                                                                    ),
                color = postureColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (detectionState) {
                    DetectionState.NORMAL -> {
                        if (currentPosture == PostureType.NO_PERSON) {
                            stringResource(R.string.no_person_hint)
                        }
                        else {
                            stringResource(R.string.good_posture_hint)
                        }
                    }
                    
                    DetectionState.WARNING -> stringResource(R.string.bad_posture_hint)
                    DetectionState.ALERT -> stringResource(R.string.bad_posture_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                )
        }
    }
}

// 摄像头权限提示卡片
@Composable
fun CameraPermissionCard(
    onRequestPermission : () -> Unit,
                        ) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
              ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = stringResource(R.string.icon_warning),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
                )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.camera_permission_required),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
                )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.camera_permission_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
                )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.permission_request_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
                )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
                  ) {
                Icon(
                    imageVector = Icons.Filled.Camera,
                    contentDescription = stringResource(R.string.icon_camera),
                    modifier = Modifier.size(20.dp)
                    )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.request_camera_permission))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.permission_fallback_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
                )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.permission_settings_path),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
                )
        }
    }
}

// 摄像头预览组件
@Composable
fun CameraPreview(
    viewModel : MainViewModel,
    modifier : Modifier = Modifier,
                 ) {
    // 宽:高 = 3:4
    val aspectRatio = 3f / 4f
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
                                        )
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
           ) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        // 初始化摄像头服务
                        viewModel.initializeCameraService(
                            context = ctx,
                            lifecycleOwner = lifecycleOwner,
                            surfaceView = this
                                                         )
                    }
                },
                modifier = Modifier.fillMaxSize()
                       )
        }
    }
}
