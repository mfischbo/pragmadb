package net.fischboeck.pragmadb.server.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.util.Headers
import net.fischboeck.pragmadb.framework.context.BeanRegistry
import net.fischboeck.pragmadb.server.config.HttpBeans
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest

open class BaseIntegrationTest {

    internal val httpClient: HttpClient
    internal val objectMapper: ObjectMapper
    internal val port = 8081

    init {
        HttpBeans.register()
        httpClient = HttpClient.newHttpClient()
        objectMapper = BeanRegistry.getBean(ObjectMapper::class.java.canonicalName) as ObjectMapper
    }

    fun request(url: String): HttpRequest.Builder {
        return HttpRequest.newBuilder(URI.create(url))
            .header(Headers.CONTENT_TYPE.toString(), "application/json")
    }
}