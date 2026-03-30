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
