import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `jvm-test-suite`
}

group = "com.zenith"
version = property("plugin_version") as String
val pluginId = "zenithproxy.plugin.dev"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api(libs.shadow)
}

java {
    withSourcesJar()
}

kotlin {
    explicitApi()
    val jdkRelease = "17"
    compilerOptions {
        allWarningsAsErrors = true
        // https://docs.gradle.org/current/userguide/compatibility.html#kotlin
        apiVersion = KotlinVersion.KOTLIN_2_3
        languageVersion = apiVersion
        jvmTarget = JvmTarget.fromTarget(jdkRelease)
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        freeCompilerArgs.add("-Xjdk-release=$jdkRelease")
    }
    target.compilations.configureEach {
        compileJavaTaskProvider { options.release = jdkRelease.toInt() }
    }
}

gradlePlugin {
    val zenithPlugin = plugins.create(pluginId) {
        id = pluginId
        implementationClass = "com.zenith.ZenithProxyDevGradlePlugin"
    }
}

testing {
    suites {
        val test = getByName<JvmTestSuite>("test") {
            useJUnitJupiter()
        }
    }
}

publishing {
    repositories {
        maven {
            name = "vc"
            url = uri("https://maven.2b2t.vc/releases")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}
