import java.io.*
import java.util.Properties
import java.util.TreeMap
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.desktop.common)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.swing)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":innertube"))
    implementation(project(":connectivity"))
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

val generatedResourcesDir = layout.buildDirectory.dir("generated/resources")

sourceSets.main {
    resources.srcDir(generatedResourcesDir)
}

abstract class ConvertI18nTask : DefaultTask() {
    @get:InputDirectory
    abstract val androidResDir: DirectoryProperty

    @get:InputDirectory
    abstract val desktopOverridesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun convert() {
        val i18nDir = outputDir.asFile.get()
        i18nDir.mkdirs()

        val factory = DocumentBuilderFactory.newInstance()
        val desktopOverrides: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

        val desktopFiles = desktopOverridesDir.asFile.get().listFiles() ?: emptyArray()
        for (file in desktopFiles) {
            val name = file.nameWithoutExtension
            val locale: String = when {
                name == "strings_desktop" -> ""
                name.startsWith("strings_desktop_") -> name.removePrefix("strings_desktop_")
                else -> continue
            }
            val props = Properties()
            try {
                file.inputStream().buffered().use { stream -> props.load(stream) }
            } catch (_: Exception) { }
            val map: MutableMap<String, String> = mutableMapOf()
            for (entry in props.entries) {
                map[entry.key.toString()] = entry.value.toString()
            }
            desktopOverrides[locale] = map
        }

        val allLocales: MutableSet<String> = mutableSetOf("")

        val nonLocaleQualifiers = setOf("night", "v31", "night-v31", "v33", "night-v33")
        val resDirs = androidResDir.asFile.get().listFiles() ?: emptyArray()
        for (dir in resDirs) {
            if (!dir.isDirectory) continue
            val locale: String = when {
                dir.name == "values" -> ""
                dir.name.startsWith("values-") -> {
                    val qualifier = dir.name.removePrefix("values-")
                    if (qualifier in nonLocaleQualifiers) continue
                    qualifier
                }
                else -> continue
            }
            allLocales.add(locale)
        }

        for (locale in allLocales) {
            val props: MutableMap<String, String> = mutableMapOf()

            val defaultMetrolistFile = File(androidResDir.asFile.get(), "values/metrolist_strings.xml")
            if (defaultMetrolistFile.exists()) {
                parseAndroidXml(defaultMetrolistFile, factory, props)
            }

            if (locale.isNotEmpty()) {
                val localizedFile = File(androidResDir.asFile.get(), "values-$locale/metrolist_strings.xml")
                if (localizedFile.exists()) {
                    parseAndroidXml(localizedFile, factory, props)
                }
            }

            val overrides = desktopOverrides[locale]
            if (overrides != null) {
                props.putAll(overrides)
            }

            val propsFile: File = if (locale.isEmpty()) {
                File(i18nDir, "strings.properties")
            } else {
                File(i18nDir, "strings_$locale.properties")
            }

            val sorted = TreeMap(props)
            val outProps = Properties()
            for ((k, v) in sorted) {
                outProps.setProperty(k, v)
            }
            try {
                propsFile.outputStream().buffered().use { stream ->
                    outProps.store(stream, "Auto-generated from Android XML + desktop overrides")
                }
            } catch (_: Exception) { }
        }
    }

    private fun parseAndroidXml(file: File, factory: DocumentBuilderFactory, props: MutableMap<String, String>) {
        try {
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(file)
            doc.documentElement.normalize()

            val stringNodes = doc.getElementsByTagName("string")
            for (i in 0 until stringNodes.length) {
                val node = stringNodes.item(i)
                val name = node.attributes.getNamedItem("name")?.textContent ?: continue
                val raw = node.textContent ?: continue
                val value = unescapeAndroidXml(raw)
                if (name.isNotEmpty() && value.isNotEmpty()) {
                    props[name] = value
                }
            }

            val pluralsNodes = doc.getElementsByTagName("plurals")
            for (i in 0 until pluralsNodes.length) {
                val node = pluralsNodes.item(i)
                val name = node.attributes.getNamedItem("name")?.textContent ?: continue
                val children = node.childNodes
                for (j in 0 until children.length) {
                    val child = children.item(j)
                    if (child.nodeName == "item") {
                        val quantity = child.attributes.getNamedItem("quantity")?.textContent
                        if (quantity == "other") {
                            val raw = child.textContent ?: continue
                            val value = unescapeAndroidXml(raw)
                            props[name] = value
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) { }
    }

    private fun unescapeAndroidXml(raw: String): String {
        return raw
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace("\\'", "'")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\@", "@")
            .replace("\\\\", "\\")
            .replace("\\?", "?")
    }
}

val convertI18n = tasks.register<ConvertI18nTask>("convertI18n") {
    description = "Convert Android XML string resources to Java .properties files"
    androidResDir.set(file("../app/src/main/res"))
    desktopOverridesDir.set(file("src/main/resources/i18n"))
    outputDir.set(generatedResourcesDir.map { it.dir("i18n") })
}

tasks.named("processResources") {
    dependsOn(convertI18n)
}

compose.desktop {
    application {
        mainClass = "com.metrolist.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "Metrolist"
            packageVersion = "13.4.2"
            description = "Metrolist Desktop - YouTube Music client"

            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }
        }
    }
}
