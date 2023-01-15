package net.fischboeck.pragmadb.server.controller

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers

class AdminController: HttpHandler {

    fun handleRoot(): String {

        return """
        {
            "version": "0.1.0-SNAPSHOT"
        }   
        """.trimIndent()
    }

    override fun handleRequest(exchange: HttpServerExchange) {
        exchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/json")
        exchange.responseSender.send(handleRoot())
    }
}