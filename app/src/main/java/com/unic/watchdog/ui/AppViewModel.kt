package com.unic.watchdog.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import com.unic.watchdog.core.URUMQI
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
        MutableStateFlow(Poi("乌鲁木齐", "新疆 · 乌鲁木齐", URUMQI))
    val navDestination: StateFlow<Poi> = _navDestination.asStateFlow()

    /** 设备当前位置，定位成功前为 null（调用方回退到默认中心）。 */
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _focusEvent = MutableStateFlow(FocusEvent(null))
    val focusEvent: StateFlow<FocusEvent> = _focusEvent.asStateFlow()

    private val _navTick = MutableStateFlow(0)
    val navTick: StateFlow<Int> = _navTick.asStateFlow()

    private var token = 0L

    /** 持续接收定位更新，直到拿到首个有效位置为止。 */
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _currentLocation.value = LatLng(location.latitude, location.longitude)
            stopLocationUpdates()
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
    }

    private fun locationManager(): LocationManager =
        getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /** 启动时自动获取当前位置；无权限或定位失败时保持 null。 */
    @SuppressLint("MissingPermission")
    fun refreshLocation() {
        val app = getApplication<Application>()
        val fine = ContextCompat.checkSelfPermission(
            app, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            app, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return

        val lm = locationManager()

        // 先用最近一次已知位置（任意 provider）快速回填，避免地图空白等待。
        val last = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).firstNotNullOfOrNull { p ->
            try {
                lm.getLastKnownLocation(p)
            } catch (_: Exception) {
                null
            }
        }
        if (last != null) _currentLocation.value = LatLng(last.latitude, last.longitude)

        // 再主动请求定位更新（网络 + GPS）。用系统 LocationManager 而非 Google Fused，
        // 兼容国内无 Google 定位服务的设备。
        val providers = mutableListOf(LocationManager.NETWORK_PROVIDER)
        if (fine) providers.add(LocationManager.GPS_PROVIDER)
        providers.forEach { p ->
            try {
                lm.requestLocationUpdates(p, 5000L, 0f, locationListener, Looper.getMainLooper())
            } catch (_: Exception) {
                // 该 provider 不可用时忽略，继续尝试其它。
            }
        }
    }

    private fun stopLocationUpdates() {
        try {
            locationManager().removeUpdates(locationListener)
        } catch (_: Exception) {
        }
    }

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
