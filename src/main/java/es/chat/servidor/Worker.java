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

                if (!validarComando(comandoRecibido)) {
                    continue;
                }

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
     */
    private void ejecutarComando(CliCmd comando, String parametros) {
        switch (comando) {
            case PRV -> enviarMensajePrivado(parametros);
            case EXI -> desconectar();
            case LUS -> pedirListaUsuarios();
            case MSG -> enviarMensajeGeneral(parametros);
            case CON -> iniciarSesion(parametros);
        }
    }

    /**
     * Valida el comando recibido por el cliente. Si el comando no es válido, muestra un mensaje de error.
     * @param comandoRecibido Comando recibido
     * @return {@code true} si el comando es válido, {@code false} en caso contrario
     */
    private boolean validarComando(String comandoRecibido) {
        if (Validar.comandoParametros(comandoRecibido)) {
            return true;
        }
        
        System.err.printf("COMANDO NO VÁLIDO. %s: %s%n",
            cliente.getAlias() == null ? "Nueva conexión" : cliente.getAlias(), comandoRecibido);

        return false;
    }

    /**
     * Inicia la sesión del cliente con el alias especificado. Si el alias ya está en uso o no es válido,
     * no se inicia la sesión y se envía un mensaje de error al cliente. En caso contrario, se inicia la sesión
     * y se envía un mensaje de confirmación al cliente y a todos los clientes conectados.
     * @param alias Alias del cliente
     */
    private void iniciarSesion(String alias) {
        if (cliente.getAlias() != null) {
            cliente.enviarRespuesta(String.format("%s %s", ServCmd.NOK, "Ya estás conectado"));
            System.err.printf("Cliente ya conectado: %s%n", cliente.getAlias());
            return;
        }

        if (!Validar.alias(alias)) {
            cliente.enviarRespuesta(String.format("%s %s", ServCmd.NOK, "Alias no válido"));
            System.err.printf("Alias no válido: %s%n", alias);
            return;
        }

        synchronized (lock) {
            if (clientes.stream().anyMatch(c -> c.getAlias().equals(alias))) {
                cliente.enviarRespuesta(String.format("%s %s", ServCmd.NOK, "Alias ya en uso"));
                System.err.printf("Alias en uso: %s%n", alias);
                return;
            }

            cliente.setAlias(alias);

            clientes.forEach(c -> c.enviarRespuesta(String.format("%s %s", ServCmd.CON, cliente.getAlias())));
            clientes.add(cliente);
        }

        if (cliente.enviarRespuesta(String.format("%s ¡Conectado!", ServCmd.OK))) {
            System.out.printf("Cliente conectado: %s%n", cliente.getAlias());
        }
    }

    /**
     * Envía un mensaje general a todos los clientes conectados.
     * @param mensaje Mensaje a enviar
     */
    private void enviarMensajeGeneral(String mensaje) {
        synchronized (lock) {
            clientes.forEach(c -> c.enviarRespuesta(String.format("%s %s %s", ServCmd.CHT, cliente.getAlias(), mensaje)));
        }

        System.out.printf("Mensaje de %s: %s%n", cliente.getAlias(), mensaje);
    }

    /**
     * Envía la lista de usuarios conectados al cliente.
     */
    private void pedirListaUsuarios() {
        String listaCSV;

        synchronized (lock) {
            listaCSV = String.join(",", clientes.stream().map(Cliente::getAlias).toList());
        }

        if (cliente.enviarRespuesta(String.format("%s %s", ServCmd.LST, listaCSV))) {
            System.out.printf("Lista de usuarios enviada a %s%n", cliente.getAlias());
        }
    }

    /**
     * Desconecta al cliente y envía un mensaje de desconexión a todos los clientes conectados.
     */
    private void desconectar() {
        synchronized (lock) {
            clientes.remove(cliente);
            clientes.forEach(c -> c.enviarRespuesta(String.format("%s %s", ServCmd.EXI, cliente.getAlias())));
        }

        System.out.printf("Cliente desconectado: %s%n", cliente.getAlias());
    }

    /**
     * Envía un mensaje privado al destinatario especificado. Si el destinatario no existe
     * o es el propio cliente, no se envía el mensaje. En caso contrario, se envía el mensaje
     * al destinatario y al cliente que lo envió.
     * @param parametros Destinatario y mensaje
     */
    private void enviarMensajePrivado(String parametros) {
        String[] splitDestinatarioMensaje = parametros.split(" ", 2);
        String aliasDestinatario = splitDestinatarioMensaje[0];
        String mensaje = splitDestinatarioMensaje[1];

        Optional<Cliente> destinatario;

        synchronized (lock) {
            destinatario = clientes.stream().filter(c -> c.getAlias().equals(aliasDestinatario)).findFirst();
        }

        if (destinatario.isEmpty() || cliente.equals(destinatario.get())) {
            return;
        }

        destinatario.get().enviarRespuesta(String.format("%s %s %s", ServCmd.PRV, cliente.getAlias(), mensaje));
        cliente.enviarRespuesta(String.format("%s %s %s", ServCmd.PRV, cliente.getAlias(), mensaje));
        
        System.out.printf("Mensaje privado de %s a %s: %s%n", cliente.getAlias(), aliasDestinatario, mensaje);
    }
}
