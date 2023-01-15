package net.fischboeck.pragmadb.util

import net.fischboeck.pragmadb.server.PragmaServer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class IntegrationTestExtension: BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private val server = PragmaServer(8081, "localhost")

    override fun beforeAll(p0: ExtensionContext) {

        val uniqueId = this.javaClass.canonicalName!!
        val value = p0.root.getStore(ExtensionContext.Namespace.GLOBAL).get(uniqueId)
        if (value == null) {
            p0.root.getStore(ExtensionContext.Namespace.GLOBAL).put(uniqueId, this)
            server.start()
        }
    }

    override fun close() {
        server.stop()
    }
}