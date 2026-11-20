package com.unic.left.feature.mine

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unic.left.core.AppContainer
import com.unic.left.data.repository.EntityMappers
import com.unic.left.feature.common.SectionTitle
import com.unic.left.model.TravelRecord
import com.unic.left.ui.AppViewModel
import com.unic.left.ui.theme.BrandPrimary
import com.unic.left.ui.theme.Ink
import com.unic.left.ui.theme.MutedInk
import com.unic.left.ui.theme.SubInk
import com.unic.left.ui.theme.SurfaceTint

@Composable
fun MineScreen(appVm: AppViewModel) {
    var selected by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<TravelRecord?>(null) }
    if (detail != null) {
        RecordDetail(record = detail!!, onBack = { detail = null })
        return
    }
    val entities by remember { AppContainer.recordRepo.observeAll() }.collectAsState(initial = emptyList())
    val records = entities.map { EntityMappers.record(it) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BrandPrimary.copy(0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(Modifier.padding(start = 13.dp)) {
                Text("旅行者 小林", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    "在路上，也是一种生活",
                    fontSize = 11.sp,
                    color = MutedInk,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            StatCard("${records.size}", "行程记录", Modifier.weight(1f))
            StatCard("86.4", "累计公里", Modifier.weight(1f))
            StatCard("12", "到过地点", Modifier.weight(1f))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 14.dp)
        ) {
            FilterChip(
                selected = selected == 0,
                onClick = { selected = 0 },
                label = { Text("行程记录") })
            FilterChip(
                selected = selected == 1,
                onClick = { selected = 1 },
                label = { Text("我的收藏") })
        }
        SectionTitle(if (selected == 0) "历史轨迹" else "收藏地点", "共 ${records.size} 条")
        records.forEach { r ->
            RecordCard(r, onClick = { detail = r })
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Card(
        modifier = modifier, colors = CardDefaults.cardColors(containerColor = SurfaceTint),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
            Text(label, fontSize = 10.sp, color = MutedInk)
        }
    }
}

@Composable
private fun RecordCard(record: TravelRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(11.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(record.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(record.date, fontSize = 10.5.sp, color = MutedInk)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(SurfaceTint),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("  ●━━━━━━━━━━●  ", color = BrandPrimary, fontSize = 16.sp)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("${record.distanceText} 公里", fontSize = 10.5.sp, color = SubInk)
                Text("${record.durationText} 用时", fontSize = 10.5.sp, color = SubInk)
                Text(
                    "${record.mode} · ${record.pointNames.size} 个点",
                    fontSize = 10.5.sp,
                    color = SubInk
                )
            }
        }
    }
}

@Composable
private fun RecordDetail(record: TravelRecord, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceTint),
            contentAlignment = Alignment.Center
        ) {
            Text("●━━━━━━━━●━━━━━━●", color = BrandPrimary, fontSize = 22.sp)
        }
        SectionTitle(record.title, record.date)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailStat(record.distanceText, "公里", Modifier.weight(1f))
            DetailStat(record.durationText, "用时", Modifier.weight(1f))
            DetailStat("${record.pointNames.size}", "路径点", Modifier.weight(1f))
        }
        SectionTitle("途经路径点", record.mode)
        record.pointNames.forEachIndexed { i, name ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (i == record.pointNames.lastIndex) BrandPrimary else BrandPrimary.copy(
                                0.5f
                            )
                        )
                )
                Text("  第 ${i + 1} 站 · $name", fontSize = 11.5.sp, color = SubInk)
            }
        }
    }
}

@Composable
private fun DetailStat(value: String, label: String, modifier: Modifier) {
    Card(
        modifier = modifier, colors = CardDefaults.cardColors(containerColor = SurfaceTint),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
            Text(label, fontSize = 10.sp, color = MutedInk)
        }
    }
}
