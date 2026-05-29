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

/** Profile DataStore 实例 */
private val Context.profileStore by preferencesDataStore(name = "profile_config")

/**
 * Profile 配置存储仓库
 *
 * 按 profileId（UUID）独立存储，与 appWidgetId 解耦。
 * 一个 Profile 可被多个 Widget 引用。
 */
@Singleton
class WidgetProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** 获取指定 Profile */
    fun getProfile(profileId: String): Flow<WidgetProfile> {
        return context.profileStore.data.map { prefs ->
            WidgetProfile(
                id = profileId,
                name = prefs[nameKey(profileId)] ?: "",
                username = prefs[usernameKey(profileId)] ?: "",
                color = prefs[colorKey(profileId)] ?: WidgetProfile.DEFAULT_COLOR,
                refreshIntervalMinutes = prefs[refreshKey(profileId)]
                    ?: WidgetProfile.DEFAULT_REFRESH_INTERVAL,
                quietHourStart = prefs[quietStartKey(profileId)]
                    ?: WidgetProfile.DEFAULT_QUIET_START,
                quietHourEnd = prefs[quietEndKey(profileId)]
                    ?: WidgetProfile.DEFAULT_QUIET_END,
                theme = prefs[themeKey(profileId)] ?: WidgetProfile.DEFAULT_THEME
            )
        }
    }

    /** 获取指定 Profile 快照（一次性读取） */
    suspend fun getProfileSnapshot(profileId: String): WidgetProfile {
        return context.profileStore.data.map { prefs ->
            WidgetProfile(
                id = profileId,
                name = prefs[nameKey(profileId)] ?: "",
                username = prefs[usernameKey(profileId)] ?: "",
                color = prefs[colorKey(profileId)] ?: WidgetProfile.DEFAULT_COLOR,
                refreshIntervalMinutes = prefs[refreshKey(profileId)]
                    ?: WidgetProfile.DEFAULT_REFRESH_INTERVAL,
                quietHourStart = prefs[quietStartKey(profileId)]
                    ?: WidgetProfile.DEFAULT_QUIET_START,
                quietHourEnd = prefs[quietEndKey(profileId)]
                    ?: WidgetProfile.DEFAULT_QUIET_END,
                theme = prefs[themeKey(profileId)] ?: WidgetProfile.DEFAULT_THEME
            )
        }.first()
    }

    /** 获取所有 Profile ID 列表 */
    fun getAllProfileIds(): Flow<List<String>> {
        return context.profileStore.data.map { prefs ->
            prefs.asMap().keys.mapNotNull { key ->
                val name = key.name
                if (name.startsWith(KEY_PREFIX_NAME)) {
                    name.removePrefix(KEY_PREFIX_NAME)
                } else null
            }
        }
    }

    /** 保存 Profile */
    suspend fun saveProfile(profile: WidgetProfile) {
        context.profileStore.edit { prefs ->
            prefs[nameKey(profile.id)] = profile.name
            prefs[usernameKey(profile.id)] = profile.username
            prefs[colorKey(profile.id)] = profile.color
            prefs[refreshKey(profile.id)] = profile.refreshIntervalMinutes
            prefs[quietStartKey(profile.id)] = profile.quietHourStart
            prefs[quietEndKey(profile.id)] = profile.quietHourEnd
            prefs[themeKey(profile.id)] = profile.theme
        }
    }

    /** 删除 Profile */
    suspend fun deleteProfile(profileId: String) {
        context.profileStore.edit { prefs ->
            prefs.remove(nameKey(profileId))
            prefs.remove(usernameKey(profileId))
            prefs.remove(colorKey(profileId))
            prefs.remove(refreshKey(profileId))
            prefs.remove(quietStartKey(profileId))
            prefs.remove(quietEndKey(profileId))
            prefs.remove(themeKey(profileId))
        }
    }

    companion object {
        private const val KEY_PREFIX_NAME = "profile_name_"
        private const val KEY_PREFIX_USERNAME = "profile_username_"
        private const val KEY_PREFIX_COLOR = "profile_color_"
        private const val KEY_PREFIX_REFRESH = "profile_refresh_"
        private const val KEY_PREFIX_QUIET_START = "profile_quiet_start_"
        private const val KEY_PREFIX_QUIET_END = "profile_quiet_end_"
        private const val KEY_PREFIX_THEME = "profile_theme_"

        private fun nameKey(id: String) = stringPreferencesKey("$KEY_PREFIX_NAME$id")
        private fun usernameKey(id: String) = stringPreferencesKey("$KEY_PREFIX_USERNAME$id")
        private fun colorKey(id: String) = stringPreferencesKey("$KEY_PREFIX_COLOR$id")
        private fun refreshKey(id: String) = longPreferencesKey("$KEY_PREFIX_REFRESH$id")
        private fun quietStartKey(id: String) = intPreferencesKey("$KEY_PREFIX_QUIET_START$id")
        private fun quietEndKey(id: String) = intPreferencesKey("$KEY_PREFIX_QUIET_END$id")
        private fun themeKey(id: String) = stringPreferencesKey("$KEY_PREFIX_THEME$id")
    }
}
