package org.gravetti.stw.main.command

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.gravetti.stw.main.command.device.DemoDeviceCommand
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class DeviceCommand(
    private val csvCommand: CsvCommand,
    private val demoDeviceCommand: DemoDeviceCommand
): NoRunCliktCommand(name = "device") {
    @PostConstruct
    fun init() {
        subcommands(csvCommand, demoDeviceCommand)
    }
}
