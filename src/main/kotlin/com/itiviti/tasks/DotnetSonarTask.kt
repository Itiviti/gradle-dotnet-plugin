package com.itiviti.tasks

import com.itiviti.DotnetSonarPlugin
import com.itiviti.extensions.DotnetSonarExtension
import org.gradle.api.tasks.Exec

open class DotnetSonarTask: Exec() {
    init {
        commandLine(project.buildDir.resolve(DotnetSonarExtension.toolPath).resolve("dotnet-sonarscanner"))

        val extension = project.extensions.getByType(DotnetSonarPlugin::class.java)
        val props = extension.computeSonarProperties(project)
        val endProps = props.filter { it.key == "sonar.login" }
        extension.buildArgs(endProps).forEach { args(it) }

        args("end")
    }
}