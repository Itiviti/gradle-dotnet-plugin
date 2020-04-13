package com.itiviti.extensions

open class DotnetNugetPushExtension {
    /**
     * The API key for the server.
     */
    var apiKey: String? = null

    /**
     * Specifies the server URL. This option is required unless DefaultPushSource config value is set in the NuGet config file.
     */
    var source: String? = null
}