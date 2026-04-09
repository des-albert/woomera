package org.dba.woomera

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters.eq
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.TabPane
import javafx.scene.control.TextInputDialog
import javafx.scene.control.ToggleButton
import javafx.scene.control.Tooltip
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.scene.input.Dragboard
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.Optional
import kotlin.code
import kotlin.compareTo
import kotlin.toString

class Woomera {

    lateinit var toggleBuild: ToggleButton
    lateinit var togglePart: ToggleButton
    lateinit var labelFileStatus: Label

    @FXML
    lateinit var labelStatus: Label

    @FXML
    lateinit var imageViewTrash: ImageView

    @FXML
    lateinit var buttonLoadProduct: Button

    @FXML
    lateinit var buttonQuit: Button

    @FXML
    lateinit var comboBoxProduct: ComboBox<String>

    @FXML
    lateinit var tabPaneMain: TabPane

    @FXML
    lateinit var labelJDK: Label

    @FXML
    lateinit var labelJavaFX: Label

    @FXML
    lateinit var treeViewPart: TreeView<Any>

    @FXML
    lateinit var treeViewBuild: TreeView<Any>

    companion object {

        val partHashMap: HashMap<String, Part> = HashMap()
        val slotHashMap: HashMap<String, Slot> = HashMap()
        var selectedTreeItem: TreeItem<Any>? = null
        var newData: Boolean = false
    }

    private val dfPart: DataFormat = DataFormat("Part")
    private val dfSlot: DataFormat = DataFormat("Slot")
    private val dfParentPart: DataFormat = DataFormat("ParentPart")
    private val dfParentSlot: DataFormat = DataFormat("ParentSlot")
    private val dfBuild: DataFormat = DataFormat("BuildPart")

    val folderContext: ContextMenu = ContextMenu()
    val partContext: ContextMenu = ContextMenu()
    val quantityContext: ContextMenu = ContextMenu()

    val buildHashMap: HashMap<String, TreeItem<Any>> = HashMap()
    val catHashMap: HashMap<String, TreeItem<Any>> = HashMap()

    val logger: Logger = LoggerFactory.getLogger("Woomera")

    var parts: List<Part> = emptyList()
    var slots: List<Slot> = emptyList()
    var products: List<String> = emptyList()
    var selectedProduct: String = ""
    var productParts: String = ""
    var productSlots: String = ""

    private val controllerScope = CoroutineScope(Dispatchers.JavaFx + SupervisorJob())
    private lateinit var partTreeRootItem: TreeItem<Any>
    private lateinit var buildTreeRootItem: TreeItem<Any>

    @FXML
    fun initialize() {
        labelJDK.text = "Java SDK %s".format(System.getProperty("java.version"))
        labelJavaFX.text = "JavaFX version %s".format(System.getProperty("javafx.runtime.version"))


        controllerScope.launch {
            val collections = getProducts()
            products = collections
                .filter { it.startsWith("Parts") }
                .map { it.substringAfter("-") }
            comboBoxProduct.items.clear()
            comboBoxProduct.items = FXCollections.observableArrayList(products)

        }

        definePartsTreeView()
        defineTrash()
        defineBuildTreeView()
        addContext()

    }

