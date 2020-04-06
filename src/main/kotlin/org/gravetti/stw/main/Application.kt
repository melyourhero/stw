package org.gravetti.stw.main

import org.gravetti.stw.main.command.StwCommand

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val context = SpringApplication.run(Application::class.java, *args)
    val stwCommand = context.getBean(StwCommand::class.java)
    stwCommand.main(args)
}
