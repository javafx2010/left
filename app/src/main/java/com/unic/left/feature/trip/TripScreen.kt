package com.unic.left.feature.trip

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unic.left.core.AppContainer
import com.unic.left.data.repository.GuideRepository
import com.unic.left.feature.common.PageTitle
import com.unic.left.feature.common.SectionTitle
import com.unic.left.map.TencentMapView
import com.unic.left.model.TripPoint
import com.unic.left.ui.AppViewModel
import com.unic.left.ui.theme.BrandAccent
import com.unic.left.ui.theme.BrandPrimary
import com.unic.left.ui.theme.Ink
import com.unic.left.ui.theme.MutedInk
import com.unic.left.ui.theme.SurfaceTint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private sealed interface TripRoute {
    data object List : TripRoute
    data class Edit(val id: Long) : TripRoute
    data class Picker(val id: Long) : TripRoute
}

@Composable
fun TripScreen(appVm: AppViewModel) {
    var route by remember { mutableStateOf<TripRoute>(TripRoute.List) }
    val scope = rememberCoroutineScope()
    when (val r = route) {
        TripRoute.List -> TripListScreen(onOpen = { route = TripRoute.Edit(it) })
        is TripRoute.Edit -> TripEditScreen(
            r.id,
            onBack = { route = TripRoute.List },
            onMapPick = { route = TripRoute.Picker(r.id) })

        is TripRoute.Picker -> MapPickerScreen(
            onBack = { route = TripRoute.Edit(r.id) },
            onConfirm = { name, lat, lng ->
                scope.launch {
                    val count = AppContainer.tripRepo.observePoints(r.id).first().size
                    AppContainer.tripRepo.addPoint(
                        r.id, TripPoint(
                            name = name, time = "待定", type = "途经点",
                            latLng = com.tencent.tencentmap.mapsdk.maps.model.LatLng(lat, lng),
                            orderIndex = count
                        )
                    )
                }
                route = TripRoute.Edit(r.id)
            }
        )
    }
}

@Composable
private fun TripListScreen(onOpen: (Long) -> Unit) {
    val entities by remember { AppContainer.tripRepo.observeAll() }.collectAsState(initial = emptyList())
    val pointsMaps = remember { mutableMapOf<Long, List<com.unic.left.data.db.TripPointEntity>>() }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        PageTitle("我的行程", "编辑路线，按顺序添加路径点") {
            IconButton(onClick = {
                scope.launch {
                    val id = AppContainer.tripRepo.create("新行程", "images/hero-dali.png")
                    onOpen(id)
                }
            }) { Icon(Icons.Default.Add, contentDescription = "新建行程", tint = BrandPrimary) }
        }
        Spacer(Modifier.height(8.dp))
        entities.forEach { e ->
            val pts by remember(e.id) { AppContainer.tripRepo.observePoints(e.id) }
                .collectAsState(initial = emptyList())
            TripCard(e.title, e.coverAsset, pts.size) { onOpen(e.id) }
        }
        SectionTitle("行程小贴士", "提示")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoCard("一键导航", "按路径点顺序", Icons.Default.Place, Modifier.weight(1f))
            InfoCard("地图选点", "添加新路径点", Icons.Default.Place, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TripCard(title: String, cover: String, pointCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(17.dp), elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = "file:///android_asset/$cover", contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(15.dp)
            ) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "共 $pointCount 站 · 点击编辑路线",
                    fontSize = 10.5.sp,
                    color = Color.White.copy(0.9f)
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.9f))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    "$pointCount 个路径点",
                    fontSize = 10.sp,
                    color = BrandPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    Card(
        modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(19.dp)
            )
            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(sub, fontSize = 10.sp, color = MutedInk)
        }
    }
}

@Composable
private fun TripEditScreen(tripId: Long, onBack: () -> Unit, onMapPick: () -> Unit) {
    val tripEntity by remember { AppContainer.tripRepo.observeAll() }
        .collectAsState(initial = emptyList())
    val trip = tripEntity.firstOrNull { it.id == tripId }
    var title by remember(trip?.title) { mutableStateOf(trip?.title ?: "新行程") }
    val points by remember(tripId) { AppContainer.tripRepo.observePoints(tripId) }
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            AsyncImage(
                model = "file:///android_asset/${trip?.coverAsset ?: "images/hero-dali.png"}",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(12.dp)
                    .background(Color.White.copy(0.85f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    "编辑行程",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "调整顺序 · 地图选点添加路径",
                    fontSize = 10.5.sp,
                    color = Color.White.copy(0.9f)
                )
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                label = { Text("行程标题") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )
            SectionTitle("路径点", "${points.size} 个点")
            points.forEachIndexed { index, p ->
                PointRow(
                    index, p.name, p.type, p.time,
                    onUp = { scope.launch { /* reorder not persisted in v1 */ } },
                    onDown = {},
                    onDelete = { scope.launch { AppContainer.tripRepo.deletePoint(p.id) } })
            }
            Button(
                onClick = onMapPick, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceTint,
                    contentColor = BrandPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Place, contentDescription = null)
                Text(" 添加路径点 · 地图选点")
            }
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceTint,
                    contentColor = BrandPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" 添加路径点 · 从推荐选择")
            }
            Button(
                onClick = {
                    scope.launch {
                        AppContainer.tripRepo.rename(
                            tripId,
                            title
                        ); onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("保存行程")
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false }, title = { Text("选择推荐地点") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    GuideRepository.spots.forEach { spot ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        AppContainer.tripRepo.addPoint(
                                            tripId, TripPoint(
                                                name = spot.name,
                                                time = "待定",
                                                type = spot.category,
                                                latLng = spot.latLng,
                                                orderIndex = points.size
                                            )
                                        )
                                    }
                                    showAddDialog = false
                                }
                                .padding(12.dp)) {
                            Text(spot.name, fontSize = 13.sp, color = Ink)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddDialog = false }) { Text("完成") } }
        )
    }
}

@Composable
private fun PointRow(
    index: Int, name: String, type: String, time: String,
    onUp: () -> Unit, onDown: () -> Unit, onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(BrandPrimary.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(" $type", fontSize = 9.sp, color = BrandAccent)
                }
                Text("$time · 途经点 ${index + 1}", fontSize = 10.sp, color = MutedInk)
            }
            IconButton(
                onClick = onUp,
                modifier = Modifier.size(26.dp)
            ) { Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(16.dp)) }
            IconButton(
                onClick = onDown,
                modifier = Modifier.size(26.dp)
            ) { Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(16.dp)) }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = Color(0xFFD65B3B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MapPickerScreen(onBack: () -> Unit, onConfirm: (String, Double, Double) -> Unit) {
    var picked by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            TencentMapView(Modifier.fillMaxSize(), onMapReady = {}, onMapClick = { ll ->
                picked = Triple("地图选点", ll.latitude, ll.longitude)
            })
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(12.dp)
                    .background(Color.White, RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(18.dp)
        ) {
            Text("已选位置", fontSize = 12.sp, color = MutedInk)
            Text(
                picked?.first ?: "尚未选择位置",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Button(
                onClick = { picked?.let { onConfirm(it.first, it.second, it.third) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("确定为路径点")
            }
        }
    }
}
