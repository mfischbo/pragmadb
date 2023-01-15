package net.fischboeck.pragmadb

import net.fischboeck.pragmadb.core.model.Document
import net.fischboeck.pragmadb.core.model.MediaType
import net.fischboeck.pragmadb.core.model.command.InsertCommand
import net.fischboeck.pragmadb.core.service.StorageEngineRegistry
import net.fischboeck.pragmadb.server.PragmaServer
import net.fischboeck.pragmadb.server.config.HttpBeans

fun main(args: Array<String>) {

    // This starts the "application context" and registers all beans
    HttpBeans.register()


    val value = """
    {
        "name": "Alfred",
        "age": 24
    }
    """.trimIndent()

    val value2 = """
    {
       "name": "Alfred",
       "age": 25
    }
    """

    // for testing purpose store 2 documents in a test collection
    val engine = StorageEngineRegistry.createCollection("test")
    val document = Document(content = value.toByteArray())
    val doc2 = Document(content = "This is just some plain text".toByteArray(), mediaType = MediaType.TEXT_PLAIN)
    val doc3 = document.withContent(value2.toByteArray())

    engine.accept(listOf(
        InsertCommand(document),
        InsertCommand(doc2),
        InsertCommand(doc3)
    ))

    val server = PragmaServer(8081, "localhost")
    server.start()
}