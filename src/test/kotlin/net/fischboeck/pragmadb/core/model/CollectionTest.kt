package net.fischboeck.pragmadb.core.model

import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CollectionTest {

    private val collection = Collection("foo")

    @Test
    fun `can insert into collection`() {

        val document = Document(content = "foo".toByteArray())

        val result = collection.save(document)
        assertEquals(document.id, result.id)
        assertEquals(1, result.revision)

        "foo".toByteArray(Charset.defaultCharset()).forEachIndexed { idx, byte ->
            assertEquals(byte, document.content[idx])
        }
    }


    @Test
    fun `increments revision when inserting into collection`() {

        val document1 = Document(content = "foo".toByteArray())

        collection.save(document1)
        val result = collection.save(document1)

        assertEquals(document1.id, result.id)
        assertEquals(2, result.revision)
        "foo".toByteArray(Charset.defaultCharset()).forEachIndexed { index, byte ->
            assertEquals(byte, document1.content[index])
        }
    }

    @Test
    fun `stores copies on inserts`() {

        val document = Document(content = "foo".toByteArray())

        val result1 = collection.save(document)

        val document2 = document.withContent("bar".toByteArray())
        val result2 = collection.save(document2)

        assertEquals(document.id, result1.id)
        assertEquals(document.id, result2.id)

        assertEquals(1, result1.revision)
        assertEquals(2, result2.revision)

        assertEquals("foo", String(result1.content))
        assertEquals("bar", String(result2.content))
    }


    @Test
    fun `getDocumentById - returns document for given id`() {

        val document1 = Document(content = "foo".toByteArray())
        val document2 = Document(content = "bar".toByteArray())

        val save1 = collection.save(document1)
        val save2 = collection.save(document2)

        val result1 = collection.getDocumentById(document1.id)
        val result2 = collection.getDocumentById(document2.id)
        assertEquals(save1, result1)
        assertEquals(save2, result2)
    }

    @Test
    fun `getDocumentById - returns null for unknown id's`() {

        val result = collection.getDocumentById(UUID.randomUUID())
        assertNull(result)
    }

    @Test
    fun `getDocumentById - returns highest revision by default`() {

        val document1 = Document(content = "foo".toByteArray())
        val document2 = document1.withContent("bar".toByteArray())

        collection.save(document1)
        val save2 = collection.save(document2)

        val result = collection.getDocumentById(document1.id)
        assertNotNull(result)
        assertEquals(document2.id, result.id)
        assertEquals(2, result.revision)
        assertEquals(save2, result)
    }

    @Test
    fun `getDocumentById - returns null for unknown revision`() {

        val document1 = Document(content = "foo".toByteArray())
        collection.save(document1)

        val result = collection.getDocumentById(document1.id, Long.MAX_VALUE)
        assertNull(result)
    }


    @Test
    fun `getDocumentById - returns specified revision`() {

        val document1 = Document(content = "foo".toByteArray())
        val document2 = document1.withContent("bar".toByteArray())

        collection.save(document1)
        collection.save(document2)

        val result1 = collection.getDocumentById(document1.id, 1)
        val result2 = collection.getDocumentById(document1.id, 2)

        assertNotNull(result1)
        assertNotNull(result2)

        assertEquals(1, result1.revision)
        assertEquals(2, result2.revision)
    }

    @Test
    fun `delete - deletes only document with given ids`() {

        val document1 = Document(content = "foo".toByteArray())
        var document2 = Document(content = "bar".toByteArray())

        collection.save(document1)
        document2 = collection.save(document2)
        collection.delete(document1.id)
        val result1 = collection.getDocumentById(document1.id)
        val result2 = collection.getDocumentById(document2.id)

        assertNull(result1)
        assertEquals(document2, result2)
    }
}