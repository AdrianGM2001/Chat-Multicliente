package es.chat.servidor;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import es.chat.modelo.comando.CliCmd;
import es.chat.modelo.comando.ServCmd;
import es.chat.modelo.Cliente;
import es.chat.util.Validar;

/**
 * Clase que implementa la interfaz Runnable y se encarga de gestionar los comandos
 * enviados por un cliente. Se ejecuta en un hilo independiente por cada cliente.
 * Tiene acceso a la lista de clientes conectados y al objeto de sincronización de la
 * lista de clientes para evitar problemas de concurrencia.
 * @version 1.0
 * @author Adrián González
 */
public class Worker implements Runnable {
    private final Cliente cliente;
    private final List<Cliente> clientes;
    private final Object lock;

    public Worker(Cliente cliente, List<Cliente> clientes, Object lock) {
        this.cliente = cliente;
        this.clientes = clientes;
        this.lock = lock;
    }

    /**
     * Se ejecuta al iniciar el hilo. Lee los comandos enviados por el cliente
     * y ejecuta la acción correspondiente. En caso de error, desconecta al cliente.
     */
    @Override
    public void run() {
        try {
            CliCmd comando = null;

            while (comando != CliCmd.EXI) {
                String comandoRecibido = cliente.getEntrada().readUTF();

                if (validarComando(comandoRecibido)) continue;

                String[] splitComandoParametros = comandoRecibido.split(" ", 2);
                comando = CliCmd.valueOf(splitComandoParametros[0]);
                String parametros = splitComandoParametros.length == 2 ? splitComandoParametros[1] : "";

                ejecutarComando(comando, parametros);
            } 
        } catch (IOException e) {
            if (cliente.getAlias() != null) {
                desconectar(); // Desconectar cliente en caso de error
            }
        }
    }

    /**
     * Ejecuta el comando recibido por el cliente.
     * @param comando Comando recibido
     * @param parametros Parámetros del comando
     * @throws IOException Si hay un error al enviar mensajes
     */
    private void ejecutarComando(CliCmd comando, String parametros) throws IOException {
        switch (comando) {
            case PRV -> enviarMensajePrivado(parametros);
            case EXI -> desconectar();
            case LUS -> pedirListaUsuarios();
            case MSG -> enviarMensajeGeneral(parametros);
            case CON -> iniciarSesion(parametros);
        }
    }

    /**
     * Valida el comando recibido por el cliente.
     * @param comandoRecibido Comando recibido
     * @return {@code true} si el comando no es válido, {@code false} en caso contrario
     * @throws IOException Si hay un error al enviar mensajes
     */
    private boolean validarComando(String comandoRecibido) throws IOException {
        if (!Validar.comandoParametros(comandoRecibido)) {
            System.err.printf("COMANDO NO VÁLIDO. %s: %s%n",
                cliente.getAlias() == null ? "Nueva conexión" : cliente.getAlias(), comandoRecibido);

            if (cliente.getAlias() == null) {
                cliente.getSalida().writeUTF(String.format("%s %s", ServCmd.NOK, "Alias no válido"));
            }

            return true;
        }
        return false;
    }

    /**
     * Inicia la sesión del cliente con el alias proporcionado. Si el alias ya está en uso,
     * envía un mensaje de error al cliente. En caso contrario, añade al cliente a la lista
     * de clientes conectados y envía un mensaje de confirmación a todos los clientes conectados.
     * @param alias Alias del cliente
     * @throws IOException Si hay un error al enviar mensajes
     */
    private void iniciarSesion(String alias) throws IOException {
        if (cliente.getAlias() != null) {
            cliente.getSalida().writeUTF(String.format("%s %s", ServCmd.NOK, "Ya estás conectado"));
            return;
        }

        synchronized (lock) {
            if (clientes.stream().anyMatch(c -> c.getAlias().equals(alias))) {
                cliente.getSalida().writeUTF(String.format("%s %s", ServCmd.NOK, "Alias ya en uso"));
                return;
            }
    
            cliente.setAlias(alias);
            clientes.add(cliente);
            cliente.getSalida().writeUTF(String.format("%s ¡Conectado!", ServCmd.OK));
    
            for (Cliente c : clientes) {
                if (!c.getAlias().equals(cliente.getAlias())) {
                    c.getSalida().writeUTF(String.format("%s %s", ServCmd.CON,  cliente.getAlias()));
                }
            }
        }

        System.out.printf("Cliente conectado: %s%n", cliente.getAlias());
    }

    /**
     * Envía un mensaje general a todos los clientes conectados.
     * @param mensaje Mensaje a enviar
     * @throws IOException Si hay un error al enviar mensajes
     */
    private void enviarMensajeGeneral(String mensaje) throws IOException {
        synchronized (lock) {
            for (Cliente c : clientes) {
                c.getSalida().writeUTF(String.format("%s %s %s", ServCmd.CHT, cliente.getAlias(), mensaje));
            }
        }

        System.out.printf("Mensaje de %s: %s%n", cliente.getAlias(), mensaje);
    }

    /**
     * Envía la lista de usuarios conectados al cliente.
     * @throws IOException Si hay un error al enviar mensajes
     */
    private void pedirListaUsuarios() throws IOException {
        synchronized (lock) {
            String listaCSV = clientes.stream().map(Cliente::getAlias).reduce((a, b) -> a + "," + b).orElse("");
            cliente.getSalida().writeUTF(String.format("%s %s", ServCmd.LST, listaCSV));
        }

        System.out.printf("Lista de usuarios enviada a %s%n", cliente.getAlias());
    }

    /**
     * Desconecta al cliente y envía un mensaje de desconexión a todos los clientes conectados.
     */
    private void desconectar() {
        synchronized (lock) {
            clientes.remove(cliente);

            for (Cliente c : clientes) {
                try {
                    c.getSalida().writeUTF(String.format("%s %s", ServCmd.EXI, cliente.getAlias()));
                } catch (IOException e) {
                    System.err.printf("Error al enviar mensaje a %s: %s%n", c.getAlias(), e.getMessage());
                }
            }
        }

        System.out.printf("Cliente desconectado: %s%n", cliente.getAlias());

        cliente.setAlias(null);
    }

    /**
     * Envía un mensaje privado al destinatario especificado. Si el destinatario no existe
     * o es el propio cliente, no se envía el mensaje. En caso contrario, se envía el mensaje
     * al destinatario y al cliente que lo envió.
     * @param parametros Destinatario y mensaje
     * @throws IOException Si hay un error al enviar mensajes
     */
    private void enviarMensajePrivado(String parametros) throws IOException {
        String[] splitDestinatarioMensaje = parametros.split(" ", 2);
        String aliasDestinatario = splitDestinatarioMensaje[0];
        String mensaje = splitDestinatarioMensaje[1];

        Optional<Cliente> destinatario;

        synchronized (lock) {
            destinatario = clientes.stream().filter(c -> c.getAlias().equals(aliasDestinatario)).findFirst();
        }

        if (destinatario.isPresent() && !cliente.equals(destinatario.get())) {
            destinatario.get().getSalida().writeUTF(String.format("%s %s %s", ServCmd.PRV, cliente.getAlias(), mensaje));
            cliente.getSalida().writeUTF(String.format("%s %s %s", ServCmd.PRV, cliente.getAlias(), mensaje));
        }

        System.out.printf("Mensaje privado de %s a %s: %s%n", cliente.getAlias(), aliasDestinatario, mensaje);
    }
}
