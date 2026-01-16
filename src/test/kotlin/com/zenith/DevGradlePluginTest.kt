package com.zenith

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DevGradlePluginTest {

    @Test
    fun examplePluginTest() {
        val tmpDir = File("build/tmp/example-plugin")
        tmpDir.deleteRecursively()
        tmpDir.mkdirs()

        val srcDir = File("src/test/resources/projects/example-plugin")
        srcDir.copyRecursively(tmpDir)
        System.setProperty("org.gradle.daemon", "false")

        val result = GradleRunner.create()
            .withProjectDir(tmpDir)
            .withArguments("clean", "build")
            .withPluginClasspath()
            .withGradleVersion("8.14")
            .forwardOutput()
            .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"), "Build should be successful")
    }
}
