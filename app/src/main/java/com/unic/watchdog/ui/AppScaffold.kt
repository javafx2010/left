package com.unic.watchdog.ui

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.unic.watchdog.feature.home.HomeScreen
import com.unic.watchdog.feature.nav.NavScreen
import com.unic.watchdog.feature.trip.TripScreen
import com.unic.watchdog.ui.theme.BrandPrimary
import com.unic.watchdog.ui.theme.MutedInk

private data class TabItem(val label: String, val icon: ImageVector)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppScaffold(appVm: AppViewModel = viewModel()) {
    val selected by appVm.selectedTab.collectAsStateWithLifecycle()
    val navigating by appVm.navigating.collectAsStateWithLifecycle()

    // 启动即申请定位权限，授权后取一次当前位置供地图/导航使用。
    val locationPermission =
        rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
            if (granted) appVm.refreshLocation()
        }
    LaunchedEffect(Unit) {
        if (locationPermission.status.isGranted) appVm.refreshLocation()
        else locationPermission.launchPermissionRequest()
    }

    val tabs = listOf(
        TabItem("首页", Icons.Default.Home),
        TabItem("行程", Icons.Default.Route),
        TabItem("导航", Icons.Default.Place)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!navigating) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selected == index,
                            onClick = { appVm.setTab(index) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandPrimary,
                                selectedTextColor = BrandPrimary,
                                unselectedIconColor = MutedInk,
                                unselectedTextColor = MutedInk,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        // 导航中/陪伴模式去掉系统栏 padding，让深色 HUD / 地图铺满全屏（edge-to-edge）。
        Box(
            Modifier
                .fillMaxSize()
                .then(if (navigating) Modifier else Modifier.padding(padding))
        ) {
            when (selected) {
                TAB_HOME -> HomeScreen(appVm)
                TAB_TRIP -> TripScreen(appVm)
                TAB_NAV -> NavScreen(appVm)
            }
        }
    }
}
