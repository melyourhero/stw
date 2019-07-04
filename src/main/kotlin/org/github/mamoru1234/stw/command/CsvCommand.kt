package org.github.mamoru1234.stw.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import mu.KotlinLogging
import org.apache.commons.io.FileUtils.getFile
import org.github.mamoru1234.stw.client.csv.CsvAdapterClient
import org.github.mamoru1234.stw.client.docker.DockerClient
import org.github.mamoru1234.stw.client.docker.DockerContainerInfo
import org.github.mamoru1234.stw.ext.convertToFile
import org.github.mamoru1234.stw.ext.getRandomPort
import org.github.mamoru1234.stw.service.CLOUD_MQTT_URL
import org.github.mamoru1234.stw.service.CSV_ADAPTER_DATA_PATH
import org.github.mamoru1234.stw.service.UserConfig
import org.github.mamoru1234.stw.service.device.DeviceService
import org.github.mamoru1234.stw.utils.getWorkingDir
import org.github.mamoru1234.stw.utils.nonEmpty
import java.util.*

class CsvCommand(
    private val dockerClient: DockerClient,
    private val deviceService: DeviceService,
    private val csvAdapterClient: CsvAdapterClient,
    private val userConfig: UserConfig
): CliktCommand(name = "csv", help = "Attach device with csv adapter") {
    private val logger = KotlinLogging.logger {}

    private val orgId by option(help = "Organization for atom")
        .default("c2c10fb1-59f6-4766-b207-4427973be4fd")
    private val userNodeId by option(help = "Node id for atom")
        .defaultLazy { UUID.randomUUID().toString() }
    private val forceDevice by option(help = "Force start new device")
        .flag(default = false)
    private val cloudUrl by option()
        .defaultLazy { userConfig.readValue(CLOUD_MQTT_URL, "Cloud mqtt bridge url", ::nonEmpty) }
    private val adapterPort by option(help = "Adapter port to use")
        .defaultLazy { getRandomPort().toString() }
    private val deviceName by option(help = "device name")
        .defaultLazy { UUID.randomUUID().toString() }
    private val deviceId by option(help = "device id")
        .defaultLazy { UUID.randomUUID().toString() }
    private val userDataPath by option("--data", "-d", help = "Path to user data")
        .file(exists = true, folderOkay = false)
        .defaultLazy {
            userConfig.getProperty(CSV_ADAPTER_DATA_PATH)?.convertToFile()
                ?: getFile(getWorkingDir(), "data.csv")
        }
    private val delay by option(help = "Delay for data emit").int().default(1000)

    override fun run() {
        val atomNodeId = ensureAtom()
        val atomInfo = getAtomInfo(atomNodeId)
        val csvAdapterInfo = deviceService.startCsvAdapter(atomNodeId, atomInfo, port = adapterPort)
        val adapterUrl = "http://localhost:${csvAdapterInfo.ports[0].first}"
        if (!userDataPath.exists() || !userDataPath.isFile) {
            throw PrintMessage("Invalid user data path ${userDataPath.absolutePath}")
        }
        csvAdapterClient.attachDevice(
            deviceName = deviceName,
            adapterUrl = adapterUrl,
            deviceId = deviceId,
            data = userDataPath,
            delay = delay
        )
    }

    private fun getAtomInfo(nodeId: String): DockerContainerInfo {
        return dockerClient.list().find { it.name == "atom_$nodeId" }
            ?: throw PrintMessage("Atom with node id: $nodeId not found")
    }

    private fun ensureAtom(): String {
        if (forceDevice) {
            deviceService.startAtom(orgId, cloudUrl, userNodeId)
            return userNodeId
        }
        val existingAtoms = deviceService.findAtomWithParams(dockerClient.list(), orgId, cloudUrl)
        if (existingAtoms.isNotEmpty()) {
            logger.info("Reusing existing atom instance")
            return existingAtoms[0].name.substring(5)
        }
        deviceService.startAtom(orgId, cloudUrl, userNodeId)
        return userNodeId
    }
}
