import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

// 读取本地解密密钥（local.properties，不进版本库）与密文（tencent-key.properties，可提交）。
fun loadProps(path: String): Properties = Properties().apply {
    val f = rootProject.file(path)
    if (f.exists()) f.inputStream().use { load(it) }
}

fun decryptTencentMapKey(): String {
    val secret = loadProps("local.properties").getProperty("tencent.map.key.secret")
    val keyProps = loadProps("app/tencent-key.properties")
    val cipherText = keyProps.getProperty("tencent.map.key.cipher")
    val ivText = keyProps.getProperty("tencent.map.key.iv")
    if (secret.isNullOrBlank() || cipherText.isNullOrBlank() || ivText.isNullOrBlank()) {
        throw GradleException(
            "腾讯地图 Key 未配置：请在 local.properties 中设置 tencent.map.key.secret（与 app/tencent-key.properties 的密文配对）。"
        )
    }
    val keyBytes = secret.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val ivBytes = ivText.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
    return String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), Charsets.UTF_8)
}

val tencentMapKey = decryptTencentMapKey()

android {
    namespace = "com.unic.left"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.unic.left"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // 运行时从密文解密得到，Manifest 中只保留占位符，明文不进版本库。
        manifestPlaceholders["TENCENT_MAP_KEY"] = tencentMapKey

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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