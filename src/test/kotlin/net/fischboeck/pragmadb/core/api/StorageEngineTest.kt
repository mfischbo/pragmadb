package net.fischboeck.pragmadb.core.api

import net.fischboeck.pragmadb.core.model.Collection
import net.fischboeck.pragmadb.core.model.Document
import net.fischboeck.pragmadb.core.model.command.DeleteCommand
import net.fischboeck.pragmadb.core.model.command.GetDocumentCommand
import net.fischboeck.pragmadb.core.model.command.InsertCommand
import net.fischboeck.pragmadb.core.model.exception.DocumentNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import java.util.concurrent.ExecutionException
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StorageEngineTest {


    @Test
    fun `completes with insert and deletions in order`() {

        val collection = Collection("foo")
        val document = Document(content = "foo".toByteArray())

        val engine = StorageEngine(collection)

        val insert = InsertCommand(document)
        val delete = DeleteCommand(document.id)

        engine.accept(insert)
        engine.accept(delete)

        val insertedDocument = insert.result.get()
        assertNotNull(insertedDocument)
    }

    @Test
    fun `completes with exception for unknown document`() {

        val collection = Collection("foo")
        val engine = StorageEngine(collection)

        val getById = GetDocumentCommand(UUID.randomUUID())
        engine.accept(getById)

        val exception = assertThrows<ExecutionException> {
            getById.result.get()
        }
        assertTrue(exception.cause is DocumentNotFoundException)
    }
}