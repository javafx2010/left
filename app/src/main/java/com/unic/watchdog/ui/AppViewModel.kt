package com.unic.watchdog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import com.unic.watchdog.model.Poi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 4 tab：首页 / 行程 / 导航 / 我的。 */
const val TAB_HOME = 0
const val TAB_TRIP = 1
const val TAB_NAV = 2
const val TAB_MINE = 3

data class FocusEvent(val poi: Poi?, val token: Long = 0L)

/** 跨 tab 共享状态。 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val _selectedTab = MutableStateFlow(TAB_HOME)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _navDestination =
        MutableStateFlow(Poi("洱海生态廊道", "大理 · 洱海", LatLng(25.7000, 100.1800)))
    val navDestination: StateFlow<Poi> = _navDestination.asStateFlow()

    private val _focusEvent = MutableStateFlow(FocusEvent(null))
    val focusEvent: StateFlow<FocusEvent> = _focusEvent.asStateFlow()

    private val _navTick = MutableStateFlow(0)
    val navTick: StateFlow<Int> = _navTick.asStateFlow()

    private var token = 0L

    fun setTab(tab: Int) {
        _selectedTab.value = tab
    }

    /** 从任意页发起导航：设置目的地并切到导航 tab。 */
    fun navigateTo(poi: Poi) {
        _navDestination.value = poi
        _navTick.value++
        _selectedTab.value = TAB_NAV
    }

    /** 请求某 tab 在地图上定位某点（用 token 保证一次性）。 */
    fun focusOnMap(poi: Poi) {
        token++
        _focusEvent.value = FocusEvent(poi, token)
    }
}
