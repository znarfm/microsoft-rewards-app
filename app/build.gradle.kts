plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.znarfm.bing"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.znarfm.bing"
        minSdk = 24
        targetSdk = 36
        versionCode = 39
        versionName = "2.5.7"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_FILE")
                ?: (project.findProperty("KEYSTORE_FILE") as? String)
                ?: "upload-keystore.jks"
            storeFile = file(keystorePath)
            storePassword = System.getenv("KEYSTORE_STORE_PASSWORD")
                ?: (project.findProperty("KEYSTORE_STORE_PASSWORD") as? String)
            keyAlias = System.getenv("KEYSTORE_KEY_ALIAS")
                ?: (project.findProperty("KEYSTORE_KEY_ALIAS") as? String)
            keyPassword = System.getenv("KEYSTORE_KEY_PASSWORD")
                ?: (project.findProperty("KEYSTORE_KEY_PASSWORD") as? String)
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseConfig = signingConfigs.getByName("release")
            val hasReleaseSigning = releaseConfig.storeFile?.exists() == true &&
                !releaseConfig.storePassword.isNullOrBlank() &&
                !releaseConfig.keyAlias.isNullOrBlank() &&
                !releaseConfig.keyPassword.isNullOrBlank()
            signingConfig = if (hasReleaseSigning) {
                releaseConfig
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    splits {
        abi {
            val isBuildingBundle = gradle.startParameter.taskNames.any {
                it.contains("bundle", ignoreCase = true)
            }
            isEnable = !isBuildingBundle
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
