package net.fischboeck.pragmadb.core.service

import net.fischboeck.pragmadb.core.api.StorageEngine
import net.fischboeck.pragmadb.core.model.Collection

object StorageEngineRegistry {

    private val engines: MutableList<StorageEngine> = mutableListOf()

    fun getAllCollections(): List<String> {
        return engines.map {
            it.name
        }.toList()
    }

    fun getStorageEngine(collectionName: String): StorageEngine? {
        return engines.firstOrNull {
            it.name == collectionName
        }
    }

    fun createCollection(name: String): StorageEngine {

        val engine = engines.firstOrNull {
            it.name == name
        }
        if (engine != null)
            return engine

        val collection = Collection(name)
        val retval = StorageEngine(collection)
        engines.add(retval)
        return retval
    }

    fun deleteCollection(name: String): Boolean {
        val engine = engines.find { it.name == name }
        return engines.remove(engine)
    }


    fun purgeCollections() {
        engines.clear()
    }
}