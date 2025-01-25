package es.chat.cliente;

import es.chat.controlador.ClienteController;
import javafx.application.Platform;

import java.io.DataInputStream;

import es.chat.modelo.comando.ServCmd;

/**
 * Clase que se encarga de escuchar los mensajes que envía el servidor.
 * Se ejecuta en un hilo separado para no bloquear la interfaz.
 * @see ClienteController
 * @see ServCmd
 * @version 1.0
 * @author Adrián González
 */
public class EscuchaHilo extends Thread {
    private final DataInputStream entrada;
    private final ClienteController clienteController;

    public EscuchaHilo(DataInputStream entrada, ClienteController clienteController) {
        this.entrada = entrada;
        this.clienteController = clienteController;
    }

    /**
     * Se ejecuta al iniciar el hilo. Escucha los mensajes del servidor y los procesa.
     */
    @Override
    public void run() {
        while (currentThread().isAlive()) {
            try {
                String mensajeRecibido = entrada.readUTF();
                String[] splitMensaje = mensajeRecibido.split(" ", 2);
                ServCmd comando = ServCmd.valueOf(splitMensaje[0]);
                String parametros = splitMensaje.length > 1 ? splitMensaje[1] : "";

                ejecutarComando(comando, parametros);
            } catch (Exception e) {
                System.out.println("Se ha cerrado la conexión. " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Ejecuta el comando recibido del servidor.
     * @param comando Comando recibido del servidor.
     * @param parametros Parámetros del comando.
     */
    private void ejecutarComando(ServCmd comando, String parametros) {
        switch (comando) {
            case CON -> agregarUsuario(parametros);
            case CHT -> recibirGeneral(parametros);
            case PRV -> recibirPrivado(parametros);
            case LST -> recibirListaUsuarios(parametros);
            case EXI -> eliminarUsuario(parametros);
            case NOK -> errorAlConectar(parametros);
            case OK -> exitoAlConectar(parametros);
        }
    }

    /**
     * Añade un nuevo usuario a la lista de usuarios.
     * @param alias Alias del nuevo usuario.
     */
    private void agregarUsuario(String alias) {
        Platform.runLater(() -> clienteController.addNuevoUsuario(alias));
        System.out.printf("Nuevo usuario: %s%n", alias);
    }

    /**
     * Elimina un usuario de la lista de usuarios.
     * @param alias Alias del usuario a eliminar.
     */
    private void eliminarUsuario(String alias) {
        Platform.runLater(() -> clienteController.deleteUsuario(alias));
        System.out.printf("Usuario desconectado: %s%n", alias);
    }

    /**
     * Recibe la lista de usuarios conectados y la muestra en la interfaz.
     * @param listaCSV Lista de usuarios conectados separada por comas.
     */
    private void recibirListaUsuarios(String listaCSV) {
        Platform.runLater(() -> clienteController.recibirUsuarios(listaCSV));
        System.out.printf("Lista de usuarios: %s%n", listaCSV);
    }

    /**
     * Recibe un mensaje privado y lo muestra en la interfaz.
     * @param parametros Parámetros del mensaje privado.
     */
    private void recibirPrivado(String parametros) {
        String[] splitParametros = parametros.split(" ", 2);
        Platform.runLater(() -> clienteController.recibirMensaje(String.format("[PRIVADO] %s: %s", splitParametros[0], splitParametros[1])));
        System.out.printf("Mensaje privado de %s: %s%n", splitParametros[0], splitParametros[1]);
    }

    /**
     * Recibe un mensaje general y lo muestra en la interfaz.
     * @param parametros Parámetros del mensaje general.
     */
    private void recibirGeneral(String parametros) {
        String[] splitParametros = parametros.split(" ", 2);
        Platform.runLater(() -> clienteController.recibirMensaje(String.format("%s: %s", splitParametros[0], splitParametros[1])));
        System.out.printf("Mensaje de %s: %s%n", splitParametros[0], splitParametros[1]);
    }

    /**
     * Muestra un mensaje de error en la interfaz.
     * @param mensaje Mensaje de error.
     */
    private void errorAlConectar(String mensaje) {
        Platform.runLater(() -> clienteController.recibirNOK(mensaje));
        System.out.printf("Conexión rechazada. %s%n", mensaje);
    }

    /**
     * Muestra un mensaje de éxito en la interfaz.
     * @param mensaje Mensaje de éxito.
     */
    private void exitoAlConectar(String mensaje) {
        Platform.runLater(() -> clienteController.recibirOK(mensaje));
        System.out.printf("Conexión aceptada. %s%n", mensaje);
    }
}
