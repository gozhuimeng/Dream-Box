package com.zhuimeng.dreambox

import android.os.Bundle
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhuimeng.dreambox.data.WidgetProfile
import com.zhuimeng.dreambox.ui.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
    viewModel: ProfileViewModel = viewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val editingProfile by viewModel.editingProfile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

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
            ExtendedFloatingActionButton(
                onClick = { viewModel.createProfile() },
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
                        "暂无配置",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击下方按钮创建 GitHub 配置，\n然后在桌面添加 Widget 选择使用",
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
                        onEdit = { viewModel.editProfile(profile) },
                        onDelete = { viewModel.deleteProfile(profile.id) }
                    )
                }
            }
        }

        // 编辑/新建对话框
        editingProfile?.let { profile ->
            ProfileEditDialog(
                profile = profile,
                onSave = { viewModel.saveProfile(it) },
                onDismiss = { viewModel.cancelEdit() }
            )
        }
    }
}

@Composable
fun ProfileCard(
    profile: WidgetProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 颜色指示器
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

@Composable
private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor("#$hex"))
    } catch (_: Exception) {
        Color(android.graphics.Color.parseColor("#198754"))
    }
}

@Composable
fun ProfileEditDialog(
    profile: WidgetProfile,
    onSave: (WidgetProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var username by remember { mutableStateOf(profile.username) }
    var color by remember { mutableStateOf(profile.color) }
    var refreshInterval by remember {
        mutableStateOf(profile.refreshIntervalMinutes.toString())
    }
    var quietStart by remember { mutableStateOf(profile.quietHourStart.toString()) }
    var quietEnd by remember { mutableStateOf(profile.quietHourEnd.toString()) }
    var isDarkTheme by remember { mutableStateOf(profile.theme == WidgetProfile.THEME_DARK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (profile.id.isBlank()) "新建配置" else "编辑配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("配置名称") },
                    placeholder = { Text("如 工作号、小号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                        label = { Text("安静开始（0~23 时）") },
                        placeholder = { Text("例: 22") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quietEnd,
                        onValueChange = { quietEnd = it },
                        label = { Text("安静结束（0~23 时）") },
                        placeholder = { Text("例: 7") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "两端相同时=关闭，支持跨天（如22~7表示夜间不刷新）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            TextButton(onClick = {
                val updated = profile.copy(
                    name = name.trim(),
                    username = username.trim(),
                    color = color.trim().ifBlank { WidgetProfile.DEFAULT_COLOR },
                    refreshIntervalMinutes = refreshInterval.toLongOrNull()
                        ?: WidgetProfile.DEFAULT_REFRESH_INTERVAL,
                    quietHourStart = quietStart.toIntOrNull()
                        ?: WidgetProfile.DEFAULT_QUIET_START,
                    quietHourEnd = quietEnd.toIntOrNull()
                        ?: WidgetProfile.DEFAULT_QUIET_END,
                    theme = if (isDarkTheme) WidgetProfile.THEME_DARK
                        else WidgetProfile.THEME_LIGHT
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
