package es.chat.controlador;

import es.chat.cliente.Cliente;
import es.chat.cliente.EscuchaHilo;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import es.chat.modelo.comando.CliCmd;
import es.chat.modelo.comando.ServCmd;

/**
 * Controlador de la vista de cliente. Se encarga de gestionar la interfaz gráfica y la comunicación con el servidor.
 * Inicia el hilo de escucha de mensajes del servidor y envía mensajes al servidor.
 * @see EscuchaHilo
 * @see Cliente
 * @see CliCmd
 * @version 1.0
 * @author Adrián González
 */
public class ClienteController implements Initializable {
    private Socket socketCliente;
    private DataOutputStream salida;

    @FXML
    private TextField aliasIntroducido;

    @FXML
    private Label mensajeEstado;

    @FXML
    private Circle estado;

    @FXML
    private Button botConexion;

    @FXML
    private ListView<String> listaUsuarios;

    @FXML
    private TextArea mensajes;

    @FXML
    private TextField mensajeIntroducido;

    @FXML
    private HBox chat;

    /**
     * Inicializa la vista de cliente. Oculta el chat y añade un listener al click de la lista de usuarios.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        chat.setVisible(false);
        listaUsuarios.setOnMouseClicked(event -> onUsuarioClick(listaUsuarios.getSelectionModel().getSelectedItem()));
        iniciar();
    }

    /**
     * Inicia la conexión con el servidor y el hilo de escucha de mensajes del servidor.
     * Muestra un mensaje de error si no se puede conectar con el servidor.
     * @see EscuchaHilo
     * @see Cliente
     */
    private void iniciar() {
        try {
            socketCliente = new Socket("localhost", 4444);
            DataInputStream entrada = new DataInputStream(socketCliente.getInputStream());
            salida = new DataOutputStream(socketCliente.getOutputStream());
            Cliente.hiloEscucha = new Thread(new EscuchaHilo(entrada, this));
            Cliente.hiloEscucha.start();
        } catch (IOException e) {
            mensajeEstado.setText("Error al conectar con el servidor");
        }
    }

    /**
     * Envía un mensaje al servidor para conectarse con un alias.
     */
    @FXML
    private void onConectarClick() {
        try {
            salida.writeUTF(String.format("%s %s", CliCmd.CON, aliasIntroducido.getText()));
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    /**
     * Cambia el estado de la conexión y muestra un mensaje de error en la interfaz gráfica.
     * @param mensaje Mensaje a mostrar en la interfaz gráfica.
     */
    public void recibirNOK(String mensaje) {
        cambiarEstado(ServCmd.NOK, mensaje);
    }

    /**
     * Cambia el estado de la conexión y muestra un mensaje de éxito en la interfaz gráfica.
     * @param mensaje Mensaje a mostrar en la interfaz gráfica.
     */
    public void recibirOK(String mensaje) {
        try {
            salida.writeUTF(CliCmd.LUS.name());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }

        cambiarEstado(ServCmd.OK, mensaje);
    }

    /**
     * Cambia el estado de la conexión y muestra el mensaje correspondiente en la interfaz gráfica.
     * @param comando Comando recibido del servidor.
     * @param mensaje Mensaje a mostrar en la interfaz gráfica.
     */
    private void cambiarEstado(ServCmd comando, String mensaje) {
        boolean conexionEstablecida = comando == ServCmd.OK;

        chat.setVisible(conexionEstablecida);
        aliasIntroducido.setEditable(!conexionEstablecida);
        estado.setStyle(conexionEstablecida ? "-fx-fill: green;" : "-fx-fill: red;");
        mensajeEstado.setText(mensaje);
        mensajeEstado.setStyle(conexionEstablecida ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        botConexion.setText(conexionEstablecida ? "Desconectar" : "Conectar");
        botConexion.setOnAction(event -> {
            if (conexionEstablecida) {
                onDesconectarClick();
            } else {
                onConectarClick();
            }
        });
    }

    /**
     * Recibe una lista de usuarios separados por comas y los muestra en la lista de usuarios.
     * @param listaSeparadaPorComas Lista de usuarios separados por comas.
     */
    public void recibirUsuarios(String listaSeparadaPorComas) {
        List<String> lista = Arrays.asList(listaSeparadaPorComas.split(","));
        listaUsuarios.getItems().clear();
        listaUsuarios.getItems().addAll(lista);
    }

    /**
     * Añade un nuevo usuario a la lista de usuarios.
     * @param usuario Usuario a añadir a la lista de usuarios.
     */
    public void addNuevoUsuario(String usuario) {
        listaUsuarios.getItems().add(usuario);
    }

    /**
     * Elimina un usuario de la lista de usuarios.
     * @param usuario Usuario a eliminar de la lista de usuarios.
     */
    public void deleteUsuario(String usuario) {
        listaUsuarios.getItems().remove(usuario);
    }

    /**
     * Envía un mensaje al servidor para desconectarse y cierra la conexión con el servidor.
     */
    @FXML
    private void onDesconectarClick() {
        try {
            salida.writeUTF(CliCmd.EXI.name());
            Cliente.hiloEscucha.interrupt();
            socketCliente.close();
            mensajes.clear();
            iniciar();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }

        cambiarEstado(ServCmd.EXI, "Desconectado");
    }

    /**
     * Envía un mensaje al servidor con el mensaje introducido en la interfaz gráfica. Limpia el campo de texto.
     */
    @FXML
    private void onEnviarClick() {
        try {
            salida.writeUTF(String.format("%s %s", CliCmd.MSG, mensajeIntroducido.getText()));
            mensajeIntroducido.setText("");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    /**
     * Recibe un mensaje del servidor y lo muestra en la interfaz gráfica.
     * @param mensaje Mensaje recibido del servidor.
     */
    public void recibirMensaje(String mensaje) {
        mensajes.appendText(mensaje + "\n");
    }

    /**
     * Muestra el menú para enviar un mensaje privado a un usuario. Si el alias es el mismo que el introducido, no se muestra el menú.
     * @param alias Alias del usuario al que se quiere enviar un mensaje privado.
     */
    private void onUsuarioClick(String alias) {
        if (alias.equals(aliasIntroducido.getText())) {
            return;
        }

        TextField mensaje = new TextField();
        mensaje.setPromptText("Escribe tu mensaje...");

        VBox contenido = new VBox();
        contenido.setSpacing(10);
        contenido.getChildren().add(mensaje);

        ButtonType enviar = new ButtonType("Enviar");
        ButtonType cancelar = new ButtonType("Cancelar");

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(String.format("Chat privado con %s", alias));
        alert.getButtonTypes().addAll(enviar, cancelar);
        alert.getDialogPane().setContent(contenido);
        alert.getDialogPane().setStyle("-fx-background-color: #00447c;");
        alert.getButtonTypes().forEach(bt -> alert.getDialogPane().lookupButton(bt).setStyle("-fx-background-color: #969bb9; -fx-text-fill: white;"));
        alert.showAndWait().ifPresent(bt -> {
            if (bt == enviar) {
                try {
                    salida.writeUTF(String.format("%s %s %s", CliCmd.PRV, alias, mensaje.getText()));
                } catch (IOException e) {
                    System.out.println("IOException: " + e.getMessage());
                }
            } else {
                alert.close();
            }
        });
    }
}
