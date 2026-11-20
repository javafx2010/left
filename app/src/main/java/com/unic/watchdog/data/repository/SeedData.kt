package com.unic.watchdog.data.repository

import com.unic.watchdog.core.AppContainer
import com.unic.watchdog.model.FavoritePlace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 首次启动填充首页默认常用地点（仅当库为空时）。 */
object SeedData {
    fun seedIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            val placeRepo = AppContainer.placeRepo
            if (placeRepo.observeAll().first().isEmpty()) {
                defaultPlaces().forEach { placeRepo.add(it) }
            }
        }
    }

    /** 首页网格的默认 8 个常用地点（与原型一致），坐标留空（点击进入选点）。 */
    private fun defaultPlaces() = listOf(
        FavoritePlace("pl_home", "家", "人民路 128 号", "home", "#0E7C74"),
        FavoritePlace("pl_company", "公司", "创新园 A 座", "briefcase", "#3F63BD"),
        FavoritePlace("pl_airport", "机场", "凤仪机场", "plane", "#D98613"),
        FavoritePlace("pl_oldtown", "大理古城", "常用目的地", "flag", "#E0664A"),
        FavoritePlace("pl_inn", "客栈", "既下山民宿", "bed", "#8E86D8"),
        FavoritePlace("pl_gym", "健身房", "古城南门", "star", "#2B8FAE"),
        FavoritePlace("pl_cafe", "咖啡馆", "人民路小店", "coffee", "#B06B3D"),
        FavoritePlace("pl_erhai", "洱海", "生态廊道", "bike", "#0E7C74")
    )
}
