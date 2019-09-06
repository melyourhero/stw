package org.github.mamoru1234.stw.command

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.github.mamoru1234.stw.command.attach.UICommand
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class AttachCommand(
    private val uiCommand: UICommand
): NoRunCliktCommand(name = "attach", help = "Attach cloud component") {
    @PostConstruct
    fun init() {
        subcommands(uiCommand)
    }
}
