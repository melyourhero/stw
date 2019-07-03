package org.github.mamoru1234.stw.service

import com.github.ajalt.clikt.core.PrintMessage
import mu.KotlinLogging
import org.apache.commons.io.FileUtils.*
import org.github.mamoru1234.stw.client.docker.DockerClient
import org.github.mamoru1234.stw.ext.environment
import org.github.mamoru1234.stw.ext.saveWait
import org.github.mamoru1234.stw.ext.shellCommand
import org.github.mamoru1234.stw.utils.getWorkingDir
import org.github.mamoru1234.stw.utils.nonEmpty
import java.io.File

class StwService(
    private val userConfig: UserConfig,
    private val cloudComposeService: CloudComposeService,
    private val dockerClient: DockerClient
    ) {
    private val log = KotlinLogging.logger {}

    fun buildCloudSources(cloudPathFile: File, withClean: Boolean): File {
        val userName = userConfig.readValue(REGISTRY_USER, "Registry user", ::nonEmpty)
        val userPass = userConfig.readValue(REGISTRY_PASS, "Registry pass", ::nonEmpty)
        val env = mapOf(
            "ORG_GRADLE_PROJECT_registry.username" to userName,
            "ORG_GRADLE_PROJECT_registry.password" to userPass,
            "ORG_GRADLE_PROJECT_nexusUsername" to userName,
            "ORG_GRADLE_PROJECT_nexusPassword" to userPass
        )
        if (withClean) {
            shellCommand("./gradlew clean", cloudPathFile, env)
        }
        shellCommand("./gradlew buildDockerImage -PforceDockerBuildImages", cloudPathFile, env)
        shellCommand("./gradlew prepareCloudDockerFiles", cloudPathFile, env)
        val cloudDockerComposeSrcDir = getFile(cloudPathFile, "riot-cloud", "build", "docker-compose")
        val cloudDockerComposeDstDir = getFile(getWorkingDir(), "cloud-docker-compose")
        if (cloudDockerComposeDstDir.isDirectory) {
            deleteDirectory(cloudDockerComposeDstDir)
        }
        copyDirectory(cloudDockerComposeSrcDir, cloudDockerComposeDstDir)
        return cloudDockerComposeDstDir
    }

    fun startCloud() {
        val cloudDockerComposeSrcDir = getFile(getWorkingDir(), "cloud-docker-compose")
        val cloudDockerComposeDstDir = getFile(getWorkingDir(), "stw-compose")
        cloudDockerComposeDstDir.mkdirs()
        if (!cloudDockerComposeSrcDir.exists()) {
            throw PrintMessage("Cloud compose src dir doesn't exists")
        }
        cloudComposeService.prepareComposeDir(cloudDockerComposeSrcDir, cloudDockerComposeDstDir)
        val env = mapOf("PUBLIC_HOST" to userConfig.machineIP)
        val command = "docker-compose -p \"riotcloud\" up -d"
        shellCommand(command, cloudDockerComposeDstDir, env)
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

    private fun shellCommand(command: String, path: File, env: Map<String, String> = emptyMap()) {
        ProcessBuilder()
            .environment(env)
            .directory(path)
            .inheritIO()
            .shellCommand(command)
            .start()
            .saveWait()
    }
}