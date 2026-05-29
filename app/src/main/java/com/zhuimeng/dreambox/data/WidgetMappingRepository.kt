package com.zhuimeng.dreambox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Widget↔Profile 映射 DataStore 实例 */
private val Context.widgetMappingStore by preferencesDataStore(name = "widget_mapping")

/**
 * Widget ↔ Profile 映射仓库
 *
 * 管理 appWidgetId → profileId 的映射关系。
 * 一个 Profile 可被多个 Widget 引用，一个 Widget 只能引用一个 Profile。
 */
@Singleton
class WidgetMappingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** 获取指定 Widget 绑定的 Profile ID */
    fun getProfileIdForWidget(appWidgetId: Int): Flow<String?> {
        return context.widgetMappingStore.data.map { prefs ->
            prefs[mappingKey(appWidgetId)]
        }
    }

    /** 获取指定 Widget 绑定的 Profile ID（快照） */
    suspend fun getProfileIdForWidgetSnapshot(appWidgetId: Int): String? {
        return context.widgetMappingStore.data.map { prefs ->
            prefs[mappingKey(appWidgetId)]
        }.first()
    }

    /** 绑定 Widget 到指定 Profile */
    suspend fun setWidgetProfile(appWidgetId: Int, profileId: String) {
        context.widgetMappingStore.edit { prefs ->
            prefs[mappingKey(appWidgetId)] = profileId
        }
    }

    /** 解除 Widget 绑定（删除映射） */
    suspend fun removeWidget(appWidgetId: Int) {
        context.widgetMappingStore.edit { prefs ->
            prefs.remove(mappingKey(appWidgetId))
        }
    }

    /** 获取引用指定 Profile 的所有 Widget ID */
    fun getWidgetsForProfile(profileId: String): Flow<List<Int>> {
        return context.widgetMappingStore.data.map { prefs ->
            prefs.asMap().entries
                .filter { it.value == profileId }
                .mapNotNull { entry ->
                    val name = entry.key.name
                    if (name.startsWith(KEY_PREFIX_MAPPING)) {
                        name.removePrefix(KEY_PREFIX_MAPPING).toIntOrNull()
                    } else null
                }
        }
    }

    /** 获取引用指定 Profile 的 Widget 数（快照） */
    suspend fun getWidgetCountForProfileSnapshot(profileId: String): Int {
        return context.widgetMappingStore.data.map { prefs ->
            prefs.asMap().values.count { it == profileId }
        }.first()
    }

    /** 获取所有有映射的 Widget ID 列表 */
    fun getAllMappedWidgetIds(): Flow<List<Int>> {
        return context.widgetMappingStore.data.map { prefs ->
            prefs.asMap().keys.mapNotNull { key ->
                val name = key.name
                if (name.startsWith(KEY_PREFIX_MAPPING)) {
                    name.removePrefix(KEY_PREFIX_MAPPING).toIntOrNull()
                } else null
            }
        }
    }

    /** 检查指定 Profile 是否有活跃 Widget 引用 */
    suspend fun isProfileActive(profileId: String): Boolean {
        return getWidgetCountForProfileSnapshot(profileId) > 0
    }

    companion object {
        private const val KEY_PREFIX_MAPPING = "widget_"

        private fun mappingKey(appWidgetId: Int) =
            stringPreferencesKey("$KEY_PREFIX_MAPPING${appWidgetId}_profile")
    }
}
