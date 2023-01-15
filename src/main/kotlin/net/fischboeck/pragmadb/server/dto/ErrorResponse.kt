package net.fischboeck.pragmadb.server.dto

data class ErrorResponse(
    val status: Int,
    val message: String
)