package org.dba.woomera
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.io.Serializable

data class Slot (
    @param:BsonId val id: ObjectId = ObjectId(),
    val name: String,
    val type: String,
    val quantity: Int,
    val description: String,
    val parts: ArrayList<String>
) : Serializable
