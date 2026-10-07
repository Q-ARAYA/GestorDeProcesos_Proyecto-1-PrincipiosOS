package com.tec.minipc.core;

import java.util.Collections;
import java.util.List;

/**
 * Se lanza cuando el archivo .asm no cumple el formato requerido.
 * Agrupa TODOS los errores encontrados (no solo el primero), para que la UI
 * pueda mostrarle al usuario la lista completa de líneas con problemas de una vez.
 */
public class AssemblyException extends Exception {

    private final List<String> errores;

    /**
     * Inicializa AssemblyException con los recursos y valores recibidos.
     * @param errores lista de errores de ensamblado
     */
    public AssemblyException(List<String> errores) {
        super(construirMensaje(errores));
        this.errores = Collections.unmodifiableList(errores);
    }

    /**
     * Combina los errores de ensamblado en un mensaje legible para mostrar al usuario.
     * @param errores errores recopilados
     * @return texto asociado con el estado o valor consultado.
     */
    private static String construirMensaje(List<String> errores) {
        StringBuilder sb = new StringBuilder();
        sb.append("El archivo .asm tiene ").append(errores.size()).append(" error(es) de formato:\n");
        for (String e : errores) {
            sb.append(" - ").append(e).append("\n");
        }
        return sb.toString();
    }

    /** @return la lista completa de mensajes de error, uno por línea inválida. */
    public List<String> getErrores() {
        return errores;
    }
}