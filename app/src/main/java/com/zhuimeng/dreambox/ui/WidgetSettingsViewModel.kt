package com.zhuimeng.dreambox.ui

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhuimeng.dreambox.data.WidgetConfig
import com.zhuimeng.dreambox.data.WidgetConfigRepository
import com.zhuimeng.dreambox.widget.GithubWidgetProvider
import com.zhuimeng.dreambox.widget.GithubWidgetWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    private val application: Application,
    private val configRepository: WidgetConfigRepository
) : AndroidViewModel(application) {

    /** 所有配置的 widget 列表 */
    private val _widgetConfigs = MutableStateFlow<List<WidgetConfig>>(emptyList())
    val widgetConfigs: StateFlow<List<WidgetConfig>> = _widgetConfigs.asStateFlow()

    /** 编辑中的配置 */
    private val _editingConfig = MutableStateFlow<WidgetConfig?>(null)
    val editingConfig: StateFlow<WidgetConfig?> = _editingConfig.asStateFlow()

    init {
        // 监听所有配置变化
        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            configRepository.getAllWidgetIds()
                .flatMapLatest { ids ->
                    if (ids.isEmpty()) {
                        kotlinx.coroutines.flow.flowOf(emptyList())
                    } else {
                        combine(
                            ids.map { id -> configRepository.getConfig(id) }
                        ) { configs -> configs.toList() }
                    }
                }
                .collect { configs ->
                    _widgetConfigs.value = configs
                }
        }
    }

    /** 保存配置 */
    fun saveConfig(config: WidgetConfig) {
        viewModelScope.launch {
            configRepository.saveConfig(config)
            // 启动周期性刷新
            GithubWidgetWorker.startPeriodicRefresh(
                application,
                config.appWidgetId,
                config.refreshIntervalMinutes
            )
            // 立即刷新一次
            triggerRefresh(config.appWidgetId)
            _editingConfig.value = null
        }
    }

    /** 删除配置 */
    fun deleteConfig(appWidgetId: Int) {
        viewModelScope.launch {
            configRepository.deleteConfig(appWidgetId)
            GithubWidgetWorker.cancelPeriodicRefresh(application, appWidgetId)
        }
    }

    /** 立即刷新指定 widget */
    fun triggerRefresh(appWidgetId: Int) {
        viewModelScope.launch {
            val config = configRepository.getConfigSnapshot(appWidgetId)
            val appWidgetManager = AppWidgetManager.getInstance(application)
            GithubWidgetProvider.updateWidgetUi(
                application, appWidgetManager, appWidgetId
            )
            GithubWidgetWorker.enqueueRefresh(
                application, intArrayOf(appWidgetId)
            )
        }
    }

    /** 开始编辑配置 */
    fun editConfig(config: WidgetConfig) {
        _editingConfig.value = config
    }

    /** 为新 widget 创建默认配置 */
    fun createDefaultConfig(appWidgetId: Int) {
        _editingConfig.value = WidgetConfig(appWidgetId = appWidgetId)
    }

    /** 取消编辑 */
    fun cancelEdit() {
        _editingConfig.value = null
    }
}
