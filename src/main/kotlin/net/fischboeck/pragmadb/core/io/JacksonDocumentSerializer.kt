package net.fischboeck.pragmadb.core.io

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import net.fischboeck.pragmadb.core.model.Document

class JacksonDocumentSerializer: StdSerializer<Document>(Document::class.java) {

    override fun serialize(doc: Document, jsonGen: JsonGenerator, provider: SerializerProvider) {
        val content = String(doc.content, Charsets.UTF_8)
        //jsonGen.writeString(content)
        jsonGen.writeRawValue(content)
    }
}
