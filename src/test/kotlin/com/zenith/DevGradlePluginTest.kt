package com.zenith

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class DevGradlePluginTest {

    @Test
    fun examplePluginTest() {
        val result = GradleRunner.create()
            .withProjectDir(Path.of("src/test/resources/projects/example-plugin").toFile())
            .withArguments("build")
            .withPluginClasspath()
            .withGradleVersion("8.14")
            .forwardOutput()
            .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"), "Build should be successful")
    }
}
