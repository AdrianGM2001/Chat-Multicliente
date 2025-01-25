module es.chat.cliente {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires javafx.graphics;

    opens es.chat.cliente to javafx.fxml;
    exports es.chat.cliente;

    exports es.chat.controlador;
    opens es.chat.controlador to javafx.fxml;
}