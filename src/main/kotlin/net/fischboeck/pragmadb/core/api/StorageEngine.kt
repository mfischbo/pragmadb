package net.fischboeck.pragmadb.core.api

import mu.KLogging
import net.fischboeck.pragmadb.core.model.Collection
import net.fischboeck.pragmadb.core.model.command.Command
import net.fischboeck.pragmadb.core.model.command.DeleteCommand
import net.fischboeck.pragmadb.core.model.command.GetAllDocumentsCommand
import net.fischboeck.pragmadb.core.model.command.GetDocumentCommand
import net.fischboeck.pragmadb.core.model.command.InsertCommand
import net.fischboeck.pragmadb.core.model.delete
import net.fischboeck.pragmadb.core.model.exception.DocumentNotFoundException
import net.fischboeck.pragmadb.core.model.getDocumentById
import net.fischboeck.pragmadb.core.model.save
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

/**
 * Acts as thread safe interface to modify collections
 */
class StorageEngine(private val collection: Collection): Runnable {

    private val commandQueue = ConcurrentLinkedQueue<Command>()
    private val executionPool = Executors.newFixedThreadPool(1)
    val name = collection.name


    fun accept(command: Command) {
        commandQueue.add(command)
        logger.debug { "Added command to queue" }
        executionPool.execute(this)
    }

    fun accept(commands: Iterable<Command>) {
        commandQueue.addAll(commands)
        logger.debug { "Added commands to queue" }
        executionPool.execute(this)
    }

    override fun run() {

        logger.debug { "Processing ${commandQueue.size} commands in the queue"}

        while (commandQueue.isNotEmpty()) {

            when (val command = commandQueue.poll()) {
                is GetDocumentCommand -> process(command)
                is InsertCommand -> process(command)
                is DeleteCommand -> process(command)
                is GetAllDocumentsCommand -> process(command)
            }
        }
    }

    private fun process(command: GetDocumentCommand) {

        val document = if (command.revision != null) {
            collection.getDocumentById(command.id, command.revision.toLong())
        } else {
            collection.getDocumentById(command.id)
        }

        if (document != null) {
            command.result.complete(document)
        } else {
            command.result.completeExceptionally(DocumentNotFoundException("Unknown document for id=${command.id}"))
        }
    }

    private fun process(command: InsertCommand) {
        val document = collection.save(command.document)
        command.result.complete(document)
        logger.debug { "Inserted document [id=${document.id}] into collection ${collection.name}" }
    }

    private fun process(command: DeleteCommand) {
        try {
            collection.delete(command.id)
            logger.debug { "Deleted document with [id=${command.id}" }
            command.result.complete(Unit)
        } catch (ex: Exception) {
            command.result.completeExceptionally(ex)
        }
    }

    private fun process(command: GetAllDocumentsCommand) {

        if (command.includeRevs) {
            val documents = collection.documents.flatMap {
                it.value.values
            }
            command.result.complete(documents)
            return
        }

        val documents = collection.documents.map {
            val highestRev = it.value.keys.max()
            it.value[highestRev]!!
        }
        command.result.complete(documents)
    }

    companion object: KLogging()
}