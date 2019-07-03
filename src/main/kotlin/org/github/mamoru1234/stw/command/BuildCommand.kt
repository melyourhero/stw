package org.github.mamoru1234.stw.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.github.mamoru1234.stw.ext.convertToFile
import org.github.mamoru1234.stw.service.CLOUD_PATH
import org.github.mamoru1234.stw.service.StwService
import org.github.mamoru1234.stw.service.UserConfig
import org.github.mamoru1234.stw.utils.validateDir

class BuildCommand(
    private val userConfig: UserConfig,
    private val stwService: StwService
): CliktCommand(name = "build", help = "Build cloud sources") {
    private val withClean by option(help = "Should execute clean").flag(default = false)
    private val cloudPath by option(help = "Cloud sources path")
        .file(folderOkay = true, exists = true)
        .defaultLazy {
            val cloudPath = userConfig.readValue(CLOUD_PATH, "Cloud path", ::validateDir)
            return@defaultLazy cloudPath.convertToFile()
        }

    override fun run() {
        stwService.buildCloudSources(cloudPath, withClean)
    }
}
