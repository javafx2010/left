package com.unic.left.map

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tencent.lbssearch.HttpResponseListener
import com.tencent.lbssearch.TencentSearch
import com.tencent.lbssearch.`object`.param.SearchParam
import com.tencent.lbssearch.`object`.result.SearchResultObject
import com.tencent.tencentmap.mapsdk.maps.CameraUpdateFactory
import com.tencent.tencentmap.mapsdk.maps.TencentMap
import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions

private data class Poi(
    val title: String,
    val address: String,
    val latLng: LatLng
)

private val BEIJING = LatLng(39.9042, 116.4074)

@Composable
fun MapSearchScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Poi>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tencentMap by remember { mutableStateOf<TencentMap?>(null) }

    val search = remember { TencentSearch(context) }

    fun runSearch(keyword: String) {
        if (keyword.isBlank()) return
        searching = true
        error = null
        focusManager.clearFocus()
        val param = SearchParam(keyword, SearchParam.Region("北京").autoExtend(false))
        search.search(param, object : HttpResponseListener<SearchResultObject> {
            override fun onSuccess(statusCode: Int, result: SearchResultObject?) {
                mainHandler.post {
                    val list = result?.data.orEmpty().map { d ->
                        Poi(d.title.orEmpty(), d.address.orEmpty(), d.latLng)
                    }
                    results = list
                    searching = false
                    if (list.isEmpty()) error = "没有找到相关地点"
                }
            }

            override fun onFailure(statusCode: Int, msg: String?, throwable: Throwable?) {
                mainHandler.post {
                    searching = false
                    error = if (statusCode == 199 || msg?.contains("WebserviceAPI", true) == true) {
                        "此 Key 未开启 WebServiceAPI：请在腾讯位置服务控制台的 Key 设置中勾选 WebServiceAPI 后重试"
                    } else {
                        "搜索失败 ($statusCode) ${msg.orEmpty()}"
                    }
                }
            }
        })
    }

    fun locate(poi: Poi) {
        tencentMap?.apply {
            clear()
            addMarker(MarkerOptions().position(poi.latLng).title(poi.title).snippet(poi.address))
            animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, 15f))
        }
        focusManager.clearFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TencentMapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map ->
                tencentMap = map
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(BEIJING, 12f))
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .padding(top = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索地点，如：天安门") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch(query) }),
                    trailingIcon = {
                        if (searching) {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                        } else {
                            IconButton(onClick = { runSearch(query) }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                        }
                    }
                )
            }

            error?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (results.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    items(results) { poi ->
                        ListItem(
                            headlineContent = { Text(poi.title) },
                            supportingContent = { Text(poi.address) },
                            modifier = Modifier.clickable { locate(poi) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
