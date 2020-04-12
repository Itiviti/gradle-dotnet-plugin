package com.itiviti

import groovy.json.JsonSlurper
import kotlinx.coroutines.*
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.regex.Pattern

class DotnetPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("dotnet", DotnetPluginExtension::class.java, project.name)
        val extensionAware = extension as ExtensionAware
        val restoreExtension = extensionAware.extensions.create("restore", DotnetRestoreExtension::class.java)
        extensionAware.extensions.create("build", DotnetBuildExtension::class.java)
        val testExtension = extensionAware.extensions.create("test", DotnetTestExtension::class.java, project.buildDir)
        (testExtension as ExtensionAware).extensions.create("nunit", DotnetNUnitExtension::class.java, project.buildDir)

        project.afterEvaluate {
            it.logger.lifecycle("Start restoring packages")

            if (restore(it, extension, restoreExtension) != 0) {
                throw GradleException("dotnet restore fails")
            }

            it.logger.lifecycle("Complete restoring packages")

            it.logger.lifecycle("Start parsing project")

            parseProjects(project, extension)

            it.logger.lifecycle("Complete parsing project")
        }

        // Register a task
        val dotnetClean = project.tasks.register("dotnetClean", DotnetCleanTask::class.java) { task ->
            task.group = "dotnet"
            task.description = "Cleans the output of a project."
        }

        val dotnetBuild = project.tasks.register("dotnetBuild", DotnetBuildTask::class.java) { task ->
            task.group = "dotnet"
            task.description = "Builds a project and all of its dependencies."
        }

        val dotnetTest = project.tasks.register("dotnetTest", DotnetTestTask::class.java) { task ->
            task.group = "dotnet"
            task.description = ".NET test driver used to execute unit tests."
        }

        project.plugins.withId("base") {
            dotnetClean.configure { it.dependsOn("clean") }
            dotnetBuild.configure { it.dependsOn("build") }
            dotnetTest.configure { it.dependsOn("test") }
        }
    }

    private fun restore(project: Project, extension: DotnetPluginExtension, restoreExtension: DotnetRestoreExtension): Int = runBlocking {
        val command = mutableListOf(extension.dotnetExecutable, "restore")

        if (!extension.solution.isNullOrBlank()) {
            command.add(extension.solution!!)
        }
        command.add("--verbosity")
        command.add(extension.verbosity)
        if (restoreExtension.force) {
            command.add("--force")
        }
        if (restoreExtension.noCache) {
            command.add("--no-cache")
        }

        project.logger.info("  Start executing command: {} in {}", command.joinToString(" "), extension.workingDir)

        val restore = async (Dispatchers.IO) restoreCoroutine@ {
            val process = ProcessBuilder(command)
                    .directory(extension.workingDir)
                    .start()

            val info = launch(Dispatchers.IO) {
                process.inputStream.reader().forEachLine { line ->
                    project.logger.info(line)
                }
            }

            val error = launch(Dispatchers.IO) {
                process.errorStream.reader().forEachLine { line ->
                    project.logger.error(line)
                }
            }
            process.waitFor()

            info.join()
            error.join()

            return@restoreCoroutine process.exitValue()
        }

        return@runBlocking restore.await()
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseProjects(project: Project, extension: DotnetPluginExtension) = runBlocking {
        var targetFile: File?
        if (extension.solution.isNullOrBlank()) {
            // guess the solution similar to dotnet cli, i.e. searches the current working directory for a file that has a file extension that ends in either proj or sln and uses that file.
            targetFile = extension.workingDir.listFiles{ file -> file.length() > 0 && file.name.endsWith(".sln", true)}?.firstOrNull()
            if (targetFile == null) {
                targetFile = extension.workingDir.listFiles{ file -> file.length() > 0 && file.name.endsWith("proj", true)}?.firstOrNull()
            }
        } else {
            targetFile = File(extension.workingDir, extension.solution!!)
        }

        if (targetFile?.exists() != true) {
            throw GradleException("Cannot find a valid project file, please setup workingDir / solution correctly")
        }

        val tempDir = withContext(Dispatchers.IO) {
            Files.createDirectories(File(project.buildDir, "tmp/dotnet").toPath())
        }

        project.logger.info("  Extracting parser to {}", tempDir)

        val reflections = Reflections(ConfigurationBuilder().setScanners(ResourcesScanner()).setUrls(ClasspathHelper.forPackage("com.itiviti.parser")))
        reflections.getResources(Pattern.compile(".*\\.cs(proj)?")).forEach {
            Files.copy(javaClass.classLoader.getResourceAsStream(it)!!, tempDir.resolve(File(it).name), StandardCopyOption.REPLACE_EXISTING)
        }

        val arg = "{${if (!extension.platform.isNullOrBlank()) "'Platform':'${extension.platform}'," else ""}'Configuration':'${extension.configuration}'}"
        val command = listOf(extension.dotnetExecutable, "run", "--", targetFile.absolutePath, arg)
        project.logger.info("  Start executing command: {} in {}", command.joinToString(" "), tempDir)

        val jsonText = async(Dispatchers.IO) {
            val process = ProcessBuilder(command)
                    .directory(tempDir.toFile())
                    .start()

            val error = launch(Dispatchers.IO) {
                process.errorStream.reader().forEachLine {
                    project.logger.error(it)
                }
            }

            val result = async(Dispatchers.IO) parser@ {
                val sb = StringBuilder()
                process.inputStream.reader().forEachLine {
                    sb.appendln(it)
                }
                return@parser sb.toString()
            }

            process.waitFor()
            error.join()

            if (process.exitValue() != 0) {
                throw GradleException("Parse project file $targetFile failed")
            }

            return@async result.await()
        }

        val result = JsonSlurper().parseText(jsonText.await()) as MutableMap<String, Any>

        extension.allProjects = result.map { entry -> entry.key to DotnetProject(entry.value as Map<String, Any>) }.toMap()
    }
}
