package net.fischboeck.pragmadb.server.controller

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.StatusCodes
import net.fischboeck.pragmadb.core.model.Document
import net.fischboeck.pragmadb.core.model.MediaType
import net.fischboeck.pragmadb.core.model.command.DeleteCommand
import net.fischboeck.pragmadb.core.model.command.GetDocumentCommand
import net.fischboeck.pragmadb.core.model.command.InsertCommand
import net.fischboeck.pragmadb.core.model.exception.DocumentNotFoundException
import net.fischboeck.pragmadb.core.model.exception.UnknownMediaTypeException
import net.fischboeck.pragmadb.core.service.StorageEngineRegistry
import net.fischboeck.pragmadb.server.dto.ErrorResponse
import java.nio.ByteBuffer
import java.util.UUID

class DocumentByIdController: BaseController(), HttpHandler {

    // GET /collections/{collectionName}/docs/{documentId}
    override fun handleRequest(exchange: HttpServerExchange) {

        val parts = exchange.requestURI.split("/")
        val collectionName = parts[2]

        val id: UUID
        try {
            id = UUID.fromString(parts[4])
        } catch (ex: IllegalArgumentException) {
            logger.warn { "Malformed UUID provided for input string ${parts[4]}" }
            return sendErrorResponse(exchange, ErrorResponse(StatusCodes.BAD_REQUEST, "Unknown document for id ${parts[4]}"))
        }

        val revision = queryParameterAsInt("rev", exchange)


        val engine = StorageEngineRegistry.getStorageEngine(collectionName)
            ?: return sendUnknownCollectionResponse(exchange, collectionName)

        val command = GetDocumentCommand(id, revision)
        engine.accept(command)


        command.result.whenCompleteAsync {doc, ex ->

            if (ex != null && ex is DocumentNotFoundException) {
                sendErrorResponse(exchange, ErrorResponse(StatusCodes.NOT_FOUND, ex.message ?: ""))
            } else if (ex != null) {
                logger.error("Caught unknown exception for request ${exchange.requestURI}", ex)
                sendErrorResponse(exchange, ErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, "Unknown error"))
            }
            sendDocumentResponse(exchange, doc, StatusCodes.OK)
        }
    }
}

// POST /collections/{collectionName}/docs
class CreateDocumentController: BaseController(), HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {

        val parts = exchange.requestURI.split("/")
        val collectionName = parts[2]

        // parse media type
        val mediaTypeHeader = exchange.requestHeaders.get(Headers.CONTENT_TYPE).first
        val mediaType: MediaType
        try {
            mediaType = MediaType.forString(mediaTypeHeader)
        } catch (ex: UnknownMediaTypeException) {
            return sendErrorResponse(exchange, ErrorResponse(
                StatusCodes.UNSUPPORTED_MEDIA_TYPE,
                "Media Type: $mediaTypeHeader is not supported"
            ))
        }

        // parse content
        val buffer = ByteBuffer.allocate(exchange.requestContentLength.toInt())
        exchange.requestChannel.read(buffer)

        val engine = StorageEngineRegistry.getStorageEngine(collectionName)
            ?: return sendUnknownCollectionResponse(exchange, collectionName)

        val document = Document(
            content = buffer.array(),
            mediaType = mediaType
        )

        // insert
        val insertCommand = InsertCommand(document)
        engine.accept(insertCommand)

        // response
        insertCommand.result.whenCompleteAsync { doc, exception ->

            if (exception != null) {
                logger.error("Unable to insert document", exception)
                sendErrorResponse(exchange, ErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, "Could not insert document for unknown reason"))
            } else {
                sendDocumentResponse(exchange, doc, StatusCodes.CREATED)
            }
        }
    }
}


// PUT /collections/{collectionName}/docs/{documentId}
class UpdateDocumentController: BaseController(), HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {

        val parts = exchange.requestURI.split("/")
        val collection = parts[2]
        val documentId = UUID.fromString(parts[4])

        // changing media types on updates is not supported, so we just read the content
        val buffer = ByteBuffer.allocate(exchange.requestContentLength.toInt())
        exchange.requestChannel.read(buffer)

        val engine = StorageEngineRegistry.getStorageEngine(collection)
            ?: return sendUnknownCollectionResponse(exchange, collection)

        val command = GetDocumentCommand(documentId)
        engine.accept(command)

        command.result.whenCompleteAsync { document, exception ->

            if (exception != null && exception is DocumentNotFoundException) {
                sendErrorResponse(exchange, ErrorResponse(
                    StatusCodes.NOT_FOUND, exception.message ?: ""
                ))
            }

            if (document != null) {
                val newRevision = document.withContent(buffer.array())
                val insertCommand = InsertCommand(newRevision)
                engine.accept(insertCommand)

                insertCommand.result.whenCompleteAsync { update, ex ->
                    if (update != null) {
                        sendDocumentResponse(exchange, update, StatusCodes.OK)
                    }

                    sendErrorResponse(exchange, ErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, ex.message ?: ""))
                }
            }
        }
    }
}

// DELETE /collections/{collectionName}/docs/{documentId}
class DeleteDocumentController: BaseController(), HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {

        val parts = exchange.requestURI.split("/")
        val collection = parts[2]
        val documentId = UUID.fromString(parts[4])

        val engine = StorageEngineRegistry.getStorageEngine(collection)
            ?: return sendUnknownCollectionResponse(exchange, collection)

        val deleteCommand = DeleteCommand(documentId)
        engine.accept(deleteCommand)

        deleteCommand.result.whenCompleteAsync {_, exception ->

            if (exception != null) {
                logger.warn("Failed to delete document for id: $documentId", exception)
                sendErrorResponse(exchange, ErrorResponse(
                    StatusCodes.INTERNAL_SERVER_ERROR, exception.message ?: "Unknown error occurred"
                ))
            }

            exchange.statusCode = StatusCodes.OK
            exchange.responseSender.send("")
        }
    }
}

