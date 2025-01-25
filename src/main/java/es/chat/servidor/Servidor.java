package es.chat.servidor;

import es.chat.modelo.Cliente;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que inicia el servidor y acepta conexiones de clientes.
 * Crea un hilo {@code Worker} por cada {@code Cliente} que se conecta.
 * @see Worker
 * @see Cliente
 * @version 1.0
 * @author Adrián González
 */
public class Servidor {
    public static final int PUERTO = 4444;

    public static void main(String[] args) {
        List<Cliente> clientes = new ArrayList<>();
        Object lock = new Object();

        try (ServerSocket socketServidor = new ServerSocket(PUERTO)) {
            System.out.println("Escuchando: " + socketServidor);
            Socket socketCliente;

            while (!socketServidor.isClosed()) {
                try {
                    socketCliente = socketServidor.accept();
                    Cliente c = new Cliente(socketCliente);
                    Worker w = new Worker(c, clientes, lock);
                    new Thread(w).start();
                } catch (IOException e) {
                    System.out.println("IOException: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("No puede escuchar en el puerto: " + PUERTO);
            System.exit(-1);
        }
    }
}
