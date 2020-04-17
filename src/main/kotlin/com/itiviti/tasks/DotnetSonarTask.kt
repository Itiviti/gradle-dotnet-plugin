package com.itiviti.tasks

import com.itiviti.extensions.DotnetSonarExtension

open class DotnetSonarTask: BaseExecTask("sonarscanner", "end") {
    init {
        args("--tool-path", getNestedExtension(DotnetSonarExtension::class.java).toolPath.absolutePath)
    }
}