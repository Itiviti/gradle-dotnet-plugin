package com.itiviti.tasks

import com.itiviti.extensions.DotnetRestoreExtension
import com.itiviti.extensions.DotnetSonarExtension

open class DotnetInstallSonarTask: BaseExecTask("tool", "update", "dotnet-sonarscanner") {
    init {
        val extension = getNestedExtension(DotnetSonarExtension::class.java)

        val version = extension.version
        if (version != null) {
            args("--version", version)
        }

        val restoreExtension = getNestedExtension(DotnetRestoreExtension::class.java)
        restoreExtension.source.forEach {
            args("--add-source", it)
        }
        args("--tool-path", project.layout.buildDirectory.dir(DotnetSonarExtension.toolPath).get().asFile)
    }
}