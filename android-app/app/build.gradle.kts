plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pptclicker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pptclicker"
        minSdk = 28          // Android 9.0 — BluetoothHidDevice API 起始版本
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    // 签名配置：从环境变量读取 keystore；本地未配置时回退到 debug 签名。
    // CI（GitHub Actions）通过 Secrets 注入这些环境变量，详见 .github/scripts/gen-keystore.md。
    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            if (!keystoreFile.isNullOrEmpty()) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 配置了 keystore 才用 release 签名，否则 Gradle 会用默认 debug 签名（仍可安装）
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            if (!keystoreFile.isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // WebSocket 客户端（用于 WiFi 模式连接伴侣程序）
    implementation("org.java-websocket:Java-WebSocket:1.5.6")
    // JSON 解析
    implementation("org.json:json:20240303")
    // AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // Lifecycle / 协程
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
