package com.zhuimeng.dreambox

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhuimeng.dreambox.data.WidgetConfig
import com.zhuimeng.dreambox.ui.WidgetSettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                DreamboxTheme()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamboxTheme(
    viewModel: WidgetSettingsViewModel = viewModel()
) {
    val widgetConfigs by viewModel.widgetConfigs.collectAsStateWithLifecycle()
    val editingConfig by viewModel.editingConfig.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dreambox", fontWeight = FontWeight.Bold)
                        Text(
                            "坠梦 · 个人工具箱",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            val context = LocalContext.current
            ExtendedFloatingActionButton(
                onClick = {
                    Toast.makeText(
                        context,
                        "请在桌面长按空白处 → 添加 Widget 来添加新的 GitHub 热力图",
                        Toast.LENGTH_LONG
                    ).show()
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "添加") },
                text = { Text("添加 Widget") }
            )
        }
    ) { padding ->
        if (widgetConfigs.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "暂无 Widget 配置",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击下方按钮添加 GitHub 贡献热力图 Widget",
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
                items(widgetConfigs, key = { it.appWidgetId }) { config ->
                    WidgetConfigCard(
                        config = config,
                        onEdit = { viewModel.editConfig(config) },
                        onDelete = { viewModel.deleteConfig(config.appWidgetId) },
                        onRefresh = { viewModel.triggerRefresh(config.appWidgetId) }
                    )
                }
            }
        }

        // 编辑对话框
        editingConfig?.let { config ->
            WidgetConfigDialog(
                config = config,
                onSave = { viewModel.saveConfig(it) },
                onDismiss = { viewModel.cancelEdit() }
            )
        }
    }
}

@Composable
fun WidgetConfigCard(
    config: WidgetConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行：用户名 + ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 颜色指示器
                    Surface(
                        modifier = Modifier.size(16.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = hexToColor(config.color)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = config.username.ifBlank { "未设置" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "#${config.appWidgetId}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 详情行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailItem("刷新", "每 ${config.refreshIntervalMinutes} 分钟")
                DetailItem("安静时段", "${config.quietHourStart}:00 - ${config.quietHourEnd}:00")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Widget 配置编辑对话框
 */
@Composable
fun WidgetConfigDialog(
    config: WidgetConfig,
    onSave: (WidgetConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(config.username) }
    var color by remember { mutableStateOf(config.color) }
    var refreshInterval by remember {
        mutableStateOf(config.refreshIntervalMinutes.toString())
    }
    var quietStart by remember { mutableStateOf(config.quietHourStart.toString()) }
    var quietEnd by remember { mutableStateOf(config.quietHourEnd.toString()) }
    var isDarkTheme by remember { mutableStateOf(config.theme == WidgetConfig.THEME_DARK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Widget 配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("GitHub 用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("热力图颜色（十六进制）") },
                    placeholder = { Text("如 198754") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = refreshInterval,
                    onValueChange = { refreshInterval = it },
                    label = { Text("刷新间隔（分钟）") },
                    placeholder = { Text("60") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quietStart,
                        onValueChange = { quietStart = it },
                        label = { Text("安静时段开始") },
                        placeholder = { Text("22") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quietEnd,
                        onValueChange = { quietEnd = it },
                        label = { Text("安静时段结束") },
                        placeholder = { Text("7") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                // 暗色主题开关
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
            TextButton(onClick = {
                val updated = config.copy(
                    username = username.trim(),
                    color = color.trim().ifBlank { WidgetConfig.DEFAULT_COLOR },
                    refreshIntervalMinutes = refreshInterval.toLongOrNull()
                        ?: WidgetConfig.DEFAULT_REFRESH_INTERVAL,
                    quietHourStart = quietStart.toIntOrNull()
                        ?: WidgetConfig.DEFAULT_QUIET_START,
                    quietHourEnd = quietEnd.toIntOrNull()
                        ?: WidgetConfig.DEFAULT_QUIET_END,
                    theme = if (isDarkTheme) WidgetConfig.THEME_DARK
                        else WidgetConfig.THEME_LIGHT
                )
                onSave(updated)
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/** 十六进制颜色字符串 → Compose Color */
@Composable
private fun hexToColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor("#$hex"))
    } catch (_: Exception) {
        Color(android.graphics.Color.parseColor("#198754"))
    }
}
