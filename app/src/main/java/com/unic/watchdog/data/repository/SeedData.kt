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

    /** 首页网格的默认 4 个常用地点，坐标留空。 */
    private fun defaultPlaces() = listOf(
        FavoritePlace("pl_home", "家", "常用地点", "home", "#0E7C74"),
        FavoritePlace("pl_company", "公司", "常用地点", "briefcase", "#3F63BD"),
        FavoritePlace("pl_hometown", "老家", "常用地点", "flag", "#D98613"),
        FavoritePlace("pl_mall", "商场", "常用地点", "star", "#8E86D8")
    )
}
