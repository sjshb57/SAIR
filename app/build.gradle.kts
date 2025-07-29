plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.aefyr.sai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aefyr.sai"
        minSdk = 21
        targetSdk = 34
        versionCode = 60
        versionName = "4.5"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }
        resConfigs("zh-rCN","zh-rTW")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "version"
    productFlavors {
        create("normal") {
            dimension = "version"
            buildConfigField("int", "DEFAULT_THEME", "0")
            buildConfigField("int", "DEFAULT_DARK_THEME", "1")
            buildConfigField("boolean", "HIDE_DONATE_BUTTON", "false")
        }

        create("fdroid") {
            dimension = "version"
            applicationId = "com.aefyr.sai.fdroid"
            buildConfigField("int", "DEFAULT_THEME", "6")
            buildConfigField("int", "DEFAULT_DARK_THEME", "11")
            buildConfigField("boolean", "HIDE_DONATE_BUTTON", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room.runtime)
 //   implementation(libs.androidx.room.compiler)
    annotationProcessor(libs.androidx.room.compiler) {
    exclude(group = "com.intellij", module = "annotations")
   }
    implementation(libs.material)

    implementation(libs.glide)
    implementation(libs.flexbox)
    implementation(libs.tooltips)
    implementation(libs.gson)
    implementation(libs.shimmer)

    // Shizuku/Sui
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    debugImplementation(libs.leakcanary.android)
}