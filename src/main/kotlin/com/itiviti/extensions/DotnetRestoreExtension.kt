package com.itiviti.extensions

import javax.print.DocFlavor

open class DotnetRestoreExtension {
    /**
    * Config file for custom packages
    */
    var configFile = ""
    /**
     * Specifies to not cache packages and HTTP requests.
     */
    var noCache = false

    /**
     * Forces all dependencies to be resolved even if the last restore was successful. Specifying this flag is the same as deleting the project.assets.json file.
     */
    var force = false

    /**
     * Specifies a NuGet package source to use during the restore operation. This setting overrides all of the sources specified in the nuget.config files.
     */
    var source: MutableList<String> = mutableListOf()

    /**
     * Delay dotnet restore until dotnetBuild rather than in evaluation phase, could lead to missing properties due to missing dependencies
     */
    var beforeBuild = false
}
