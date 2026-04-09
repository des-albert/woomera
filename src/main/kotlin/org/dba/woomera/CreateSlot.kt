package org.dba.woomera

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.RadioButton
import javafx.scene.control.TextField
import javafx.scene.control.ToggleGroup
import javafx.stage.Stage
import org.bson.types.ObjectId
import org.dba.woomera.Woomera.Companion.partHashMap
import org.dba.woomera.Woomera.Companion.slotHashMap
import org.dba.woomera.Woomera.Companion.selectedTreeItem
import org.dba.woomera.Woomera.Companion.newData

class CreateSlot {
    @FXML
    lateinit var CountGroup: ToggleGroup
    @FXML
    lateinit var textFieldSlotName: TextField
    @FXML
    lateinit var textFieldSlotDescription: TextField
    @FXML
    lateinit var textFieldSlotCount: TextField
    @FXML
    lateinit var radioButtonMax: RadioButton
    @FXML
    lateinit var radioButtonExact: RadioButton
    @FXML
    lateinit var radioButtonUnlimited: RadioButton
    @FXML
    lateinit var buttonAddSlot: Button
    @FXML
    lateinit var buttonCreateSlotDone: Button
    @FXML
    lateinit var labelCreateSlotStatus: Label

    fun initialize() {

    }

    fun buttonAddSlotOnAction() {
        val name = textFieldSlotName.text
        val description = textFieldSlotDescription.text
        val type = CountGroup.selectedToggle.userData.toString()
        var count = 0

        if (name.isEmpty() || type.isEmpty() || textFieldSlotCount.text.isEmpty()) {
            labelCreateSlotStatus.text = "Fields cannot be null"
            labelCreateSlotStatus.style = "-fx-text-fill: status-error-color"
            return
        }
        if (slotHashMap.containsKey(name)) {
            labelCreateSlotStatus.text = "Duplicate slot name %s".format(name)
            labelCreateSlotStatus.style = "-fx-text-fill: status-error-color"
            return
        }
        if (radioButtonUnlimited.isSelected) {
            count = 0
        } else {
            count = textFieldSlotCount.text.toInt()
        }
        val parts: ArrayList<String> = ArrayList()
        val newSlot = Slot(ObjectId(), name, type, count, description, parts)

        slotHashMap[newSlot.name] = newSlot
        val part: Part = selectedTreeItem?.value as Part
        part.slots.add(newSlot.name)
        partHashMap[part.code] = part

        labelCreateSlotStatus.text = "New Slot %s - %s created".format(name, description)
        labelCreateSlotStatus.style = "-fx-text-fill: status-good-color"

        newData = true
    }


    fun buttonCreateSlotDoneOnAction() {
        val stage = buttonCreateSlotDone.scene.window as Stage
        stage.close()
    }

}