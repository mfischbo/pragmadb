package net.fischboeck.pragmadb.server.dto

data class GetCollectionsResponse(
    val collections: List<String>
)

data class CreateCollectionRequest(
    val name: String
)

data class CreateCollectionResponse(
    val name: String
)