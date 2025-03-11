package com.zenith

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ZenithProxyDevGradlePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("java")
        project.plugins.apply("org.jetbrains.gradle.plugin.idea-ext")
        project.plugins.apply("com.gradleup.shadow")
        val extension = project.extensions.create("zenithProxy", ZenithProxyDevExtension::class.java, project)
        project.repositories.apply {
            maven { mvn ->
                mvn.url("https://libraries.minecraft.net")
                mvn.mavenContent { content ->
                    content.includeGroup("com.mojang")
                }
            }
            maven { mvn ->
                mvn.url("https://repo.opencollab.dev/maven-releases/")
                mvn.mavenContent { content ->
                    content.includeGroupByRegex("org.cloudburstmc.*")
                }
            }
            maven { mvn ->
                mvn.url("https://repo.papermc.io/repository/maven-public/")
                mvn.mavenContent { content ->
                    content.includeGroup("com.velocitypowered")
                }
            }
            maven { mvn ->
                mvn.url("https://repo.viaversion.com")
                mvn.mavenContent { content ->
                    content.includeGroup("com.viaversion")
                    content.includeGroup("net.raphimc")
                }
            }
            maven { mvn ->
                mvn.url("https://maven.lenni0451.net/releases")
                mvn.mavenContent { content ->
                    content.includeGroup("net.raphimc")
                    content.includeGroup("net.lenni0451")
                }
            }
            mavenCentral()
        }
        val shade = project.configurations.create("shade")
        project.configurations.getByName("implementation").extendsFrom(shade)
        val sourceSets = (project.extensions.getByName("sourceSets") as SourceSetContainer)
        val mainSourceSet = sourceSets.getByName("main")
        project.tasks.withType(ShadowJar::class.java) {
            it.configurations = listOf(shade)
            it.archiveClassifier.set("")
            project.tasks.getByName("build").dependsOn(it)
        }
        project.tasks.getByName("jar").enabled = false
        val shadowTask = project.tasks.withType(ShadowJar::class.java).first()
        val copyPluginTask = project.tasks.register("copyPlugin", Copy::class.java) {
            it.group = "run"
            it.description = "Copy Plugin To Run Directory"
            it.from(shadowTask.archiveFile) {
                it.include("*.jar")
                it.rename("(.*)", "plugin.jar")
            }
            it.dependsOn(project.tasks.getByName("build"))
        }
        val runTask = project.tasks.register("run", JavaExec::class.java) {
            it.group = "run"
            it.description = "Execute ZenithProxy With Plugin"
            it.classpath = mainSourceSet.runtimeClasspath
                // filter out duplicate classpath entries
                // we want zenith to load our plugin classes from the run directory like it would in prod
                .filter { !it.path.contains(project.layout.buildDirectory.asFile.get().path) }
            it.mainClass.set("com.zenith.Proxy")
            it.jvmArgs = listOf("-Xmx300m", "-XX:+UseG1GC")
            it.standardInput = System.`in`
            it.environment("ZENITH_DEV", "true")
            it.dependsOn(copyPluginTask)
        }
        val templateTask = project.tasks.register("generateTemplates", Copy::class.java) {
            it.group = "build"
            it.description = "Generates class templates"

            it.from(project.file("src/main/templates"))
            it.into(project.layout.buildDirectory.dir("generated/sources/templates"))
            it.enabled = false
        }
        mainSourceSet.java.srcDir(templateTask.map { it.outputs })
        project.plugins.getPlugin(IdeaPlugin::class.java).model.project.settings.taskTriggers.afterSync(templateTask)
        project.tasks.withType(JavaCompile::class.java) {
            it.dependsOn(templateTask)
        }
        project.afterEvaluate {
            if (extension.autoDependencies.get()) {
                val zenithMavenCoordinates = "com.zenith:ZenithProxy:${extension.mc.get()}-SNAPSHOT"
                project.dependencies.apply {
                    add("implementation", zenithMavenCoordinates)
                    add("annotationProcessor", zenithMavenCoordinates)
                }
            }
            project.tasks.withType(ShadowJar::class.java) {
                it.manifest {
                    it.attributes(mapOf(
                        "Date" to OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString(),
                        "MC-Version" to extension.mc.get()
                    ))
                }
            }
            copyPluginTask.configure {
                it.into(extension.runDirectory.get().dir("plugins"))
            }
            runTask.configure {
                it.workingDir = extension.runDirectory.get().asFile
            }
            if (extension.generateTemplateTask.get()) {
                templateTask.configure {
                    val props = extension.templateProperties.get()
                    it.inputs.properties(props)
                    it.expand(props)
                    it.enabled = true
                }
            }
            project.plugins.getPlugin(IdeaPlugin::class.java).model.module {
                it.excludeDirs.add(extension.runDirectory.get().asFile)
                it.excludeDirs.add(project.layout.projectDirectory.dir(".idea").asFile)
            }
            project.tasks.withType(JavaCompile::class.java) {
                it.options.encoding = "UTF-8"
                it.options.release.set(extension.javaReleaseVersion.get().asInt())
            }
        }
    }
}

fun MavenArtifactRepository.url(url: Any) {
    setUrl(url)
}
