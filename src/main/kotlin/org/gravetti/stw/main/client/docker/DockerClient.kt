package org.gravetti.stw.main.client.docker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.gravetti.stw.main.ext.commandString
import org.gravetti.stw.main.ext.saveWait
import org.gravetti.stw.main.ext.shellCommand
import org.gravetti.stw.main.ext.waitForString
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class DockerClient(
    @Qualifier("appJsonMapper")
    private val objectMapper: ObjectMapper
) {
    fun pull(imageName: String) {
        val command = "docker pull $imageName"
        runCommand(command)
    }

    fun run(options: DockerRunOptions) {
        var command = "docker run"

        if (options.detached) {
            command += " -d"
        }

        if (options.name != null) {
            command += " --name=${options.name}"
        }

        options.ports.forEach {
            (hostPort, imagePort) ->
            command += " -p $hostPort:$imagePort"
        }

        if (options.network != null) {
            command += " --network=${options.network}"
        }

        options.volumes.forEach {
            volume ->
            command += " -v $volume"
        }

        options.env.forEach {
            (envKey, envValue) ->
            command += " -e $envKey=$envValue"
        }

        command += " ${options.imageName}"

        if (options.execOptions != null) {
            command += " ${options.execOptions}"
        }

        runCommand(command)
    }

    fun list(): List<DockerContainerInfo> {
        val psString = ProcessBuilder()
                .commandString("docker ps -aq")
                .start()
                .waitForString()

        if (psString.isBlank()) {
            return emptyList()
        }

        val process = ProcessBuilder()
                .commandString("docker inspect $psString")
                .start()

        return objectMapper.readValue(process.inputStream)
    }

    fun remove(filter: String) {
        runCommand("docker rm -fv $filter")
    }

    fun remove(info: DockerContainerInfo) {
        runCommand("docker rm -fv ${info.id}")
    }

    fun restart(infos: List<DockerContainerInfo>) {
        val ids = infos.map { it.id }.joinToString(" ")
        runCommand("docker restart $ids")
    }

    fun removeAll(infoList: List<DockerContainerInfo>) {
        if (infoList.isEmpty()) return
        val ids = infoList.map { it.id }.joinToString(" ")
        runCommand("docker rm -fv $ids", safe = false)
    }

    private fun runCommand(command: String, safe: Boolean = true) {
        ProcessBuilder()
                .shellCommand(command)
                .inheritIO()
                .start()
                .let {
                    if (safe) it.saveWait() else it.waitFor()
                }
    }
}
