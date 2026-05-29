package com.zhuimeng.dreambox.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.zhuimeng.dreambox.data.GithubChartApi
import com.zhuimeng.dreambox.data.SvgRenderer
import com.zhuimeng.dreambox.data.WidgetConfigRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class GithubWidgetWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val configRepository: WidgetConfigRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "GithubWidgetWorker"
        private const val KEY_WIDGET_IDS = "widget_ids"
        private const val WORK_NAME_PREFIX = "github_widget_refresh_"

        fun enqueueRefresh(context: Context, appWidgetIds: IntArray) {
            Log.d(TAG, "enqueueRefresh: ids=${appWidgetIds.contentToString()}")
            for (appWidgetId in appWidgetIds) {
                val workName = "${WORK_NAME_PREFIX}oneshot_$appWidgetId"
                val workRequest = OneTimeWorkRequestBuilder<GithubWidgetWorker>()
                    .setInputData(workDataOf(KEY_WIDGET_IDS to intArrayOf(appWidgetId)))
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
            }
        }

        fun startPeriodicRefresh(
            context: Context,
            appWidgetId: Int,
            intervalMinutes: Long
        ) {
            val effectiveInterval = maxOf(intervalMinutes, 15L)
            Log.d(TAG, "startPeriodicRefresh: id=$appWidgetId interval=${effectiveInterval}min")
            val workRequest = PeriodicWorkRequestBuilder<GithubWidgetWorker>(
                effectiveInterval, TimeUnit.MINUTES
            )
                .setInputData(workDataOf(KEY_WIDGET_IDS to intArrayOf(appWidgetId)))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "$WORK_NAME_PREFIX$appWidgetId",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancelPeriodicRefresh(context: Context, appWidgetId: Int) {
            Log.d(TAG, "cancelPeriodicRefresh: id=$appWidgetId")
            WorkManager.getInstance(context)
                .cancelUniqueWork("$WORK_NAME_PREFIX$appWidgetId")
        }
    }

    private val appWidgetManager = AppWidgetManager.getInstance(appContext)

    override suspend fun doWork(): Result {
        val appWidgetIds = inputData.getIntArray(KEY_WIDGET_IDS)
        Log.d(TAG, "doWork 开始执行, ids=${appWidgetIds?.contentToString()}")

        if (appWidgetIds == null) {
            Log.e(TAG, "doWork: 没有 widget IDs")
            return Result.failure()
        }

        for (appWidgetId in appWidgetIds) {
            Log.d(TAG, "处理 widget id=$appWidgetId")
            try {
                // 1. 读取配置
                Log.d(TAG, "读取配置 id=$appWidgetId")
                val config = configRepository.getConfigSnapshot(appWidgetId)
                Log.d(TAG, "配置: user=${config.username}, color=${config.color}, theme=${config.theme}")

                if (config.username.isBlank()) {
                    Log.w(TAG, "用户名未配置, 跳过 id=$appWidgetId")
                    // 不更新 UI — 保持当前显示，防止触发 onUpdate 循环
                    continue
                }

                // 2. 检查安静时段
                if (isInQuietHours(config.quietHourStart, config.quietHourEnd)) {
                    Log.d(TAG, "安静时段中, 跳过刷新 id=$appWidgetId")
                    continue
                }

                val isDark = config.theme == "dark"

                // 3. 获取 widget 实际显示尺寸
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
                val hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
                val density = appContext.resources.displayMetrics.density
                val chartWidthPx = (wDp * density).toInt()
                val chartHeightPx = ((hDp - 56) * density).toInt() // 减去顶部用户名和底部工具栏
                Log.d(TAG, "widget 尺寸: ${wDp}x${hDp}dp, 图表区域: ${chartWidthPx}x${chartHeightPx}px")

                // 4. 获取 SVG
                Log.d(TAG, "开始获取 SVG: color=${config.color}, user=${config.username}")
                val svgBytes = GithubChartApi.fetchSvgBytes(
                    color = config.color,
                    username = config.username
                )
                Log.d(TAG, "SVG 获取成功: ${svgBytes.size} bytes")

                // 5. 渲染为 Bitmap（裁剪左侧标签 + 放大填满）
                Log.d(TAG, "开始渲染 SVG -> Bitmap")
                val bitmap = SvgRenderer.renderToWidgetBitmap(
                    svgBytes,
                    chartWidthPx,
                    chartHeightPx
                )
                Log.d(TAG, "渲染结果: bitmap=${if (bitmap != null) "${bitmap.width}x${bitmap.height}" else "null"}")

                // 6. 更新 Widget UI
                val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                GithubWidgetProvider.updateWidgetData(
                    context = appContext,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    username = config.username,
                    chartBitmap = bitmap,
                    timestamp = "更新于 $timestamp",
                    isDarkTheme = isDark
                )
                Log.d(TAG, "Widget UI 已更新 id=$appWidgetId")

                // 7. 启动周期性刷新
                GithubWidgetWorker.startPeriodicRefresh(
                    appContext, appWidgetId, config.refreshIntervalMinutes
                )

            } catch (e: Exception) {
                Log.e(TAG, "处理 widget id=$appWidgetId 失败", e)
                // 显示错误信息到 widget（仅当配置读取成功时）
                try {
                    val snapshot = configRepository.getConfigSnapshot(appWidgetId)
                    val isDark = snapshot.theme == "dark"
                    GithubWidgetProvider.showError(
                        appContext, appWidgetManager, appWidgetId,
                        "加载失败: ${e.message ?: "未知错误"}",
                        isDarkTheme = isDark
                    )
                } catch (_: Exception) {}
            }
        }

        return Result.success()
    }

    private fun isInQuietHours(startHour: Int, endHour: Int): Boolean {
        if (startHour == endHour) return false
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (startHour < endHour) {
            currentHour in startHour until endHour
        } else {
            currentHour >= startHour || currentHour < endHour
        }
    }
}
