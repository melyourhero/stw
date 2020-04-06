package org.gravetti.stw.main.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ajalt.clikt.core.PrintMessage
import mu.KotlinLogging
import org.apache.commons.io.FileUtils.*
import org.gravetti.stw.main.client.cloud.getCloudApiClient
import org.gravetti.stw.main.client.docker.DockerClient
import org.gravetti.stw.main.client.docker.DockerRunOptions
import org.gravetti.stw.main.ext.environment
import org.gravetti.stw.main.ext.execRetry
import org.gravetti.stw.main.ext.saveWait
import org.gravetti.stw.main.ext.shellCommand
import org.gravetti.stw.main.utils.nonEmpty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.File

const val RUNNING_STATE = "running"

@Service
class StwService(
    @Qualifier("appJsonMapper")
    private val mapper: ObjectMapper,
    private val userConfig: UserConfig,
    private val cloudComposeService: CloudComposeService,
    private val dockerClient: DockerClient
) {
    private val log = KotlinLogging.logger {}

    fun buildCloudSources(cloudPathFile: File, cloudDockerComposeDstDir: File, withClean: Boolean): File {
        val userName = userConfig.readValue(REGISTRY_USER, "Registry user", ::nonEmpty)
        val userPass = userConfig.readValue(REGISTRY_PASS, "Registry pass", ::nonEmpty)
        val env = mapOf(
            "ORG_GRADLE_PROJECT_registryUsername" to userName,
            "ORG_GRADLE_PROJECT_registryPassword" to userPass,
            "ORG_GRADLE_PROJECT_nexusUsername" to userName,
            "ORG_GRADLE_PROJECT_nexusPassword" to userPass
        )
        if (withClean) {
            shellCommand("./gradlew clean", cloudPathFile, env)
        }
        shellCommand("./gradlew buildDockerImage -PforceDockerBuildImages", cloudPathFile, env)
        shellCommand("./gradlew prepareCloudDockerFiles", cloudPathFile, env)
        val cloudDockerComposeSrcDir = getFile(cloudPathFile, "riot-cloud", "build", "docker-compose")
        if (cloudDockerComposeDstDir.exists()) {
            deleteDirectory(cloudDockerComposeDstDir)
        }
        copyDirectory(cloudDockerComposeSrcDir, cloudDockerComposeDstDir)
        return cloudDockerComposeDstDir
    }

    fun startCloud(cloudDockerComposeSrcDir: File, cloudDockerComposeDstDir: File) {
        if (cloudDockerComposeDstDir.exists()) {
            deleteDirectory(cloudDockerComposeDstDir)
        }
        cloudDockerComposeDstDir.mkdirs()
        if (!cloudDockerComposeSrcDir.exists()) {
            throw PrintMessage("Cloud compose src dir doesn't exists")
        }
        cloudComposeService.prepareComposeDir(cloudDockerComposeSrcDir, cloudDockerComposeDstDir)
        val env = mapOf("PUBLIC_HOST" to userConfig.machineIP)
        val command = "docker-compose -p \"riotcloud\" up -d"
        shellCommand(command, cloudDockerComposeDstDir, env)
        val cloudApiClient = getCloudApiClient("http://localhost:8080", mapper)
        cloudHealthFix()
        cloudApiClient.healthCheck().execRetry(20000)
    }

    fun startUi(apiPath: String, uiPath: String, uiPort: String, name: String) {
        val uiImage = userConfig.getPropertyWithDefault(UI_IMAGE)
        val dockerImageName = userConfig.getDockerImageName(uiImage)
        val runOptions = DockerRunOptions(dockerImageName).apply {
            this.ports += uiPort to "80"
            this.network = "riotcloud_default"
            this.name = name
            this.env += "GRAVETTI_API_PATH" to apiPath
            this.env += "GRAVETTI_PLATFORM_VERSION" to "2.5.0"
            this.env += "GRAVETTI_UI_PATH" to uiPath
        }
        dockerClient.run(runOptions)
    }

    private fun cloudHealthFix() {
        val healthSleep = userConfig.getPropertyWithDefault(CLOUD_HEALTH_SLEEP).toLong()
        log.info("Cloud health fix started")
        while (true) {
            log.debug("Checking cloud health...")
            val cloudFailedContainers = dockerClient.list()
                .filter { it.networkNames.contains("riotcloud_default") && it.state != RUNNING_STATE }
            if (cloudFailedContainers.isEmpty()) {
                break
            }
            dockerClient.restart(cloudFailedContainers)
            log.debug("Waiting next round.")
            Thread.sleep(healthSleep)
        }
        log.debug("Check success")
    }

    fun removeCloud(cloudComposeDir: File) {
        val containers = dockerClient.list()
        dockerClient.removeAll(containers.filter {
            it.name.contains("riotcloud_kafka") || it.networkNames.contains("riotcloud_default")
        })
        if (cloudComposeDir.isDirectory && cloudComposeDir.exists()) {
            log.info("removing cloud with compose")
            val command = "docker-compose -p \"riotcloud\" down -v"
            shellCommand(command, cloudComposeDir, emptyMap())
        }
    }

    fun removeDevices() {
        val containers = dockerClient.list()
        dockerClient.removeAll(containers.filter {
            val targetName = it.name.startsWith("atom_") || it.name.startsWith("csv_")
            targetName && it.networkNames.contains("riotcloud_default")
        })
    }

    fun setBranch(cloudPathFile: File, branchName: String) {
        shellCommand("git fetch", cloudPathFile)
        shellCommand("git checkout $branchName", cloudPathFile)
    }

    fun shellCommand(command: String, path: File, env: Map<String, String> = emptyMap()) {
        ProcessBuilder()
            .environment(env)
            .directory(path)
            .inheritIO()
            .shellCommand(command)
            .start()
            .saveWait()
    }
}
