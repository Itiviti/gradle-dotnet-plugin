package com.itiviti.extensions

import java.io.File

/**
 * https://docs.microsoft.com/en-us/dotnet/core/tools/dotnet-test
 */
open class DotnetTestExtension(buildDir: File) {
    /**
     * Filters out tests in the current project using the given expression.
     * http://blog.prokrams.com/2019/12/16/nunit3-filter-dotnet/
     */
    var filter: String? = null

    /**
     * The .runsettings file to use for running the tests
     */
    var settings: File? = null

    /**
     * Enable coverlet report
     */
    var collectCoverage: Boolean = true

    /**
     * Coverlet report destination
     */
    var coverletOutput: File = buildDir.resolve("reports/coverlet/")

    /**
     * Coverlet output format
     */
    var coverletOutputFormat: String = "opencover"

    /**
     * Coverlet exclude files, default to **\*.designer.cs;**\*.xaml.cs;**\*.g.cs
     */
    var coverletExcludeFiles: String = "**/*.designer.cs;**/*.xaml.cs;**/*.g.cs"

    /**
     * Ignore dotnet test exit code or not, default to false
     */
    var ignoreExitValue = false
}