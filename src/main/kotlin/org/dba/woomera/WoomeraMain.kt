package org.dba.woomera

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class WoomeraMain : Application() {
    override fun start(stage: Stage) {

        MongoManage.connect()

        val fxmlLoader = FXMLLoader(WoomeraMain::class.java.getResource("woomera.fxml"))
        val scene = Scene(fxmlLoader.load(), 1280.0, 830.0)
        val icon = Image(javaClass.getResourceAsStream("/img/woomera.png"))
        stage.icons.add(icon)
        stage.title = "Woomera"
        stage.scene = scene
        stage.show()
    }
}

fun main() {
    Application.launch(WoomeraMain::class.java)
}
