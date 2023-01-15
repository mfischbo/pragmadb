package net.fischboeck.pragmadb.server.controller

import com.fasterxml.jackson.module.kotlin.readValue
import io.undertow.util.StatusCodes
import net.fischboeck.pragmadb.core.service.StorageEngineRegistry
import net.fischboeck.pragmadb.server.dto.CreateCollectionRequest
import net.fischboeck.pragmadb.server.dto.CreateCollectionResponse
import net.fischboeck.pragmadb.server.dto.ErrorResponse
import net.fischboeck.pragmadb.server.dto.GetCollectionsResponse
import net.fischboeck.pragmadb.util.IntegrationTestExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@ExtendWith(IntegrationTestExtension::class)
class CollectionControllerIntegrationTest: BaseIntegrationTest() {

    @AfterEach
    fun cleanup() {
        StorageEngineRegistry.purgeCollections()
    }

    @Test
    fun `getCollections - empty list when no collections registered`() {

        val request = request("http://localhost:$port/collections")
            .GET()
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        val result = objectMapper.readValue<GetCollectionsResponse>(response.body())

        assertEquals(200, response.statusCode())
        assertEquals(0, result.collections.size)
    }

    @Test
    fun `getCollections - returns inserted collections`() {

        val engine = StorageEngineRegistry.createCollection("foo")

        val request = request("http://localhost:$port/collections")
            .GET()
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        val result = objectMapper.readValue<GetCollectionsResponse>(response.body())

        assertEquals(200, response.statusCode())
        assertEquals(1, result.collections.size)
        assertEquals(engine.name, result.collections[0])
    }

    @Test
    fun `create collection - fails with 400 if name is empty`() {

        val command = CreateCollectionRequest("       ")
        val request = request("http://localhost:$port/collections")
            .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(command)))
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        val result = objectMapper.readValue<ErrorResponse>(response.body())

        assertEquals(StatusCodes.BAD_REQUEST, response.statusCode())
        assertEquals(StatusCodes.BAD_REQUEST, result.status)
        assertEquals("The collection name must not be empty", result.message)
    }

    @Test
    fun `create collection - return 200 when collection with name already exists`() {

        val command = CreateCollectionRequest("test-me")
        val request = request("http://localhost:$port/collections")
            .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(command)))
            .build()
        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.CREATED, response.statusCode())

        // post same collection again
        val response2 = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.CREATED, response2.statusCode())
    }

    @Test
    fun `createCollection - can create collection`() {

        val command = CreateCollectionRequest(name = "test-collection")

        val request = request("http://localhost:$port/collections")
            .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(command)))
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        val result = objectMapper.readValue<CreateCollectionResponse>(response.body())

        assertEquals(StatusCodes.CREATED, response.statusCode())
        assertEquals(command.name, result.name)
    }

    @Test
    fun `deleteCollection - fails with 404 if collection unknown`() {

        val request = request("http://localhost:$port/collections/foo")
            .DELETE()
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.NOT_FOUND, response.statusCode())
    }

    @Test
    fun `deleteCollection - returns 200 and collection is not retrievable again`() {

        val engine = StorageEngineRegistry.createCollection("test-collection")
        val request = request("http://localhost:$port/collections/${engine.name}")
            .DELETE()
            .build()

        val response = httpClient.send(request, BodyHandlers.ofString())
        assertEquals(StatusCodes.OK, response.statusCode())

        assertFalse(StorageEngineRegistry.getAllCollections().contains(engine.name))
    }
}