    suspend fun getProducts(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val names = MongoManage.database.listCollectionNames().toList()
                names

            } catch (e: Exception) {
                println("Error loading Product Collections : ${e.message}")
                emptyList()
            }
        }
    }

    @FXML
    fun buttonLoadProductOnAction() {
        selectedProduct = comboBoxProduct.value
        productParts = "Parts-$selectedProduct"
        productSlots = "Slots-$selectedProduct"

        controllerScope.launch {
            parts = getParts(productParts)
            slots = getSlots(productSlots)


            for (part in parts) {
                partHashMap[part.code] = part
            }

            for (slot in slots) {
                slotHashMap[slot.name] = slot
            }

            loadPartTree()

        }
    }

    suspend fun getParts(collection: String): List<Part> {
        return withContext(Dispatchers.IO) {
            try {
                val partCollection = MongoManage.database.getCollection<Part>(collection)
                partCollection.find().toList()

            } catch (e: Exception) {
                logger.error("Error loading Part Collection : ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun getSlots(collection: String): List<Slot> {
        return withContext(Dispatchers.IO) {
            try {
                val slotCollection = MongoManage.database.getCollection<Slot>(collection)
                slotCollection.find().toList()

            } catch (e: Exception) {
                logger.error("Error loading Slot Collections : ${e.message}")
                emptyList()
            }
        }
    }

    @FXML
    fun partDragDetected(event: MouseEvent) {

        val selectedItem = treeViewPart.selectionModel.selectedItem
        if (event.isDragDetect) {
            if (selectedItem.value != null) {
                val dataToDrag: Any = selectedItem.value
                val db: Dragboard = treeViewPart.startDragAndDrop(TransferMode.COPY)
                val content = ClipboardContent()

                when (dataToDrag) {

                    // Drag a Part

                    is Part -> {
                        content[dfPart] = dataToDrag
                        selectedItem.parent?.value?.let { parent ->
                            if (parent !is Folder) {
                                content[dfParentSlot] = parent
                            }
                        }
                    }

                    // Drag a Slot

                    is Slot -> {
                        content[dfSlot] = dataToDrag
                        selectedItem.parent?.value?.let { parent ->
                            if (parent is Part) {
                                content[dfParentPart] = parent
                            }
                        }
                    }
                }
                db.setContent(content)
            }
        }
        event.consume()
    }

    @FXML
    fun buildDragDetected(event: MouseEvent) {
        val selectedItem = treeViewBuild.selectionModel.selectedItem
        if (event.isDragDetect) {
            if (selectedItem.value != null) {
                val dataToDrag: Any = selectedItem.value
                val db: Dragboard = treeViewBuild.startDragAndDrop(TransferMode.COPY)
                val content = ClipboardContent()

                when (dataToDrag) {
                    is BuildPart -> {
                        content[dfBuild] = dataToDrag
                        selectedItem.parent?.value?.let { parent ->
                            if (parent !is Folder) {
                                content[dfParentSlot] = parent
                            }
                        }
                    }

                    is BuildSlot -> {
                        labelStatus.text = "build slot cannot be removed"
                        labelStatus.styleClass.clear()
                        labelStatus.styleClass.add("label-failure")
                    }
                }
                db.setContent(content)
            }
            event.consume()
        }
    }

    // Initialize Parts treeView

    private fun definePartsTreeView() {
        val partRoot = Folder("Root Parts Folder")
        partTreeRootItem = TreeItem<Any>(partRoot)
        treeViewPart.root = partTreeRootItem
        treeViewPart.isShowRoot = false

        // Cell Factory for Parts treeView

        treeViewPart.cellFactory = Callback { _ ->
            val cell = object : TreeCell<Any>() {
                override fun updateItem(item: Any?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        text = null
                        graphic = null
                        tooltip = null

                    } else {
                        when (item) {
                            is Folder -> {
                                val folder: Folder = item
                                text = folder.category
                                style = "-fx-text-fill: folder-leaf-color"
                                contextMenu = folderContext
                            }

                            is Part -> {
                                val part: Part = item
                                text = part.name
                                graphic = treeItem.graphic
                                val parent = this.treeItem.parent.value
                                style = if (parent is Folder)
                                    "-fx-text-fill: part-leaf-color"
                                else
                                    "-fx-text-fill: part-link-color"
                                tooltip = Tooltip(part.code + " - " + part.name)
                                contextMenu = partContext
                            }

                            is Slot -> {
                                val slot: Slot = item
                                text = slot.name
                                style = "-fx-text-fill: slot-leaf-color"
                                tooltip = Tooltip(slot.type + "-" + slot.quantity + " - " + slot.description)
                            }
                        }
                    }
                }
            }
            cell.setOnDragOver { event ->
                event.acceptTransferModes(TransferMode.COPY)
                event.consume()
            }
            cell.setOnDragDropped { event ->
                when (val target = cell.treeItem.value) {
                    is Slot -> {

                        // Add part to slot

                        if (event.dragboard.hasContent(dfPart)) {
                            val part: Part = event.dragboard.getContent(dfPart) as Part
                            val code = part.code
                            if (!target.parts.contains(code)) {
                                target.parts.add(code)
                                slotHashMap[target.name] = target
                                loadPartTree()
                                labelStatus.text = "part $code added to slot ${target.name}"
                                labelStatus.styleClass.clear()
                                labelStatus.styleClass.add("label-success")
                            }
                        }
                    }

                    // Add slot to part

                    is Part -> {
                        if (event.dragboard.hasContent(dfSlot)) {
                            val slot: Slot = event.dragboard.getContent(dfSlot) as Slot
                            if (!target.slots.contains(slot.name)) {
                                target.slots.add(slot.name)
                                partHashMap[target.code] = target
                                loadPartTree()
                                labelStatus.text = "slot ${slot.name} added to part ${target.code}"
                                labelStatus.styleClass.clear()
                                labelStatus.styleClass.add("label-success")
                            }
                        }
                    }
                }
                event.isDropCompleted = true
                event.consume()
            }
            return@Callback cell
        }
    }

    // Trash handler

    fun defineTrash() {

        imageViewTrash.setOnDragOver { event ->
            event.acceptTransferModes(TransferMode.COPY)
            event.consume()
        }

        imageViewTrash.setOnDragEntered { event ->
            val iconPath = "/img/glass trash.png"
            val icon = Image(javaClass.getResourceAsStream(iconPath))
            imageViewTrash.image = icon
            event.consume()
        }

        imageViewTrash.setOnDragExited { event ->
            val iconPath = "/img/trash.png"
            val icon = Image(javaClass.getResourceAsStream(iconPath))
            imageViewTrash.image = icon
            event.consume()
        }

        imageViewTrash.setOnDragDropped { event ->

            if (event.dragboard.hasContent(dfPart)) {
                if (event.dragboard.hasContent(dfParentSlot)) {

                    // Remove Part from Slot in Part TreeView

                    val part: Part = event.dragboard.getContent(dfPart) as Part
                    val slot: Slot = event.dragboard.getContent(dfParentSlot) as Slot
                    val slotParts = slot.parts
                    val code = part.code
                    if (slotParts.contains(code)) {
                        slotParts.remove(code)
                        slotHashMap[slot.name] = slot
                        loadPartTree()
                    }
                    labelStatus.text = "part $code removed from slot ${slot.name}"
                    labelStatus.styleClass.clear()
                    labelStatus.styleClass.add("label-success")

                    // Delete Part

                } else {
                    val part: Part = event.dragboard.getContent(dfPart) as Part
                    val code = part.code
                    partHashMap.remove(code)

                    // Remove part from containing slots

                    for (slot in slotHashMap.values) {
                        if (slot.parts.contains(code)) {
                            val slotParts = slot.parts
                            slotParts.remove(code)
                            slotHashMap[slot.name] = slot
                        }
                    }
                    labelStatus.text = "part $code removed"
                    labelStatus.styleClass.clear()
                    labelStatus.styleClass.add("label-success")

                    loadPartTree()
                }
            } else if (event.dragboard.hasContent(dfSlot)) {

                // Remove Slot from Part

                val slot: Slot = event.dragboard.getContent(dfSlot) as Slot
                val part: Part = event.dragboard.getContent(dfParentPart) as Part
                val partSlots = part.slots
                partSlots.remove(slot.name)
                loadPartTree()


                // Remove Build Part

            } else if (event.dragboard.hasContent(dfBuild)) {

                val part: BuildPart = event.dragboard.getContent(dfBuild) as BuildPart
                val slot: BuildSlot = event.dragboard.getContent(dfParentSlot) as BuildSlot
                val parentItem =
                    if (buildHashMap.containsKey(slot.name)) buildHashMap[slot.name] else null
                val partItem =
                    if (buildHashMap.containsKey(part.code)) buildHashMap[part.code] else null
                if (parentItem != null && partItem != null && parentItem.children.contains(partItem)) {
                    parentItem.children?.remove(partItem)
                    buildHashMap.remove(part.code)

                    labelStatus.text = "${slot.content} parts removed from slot ${slot.name}"
                    labelStatus.styleClass.clear()
                    labelStatus.styleClass.add("label-success")

                    slot.content = 0
                    parentItem.value = slot
                    treeViewBuild.refresh()

                } else {
                    labelStatus.text = "${part.code} not found in slot ${slot.name}"
                    labelStatus.styleClass.clear()
                    labelStatus.styleClass.add("label-failure")
                }

            }
            event.isDropCompleted
            event.consume()
        }
    }

    // Build treeView

    fun defineBuildTreeView() {

        val slots = ArrayList<String>()
        slots.add("build")
        val buildPart = BuildPart("build", "build", "build", "", slots, false)

        buildTreeRootItem = TreeItem<Any>(buildPart)
        treeViewBuild.root = buildTreeRootItem
        treeViewBuild.isShowRoot = true

        treeViewBuild.cellFactory = Callback { _ ->
            val cell = object : TreeCell<Any>() {
                override fun updateItem(item: Any?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        text = null
                        graphic = null
                        tooltip = null

                    } else {
                        when (item) {
                            is BuildPart -> {
                                val part: BuildPart = item
                                tooltip = Tooltip(part.totalCount.toString() + " x " + part.name)
                                text = if (part.buildCount > 1) {
                                    "${part.buildCount} x ${part.name}"
                                } else {
                                    part.name
                                }
                                style = "-fx-text-fill: part-leaf-color"
                                graphic = treeItem.graphic
                                contextMenu = quantityContext
                            }

                            is BuildSlot -> {
                                val slot: BuildSlot = item
                                text = slot.name
                                style = "-fx-text-fill: slot-leaf-color"
                                val parts = slot.parts
                                var toolText = StringBuilder()
                                if (slot.type == "E")
                                    toolText = StringBuilder("exact - " + slot.quantity)
                                else if (slot.type == "M")
                                    toolText = StringBuilder("max - " + slot.quantity)
                                toolText.append(" current - ")
                                toolText.append(slot.content)
                                toolText.append("\n")
                                for (code in parts) {
                                    val slotPart = partHashMap[code]
                                    toolText.append(slotPart!!.name)
                                    toolText.append("\n")
                                }
                                tooltip = Tooltip(toolText.toString())
                            }
                        }
                    }
                }
            }
            cell.setOnDragOver { event ->
                event.acceptTransferModes(TransferMode.COPY)
                event.consume()
            }

            cell.setOnDragDropped { event ->
                val targetItem = cell.treeItem
                if (event.dragboard.hasContent(dfPart)) {
                    val part: Part = event.dragboard.getContent(dfPart) as Part

                    // Add Part to Build Base

                    when (val target = targetItem.value) {
                        is BuildPart -> {
                            val baseSlot = slotHashMap["build"]
                            if (baseSlot?.parts?.contains(part.code) == true) {

                                val targetPart: BuildPart = target
                                var addQty = 0
                                if (baseSlot.type == "U") {
                                    val countDialog = TextInputDialog()
                                    countDialog.headerText = "enter part quantity : "
                                    val addCount: Optional<String> = countDialog.showAndWait()
                                    if (addCount.isPresent)
                                        addQty = addCount.get().toInt()
                                }
                                val addPart = BuildPart(part)
                                addPart.buildCount = addQty
                                addPart.totalCount = addQty
                                addPart.parent = targetPart.code
                                val partTreeItem = newTreeItem(addPart)
                                buildHashMap[part.code] = partTreeItem
                                buildTreeRootItem.children.add(partTreeItem)

                                if ( ! addPart.slots.isEmpty()) {
                                    addSlotParts(addPart, partTreeItem)
                                }

                                labelStatus.text = "$addQty ${addPart.code} parts added to base"
                                labelStatus.styleClass.clear()
                                labelStatus.styleClass.add("label-success")

                            }

                        }

                        // Add Part to Slot

                        is BuildSlot -> {
                            val targetSlot: BuildSlot = target
                            if (!targetSlot.parts.isEmpty() && targetSlot.content == 0) {
                                val slotParts = targetSlot.parts
                                if (slotParts.contains(part.code)) {
                                    var addQty = 0
                                    val addPart = BuildPart(part)
                                    val currentQty = targetSlot.content
                                    val maxQty = targetSlot.quantity

                                    if (targetSlot.type == "E") {
                                        addQty = targetSlot.quantity
                                    } else if (targetSlot.type == "M" || currentQty < maxQty) {
                                        val countDialog = TextInputDialog()
                                        val limitQty = maxQty - currentQty
                                        if (targetSlot.type == "U")
                                            countDialog.headerText = "enter part quantity : "
                                        else
                                            countDialog.headerText = "enter part quantity <= $limitQty : "
                                        val addCount: Optional<String> = countDialog.showAndWait()
                                        if (addCount.isPresent)
                                            addQty = addCount.get().toInt()
                                        if (targetSlot.type == "M" && addQty > limitQty)
                                            addQty = limitQty
                                    }

                                    addPart.buildCount = addQty
                                    val targetPart: BuildPart = targetItem.parent.value as BuildPart
                                    addPart.totalCount = targetPart.totalCount * addQty
                                    targetSlot.content = currentQty + addQty
                                    addPart.parent = targetSlot.name
                                    val partTreeItem = newTreeItem(addPart)
                                    buildHashMap[addPart.code] = partTreeItem
                                    targetItem.children.add(partTreeItem)

                                    // Add slots to new part

                                    if ( ! addPart.slots.isEmpty()) {
                                        addSlotParts(addPart, partTreeItem)
                                    }

                                    labelStatus.text =
                                        "$addQty ${addPart.code} parts added to slot ${targetSlot.name}"
                                    labelStatus.styleClass.clear()
                                    labelStatus.styleClass.add("label-success")


                                }
                            } else {
                                labelStatus.text = "part ${part.code} cannot be added to slot ${targetSlot.name}"
                                labelStatus.styleClass.clear()
                                labelStatus.styleClass.add("label-failure")
                            }
                        }
                    }
                }
                event.isDropCompleted
                event.consume()
            }
            return@Callback cell
        }
    }

    private fun addSlotParts(addPart: BuildPart, item: TreeItem<Any>) {
        for (slotName: String in addPart.slots) {
            val slot = slotHashMap[slotName]
            val addSlot = BuildSlot(slot!!)
            addSlot.parent = addPart.code
            val slotTreeItem = newTreeItem(addSlot)
            buildHashMap[slotName] = slotTreeItem
            item.children.add(slotTreeItem)
        }
    }

    private fun addContext() {

        // Create new part

        val newPart = MenuItem("create part")
        newPart.setOnAction {
            selectedTreeItem = treeViewPart.selectionModel.selectedItem
            try {
                val fxmlLoader = FXMLLoader(javaClass.getResource("createPart.fxml"))
                val partForm: Parent = fxmlLoader.load()
                val partStage = Stage()
                partStage.title = "Create a part"
                partStage.setOnHiding {
                    loadPartTree()
                }
                partStage.scene = Scene(partForm)
                partStage.show()
            } catch (e: IOException) {
                labelStatus.text = e.message
                labelStatus.styleClass.clear()
                labelStatus.styleClass.add("label-failure")
            }
        }

        // Create new slot context menu

        val newSlot = MenuItem("create slot")
        newSlot.setOnAction {
            selectedTreeItem = treeViewPart.selectionModel.selectedItem
            try {
                val fxmlLoader = FXMLLoader(javaClass.getResource("createSlot.fxml"))
                val slotForm: Parent = fxmlLoader.load()
                val slotStage = Stage()
                slotStage.title = "Create a slot"
                slotStage.setOnHiding {
                    loadPartTree()
                }
                slotStage.scene = Scene(slotForm)
                slotStage.show()
            } catch (e: IOException) {
                labelStatus.text = e.message
                labelStatus.styleClass.clear()
                labelStatus.styleClass.add("label-failure")
            }
        }

        // Create a context menu to change build quantity

        val partQty = MenuItem("change quantity ")
        partQty.setOnAction {
            selectedTreeItem = treeViewBuild.selectionModel.selectedItem
            try {
                val fxmlLoader = FXMLLoader(javaClass.getResource("changeQty.fxml"))
                val qtyForm: Parent = fxmlLoader.load()
                val qtyStage = Stage()

                qtyStage.setOnHiding {
                    loadBuildTree()
                }
                qtyStage.title = "Change part quantity"
                qtyStage.scene = Scene(qtyForm)
                qtyStage.show()

            } catch (e: IOException) {
                labelStatus.text = e.message
                labelStatus.styleClass.clear()
                labelStatus.styleClass.add("label-failure")
            }
        }

        folderContext.items.add(newPart)
        partContext.items.add(newSlot)
        quantityContext.items.add(partQty)
    }

    private fun loadPartTree() {

        partTreeRootItem.children.clear()
        catHashMap.clear()
        var treeItem: TreeItem<Any>
        var slot: Slot
        var category: String

        // Add Parts to partTreeView

        for (part in partHashMap.values) {
            category = part.category
            if (!catHashMap.containsKey(category)) {
                val folder = Folder(category)
                treeItem = TreeItem(folder)
                catHashMap[category] = treeItem
                partTreeRootItem.children.add(treeItem)
            } else {
                treeItem = catHashMap[category]!!
            }

            val leafItem = newTreeItem(part)
            treeItem.children.add(leafItem)

            // Add Slots to Part

            if (!part.slots.isEmpty()) {
                for (code: String in part.slots) {
                    if (slotHashMap.containsKey(code)) {
                        slot = slotHashMap[code]!!
                        val slotItem: TreeItem<Any> = newTreeItem(slot)
                        leafItem.children.add(slotItem)

                        // Add Parts to each Slot

                        if (!slot.parts.isEmpty()) {
                            for (code: String in slot.parts) {
                                val slotPart: Part = partHashMap[code]!!
                                val slotPartItem: TreeItem<Any> = newTreeItem(slotPart)
                                slotItem.children.add(slotPartItem)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun newTreeItem(value: Any): TreeItem<Any> {
        var treeItem: TreeItem<Any>
        when (value) {
            is Part -> {
                val part: Part = value
                val image = part.category
                treeItem = itemIcon(part, image)
            }

            is BuildPart -> {
                val buildPart: BuildPart = value
                val image = buildPart.category
                treeItem = itemIcon(buildPart, image)
            }

            else ->
                treeItem = TreeItem<Any>(value)
        }
        return treeItem
    }

    private fun itemIcon(item: Any, image: String): TreeItem<Any> {
        var treeItem: TreeItem<Any>
        val iconPath = "/img/$image.png"
        val iconStream = javaClass.getResourceAsStream(iconPath)
        if (iconStream != null) {
            val icon = Image(iconStream)
            treeItem = TreeItem(item, ImageView(icon))
        } else {
            treeItem = TreeItem(item)
        }
        return treeItem
    }

    private fun loadBuildTree() {
        for (item in buildTreeRootItem.children) {
            item.isExpanded
        }
        treeViewBuild.refresh()
    }

    fun buttonQuitOnAction() {
        controllerScope.launch {
            if (newData) {
                saveParts(productParts)
                saveSlots(productSlots)
            }

            val stage: Stage = buttonQuit.scene.window as Stage
            stage.close()

        }
    }

    suspend fun saveParts(collection: String) {
        withContext(Dispatchers.IO) {
            try {
                val partCollection = MongoManage.database.getCollection<Part>(collection)
                for (part in partHashMap.values) {
                    if (partCollection.find(eq("code", part.code)).firstOrNull() == null) {
                        partCollection.insertOne(part)
                        logger.info("Part ${part.code} saved ")
                    }
                }
            } catch (e: MongoWriteException) {
                    logger.error("Error saving Parts Collection : ${e.message}")

            }
        }
    }

    suspend fun saveSlots(collection: String) {
        withContext(Dispatchers.IO) {
            try {
                val slotCollection = MongoManage.database.getCollection<Slot>(collection)
                for (slot in slotHashMap.values) {
                    if (slotCollection.find(eq("name", slot.name)).firstOrNull() == null) {
                        slotCollection.insertOne(slot)
                        logger.info("Slot ${slot.name} saved ")
                    }
                }
            } catch (e: MongoWriteException) {
                logger.error("Error saving Slots Collection : ${e.message}")

            }
        }
    }

    fun buttonExportOnAction() {
        val exportList = mutableListOf<BuildPart>()

        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("export")
            val fileChooser = FileChooser()
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Excel Files", "*.xlsx"))
            fileChooser.title = "save config as .xlsx file"
            val exportFile = fileChooser.showSaveDialog(tabPaneMain.scene.window)
            if (exportFile.exists()) {
                if (!exportFile.delete()) {
                    labelFileStatus.text = "file delete error $exportFile"
                    labelFileStatus.styleClass.clear()
                    labelFileStatus.styleClass.add("label-failure")
                }
            }
            if (exportFile.createNewFile()) {

                lateinit var cell: XSSFCell

                exportList.clear()
                exportTree(buildTreeRootItem, exportList)

                val centeredStyle: XSSFCellStyle = workbook.createCellStyle()
                centeredStyle.alignment = HorizontalAlignment.CENTER

                var rowIndex = 0
                exportList.forEach { part ->
                    val od1 = part.od1
                    if (part.totalCount > 0) {
                        var row = sheet.createRow(rowIndex)
                        cell = row.createCell(0)
                        cell.setCellValue(part.totalCount.toString())
                        cell.cellStyle = centeredStyle
                        cell = row.createCell(1)
                        cell.setCellValue(part.code)
                        cell = row.createCell(2)
                        cell.setCellValue(part.description)
                        rowIndex++
                        if (od1) {
                            row = sheet.createRow(rowIndex)
                            val code = part.code.padEnd(13, ' ') + "0D1"
                            cell = row.createCell(0)
                            cell.setCellValue(part.totalCount.toString())
                            cell.cellStyle = centeredStyle
                            cell = row.createCell(1)
                            cell.setCellValue(code)
                            cell = row.createCell(2)
                            cell.setCellValue("Factory Integrated")
                            rowIndex++
                        }
                    }
                }
                val outputStream = FileOutputStream(exportFile)
                workbook.write(outputStream)
                workbook.close()
            }
            labelFileStatus.text = "file exported to $exportFile"
            labelFileStatus.styleClass.clear()
            labelFileStatus.styleClass.add("label-success")

        } catch (e: IOException) {
            logger.error("export - {}", e.message)
        }
    }

    private fun exportTree(item: TreeItem<Any>, list: MutableList<BuildPart>) {

        if (item.value is BuildPart) {
            item.value?.let {
                list.add(it as BuildPart)
            }
        }

        for (childItem: TreeItem<Any> in item.children) {
            exportTree(childItem, list)
        }
    }

    fun buttonSaveConfigOnAction() {
        val fileChooser = FileChooser().apply {
            extensionFilters.add(FileChooser.ExtensionFilter("json file (*.json)", "*.json"))
            title = "save config as JSON file"
            initialFileName = "build_config.json"
        }
        val jsonFile = fileChooser.showSaveDialog(tabPaneMain.scene.window)
        try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonArray = JsonArray()
            if (jsonFile.exists()) {
                if (!jsonFile.delete()) {
                    labelFileStatus.text = "file delete error $jsonFile"
                    labelFileStatus.styleClass.clear()
                    labelFileStatus.styleClass.add("label-failure")
                }
            }
            if (jsonFile.createNewFile()) {

                addTreeItemsToJsonArray(buildTreeRootItem, jsonArray, gson)

                FileWriter(jsonFile).use { fw ->
                    BufferedWriter(fw).use { bw ->
                        gson.toJson(jsonArray, bw)
                    }
                }
            }
            labelFileStatus.text = "Configuration saved to ${jsonFile.name}"
            labelFileStatus.styleClass.clear()
            labelFileStatus.styleClass.add("label-success")

        } catch (e: IOException) {
            logger.error("save config - {}", e.message)
        }
    }

    private fun addTreeItemsToJsonArray(item: TreeItem<Any>, jsonArray: JsonArray, gson: Gson) {
        try {
            item.value?.let {
                val jsonElement = gson.toJsonTree(it)
                jsonArray.add(jsonElement)
            }

            for (child in item.children) {
                addTreeItemsToJsonArray(child, jsonArray, gson)
            }
        } catch (e: IOException) {
            logger.error("save Tree - ${item.value} {}", e.message)
        }
    }

    fun buttonLoadConfigOnAction() {

        val fileChooser = FileChooser().apply {
            extensionFilters.add(FileChooser.ExtensionFilter("json file (*.json)", "*.json"))
            title = "open config JSON file"
        }
        val jsonFile = fileChooser.showOpenDialog(tabPaneMain.scene.window)
        val gson = Gson()
        try {
            FileReader(jsonFile).use { fr ->
                BufferedReader(fr).use { br ->
                    readTree(br, gson)
                }
            }

        } catch (e: IOException) {
            logger.error("load config - {}", e.message)
        }

        labelFileStatus.text = "Configuration loaded from ${jsonFile.name}"
        labelFileStatus.styleClass.clear()
        labelFileStatus.styleClass.add("label-success")

        treeViewBuild.refresh()
    }

    private fun readTree(br: BufferedReader, gson: Gson) {
        buildTreeRootItem.children.clear()
        buildHashMap.clear()

        try {
            val jsonReader = JsonReader(br)
            jsonReader.beginArray()
            while (jsonReader.hasNext()) {
                val jsonElement = JsonParser.parseReader(jsonReader)
                val jsonObject = jsonElement.asJsonObject

                if (jsonObject.has("code")) {
                    val buildPart = gson.fromJson<BuildPart>(jsonObject, BuildPart::class.java)
                    if (buildPart.code == "build") {
                        buildTreeRootItem = TreeItem<Any>(buildPart)
                        buildHashMap[buildPart.code] = buildTreeRootItem
                        treeViewBuild.root = buildTreeRootItem
                        treeViewBuild.isShowRoot = true
                    } else {
                        val partItem = newTreeItem(buildPart)
                        buildHashMap[buildPart.code] = partItem
                        val parentItem = buildHashMap[buildPart.parent]
                        parentItem?.children?.add(partItem)
                    }
                } else if (jsonObject.has("name")) {
                    val slot = gson.fromJson<BuildSlot>(jsonObject, BuildSlot::class.java)
                    val slotItem = newTreeItem(slot)
                    buildHashMap[slot.name] = slotItem
                    val parentItem = buildHashMap[slot.parent]
                    parentItem?.children?.add(slotItem)
                }
            }
            jsonReader.endArray()
            jsonReader.close()


        } catch (e: IOException) {
            logger.error("readTree - {}", e.message)
        }
    }

    @FXML fun buttonPartCollapseOnAction() {
        if (togglePart.isSelected) {
            for (item: TreeItem<Any> in partTreeRootItem.children) {
                item.isExpanded = true
            }
            togglePart.text = "collapse"
        } else {
            for (item: TreeItem<Any> in partTreeRootItem.children) {
                item.isExpanded = false
            }
            togglePart.text = "expand"
        }
    }


    @FXML fun buttonBuildCollapseOnAction() {
        if (toggleBuild.isSelected) {
            expandTreeView(buildTreeRootItem, true)
            toggleBuild.text = "collapse"
        } else {
            expandTreeView(buildTreeRootItem, false)
            toggleBuild.text = "expand"
        }
    }

    private fun expandTreeView(item: TreeItem<Any>?, expand: Boolean) {
        if (item != null && !item.isLeaf) {
            item.isExpanded = true
        }
        for (child in item?.children!!) {
            expandTreeView(child, expand)
        }
    }

}