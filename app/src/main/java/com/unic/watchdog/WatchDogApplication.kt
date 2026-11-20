package com.unic.watchdog

import android.app.Application
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer
import com.unic.watchdog.core.AppContainer
import com.unic.watchdog.data.repository.SeedData

class WatchDogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 腾讯地图 SDK 6.x 要求在创建任何 MapView 之前完成隐私合规声明，否则地图无法渲染。
        TencentMapInitializer.setAgreePrivacy(this, true)
        TencentMapInitializer.start(this)
        // 初始化本地数据库服务定位与默认常用地点。
        AppContainer.init(this)
        SeedData.seedIfEmpty()
    }
}
