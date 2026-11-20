package com.unic.watchdog.feature.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unic.watchdog.core.AppContainer
import com.unic.watchdog.core.MAP_PADDING
import com.unic.watchdog.map.TencentMapView
import com.unic.watchdog.model.RouteOption
import com.unic.watchdog.model.TravelRecord
import com.unic.watchdog.ui.theme.BrandAccent
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
fun NavigatingScreen(route: RouteOption, onExit: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tencentMap by remember { mutableStateOf<com.tencent.tencentmap.mapsdk.maps.TencentMap?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var remain by remember { mutableStateOf(route.distanceMeters.toDouble()) }
    var instruction by remember { mutableStateOf("前方路况良好，沿当前道路直行") }
    var lane by remember { mutableStateOf("保持当前车道 · 注意交通安全") }
    var paused by remember { mutableStateOf(false) }
    var voice by remember { mutableStateOf(true) }
    var eyeOn by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }

    var navigator by remember { mutableStateOf<SimulatedNavigator?>(null) }

    DisposableEffect(Unit) {
        onDispose { navigator?.stop() }
    }

    fun startNav() {
        navigator?.start(scope)
    }

    fun saveRecord() {
        val df = SimpleDateFormat("MM 月 dd 日", Locale.CHINA)
        scope.launch {
            AppContainer.recordRepo.add(
                TravelRecord(
                    title = "模拟导航 ${progress}%", date = df.format(Date()),
                    distanceText = formatDistance(route.distanceMeters).replace(" km", ""),
                    durationText = "0:${"%02d".format(progress / 4)}",
                    pointNames = listOf("起点", "终点"), mode = "导航"
                )
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        TencentMapView(Modifier.fillMaxSize(), onMapReady = {
            tencentMap = it
            navigator = SimulatedNavigator(it, route) { p, r, ins, ln ->
                progress = p; remain = r; instruction = ins; lane = ln
                if (p >= 100) finished = true
            }
            // 绘制完整路线
            if (route.points.isNotEmpty()) {
                it.addPolyline(
                    com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions()
                        .addAll(route.points).color(BrandPrimary.hashCode()).width(10f).arrow(true)
                )
                val b = com.tencent.tencentmap.mapsdk.maps.model.LatLngBounds.Builder()
                    .include(route.points).build()
                it.animateCamera(
                    com.tencent.tencentmap.mapsdk.maps.CameraUpdateFactory.newLatLngBounds(
                        b,
                        MAP_PADDING
                    )
                )
            }
        })

        IconButton(
            onClick = { navigator?.stop(); onExit() },
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Ink)
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BrandPrimary),
            modifier = Modifier
                .padding(top = 60.dp)
                .padding(horizontal = 14.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TurnRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Column(Modifier.padding(start = 10.dp)) {
                    Text(
                        instruction,
                        fontSize = 15.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        lane,
                        fontSize = 11.sp,
                        color = Color.White.copy(0.85f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        if (eyeOn) {
            Card(
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8ED)),
                modifier = Modifier
                    .padding(top = 168.dp)
                    .padding(horizontal = 14.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = BrandAccent)
                    Column(Modifier.padding(start = 9.dp)) {
                        Text(
                            "电子眼提醒",
                            fontSize = 12.5.sp,
                            color = Color(0xFF9A6705),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "注意限速 · 保持安全车距",
                            fontSize = 10.5.sp,
                            color = Color(0xFFA98244)
                        )
                    }
                    IconButton(onClick = { eyeOn = false }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭提醒",
                            tint = Color(0xFFB27A10),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp), color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "${(remain / 1000).coerceAtLeast(0.0)}".take(4), fontSize = 26.sp,
                            fontWeight = FontWeight.Bold, color = BrandPrimary
                        )
                        Text("公里剩余", fontSize = 11.sp, color = MutedInk)
                    }
                    Text(
                        "预计 ${maxOf(1, (100 - progress) / 4)} 分钟到达",
                        fontSize = 11.5.sp,
                        color = SubInk
                    )
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress / 100f, modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = BrandPrimary, trackColor = Color(0xFFE7EFED)
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                        6.dp
                    )
                ) {
                    NavBtn(
                        if (voice) "播报开" else "播报关",
                        if (voice) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        Modifier.weight(1f)
                    ) {
                        voice = !voice
                    }
                    NavBtn("设置", Icons.Default.CameraAlt, Modifier.weight(1f)) { }
                    NavBtn(
                        if (paused) "继续" else "暂停",
                        if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        Modifier.weight(1f)
                    ) {
                        paused = !paused
                        navigator?.setPaused(paused)
                        if (!paused && navigator == null) startNav()
                    }
                    NavBtn("结束", Icons.Default.Stop, Modifier.weight(1f), danger = true) {
                        navigator?.stop(); onExit()
                    }
                }
                Button(
                    onClick = {
                        if (navigator == null) startNav() else {
                            navigator?.setPaused(false); paused = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text(if (navigator == null) "开始模拟导航" else "继续模拟导航")
                }
            }
        }
    }

    LaunchedEffect(finished) {
        if (finished) {
            saveRecord(); navigator?.stop()
        }
    }
}

@Composable
private fun NavBtn(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier,
    danger: Boolean = false, onClick: () -> Unit
) {
    Button(
        onClick = onClick, modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (danger) Color(0xFFFFF1EE) else Color(0xFFF5F8F8),
            contentColor = if (danger) Color(0xFFD65B3B) else SubInk
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Text(label, fontSize = 10.5.sp)
    }
}
