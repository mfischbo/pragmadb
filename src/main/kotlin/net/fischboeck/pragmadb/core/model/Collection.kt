package net.fischboeck.pragmadb.core.model

import java.util.UUID

data class Collection(
    val name: String,
    val documents: MutableMap<UUID, MutableMap<Long, Document>> = mutableMapOf()
)

fun Collection.save(document: Document): Document {

    // new document with revision 1
    if (!documents.containsKey(document.id)) {
        val insert = Document(
            id = document.id,
            revision = 1,
            content = document.content,
            mediaType = document.mediaType
        )
        documents[document.id] = mutableMapOf(insert.revision to insert)
        return insert
    }

    // new revision
    val insert = Document(
        id = document.id,
        revision = documents[document.id]!!.size + 1L,
        content = document.content,
        mediaType = document.mediaType
    )
    documents[document.id]!![insert.revision] = insert
    return insert
}

/**
 * Adds the document to the collection while skipping revision handling
 */
fun Collection.put(document: Document) {

    if (documents.containsKey(document.id)) {
        documents[document.id]!![document.revision] = document
    } else {
        documents[document.id] = mutableMapOf(document.revision to document)
    }
}

fun Collection.getDocumentById(id: UUID): Document? {

    return if (!documents.containsKey(id)) {
        null
    } else {
        val revision = documents[id]!!.keys.max()
        return documents[id]!![revision]
    }
}

fun Collection.getDocumentById(id: UUID, revision: Long): Document? {
    return documents[id]?.get(revision)
}

fun Collection.delete(id: UUID) {
    documents.remove(id)
}