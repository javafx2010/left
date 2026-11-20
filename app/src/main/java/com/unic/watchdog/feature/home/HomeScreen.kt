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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unic.watchdog.core.AppContainer
import com.unic.watchdog.core.URUMQI
import com.unic.watchdog.data.repository.EntityMappers
import com.unic.watchdog.feature.common.PageTitle
import com.unic.watchdog.feature.common.SectionTitle
import com.unic.watchdog.feature.common.parseColor
import com.unic.watchdog.feature.common.placeIcon
import com.unic.watchdog.model.FavoritePlace
import com.unic.watchdog.model.Poi
import com.unic.watchdog.ui.AppViewModel
import com.unic.watchdog.ui.TAB_MINE
import com.unic.watchdog.ui.TAB_TRIP
import com.unic.watchdog.ui.theme.BrandPrimary
import com.unic.watchdog.ui.theme.Ink
import com.unic.watchdog.ui.theme.MutedInk
import com.unic.watchdog.ui.theme.SubInk
import kotlinx.coroutines.launch

private data class Recent(val name: String, val sub: String)

@Composable
fun HomeScreen(appVm: AppViewModel) {
    val placesFlow = remember { AppContainer.placeRepo.observeAll() }
    val entities by placesFlow.collectAsState(initial = emptyList())
    val places = entities.map { EntityMappers.place(it) }

    var editing by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val recents = remember { mutableStateListOf<Recent>() }
    val scope = rememberCoroutineScope()

    fun go(place: FavoritePlace) {
        val poi = place.latLng?.let { Poi(place.name, place.sub, it, "常用地点") }
            ?: Poi(place.name, place.sub, URUMQI, "常用地点")
        recents.add(0, Recent(place.name, "刚刚"))
        appVm.navigateTo(poi)
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
                PlaceGridItem(place, editing, onClick = { go(place) }, onDelete = {
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
