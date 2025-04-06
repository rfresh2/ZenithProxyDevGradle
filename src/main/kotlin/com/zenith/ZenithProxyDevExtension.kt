package com.zenith

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.jvm.toolchain.JavaLanguageVersion

abstract class ZenithProxyDevExtension(project: Project) {
    @get:Input
    abstract val runDirectory: DirectoryProperty

    @get:Input
    abstract val generateTemplateTask: Property<Boolean>

    @get:Input
    abstract val templateProperties: MapProperty<String, Any>

    @get:Input
    abstract val javaReleaseVersion: Property<JavaLanguageVersion>

    init {
        runDirectory.convention(project.layout.projectDirectory.dir("run"))
        generateTemplateTask.convention(true)
        templateProperties.convention(mutableMapOf())
        javaReleaseVersion.convention(JavaLanguageVersion.of(21))
    }
}
