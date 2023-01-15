package net.fischboeck.pragmadb.core.io

import org.junit.jupiter.api.Test
import net.fischboeck.pragmadb.core.model.Collection
import net.fischboeck.pragmadb.core.model.Document
import net.fischboeck.pragmadb.core.model.save
import java.nio.file.FileSystems
import java.util.UUID

class KetchupSerializerTest {


 //   @Test
    fun `dont do shit on my filesystem`() {

        val collection = Collection(name = "ketchup-collection")
        val document = Document(content = "Brooooo!".toByteArray())

        collection.save(document)

        val testee = KetchupSerializer()

        testee.serialize(collection)

        val result = testee.deserialize(FileSystems.getDefault().getPath("/tmp", "ketchup.bin"))

        println(result)
    }
}