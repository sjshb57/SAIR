plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.aefyr.flexfilter"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        // NewApi in a library is only caught when the library itself is linted.
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.recyclerview)
    implementation(libs.flexbox)
    // BaseBottomSheetDialogFragment 继承 BottomSheetDialogFragment,类型暴露在公开 API 上
    api(libs.material)
}
