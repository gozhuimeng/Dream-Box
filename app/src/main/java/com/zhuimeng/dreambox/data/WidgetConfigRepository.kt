package com.zhuimeng.dreambox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore 实例（单例） */
private val Context.widgetConfigStore by preferencesDataStore(name = "widget_config")

/**
 * Widget 配置存储仓库
 *
 * 使用 DataStore Preferences 存储每个 widget 实例的配置。
 * 以 appWidgetId 为键前缀，支持多用户独立配置。
 */
@Singleton
class WidgetConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** 获取指定 widget 的配置（返回 Flow，监听变化） */
    fun getConfig(appWidgetId: Int): Flow<WidgetConfig> {
        return context.widgetConfigStore.data.map { prefs ->
            WidgetConfig(
                appWidgetId = appWidgetId,
                username = prefs[usernameKey(appWidgetId)] ?: "",
                color = prefs[colorKey(appWidgetId)] ?: WidgetConfig.DEFAULT_COLOR,
                refreshIntervalMinutes = prefs[refreshKey(appWidgetId)]
                    ?: WidgetConfig.DEFAULT_REFRESH_INTERVAL,
                quietHourStart = prefs[quietStartKey(appWidgetId)]
                    ?: WidgetConfig.DEFAULT_QUIET_START,
                quietHourEnd = prefs[quietEndKey(appWidgetId)]
                    ?: WidgetConfig.DEFAULT_QUIET_END,
                theme = prefs[themeKey(appWidgetId)] ?: WidgetConfig.DEFAULT_THEME
            )
        }
    }

    /** 获取指定 widget 的配置快照（一次性读取，用于 Worker） */
    suspend fun getConfigSnapshot(appWidgetId: Int): WidgetConfig {
        return context.widgetConfigStore.data.map { prefs ->
            WidgetConfig(
                appWidgetId = appWidgetId,
                username = prefs[usernameKey(appWidgetId)] ?: "",
                color = prefs[colorKey(appWidgetId)] ?: WidgetConfig.DEFAULT_COLOR,
                refreshIntervalMinutes = prefs[refreshKey(appWidgetId)]
                    ?: WidgetConfig.DEFAULT_REFRESH_INTERVAL,
                quietHourStart = prefs[quietStartKey(appWidgetId)]
                    ?: WidgetConfig.DEFAULT_QUIET_START,
                quietHourEnd = prefs[quietEndKey(appWidgetId)]
                    ?: WidgetConfig.DEFAULT_QUIET_END,
                theme = prefs[themeKey(appWidgetId)] ?: WidgetConfig.DEFAULT_THEME
            )
        }.first()
    }

    /** 保存 widget 配置 */
    suspend fun saveConfig(config: WidgetConfig) {
        context.widgetConfigStore.edit { prefs ->
            prefs[usernameKey(config.appWidgetId)] = config.username
            prefs[colorKey(config.appWidgetId)] = config.color
            prefs[refreshKey(config.appWidgetId)] = config.refreshIntervalMinutes
            prefs[quietStartKey(config.appWidgetId)] = config.quietHourStart
            prefs[quietEndKey(config.appWidgetId)] = config.quietHourEnd
            prefs[themeKey(config.appWidgetId)] = config.theme
        }
    }

    /** 删除 widget 配置 */
    suspend fun deleteConfig(appWidgetId: Int) {
        context.widgetConfigStore.edit { prefs ->
            prefs.remove(usernameKey(appWidgetId))
            prefs.remove(colorKey(appWidgetId))
            prefs.remove(refreshKey(appWidgetId))
            prefs.remove(quietStartKey(appWidgetId))
            prefs.remove(quietEndKey(appWidgetId))
            prefs.remove(themeKey(appWidgetId))
        }
    }

    /** 获取所有已配置的 widget ID 列表 */
    fun getAllWidgetIds(): Flow<List<Int>> {
        // 从存储的键中解析出 appWidgetId
        return context.widgetConfigStore.data.map { prefs ->
            prefs.asMap().keys.mapNotNull { key ->
                // 查找所有 username_ 前缀的键，提取 appWidgetId
                val name = key.name
                if (name.startsWith(KEY_PREFIX_USERNAME)) {
                    name.removePrefix(KEY_PREFIX_USERNAME).toIntOrNull()
                } else null
            }
        }
    }

    companion object {
        private const val KEY_PREFIX_USERNAME = "username_"
        private const val KEY_PREFIX_COLOR = "color_"
        private const val KEY_PREFIX_REFRESH = "refresh_"
        private const val KEY_PREFIX_QUIET_START = "quiet_start_"
        private const val KEY_PREFIX_QUIET_END = "quiet_end_"
        private const val KEY_PREFIX_THEME = "theme_"

        private fun usernameKey(id: Int) = stringPreferencesKey("$KEY_PREFIX_USERNAME$id")
        private fun colorKey(id: Int) = stringPreferencesKey("$KEY_PREFIX_COLOR$id")
        private fun refreshKey(id: Int) = longPreferencesKey("$KEY_PREFIX_REFRESH$id")
        private fun quietStartKey(id: Int) = intPreferencesKey("$KEY_PREFIX_QUIET_START$id")
        private fun quietEndKey(id: Int) = intPreferencesKey("$KEY_PREFIX_QUIET_END$id")
        private fun themeKey(id: Int) = stringPreferencesKey("$KEY_PREFIX_THEME$id")

        /** 不使用 Hilt 的静态删除方法（用于 Provider onDeleted 等场景） */
        suspend fun deleteWidgetConfig(context: Context, appWidgetId: Int) {
            context.widgetConfigStore.edit { prefs ->
                prefs.remove(usernameKey(appWidgetId))
                prefs.remove(colorKey(appWidgetId))
                prefs.remove(refreshKey(appWidgetId))
                prefs.remove(quietStartKey(appWidgetId))
                prefs.remove(quietEndKey(appWidgetId))
                prefs.remove(themeKey(appWidgetId))
            }
        }
    }
}
