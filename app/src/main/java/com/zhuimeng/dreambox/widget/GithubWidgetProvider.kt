package com.zhuimeng.dreambox.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.zhuimeng.dreambox.R

/**
 * GitHub 贡献热力图 Widget Provider
 *
 * 每个 widget 实例独立配置（用户名、颜色、刷新频率、安静时段），
 * 配置存储在 DataStore 中，按 appWidgetId 索引。
 *
 * 数据更新通过 WorkManager 实现（而非系统默认的定时更新），
 * 以支持自定义刷新频率、安静时段和手动刷新。
 */
class GithubWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetUi(context, appWidgetManager, appWidgetId)
        }
        // 触发后台工作刷新数据
        GithubWidgetWorker.enqueueRefresh(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        // 第一个 widget 被添加时触发
    }

    override fun onDisabled(context: Context) {
        // 最后一个 widget 被移除时触发
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // widget 被删除时，清理对应的配置
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        /**
         * 更新 widget 的 UI（显示加载状态/用户名）
         */
        fun updateWidgetUi(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.github_widget_layout)

            // 设置用户名占位（后续由 Worker 更新）
            views.setTextViewText(R.id.widget_username, "加载中...")

            // 设置刷新按钮点击事件
            val refreshIntent = Intent(context, GithubWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * 更新 widget 展示的数据（图表 bitmap + 用户名 + 时间戳）
         */
        fun updateWidgetData(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            username: String,
            chartBitmap: android.graphics.Bitmap?,
            timestamp: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.github_widget_layout)

            views.setTextViewText(R.id.widget_username, username)

            if (chartBitmap != null) {
                views.setImageViewBitmap(R.id.widget_chart, chartBitmap)
            }

            views.setTextViewText(R.id.widget_timestamp, timestamp)

            // 刷新按钮
            val refreshIntent = Intent(context, GithubWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
