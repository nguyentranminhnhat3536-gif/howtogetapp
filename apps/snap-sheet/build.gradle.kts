// AGP 9 có sẵn Kotlin (built-in Kotlin), không cần plugin org.jetbrains.kotlin.android.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
