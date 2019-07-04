package org.github.mamoru1234.stw.service.device

import com.github.ajalt.clikt.core.PrintMessage
import mu.KotlinLogging
import org.github.mamoru1234.stw.client.docker.DockerClient
import org.github.mamoru1234.stw.client.docker.DockerContainerInfo
import org.github.mamoru1234.stw.client.docker.DockerRunOptions
import org.github.mamoru1234.stw.service.ATOM_IMAGE
import org.github.mamoru1234.stw.service.CLOUD_MAX_FILE_SIZE
import org.github.mamoru1234.stw.service.CSV_ADAPTER_IMAGE
import org.github.mamoru1234.stw.service.UserConfig
import org.github.mamoru1234.stw.utils.nonEmpty

class DeviceService(
    private val dockerClient: DockerClient,
    private val userConfig: UserConfig
) {
    private val logger = KotlinLogging.logger {}

    fun findAtomWithParams(containers: List<DockerContainerInfo>,
                           orgId: String, cloudUrl: String): List<DockerContainerInfo> {
        return containers.filter {
            it.name.startsWith("atom_")
                    && it.networkNames.contains("riotcloud_default")
                    && it.args.contains("--cloudURL=$cloudUrl")
                    && it.args.contains("--orgId=$orgId")
        }
    }

    fun findAtomWithParams(containers: List<DockerContainerInfo>,
                           orgId: String, cloudUrl: String, nodeId: String): List<DockerContainerInfo> {
        val filteredContainers = findAtomWithParams(containers, orgId, cloudUrl)
        return filteredContainers.filter { it.args.contains("--nodeId=$nodeId") }
    }

    fun startAtom(orgId: String, cloudUrl: String, nodeId: String) {
        val sameAtoms = findAtomWithParams(dockerClient.list(), orgId, cloudUrl, nodeId)
        if (sameAtoms.isNotEmpty()) {
            throw PrintMessage("Atom with same params is already running")
        }
        val atomImage = userConfig.readValue(ATOM_IMAGE, "Enter atom image", ::nonEmpty)
        val atomOptions = DockerRunOptions(imageName = atomImage).apply {
            execOptions = "--cloudURL=$cloudUrl --orgId=$orgId --nodeId=$nodeId"
            network = "riotcloud_default"
            name = "atom_$nodeId"
        }
        dockerClient.run(atomOptions)
    }

    fun startCsvAdapter(nodeId: String,
                        atomInfo: DockerContainerInfo, port: String): DockerContainerInfo {
        val existingCsvAdapter = dockerClient.list().find { it.name == "csv_$nodeId" }
        if (existingCsvAdapter != null) {
            logger.info("Found csv adapter for node $nodeId")
            return existingCsvAdapter
        }
        val imageName = userConfig.readValue(CSV_ADAPTER_IMAGE, "Csv adapter image", ::nonEmpty)
        val atomIp = atomInfo.ips["riotcloud_default"]
        logger.debug("Runnning adapter for atom: ${atomInfo.name}, atomIP: $atomIp")
        val fileSizeLimit = userConfig.getProperty(CLOUD_MAX_FILE_SIZE, "100MB")
        val adapterPort = port
        val adapterOptions = DockerRunOptions(imageName).apply {
            this.name = "csv_$nodeId"
            this.ports += adapterPort to "45678"
            this.network = "riotcloud_default"
            this.env += "DATALOADER_ATOM_HOST" to atomIp!!
            this.env += "SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE" to fileSizeLimit
            this.env += "SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE" to fileSizeLimit
        }

        dockerClient.run(adapterOptions)
        return dockerClient.list().find { it.name == "csv_$nodeId" }
            ?: throw PrintMessage("Cannot start adapter for node $nodeId")
    }
}
