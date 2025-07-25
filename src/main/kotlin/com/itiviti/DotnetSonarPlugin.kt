package com.itiviti

import com.itiviti.extensions.DotnetNUnitExtension
import com.itiviti.extensions.DotnetPluginExtension
import com.itiviti.extensions.DotnetSonarExtension
import com.itiviti.extensions.DotnetTestExtension
import com.itiviti.tasks.DotnetBuildTask
import com.itiviti.tasks.DotnetInstallSonarTask
import com.itiviti.tasks.DotnetSonarTask
import com.itiviti.tasks.DotnetTestTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.sonarqube.gradle.*

class DotnetSonarPlugin: Plugin<Project> {

    companion object {
        val MANDATORY_ARGS: Map<String, String> = mapOf(
            "k" to "sonar.projectKey",
            "n" to "sonar.projectName",
            "v" to "sonar.projectVersion"
        )
        const val LOGIN_PROPERTY = "sonar.login"

        const val NUNIT_REPORT_PATH = "sonar.cs.nunit.reportsPaths"
        const val OPENCOVER_REPORT_PATH = "sonar.cs.opencover.reportsPaths"

        val IGNORED_PROPERTIES = listOf(
            // is automatically set and cannot be overridden on the command line
            "sonar.working.directory"
        )

        private fun buildArg(key: String, value: Any?): String {
            return "/d:${key}=${value}"
        }
    }

    private val actionBroadcast = ActionBroadcast<SonarProperties>()

    override fun apply(project: Project) {
        val sonarQubeExtension = project.extensions.create(SonarExtension.SONAR_EXTENSION_NAME, SonarExtension::class.java, actionBroadcast)
        // legacy extension kept for plugin compatibility
        project.extensions.create("sonarqube", SonarExtension::class.java, actionBroadcast)

        project.plugins.withType(DotnetPlugin::class.java) {
            (project.extensions.getByType(DotnetPluginExtension::class.java) as ExtensionAware)
                    .extensions.create(SonarExtension.SONAR_EXTENSION_NAME, DotnetSonarExtension::class.java)
        }

        val sonarInstallTask = project.tasks.register("dotnetInstallSonar", DotnetInstallSonarTask::class.java) {
            with(it) {
                group = DotnetPlugin.TASK_GROUP
                description = "Install dotnet-sonarscanner as global tools."
            }
        }

        val sonarTask = project.tasks.register(SonarExtension.SONAR_TASK_NAME) {
            it.dependsOn(sonarInstallTask)
            it.doLast {
                project.logger.info("${SonarExtension.SONAR_TASK_NAME} task was invoked, enable dotnetSonar")
                project.tasks.withType(DotnetSonarTask::class.java).configureEach { sonarDotnetTask ->
                    sonarDotnetTask.enabled = true
                }
            }
        }
        val sonarDotnetTask = project.tasks.register("dotnetSonar", DotnetSonarTask::class.java) {
            with(it) {
                group = DotnetPlugin.TASK_GROUP
                description = "Run sonarqube analysis."
                enabled = false

                mustRunAfter(sonarInstallTask)
                mustRunAfter(sonarTask)

                doLast {
                    // Clean .sonarqube
                    project.delete(project.projectDir.resolve(".sonarqube"))
                }
            }
        }

        project.tasks.withType(DotnetBuildTask::class.java).configureEach { task -> task.finalizedBy(sonarDotnetTask) }
        project.tasks.withType(DotnetTestTask::class.java).configureEach { task -> task.finalizedBy(sonarDotnetTask) }

        project.tasks.withType(DotnetBuildTask::class.java).configureEach { task ->
            task.mustRunAfter(sonarTask)

            task.doFirst {
                val extension = project.extensions.getByType(DotnetPluginExtension::class.java)
                val sonarQubeProperties = computeSonarProperties(project)
                if (sonarQubeProperties.containsKey(LOGIN_PROPERTY)) {
                    sonarDotnetTask.get().args(buildArg(LOGIN_PROPERTY, sonarQubeProperties[LOGIN_PROPERTY]))
                }

                // sonarqube is in task graph and executed
                if (sonarDotnetTask.get().enabled) {

                    setupReportPath(sonarQubeExtension, extension)
                    project.providers.exec { exec ->
                        exec.workingDir = extension.workingDir
                        exec.commandLine(project.layout.buildDirectory.dir(DotnetSonarExtension.toolPath).get().asFile.resolve("dotnet-sonarscanner"))
                        exec.args("begin")

                        buildArgs(sonarQubeProperties).forEach {
                            exec.args(it)
                        }
                    }.result.get()
                }
            }
        }
    }

    private fun setupReportPath(sonarQube: SonarExtension, extension: DotnetPluginExtension) {
        val testExtension = (extension as ExtensionAware).extensions.getByType(DotnetTestExtension::class.java)
        val nunitExtension = (testExtension as ExtensionAware).extensions.getByType(DotnetNUnitExtension::class.java)

        // Nunit report
        sonarQube.properties {
            it.property(NUNIT_REPORT_PATH, nunitExtension.testOutputXml.resolve("*.xml").absolutePath)
        }

        // Opencover report via coverlet
        if (testExtension.collectCoverage && testExtension.coverletOutputFormat.contains("opencover")) {
            sonarQube.properties {
                it.property(OPENCOVER_REPORT_PATH, testExtension.coverletOutput.resolve("*.xml").absolutePath)
            }
        }
    }

    private fun buildArgs(properties: Map<String, Any?>): List<String> {
        val mandatoryArgs = MANDATORY_ARGS.map {
            "/${it.key}:${properties[it.value]}"
        }

        val otherArgs = properties.filter {
            !MANDATORY_ARGS.values.contains(it.key) && !IGNORED_PROPERTIES.contains(it.key) && !it.value?.toString().isNullOrEmpty()
        }.map { buildArg(it.key, it.value) }

        return mandatoryArgs + otherArgs
    }

    private fun computeSonarProperties(project: Project): Map<String, Any?> {
        val actionBroadcastMap = mutableMapOf<String, ActionBroadcast<SonarProperties>>()
        actionBroadcastMap[project.path] = actionBroadcast
        val propertyComputer = SonarPropertyComputer(actionBroadcastMap, project)
        return propertyComputer.computeSonarProperties()
    }
}