package net.fischboeck.pragmadb.server.controller

import io.undertow.util.Headers
import io.undertow.util.StatusCodes
import net.fischboeck.pragmadb.core.api.StorageEngine
import net.fischboeck.pragmadb.core.model.Document
import net.fischboeck.pragmadb.core.model.MediaType
import net.fischboeck.pragmadb.core.model.command.InsertCommand
import net.fischboeck.pragmadb.core.service.StorageEngineRegistry
import net.fischboeck.pragmadb.util.IntegrationTestExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Isolated
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(IntegrationTestExtension::class)
class DocumentControllerIntegrationTest: BaseIntegrationTest() {

    private lateinit var engine: StorageEngine

    companion object {
        const val ID: String = "1bede34c-4afb-4a18-bf1e-7e3d89b0ec8d"
        const val CONTENT = """{"name":"test","age":24}"""
    }

    @BeforeEach
    fun setup() {
        engine = StorageEngineRegistry.createCollection("test-collection")

        val cmd = InsertCommand(Document(
            id = UUID.fromString(ID),
            mediaType = MediaType.TEXT_PLAIN,
            content = "foo".toByteArray()
        ))
        engine.accept(cmd)

        // insert 2nd revision
        val cmd2 = InsertCommand(cmd.document.withContent("bar".toByteArray()))
        engine.accept(cmd2)
    }

    @AfterEach
    fun cleanup() {
        StorageEngineRegistry.purgeCollections()
    }



    @Test
    fun `get document - fails with 400 for malformed UUIDs`() {

        val request = request("http://localhost:$port/collections/test-collection/docs/hello-world-364")
            .GET()
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.BAD_REQUEST, response.statusCode())
    }

    @Test
    fun `get document - fails with 404 on unknown collection`() {

        val request = request("http://localhost:$port/collections/foobar/docs/${UUID.randomUUID()}")
            .GET()
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.NOT_FOUND, response.statusCode())
    }

    @Test
    fun `get document - fails with 404 on unknown document id`() {

        val request = request("http://localhost:$port/collections/test-collection/docs/${UUID.randomUUID()}")
            .GET()
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.NOT_FOUND, response.statusCode())
    }

    @Test
    fun `get document - fails with 404 on unknown revision if specified`() {

        val request = request("http://localhost:$port/collections/test-collection/docs/$ID?rev=6000")
            .GET()
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.NOT_FOUND, response.statusCode())
    }

    @Test
    fun `get document - returns document with highest revision if not specified`() {

        val request = request("http://localhost:$port/collections/test-collection/docs/$ID")
            .GET()
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.OK, response.statusCode())
        assertEquals("bar", response.body())
    }

    @Test
    fun `get document - returns document with specified revision`() {

        val request = request("http://localhost:$port/collections/test-collection/docs/$ID?rev=1")
            .GET()
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.OK, response.statusCode())
        assertEquals("foo", response.body())
    }


    @Test
    fun `insert document - fails with 415 on unsupported media types`() {

        val request = HttpRequest.newBuilder(URI.create("http://localhost:$port/collections/test-collection/docs"))
            .header(Headers.CONTENT_TYPE.toString(), "applicaton/custom-thingy")
            .POST(BodyPublishers.ofString("0xffff"))
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.UNSUPPORTED_MEDIA_TYPE, response.statusCode())
    }

    @Test
    fun `insert document - fails with 404 on unknown collection`() {

        val request = request("http://localhost:$port/collections/foobar/docs")
            .POST(BodyPublishers.ofString(CONTENT))
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.NOT_FOUND, response.statusCode())
    }

    @Test
    fun `update document - fails with 404 for unknown collection`() {

        val request = request("http://localhost:$port/collections/foobar/docs/$ID")
            .PUT(BodyPublishers.ofString(CONTENT))
            .build()
        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.NOT_FOUND, response.statusCode())
        assertTrue(response.body().contains("Unknown collection with name foobar"))
    }

    @Test
    fun `update document - fails with 404 for unknown document id`() {

        val request = request("http://localhost:$port/collections/test-collection/docs/${UUID.randomUUID()}")
            .PUT(BodyPublishers.ofString(CONTENT))
            .build()
        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.NOT_FOUND, response.statusCode())
        assertTrue(response.body().contains("Unknown document for id="))
    }

    @Test
    fun `delete document - fails with 404 on unknown collection`() {

        val request = request("http://localhost:$port/collections/foo/docs/$ID")
            .DELETE()
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.NOT_FOUND, response.statusCode())
        assertTrue(response.body().contains("Unknown collection with name foo"))
    }

    @Test
    fun `delete document - returns 200 for unknown document`() {

        val request = request("http://localhost:$port/collections/test-collection/docs/${UUID.randomUUID()}")
            .DELETE()
            .build()
        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.OK, response.statusCode())
    }

    @Test
    fun `complete lifecycle`() {

        val content = """{"name": "test", "age": 22}"""

        // insert document
        val request = request("http://localhost:$port/collections/test-collection/docs")
            .POST(BodyPublishers.ofString(content))
            .build()
        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.CREATED, response.statusCode())
        assertEquals(content, response.body())

        // read the document
        val location = response.headers().firstValue(Headers.LOCATION_STRING)
        val getRequest = request(location.get())
            .GET()
            .build()

        val getDocumentResponse = httpClient.send(getRequest, BodyHandlers.ofString())
        assertEquals(StatusCodes.OK, getDocumentResponse.statusCode())
        assertEquals(content, getDocumentResponse.body())

        // update the document
        val content2 = """{"name": "test", "age": 23}"""
        val putRequest = request(location.get())
            .PUT(BodyPublishers.ofString(content2))
            .build()

        val putResponse = httpClient.send(putRequest, BodyHandlers.ofString())
        assertEquals(StatusCodes.OK, putResponse.statusCode())
        assertEquals(content2, putResponse.body())

        // ensure the new revision can be retrieved
        val getRequest2 = request(location.get())
            .GET()
            .build()

        val getResponse2 = httpClient.send(getRequest2, BodyHandlers.ofString())
        assertEquals(StatusCodes.OK, getResponse2.statusCode())
        assertEquals(content2, getResponse2.body())

        // delete the document
        val deleteRequest = request(location.get()).DELETE().build()
        val deleteResponse = httpClient.send(deleteRequest, BodyHandlers.ofString())

        assertEquals(StatusCodes.OK, deleteResponse.statusCode())
        assertEquals("", deleteResponse.body())

        // ensure we retrieve 404 on get
        val getResponse3 = httpClient.send(request(location.get()).GET().build(), BodyHandlers.ofString())
        assertEquals(StatusCodes.NOT_FOUND, getResponse3.statusCode())
    }
}