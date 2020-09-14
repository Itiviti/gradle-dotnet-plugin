package com.itiviti.tasks

import com.itiviti.extensions.DotnetSonarExtension
import org.gradle.api.tasks.Exec
import org.sonarqube.gradle.SonarQubeExtension

open class DotnetSonarTask: Exec() {
    init {
        commandLine(project.buildDir.resolve(DotnetSonarExtension.toolPath).resolve("dotnet-sonarscanner"))
        args("end")

        val sonarQubeExtension = project.extensions.getByType(SonarQubeExtension::class.java)
        sonarQubeExtension.properties {  }
    }
}