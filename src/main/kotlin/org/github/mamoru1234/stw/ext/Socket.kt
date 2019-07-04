package org.github.mamoru1234.stw.ext

import java.net.ServerSocket

fun getRandomPort(): Int {
    val serverSocket = ServerSocket(0)
    val port = serverSocket.localPort
    serverSocket.close()
    return port
}
