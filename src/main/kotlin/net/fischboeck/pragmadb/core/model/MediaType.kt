package net.fischboeck.pragmadb.core.model

import net.fischboeck.pragmadb.core.model.exception.UnknownMediaTypeException

enum class MediaType(val httpName: String) {
    TEXT_PLAIN("text/plain"),
    APPLICATION_JSON("application/json");


    companion object {

        fun forString(value: String): MediaType {

            return when (value.trim().lowercase()) {
                "text/plain" -> TEXT_PLAIN
                "application/json" -> APPLICATION_JSON
                else -> throw UnknownMediaTypeException("value")
            }
        }
    }
}