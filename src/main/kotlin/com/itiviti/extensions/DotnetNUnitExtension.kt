package com.itiviti.extensions

import java.io.File

/**
 * https://github.com/nunit/docs/wiki/Tips-And-Tricks
 */
open class DotnetNUnitExtension(buildDir: File) {
    /**
     * To determine how parallelization should be performed
     */
    var numberOfTestWorkers: Int = -1

    /**
     * Output report xml destination, default to build/reports/nunit
     */
    var testOutputXml: File = buildDir.resolve("reports/nunit")

    /**
     * Stops on first error
     */
    var stopOnError: Boolean = false

    /**
     * NUnit Filter expression
     */
    var where: String = ""
}