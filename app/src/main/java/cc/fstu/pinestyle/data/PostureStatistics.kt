package cc.fstu.pinestyle.data

import java.util.*

// 坐姿统计数据类
// 记录一次应用运行的检测数据
data class PostureStatistics(
    // 会话ID
    val sessionId : String = "",
    // 开始时间
    val startTime : Date = Date(),
    // 结束时间
    var endTime : Date? = null,
    // 总检测时长（毫秒）
    var totalDetectionTime : Long = 0L,
    // 良好坐姿时长（毫秒）
    var goodPostureTime : Long = 0L,
    // 跷二郎腿时长（毫秒）
    var crossLegTime : Long = 0L,
    // 脖子前伸时长（毫秒）
    var hunchbackTime : Long = 0L,
    // 无人时长（毫秒）
    var noPersonTime : Long = 0L,
                            ) {
    // 计算坐姿评分（0-100分）
    fun calculateScore() : Float {
        if (totalDetectionTime == 0L) return 0f
        val goodRatio = goodPostureTime.toFloat() / totalDetectionTime
        return goodRatio * 100f
    }
    
}
