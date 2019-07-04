package org.github.mamoru1234.stw

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.github.mamoru1234.stw.client.csv.CsvAdapterClient
import org.github.mamoru1234.stw.client.docker.DockerClient
import org.github.mamoru1234.stw.command.*
import org.github.mamoru1234.stw.service.CloudComposeService
import org.github.mamoru1234.stw.service.StwService
import org.github.mamoru1234.stw.service.UserConfig
import org.github.mamoru1234.stw.service.device.DeviceService

class Stw: NoRunCliktCommand()

fun main(args: Array<String>) {
    val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val yamlMapper = ObjectMapper(YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerKotlinModule()
    val userConfig = UserConfig()
    val dockerClient = DockerClient(mapper)
    val csvAdapterClient = CsvAdapterClient(mapper)
    val cloudComposeService = CloudComposeService(yamlMapper, userConfig)
    val stwService = StwService(mapper, userConfig, cloudComposeService, dockerClient)
    val deviceService = DeviceService(dockerClient, userConfig)
    val downCommand = DownCommand(stwService)
    val buildCommand = BuildCommand(userConfig, stwService)
    val updateCommand = UpdateCommand(userConfig, stwService)
    val startCommand = StartCommand(userConfig, stwService)
    val csvCommand = CsvCommand(dockerClient, deviceService, csvAdapterClient, userConfig)
    val upCommand = UpCommand(stwService)
    Stw().subcommands(
        downCommand, buildCommand, upCommand,
        updateCommand, startCommand, csvCommand
    ).main(args)
}
