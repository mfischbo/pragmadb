package net.fischboeck.pragmadb.core.model.command

import net.fischboeck.pragmadb.core.model.Document
import java.util.UUID
import java.util.concurrent.CompletableFuture

interface Command {
}

data class InsertCommand(
    val document: Document,
): Command {
    val result = CompletableFuture<Document>()
}

data class DeleteCommand(
    val id: UUID,
): Command {
    val result = CompletableFuture<Unit>()
}

data class GetDocumentCommand(
    val id: UUID,
    val revision: Int? = null
): Command {
    val result = CompletableFuture<Document>()
}

data class GetAllDocumentsCommand(
    val includeRevs: Boolean
): Command {
    val result = CompletableFuture<List<Document>>()
}