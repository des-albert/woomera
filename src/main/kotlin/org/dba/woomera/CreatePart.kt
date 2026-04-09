package org.dba.woomera

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
import javafx.stage.Stage
import org.bson.types.ObjectId
import org.dba.woomera.Woomera.Companion.partHashMap
import org.dba.woomera.Woomera.Companion.selectedTreeItem

class CreatePart {
    @FXML
    lateinit var toggle0D1: ToggleButton
    @FXML
    lateinit var textFieldPartCode: TextField
    @FXML
    lateinit var textFieldPartDescription: TextField
    @FXML
    lateinit var textFieldPartCat: TextField
    @FXML
    lateinit var textFieldPartName: TextField
    @FXML
    lateinit var labelAddStatus: Label
    @FXML
    lateinit var buttonAddPart: Button
    @FXML
    lateinit var buttonAddPartDone: Button

    fun initialize() {
        val folder: Folder = (selectedTreeItem?.value) as Folder
        textFieldPartCat.text = folder.category
    }

    fun buttonAddPartAction() {
        val code = textFieldPartCode.text
        val description = textFieldPartDescription.text
        val category = textFieldPartCat.text
        val name = textFieldPartName.text
        val od1 = toggle0D1.text == "0D1"

        if (code.isEmpty() || description.isEmpty() || category.isEmpty()) {
            labelAddStatus.text = "Fields cannot be NULL"
            labelAddStatus.style = "-fx-text-fill: status-error-color"
            return
        }
        val slots: ArrayList<String> = ArrayList()
        val newPart = Part(ObjectId(), code, description, category, slots, name, od1)
        if (partHashMap.containsKey(newPart.code)) {
            labelAddStatus.text = "Duplicate part code %s".format(code)
            labelAddStatus.style = "-fx-text-fill: status-error-color"
        } else {
            partHashMap[newPart.code] = newPart
            labelAddStatus.text = "New Part %s - %s created".format(code, description)
            labelAddStatus.style = "-fx-text-fill: status-good-color"

        }

    }

    fun toggle0D1Action() {
        if (toggle0D1.isSelected) {
            toggle0D1.text = ""
        } else {
            toggle0D1.text = "0D1"
        }
    }

    fun buttonAddPartDoneAction() {

        val stage = buttonAddPartDone.scene.window as Stage
        stage.close()
    }

}