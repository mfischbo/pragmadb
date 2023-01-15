package net.fischboeck.pragmadb.core.io

import net.fischboeck.pragmadb.core.model.Collection
import net.fischboeck.pragmadb.core.model.Document
import net.fischboeck.pragmadb.core.model.put
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID

class KetchupSerializer {


    fun serialize(collection: Collection) {

        val path = FileSystems.getDefault().getPath("/tmp", "ketchup.bin")
        println(path)

        val outStream = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)

        outStream.write(collection.name.toByteArray())
        outStream.write(";".toByteArray())
        collection.documents.forEach {

            it.value.forEach { revision ->
                outStream.write("${revision.value.id};".toByteArray())
                outStream.write("${revision.value.revision};".toByteArray())
                outStream.write("${revision.value.content.size};".toByteArray())
                outStream.write(revision.value.content)
            }
        }
        outStream.close()
    }


    fun deserialize(path: Path): Collection {

        val inStream = Files.newInputStream(path, StandardOpenOption.READ)
        val collectionName = readToken(inStream) ?: throw IOException("Unreadable collection name")

        val collection = Collection(name = collectionName)
        do {

            val id = readAsUUID(inStream) ?: return collection
            val revision = readAsInt(inStream)?.toLong() ?: throw IOException("Failed to consume revision")
            val size = readAsInt(inStream) ?: throw IOException("Failed to consume content size")
            val content = readContent(inStream, size)

            val document = Document(id = id, revision = revision, content = content)
            collection.put(document)

        } while (id != null)
        return collection
    }


    private fun readAsUUID(inputStream: InputStream): UUID? {
        val content = readToken(inputStream)
        if (content.isNullOrEmpty()) {
            return null
        }
        return UUID.fromString(content)
    }

    private fun readAsInt(inputStream: InputStream): Int? {
        val content = readToken(inputStream)
        if (content.isNullOrEmpty()) {
            return null
        }
        return content.toInt()
    }


    private fun readToken(inputStream: InputStream): String? {
        val buffer = StringBuffer()
        do {
            val x = inputStream.read()
            if (x == -1) {
                return null
            }
            buffer.append(Char(x))
        } while (x != ';'.code )

        if (buffer.isEmpty()) {
            return null
        }
        return buffer.toString().substring(0, buffer.lastIndex)
    }

    private fun readContent(inputStream: InputStream, size: Int): ByteArray {

        val buffer = inputStream.readNBytes(size)
        inputStream.read(buffer, 0, size)
        return buffer
    }
}