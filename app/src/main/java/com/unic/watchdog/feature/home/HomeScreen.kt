package com.unic.watchdog.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.tencentmap.mapsdk.maps.CameraUpdateFactory
import com.tencent.tencentmap.mapsdk.maps.TencentMap
import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions
import com.unic.watchdog.core.AppContainer
import com.unic.watchdog.core.KeyProvider
import com.unic.watchdog.core.URUMQI
import com.unic.watchdog.data.repository.EntityMappers
import com.unic.watchdog.feature.common.PageTitle
import com.unic.watchdog.feature.common.SectionTitle
import com.unic.watchdog.feature.common.parseColor
import com.unic.watchdog.feature.common.placeIcon
import com.unic.watchdog.map.TencentMapView
import com.unic.watchdog.model.FavoritePlace
import com.unic.watchdog.model.Poi
import com.unic.watchdog.ui.AppViewModel
import com.unic.watchdog.ui.TAB_MINE
import com.unic.watchdog.ui.TAB_TRIP
import com.unic.watchdog.ui.theme.BrandPrimary
import com.unic.watchdog.ui.theme.Ink
import com.unic.watchdog.ui.theme.MutedInk
import com.unic.watchdog.ui.theme.SubInk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

private data class Recent(val name: String, val sub: String)

private data class SearchHit(val title: String, val address: String, val latLng: LatLng)

@Composable
fun HomeScreen(appVm: AppViewModel) {
    val placesFlow = remember { AppContainer.placeRepo.observeAll() }
    val entities by placesFlow.collectAsState(initial = emptyList())
    val places = entities.map { EntityMappers.place(it) }
    val currentLocation by appVm.currentLocation.collectAsState(initial = null)

    var editing by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var pickingFor by remember { mutableStateOf<FavoritePlace?>(null) }
    val recents = remember { mutableStateListOf<Recent>() }
    val scope = rememberCoroutineScope()

    fun go(place: FavoritePlace) {
        // 直接点击：进入导航（有定位点就导航到该点，否则回退到当前位置）。
        val latLng = place.latLng ?: currentLocation ?: return
        recents.add(0, Recent(place.name, "刚刚"))
        appVm.navigateTo(Poi(place.name, place.sub, latLng, "常用地点"))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        PageTitle("常用地点", "一键出发，去常去的地方") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (editing) "完成" else "编辑",
                    color = BrandPrimary, fontSize = 11.5.sp,
                    modifier = Modifier
                        .clickable { editing = !editing }
                        .padding(8.dp)
                )
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(BrandPrimary), contentAlignment = Alignment.Center
                ) {
                    Text(
                        "旅",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(places) { place ->
                PlaceGridItem(place, editing, onClick = {
                    // 编辑模式：点卡片设置定位点；正常模式：一键出发导航。
                    if (editing) pickingFor = place else go(place)
                }, onDelete = {
                    scope.launch { AppContainer.placeRepo.remove(place.id) }
                })
            }
            item {
                AddPlaceItem(onClick = { showAdd = true })
            }
        }

        SectionTitle("快捷入口", "常用功能")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickCard(
                Icons.Default.Route,
                "编辑行程",
                "添加路径点",
                Modifier.weight(1f)
            ) { appVm.setTab(TAB_TRIP) }
            QuickCard(
                Icons.Default.History,
                "行程记录",
                "查看历史轨迹",
                Modifier.weight(1f)
            ) { appVm.setTab(TAB_MINE) }
        }

        SectionTitle("最近导航", "今日")
        recents.forEach { r ->
            RecentRow(r, onClick = {
                appVm.navigateTo(Poi(r.name, r.sub, URUMQI, "最近导航"))
            })
        }
    }

    if (showAdd) {
        AddPlaceDialog(onDismiss = { showAdd = false }, onAdd = { name, icon ->
            scope.launch {
                AppContainer.placeRepo.add(
                    FavoritePlace(
                        "pl${System.currentTimeMillis()}",
                        name,
                        "新添加地点",
                        icon,
                        "#0E7C74"
                    )
                )
            }
            showAdd = false
        })
    }

    pickingFor?.let { place ->
        PlaceMapPicker(
            place = place,
            currentLocation = currentLocation,
            onDismiss = { pickingFor = null },
            onConfirm = { latLng ->
                scope.launch { AppContainer.placeRepo.add(place.copy(latLng = latLng)) }
                pickingFor = null
            }
        )
    }
}

@Composable
private fun PlaceGridItem(
    place: FavoritePlace,
    editing: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }) {
        Box {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(parseColor(place.colorHex)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    placeIcon(place.iconName),
                    contentDescription = place.name,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            if (editing) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE05C4A))
                        .clickable { onDelete() }
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(place.name, fontSize = 11.5.sp, color = SubInk, maxLines = 1)
    }
}

