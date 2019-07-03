package org.github.mamoru1234.stw.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.apache.commons.io.FileUtils
import org.github.mamoru1234.stw.service.StwService
import org.github.mamoru1234.stw.utils.getWorkingDir

class UpCommand(
    private val stwService: StwService
): CliktCommand(name = "up", help = "Start local cloud") {
    private val cloudComposeDir by option(help = "Directory with build cloud compose")
        .file(exists = false, fileOkay = false)
        .defaultLazy { FileUtils.getFile(getWorkingDir(), "cloud-docker-compose") }

    override fun run() {
        stwService.startCloud(cloudComposeDir)
    }
}
