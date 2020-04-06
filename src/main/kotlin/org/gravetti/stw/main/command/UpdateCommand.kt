package org.gravetti.stw.main.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import mu.KotlinLogging
import org.gravetti.stw.main.ext.convertToFile
import org.gravetti.stw.main.service.CLOUD_PATH
import org.gravetti.stw.main.service.StwService
import org.gravetti.stw.main.service.UserConfig
import org.gravetti.stw.main.utils.validateDir
import org.springframework.stereotype.Component

@Component
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
