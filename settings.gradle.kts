pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven {
            url = uri("https://repo.rikka.app/")
            metadataSources {
                artifact()
            }
        }
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "SAIR"
include(":app")