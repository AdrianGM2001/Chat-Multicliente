package es.chat.modelo.comando;

/**
 * Comandos del cliente
 * <ul>
 *     <li>{@code CON}: Entra en el chat, p. ej: {@code CON Juan}</li>
 *     <li>{@code MSG}: Envía un mensaje general, p. ej: {@code MSG Hola}</li>
 *     <li>{@code PRV}: Envía un mensaje privado, requiere añadir el alias del destinatario y el mensaje, p. ej: {@code PRV Juan Hola}</li>
 *     <li>{@code LUS}: Solicita la lista de usuarios, p. ej: {@code LUS}</li>
 *     <li>{@code EXI}: Abandona el chat, p. ej: {@code EXI}</li>
 * </ul>
 * @version 1.0
 * @author Adrián González
 */
public enum CliCmd {
    CON,
    MSG,
    PRV,
    LUS,
    EXI
}
