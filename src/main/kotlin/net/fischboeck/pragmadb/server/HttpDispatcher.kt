package net.fischboeck.pragmadb.server

import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import mu.KLogging
import net.fischboeck.pragmadb.server.controller.*

class HttpDispatcher(
    private val exchange: HttpServerExchange
): Runnable {

    private val adminController = AdminController()
    private val collectionController = CollectionController()
    private val createCollectionController = CreateCollectionController()
    private val collectionDocumentController = CollectionDocumentController()
    private val deleteCollectionController = DeleteCollectionController()


    private val getDocumentByIdController = DocumentByIdController()
    private val createDocumentController = CreateDocumentController()
    private val updateDocumentController = UpdateDocumentController()
    private val deleteDocumentController = DeleteDocumentController()


    private val routingHandler: RoutingHandler = RoutingHandler()
        .get("/", adminController)

        // collections
        .get("/collections", collectionController)
        .post("/collections", createCollectionController)
        .delete("/collections/{collectionName}", deleteCollectionController)
        .get("/collections/{collectionName}/docs", collectionDocumentController)

        // documents
        .get("/collections/{collectionName}/docs/{id}", getDocumentByIdController)
        .post("/collections/{collectionName}/docs", createDocumentController)
        .put("/collections/{collectionName}/docs/{documentId}", updateDocumentController)
        .delete("/collections/{collectionName}/docs/{documentId}", deleteDocumentController)

    override fun run() {
        routingHandler.handleRequest(exchange)
    }

    companion object: KLogging()
}