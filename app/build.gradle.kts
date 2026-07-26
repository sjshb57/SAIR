@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.aefyr.sai"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aefyr.sai"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 65
        versionName = "5.0"
        resourceConfigurations += setOf("zh-rCN", "zh-rTW")
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }
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
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/ASL2.0",
                "META-INF/CHANGES",
                "META-INF/README.md",
                "META-INF/*.version"
            )
        }
    }
}

dependencies {
    implementation(project(":libs:flexfilter"))
    implementation(project(":libs:filepicker"))
    implementation(project(":libs:tooltips"))
    implementation(project(":libs:pseudoapksigner"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room.runtime)

    annotationProcessor(libs.androidx.room.compiler) {
        exclude(group = "com.intellij", module = "annotations")
    }

    implementation(libs.material)
    implementation(libs.glide)
    implementation(libs.flexbox)
    implementation(libs.gson)
    implementation(libs.shimmer.android)

    // Shizuku/Sui
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    debugImplementation(libs.leakcanary.android)
}
