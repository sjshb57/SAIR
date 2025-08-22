@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.aefyr.sai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aefyr.sai"
        minSdk = 23
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 63
        versionName = "4.8"
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

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("RELEASE_KEY_PWD") ?: ""
            storeFile = file("../keystore.jks")
            storePassword = System.getenv("RELEASE_KEY_STORE_PWD") ?: ""
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        //    isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }

    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/*android*")
            excludes.add("META-INF/*kotlin*")
            excludes.add("okhttp3/**")
            excludes.add("kotlin/**")
            excludes.add("META-INF/ASL2.0")
            excludes.add("META-INF/README.md")
            excludes.add("META-INF/services/**")
            excludes.add("META-INF/CHANGES")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room.runtime)

    annotationProcessor(libs.androidx.room.compiler) {
    exclude(group = "com.intellij", module = "annotations") }

    implementation(libs.material)
    implementation(libs.glide)
    implementation(libs.flexbox)
    implementation(libs.tooltips)
    implementation(libs.gson)
    implementation(libs.shimmer.android)

    // Shizuku/Sui
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    debugImplementation(libs.leakcanary.android)
}