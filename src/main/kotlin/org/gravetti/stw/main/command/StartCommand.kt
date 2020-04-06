package org.gravetti.stw.main.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.apache.commons.io.FileUtils
import org.gravetti.stw.main.ext.convertToFile
import org.gravetti.stw.main.service.CLOUD_PATH
import org.gravetti.stw.main.service.StwService
import org.gravetti.stw.main.service.UserConfig
import org.gravetti.stw.main.utils.getWorkingDir
import org.gravetti.stw.main.utils.validateDir
import org.springframework.stereotype.Component

@Component
class StartCommand(
    private val userConfig: UserConfig,
    private val stwService: StwService
): CliktCommand(name = "start", help = "Start cloud env") {
    private val cloudPath by option(help = "Cloud sources path")
        .file(folderOkay = true, exists = true)
        .defaultLazy {
            val cloudPath = userConfig.readValue(CLOUD_PATH, "Cloud path", ::validateDir)
            return@defaultLazy cloudPath.convertToFile()
        }

    private val cloudComposeBuildDir by option(help = "Directory with build cloud compose")
        .file(exists = false, fileOkay = false)
        .defaultLazy { FileUtils.getFile(getWorkingDir(), "cloud-docker-compose") }

    private val cloudComposeDir by option(help = "Directory with processed cloud compose")
        .file(exists = false, fileOkay = false)
        .defaultLazy { FileUtils.getFile(getWorkingDir(), "stw-compose") }

    private val skipBuild by option(help = "Skip build from sources step").flag(default = false)
    private val withClean by option(help = "With clean step").flag(default = false)

    override fun run() {
        stwService.removeCloud(cloudComposeDir)
        if (!skipBuild) {
            stwService.buildCloudSources(cloudPath, cloudComposeBuildDir, withClean)
        }
        stwService.startCloud(cloudComposeBuildDir, cloudComposeDir)
    }
}
