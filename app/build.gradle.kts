import javax.imageio.ImageIO

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

android {
    namespace = "com.unic.watchdog"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.unic.watchdog"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 腾讯 SDK 6.x 通过 Manifest 的 TencentMapSDK meta-data 取全局 Key（token 中转通道需要）。
        // 构建期从 assets/mapkey.png 的 LSB 解码，保持源码/脚本中无明文 Key。
        manifestPlaceholders["TENCENT_MAP_SDK_KEY"] =
            decodeMapKey(file("src/main/assets/mapkey.png"))
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.compose.adaptive)
    implementation(libs.androidx.compose.adaptive.layout)
    implementation(libs.androidx.compose.adaptive.navigation3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.tencent.map.vector.sdk)
    implementation(libs.tencent.openmap.foundation)
    implementation(libs.tencent.sdk.utilities)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.coil.compose)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.logging.interceptor)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.play.services.location)
    implementation(libs.retrofit)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)
}

/** 从 assets/mapkey.png 的 RGB LSB 解码 Key，与 KeyProvider/encode_map_key.py 的位格式一致。 */
fun decodeMapKey(file: File): String {
    val img = ImageIO.read(file)
    val bits = mutableListOf<Int>()
    for (y in 0 until img.height) {
        for (x in 0 until img.width) {
            val c = img.getRGB(x, y)
            bits.add((c shr 16) and 1)
            bits.add((c shr 8) and 1)
            bits.add(c and 1)
        }
    }
    fun byteAt(i: Int): Int {
        var v = 0
        for (j in 0 until 8) v = (v shl 1) or bits[i + j]
        return v
    }

    val magic = ByteArray(4) { byteAt(it * 8).toByte() }
    require(magic.contentEquals(byteArrayOf(0x4C, 0x46, 0x54, 0x31))) { "mapkey.png magic 不匹配" }
    val len = (byteAt(32) shl 8) or byteAt(40)
    require(len in 1..512) { "mapkey.png 非法长度 $len" }
    return ByteArray(len) { byteAt(48 + it * 8).toByte() }.decodeToString()
}