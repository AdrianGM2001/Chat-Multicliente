package es.chat.util;

import java.util.EnumSet;

import es.chat.modelo.comando.CliCmd;

public class Validar {
    /**
     * Valida si el comando es correcto. Un comando correcto tiene una longitud
     * mayor o igual a 3 y debe tener su valor correspondiente en la enumeración {@code CliCmd}.
     * @param comando Comando a validar (con o sin parámetros)
     * @return {@code true} si el comando es correcto, {@code false} en caso contrario
     * @see CliCmd
     */
    public static boolean comando(String comando) {
        return comando.length() >= 3 && EnumSet.allOf(CliCmd.class)
            .stream()
            .anyMatch(c -> c.name().equals(comando.substring(0, 3)));
    }

    /**
     * Valida si el alias es correcto. Un alias correcto solo puede contener:
     * <ul>
     *  <li>Letras
     *      <ul>
     *          <li>Mayúsculas</li>
     *          <li>Minúsculas</li>
     *          <li>Con tildes, diéresis y la letra ñ (por si te llamas Iñaki, Íñigo, Begoña...)</li>
     *      </ul>
     *  </li>
     *  <li>Números</li>
     * </ul>
     * 
     * @param alias Alias a validar
     * @return {@code true} si el alias es correcto, {@code false} en caso contrario
     */
    public static boolean alias(String alias) {
        return alias.matches("^[a-zA-Z0-9áéíóúÁÉÍÓÚüÜñÑ]+$");
    }

    /**
     * Valida si el comando y los parámetros son correctos.
     * @param comando Comando a validar (con parámetros)
     * @return {@code true} si es correcto, {@code false} en caso contrario
     */
    public static boolean parametros(String comando) {
        if (comando.length() < 3) {
            return false;
        }
        
        CliCmd c = CliCmd.valueOf(comando.substring(0, 3));
        String[] comandoSplit = comando.split(" ", 2);

        return switch (c) {
            case CON, MSG -> comandoSplit.length == 2;
            case PRV -> {
                String[] aliasMensaje = comandoSplit[1].split(" ", 2);
                yield aliasMensaje.length == 2 && alias(aliasMensaje[0]);
            }
            case LUS, EXI -> comandoSplit.length == 1;
        };
    }

    /**
     * Valida si el comando y los parámetros son correctos.
     * @param comandoParametros Comando y parámetros a validar
     * @return {@code true} si es correcto, {@code false} en caso contrario
     * @see #comando(String comando)
     * @see #parametros(String comando)
     */
    public static boolean comandoParametros(String comandoParametros) {
        return comando(comandoParametros) && parametros(comandoParametros);
    }
}
