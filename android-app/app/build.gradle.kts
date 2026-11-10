plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.droidpad"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.droidpad"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_PATH")
                ?: project.findProperty("android.injected.signing.store.file") as String?
            if (keystoreFile != null && file(keystoreFile).exists()) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: project.findProperty("android.injected.signing.store.password") as String? ?: ""
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: project.findProperty("android.injected.signing.key.alias") as String? ?: ""
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: project.findProperty("android.injected.signing.key.password") as String? ?: ""
            } else {
                // Fallback for CI/open-source release builds:
                // Sign with debug key so generated APK can be installed on devices immediately
                val debugConfig = getByName("debug")
                initWith(debugConfig)
                val debugStore = storeFile ?: debugConfig.storeFile
                if (debugStore != null && !debugStore.exists()) {
                    debugStore.parentFile?.mkdirs()
                    try {
                        ProcessBuilder(
                            "keytool", "-genkey", "-v",
                            "-keystore", debugStore.absolutePath,
                            "-storepass", "android",
                            "-alias", "androiddebugkey",
                            "-keypass", "android",
                            "-keyalg", "RSA",
                            "-keysize", "2048",
                            "-validity", "10000",
                            "-dname", "CN=Android Debug,O=Android,C=US"
                        ).inheritIO().start().waitFor()
                    } catch (e: Exception) {
                        project.logger.warn("Could not auto-generate debug keystore: ${e.message}")
                    }
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.json)

    debugImplementation(libs.androidx.ui.tooling)
}
