import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Upload key chỉ có trên CI khi build bản phát hành (workflow đặt biến này).
val uploadKeystorePath: String? = System.getenv("UPLOAD_KEYSTORE_FILE")

android {
    namespace = "com.ethanstudio.snapsheet"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ethanstudio.snapsheet"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        // Tăng bằng: python3 scripts/bump_version.py <slug>
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (uploadKeystorePath != null) {
            create("upload") {
                storeFile = file(uploadKeystorePath)
                storePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("upload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget("17")
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Báo cáo dạng chữ để CI đọc và gom lỗi; lint không chặn build debug.
        textReport = true
        abortOnError = false
        disable.addAll(
            listOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable", "OldTargetApi"),
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.mlkit.docscanner)
    implementation(libs.mlkit.text)
    implementation(libs.billing.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}

// Cấu hình Firebase (google-services.json) KHÔNG nằm trong git: CI ghi file từ GitHub Secret
// GOOGLE_SERVICES_JSON_SNAP_SHEET trước khi build. Thiếu file thì app vẫn build và chạy, chỉ là chưa đăng nhập được.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
