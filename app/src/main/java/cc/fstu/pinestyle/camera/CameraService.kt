package cc.fstu.pinestyle.camera

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import androidx.lifecycle.LifecycleOwner
import cc.fstu.pinestyle.data.Camera
import cc.fstu.pinestyle.data.Device
import cc.fstu.pinestyle.data.PostureType
import cc.fstu.pinestyle.ml.MoveNet
import cc.fstu.pinestyle.ml.PoseClassifier
import cc.fstu.pinestyle.ml.SafeMoveNet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 摄像头服务 - 基于Camera2 API
class CameraService(
    private val context : Context,
    private val surfaceView : SurfaceView,
    private val onPostureDetected : (PostureType, Float) -> Unit,
    // 设备不支持时的回调
    private val onDeviceNotSupported : ((Device, String) -> Unit)? = null,
    lifecycleOwner : LifecycleOwner,
                   ) {
    
    companion object {
        private const val TAG = "CameraService"
        private const val MIN_CONFIDENCE = 0.3f
    }
    
    private var cameraSource : CameraSource? = null
    private var isRunning = false
    private var currentCamera = Camera.FRONT
    private var currentDevice = Device.CPU
    
    // 操作互斥锁
    private var isProcessing = false
    
    // 脖子前伸计数器
    private var forwardheadCounter = 0
    
    // 翘二郎腿计数器
    private var crosslegCounter = 0
    
    // 标准坐姿计数器
    private var standardCounter = 0
    
    // 未检测到人体计数器
    private var missingCounter = 0
    
    // 历史姿态寄存器
    private var poseRegister = "standard"
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    // 初始化摄像头服务
    fun initialize() {
        try {
            // 创建CameraSource
            cameraSource = CameraSource(surfaceView, currentCamera, object : CameraSource.CameraSourceListener {
                override fun onFPSListener(fps : Int) {
                }
                
                override fun onDetectedInfo(personScore : Float?, poseLabels : List<Pair<String, Float>>?) {
                    if (poseLabels != null && personScore != null && personScore > MIN_CONFIDENCE) {
                        missingCounter = 0
                        val sortedLabels = poseLabels.sortedByDescending { it.second }
                        
                        when (sortedLabels[0].first) {
                            "forwardhead" -> {
                                crosslegCounter = 0
                                standardCounter = 0
                                if (poseRegister == "forwardhead") {
                                    forwardheadCounter ++
                                }
                                poseRegister = "forwardhead"
                                // 转换姿态分类结果到PostureType
                                val postureType = PostureType.HUNCHBACK
                                onPostureDetected(postureType, sortedLabels[0].second)
                            }
                            
                            "crossleg" -> {
                                forwardheadCounter = 0
                                standardCounter = 0
                                if (poseRegister == "crossleg") {
                                    crosslegCounter ++
                                }
                                poseRegister = "crossleg"
                                // 转换姿态分类结果到PostureType
                                val postureType = PostureType.CROSS_LEG
                                onPostureDetected(postureType, sortedLabels[0].second)
                            }
                            
                            else -> {
                                forwardheadCounter = 0
                                crosslegCounter = 0
                                if (poseRegister == "standard") {
                                    standardCounter ++
                                }
                                poseRegister = "standard"
                                // 转换姿态分类结果到PostureType
                                val postureType = PostureType.GOOD
                                onPostureDetected(postureType, sortedLabels[0].second)
                            }
                        }
                    }
                    else {
                        // 未检测到人体或置信度不足
                        missingCounter ++
                        // 转换姿态分类结果到PostureType
                        val postureType = PostureType.NO_PERSON
                        onPostureDetected(postureType, 0f)
                    }
                }
            })
            // 准备摄像头
            cameraSource?.prepareCamera()
            // 初始化ML模型
            initializeModels()
        }
        catch (e : Exception) {
            Log.e(TAG, "摄像头服务初始化失败", e)
        }
    }
    
    // 初始化ML模型
    private fun initializeModels(device : Device = currentDevice) {
        Log.d(TAG, "initializeModels被调用，设备: $device, cameraSource: ${cameraSource != null}")
        coroutineScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始初始化ML模型，使用设备: $device")
                // 尝试创建安全的MoveNet检测器
                val detector = try {
                    Log.d(TAG,
                        "创建SafeMoveNet检测器，设备: $device, onDeviceNotSupported回调是否为空: ${onDeviceNotSupported == null}")
                    SafeMoveNet.create(context, device) { originalDevice ->
                        // 当检测器自动回退到CPU时调用
                        Log.w(TAG, "检测器从 $originalDevice 自动回退到CPU - 回调开始")
                        
                        // 更新当前设备为CPU
                        if (currentDevice != Device.CPU) {
                            Log.w(TAG, "自动更新CameraService设备设置: $currentDevice -> CPU")
                            currentDevice = Device.CPU
                        }
                        Log.w(TAG,
                            "调用onDeviceNotSupported回调，设备: $originalDevice, 回调是否为空: ${onDeviceNotSupported == null}")
                        onDeviceNotSupported?.invoke(originalDevice, "设备不支持模型操作，已自动回退到CPU模式")
                        Log.w(TAG, "onDeviceNotSupported回调调用完成")
                    }
                }
                catch (e : Throwable) {
                    // 捕获所有异常
                    Log.e(TAG, "创建SafeMoveNet检测器失败: ${e.message}", e)
                    // 如果GPU/NNAPI失败，尝试回退到CPU
                    if (device != Device.CPU) {
                        Log.w(TAG, "设备 $device 初始化失败，尝试回退到CPU")
                        try {
                            SafeMoveNet.create(context, Device.CPU)
                        }
                        catch (fallbackE : Throwable) {
                            Log.e(TAG, "回退到CPU也失败", fallbackE)
                            null
                        }
                    }
                    else {
                        null
                    }
                }
                if (detector == null) {
                    Log.e(TAG, "无法创建MoveNet检测器")
                    onDeviceNotSupported?.invoke(device, "无法初始化模型，请重启应用")
                    return@launch
                }
                Log.d(TAG, "SafeMoveNet检测器创建完成，准备设置到cameraSource")
                if (cameraSource != null) {
                    cameraSource?.setDetector(detector)
                    Log.d(TAG, "SafeMoveNet检测器设置成功，使用设备: $device")
                    // 检查检测器是否已经回退到CPU模式
                    val safeDetector = detector
                    if (safeDetector.hasFallenBackToCpu()) {
                        Log.w(TAG, "检测器已自动回退到CPU模式")
                        onDeviceNotSupported?.invoke(device, "GPU不支持模型操作，已自动回退到CPU模式")
                    }
                }
                else {
                    Log.w(TAG, "cameraSource为null，无法设置检测器。检测器已创建但未设置")
                }
                // 创建姿态分类器
                val classifier = PoseClassifier.create(context)
                cameraSource?.setClassifier(classifier)
                Log.d(TAG, "姿态分类器创建成功")
                
                Log.d(TAG, "ML模型初始化成功")
            }
            catch (e : Throwable) {
                Log.e(TAG, "ML模型初始化失败", e)
                e.printStackTrace()
                // 如果GPU/NNAPI失败，尝试回退到CPU
                if (device != Device.CPU) {
                    Log.w(TAG, "设备 $device 不支持，尝试回退到CPU")
                    try {
                        val fallbackDetector = MoveNet.create(context, Device.CPU)
                        cameraSource?.setDetector(fallbackDetector)
                        Log.w(TAG, "已回退到CPU模式")
                        // 通知UI设备不支持
                        onDeviceNotSupported?.invoke(device, "设备 $device 不支持，已自动回退到CPU模式")
                    }
                    catch (fallbackE : Throwable) {
                        Log.e(TAG, "回退到CPU也失败", fallbackE)
                        onDeviceNotSupported?.invoke(device,
                            "设备 $device 不支持且回退到CPU也失败: ${fallbackE.message}")
                    }
                }
                else {
                    // 即使是CPU模式也失败，通知UI
                    onDeviceNotSupported?.invoke(device, "CPU模式初始化失败: ${e.message}")
                }
            }
        }
    }
    
    // 启动摄像头
    fun startCamera(camera : Camera? = null) {
        Log.d(TAG, "启动摄像头，cameraType: $camera, isRunning: $isRunning")
        // 更新摄像头类型
        if (camera != null) {
            currentCamera = when (camera) {
                Camera.FRONT -> Camera.FRONT
                Camera.BACK -> Camera.BACK
            }
        }
        coroutineScope.launch {
            try {
                // 总是先完全停止再重新创建
                Log.d(TAG, "确保完全停止现有摄像头")
                cameraSource?.close()
                cameraSource = null
                isRunning = false
                // 给系统一点时间释放资源
                kotlinx.coroutines.delay(300)
                Log.d(TAG, "创建新的CameraSource")
                // 总是创建新的CameraSource，确保SurfaceView正确绑定
                cameraSource = CameraSource(surfaceView, currentCamera, object : CameraSource.CameraSourceListener {
                    override fun onFPSListener(fps : Int) {
                    }
                    
                    override fun onDetectedInfo(personScore : Float?, poseLabels : List<Pair<String, Float>>?) {
                        Log.d(TAG, "检测回调，分数: $personScore, 姿态: $poseLabels")
                        if (poseLabels != null && personScore != null && personScore > MIN_CONFIDENCE) {
                            missingCounter = 0
                            val sortedLabels = poseLabels.sortedByDescending { it.second }
                            when (sortedLabels[0].first) {
                                "forwardhead" -> {
                                    crosslegCounter = 0
                                    standardCounter = 0
                                    if (poseRegister == "forwardhead") {
                                        forwardheadCounter ++
                                    }
                                    poseRegister = "forwardhead"
                                    // 转换姿态分类结果到PostureType
                                    val postureType = PostureType.HUNCHBACK
                                    onPostureDetected(postureType, sortedLabels[0].second)
                                }
                                
                                "crossleg" -> {
                                    forwardheadCounter = 0
                                    standardCounter = 0
                                    if (poseRegister == "crossleg") {
                                        crosslegCounter ++
                                    }
                                    poseRegister = "crossleg"
                                    // 转换姿态分类结果到PostureType
                                    val postureType = PostureType.CROSS_LEG
                                    onPostureDetected(postureType, sortedLabels[0].second)
                                }
                                
                                else -> {
                                    forwardheadCounter = 0
                                    crosslegCounter = 0
                                    if (poseRegister == "standard") {
                                        standardCounter ++
                                    }
                                    poseRegister = "standard"
                                    // 转换姿态分类结果到PostureType
                                    val postureType = PostureType.GOOD
                                    onPostureDetected(postureType, sortedLabels[0].second)
                                }
                            }
                        }
                        else {
                            // 未检测到人体或置信度不足
                            missingCounter ++
                            // 转换姿态分类结果到PostureType
                            val postureType = PostureType.NO_PERSON
                            onPostureDetected(postureType, 0f)
                        }
                    }
                })
                cameraSource?.prepareCamera()
                cameraSource?.resume()
                cameraSource?.initCamera()
                isRunning = true
                Log.d(TAG, "摄像头启动成功")
                // 初始化ML模型（在后台线程）
                initializeModels(currentDevice)
            }
            catch (e : Exception) {
                Log.e(TAG, "摄像头启动失败", e)
            }
        }
    }
    
    // 停止摄像头
    fun stopCamera() {
        Log.d(TAG,
            "停止摄像头，当前isRunning: $isRunning, cameraSource: ${cameraSource != null}, isProcessing: $isProcessing")
        // 检查是否正在处理中
        if (isProcessing) {
            Log.w(TAG, "摄像头操作正在处理中，跳过stopCamera")
            return
        }
        isProcessing = true
        try {
            if (cameraSource != null) {
                Log.d(TAG, "关闭cameraSource资源")
                cameraSource?.close()
                cameraSource = null
                Log.d(TAG, "cameraSource已关闭并置为null")
            }
            isRunning = false
            // 重置计数器
            forwardheadCounter = 0
            crosslegCounter = 0
            standardCounter = 0
            missingCounter = 0
            poseRegister = "standard"
            Log.d(TAG, "摄像头已完全停止")
        }
        finally {
            isProcessing = false
            Log.d(TAG, "stopCamera操作完成，isProcessing重置为false")
        }
    }
    
    // 切换摄像头
    fun switchCamera() {
        Log.d(TAG, "切换摄像头，当前: $currentCamera, isRunning: $isRunning, isProcessing: $isProcessing")
        // 检查是否正在处理中
        if (isProcessing) {
            Log.w(TAG, "摄像头操作正在处理中，跳过switchCamera")
            return
        }
        if (isRunning) {
            isProcessing = true
            try {
                // 停止当前摄像头
                stopCamera()
                // 切换摄像头
                currentCamera = when (currentCamera) {
                    Camera.FRONT -> Camera.BACK
                    Camera.BACK -> Camera.FRONT
                }
                // 重新创建CameraSource
                cameraSource = CameraSource(surfaceView, currentCamera, object : CameraSource.CameraSourceListener {
                    override fun onFPSListener(fps : Int) {
                    }
                    
                    override fun onDetectedInfo(personScore : Float?, poseLabels : List<Pair<String, Float>>?) {
                        Log.d(TAG, "检测到人体，分数: $personScore, 姿态: $poseLabels")
                        if (poseLabels != null && personScore != null && personScore > MIN_CONFIDENCE) {
                            missingCounter = 0
                            val sortedLabels = poseLabels.sortedByDescending { it.second }
                            when (sortedLabels[0].first) {
                                "forwardhead" -> {
                                    crosslegCounter = 0
                                    standardCounter = 0
                                    if (poseRegister == "forwardhead") {
                                        forwardheadCounter ++
                                    }
                                    poseRegister = "forwardhead"
                                    // 转换姿态分类结果到PostureType
                                    val postureType = PostureType.HUNCHBACK
                                    onPostureDetected(postureType, sortedLabels[0].second)
                                }
                                
                                "crossleg" -> {
                                    forwardheadCounter = 0
                                    standardCounter = 0
                                    if (poseRegister == "crossleg") {
                                        crosslegCounter ++
                                    }
                                    poseRegister = "crossleg"
                                    // 转换姿态分类结果到PostureType
                                    val postureType = PostureType.CROSS_LEG
                                    onPostureDetected(postureType, sortedLabels[0].second)
                                }
                                
                                else -> {
                                    forwardheadCounter = 0
                                    crosslegCounter = 0
                                    if (poseRegister == "standard") {
                                        standardCounter ++
                                    }
                                    poseRegister = "standard"
                                    // 转换姿态分类结果到PostureType
                                    val postureType = PostureType.GOOD
                                    onPostureDetected(postureType, sortedLabels[0].second)
                                }
                            }
                        }
                        else {
                            // 未检测到人体或置信度不足
                            missingCounter ++
                            // 转换姿态分类结果到PostureType
                            val postureType = PostureType.NO_PERSON
                            onPostureDetected(postureType, 0f)
                        }
                    }
                })
                cameraSource?.prepareCamera()
                initializeModels(currentDevice)
                // 重新启动摄像头
                startCamera()
            }
            finally {
                isProcessing = false
                Log.d(TAG, "switchCamera操作完成，isProcessing重置为false")
            }
        }
        else {
            Log.w(TAG, "尝试切换摄像头但摄像头未运行")
        }
    }
    
    // 更新摄像头选择器
    fun updateCameraSelector(camera : Camera) {
        currentCamera = when (camera) {
            Camera.FRONT -> Camera.FRONT
            Camera.BACK -> Camera.BACK
        }
        Log.d(TAG, "摄像头选择器更新为: $currentCamera")
    }
    
    // 更新运算设备
    fun updateDevice(device : Device) {
        Log.d(TAG,
            "updateDevice被调用，当前设备: $currentDevice, 新设备: $device, isRunning: $isRunning, isProcessing: $isProcessing")
        // 检查是否正在处理中
        if (isProcessing) {
            Log.w(TAG, "摄像头操作正在处理中，跳过updateDevice")
            return
        }
        if (currentDevice != device) {
            isProcessing = true
            try {
                Log.d(TAG, "更新运算设备: $currentDevice -> $device")
                // 保存当前摄像头状态
                val wasRunning = isRunning
                // 如果摄像头正在运行，先停止摄像头
                if (wasRunning) {
                    Log.d(TAG, "摄像头正在运行，先停止摄像头")
                    cameraSource?.close()
                    cameraSource = null
                    isRunning = false
                }
                currentDevice = device
                // 总是重新初始化模型，确保下次启动摄像头时使用正确的设备
                Log.d(TAG, "重新初始化模型")
                initializeModels(device)
                // 如果之前摄像头在运行，重新启动摄像头
                if (wasRunning) {
                    Log.d(TAG, "重新启动摄像头")
                    startCamera(currentCamera)
                }
            }
            finally {
                isProcessing = false
                Log.d(TAG, "updateDevice操作完成，isProcessing重置为false")
            }
        }
        else {
            Log.d(TAG, "设备未变化，跳过更新")
        }
    }
    
    // 获取当前摄像头类型
    fun getCurrentCameraType() : Camera {
        return when (currentCamera) {
            Camera.FRONT -> Camera.FRONT
            Camera.BACK -> Camera.BACK
        }
    }
    
    // 释放资源
    fun release() {
        stopCamera()
        cameraSource = null
        Log.d(TAG, "摄像头服务资源已释放")
    }
}
