import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.changelog.Changelog

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "1.10.0"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "2.0.0"
    // kotlinx serialization - read more: https://github.com/Kotlin/kotlinx.serialization
    kotlin("plugin.serialization") version "1.5.31"
    // detekt linter - read more: https://github.com/detekt/detekt
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    // diktat linter - read more: https://github.com/saveourtool/diktat
    id("org.cqfn.diktat.diktat-gradle-plugin") version "1.2.4.2"
    // Gradle Kover Plugin
    id("org.jetbrains.kotlinx.kover") version "0.6.1"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Set the JVM language level used to build project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.22.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("org.jetbrains:markdown:0.2.4")
    implementation("io.sentry:sentry:6.1.0")
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.set(emptyList())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover.xmlReport {
    onCheck.set(true)
}

// Configure detekt plugin.
// Read more: https://detekt.github.io/detekt/kotlindsl.html
detekt {
    config = files("./detekt-config.yml")
    buildUponDefaultConfig = true

    reports {
        html.enabled = true
        xml.enabled = false
        txt.enabled = false
    }
}

tasks {
    processResources {
        val tokens = mapOf(
            "sentry_dsn" to (System.getenv("SENTRY_DSN") ?: ""),
        )

        filesMatching("plugin_config.json") {
            filteringCharset = "UTF-8"
            filter<ReplaceTokens>("tokens" to tokens)
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Get the latest available change notes from the changelog file
        changeNotes.set(provider {
            with(changelog) {
                renderItem(
                    getOrNull(properties("pluginVersion")) ?: getLatest(),
                    Changelog.OutputType.HTML,
                )
            }
        })
    }

    runIde {
        maxHeapSize = "2g"
    }
}
