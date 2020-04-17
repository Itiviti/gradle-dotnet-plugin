package com.itiviti.tasks

import com.itiviti.extensions.DotnetSonarExtension

open class DotnetInstallSonarTask: BaseExecTask("tool", "update", "dotnet-sonarscanner") {
    init {
        val extension = getNestedExtension(DotnetSonarExtension::class.java)

        val version = extension.version
        if (version != null) {
            args("--version", version)
        }

        args("--tool-path", extension.toolPath.absolutePath)
    }
}