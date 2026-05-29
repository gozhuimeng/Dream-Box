package com.zhuimeng.dreambox.widget

import android.util.Log

/**
 * 2x1 迷你 Widget Provider
 *
 * 仅展示热力图，无文字信息，继承自主 Provider 的所有逻辑。
 * 布局由 updateWidgetData() 根据 widget 尺寸自动切换。
 */
class GithubWidgetTinyProvider : GithubWidgetProvider() {

    companion object {
        private const val TAG = "GithubWidgetTinyProvider"
    }

    // 所有逻辑继承自 GithubWidgetProvider：
    // - onUpdate() — 防抖 + 触发 Worker
    // - 布局切换 — 由 GithubWidgetProvider.updateWidgetData() 根据尺寸自动判断
}
