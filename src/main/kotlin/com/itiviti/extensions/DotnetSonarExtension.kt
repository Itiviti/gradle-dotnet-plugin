package com.itiviti.extensions

import java.io.File

open class DotnetSonarExtension(buildDir: File) {
    var version: String? = null

    var toolPath: File = buildDir.resolve("dotnet/tool")
}