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
     * Inicia la conexión con el servidor y envía un mensaje al servidor para conectarse con un alias.
     */
    @FXML
    private void onConectarClick() {
        if (iniciarConexion()) {
            peticionAlServidor(String.format("%s %s", CliCmd.CON, aliasIntroducido.getText()));
        }
    }

    /**
     * Inicia la conexión con el servidor y el hilo de escucha de mensajes del servidor.
     * Muestra un mensaje de error si no se puede conectar con el servidor.
     * @see EscuchaHilo
     * @see Cliente
     */
    private boolean iniciarConexion() {
        try {
            socketCliente = new Socket("localhost", 4444);
            DataInputStream entrada = new DataInputStream(socketCliente.getInputStream());
            salida = new DataOutputStream(socketCliente.getOutputStream());
            Cliente.hiloEscucha = new Thread(new EscuchaHilo(entrada, this));
            Cliente.hiloEscucha.start();
            return true;
        } catch (IOException e) {
            mensajeEstado.setText("Error al conectar con el servidor");
            return false;
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
        iniciarInterfaz();
        cambiarEstado(ServCmd.OK, mensaje);
        peticionAlServidor(CliCmd.LUS.name());
    }

    private void iniciarInterfaz() {
        chatActual = new Chat("[General]");
        chats = new ArrayList<>();
        chats.add(chatActual);
        chatsListView.getItems().setAll(chats);
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
        chatsListView.getItems().addAll(chats.stream().skip(1).toList());
        chats.stream().skip(1).forEach(c -> chats.getFirst().addMensaje(String.format("%s se ha conectado.", c.getAlias())));
        chats.stream().skip(1).forEach(c -> c.addMensaje(String.format("%s se ha conectado.", c.getAlias())));
        mensajes.setText(chats.getFirst().getMensajes());
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
        chats.getFirst().addMensaje(String.format("%s se ha conectado.", alias));
        chats.add(nuevoChat);
        chatsListView.getItems().add(nuevoChat);

        if (chatActual.equals(chats.getFirst())) {
            mensajes.setText(chatActual.getMensajes());
        }
    }

    /**
     * Elimina un usuario del ListView de usuarios, pero no de la lista de chats (en caso de que el usuario vuelva a conectarse,
     * se podrán ver los mensajes anteriores, pero solo el usuario actual).
     * @param alias Usuario a eliminar del ListView de usuarios.
     */
    public void deleteUsuario(String alias) {
        Chat eliminarChat = chats.stream()
            .filter(c -> c.getAlias().equals(alias))
            .findFirst()
            .orElseThrow();

        chatsListView.getItems().remove(eliminarChat);

        // El chat se mantiene en la lista de chats (pero no en el ListView), pero se añade un mensaje de desconexión
        eliminarChat.addMensaje(String.format("%s se ha desconectado.", alias));
        chats.getFirst().addMensaje(String.format("%s se ha desconectado.", alias));

        // Si el chat actual es el que se ha eliminado, se cambia al chat general
        if (chatActual.equals(eliminarChat)) {
            chatActual = chats.getFirst();
            mensajes.setText(chatActual.getMensajes());
        }

        if (chatActual.equals(chats.getFirst())) {
            mensajes.setText(chatActual.getMensajes());
        }
    }

    /**
     * Envía un mensaje al servidor para desconectarse y cierra la conexión con el servidor. Limpia
     * el ListView de chats, la lista de chats y el chat actual.
     */
    @FXML
    private void onDesconectarClick() {
        peticionAlServidor(CliCmd.EXI.name());

        try {
            Cliente.hiloEscucha.interrupt();
            socketCliente.close();
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

        peticionAlServidor(mensaje);
        mensajeIntroducido.setText("");
    }

    /**
     * Recibe un mensaje general y lo muestra en la interfaz gráfica.
     * @param alias Alias del usuario que envía el mensaje.
     * @param mensaje Mensaje recibido.
     */
    public void recibirGeneral(String alias, String mensaje) {
        Chat chatGeneral = chats.getFirst(); // El chat general siempre está en la primera posición

        chatGeneral.addMensaje(String.format("%s: %s", alias, mensaje));
        chatGeneral.incrementarMensajesNoLeidos();

        if (chatActual.equals(chats.getFirst())) {
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
        Optional<Chat> emisorChat = chats.stream()
                .filter(u -> u.getAlias().equals(alias))
                .findFirst();

        Chat emisorChatEncontrado;

        if (emisorChat.isEmpty()) {
            chatActual.addMensaje(String.format("%s: %s", alias, mensaje));
            mensajes.setText(chatActual.getMensajes());
            puedeCambiarChat = true;
            return;
        }

        emisorChatEncontrado = emisorChat.get();

        emisorChatEncontrado.addMensaje(String.format("%s: %s", alias, mensaje));
        emisorChatEncontrado.incrementarMensajesNoLeidos();

        if (emisorChatEncontrado.equals(chatActual)) {
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

        if (chatActual.equals(chats.getFirst())) {
            labelChatActual.setText("Chat general");
        } else {
            labelChatActual.setText(String.format("Chat privado con %s", chatActual.getAlias()));
        }

        chatActual.resetMensajesNoLeidos();
        chatsListView.refresh();
        mensajes.setText(chatActual.getMensajes());
    }

    /**
     * Envía una petición al servidor. Si hay un error al enviar la petición, muestra un mensaje de error en la consola.
     * @param peticion Petición a enviar al servidor.
     */
    private void peticionAlServidor(String peticion) {
        try {
            salida.writeUTF(peticion);
        } catch (IOException e) {
            System.err.printf("ERROR. %s%n%s%n", peticion, e.getMessage());
        }
    }
}
