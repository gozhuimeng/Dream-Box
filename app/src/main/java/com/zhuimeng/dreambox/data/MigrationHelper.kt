package com.zhuimeng.dreambox.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 数据迁移助手
 *
 * 将 v0.1.2 及之前的旧版 WidgetConfig（按 appWidgetId 存储）
 * 迁移为新的 Profile + Mapping 数据模型。
 */
object MigrationHelper {
    private const val TAG = "MigrationHelper"
    private const val KEY_MIGRATION_DONE = "migration_v013_done"

    /** 旧版 DataStore 实例 */
    private val Context.oldWidgetConfigStore by preferencesDataStore(name = "widget_config")

    /** 迁移状态 DataStore */
    private val Context.migrationStore by preferencesDataStore(name = "migration_state")

    /**
     * 执行迁移（幂等，只会执行一次）
     */
    fun migrateIfNeeded(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val done = context.migrationStore.data
                    .map { prefs -> prefs[booleanPreferencesKey(KEY_MIGRATION_DONE)] ?: false }
                    .first()

                if (done) {
                    Log.d(TAG, "迁移已完成，跳过")
                    return@launch
                }

                Log.d(TAG, "开始数据迁移 v0.1.2 → v0.1.3")
                performMigration(context)

                context.migrationStore.edit { prefs ->
                    prefs[booleanPreferencesKey(KEY_MIGRATION_DONE)] = true
                }
                Log.d(TAG, "数据迁移完成")
            } catch (e: Exception) {
                Log.e(TAG, "迁移失败", e)
            }
        }
    }

    private suspend fun performMigration(context: Context) {
        val profileRepo = WidgetProfileRepository(context)
        val mappingRepo = WidgetMappingRepository(context)

        // 1. 读取旧版配置 — 从键名中提取 appWidgetId
        val oldPrefs = context.oldWidgetConfigStore.data.first()
        val idSet = mutableSetOf<Int>()
        for (key in oldPrefs.asMap().keys) {
            val keyName = key.name
            if (keyName.startsWith("username_")) {
                val id = keyName.removePrefix("username_").toIntOrNull()
                if (id != null) idSet.add(id)
            }
        }
        val oldIds = idSet.toList()

        if (oldIds.isEmpty()) {
            Log.d(TAG, "没有旧版配置需要迁移")
            return
        }

        Log.d(TAG, "找到 ${oldIds.size} 个旧版配置, 开始迁移")

        for (appWidgetId in oldIds) {
            try {
                // 2. 读取旧配置
                val username = oldPrefs[stringPreferencesKey("username_$appWidgetId")] ?: ""
                val color = oldPrefs[stringPreferencesKey("color_$appWidgetId")] ?: WidgetProfile.DEFAULT_COLOR
                val refresh = oldPrefs[longPreferencesKey("refresh_$appWidgetId")] ?: WidgetProfile.DEFAULT_REFRESH_INTERVAL
                val quietStart = oldPrefs[intPreferencesKey("quiet_start_$appWidgetId")] ?: WidgetProfile.DEFAULT_QUIET_START
                val quietEnd = oldPrefs[intPreferencesKey("quiet_end_$appWidgetId")] ?: WidgetProfile.DEFAULT_QUIET_END
                val theme = oldPrefs[stringPreferencesKey("theme_$appWidgetId")] ?: WidgetProfile.DEFAULT_THEME

                // 3. 创建 Profile
                val profileId = UUID.randomUUID().toString()
                val profileName = if (username.isNotBlank()) "@$username" else "未命名 #$appWidgetId"
                val profile = WidgetProfile(
                    id = profileId,
                    name = profileName,
                    username = username,
                    color = color,
                    refreshIntervalMinutes = refresh,
                    quietHourStart = quietStart,
                    quietHourEnd = quietEnd,
                    theme = theme
                )
                profileRepo.saveProfile(profile)

                // 4. 建立映射
                mappingRepo.setWidgetProfile(appWidgetId, profileId)

                Log.d(TAG, "迁移 widget=$appWidgetId → profile=$profileId ($profileName)")

            } catch (e: Exception) {
                Log.e(TAG, "迁移 widget=$appWidgetId 失败", e)
            }
        }

        Log.d(TAG, "旧版配置迁移完毕, 共迁移 ${oldIds.size} 个")
    }

    /**
     * 清理旧版 WorkManager 任务
     * 升级安装后，旧版本入队的 Worker 可能使用旧的类/依赖，需要清理以防冲突。
     */
    fun cleanupStaleWork(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val workManager = WorkManager.getInstance(context)
                workManager.cancelAllWorkByTag(com.zhuimeng.dreambox.widget.GithubWidgetWorker.WORK_TAG)
                Log.d(TAG, "已清理旧版 WorkManager 任务")
            } catch (e: Exception) {
                Log.e(TAG, "清理旧版 WorkManager 任务失败", e)
            }
        }
    }
}

// DataStore 中读取值需要这些访问函数
private fun stringPreferencesKey(name: String) = androidx.datastore.preferences.core.stringPreferencesKey(name)
private fun intPreferencesKey(name: String) = androidx.datastore.preferences.core.intPreferencesKey(name)
private fun longPreferencesKey(name: String) = androidx.datastore.preferences.core.longPreferencesKey(name)
