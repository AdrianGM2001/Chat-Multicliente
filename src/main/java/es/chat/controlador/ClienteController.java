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
import java.util.*;

import es.chat.modelo.Chat;
import es.chat.modelo.comando.CliCmd;
import es.chat.modelo.comando.ServCmd;

/**
 * Controlador de la vista de cliente. Se encarga de gestionar la interfaz gráfica y la comunicación con el servidor.
 * Inicia el hilo de escucha de mensajes del servidor y envía mensajes al servidor.
 * @see EscuchaHilo
 * @see Cliente
 * @see Chat
 * @see CliCmd
 * @version 1.0
 * @author Adrián González
 */
public class ClienteController implements Initializable {
    private Socket socketCliente;
    private DataOutputStream salida;
    private List<Chat> chats;
    /**
     * Chat abierto en la interfaz. Se inicializa con el chat general.
     */
    private Chat chatActual;
    /**
     * Indica si se puede cambiar de chat. Se pone a false al enviar un mensaje privado para evitar cambiar de chat,
     * ya que se espera a recibir un mensaje privado de vuelta y escribirlo en el mismo chat.
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
    private ListView<Chat> chatsListView;

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
        chatsListView.setOnMouseClicked(event -> onUsuarioClick(chatsListView.getSelectionModel().getSelectedItem()));
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
        cambiarEstado(ServCmd.OK, mensaje);

        try {
            salida.writeUTF(CliCmd.LUS.name());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }

        // Inicializar lista de chats
        chatActual = new Chat("[General]");
        chats = new ArrayList<>();
        chats.add(chatActual);
        labelChatActual.setText("Chat general");
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
        Arrays.stream(listaSeparadaPorComas.split(","))
                .filter(c -> !c.equals(aliasIntroducido.getText()))
                .map(Chat::new)
                .forEach(chats::add);
        chatsListView.getItems().addAll(chats);
    }

    /**
     * Añade un nuevo usuario a la lista de usuarios. Si el usuario ya estuvo conectado, añade un mensaje de reconexión al chat.
     * @param alias Usuario a añadir a la lista de usuarios.
     */
    public void addNuevoUsuario(String alias) {
        Chat nuevoChat = chats.stream()
                .filter(u -> u.getAlias().equals(alias))
                .findFirst()
                .orElse(new Chat(alias));

        nuevoChat.addMensaje(String.format("%s se ha conectado.", alias));
        chats.add(nuevoChat);
        chatsListView.getItems().add(nuevoChat);
    }

    /**
     * Elimina un usuario del ListView de usuarios, pero no de la lista de chats (en caso de que el usuario vuelva a conectarse,
     * se podrán ver los mensajes anteriores, pero solo el usuario actual).
     * @param alias Usuario a eliminar del ListView de usuarios.
     */
    public void deleteUsuario(String alias) {
        Chat aEliminar = chats.stream()
            .filter(c -> c.getAlias().equals(alias))
            .findFirst()
            .orElseThrow();

        chatsListView.getItems().remove(aEliminar);

        aEliminar.addMensaje(String.format("%s se ha desconectado.", alias));

        if (chatActual.equals(aEliminar)) {
            chatActual = chats.getFirst();
            mensajes.setText(chatActual.getMensajes());
        }
    }

    /**
     * Envía un mensaje al servidor para desconectarse y cierra la conexión con el servidor. Limpia
     * el ListView de chats, la lista de chats y el chat actual.
     */
    @FXML
    private void onDesconectarClick() {
        try {
            salida.writeUTF(CliCmd.EXI.name());
            Cliente.hiloEscucha.interrupt();
            socketCliente.close();
            mensajes.clear();
            chats.clear();
            chatsListView.getItems().clear();
            chatActual = null;
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

        if (chatActual.getAlias().equals("[General]")) {
            mensaje = String.format("%s %s", CliCmd.MSG, mensajeIntroducido.getText());
        } else {
            mensaje = String.format("%s %s %s", CliCmd.PRV, chatActual.getAlias(), mensajeIntroducido.getText());
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
     * Recibe un mensaje general y lo muestra en la interfaz gráfica.
     * @param alias Alias del usuario que envía el mensaje.
     * @param mensaje Mensaje recibido.
     */
    public void recibirGeneral(String alias, String mensaje) {
        Chat chatGeneral = chats.getFirst(); // El chat general siempre está en la primera posición
        if (alias.equals(aliasIntroducido.getText())) {
            alias = "Yo";
        }

        chatGeneral.addMensaje(String.format("%s: %s", alias, mensaje));
        chatGeneral.incrementarMensajesNoLeidos();

        if (chatActual.getAlias().equals("[General]")) {
            mensajes.setText(chatActual.getMensajes());
            chatActual.resetMensajesNoLeidos();
        }

        chatsListView.refresh();
    }

    /**
     * Recibe un mensaje privado y lo muestra en la interfaz gráfica.
     * @param alias Alias del usuario que envía el mensaje.
     * @param mensaje Mensaje recibido.
     */
    public void recibirPrivado(String alias, String mensaje) {
        Optional<Chat> aEscribir = chats.stream()
                .filter(u -> u.getAlias().equals(alias))
                .findFirst();

        Chat aEscribirEncontrado;

        if (aEscribir.isEmpty()) {
            chatActual.addMensaje(String.format("Yo: %s", mensaje));
            mensajes.setText(chatActual.getMensajes());
            puedeCambiarChat = true;
            return;
        }

        aEscribirEncontrado = aEscribir.get();

        aEscribirEncontrado.addMensaje(String.format("%s: %s", alias, mensaje));
        aEscribirEncontrado.incrementarMensajesNoLeidos();

        if (aEscribirEncontrado.equals(chatActual)) {
            mensajes.setText(chatActual.getMensajes());
            chatActual.resetMensajesNoLeidos();
        }

        chatsListView.refresh();
    }

    /**
     * Muestra el chat con un usuario al hacer click en la lista de usuarios.
     * @param chatSeleccionado Chat seleccionado en la lista de usuarios.
     */
    private void onUsuarioClick(Chat chatSeleccionado) {
        if (chatSeleccionado == null || !puedeCambiarChat) {
            return;
        }

        chatActual = chatSeleccionado;

        if (chatActual.getAlias().equals("[General]")) {
            labelChatActual.setText("Chat general");
        } else {
            labelChatActual.setText(String.format("Chat privado con %s", chatActual.getAlias()));
        }

        chatActual.resetMensajesNoLeidos();
        chatsListView.refresh();
        mensajes.setText(chatActual.getMensajes());
    }
}
