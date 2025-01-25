package es.chat.modelo.comando;

/**
 * Comandos del cliente
 * <ul>
 *     <li>{@code OK}: Respuesta de que ha podido entrar al chat, p. ej: {@code OK Inicio correcto}</li>
 *     <li>{@code NOK}: Respuesta de que no ha podido entrar al chat, p. ej: {@code NOK Alias ya en uso}</li>
 *     <li>{@code CHT}: Envía un mensaje general, requiere añadir el alias del emisor y el mensaje, p. ej: {@code CHT Juan Hola}</li>
 *     <li>{@code PRV}: Envía un mensaje privado, requiere añadir el alias del emisor y el mensaje, p. ej: {@code PRV Juan Hola}</li>
 *     <li>{@code LST}: Envia la lista de usuarios en formato CSV, p. ej: {@code LST Juan, Alex}</li>
 *     <li>{@code EXI}: Notifica la salida de un usuario, p. ej: {@code EXI Juan}</li>
 *     <li>{@code CON}: Notifica la llegada de un usuario, p. ej: {@code CON Juan}</li>
 * </ul>
 * @version 1.0
 * @author Adrián González
 */
public enum ServCmd {
    OK,
    NOK,
    CHT,
    PRV,
    LST,
    EXI,
    CON
}
