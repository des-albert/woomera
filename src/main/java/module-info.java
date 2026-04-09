module org.dba.woomera {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires org.slf4j;
    requires org.mongodb.driver.kotlin.coroutine;
    requires com.google.gson;

    opens org.dba.woomera to javafx.fxml, com.google.gson;
    exports org.dba.woomera;
}