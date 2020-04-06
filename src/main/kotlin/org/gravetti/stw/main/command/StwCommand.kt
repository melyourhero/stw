package org.gravetti.stw.main.command

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class StwCommand(
    private val attachCommand: AttachCommand,
    private val buildCommand: BuildCommand,
    private val downCommand: DownCommand,
    private val startCommand: StartCommand,
    private val upCommand: UpCommand,
    private val updateCommand: UpdateCommand,
    private val deviceCommand: DeviceCommand
): NoRunCliktCommand() {
    @PostConstruct
    fun init() {
        subcommands(attachCommand, buildCommand, downCommand, startCommand, upCommand, updateCommand, deviceCommand)
    }
}
