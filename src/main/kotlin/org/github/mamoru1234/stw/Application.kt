package org.github.mamoru1234.stw

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.github.mamoru1234.stw.client.docker.DockerClient
import org.github.mamoru1234.stw.command.BuildCommand
import org.github.mamoru1234.stw.command.DownCommand
import org.github.mamoru1234.stw.command.UpCommand
import org.github.mamoru1234.stw.service.CloudComposeService
import org.github.mamoru1234.stw.service.StwService
import org.github.mamoru1234.stw.service.UserConfig

class Main: NoRunCliktCommand()

fun main(args: Array<String>) {
    val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val yamlMapper = ObjectMapper(YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerKotlinModule()
    val userConfig = UserConfig()
    val dockerClient = DockerClient(mapper)
    val cloudComposeService = CloudComposeService(yamlMapper, userConfig)
    val stwService = StwService(userConfig, cloudComposeService, dockerClient)
    val downCommand = DownCommand(stwService)
    val buildCommand = BuildCommand(userConfig, stwService)
    val upCommand = UpCommand(stwService)
    Main().subcommands(downCommand, buildCommand, upCommand).main(args)
}