@Composable
private fun AddPlaceItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF7FBFA)), contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加",
                    tint = BrandPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("添加地点", fontSize = 11.5.sp, color = BrandPrimary)
    }
}

@Composable
private fun QuickCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                icon,
                contentDescription = title,
                tint = BrandPrimary,
                modifier = Modifier.size(19.dp)
            )
            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(sub, fontSize = 10.sp, color = MutedInk)
        }
    }
}

@Composable
private fun RecentRow(recent: Recent, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp), color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Route,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(Modifier.padding(start = 10.dp)) {
                Text(recent.name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(recent.sub, fontSize = 10.sp, color = MutedInk)
            }
        }
    }
}

@Composable
private fun AddPlaceDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val icons = listOf("home", "briefcase", "plane", "flag", "bed", "star", "bike", "coffee")
    var selected by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("添加常用地点") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    placeholder = { Text("例如：父母家") }, singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Text("地点类型", fontSize = 11.sp, color = MutedInk)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    icons.forEachIndexed { i, ic ->
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (i == selected) BrandPrimary.copy(alpha = 0.15f) else Color(
                                        0xFFF2F6F6
                                    )
                                )
                                .clickable { selected = i }, contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                placeIcon(ic),
                                contentDescription = ic,
                                tint = if (i == selected) BrandPrimary else SubInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onAdd(
                    name.trim(),
                    icons[selected]
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun PlaceMapPicker(
    place: FavoritePlace,
    currentLocation: LatLng?,
    onDismiss: () -> Unit,
    onConfirm: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val client = remember { OkHttpClient() }
    val key = remember { KeyProvider.get(context) }

    var picked by remember { mutableStateOf<LatLng?>(null) }
    var map by remember { mutableStateOf<TencentMap?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    fun select(ll: LatLng) {
        picked = ll
        map?.let { m ->
            m.clear()
            m.addMarker(MarkerOptions().position(ll).title(place.name))
            m.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 15f))
        }
        results = emptyList()
        query = ""
    }

    // 关键词搜索（400ms 防抖，全国范围，直接走腾讯 WebService 检索 API）。
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(400)
        searching = true
        results = runCatching { searchPlaces(client, key, q) }.getOrDefault(emptyList())
        searching = false
    }

    // 定位到达后把地图移到当前位置。
    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))
        }
    }

    Box(Modifier.fillMaxSize()) {
        TencentMapView(
            Modifier.fillMaxSize(),
            onMapReady = { m ->
                map = m
                currentLocation?.let { loc ->
                    m.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))
                }
            },
            onMapClick = { ll -> select(ll) }
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.White, RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索地点") },
                        leadingIcon = {
                            if (searching) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = BrandPrimary
                                )
                            } else {
                                Icon(Icons.Default.Search, null, tint = BrandPrimary)
                            }
                        },
                        singleLine = true
                    )
                }
            }
            if (results.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        results.take(6).forEach { r ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { select(r.latLng) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(Modifier.padding(start = 10.dp)) {
                                    Text(
                                        r.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Ink,
                                        maxLines = 1
                                    )
                                    if (r.address.isNotBlank()) {
                                        Text(
                                            r.address,
                                            fontSize = 11.sp,
                                            color = MutedInk,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (picked == null) {
            Icon(
                Icons.Default.Place,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
                .padding(18.dp)
        ) {
            Text(
                "为「${place.name}」选择定位点",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                picked?.let { "已选位置：${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}" }
                    ?: "搜索或点击地图选择位置",
                fontSize = 12.sp,
                color = MutedInk
            )
            Button(
                onClick = { picked?.let(onConfirm) },
                enabled = picked != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("确定")
            }
        }
    }
}

/** 腾讯 WebService 地点检索，直接 HTTP 调用，规避 SDK 检索模块 SearchFactory 缺失。 */
private suspend fun searchPlaces(
    client: OkHttpClient,
    key: String,
    keyword: String
): List<SearchHit> =
    withContext(Dispatchers.IO) {
        val url = "https://apis.map.qq.com/ws/place/v1/search?keyword=" +
                URLEncoder.encode(keyword, "UTF-8") +
                "&boundary=region(" + URLEncoder.encode("全国", "UTF-8") + ",0)" +
                "&key=" + key +
                "&page_size=10&page_index=1"
        val resp = client.newCall(Request.Builder().url(url).build()).execute()
        val body = resp.body?.string() ?: return@withContext emptyList()
        val json = JSONObject(body)
        if (json.optInt("status") != 0) return@withContext emptyList()
        val arr = json.optJSONArray("data") ?: return@withContext emptyList()
        val out = mutableListOf<SearchHit>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val loc = o.optJSONObject("location") ?: continue
            out.add(
                SearchHit(
                    o.optString("title"),
                    o.optString("address"),
                    LatLng(loc.optDouble("lat"), loc.optDouble("lng"))
                )
            )
        }
        out
    }
