plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.aefyr.sai"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aefyr.sai"
        minSdk = 24
        targetSdk = 37
        versionCode = 67
        versionName = "5.2"
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

    val releaseKeystore = rootProject.file("keystore.jks")

    signingConfigs {
        create("release") {
            if (releaseKeystore.exists()) {
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("RELEASE_KEY_PWD") ?: ""
                storeFile = releaseKeystore
                storePassword = System.getenv("RELEASE_KEY_STORE_PWD") ?: ""
            }
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Without the keystore the release build stays unsigned instead of failing,
            // so a fresh clone can still be built.
            if (releaseKeystore.exists())
                signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    androidResources {
        localeFilters += setOf("zh-rCN", "zh-rTW")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        // apksig's v1 signer and certificate parser use java.util.Base64, which is API 26+.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        // Library modules are not linted by :app:lint unless this is enabled.
        checkDependencies = true
        abortOnError = true
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
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.apksig)

    implementation(project(":libs:flexfilter"))
    implementation(project(":libs:filepicker"))
    implementation(project(":libs:tooltips"))

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
