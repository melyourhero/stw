package org.github.mamoru1234.stw.service

import mu.KotlinLogging
import org.github.mamoru1234.stw.client.docker.DockerClient
import org.github.mamoru1234.stw.ext.environment
import org.github.mamoru1234.stw.ext.saveWait
import org.github.mamoru1234.stw.ext.shellCommand
import java.io.File

class StwService(
    private val dockerClient: DockerClient
    ) {
    private val log = KotlinLogging.logger {}

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
