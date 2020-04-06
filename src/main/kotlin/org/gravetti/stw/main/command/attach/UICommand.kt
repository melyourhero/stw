package org.gravetti.stw.main.command.attach

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import org.gravetti.stw.main.service.StwService
import org.gravetti.stw.main.service.UserConfig
import org.springframework.stereotype.Component

@Component
class UICommand(
    private val userConfig: UserConfig,
    private val stwService: StwService
): CliktCommand(name = "ui", help = "Attach ui to running cloud") {
    private val apiPath by option(help = "Path of cloud api")
        .defaultLazy { "${userConfig.machineIP}:8080" }
    private val uiPath by option(help = "base path for ui")
        .default("/")
    private val uiPort by option(help = "Port to bind UI")
        .default("8082")

    private val containerName by option(help = "Name of docker container")
        .default("riotcloud_ui")

    override fun run() {
        stwService.startUi(apiPath, uiPath, uiPort, containerName)
    }
}
