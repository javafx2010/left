package com.unic.watchdog.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unic.watchdog.feature.common.PageTitle
import com.unic.watchdog.ui.AppViewModel
import com.unic.watchdog.ui.NavSettings
import com.unic.watchdog.ui.theme.BrandPrimary
import com.unic.watchdog.ui.theme.Ink
import com.unic.watchdog.ui.theme.MutedInk
import com.unic.watchdog.ui.theme.SubInk
import com.unic.watchdog.ui.theme.SurfaceTint

/** 设置页：导航设置（播报 / 播报设置 / 暂停）+ 通用（关于 / 清缓存）。 */
@Composable
fun SettingsScreen(appVm: AppViewModel) {
    val settings by appVm.navSettings.collectAsStateWithLifecycle()
    var voiceDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        PageTitle("设置", "偏好与导航相关配置")

        Spacer(Modifier.height(10.dp))
        SectionLabel("导航设置")
        SettingsCard {
            SettingRow(
                icon = Icons.Default.VolumeUp,
                title = "导航播报",
                sub = "开启语音引导提示"
            ) {
                Switch(checked = settings.voiceOn, onCheckedChange = appVm::setVoiceOn)
            }
            SettingRow(
                icon = Icons.Default.Settings,
                title = "播报设置",
                sub = "播报模式与音量",
                onClick = { voiceDialog = true },
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MutedInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            SettingRow(
                icon = Icons.Default.Pause,
                title = "暂停导航",
                sub = "暂停 / 继续导航进度"
            ) {
                Switch(checked = settings.paused, onCheckedChange = appVm::setPaused)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("通用")
        SettingsCard {
            SettingRow(
                icon = Icons.Default.Info,
                title = "关于应用",
                sub = "版本 v1.1 · 行迹 TravelMate",
                onClick = { },
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MutedInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            SettingRow(
                icon = Icons.Default.Delete,
                title = "清除缓存",
                sub = "释放本地存储空间",
                onClick = { },
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MutedInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }

    if (voiceDialog) {
        VoiceSettingsDialog(
            settings = settings,
            onDismiss = { voiceDialog = false },
            onMode = appVm::setVoiceMode,
            onVolume = appVm::setVolume
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Ink,
        modifier = Modifier.padding(start = 2.dp, bottom = 9.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(androidx.compose.ui.graphics.Color.White)
    ) {
        content()
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    sub: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(17.dp)
            )
        }
        Column(
            Modifier
                .padding(start = 11.dp)
                .weight(1f)
        ) {
            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(sub, fontSize = 10.sp, color = MutedInk, modifier = Modifier.padding(top = 3.dp))
        }
        trailing?.invoke()
    }
}

@Composable
private fun VoiceSettingsDialog(
    settings: NavSettings,
    onDismiss: () -> Unit,
    onMode: (String) -> Unit,
    onVolume: (Int) -> Unit
) {
    val modes = listOf("简洁", "详细", "静音")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导航播报设置") },
        text = {
            Column {
                modes.forEach { m ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onMode(m) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = settings.voiceMode == m, onClick = { onMode(m) })
                        Text(m, fontSize = 13.sp, color = Ink)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("播报音量", fontSize = 11.5.sp, color = MutedInk)
                    Spacer(Modifier.width(8.dp))
                    Text("${settings.volume}%", fontSize = 11.5.sp, color = SubInk)
                }
                Slider(
                    value = settings.volume.toFloat(),
                    onValueChange = { onVolume(it.toInt()) },
                    valueRange = 0f..100f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}
