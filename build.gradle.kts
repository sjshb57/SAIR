plugins {
    alias(libs.plugins.android.application) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject)
}