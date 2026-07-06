plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `jvm-test-suite`
}

group = "com.zenith"
version = "1.0.1-SNAPSHOT"
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
