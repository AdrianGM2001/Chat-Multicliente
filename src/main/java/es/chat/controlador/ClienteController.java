package es.chat.controlador;

import es.chat.cliente.Cliente;
import es.chat.cliente.EscuchaHilo;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private Map<String, String> chats;
    private String aliasChatActual;
    /**
     * Indica si se puede cambiar de chat. Esto es necesario ya que el servidor envía como respuesta a un mensaje privado
     * el alias del emisor y el mensaje enviado, por lo que se debe evitar cambiar de chat tras el envío de un mensaje y no 
     * haber recibido la respuesta del servidor, ya que de esta manera se asegura que se muestre en el chat desde el que ha 
     * sido enviado.
     */
    private boolean puedeCambiarChat = true;

    @FXML
    private TextField aliasIntroducido;

    @FXML
    private Label mensajeEstado;

    @FXML
    private Circle estado;

    @FXML
    private Button botConexion;

    @FXML
    private Label labelChatActual;

    @FXML
    private Button botGeneral;

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
    }

    /**
     * Inicia la conexión con el servidor y el hilo de escucha de mensajes del servidor.
     * Muestra un mensaje de error si no se puede conectar con el servidor.
     * @see EscuchaHilo
     * @see Cliente
     */
    private void iniciarConexion() {
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
        iniciarConexion();

        if (socketCliente == null || socketCliente.isClosed()) {
            System.out.println("No hay conexión con el servidor.");
            return;
        }

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
     * Cambia el estado de la conexión y muestra un mensaje de éxito en la interfaz gráfica. Inicializa la lista de chats y el chat actual.
     * @param mensaje Mensaje a mostrar en la interfaz gráfica.
     */
    public void recibirOK(String mensaje) {
        try {
            salida.writeUTF(CliCmd.LUS.name());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }

        cambiarEstado(ServCmd.OK, mensaje);
        chats = new HashMap<>();
        chats.put("_General", "");
        aliasChatActual = "_General";
        labelChatActual.setText("Chat general");
        botGeneral.setVisible(false);
        puedeCambiarChat = true;
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
     * Inicializa la lista de chats con los usuarios y un mensaje vacío.
     * @param listaSeparadaPorComas Lista de usuarios separados por comas.
     */
    public void recibirUsuarios(String listaSeparadaPorComas) {
        List<String> lista = Arrays.asList(listaSeparadaPorComas.split(","));
        listaUsuarios.getItems().clear();
        listaUsuarios.getItems().addAll(lista);
        listaUsuarios.getItems().forEach(usuario -> chats.put(usuario, ""));
    }

    /**
     * Añade un nuevo usuario a la lista de usuarios. Si el usuario ya estuvo conectado, añade un mensaje de reconexión al chat.
     * @param usuario Usuario a añadir a la lista de usuarios.
     */
    public void addNuevoUsuario(String usuario) {
        listaUsuarios.getItems().add(usuario);

        if (!chats.containsKey(usuario)) {
            chats.put(usuario, "");
            return;
        }

        chats.put(usuario, chats.get(usuario) + String.format("%s se ha reconectado.%n", usuario));

        if (aliasChatActual.equals(usuario)) {
            mensajes.setText(chats.get(usuario));
        }
    }

    /**
     * Elimina un usuario de la lista de usuarios.
     * @param usuario Usuario a eliminar de la lista de usuarios.
     */
    public void deleteUsuario(String usuario) {
        listaUsuarios.getItems().remove(usuario);

        chats.put(usuario, chats.get(usuario) + String.format("%s se ha desconectado.%n", usuario));

        if (aliasChatActual.equals(usuario)) {
            mensajes.setText(chats.get(usuario));
        }
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
        String mensaje;

        if (mensajeIntroducido.getText().isBlank()) {
            return;
        }

        if (aliasChatActual.equals("_General")) {
            mensaje = String.format("%s %s", CliCmd.MSG, mensajeIntroducido.getText());
        } else {
            mensaje = String.format("%s %s %s", CliCmd.PRV, aliasChatActual, mensajeIntroducido.getText());
            puedeCambiarChat = false;
        }

        try {
            salida.writeUTF(mensaje);
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
        mensajeIntroducido.setText("");
    }

    /**
     * Muestra el chat general al hacer click en el botón de chat general. Si no se puede cambiar de chat, no hace nada.
     */
    @FXML
    private void onGeneralClick() {
        if (!puedeCambiarChat) {
            return;
        }

        aliasChatActual = "_General";
        labelChatActual.setText("Chat general");
        mensajes.setText(chats.get(aliasChatActual));
        botGeneral.setVisible(false);
    }

    /**
     * Recibe un mensaje general y lo muestra en la interfaz gráfica.
     * @param alias Alias del usuario que envía el mensaje.
     * @param mensaje Mensaje recibido.
     */
    public void recibirGeneral(String alias, String mensaje) {
        if (alias.equals(aliasIntroducido.getText())) {
            alias = "Yo";
        }

        chats.put("_General", chats.get("_General") + String.format("%s: %s%n", alias, mensaje));

        if (aliasChatActual.equals("_General")) {
            mensajes.setText(chats.get("_General"));
        }
    }

    /**
     * Recibe un mensaje privado y lo muestra en la interfaz gráfica.
     * @param alias Alias del usuario que envía el mensaje.
     * @param mensaje Mensaje recibido.
     */
    public void recibirPrivado(String alias, String mensaje) {
        if (alias.equals(aliasIntroducido.getText())) {
            chats.put(aliasChatActual, chats.get(aliasChatActual) + String.format("Yo: %s%n", mensaje));
            puedeCambiarChat = true;
            mensajes.setText(chats.get(aliasChatActual));
            return;
        }

        chats.put(alias, chats.get(alias) + String.format("%s: %s%n", alias, mensaje));

        if (aliasChatActual.equals(alias)) {
            mensajes.setText(chats.get(alias));
        }
    }

    /**
     * Muestra el chat privado con un usuario al hacer click en la lista de usuarios.
     * @param alias Alias del usuario al que se quiere enviar un mensaje privado.
     */
    private void onUsuarioClick(String alias) {
        if (alias == null || alias.equals(aliasIntroducido.getText()) || !puedeCambiarChat) {
            return;
        }

        aliasChatActual = alias;
        labelChatActual.setText(String.format("Chat privado con %s", alias));
        mensajes.setText(chats.get(aliasChatActual));
        botGeneral.setVisible(true);
    }
}
