package com.itiviti.tasks

import com.itiviti.extensions.DotnetPluginExtension
import com.itiviti.extensions.DotnetSonarExtension
import org.gradle.api.tasks.Exec

open class DotnetSonarTask: Exec() {
    init {
        commandLine(project.buildDir.resolve(DotnetSonarExtension.toolPath).resolve("dotnet-sonarscanner"))
        args("end")

        val pluginExtension = project.extensions.getByType(DotnetPluginExtension::class.java)
        workingDir = pluginExtension.workingDir
    }
}