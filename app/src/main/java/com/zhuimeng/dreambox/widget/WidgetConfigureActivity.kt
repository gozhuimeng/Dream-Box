package com.zhuimeng.dreambox.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.zhuimeng.dreambox.R
import com.zhuimeng.dreambox.data.WidgetMappingRepository
import com.zhuimeng.dreambox.data.WidgetProfile
import com.zhuimeng.dreambox.data.WidgetProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Widget 配置/Profile 选择界面
 *
 * 两种启动方式:
 * 1. 添加新 Widget 时由系统启动（APPWIDGET_CONFIGURE 流程）
 * 2. 点击已有 Widget 时启动（ACTION_PICK_PROFILE 自定义 action）
 */
@AndroidEntryPoint
class WidgetConfigureActivity : ComponentActivity() {

    companion object {
        private const val TAG = "WidgetConfigureAct"
        const val ACTION_PICK_PROFILE = "com.zhuimeng.dreambox.action.PICK_PROFILE"
    }

    @Inject
    lateinit var profileRepository: WidgetProfileRepository

    @Inject
    lateinit var mappingRepository: WidgetMappingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val isAddFlow = intent?.action != ACTION_PICK_PROFILE

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "无效的 appWidgetId")
            finish()
            return
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF90CAF9),
                    secondary = Color(0xFF80CBC4),
                    surface = Color(0xFF1C1B1F),
                    background = Color(0xFF121212)
                )
            ) {
                ProfilePickerScreen(
                    appWidgetId = appWidgetId,
                    isAddFlow = isAddFlow,
                    profileRepository = profileRepository,
                    mappingRepository = mappingRepository,
                    onProfileSelected = { profileId ->
                        lifecycleScope.launch {
                            bindWidgetToProfile(appWidgetId, profileId, isAddFlow)
                        }
                    },
                    onCreateNew = { name, username, color, theme ->
                        lifecycleScope.launch {
                            val profileId = UUID.randomUUID().toString()
                            val profile = WidgetProfile(
                                id = profileId,
                                name = name.ifBlank { "@$username" },
                                username = username,
                                color = color.ifBlank { WidgetProfile.DEFAULT_COLOR },
                                theme = theme
                            )
                            profileRepository.saveProfile(profile)
                            Log.d(TAG, "创建新 Profile: $profileId name=${profile.name}")
                            bindWidgetToProfile(appWidgetId, profileId, isAddFlow)
                        }
                    }
                )
            }
        }
    }

    private suspend fun bindWidgetToProfile(appWidgetId: Int, profileId: String, isAddFlow: Boolean) {
        // 1) 先验证 Profile 是否存在且有效
        val verifyProfile = profileRepository.getProfileSnapshot(profileId)
        Log.d(TAG, "验证 Profile: id=${verifyProfile.id} name=${verifyProfile.name} user=${verifyProfile.username} dark=${verifyProfile.theme}")
        if (verifyProfile.username.isBlank()) {
            Log.w(TAG, "Profile $profileId 用户名为空, 仍继续绑定（Worker 会显示错误）")
        }

        // 2) 写入映射关系
        mappingRepository.setWidgetProfile(appWidgetId, profileId)
        Log.d(TAG, "绑定: widget=$appWidgetId → profile=$profileId")

        // 验证映射已写入
        val readback = mappingRepository.getProfileIdForWidgetSnapshot(appWidgetId)
        Log.d(TAG, "映射回读验证: widget=$appWidgetId → profile=$readback")

        // 不替换整个 Widget，只把时间戳改为"切换中..."，保持旧图表可见
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val layoutResId = if (isTinyWidget(appWidgetId)) R.layout.github_widget_layout_tiny
            else R.layout.github_widget_layout
        val partialViews = RemoteViews(packageName, layoutResId)
        try { partialViews.setTextViewText(R.id.widget_timestamp, "切换中...") } catch (_: Exception) {}
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, partialViews)

        // 触发一次刷新（强制替换已有任务）
        val profile = profileRepository.getProfileSnapshot(profileId)
        Log.d(TAG, "启动刷新: widget=$appWidgetId profile=$profileId interval=${profile.refreshIntervalMinutes}")
        GithubWidgetWorker.startPeriodicRefresh(this, appWidgetId, profile.refreshIntervalMinutes)
        GithubWidgetWorker.forceRefresh(this, intArrayOf(appWidgetId))

        if (isAddFlow) {
            val resultValue = Intent().putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                appWidgetId
            )
            setResult(RESULT_OK, resultValue)
            finish()
        } else {
            // 选择流程：直接关闭并返回桌面
            finishAndRemoveTask()
        }
    }

    private fun isTinyWidget(appWidgetId: Int): Boolean {
        val manager = AppWidgetManager.getInstance(this)
        val options = manager.getAppWidgetOptions(appWidgetId)
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        return minWidthDp < 130
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePickerScreen(
    appWidgetId: Int,
    isAddFlow: Boolean,
    profileRepository: WidgetProfileRepository,
    mappingRepository: WidgetMappingRepository,
    onProfileSelected: (profileId: String) -> Unit,
    onCreateNew: (name: String, username: String, color: String, theme: String) -> Unit
) {
    val allProfileIds by profileRepository.getAllProfileIds()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // 逐个读取 Profile 详情（简化：直接用 LaunchedEffect 批量读取）
    var profiles by remember { mutableStateOf<List<WidgetProfile>>(emptyList()) }
    LaunchedEffect(allProfileIds) {
        profiles = allProfileIds.map { id ->
            profileRepository.getProfileSnapshot(id)
        }
    }

    val currentProfileId by mappingRepository
        .getProfileIdForWidget(appWidgetId)
        .collectAsStateWithLifecycle(initialValue = null)

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isAddFlow) "选择配置" else "切换配置")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "新建") },
                text = { Text("新建配置") }
            )
        }
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "还没有配置项",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击下方按钮创建第一个 GitHub 配置",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        isSelected = profile.id == currentProfileId,
                        onSelect = { onProfileSelected(profile.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, username, color, theme ->
                showCreateDialog = false
                onCreateNew(name, username, color, theme)
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: WidgetProfile,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            CardDefaults.outlinedCardBorder()
        else
            null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = RoundedCornerShape(6.dp),
                color = parseColor(profile.color)
            ) {}

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name.ifBlank { profile.username.ifBlank { "未命名" } },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (profile.username.isNotBlank()) {
                    Text(
                        text = "@${profile.username}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Text(
                    text = "当前使用",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor("#$hex"))
    } catch (_: Exception) {
        Color(android.graphics.Color.parseColor("#198754"))
    }
}

@Composable
private fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, username: String, color: String, theme: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(WidgetProfile.DEFAULT_COLOR) }
    var isDarkTheme by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("配置名称（选填）") },
                    placeholder = { Text("如 工作号、小号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("GitHub 用户名") },
                    placeholder = { Text("必填") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("热力图颜色（十六进制）") },
                    placeholder = { Text("198754") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("暗色背景", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { isDarkTheme = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (username.isBlank()) return@TextButton
                    onConfirm(
                        name.trim(),
                        username.trim(),
                        color.trim().ifBlank { WidgetProfile.DEFAULT_COLOR },
                        if (isDarkTheme) WidgetProfile.THEME_DARK else WidgetProfile.THEME_LIGHT
                    )
                },
                enabled = username.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
