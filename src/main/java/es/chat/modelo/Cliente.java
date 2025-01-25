package es.chat.modelo;

import es.chat.servidor.Servidor;
import es.chat.servidor.Worker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Clase que representa a un cliente. Contiene su alias, el flujo de salida y el flujo de entrada.
 * Se utiliza en el servidor para gestionar los clientes conectados.
 * @see Servidor
 * @see Worker
 * @version 1.0
 * @author Adrián González
 */
public class Cliente {
    private String alias;
    private DataOutputStream salida;
    private DataInputStream entrada;

    public Cliente(Socket socketCliente) {
        try {
            salida = new DataOutputStream(socketCliente.getOutputStream());
            entrada = new DataInputStream(socketCliente.getInputStream());
        } catch (Exception e) {
            System.err.println("Error al crear los flujos de entrada y salida del cliente.");
        }
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public DataOutputStream getSalida() {
        return salida;
    }

    public DataInputStream getEntrada() {
        return entrada;
    }
}
