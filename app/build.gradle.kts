plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Khoá ký bản phát hành: chỉ có khi chạy trên GitHub (lấy từ Secrets), máy khác thì để trống.
val uploadKeystore: String? = System.getenv("PEACHY_KEYSTORE_FILE")
val buildNumber: Int = (System.getenv("VERSION_CODE") ?: "1").toInt()

android {
    namespace = "com.cindy.ngaykyniem"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cindy.peachy"
        minSdk = 26
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    signingConfigs {
        if (uploadKeystore != null) {
            create("upload") {
                storeFile = file(uploadKeystore)
                storePassword = System.getenv("PEACHY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PEACHY_KEY_ALIAS") ?: "peachy"
                keyPassword = System.getenv("PEACHY_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (uploadKeystore != null) signingConfigs.getByName("upload") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
