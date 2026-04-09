package org.dba.woomera


import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.slf4j.LoggerFactory


object MongoManage {

    private var client: MongoClient? = null

    private const val DATABASE_NAME = "Woomera"
    lateinit var database: MongoDatabase

    private val logger = LoggerFactory.getLogger("MongoManage")


    fun connect() {
        if (client != null) {
            logger.warn("Attempted to connect, but already connected to MongoDB.")
            return
        }
        try {
            client = MongoClient.create("mongodb://localhost:27017")
            database = client?.getDatabase(DATABASE_NAME) ?: throw IllegalStateException("MongoDB is not connected")

        } catch (e: Exception) {
            logger.error("Failed to connect to MongoDB: ${e.message}")
        }
    }

}