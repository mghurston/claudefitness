// Top-level build file — plugins declared here with apply false, applied in modules.
// AGP 9 has Kotlin support built in, so there is no separate kotlin-android plugin to
// declare — the Kotlin version comes from `androidComponents`/the AGP-bundled compiler.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
