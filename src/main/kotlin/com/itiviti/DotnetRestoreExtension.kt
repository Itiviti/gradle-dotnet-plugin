package com.itiviti

open class DotnetRestoreExtension {
    /**
     * Specifies to not cache packages and HTTP requests.
     */
    var noCache = true

    /**
     * Forces all dependencies to be resolved even if the last restore was successful. Specifying this flag is the same as deleting the project.assets.json file.
     */
    var force = false
}