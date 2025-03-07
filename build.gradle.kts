plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "com.zenith"
version = "1.0.0-SNAPSHOT"
val pluginId = "zenithproxy.plugin.dev"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api(libs.idea.ext)
}

java {
    withSourcesJar()
}

gradlePlugin {
    val zenithPlugin by plugins.creating {
        id = pluginId
        implementationClass = "com.zenith.ZenithProxyDevGradlePlugin"
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
