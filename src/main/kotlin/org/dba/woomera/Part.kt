package org.dba.woomera

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.io.Serializable

data class Part (
    @param:BsonId val id: ObjectId = ObjectId(),
    val code: String,
    val description: String,
    val category: String,
    val slots: ArrayList<String>,
    val name: String,
    val od1: Boolean
) : Serializable