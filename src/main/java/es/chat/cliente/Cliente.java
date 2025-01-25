package es.chat.cliente;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Clase principal de la aplicación cliente. Inicia la interfaz gráfica.
 * @version 1.0
 * @author Adrián González
 */
public class Cliente extends Application {
    /*
     * Hilo que escucha los mensajes del servidor, se interrumpe al cerrar la interfaz.
     * Se inicializa y ejecuta en iniciar() de ClienteController.
     */
    public static Thread hiloEscucha;
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Cliente.class.getResource("cliente-run.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        stage.setTitle("Chat");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // Cerrar hilo al cerrar la ventana
        stage.setOnCloseRequest(e -> {
            hiloEscucha.interrupt();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
