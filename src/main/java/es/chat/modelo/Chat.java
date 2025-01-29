package es.chat.modelo;

import java.util.Objects;

/**
 * Clase que representa un chat. Contiene su alias, el número de mensajes no leídos y los mensajes.
 * @version 1.0
 * @author Adrián González
 */
public class Chat {
    private final String alias;
    private int numMensajesNoLeidos;
    private String mensajes;

    public Chat(String alias) {
        this.alias = alias;
        numMensajesNoLeidos = 0;
        mensajes = "";
    }

    public String getAlias() {
        return alias;
    }

    public void incrementarMensajesNoLeidos() {
        numMensajesNoLeidos++;
    }

    public void resetMensajesNoLeidos() {
        numMensajesNoLeidos = 0;
    }

    public String getMensajes() {
        return mensajes;
    }

    public void addMensaje(String mensaje) {
        mensajes += String.format("%s%n", mensaje);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return Objects.equals(alias, chat.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(alias);
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", alias, numMensajesNoLeidos);
    }
    
}
