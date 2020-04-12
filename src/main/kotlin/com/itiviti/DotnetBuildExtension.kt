package com.itiviti

open class DotnetBuildExtension {
    /**
     * extra parameters supply to msbuild, such as PackageVersion and ProjectVersion to project.version
     */
    val parameters: MutableMap<String, String> = mutableMapOf()
}