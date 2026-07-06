package com.zenith

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

class DevGradlePluginTest {

    @ParameterizedTest
    @ValueSource(strings = ["9.6.0", "9.6.1"])
    fun examplePluginTest(gradleVersion: String) {
        val tmpDir = File("build/tmp/example-plugin")
        tmpDir.deleteRecursively()
        tmpDir.mkdirs()

        val srcDir = File("src/test/resources/projects/example-plugin")
        srcDir.copyRecursively(tmpDir)
        System.setProperty("org.gradle.daemon", "false")
        val runnerBuilder = GradleRunner.create()
            .withProjectDir(tmpDir)
            .withArguments("clean", "build")
            .withPluginClasspath()
            .withGradleVersion(gradleVersion)
            .forwardOutput()
            .withDebug(true);

        if (gradleVersion == "9.6.0") {
            val result = runnerBuilder.buildAndFail()
            assertTrue(result.output.contains("BUILD FAILED"), "Build should fail")
        } else {
            val result = runnerBuilder.build()
            assertTrue(result.output.contains("BUILD SUCCESSFUL"), "Build should be successful")
        }
    }
}
