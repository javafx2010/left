package com.unic.watchdog.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tencent.tencentmap.mapsdk.maps.MapView
import com.tencent.tencentmap.mapsdk.maps.TencentMap
import com.tencent.tencentmap.mapsdk.maps.TencentMapOptions
import com.unic.watchdog.core.KeyProvider

/**
 * 在 Compose 中承载腾讯地图 MapView，并完整转发生命周期。
 * Key 在运行时从 assets/mapkey.png 解码，经 TencentMapOptions.setMapKey 注入，
 * 明文 Key 不进入源码或构建脚本。
 */
@Composable
fun TencentMapView(
    modifier: Modifier = Modifier,
    onMapReady: (TencentMap) -> Unit,
    onMapClick: ((com.tencent.tencentmap.mapsdk.maps.model.LatLng) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context, TencentMapOptions().setMapKey(KeyProvider.get(context)))
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // tab 切换时 MapView 离开组合，主动销毁一次避免 native 内存泄漏。
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)

    LaunchedEffect(mapView) {
        mapView.getMap()?.let { map ->
            map.setOnMapClickListener { latLng -> onMapClick?.invoke(latLng) }
            onMapReady(map)
        }
    }
}
