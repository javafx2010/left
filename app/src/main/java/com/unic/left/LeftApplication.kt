package com.unic.left

import android.app.Application
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer

class LeftApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 腾讯地图 SDK 6.x 要求在创建任何 MapView 之前完成隐私合规声明，否则地图无法渲染。
        TencentMapInitializer.setAgreePrivacy(this, true)
        TencentMapInitializer.start(this)
    }
}
