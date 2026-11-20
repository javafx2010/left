package com.unic.watchdog.feature.nav

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tencent.lbssearch.HttpResponseListener
import com.tencent.lbssearch.TencentSearch
import com.tencent.lbssearch.`object`.param.DrivingParam
import com.tencent.lbssearch.`object`.result.DrivingResultObject
import com.tencent.tencentmap.mapsdk.maps.CameraUpdateFactory
import com.tencent.tencentmap.mapsdk.maps.TencentMap
import com.tencent.tencentmap.mapsdk.maps.model.LatLngBounds
import com.unic.watchdog.core.BEIJING
import com.unic.watchdog.core.MAP_PADDING
import com.unic.watchdog.map.TencentMapView
import com.unic.watchdog.model.RouteOption
import com.unic.watchdog.model.TravelMode
import com.unic.watchdog.ui.AppViewModel
import com.unic.watchdog.ui.theme.BrandPrimary
import com.unic.watchdog.ui.theme.Ink
import com.unic.watchdog.ui.theme.MutedInk
import com.unic.watchdog.ui.theme.SubInk

private sealed interface NavRoute {
    data object Plan : NavRoute
    data class Navigating(val route: RouteOption) : NavRoute
}

@Composable
fun NavScreen(appVm: AppViewModel) {
    var route by remember { mutableStateOf<NavRoute>(NavRoute.Plan) }
    when (val r = route) {
        NavRoute.Plan -> RoutePlanScreen(appVm, onStartNav = { route = NavRoute.Navigating(it) })
        is NavRoute.Navigating -> NavigatingScreen(r.route, onExit = { route = NavRoute.Plan })
    }
}

@Composable
private fun RoutePlanScreen(appVm: AppViewModel, onStartNav: (RouteOption) -> Unit) {
    val destination by appVm.navDestination.collectAsStateWithLifecycle()
    val currentLocation by appVm.currentLocation.collectAsStateWithLifecycle()
    val focus by appVm.focusEvent.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    // 检索 API 不走 MapView 的 setMapKey，需显式传入从图片解码的 Key。
    val search =
        remember { TencentSearch(context, com.unic.watchdog.core.KeyProvider.get(context)) }
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(TravelMode.DRIVE) }
    var routes by remember { mutableStateOf<List<RouteOption>>(emptyList()) }
    var selected by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tencentMap by remember { mutableStateOf<TencentMap?>(null) }

    fun drawRoute(option: RouteOption) {
        val map = tencentMap ?: return
        map.clear()
        if (option.points.isNotEmpty()) {
            map.addPolyline(
                com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions()
                    .addAll(option.points).color(BrandPrimary.hashCode()).width(9f).arrow(true)
            )
            val from = option.points.first()
            val to = option.points.last()
            map.addMarker(
                com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions().position(from)
                    .title("起点")
            )
            map.addMarker(
                com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions().position(to)
                    .title(destination.title)
            )
            val b = LatLngBounds.Builder().include(option.points).build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(b, MAP_PADDING))
        }
    }

    fun plan() {
        val from = currentLocation ?: BEIJING
        val to = destination.latLng
        loading = true
        error = null
        val listener =
            object : HttpResponseListener<com.tencent.lbssearch.httpresponse.BaseObject> {
                override fun onSuccess(
                    statusCode: Int,
                    result: com.tencent.lbssearch.httpresponse.BaseObject?
                ) {
                    mainHandler.post {
                        loading = false
                        val parsed = when (result) {
                            is DrivingResultObject -> RouteParser.parseDriving(result)
                            else -> emptyList()
                        }
                        val finalRoutes =
                            parsed.ifEmpty { listOf(RouteParser.demoRoute(from, to, mode)) }
                        routes = finalRoutes
                        selected = 0
                        drawRoute(finalRoutes[0])
                        error = if (parsed.isEmpty()) "未找到规划路线，已显示演示路线" else null
                    }
                }

                override fun onFailure(statusCode: Int, msg: String?, throwable: Throwable?) {
                    mainHandler.post {
                        loading = false
                        // Key 未开启 WebServiceAPI 时生成演示路线，保证模拟导航等功能可体验。
                        val fallback = RouteParser.demoRoute(from, to, mode)
                        routes = listOf(fallback)
                        selected = 0
                        drawRoute(fallback)
                        error = "演示路线（真实规划需在腾讯控制台为 Key 开启 WebServiceAPI）"
                    }
                }
            }
        @Suppress("UNCHECKED_CAST")
        search.getRoutePlan(
            DrivingParam(from, to),
            listener as HttpResponseListener<DrivingResultObject>
        )
    }

    LaunchedEffect(mode, destination.title, currentLocation) { plan() }
    LaunchedEffect(focus.token) {
        if (focus.poi != null) {
            tencentMap?.let {
                it.clear(); it.addMarker(
                com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions()
                    .position(destination.latLng).title(destination.title)
            )
                it.animateCamera(CameraUpdateFactory.newLatLngZoom(destination.latLng, 14f))
            }
        }
    }

    // 定位结果到达后把地图移到当前位置（onMapReady 已处理先于地图就绪的情况）。
    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            tencentMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 12f))
        }
    }

    Box(Modifier.fillMaxSize()) {
        TencentMapView(Modifier.fillMaxSize(), onMapReady = {
            tencentMap = it
            it.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation ?: BEIJING, 12f))
        })

        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
                .padding(top = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                OutlinedTextField(
                    value = destination.title,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = BrandPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("目的地") })
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TravelMode.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { mode = m },
                        label = { Text(m.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            error?.let {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
        }

        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = BrandPrimary)
        }

        if (routes.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp), color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(BrandPrimary.copy(0.12f))
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(BrandPrimary)
                        )
                    }
                    val opt = routes[selected]
                    Text(
                        destination.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            opt.durationText,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            maxLines = 1
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(opt.distanceText, fontSize = 12.sp, color = SubInk, maxLines = 1)
                        Spacer(Modifier.size(6.dp))
                        Text("· ${mode.label}", fontSize = 11.sp, color = MutedInk, maxLines = 1)
                    }
                    Column(Modifier.padding(top = 12.dp)) {
                        opt.steps.take(4).forEach { s ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(BrandPrimary)
                                )
                                Spacer(Modifier.size(9.dp))
                                Text(s.instruction, fontSize = 11.5.sp, color = SubInk)
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    if (routes.size > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            routes.forEachIndexed { i, o ->
                                FilterChip(
                                    selected = i == selected,
                                    onClick = { selected = i; drawRoute(o) },
                                    label = { Text("${o.durationText} · ${o.distanceText}") })
                            }
                        }
                    }
                    Button(
                        onClick = { onStartNav(opt) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            null
                        ); Spacer(Modifier.size(6.dp)); Text("开始导航")
                    }
                }
            }
        }
    }
}
