package com.itiviti.extensions

import java.io.File

open class DotnetSonarExtension(gradleUserHomeDir: File) {
    var version: String? = null

    var toolPath: File = gradleUserHomeDir.resolve("/caches/dotnet")
}