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
import com.zhuimeng.dreambox.data.WidgetMappingRepository
import com.zhuimeng.dreambox.data.WidgetProfile
import com.zhuimeng.dreambox.data.WidgetProfileRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class GithubWidgetWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val profileRepository: WidgetProfileRepository,
    private val mappingRepository: WidgetMappingRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "GithubWidgetWorker"
        private const val KEY_WIDGET_IDS = "widget_ids"
        private const val KEY_FORCE_REFRESH = "force_refresh"
        private const val WORK_NAME_PREFIX = "github_widget_refresh_"
        const val WORK_TAG = "github_widget"

        fun enqueueRefresh(context: Context, appWidgetIds: IntArray) {
            Log.d(TAG, "enqueueRefresh: ids=${appWidgetIds.contentToString()}")
            for (appWidgetId in appWidgetIds) {
                val workName = "${WORK_NAME_PREFIX}oneshot_$appWidgetId"
                val workRequest = OneTimeWorkRequestBuilder<GithubWidgetWorker>()
                    .addTag(WORK_TAG)
                    .setInputData(workDataOf(KEY_WIDGET_IDS to intArrayOf(appWidgetId)))
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
            }
        }

        /** 强制刷新（替换已有任务，用于配置切换时，跳过安静时段检查） */
        fun forceRefresh(context: Context, appWidgetIds: IntArray) {
            Log.d(TAG, "forceRefresh: ids=${appWidgetIds.contentToString()}")
            for (appWidgetId in appWidgetIds) {
                // 直接用 REPLACE 策略替换已有任务，避免 cancel+enqueue 的竞态条件
                // 设置 FORCE_REFRESH 标识，Worker 会跳过安静时段检查
                val workName = "${WORK_NAME_PREFIX}oneshot_$appWidgetId"
                val workRequest = OneTimeWorkRequestBuilder<GithubWidgetWorker>()
                    .addTag(WORK_TAG)
                    .setInputData(workDataOf(
                        KEY_WIDGET_IDS to intArrayOf(appWidgetId),
                        KEY_FORCE_REFRESH to true
                    ))
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.REPLACE,
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
                .addTag(WORK_TAG)
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

        /**
         * 在没有 Profile 数据时显示错误信息，避免 Widget 卡在"更新中..."
         */
        @JvmStatic
        fun showErrorStatic(
            context: Context,
            appWidgetId: Int,
            message: String
        ) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val options = manager.getAppWidgetOptions(appWidgetId)
                val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                val layoutResId = if (wDp < 130)
                    com.zhuimeng.dreambox.R.layout.github_widget_layout_tiny
                else
                    com.zhuimeng.dreambox.R.layout.github_widget_layout
                val views = android.widget.RemoteViews(context.packageName, layoutResId)
                try {
                    views.setTextViewText(com.zhuimeng.dreambox.R.id.widget_username, "配置异常")
                    views.setTextColor(com.zhuimeng.dreambox.R.id.widget_username,
                        android.graphics.Color.parseColor("#FF6B6B"))
                } catch (_: Exception) {}
                try {
                    views.setTextViewText(com.zhuimeng.dreambox.R.id.widget_timestamp, message)
                } catch (_: Exception) {}
                manager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "showErrorStatic 失败", e)
            }
        }
    }

    /** 在没有 Profile 数据时显示错误 */
    private fun showErrorWithoutProfile(appWidgetId: Int, message: String) {
        showErrorStatic(appContext, appWidgetId, message)
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
                // 1. 先获取映射 → 找到 Profile ID
                val profileId = mappingRepository.getProfileIdForWidgetSnapshot(appWidgetId)
                if (profileId == null) {
                    Log.e(TAG, "widget $appWidgetId 未绑定任何 Profile, 跳过并显示错误")
                    showErrorWithoutProfile(appWidgetId, "未绑定配置，请点击切换")
                    continue
                }

                // 2. 读取 Profile 配置（包括安静时段）
                Log.d(TAG, "读取 Profile id=$profileId")
                val profile = profileRepository.getProfileSnapshot(profileId)
                Log.d(TAG, "Profile: id=${profile.id} name=${profile.name} user=${profile.username} theme=${profile.theme} quiet=${profile.quietHourStart}-${profile.quietHourEnd}")

                if (profile.username.isBlank()) {
                    Log.e(TAG, "Profile $profileId 用户名为空, 显示错误 widget=$appWidgetId")
                    showErrorWithoutProfile(appWidgetId, "配置用户名为空，请编辑配置")
                    continue
                }

                // 3. 检查是否为强制刷新（来自配置切换等用户主动操作）
                val isForce = inputData.getBoolean(KEY_FORCE_REFRESH, false)

                // 4. 非强制刷新时检查安静时段（在修改 UI 之前检查，避免 Widget 卡在"更新中..."）
                if (!isForce && isInQuietHours(profile.quietHourStart, profile.quietHourEnd)) {
                    Log.d(TAG, "安静时段中 (${profile.quietHourStart}:00-${profile.quietHourEnd}:00), 跳过自动刷新 id=$appWidgetId")
                    continue
                }

                // 5. 通过安静时段检查后，再设置"更新中..."状态
                try {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                    val layoutResId = if (wDp < 130) com.zhuimeng.dreambox.R.layout.github_widget_layout_tiny
                        else com.zhuimeng.dreambox.R.layout.github_widget_layout
                    val loadingViews = android.widget.RemoteViews(appContext.packageName, layoutResId)
                    loadingViews.setTextViewText(com.zhuimeng.dreambox.R.id.widget_timestamp, "更新中...")
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, loadingViews)
                } catch (_: Exception) {} // 2x1 小部件没有 timestamp，静默跳过

                val isDark = profile.theme == WidgetProfile.THEME_DARK

                // 6. 获取 widget 实际显示尺寸
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
                val hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
                val density = appContext.resources.displayMetrics.density
                val chartWidthPx = (wDp * density).toInt()
                val chartHeightPx = ((hDp - 56) * density).toInt()
                Log.d(TAG, "widget 尺寸: ${wDp}x${hDp}dp, 图表区域: ${chartWidthPx}x${chartHeightPx}px")

                // 6. 获取 SVG（带超时，且可被协程取消）
                Log.d(TAG, "开始获取 SVG: color=${profile.color}, user=${profile.username}")
                val svgBytes = withTimeout(60_000L) {
                    withContext(Dispatchers.IO) {
                        GithubChartApi.fetchSvgBytes(
                            color = profile.color,
                            username = profile.username
                        )
                    }
                }
                Log.d(TAG, "SVG 获取成功: ${svgBytes.size} bytes")

                // 7. 渲染为 Bitmap
                Log.d(TAG, "开始渲染 SVG -> Bitmap")
                val bitmap = SvgRenderer.renderToWidgetBitmap(
                    svgBytes,
                    chartWidthPx,
                    chartHeightPx
                )
                Log.d(TAG, "渲染结果: bitmap=${if (bitmap != null) "${bitmap.width}x${bitmap.height}" else "null"}")

                // 8. 更新 Widget UI
                val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                GithubWidgetProvider.updateWidgetData(
                    context = appContext,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    username = profile.username,
                    chartBitmap = bitmap,
                    timestamp = "更新于 $timestamp",
                    isDarkTheme = isDark
                )
                Log.d(TAG, "Widget UI 已更新 id=$appWidgetId (profile=$profileId)")

                // 9. 启动周期性刷新（按 Profile 的间隔）
                GithubWidgetWorker.startPeriodicRefresh(
                    appContext, appWidgetId, profile.refreshIntervalMinutes
                )

            } catch (e: Exception) {
                Log.e(TAG, "处理 widget id=$appWidgetId 失败", e)
                // 尝试用 Profile 数据显示带主题的错误，失败则用纯文本 fallback
                var errorShown = false
                try {
                    val pid = mappingRepository.getProfileIdForWidgetSnapshot(appWidgetId)
                    if (pid != null) {
                        val p = profileRepository.getProfileSnapshot(pid)
                        GithubWidgetProvider.showError(
                            appContext, appWidgetManager, appWidgetId,
                            "加载失败: ${e.message ?: "未知错误"}",
                            isDarkTheme = p.theme == WidgetProfile.THEME_DARK
                        )
                        errorShown = true
                    }
                } catch (_: Exception) {}
                if (!errorShown) {
                    // Fallback: 不使用任何 Profile 数据，保证 Widget 不会卡在"更新中..."
                    showErrorWithoutProfile(appWidgetId, "加载失败: ${e.message ?: "未知错误"}")
                }
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
