package com.zhuimeng.dreambox.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.zhuimeng.dreambox.data.WidgetConfig
import com.zhuimeng.dreambox.data.WidgetConfigRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Widget 配置界面
 *
 * 当用户首次添加 Widget 到桌面时，此 Activity 会被启动，
 * 填写基础信息后保存，Widget 会被添加到桌面并立即刷新。
 */
@AndroidEntryPoint
class WidgetConfigureActivity : ComponentActivity() {

    @Inject
    lateinit var configRepository: WidgetConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // 先取已有配置（如果用户之前配过）
        var existingUsername = ""
        var existingColor = WidgetConfig.DEFAULT_COLOR
        lifecycleScope.launch {
            val config = configRepository.getConfigSnapshot(appWidgetId)
            existingUsername = config.username
            existingColor = config.color
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
                ConfigureWidgetScreen(
                    appWidgetId = appWidgetId,
                    initialUsername = existingUsername,
                    initialColor = existingColor,
                    onSave = { config ->
                        lifecycleScope.launch {
                            // 保存配置
                            configRepository.saveConfig(config)
                            // 触发一次刷新
                            GithubWidgetWorker.startPeriodicRefresh(
                                this@WidgetConfigureActivity,
                                appWidgetId,
                                config.refreshIntervalMinutes
                            )
                            GithubWidgetWorker.enqueueRefresh(
                                this@WidgetConfigureActivity,
                                intArrayOf(appWidgetId)
                            )
                            // 通知系统 Widget 已配置完成
                            saveAndExit(appWidgetId)
                        }
                    },
                    onSkip = {
                        lifecycleScope.launch {
                            // 保存默认配置，使 widget 出现在 App 设置列表中
                            configRepository.saveConfig(
                                WidgetConfig(appWidgetId = appWidgetId)
                            )
                            saveAndExit(appWidgetId)
                        }
                    }
                )
            }
        }
    }

    private fun saveAndExit(appWidgetId: Int) {
        val resultValue = Intent().putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            appWidgetId
        )
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigureWidgetScreen(
    appWidgetId: Int,
    initialUsername: String,
    initialColor: String,
    onSave: (WidgetConfig) -> Unit,
    onSkip: () -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var color by remember { mutableStateOf(initialColor) }
    var refreshInterval by remember { mutableStateOf("60") }
    var isDarkTheme by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget 配置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("跳过，稍后配置")
                    }
                    Button(
                        onClick = {
                            onSave(
                                WidgetConfig(
                                    appWidgetId = appWidgetId,
                                    username = username.trim(),
                                    color = color.trim().ifBlank { WidgetConfig.DEFAULT_COLOR },
                                    refreshIntervalMinutes = refreshInterval.toLongOrNull()
                                        ?: WidgetConfig.DEFAULT_REFRESH_INTERVAL,
                                    theme = if (isDarkTheme) WidgetConfig.THEME_DARK
                                        else WidgetConfig.THEME_LIGHT
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = username.isNotBlank()
                    ) {
                        Text("保存并添加")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 说明
            Text(
                text = "GitHub 用户名必填，其他可使用默认值。\n配置后随时可以在 App 中修改。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 用户名
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("GitHub 用户名") },
                placeholder = { Text("如 zhuimeng-hstc") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 颜色
            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("热力图颜色") },
                placeholder = { Text("如 198754 (绿色)") },
                supportingText = { Text("十六进制色码，不含 #") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 刷新间隔
            OutlinedTextField(
                value = refreshInterval,
                onValueChange = { refreshInterval = it },
                label = { Text("刷新间隔（分钟）") },
                placeholder = { Text("60") },
                supportingText = { Text("最小 15 分钟，手动刷新不受限") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 主题选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "暗色背景",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { isDarkTheme = it }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}


