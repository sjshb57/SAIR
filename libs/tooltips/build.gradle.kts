plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.tomergoldst.tooltips"
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
    implementation(libs.androidx.appcompat)
}
