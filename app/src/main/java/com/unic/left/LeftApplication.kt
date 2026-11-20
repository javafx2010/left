package com.unic.left

import android.app.Application
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer
import com.unic.left.core.AppContainer
import com.unic.left.data.repository.SeedData

class LeftApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 腾讯地图 SDK 6.x 要求在创建任何 MapView 之前完成隐私合规声明，否则地图无法渲染。
        TencentMapInitializer.setAgreePrivacy(this, true)
        TencentMapInitializer.start(this)
        // 初始化本地数据库服务定位与示例数据。
        AppContainer.init(this)
        SeedData.seedIfEmpty()
    }
}
