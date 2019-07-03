package org.github.mamoru1234.stw.command

import com.github.ajalt.clikt.core.CliktCommand
import org.github.mamoru1234.stw.service.StwService

class UpCommand(
    private val stwService: StwService
): CliktCommand(name = "up", help = "Start local cloud") {
    override fun run() {
        stwService.startCloud()
    }
}
