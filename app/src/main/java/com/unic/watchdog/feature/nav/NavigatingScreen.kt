package com.unic.watchdog.feature.nav

import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.unic.watchdog.core.AppContainer
import com.unic.watchdog.core.MAP_PADDING
import com.unic.watchdog.map.TencentMapView
import com.unic.watchdog.model.NavState
import com.unic.watchdog.model.RouteOption
import com.unic.watchdog.model.TravelRecord
import com.unic.watchdog.ui.AppViewModel
import com.unic.watchdog.ui.theme.BrandPrimary
import com.unic.watchdog.ui.theme.Ink
import com.unic.watchdog.ui.theme.MutedInk
import com.unic.watchdog.ui.theme.SubInk
import com.unic.watchdog.util.formatDistance
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NavigatingScreen(route: RouteOption, appVm: AppViewModel, onExit: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by appVm.navSettings.collectAsState()
    var state by remember { mutableStateOf<NavState?>(null) }
    var navigator by remember { mutableStateOf<NavigationTracker?>(null) }
    var companionOn by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        appVm.setNavigating(true)
        onDispose {
            navigator?.stop()
            appVm.setPaused(false)
            appVm.setNavigating(false)
        }
    }

    // 陪伴模式强制横屏；退出时恢复竖屏（configChanges 保证不重建，导航状态不丢失）。
    val activity = context as? android.app.Activity
    val view = LocalView.current
    DisposableEffect(companionOn) {
        val act = activity
        if (companionOn && act != null) {
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            // 深色 HUD 铺满全屏，状态栏/手势条图标改为浅色以保证可见。
            WindowCompat.getInsetsController(act.window, view)?.apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
            onDispose {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                WindowCompat.getInsetsController(act.window, view)?.apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        } else {
            onDispose { }
        }
    }

    fun saveRecord() {
        val df = SimpleDateFormat("MM 月 dd 日", Locale.CHINA)
        scope.launch {
            AppContainer.recordRepo.add(
                TravelRecord(
                    title = "前往 ${appVm.navDestination.value.title}",
                    date = df.format(Date()),
                    distanceText = formatDistance(route.distanceMeters).replace(" km", ""),
                    durationText = "0:${"%02d".format((state?.progress ?: 0) / 4)}",
                    pointNames = listOf("起点", "终点"),
                    mode = "导航"
                )
            )
        }
    }

    fun endNav() {
        navigator?.stop()
        navigator = null
        appVm.setPaused(false)
        onExit()
    }

    // 我的页「暂停导航」开关联动到跟踪器。
    LaunchedEffect(settings.paused) {
        navigator?.setPaused(settings.paused)
    }

    LaunchedEffect(finished) {
        if (finished) {
            saveRecord()
            endNav()
        }
    }

    Box(Modifier.fillMaxSize()) {
        TencentMapView(Modifier.fillMaxSize(), onMapReady = { map ->
            navigator = NavigationTracker(context, map, route) { s ->
                state = s
                if (s.progress >= 100) finished = true
            }
            // 绘制完整路线（腾讯蓝）。
            if (route.points.isNotEmpty()) {
                map.addPolyline(
                    com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions()
                        .addAll(route.points).color(0xFF2D7FF9.toInt()).width(10f).arrow(true)
                )
                val b = com.tencent.tencentmap.mapsdk.maps.model.LatLngBounds.Builder()
                    .include(route.points).build()
                map.animateCamera(
                    com.tencent.tencentmap.mapsdk.maps.CameraUpdateFactory.newLatLngBounds(
                        b, MAP_PADDING
                    )
                )
            }
            navigator?.start()
        })

        // 返回
        IconButton(
            onClick = { endNav() },
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(0.9f))
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Ink)
        }

        // 转向指令卡 + 车道引导
        state?.let { s ->
            InstructionCard(
                s, Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 54.dp)
                    .padding(horizontal = 18.dp)
            )
        }

        // 右侧 HUD：车速表 / 海拔卡
        state?.let { s ->
            Hud(
                s, Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
            )
        }

        // 底部：剩余 / ETA / 进度 / 陪伴 + 结束
        state?.let { s ->
            BottomPanel(
                s = s,
                companionOn = companionOn,
                onCompanion = { companionOn = !companionOn },
                onEnd = { endNav() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // 陪伴模式覆盖层
        if (companionOn && state != null) {
            CompanionOverlay(
                state!!,
                onExitCompanion = { companionOn = false },
                onEnd = { endNav() })
        }
    }
}

@Composable
private fun InstructionCard(s: NavState, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
    ) {
        Column(
            Modifier
                .background(
                    Brush.linearGradient(listOf(Color(0xFF0A5F5A), Color(0xFF0E8379)))
                )
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.17f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TurnRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Text(
                    s.instruction,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 9.dp)
                )
            }
            Text(
                s.lane,
                fontSize = 9.5.sp,
                color = Color.White.copy(0.85f),
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                s.laneGuide.forEach { cell ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(29.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (cell.on) Color(0xFF5FE0A0).copy(0.24f)
                                else Color.White.copy(0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cell.arrow,
                            fontSize = 17.sp,
                            color = if (cell.on) Color(0xFF6FF0B0) else Color(0xFFA9C6C2)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Hud(s: NavState, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Speedo(s.speed, s.speed >= 120)
        Spacer(Modifier.height(7.dp))
        // 海拔卡
        Column(
            Modifier
                .width(80.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0E282B).copy(alpha = 0.86f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text("当前海拔", fontSize = 8.sp, color = Color(0xFFBCDAD6))
            Text(
                "${s.altitude} m",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 1.dp)
            )
            Text(
                s.altTrend,
                fontSize = 7.5.sp,
                color = if (s.altUp) Color(0xFF7EE0B0) else Color(0xFFFFD08A),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun Speedo(speed: Int, over: Boolean) {
    val ringColor = if (over) Color(0xFFFF7A5C) else Color(0xFF4FD1C0)
    Box(
        Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(Color(0xFF0E282B).copy(alpha = 0.86f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            val r = size.minDimension / 2f - stroke
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * (speed.coerceIn(0, 120) / 120f),
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$speed",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            Text("km/h 车速", fontSize = 7.5.sp, color = Color(0xFFBCDAD6))
        }
    }
}

@Composable
private fun BottomPanel(
    s: NavState,
    companionOn: Boolean,
    onCompanion: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.1f".format(s.remainMeters / 1000.0),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
                Text(" 公里剩余", fontSize = 11.sp, color = MutedInk)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    etaText(s),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Text("预计到达", fontSize = 11.sp, color = SubInk)
            }
        }
        Spacer(Modifier.height(13.dp))
        LinearProgressIndicator(
            progress = s.progress / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = BrandPrimary,
            trackColor = Color(0xFFE7EFED)
        )
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = onCompanion,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(if (companionOn) "退出陪伴" else "陪伴模式", fontSize = 10.5.sp)
            }
            Button(
                onClick = onEnd,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE8443A),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("结束", fontSize = 10.5.sp)
            }
        }
    }
}

/** 按真实车速估算剩余时间；速度过低或为 0 时返回「—」。 */
private fun etaText(s: NavState): String {
    if (s.speed < 5) return "—"
    val minutes = (s.remainMeters / (s.speed * 1000.0 / 60.0)).toInt()
    return if (minutes > 0) "$minutes 分钟" else "—"
}

@Composable
private fun CompanionOverlay(s: NavState, onExitCompanion: () -> Unit, onEnd: () -> Unit) {
    // 全部用真实数据：车速/转向来自真实 GPS 与路线步骤；信号灯/电子眼无真实数据源则显示未激活。
    val turnRight = s.lane.contains("右") || s.instruction.contains("右")
    val over = s.speed >= 120

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0C2B2A), Color(0xFF123A39), Color(0xFF0A2423))
                )
            )
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左：路口信号灯
            Box(
                Modifier
                    .weight(0.85f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(0.05f))
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color(0xFF051614))
                        .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(36.dp))
                        .padding(horizontal = 16.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LampDot(Color(0xFF1D3533))
                    LampDot(Color(0xFF1D3533))
                    LampDot(Color(0xFF1D3533))
                }
            }

            // 中：车速 + 电子眼10格
            Column(
                Modifier
                    .weight(2.4f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${s.speed}",
                        fontSize = 150.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (over) Color(0xFFFF5F4D) else Color(0xFF5FE0A8),
                        letterSpacing = (-8).sp,
                        lineHeight = 140.sp
                    )
                    Text(
                        " km/h",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9CD9D2),
                        modifier = Modifier.padding(start = 10.dp, bottom = 18.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(0.06f))
                        .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(18.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(10) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF2A4744))
                            )
                        }
                    }
                }
            }

            // 右：右转灯
            Box(
                Modifier
                    .weight(0.85f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (turnRight) Color(0xFFFFB648).copy(0.12f) else Color.White.copy(0.05f)
                    )
                    .border(
                        1.dp,
                        if (turnRight) Color(0xFFFFB648).copy(0.5f) else Color.White.copy(0.1f),
                        RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TurnRight,
                    contentDescription = null,
                    tint = if (turnRight) Color(0xFFFFB648) else Color(0xFF3F5D5A),
                    modifier = Modifier.size(110.dp)
                )
            }
        }

        // 顶部操作
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onExitCompanion) {
                Icon(Icons.Default.ArrowBack, contentDescription = "退出陪伴", tint = Color.White)
            }
            IconButton(onClick = onEnd) {
                Icon(Icons.Default.Flag, contentDescription = "结束", tint = Color.White)
            }
        }
    }
}

@Composable
private fun LampDot(color: Color) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
    )
}
