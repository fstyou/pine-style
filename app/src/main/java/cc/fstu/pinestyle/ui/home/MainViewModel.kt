/*
Pine Style
Copyright (C) 2026 fstu
This file is part of Pine Style.
Pine Style is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Pine Style is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with Pine Style. If not, see <https://www.gnu.org/licenses/>.
 */

package cc.fstu.pinestyle.ui.home


import android.annotation.SuppressLint
import android.content.Context
import android.view.SurfaceView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.fstu.pinestyle.camera.CameraService
import cc.fstu.pinestyle.data.Camera
import cc.fstu.pinestyle.data.DetectionState
import cc.fstu.pinestyle.data.Device
import cc.fstu.pinestyle.data.PostureType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// 管理主页状态和逻辑
class MainViewModel(
    private val sharedViewModel : cc.fstu.pinestyle.ui.shared.SharedViewModel? = null,
                   ) : ViewModel() {
    
    // 实时坐姿状态
    private val _rawPosture = MutableStateFlow(PostureType.NO_PERSON)
    
    // 二次确认的坐姿状态
    private val _confirmedPosture = MutableStateFlow(PostureType.NO_PERSON)
    val confirmedPosture : StateFlow<PostureType> = _confirmedPosture
    
    // 连续帧检测状态
    private val _detectionState = MutableStateFlow(DetectionState.NORMAL)
    val detectionState : StateFlow<DetectionState> = _detectionState
    
    // 连续不健康姿势帧数
    private var consecutiveUnhealthyFrames = 0
    
    // 警戒状态下连续不健康姿势帧数
    private var warningConsecutiveFrames = 0
    
    // 摄像头服务
    private var cameraService : CameraService? = null
    
    // 上下文和生命周期所有者
    @SuppressLint("StaticFieldLeak")
    private var context : Context? = null
    private var lifecycleOwner : LifecycleOwner? = null
    
    @SuppressLint("StaticFieldLeak")
    private var pendingSurfaceView : SurfaceView? = null
    
    // 防止重复更新摄像头的标志
    private var isSwitchingCamera = false
    
    // 保存最新的设备设置
    private var latestDevice : Device = Device.CPU
    
    // 摄像头操作串行化机制
    private var isCameraOperationInProgress = false
    private val cameraOperationQueue = mutableListOf<suspend () -> Unit>()
    
    
    // 事件记录系统
    private data class PostureEvent(
        // 事件发生时间戳
        val timestamp : Long,
        // 事件类型
        val eventType : EventType,
        // 姿势类型
        val postureType : PostureType? = null,
                                   )
    
    private enum class EventType {
        // 会话开始
        SESSION_START,
        
        // 姿势变化
        POSTURE_CHANGE,
        
        // 检测暂停
        DETECTION_PAUSE,
        
        // 检测恢复
        DETECTION_RESUME,
        
        // 会话结束
        SESSION_END
    }
    
    // 事件记录列表
    private val eventHistory = mutableListOf<PostureEvent>()
    
    // 检测是否活跃
    private var isDetectionActive : Boolean = false
    
    // 当前确认的姿势
    private var currentPosture : PostureType = PostureType.NO_PERSON
    
    // 实时统计信息
    // 良好坐姿时长（毫秒）
    private val _goodPostureDuration = MutableStateFlow(0L)
    
    // 总检测时长（毫秒，不包括未检测到人）
    private val _totalDetectionDuration = MutableStateFlow(0L)
    
    // 当前会话总时长（毫秒）
    private val _currentSessionDuration = MutableStateFlow(0L)
    
    // 详细姿势时长
    // 跷二郎腿时长
    private val _crossLegDuration = MutableStateFlow(0L)
    
    // 脖子前伸时长
    private val _hunchbackDuration = MutableStateFlow(0L)
    
    // 无人时长
    private val _noPersonDuration = MutableStateFlow(0L)
    
    init {
        // 监听设置变化
        viewModelScope.launch {
            sharedViewModel?.appSettings?.collectLatest { settings ->
                // 更新摄像头类型
                updateCameraType(settings.camera)
                // 保存设备设置
                latestDevice = settings.device
                // 更新运算设备
                if (cameraService != null) {
                    executeCameraOperation {
                        cameraService?.updateDevice(settings.device)
                    }
                }
                else {
                    android.util.Log.w("MainViewModel", "cameraService为null，保存设备设置: ${settings.device}")
                }
            }
        }
    }
    
    // 设置上下文和生命周期所有者
    fun setContextAndLifecycleOwner(context : Context, lifecycleOwner : LifecycleOwner) {
        android.util.Log.d("MainViewModel", "设置上下文和生命周期所有者，pendingSurfaceView: $pendingSurfaceView")
        this.context = context
        this.lifecycleOwner = lifecycleOwner
        // 检查是否有待处理的SurfaceView
        val pendingView = pendingSurfaceView
        if (pendingView != null) {
            android.util.Log.d("MainViewModel", "有待处理的SurfaceView，初始化摄像头服务")
            initializeCameraService(context, lifecycleOwner, pendingView)
            pendingSurfaceView = null
        }
        else {
            android.util.Log.d("MainViewModel", "没有待处理的SurfaceView")
        }
    }
    
    // 初始化摄像头服务
    fun initializeCameraService(
        context : Context,
        lifecycleOwner : LifecycleOwner,
        surfaceView : SurfaceView,
                               ) {
        android.util.Log.d("MainViewModel", "初始化摄像头服务，最新设备: $latestDevice")
        cameraService = CameraService(
            context = context,
            lifecycleOwner = lifecycleOwner,
            surfaceView = surfaceView,
            onPostureDetected = { postureType : PostureType, confidence : Float ->
                viewModelScope.launch {
                    _rawPosture.value = postureType
                    android.util.Log.d("MainViewModel", "检测结果: $postureType, 置信度: $confidence")
                    // 处理连续帧检测逻辑
                    processContinuousDetection(postureType)
                }
            },
            onDeviceNotSupported = { device, message ->
                android.util.Log.w("MainViewModel",
                    "设备不支持回调被调用: $device, 消息: $message, 当前线程: ${Thread.currentThread().name}")
                // 更新UI状态
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    android.util.Log.w("MainViewModel", "在主线程处理设备不支持")
                    // 更新SharedViewModel的设备设置为CPU
                    sharedViewModel?.let {
                        android.util.Log.w("MainViewModel", "更新SharedViewModel设备设置: $device -> CPU")
                        it.updateDevice(Device.CPU)
                    }
                    android.util.Log.w("MainViewModel",
                        "设备不支持: 无法使用 $device 模式，已自动回退到CPU模式。消息: $message")
                }
            }
                                     )
        cameraService?.initialize()
        android.util.Log.d("MainViewModel", "摄像头服务初始化完成，自动启动摄像头")
        viewModelScope.launch {
            // 给摄像头服务一点时间初始化
            kotlinx.coroutines.delay(500)
            // 默认使用前置摄像头
            cameraService?.startCamera(Camera.FRONT)
            // 记录会话开始事件
            recordEvent(EventType.SESSION_START)
        }
    }
    
    // 串行执行摄像头操作
    private fun executeCameraOperation(operation : suspend () -> Unit) {
        android.util.Log.d("MainViewModel",
            "执行摄像头操作，队列长度: ${cameraOperationQueue.size}, 是否进行中: $isCameraOperationInProgress")
        
        if (isCameraOperationInProgress) {
            // 如果已有操作在执行，加入队列
            cameraOperationQueue.add(operation)
            android.util.Log.d("MainViewModel", "操作加入队列，当前队列长度: ${cameraOperationQueue.size}")
            return
        }
        isCameraOperationInProgress = true
        viewModelScope.launch {
            try {
                android.util.Log.d("MainViewModel", "开始执行摄像头操作")
                operation()
                android.util.Log.d("MainViewModel", "摄像头操作执行完成")
            }
            catch (e : Exception) {
                android.util.Log.e("MainViewModel", "摄像头操作执行失败", e)
            }
            finally {
                // 执行下一个队列中的操作
                isCameraOperationInProgress = false
                if (cameraOperationQueue.isNotEmpty()) {
                    val nextOperation = cameraOperationQueue.removeAt(0)
                    android.util.Log.d("MainViewModel",
                        "执行队列中的下一个操作，剩余队列长度: ${cameraOperationQueue.size}")
                    executeCameraOperation(nextOperation)
                }
            }
        }
    }
    
    // 更新摄像头类型
    fun updateCameraType(camera : Camera) {
        android.util.Log.d("MainViewModel",
            "更新摄像头类型: $camera, isSwitchingCamera: $isSwitchingCamera")
        // 如果正在切换摄像头，跳过重启逻辑
        if (isSwitchingCamera) {
            android.util.Log.d("MainViewModel", "正在切换摄像头，跳过updateCameraType的重启逻辑")
            return
        }
        // 使用串行化机制执行摄像头操作
        executeCameraOperation {
            // 切换摄像头时完全重新创建资源
            android.util.Log.d("MainViewModel", "切换摄像头，重启摄像头")
            cameraService?.stopCamera()
            // 延迟确保摄像头完全停止
            kotlinx.coroutines.delay(500)
            cameraService?.updateCameraSelector(camera)
            cameraService?.startCamera(camera)
        }
    }
    
    // 处理连续帧检测逻辑
    // 两次确认机制：第一次确认后降回警戒状态，第二次确认后降回正常状态
    private fun processContinuousDetection(postureType : PostureType) {
        val currentState = _detectionState.value
        // 判断是否为不健康姿势
        val isUnhealthy = postureType == PostureType.CROSS_LEG || postureType == PostureType.HUNCHBACK
        val isHealthy = postureType == PostureType.GOOD
        val isNoPerson = postureType == PostureType.NO_PERSON
        // 处理坐姿时长统计
        when (currentState) {
            DetectionState.NORMAL -> {
                if (isUnhealthy) {
                    consecutiveUnhealthyFrames ++
                    android.util.Log.d("MainViewModel", "正常状态: 连续不健康帧数: $consecutiveUnhealthyFrames/30")
                    if (consecutiveUnhealthyFrames >= 30) {
                        // 进入警戒状态
                        _detectionState.value = DetectionState.WARNING
                        warningConsecutiveFrames = 0
                        consecutiveUnhealthyFrames = 0
                        // 更新经过二次确认的坐姿状态
                        _confirmedPosture.value = postureType
                        // 记录姿势变化事件
                        recordPostureEvent(postureType)
                        android.util.Log.d("MainViewModel", "第一次确认：进入警戒状态，确认坐姿: $postureType")
                    }
                }
                else if (isHealthy || isNoPerson) {
                    // 健康姿势或未检测到人，重置计数
                    consecutiveUnhealthyFrames = 0
                    if (isHealthy && _confirmedPosture.value != PostureType.GOOD) {
                        _confirmedPosture.value = PostureType.GOOD
                        // 记录姿势变化事件
                        recordPostureEvent(PostureType.GOOD)
                        android.util.Log.d("MainViewModel", "正常状态: 检测到健康姿势，更新确认姿势为GOOD")
                    }
                    else if (isNoPerson && _confirmedPosture.value != PostureType.NO_PERSON) {
                        _confirmedPosture.value = PostureType.NO_PERSON
                        // 记录姿势变化事件
                        recordPostureEvent(PostureType.NO_PERSON)
                        android.util.Log.d("MainViewModel", "正常状态: 检测到无人，更新确认姿势为NO_PERSON")
                    }
                    android.util.Log.d("MainViewModel", "正常状态: 检测到健康姿势或无人，重置计数")
                }
            }
            
            DetectionState.WARNING -> {
                if (isUnhealthy) {
                    warningConsecutiveFrames ++
                    android.util.Log.d("MainViewModel", "警戒状态: 连续不健康帧数: $warningConsecutiveFrames/30")
                    if (warningConsecutiveFrames >= 30) {
                        // 触发警报状态
                        _detectionState.value = DetectionState.ALERT
                        warningConsecutiveFrames = 0
                        android.util.Log.d("MainViewModel", "第二次确认：触发语音提示警报")
                    }
                }
                else if (isHealthy || isNoPerson) {
                    // 检测到健康姿势或无人，返回正常状态
                    _detectionState.value = DetectionState.NORMAL
                    consecutiveUnhealthyFrames = 0
                    warningConsecutiveFrames = 0
                    // 更新经过二次确认的坐姿状态
                    val newPosture = if (isHealthy) PostureType.GOOD else PostureType.NO_PERSON
                    _confirmedPosture.value = newPosture
                    // 记录姿势变化事件
                    recordPostureEvent(newPosture)
                    android.util.Log.d("MainViewModel",
                        "警戒状态: 检测到健康姿势或无人，自动返回正常状态，确认坐姿: $newPosture")
                }
            }
            
            DetectionState.ALERT -> {
                if (isHealthy || isNoPerson) {
                    // 第一次确认：检测到健康姿势或无人，降回警戒状态
                    _detectionState.value = DetectionState.WARNING
                    consecutiveUnhealthyFrames = 0
                    warningConsecutiveFrames = 0
                    android.util.Log.d("MainViewModel", "警报状态: 第一次确认健康姿势或无人，降回警戒状态")
                }
                else {
                    // 继续保持不健康姿势，维持警报状态
                    android.util.Log.d("MainViewModel", "警报状态: 继续保持不健康姿势")
                }
            }
        }
    }
    
    // 记录事件
    private fun recordEvent(eventType : EventType, postureType : PostureType? = null) {
        val timestamp = System.currentTimeMillis()
        val event = PostureEvent(timestamp, eventType, postureType)
        eventHistory.add(event)
        android.util.Log.d("MainViewModel", "记录事件: $eventType, 姿势: $postureType, 时间戳: $timestamp")
        // 根据事件类型更新状态
        when (eventType) {
            EventType.SESSION_START -> {
                // 会话开始
                isDetectionActive = true
                android.util.Log.d("MainViewModel", "检测会话开始")
            }
            
            EventType.POSTURE_CHANGE -> {
                // 姿势变化，更新当前姿势
                if (postureType != null) {
                    currentPosture = postureType
                    android.util.Log.d("MainViewModel", "姿势变化为: $postureType")
                }
            }
            
            EventType.DETECTION_PAUSE -> {
                // 检测暂停
                isDetectionActive = false
                android.util.Log.d("MainViewModel", "检测暂停")
            }
            
            EventType.DETECTION_RESUME -> {
                // 检测恢复
                isDetectionActive = true
                android.util.Log.d("MainViewModel", "检测恢复")
            }
            
            EventType.SESSION_END -> {
                // 会话结束
                isDetectionActive = false
                android.util.Log.d("MainViewModel", "检测会话结束")
            }
        }
        // 更新UI统计
        updateUiStatistics()
    }
    
    // 记录姿势变化事件
    private fun recordPostureChange(newPosture : PostureType) {
        // 只有姿势真正变化时才记录
        if (currentPosture != newPosture) {
            recordEvent(EventType.POSTURE_CHANGE, newPosture)
        }
    }
    
    // 记录姿势事件
    private fun recordPostureEvent(newPosture : PostureType) {
        recordPostureChange(newPosture)
    }
    
    // 计算统计数据
    private fun calculateStatistics() : Map<String, Long> {
        if (eventHistory.isEmpty()) {
            android.util.Log.d("MainViewModel", "事件历史为空，返回空统计")
            return emptyMap()
        }
        android.util.Log.d("MainViewModel", "开始计算统计，事件历史大小: ${eventHistory.size}")
        android.util.Log.d("MainViewModel", "事件历史: $eventHistory")
        // 初始化统计变量
        var goodDuration : Long = 0
        var crossLegDuration : Long = 0
        var hunchbackDuration : Long = 0
        var noPersonDuration : Long = 0
        var totalDetectionDuration : Long = 0
        // 当前时间段的状态
        var currentPostureInPeriod : PostureType = PostureType.NO_PERSON
        var isDetectionActiveInPeriod = false
        var periodStartTime : Long = eventHistory.first().timestamp
        // 初始化检测活跃状态：如果有SESSION_START事件，则检测活跃
        for (event in eventHistory) {
            when (event.eventType) {
                EventType.SESSION_START -> {
                    isDetectionActiveInPeriod = true
                    android.util.Log.d("MainViewModel", "统计计算: 检测会话开始，标记为活跃")
                }
                
                EventType.DETECTION_PAUSE -> {
                    isDetectionActiveInPeriod = false
                    android.util.Log.d("MainViewModel", "统计计算: 检测暂停")
                }
                
                EventType.DETECTION_RESUME -> {
                    isDetectionActiveInPeriod = true
                    android.util.Log.d("MainViewModel", "统计计算: 检测恢复")
                }
                
                else -> {
                }
            }
        }
        // 遍历所有事件，计算每个时间段的时长
        for (i in eventHistory.indices) {
            val event = eventHistory[i]
            val periodEndTime = event.timestamp
            val periodDuration = periodEndTime - periodStartTime
            if (periodDuration > 0 && isDetectionActiveInPeriod) {
                // 只有在检测活跃期间才计算时长
                when (currentPostureInPeriod) {
                    PostureType.GOOD -> {
                        goodDuration += periodDuration
                        totalDetectionDuration += periodDuration
                    }
                    
                    PostureType.CROSS_LEG -> {
                        crossLegDuration += periodDuration
                        totalDetectionDuration += periodDuration
                    }
                    
                    PostureType.HUNCHBACK -> {
                        hunchbackDuration += periodDuration
                        totalDetectionDuration += periodDuration
                    }
                    
                    PostureType.NO_PERSON -> {
                        noPersonDuration += periodDuration
                        // 无人时不增加总检测时长
                    }
                }
            }
            // 更新下一个时间段的状态
            when (event.eventType) {
                EventType.POSTURE_CHANGE -> {
                    if (event.postureType != null) {
                        currentPostureInPeriod = event.postureType
                        android.util.Log.d("MainViewModel", "统计计算: 姿势变化为 $currentPostureInPeriod")
                    }
                }
                
                EventType.DETECTION_PAUSE -> {
                    isDetectionActiveInPeriod = false
                    android.util.Log.d("MainViewModel", "统计计算: 检测暂停")
                }
                
                EventType.DETECTION_RESUME -> {
                    isDetectionActiveInPeriod = true
                    android.util.Log.d("MainViewModel", "统计计算: 检测恢复")
                }
                
                else -> {
                }
            }
            
            periodStartTime = periodEndTime
        }
        // 计算当前时间段的时长
        val currentTime = System.currentTimeMillis()
        val currentPeriodDuration = currentTime - periodStartTime
        if (currentPeriodDuration > 0 && isDetectionActiveInPeriod && isDetectionActive) {
            when (currentPostureInPeriod) {
                PostureType.GOOD -> {
                    goodDuration += currentPeriodDuration
                    totalDetectionDuration += currentPeriodDuration
                }
                
                PostureType.CROSS_LEG -> {
                    crossLegDuration += currentPeriodDuration
                    totalDetectionDuration += currentPeriodDuration
                }
                
                PostureType.HUNCHBACK -> {
                    hunchbackDuration += currentPeriodDuration
                    totalDetectionDuration += currentPeriodDuration
                }
                
                PostureType.NO_PERSON -> {
                    noPersonDuration += currentPeriodDuration
                }
            }
        }
        // 计算总会话时长
        val totalSessionDuration = if (eventHistory.isNotEmpty()) {
            currentTime - eventHistory.first().timestamp
        }
        else {
            0L
        }
        val statistics = mapOf(
            "goodDuration" to goodDuration,
            "crossLegDuration" to crossLegDuration,
            "hunchbackDuration" to hunchbackDuration,
            "noPersonDuration" to noPersonDuration,
            "totalDetectionDuration" to totalDetectionDuration,
            "totalSessionDuration" to totalSessionDuration
                              )
        android.util.Log.d("MainViewModel", "计算统计: $statistics")
        return statistics
    }
    
    // 更新UI统计状态
    private fun updateUiStatistics() {
        val statistics = calculateStatistics()
        // 在主线程更新所有UI状态
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            _goodPostureDuration.value = statistics["goodDuration"] ?: 0
            _totalDetectionDuration.value = statistics["totalDetectionDuration"] ?: 0
            _currentSessionDuration.value = statistics["totalSessionDuration"] ?: 0
            _crossLegDuration.value = statistics["crossLegDuration"] ?: 0
            _hunchbackDuration.value = statistics["hunchbackDuration"] ?: 0
            _noPersonDuration.value = statistics["noPersonDuration"] ?: 0
            android.util.Log.d("MainViewModel", "UI统计更新: $statistics")
        }
    }
    
    // 获取当前统计数据快照
    fun getStatisticsSnapshot() : Map<String, Long> {
        val statistics = calculateStatistics()
        // 直接返回计算出的统计结果
        android.util.Log.d("MainViewModel", "统计数据快照: $statistics")
        return statistics
    }
    
    // 释放资源
    override fun onCleared() {
        super.onCleared()
        cameraService?.release()
        cameraService = null
        
        
    }
}