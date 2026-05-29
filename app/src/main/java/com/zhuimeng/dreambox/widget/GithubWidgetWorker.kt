package com.zhuimeng.dreambox.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
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

/**
 * GitHub 贡献图更新 Worker
 *
 * 负责从 ghchart.rshah.org 获取 SVG，渲染为 Bitmap 并更新 Widget UI。
 * 支持安静时段判断（在指定时间段内跳过刷新）。
 */
@HiltWorker
class GithubWidgetWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val configRepository: WidgetConfigRepository
) : CoroutineWorker(appContext, workerParams) {

    private val appWidgetManager = AppWidgetManager.getInstance(appContext)

    override suspend fun doWork(): Result {
        val appWidgetIds = inputData.getIntArray(KEY_WIDGET_IDS) ?: return Result.failure()

        for (appWidgetId in appWidgetIds) {
            try {
                // 1. 读取配置
                val config = configRepository.getConfigSnapshot(appWidgetId)

                // 如果未配置用户名，跳过
                if (config.username.isBlank()) {
                    GithubWidgetProvider.updateWidgetUi(
                        appContext, appWidgetManager, appWidgetId
                    )
                    continue
                }

                // 2. 检查安静时段
                if (isInQuietHours(config.quietHourStart, config.quietHourEnd)) {
                    // 安静时段内，跳过刷新
                    continue
                }

                // 3. 获取 SVG
                val svgBytes = GithubChartApi.fetchSvgBytes(
                    color = config.color,
                    username = config.username
                )

                // 4. 渲染为 Bitmap
                val bitmap = SvgRenderer.renderToBitmap(svgBytes)

                // 5. 更新 Widget UI
                val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(Date())
                GithubWidgetProvider.updateWidgetData(
                    context = appContext,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    username = config.username,
                    chartBitmap = bitmap,
                    timestamp = "更新于 $timestamp"
                )

                // 6. 启动周期性刷新（如果尚未启动）
                GithubWidgetWorker.startPeriodicRefresh(
                    appContext, appWidgetId, config.refreshIntervalMinutes
                )

            } catch (e: Exception) {
                // 单个 widget 失败不影响其他 widget
                e.printStackTrace()
            }
        }

        return Result.success()
    }

    /**
     * 判断当前时间是否在安静时段内
     *
     * 安静时段支持跨天（如 22:00 ~ 07:00）
     */
    private fun isInQuietHours(startHour: Int, endHour: Int): Boolean {
        if (startHour == endHour) return false // 起止相同表示不启用

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        return if (startHour < endHour) {
            // 当天内，如 02:00 ~ 07:00
            currentHour in startHour until endHour
        } else {
            // 跨天，如 22:00 ~ 07:00
            currentHour >= startHour || currentHour < endHour
        }
    }

    companion object {
        private const val KEY_WIDGET_IDS = "widget_ids"
        private const val WORK_NAME_PREFIX = "github_widget_refresh_"

        /**
         * 立即刷新指定的 widgets
         */
        fun enqueueRefresh(context: Context, appWidgetIds: IntArray) {
            val workRequest = OneTimeWorkRequestBuilder<GithubWidgetWorker>()
                .setInputData(workDataOf(KEY_WIDGET_IDS to appWidgetIds))
                .build()
            WorkManager.getInstance(context)
                .enqueue(workRequest)
        }

        /**
         * 启动周期性刷新
         */
        fun startPeriodicRefresh(
            context: Context,
            appWidgetId: Int,
            intervalMinutes: Long
        ) {
            // WorkManager 最小间隔为 15 分钟
            val effectiveInterval = maxOf(intervalMinutes, 15L)

            val workRequest = PeriodicWorkRequestBuilder<GithubWidgetWorker>(
                effectiveInterval, TimeUnit.MINUTES
            )
                .setInputData(workDataOf(KEY_WIDGET_IDS to intArrayOf(appWidgetId)))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "$WORK_NAME_PREFIX$appWidgetId",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
        }

        /**
         * 取消指定 widget 的周期性刷新
         */
        fun cancelPeriodicRefresh(context: Context, appWidgetId: Int) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("$WORK_NAME_PREFIX$appWidgetId")
        }
    }
}
