package com.promo

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()  // НОВАЯ функция
    configureHTTP()          // Твоя существующая
    configureRouting()       // Твоя существующая (но изменим)
}

