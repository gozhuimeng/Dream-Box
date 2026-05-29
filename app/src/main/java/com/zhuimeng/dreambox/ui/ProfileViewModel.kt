package com.zhuimeng.dreambox.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhuimeng.dreambox.data.WidgetMappingRepository
import com.zhuimeng.dreambox.data.WidgetProfile
import com.zhuimeng.dreambox.data.WidgetProfileRepository
import com.zhuimeng.dreambox.widget.GithubWidgetWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Profile 管理 ViewModel
 *
 * 管理独立的配置项（Profile），与 Widget 解耦。
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: WidgetProfileRepository,
    private val mappingRepository: WidgetMappingRepository,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    /** 所有 Profile 列表 */
    private val _profiles = MutableStateFlow<List<WidgetProfile>>(emptyList())
    val profiles: StateFlow<List<WidgetProfile>> = _profiles.asStateFlow()

    /** 编辑中的 Profile */
    private val _editingProfile = MutableStateFlow<WidgetProfile?>(null)
    val editingProfile: StateFlow<WidgetProfile?> = _editingProfile.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            profileRepository.getAllProfileIds().collect { ids ->
                val items = ids.map { id -> profileRepository.getProfileSnapshot(id) }
                _profiles.value = items
            }
        }
    }

    /** 创建默认 Profile */
    fun createProfile() {
        val newProfile = WidgetProfile(
            id = UUID.randomUUID().toString(),
            name = "新配置"
        )
        _editingProfile.value = newProfile
    }

    /** 开始编辑 Profile */
    fun editProfile(profile: WidgetProfile) {
        _editingProfile.value = profile
    }

    /** 取消编辑 */
    fun cancelEdit() {
        _editingProfile.value = null
    }

    /** 保存 Profile */
    fun saveProfile(profile: WidgetProfile) {
        viewModelScope.launch {
            val p = if (profile.id.isBlank()) {
                profile.copy(id = UUID.randomUUID().toString())
            } else {
                profile
            }
            // 自动填充名称
            val finalProfile = if (p.name.isBlank()) {
                p.copy(name = if (p.username.isNotBlank()) "@${p.username}" else "未命名")
            } else {
                p
            }
            profileRepository.saveProfile(finalProfile)
            _editingProfile.value = null
            Log.d(TAG, "保存 Profile: ${finalProfile.id} name=${finalProfile.name}")
        }
    }

    /** 删除 Profile */
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            // 先检查是否有 widget 引用此 profile
            val widgetIds = mappingRepository.getWidgetsForProfile(profileId).first()
            if (widgetIds.isNotEmpty()) {
                // 清理引用此 profile 的所有 widget 映射
                widgetIds.forEach { appWidgetId ->
                    mappingRepository.removeWidget(appWidgetId)
                    GithubWidgetWorker.cancelPeriodicRefresh(getApplication(), appWidgetId)
                    Log.d(TAG, "删除 profile 时清理 widget 映射: widget=$appWidgetId")
                }
            }
            // 删除 profile
            profileRepository.deleteProfile(profileId)
            Log.d(TAG, "删除 Profile: $profileId")
        }
    }

    /** 获取指定 Profile 的 widget 引用数 */
    suspend fun getWidgetCount(profileId: String): Int {
        return mappingRepository.getWidgetCountForProfileSnapshot(profileId)
    }
}
