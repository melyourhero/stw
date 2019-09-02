package org.github.mamoru1234.stw.command

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class StwCommand(
    private val buildCommand: BuildCommand,
    private val csvCommand: CsvCommand,
    private val downCommand: DownCommand,
    private val startCommand: StartCommand,
    private val upCommand: UpCommand,
    private val updateCommand: UpdateCommand
): NoRunCliktCommand() {
    @PostConstruct
    fun init() {
        subcommands(buildCommand, csvCommand, downCommand, startCommand, upCommand, updateCommand)
    }
}
