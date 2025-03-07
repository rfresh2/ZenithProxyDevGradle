/*
 * This source file was generated by the Gradle 'init' task
 */
package com.zenith

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers
import java.time.OffsetDateTime
import java.time.ZoneOffset

abstract class ZenithProxyDevExtension(val project: Project) {
    @get:Input
    abstract val mc: Property<String>
}

class ZenithProxyDevGradlePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("java")
        project.pluginManager.apply("org.jetbrains.gradle.plugin.idea-ext")
        val extension = project.extensions.create("zenithProxy", ZenithProxyDevExtension::class.java, project)
        project.repositories.apply {
            mavenLocal()
            maven {
                it.setURL("https://maven.2b2t.vc/releases")
            }
            maven { mvn ->
                mvn.setURL("https://libraries.minecraft.net")
                mvn.mavenContent { content ->
                    content.includeGroup("com.mojang")
                }
            }
            maven { mvn ->
                mvn.setURL("https://repo.opencollab.dev/maven-releases/")
                mvn.mavenContent { content ->
                    content.includeGroupByRegex("org.cloudburstmc.*")
                }
            }
            maven { mvn ->
                mvn.setURL("https://repo.papermc.io/repository/maven-public/")
                mvn.mavenContent { content ->
                    content.includeGroup("com.velocitypowered")
                }
            }
            maven { mvn ->
                mvn.setURL("https://repo.viaversion.com")
                mvn.mavenContent { content ->
                    content.includeGroup("com.viaversion")
                    content.includeGroup("net.raphimc")
                }
            }
            maven { mvn ->
                mvn.setURL("https://maven.lenni0451.net/releases")
                mvn.mavenContent { content ->
                    content.includeGroup("net.raphimc")
                    content.includeGroup("net.lenni0451")
                }
            }
            mavenCentral()
        }
        project.afterEvaluate {
            val zenithMavenCoordinates = "com.zenith:ZenithProxy:${extension.mc.get()}-SNAPSHOT"
            project.dependencies.apply {
                add("implementation", zenithMavenCoordinates)
                add("annotationProcessor", zenithMavenCoordinates)
            }
        }
        val runDirectory = project.layout.projectDirectory.dir("run")
        val copyPluginTask = project.tasks.register("copyPlugin", Copy::class.java) {
            it.group = "build"
            it.description = "Copy Plugin To ZenithProxy"
            it.from(project.layout.buildDirectory.dir("libs")) {
                it.include("*.jar")
                it.rename("(.*)", "plugin.jar")
            }
            it.into(runDirectory.dir("plugins"))
            it.dependsOn(project.tasks.getByName("build"))
        }
        val sourceSets = (project.extensions.getByName("sourceSets") as SourceSetContainer)
        val mainSourceSet = sourceSets.getByName("main")
        project.tasks.register("run", JavaExec::class.java) {
            it.group = "build"
            it.description = "Execute ZenithProxy With Plugin"
            it.classpath = mainSourceSet.runtimeClasspath
                // filter out duplicate classpath entries
                // we want zenith to load our plugin classes from the run directory like it would in prod
                .filter { !it.path.contains(project.layout.buildDirectory.asFile.get().path) }
            it.workingDir = runDirectory.asFile
            it.mainClass.set("com.zenith.Proxy")
            it.jvmArgs = listOf("-Xmx300m", "-XX:+UseG1GC")
            it.standardInput = System.`in`
            it.environment("ZENITH_DEV", "true")
            it.dependsOn(copyPluginTask)
        }
        val templateTask = project.tasks.register("generateTemplates", Copy::class.java) {
            it.group = "build"
            it.description = "Generates class templates"
            val props = mapOf(
                "version" to project.version
            )
            it.inputs.properties(props)
            it.from(project.file("src/main/templates"))
            it.into(project.layout.buildDirectory.dir("generated/sources/templates"))
            it.expand(props)
        }
        mainSourceSet.java.srcDir(templateTask.map { it.outputs })
        project.plugins.getPlugin(IdeaPlugin::class.java).model.project.settings.taskTriggers.afterSync(templateTask)
        project.tasks.withType(JavaCompile::class.java) {
            it.options.encoding = "UTF-8"
            it.options.isDeprecation = true
            it.options.release.set(21)
            it.dependsOn(templateTask)
        }
        project.afterEvaluate {
            project.tasks.withType(Jar::class.java) {
                it.manifest { // metadata about the plugin build
                    it.attributes(mapOf(
                        "Date" to OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString(),
                        "MC-Version" to extension.mc.get()
                    ))
                }
            }
        }
        project.plugins.getPlugin(IdeaPlugin::class.java).model.module {
            it.excludeDirs.add(runDirectory.asFile)
            it.excludeDirs.add(project.layout.projectDirectory.dir(".idea").asFile)
        }
    }
}

fun MavenArtifactRepository.setURL(url: Any) {
    setUrl(url)
}
