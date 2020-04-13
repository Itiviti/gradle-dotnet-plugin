package com.itiviti.tasks

import com.itiviti.extensions.DotnetBuildExtension
import com.itiviti.extensions.DotnetNugetPushExtension

open class DotnetNugetPushTask: BaseExecTask("nuget", "push") {
    init {
        val pluginExtension = getPluginExtension()
        val nugetPushExtension = getNestedExtension(DotnetNugetPushExtension::class.java)
        val nugetPackage = pluginExtension.getMainProject().getPackageOutputPath()

        if (nugetPackage == null) {
            enabled = false
        } else {
            args(nugetPackage.resolve("${pluginExtension.getMainProject().getProperty("PackageId")}.${pluginExtension.getMainProject().getProperty("PackageVersion")}.nupkg"))
            val apiKey = nugetPushExtension.apiKey
            if (apiKey != null) {
                args("--api-key", apiKey)
            }
            val source = nugetPushExtension.source
            if (source != null) {
                args("--source", source)
            }
        }
    }
}