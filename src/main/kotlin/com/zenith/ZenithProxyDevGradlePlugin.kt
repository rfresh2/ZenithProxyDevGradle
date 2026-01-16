package com.zenith

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.DefaultTaskExecutionRequest
import java.lang.Boolean.parseBoolean
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class ZenithProxyDevGradlePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("java")
        project.plugins.apply("com.gradleup.shadow")
        val extension = project.extensions.create("zenithProxyPlugin", ZenithProxyDevExtension::class.java, project)
        val zenithDepConfig = project.configurations.create("zenithProxy")
        project.configurations.getByName("implementation").extendsFrom(zenithDepConfig)
        project.configurations.getByName("annotationProcessor").extendsFrom(zenithDepConfig)
        project.configurations.forEach {
            it.resolutionStrategy.cacheDynamicVersionsFor(1, TimeUnit.HOURS)
            it.resolutionStrategy.cacheChangingModulesFor(1, TimeUnit.HOURS)
        }
        val shade = project.configurations.create("shade")
        project.configurations.getByName("implementation").extendsFrom(shade)
        val sourceSets = (project.extensions.getByName("sourceSets") as SourceSetContainer)
        val mainSourceSet = sourceSets.getByName("main")
        project.tasks.withType(ShadowJar::class.java) {
            configurations.set(listOf(shade))
            archiveClassifier.set("")
            project.tasks.getByName("build").dependsOn(this)
        }
        project.tasks.getByName("jar").enabled = false
        val shadowTask = project.tasks.withType(ShadowJar::class.java).first()
        val copyPluginTask = project.tasks.register("copyPlugin", Copy::class.java) {
            group = "run"
            description = "Copy Plugin To Run Directory"
            from(shadowTask.archiveFile) {
                include("*.jar")
                rename("(.*)", "plugin.jar")
            }
            dependsOn(project.tasks.getByName("build"))
        }
        val runTask = project.tasks.register("run", JavaExec::class.java) {
            group = "run"
            description = "Execute ZenithProxy With Plugin"
            // only main zenithproxy jar and dependencies
            // i.e. plugin classes and shaded deps to be read by zenith classloader on plugin.jar
            classpath = zenithDepConfig
            mainClass.set("com.zenith.Proxy")
            jvmArgs = listOf("-Xmx300m", "-XX:+UseG1GC")
            if (javaVersion.majorVersion.toInt() >= 24) {
                jvmArgs("--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED")
            }
            standardInput = System.`in`
            environment("ZENITH_DEV", "true")
            dependsOn(copyPluginTask)
        }
        val templateTask = project.tasks.register("generateTemplates", Copy::class.java) {
            group = "build"
            description = "Generates class templates"

            from(project.file("src/main/templates"))
            into(project.layout.buildDirectory.dir("generated/sources/templates"))
            enabled = false
        }
        mainSourceSet.java.srcDir(templateTask.map { it.outputs })
        project.tasks.withType(JavaCompile::class.java) {
            dependsOn(templateTask)
        }
        project.afterEvaluate {
            project.tasks.withType(ShadowJar::class.java) {
                manifest {
                    attributes(mapOf(
                        "Date" to OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString(),
                    ))
                }
            }
            copyPluginTask.configure {
                into(extension.runDirectory.get().dir("plugins"))
            }
            runTask.configure {
                workingDir = extension.runDirectory.get().asFile
            }
            if (extension.generateTemplateTask.get()) {
                templateTask.configure {
                    val props = extension.templateProperties.get()
                    inputs.properties(props)
                    expand(props)
                    enabled = true
                }
                if (ideaSyncActive()) {
                    val startParameter = project.gradle.startParameter
                    val taskRequests = startParameter.taskRequests.toMutableList()
                    taskRequests += DefaultTaskExecutionRequest(listOf(templateTask.name))
                    startParameter.setTaskRequests(taskRequests)
                }
            }
            project.tasks.withType(JavaCompile::class.java) {
                options.encoding = "UTF-8"
                options.release.set(extension.javaReleaseVersion.get().asInt())
            }
        }
    }
}

fun ideaSyncActive(): Boolean {
    return parseBoolean(System.getProperty("idea.sync.active"))
}
