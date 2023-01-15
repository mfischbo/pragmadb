package net.fischboeck.pragmadb.server.controller

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.StatusCodes
import mu.KLogging
import net.fischboeck.pragmadb.core.model.Document
import net.fischboeck.pragmadb.framework.context.BeanRegistry
import net.fischboeck.pragmadb.server.dto.ErrorResponse
import java.lang.NumberFormatException
import java.nio.ByteBuffer

open class BaseController {

    internal val objectMapper = BeanRegistry.getBean(ObjectMapper::class.qualifiedName!!) as ObjectMapper

    internal fun <T> readRequest(exchange: HttpServerExchange, type: Class<T>): T {

        try {
            val buffer = ByteBuffer.allocate(exchange.requestContentLength.toInt())
            exchange.requestChannel.read(buffer)
            return objectMapper.readValue(buffer.array(), type)
        } catch (ex: JsonParseException) {
            sendErrorResponse(exchange, ErrorResponse(StatusCodes.BAD_REQUEST, "Unable to read request"))
            throw ex
        }
    }

    internal fun sendErrorResponse(exchange: HttpServerExchange, errorResponse: ErrorResponse) {

        exchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/json")
        exchange.statusCode = errorResponse.status
        exchange.responseSender.send(objectMapper.writeValueAsString(errorResponse))
    }

    internal fun sendUnknownCollectionResponse(exchange: HttpServerExchange, collectionName: String) {
        exchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/json")
        exchange.statusCode = StatusCodes.NOT_FOUND
        exchange.responseSender.send(objectMapper.writeValueAsString(ErrorResponse(
            status = StatusCodes.NOT_FOUND,
            message = "Unknown collection with name $collectionName")
        ))
    }

    internal fun sendResponse(exchange: HttpServerExchange, response: Any, httpStatus: Int) {

        exchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/json")
        exchange.statusCode = httpStatus
        exchange.responseSender.send(objectMapper.writeValueAsString(response))
    }

    internal fun sendDocumentResponse(exchange: HttpServerExchange, document: Document, httpStatus: Int) {

        val location = "${exchange.requestScheme}://${exchange.hostAndPort}${exchange.requestURI}/${document.id}"

        exchange.responseHeaders.put(Headers.CONTENT_TYPE, document.mediaType.httpName)
        exchange.responseHeaders.put(Headers.LOCATION, location)
        exchange.statusCode = httpStatus
        exchange.responseSender.send(objectMapper.writeValueAsString(document))
    }

    internal fun queryParameterAsBoolean(param: String, exchange: HttpServerExchange): Boolean {
        val deque = exchange.queryParameters[param]
        return deque?.firstOrNull { it.trim().isNotEmpty() }.toBoolean()
    }

    internal fun queryParameterAsInt(param: String, exchange: HttpServerExchange): Int? {

        return try {
            exchange.queryParameters[param]
                ?.firstOrNull {
                    it.trim().isNotEmpty()
                }?.toInt()
        } catch (ex: NumberFormatException) {
            sendErrorResponse(exchange, ErrorResponse(StatusCodes.BAD_REQUEST, "Parameter $param needs to be an integer"))
            logger.warn("Malformed query parameter [${param}] not an integer")
            return null
        }
    }

    companion object: KLogging()
}