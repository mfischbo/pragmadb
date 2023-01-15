package net.fischboeck.pragmadb.server

import io.undertow.Undertow

class PragmaServer(
    private val port: Int,
    private val host: String,
) {

    private val server: Undertow = Undertow.builder()
        .addHttpListener(port, host)
        .setHandler {
            if (it.isInIoThread) {
                it.dispatch(HttpDispatcher(it))
            }
        }
        .build()

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }
}