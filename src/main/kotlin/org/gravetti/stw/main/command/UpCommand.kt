package org.gravetti.stw.main.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.apache.commons.io.FileUtils
import org.gravetti.stw.main.service.StwService
import org.gravetti.stw.main.utils.getWorkingDir
import org.springframework.stereotype.Component

@Component
class UpCommand(
    private val stwService: StwService
): CliktCommand(name = "up", help = "Start local cloud") {
    private val cloudComposeBuildDir by option(help = "Directory with build cloud compose")
        .file(exists = false, fileOkay = false)
        .defaultLazy { FileUtils.getFile(getWorkingDir(), "cloud-docker-compose") }

    private val cloudComposeDir by option(help = "Directory with processed cloud compose")
        .file(exists = false, fileOkay = false)
        .defaultLazy { FileUtils.getFile(getWorkingDir(), "stw-compose") }

    override fun run() {
        stwService.startCloud(cloudComposeBuildDir, cloudComposeDir)
    }
}
