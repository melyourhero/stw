package org.github.mamoru1234.stw

import org.github.mamoru1234.stw.command.StwCommand
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val context = SpringApplication.run(Application::class.java, *args)
    val stwCommand = context.getBean(StwCommand::class.java)
    stwCommand.main(args)
}
