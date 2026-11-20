package com.unic.left.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unic.left.feature.home.HomeScreen
import com.unic.left.feature.mine.MineScreen
import com.unic.left.feature.nav.NavScreen
import com.unic.left.feature.trip.TripScreen
import com.unic.left.ui.theme.BrandPrimary
import com.unic.left.ui.theme.MutedInk

private data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun AppScaffold(appVm: AppViewModel = viewModel()) {
    val selected by appVm.selectedTab.collectAsStateWithLifecycle()
    val tabs = listOf(
        TabItem("首页", Icons.Default.Home),
        TabItem("行程", Icons.Default.Route),
        TabItem("导航", Icons.Default.Place),
        TabItem("我的", Icons.Default.Person)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
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
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selected) {
                TAB_HOME -> HomeScreen(appVm)
                TAB_TRIP -> TripScreen(appVm)
                TAB_NAV -> NavScreen(appVm)
                TAB_MINE -> MineScreen(appVm)
            }
        }
    }
}
