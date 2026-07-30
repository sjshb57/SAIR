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

    lint {
        // NewApi in a library is only caught when the library itself is linted.
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core)
    // FilePickerPreference 继承 Preference
    api(libs.androidx.preference)
    implementation(libs.material)
}
