package org.github.mamoru1234.stw

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.github.mamoru1234.stw.client.docker.DockerClient
import org.github.mamoru1234.stw.command.DownCommand
import org.github.mamoru1234.stw.service.StwService

class Main: NoRunCliktCommand()

fun main(args: Array<String>) {
    val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val dockerClient = DockerClient(mapper)
    val stwService = StwService(dockerClient)
    Main().subcommands(DownCommand(stwService)).main(args)
}
