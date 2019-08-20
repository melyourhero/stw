package org.github.mamoru1234.stw.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import mu.KotlinLogging
import org.github.mamoru1234.stw.ext.convertToFile
import org.github.mamoru1234.stw.service.CLOUD_PATH
import org.github.mamoru1234.stw.service.StwService
import org.github.mamoru1234.stw.service.UserConfig
import org.github.mamoru1234.stw.utils.validateDir

class UpdateCommand(
    private val userConfig: UserConfig,
    private val stwService: StwService
): CliktCommand(name = "update", help = "Update env sources") {
    private val log = KotlinLogging.logger {}

    private val branch by option(help = "Target branch").default("master")

    private val cloudPath by option(help = "Cloud sources path")
        .file(folderOkay = true, exists = true)
        .defaultLazy {
            val cloudPath = userConfig.readValue(CLOUD_PATH, "Cloud path", ::validateDir)
            return@defaultLazy cloudPath.convertToFile()
        }

    override fun run() {
        log.info("Updating sources")
        stwService.setBranch(cloudPath, branch)
        stwService.shellCommand("git pull -r", cloudPath)
    }
}
