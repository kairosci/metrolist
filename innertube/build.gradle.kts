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
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.json)
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.brotli)

    implementation("com.github.MetrolistGroup:MetrolistExtractor:6305155") {
        exclude(group = "com.google.protobuf")
    }
    testImplementation(libs.junit)
}
