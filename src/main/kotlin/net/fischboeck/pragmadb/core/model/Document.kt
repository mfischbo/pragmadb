package net.fischboeck.pragmadb.core.model

import java.util.UUID

data class Document(

    val id: UUID = UUID.randomUUID(),
    val revision: Long = 0,
    val mediaType: MediaType = MediaType.APPLICATION_JSON,
    val content: ByteArray
) {

    fun withContent(content: ByteArray): Document {
        return Document(id = id, revision = revision, content = content, mediaType = mediaType)
    }

    fun copy(): Document {
        return Document(id = id, revision = revision, content = content, mediaType = mediaType)
    }

    override fun toString(): String {
        return "{id=$id, revision=$revision, content=${String(content)}}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Document

        if (id != other.id) return false
        if (revision != other.revision) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + revision.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}
