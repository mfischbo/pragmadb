package net.fischboeck.pragmadb.server.controller

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.StatusCodes
import net.fischboeck.pragmadb.core.model.command.GetAllDocumentsCommand
import net.fischboeck.pragmadb.core.service.StorageEngineRegistry
import net.fischboeck.pragmadb.server.dto.CreateCollectionRequest
import net.fischboeck.pragmadb.server.dto.CreateCollectionResponse
import net.fischboeck.pragmadb.server.dto.ErrorResponse
import net.fischboeck.pragmadb.server.dto.GetCollectionsResponse

// GET /collections
class CollectionController: BaseController(), HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {

        val collections = StorageEngineRegistry.getAllCollections()
        sendResponse(exchange, GetCollectionsResponse(collections), StatusCodes.OK)
    }
}

// POST /collections
class CreateCollectionController: BaseController(), HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {

        try {
            val command = readRequest(exchange, CreateCollectionRequest::class.java)
            if (command.name.trim().isEmpty()) {
                return sendErrorResponse(exchange, ErrorResponse(StatusCodes.BAD_REQUEST, "The collection name must not be empty"))
            }

            val engine = StorageEngineRegistry.createCollection(command.name.trim())
            val response = CreateCollectionResponse(name = engine.name)
            sendResponse(exchange, response, StatusCodes.CREATED)
        } catch (ex: Exception) {
            // noop. Error response already sent
        }
    }
}

// DELETE /collections/{collectionName}
class DeleteCollectionController: BaseController(), HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {

        val collectionName = exchange.requestPath.split("/")[2]
        val engine = StorageEngineRegistry.getStorageEngine(collectionName)
            ?: return sendErrorResponse(exchange, ErrorResponse(StatusCodes.NOT_FOUND, "Unknown collection $collectionName"))

        val success = StorageEngineRegistry.deleteCollection(engine.name)
        if (success) {
            exchange.statusCode = StatusCodes.OK
            exchange.responseSender.close()
        } else {
            return sendErrorResponse(exchange, ErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, "Failed to remove collection $collectionName"))
        }
    }
}

// GET /collections/{collectionName}
//TODO: Currently broken and useless. Should return meta data about the collection
class CollectionDocumentController: BaseController(), HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {

        val collectionName = exchange.requestPath.split("/")[2]
        val engine = StorageEngineRegistry.getStorageEngine(collectionName)
            ?: return sendErrorResponse(exchange, ErrorResponse(StatusCodes.NOT_FOUND, "Unknown collection $collectionName"))

        val includeRevs = queryParameterAsBoolean("includeRevs", exchange)

        val command = GetAllDocumentsCommand(includeRevs)
        engine.accept(command)

        val result = command.result.get()
        sendResponse(exchange, result, StatusCodes.OK)
    }
}