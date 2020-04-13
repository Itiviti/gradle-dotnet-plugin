package com.itiviti.tasks

import com.itiviti.extensions.DotnetSonarExtension

open class DotnetInstallSonarTask: BaseExecTask("tool", "install", "--global", "dotnet-sonarscanner") {
    init {
        val version = getNestedExtension(DotnetSonarExtension::class.java).version
        if (version != null) {
            args("--version", version)
        }
    }
}