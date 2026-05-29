package com.zhuimeng.dreambox.data

/**
 * 单个 Widget 实例的配置
 *
 * @param appWidgetId Widget 实例 ID
 * @param username GitHub 用户名
 * @param color 热力图颜色（十六进制，不含 #，如 "198754"）
 * @param refreshIntervalMinutes 刷新间隔（分钟）
 * @param quietHourStart 安静时段开始小时（0-23，默认 22 点）
 * @param quietHourEnd 安静时段结束小时（0-23，默认 7 点）
 * @param theme 主题: "light" 或 "dark"
 */
data class WidgetConfig(
    val appWidgetId: Int = INVALID_WIDGET_ID,
    val username: String = "",
    val color: String = DEFAULT_COLOR,
    val refreshIntervalMinutes: Long = DEFAULT_REFRESH_INTERVAL,
    val quietHourStart: Int = DEFAULT_QUIET_START,
    val quietHourEnd: Int = DEFAULT_QUIET_END,
    val theme: String = DEFAULT_THEME
) {
    companion object {
        const val INVALID_WIDGET_ID = -1
        const val DEFAULT_COLOR = "198754"
        const val DEFAULT_REFRESH_INTERVAL = 60L
        const val DEFAULT_QUIET_START = 22
        const val DEFAULT_QUIET_END = 7
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val DEFAULT_THEME = THEME_LIGHT
    }
}
