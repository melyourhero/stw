package org.github.mamoru1234.stw.command.device

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import mu.KotlinLogging
import org.github.mamoru1234.stw.client.docker.DockerClient
import org.github.mamoru1234.stw.client.docker.DockerContainerInfo
import org.github.mamoru1234.stw.service.CLOUD_MQTT_URL
import org.github.mamoru1234.stw.service.UserConfig
import org.github.mamoru1234.stw.service.device.DeviceService
import org.github.mamoru1234.stw.utils.nonEmpty
import org.springframework.stereotype.Component
import java.util.*

@Component
class DemoDeviceCommand(
    private val dockerClient: DockerClient,
    private val deviceService: DeviceService,
    private val userConfig: UserConfig
): CliktCommand(name = "demo") {
    private val log = KotlinLogging.logger {}

    private val orgId by option(help = "Organization for atom")
        .default("c2c10fb1-59f6-4766-b207-4427973be4fd")
    private val userNodeId by option(help = "Node id for atom")
        .defaultLazy { UUID.randomUUID().toString() }
    private val cloudUrl by option(help = "Cloud URL to use for atom")
        .defaultLazy { userConfig.readValue(CLOUD_MQTT_URL, "Cloud mqtt bridge url", ::nonEmpty) }
    private val mappingsPath by option("--path", "-p", help = "Path to mappings")
        .file(exists = true, fileOkay = false)
        .required()
    private val forceDevice by option(help = "Force start new device")
        .flag(default = false)

    override fun run() {
        val atomId = ensureAtom()
        val atomInfo = getAtomInfo(atomId)
        deviceService.startDemoAdapter(atomId, atomInfo, mappingsPath)
    }
    private fun ensureAtom(): String {
        if (forceDevice) {
            deviceService.startAtom(orgId, cloudUrl, userNodeId)
            return userNodeId
        }
        val existingAtoms = deviceService.findAtomWithParams(dockerClient.list(), orgId, cloudUrl)
        if (existingAtoms.isNotEmpty()) {
            log.info("Reusing existing atom instance")
            return existingAtoms[0].name.substring(10)
        }
        deviceService.startAtom(orgId, cloudUrl, userNodeId)
        return userNodeId
    }
    private fun getAtomInfo(nodeId: String): DockerContainerInfo {
        return dockerClient.list().find { it.name == "atom_node_$nodeId" }
            ?: throw PrintMessage("Atom with node id: $nodeId not found")
    }
}
