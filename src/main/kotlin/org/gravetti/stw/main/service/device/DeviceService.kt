package org.gravetti.stw.main.service.device

import com.github.ajalt.clikt.core.PrintMessage
import mu.KotlinLogging
import org.gravetti.stw.main.client.docker.DockerClient
import org.gravetti.stw.main.client.docker.DockerContainerInfo
import org.gravetti.stw.main.client.docker.DockerRunOptions
import org.gravetti.stw.main.service.*
import org.springframework.stereotype.Service
import java.io.File

@Service
class DeviceService(
    private val dockerClient: DockerClient,
    private val userConfig: UserConfig
) {
    private val logger = KotlinLogging.logger {}

    fun findAtomWithParams(containers: List<DockerContainerInfo>,
                           orgId: String, cloudUrl: String): List<DockerContainerInfo> {
        return containers.filter {
            it.name.startsWith("atom_node_")
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
        val atomImage = userConfig.getPropertyWithDefault(ATOM_IMAGE)
        val atomOptions = DockerRunOptions(imageName = userConfig.getDockerImageName(atomImage)).apply {
            execOptions = "--cloudURL=$cloudUrl --orgId=$orgId --nodeId=$nodeId"
            network = "riotcloud_default"
            name = "atom_node_$nodeId"
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
        val imageName = userConfig.getPropertyWithDefault(CSV_ADAPTER_IMAGE)
        val atomIp = atomInfo.ips["riotcloud_default"]
        logger.debug("Running adapter for atom: ${atomInfo.name}, atomIP: $atomIp")
        val fileSizeLimit = userConfig.getPropertyWithDefault(CLOUD_MAX_FILE_SIZE)
        val adapterOptions = DockerRunOptions(imageName = userConfig.getDockerImageName(imageName)).apply {
            this.name = "csv_$nodeId"
            this.ports += port to "45678"
            this.network = "riotcloud_default"
            this.env += "DATALOADER_ATOM_HOST" to atomIp!!
            this.env += "SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE" to fileSizeLimit
            this.env += "SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE" to fileSizeLimit
        }

        dockerClient.run(adapterOptions)
        return dockerClient.list().find { it.name == "csv_$nodeId" }
            ?: throw PrintMessage("Cannot start adapter for node $nodeId")
    }

    fun startDemoAdapter(nodeId: String, atomInfo: DockerContainerInfo, mappingsPath: File) {
        val existingDemoAdapter = dockerClient.list().find { it.name == "atom_demo_$nodeId" }
        if (existingDemoAdapter != null) {
            logger.info("Found demo adapter for node $nodeId")
            return
        }
        val imageName = userConfig.getPropertyWithDefault(DEMO_ADAPTER_IMAGE)
        val atomIp = atomInfo.ips["riotcloud_default"]
        logger.info("Running demo adapter for atom: ${atomInfo.name}, atomIP: $atomIp")
        val adapterOptions = DockerRunOptions(userConfig.getDockerImageName(imageName)).apply {
            env += "ADAPTER_ENGINE_HOST" to atomIp!!
            name = "atom_demo_$nodeId"
            network = "riotcloud_default"
            ports += "8081" to "8080"
            volumes += "${mappingsPath.absolutePath}:/mappings:ro"
        }
        dockerClient.run(adapterOptions)
        dockerClient.list().find { it.name == "atom_demo_$nodeId" }
            ?: throw PrintMessage("Cannot start adapter for node $nodeId")
    }
}
