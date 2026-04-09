package org.dba.woomera

import java.io.Serializable

class BuildPart : Serializable{
    val code: String
    val description: String
    val name: String
    val category: String
    val slots: ArrayList<String>
    val od1: Boolean
    var parent: String
    var buildCount: Int
    var totalCount: Int

    constructor(code: String, description: String, name: String, category: String, slots: ArrayList<String>, od1: Boolean) {
        this.code = code
        this.description = description
        this.name = name
        this.category = category
        this.slots = slots
        this.od1 = od1
        this.buildCount = 0
        this.parent = ""
        this.totalCount = 0

    }
    constructor(part: Part) {
        code = part.code
        description = part.description
        name = part.name
        category = part.category
        slots = part.slots
        od1 = part.od1
        parent  = ""
        buildCount = 0
        totalCount = 0
    }

}