package net.fischboeck.pragmadb.server.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.fischboeck.pragmadb.core.io.JacksonDocumentSerializer
import net.fischboeck.pragmadb.core.model.Document
import net.fischboeck.pragmadb.framework.context.BeanRegistry

object HttpBeans {

    fun register() {
        BeanRegistry.registerBeanDefinition(getObjectMapper())
    }

    private fun getObjectMapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        objectMapper.registerModule(KotlinModule.Builder().build())

        val documentSerializer = SimpleModule()
        documentSerializer.addSerializer(Document::class.java, JacksonDocumentSerializer())
        objectMapper.registerModule(documentSerializer)

        return objectMapper
    }
}