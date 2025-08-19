package com.itiviti

import com.google.gson.Gson
import com.itiviti.extensions.*
import com.itiviti.tasks.*
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.regex.Pattern

class DotnetPlugin: Plugin<Project> {
    companion object {
        const val TASK_GROUP = "dotnet"

        private fun restore(project: Project, extension: DotnetPluginExtension, restoreExtension: DotnetRestoreExtension, buildExtension: DotnetBuildExtension): Int {
            return project.providers.exec { exec ->
                exec.commandLine(extension.dotnetExecutable)
                exec.workingDir(extension.workingDir)
                exec.args("restore")
                if (!extension.solution.isNullOrBlank()) {
                    exec.args(extension.solution!!)
                }
                exec.args("--verbosity", extension.verbosity)
                DotnetBuildTask.restoreArgs(restoreExtension, buildExtension, exec)
            }.result.get().exitValue
        }

        private fun getMajorVersion(version: String) = version.substringBeforeLast('-', version).substringBeforeLast('.').toDouble()

        fun listSdks(project: Project, extension: DotnetPluginExtension): String {
            return project.providers.exec { exec ->
                exec.commandLine(extension.dotnetExecutable)
                exec.args("--list-sdks")
            }.standardOutput.asText.get()
        }

        @JvmStatic
        fun getDotnetVersion(project: Project, extension: DotnetPluginExtension): String {
            return project.providers.exec { exec ->
                exec.commandLine(extension.dotnetExecutable)
                exec.args("--version")
            }.standardOutput.asText.get()
        }

        @JvmStatic
        private fun fixEnv(project: Project, msbuildSdksPath: String, extension: DotnetPluginExtension, currentVersion: String) {
            val regex = Regex("\"version\"\\s*:\\s*\"(\\S+)\"")
            val sdkRuntimeMajorVersion = try {
                Files.readAllLines(Paths.get(msbuildSdksPath, "..", "dotnet.runtimeconfig.json"))
                    .mapNotNull { regex.find(it)?.groupValues?.get(1) }
                    .map { getMajorVersion(it) }
                    .first()
            } catch (e: Exception) {
                ""
            }

            if (getMajorVersion(currentVersion) != sdkRuntimeMajorVersion) {
                project.logger.warn("MSBuildSDKsPath does not match the executing SDK, attempt to search for the right SDK")
                val sdksString = listSdks(project, extension)
                val basePath = sdksString.lines()
                        .map { it.trim() }
                        .first { it.startsWith(currentVersion) }
                        .substringAfter('[')
                        .substringBefore(']')
                val actualSdkPath = Paths.get(basePath, currentVersion, "Sdks")
                extension.msbuildSDKsPath = actualSdkPath.toString()

                project.tasks.withType(DotnetBaseTask::class.java) {
                    it.environment("MSBuildSDKsPath", actualSdkPath.toString())
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseProjects(project: Project, extension: DotnetPluginExtension, buildExtension: DotnetBuildExtension): Map<String, DotnetProject> {
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

            // Detect dotnet version
            val versionString = getDotnetVersion(project, extension)
            val majorVersion = getMajorVersion(versionString)

            if (extension.msbuildSDKsPath == null) {
                val msbuildSdksPath = System.getenv("MSBuildSDKsPath")
                if (!msbuildSdksPath.isNullOrEmpty()) {
                    fixEnv(project, msbuildSdksPath, extension, versionString)
                }
            }

            val targetFramework = when {
                versionString.startsWith("3.1") -> "netcoreapp3.1"
                majorVersion >= 5 -> "net${DecimalFormat("#.0", DecimalFormatSymbols(Locale.US)).format(majorVersion)}"
                else -> throw GradleException("""
                    Unsupported target for framework version '${versionString}'.
                    Please make sure that you have a compatible SDK installed on your machine.
                    If not, you can download the recommended SDK here: https://dotnet.microsoft.com/download/dotnet
                """.trimIndent())
            }

            project.logger.info("Use $targetFramework for project parser")

            val tempDir = project.layout.buildDirectory.dir("tmp/dotnet").get()
            Files.createDirectories(tempDir.asFile.toPath())

            project.logger.info("  Extracting parser to {}", tempDir)

            val reflections = Reflections(ConfigurationBuilder().setScanners(ResourcesScanner()).setUrls(ClasspathHelper.forPackage("com.itiviti.parser")))
            reflections.getResources(Pattern.compile(".*\\.cs(proj)?")).forEach {
                Files.copy(DotnetPlugin::class.java.classLoader.getResourceAsStream(it)!!, tempDir.file(File(it).name).asFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            val args = buildExtension.getProperties().toMutableMap()
            args["Configuration"] = extension.configuration
            if (!extension.platform.isNullOrBlank()) {
                args["Platform"] = extension.platform!!
            }
            if (buildExtension.version.get().isNotEmpty()) {
                args["Version"] = buildExtension.version.get()
            }
            if (buildExtension.packageVersion.get().isNotEmpty()) {
                args["PackageVersion"] = buildExtension.packageVersion.get()
            }

            val parser = project.providers.exec { exec ->
                exec.commandLine(extension.dotnetExecutable)
                exec.workingDir(tempDir.asFile)
                exec.args("run")
                if (majorVersion >= 6) {
                    exec.args("--property:FrameworkVersion=${targetFramework}")
                }
                if (extension.msbuildSDKsPath != null) {
                    exec.environment("MSBuildSDKsPath", extension.msbuildSDKsPath)
                }
                exec.args("-f", targetFramework, "--", targetFile, Gson().toJson(args).replace('"', '\''))
                exec.isIgnoreExitValue = true
            }

            parser.result.get().assertNormalExitValue()
            val processOutput = parser.standardOutput.asText.get()
            val result = JsonSlurper().parseText(processOutput.substring(processOutput.indexOf('{'))) as MutableMap<String, Any>

            return result.map { entry -> entry.key to DotnetProject(entry.value as Map<String, Any>) }.toMap()
        }

        private fun evaluateProject(project: Project): Map<String, DotnetProject> {
            val extension = project.extensions.getByType(DotnetPluginExtension::class.java)
            val restoreExtension = (extension as ExtensionAware).extensions.getByType(DotnetRestoreExtension::class.java)
            val buildExtension = (extension as ExtensionAware).extensions.getByType(DotnetBuildExtension::class.java)

            if (!restoreExtension.beforeBuild) {
                project.logger.lifecycle("Start restoring packages")

                if (restore(project, extension, restoreExtension, buildExtension) != 0) {
                    throw GradleException("dotnet restore fails")
                }

                project.logger.lifecycle("Complete restoring packages")
            }

            project.logger.lifecycle("Start parsing project")

            val result = parseProjects(project, extension, buildExtension)

            project.logger.lifecycle("Complete parsing project")

            return result
        }
    }

    override fun apply(project: Project) {
        project.plugins.apply(LifecycleBasePlugin::class.java)

        val extension = project.extensions.create("dotnet", DotnetPluginExtension::class.java, project.name, project.projectDir, { evaluateProject(project) })
        val extensionAware = extension as ExtensionAware
        extensionAware.extensions.create("restore", DotnetRestoreExtension::class.java)
        val buildExtension = extensionAware.extensions.create("build", DotnetBuildExtension::class.java)
        buildExtension.version.convention(project.provider { project.version.toString() })
        buildExtension.packageVersion.convention(project.provider { project.version.toString() })

        val testExtension = extensionAware.extensions.create("test", DotnetTestExtension::class.java, project.layout.buildDirectory.get().asFile)
        (testExtension as ExtensionAware).extensions.create("nunit", DotnetNUnitExtension::class.java, project.layout.buildDirectory.get().asFile)
        extensionAware.extensions.create("nugetPush", DotnetNugetPushExtension::class.java)

        project.afterEvaluate {
            if (extension.preReleaseCheck) {
                it.logger.lifecycle("Check pre-release references")

                val validated = extension.allProjects.values.all { dotnetProject ->
                    val preReleases = dotnetProject.getPackageReferences().filter { ref -> ref.version?.contains("-") == true }
                    if (preReleases.isNotEmpty()) {
                        project.logger.error("Pre-release references detected in ${dotnetProject.getProjectName()}")
                        preReleases.forEach { ref ->
                            project.logger.error("    * ${ref.name}: ${ref.version}")
                        }
                    }
                    preReleases.isEmpty()
                }
                if (!validated) {
                    throw GradleException("Aborting build due to pre-release references detected.")
                }
            }
        }

        val dotnetClean = project.tasks.register("dotnetClean", DotnetCleanTask::class.java) {
            with(it) {
                group = TASK_GROUP
                description = "Cleans the output of a project."
            }
        }
        project.tasks.named(LifecycleBasePlugin.CLEAN_TASK_NAME).configure {
            it.dependsOn(dotnetClean)
        }

        val dotnetBuild = project.tasks.register("dotnetBuild", DotnetBuildTask::class.java) {
            with(it) {
                group = TASK_GROUP
                description = "Builds a project and all of its dependencies."
            }
        }
        project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).configure {
            it.dependsOn(dotnetBuild)
        }

        val dotnetTest = project.tasks.register("dotnetTest", DotnetTestTask::class.java) {
            with(it) {
                group = TASK_GROUP
                description = ".NET test driver used to execute unit tests."
                mustRunAfter(dotnetBuild)
                filter.convention(testExtension.filter)
            }
        }
        project.tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME).configure {
            it.dependsOn(dotnetTest)
        }

        val dotnetNugetPush = project.tasks.register("dotnetNugetPush", DotnetNugetPushTask::class.java) {
            with(it) {
                group = TASK_GROUP
                description = "Push to nuget feed."
                mustRunAfter(dotnetBuild)
            }
        }

        project.plugins.withType(PublishingPlugin::class.java) {
            project.tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME).configure {
                it.dependsOn(dotnetNugetPush)
            }
        }
    }
}
