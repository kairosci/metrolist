plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    id("java-library")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.network)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coroutines.core)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)
}
