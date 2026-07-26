plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.github.angads25.filepicker"
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
}

dependencies {
    implementation(libs.androidx.annotation)
    // FilePickerPreference 继承 Preference
    api(libs.androidx.preference)
    implementation(libs.material)
}
