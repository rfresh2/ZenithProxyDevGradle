# ZenithProxyDevGradle

A gradle plugin for ZenithProxy Java plugin development

## Usage

See ZenithProxy plugin template repository: https://github.com/rfresh2/ZenithProxyExamplePlugin

This plugin is published only to my personal maven: https://maven.2b2t.vc/releases

`settings.gradle.kts`
```kotlin
pluginManagement {
    repositories {
        maven("https://maven.2b2t.vc/releases")
    }
}
```

`build.gradle.kts`
```kotlin
plugins {
    id("zenithproxy.plugin.dev") version "1.+"
}

zenithProxyPlugin {
    // configure plugin extension properties here
}
```
