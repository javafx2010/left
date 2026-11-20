package com.unic.watchdog.data.repository

import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import com.unic.watchdog.model.GuideSpot

/** 大理热门地点种子数据（原型场景：洱海、古城、苍山）。图片来自原型 assets。 */
object GuideRepository {

    val categories = listOf("全部", "景点", "美食", "交通", "住宿", "自然")

    val spots: List<GuideSpot> = listOf(
        GuideSpot(
            "dali-old", "大理古城", "景点", "千年古城，人民路与洋人街",
            "位于大理市，背靠苍山、面朝洱海，是南诏古国的都城遗址。人民路、复兴路充满文艺气息，适合闲逛。",
            "images/ancient-town.png", LatLng(25.6065, 100.2676), listOf("古城", "必去"), 180
        ),
        GuideSpot(
            "erhai", "洱海生态廊道", "自然", "环海骑行与湖光山色",
            "洱海是大理「风花雪月」四景之一。生态廊道沿湖而建，适合骑行、散步，看日出日落。",
            "images/hero-dali.png", LatLng(25.7000, 100.1800), listOf("骑行", "湖景"), 240
        ),
        GuideSpot(
            "cangshan", "苍山洗马潭", "自然", "登高望远，俯瞰洱海",
            "苍山是大理的屏障，可乘感通索道或洗马潭索道登顶，山间云雾缭绕，景色壮观。",
            "images/mountain-hike.png", LatLng(25.6500, 100.1000), listOf("徒步", "索道"), 300
        ),
        GuideSpot(
            "santa", "崇圣寺三塔", "景点", "大理地标，南诏佛国",
            "崇圣寺三塔是大理最著名的地标，始建于唐代，三塔耸立，倒影入池，是摄影胜地。",
            "images/ancient-town.png", LatLng(25.6113, 100.2619), listOf("古迹", "地标"), 120
        ),
        GuideSpot(
            "xizhou", "喜洲古镇", "景点", "白族民居与稻田风光",
            "喜洲是白族聚居地，保存有大量白族民居建筑，稻田与蓝天相映，还有著名的喜洲粑粑。",
            "images/hero-dali.png", LatLng(25.7570, 100.1300), listOf("白族", "稻田"), 150
        ),
        GuideSpot(
            "food-street", "人民路夜市", "美食", "古城美食与人文学小馆",
            "大理古城人民路聚集了众多特色餐馆、咖啡馆与小店，夜晚热闹，是美食扫街好去处。",
            "images/food-market.png", LatLng(25.6060, 100.2680), listOf("夜市", "美食"), 120
        ),
        GuideSpot(
            "jungle-hotpot", "菌子火锅", "美食", "云南野生菌特色",
            "到大理必尝的野生菌火锅，雨季尤为鲜美，古城内有多家人气店铺。",
            "images/food-market.png", LatLng(25.6055, 100.2670), listOf("特色", "火锅"), 90
        ),
        GuideSpot(
            "airport", "大理凤仪机场", "交通", "进出大理的空中门户",
            "大理凤仪国际机场位于洱海东南岸，距古城约 30 公里，是前往大理的主要航空口岸。",
            "images/hero-dali.png", LatLng(25.5510, 100.3380), listOf("机场", "交通"), 60
        ),
        GuideSpot(
            "guesthouse", "既下山民宿", "住宿", "古城特色民宿",
            "位于大理古城的特色民宿，闹中取静，是体验大理慢生活的理想住所。",
            "images/ancient-town.png", LatLng(25.6050, 100.2660), listOf("民宿", "住宿"), 60
        ),
        GuideSpot(
            "cai-cun", "才村码头", "景点", "洱海边的小渔村",
            "才村是洱海边典型的白族渔村，码头可乘船游湖，清晨看洱海日出非常出名。",
            "images/hero-dali.png", LatLng(25.6900, 100.2000), listOf("码头", "日出"), 90
        )
    )

    fun byCategory(category: String): List<GuideSpot> =
        if (category == "全部") spots else spots.filter { it.category == category }

    /** 地图选点 / 搜索建议用的地点名列表。 */
    val pickerNames = spots.map { it.name } + listOf("人民路", "环海西路", "龙龛码头", "海舌公园")
}
