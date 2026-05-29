package com.zhuimeng.dreambox.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import com.zhuimeng.dreambox.R
import com.zhuimeng.dreambox.data.WidgetMappingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

open class GithubWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "GithubWidgetProvider"
        private const val DEBOUNCE_MS = 30_000L
        private val lastEnqueueTime = ConcurrentHashMap<Int, Long>()

        /** 根据 widget 尺寸自动选择布局 */
        private fun getLayoutForSize(appWidgetManager: AppWidgetManager, appWidgetId: Int): Int {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            return when {
                minWidthDp < 130 -> R.layout.github_widget_layout_tiny  // 2x1
                else -> R.layout.github_widget_layout                   // 4x2
            }
        }

        fun updateWidgetData(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            username: String,
            chartBitmap: android.graphics.Bitmap?,
            timestamp: String,
            isDarkTheme: Boolean = false
        ) {
            Log.d(TAG, "updateWidgetData id=$appWidgetId user=$username dark=$isDarkTheme")
            val layoutResId = getLayoutForSize(appWidgetManager, appWidgetId)
            val views = createBaseViews(context, appWidgetId, layoutResId, isDarkTheme)

            try { views.setTextViewText(R.id.widget_username, username) } catch (_: Exception) {}
            try { views.setTextViewText(R.id.widget_timestamp, timestamp) } catch (_: Exception) {}

            if (chartBitmap != null) {
                views.setImageViewBitmap(R.id.widget_chart, chartBitmap)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun showError(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            errorMessage: String,
            isDarkTheme: Boolean = false
        ) {
            Log.e(TAG, "showError id=$appWidgetId: $errorMessage dark=$isDarkTheme")
            val layoutResId = getLayoutForSize(appWidgetManager, appWidgetId)
            val views = createBaseViews(context, appWidgetId, layoutResId, isDarkTheme)
            val errorColor = if (isDarkTheme) "#FF6B6B" else "#666666"
            try {
                views.setTextViewText(R.id.widget_username, "加载失败")
                views.setTextColor(R.id.widget_username, Color.parseColor(errorColor))
            } catch (_: Exception) {}
            try { views.setTextViewText(R.id.widget_timestamp, errorMessage) } catch (_: Exception) {}
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createBaseViews(
            context: Context,
            appWidgetId: Int,
            layoutResId: Int = R.layout.github_widget_layout,
            isDarkTheme: Boolean = false
        ): RemoteViews {
            val views = RemoteViews(context.packageName, layoutResId)

            // 设置圆角背景
            val bgResId = if (isDarkTheme) R.drawable.widget_bg_dark else R.drawable.widget_bg_light
            try { views.setInt(R.id.widget_root, "setBackgroundResource", bgResId) } catch (_: Exception) {}

            // 设置暗色/亮色文字颜色
            if (isDarkTheme) {
                try { views.setTextColor(R.id.widget_username, Color.parseColor("#EEEEEE")) } catch (_: Exception) {}
                try { views.setTextColor(R.id.widget_timestamp, Color.parseColor("#AAAAAA")) } catch (_: Exception) {}
            } else {
                try { views.setTextColor(R.id.widget_username, Color.parseColor("#666666")) } catch (_: Exception) {}
                try { views.setTextColor(R.id.widget_timestamp, Color.parseColor("#999999")) } catch (_: Exception) {}
            }

            // 设置刷新按钮
            try {
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
            } catch (_: Exception) {}

            // 设置点击 Widget 主体 → 打开 Profile 选择器
            try {
                val pickIntent = Intent(context, WidgetConfigureActivity::class.java).apply {
                    action = WidgetConfigureActivity.ACTION_PICK_PROFILE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val pickPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId + 10000, // 不同 ID 避免与 refresh 冲突
                    pickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pickPendingIntent)
            } catch (_: Exception) {}

            return views
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate: ids=${appWidgetIds.contentToString()}")
        val now = System.currentTimeMillis()
        val idsToRefresh = appWidgetIds.filter { id ->
            val last = lastEnqueueTime[id] ?: 0L
            if (now - last >= DEBOUNCE_MS) {
                lastEnqueueTime[id] = now
                true
            } else {
                Log.d(TAG, "onUpdate: debounce skip id=$id")
                false
            }
        }
        if (idsToRefresh.isNotEmpty()) {
            GithubWidgetWorker.enqueueRefresh(context, idsToRefresh.toIntArray())
        }
    }

    override fun onEnabled(context: Context) {
        Log.d(TAG, "onEnabled: 第一个 widget 被添加")
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "onDisabled: 最后一个 widget 被移除")
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.d(TAG, "onDeleted: ids=${appWidgetIds.contentToString()}")
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { id ->
                // 清理映射关系
                WidgetMappingRepository(context).removeWidget(id)
                Log.d(TAG, "已清理映射: widget=$id")
            }
        }
        super.onDeleted(context, appWidgetIds)
    }
}
