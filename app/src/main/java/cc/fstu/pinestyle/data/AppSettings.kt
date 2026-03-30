package cc.fstu.pinestyle.data

// 应用设置数据类
data class AppSettings(
    // 摄像头类型，默认前置
    val camera : Camera = Camera.FRONT,
    // 主题类型，默认跟随系统
    val themeType : ThemeType = ThemeType.FOLLOW_SYSTEM,
    // 运算设备，默认CPU
    val device : Device = Device.CPU,
                      )
