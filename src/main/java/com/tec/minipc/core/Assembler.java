package com.tec.minipc.core;

import com.tec.minipc.model.Instruction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Se encarga de leer un archivo .asm del disco, validar que cada línea cumpla
 * el formato requerido y convertirlo en una lista de {@link Instruction} lista
 * para cargar en memoria.
 *
 * Reglas de lectura:
 *   - Se ignoran las líneas en blanco.
 *   - Se ignoran los comentarios: todo lo que esté después de ';' o '#' en una línea.
 *   - Cada línea de código restante debe cumplir el formato que valida Instruction.parse().
 *   - El archivo debe tener extensión .asm.
 */
public class Assembler {

    private static final String EXTENSION_REQUERIDA = ".asm";

    /**
     * Lee y valida un archivo .asm completo.
     *
     * @param archivo el archivo .asm a cargar
     * @return la lista de instrucciones parseadas, en el mismo orden del archivo
     * @throws IOException si el archivo no existe o no se puede leer
     * @throws AssemblyException si el archivo no tiene la extensión correcta o si
     *         alguna línea no cumple el formato requerido (incluye TODOS los errores encontrados)
     */
    public static List<Instruction> loadFromFile(File archivo) throws IOException, AssemblyException {
        validarExtension(archivo);

        List<Instruction> instrucciones = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int numeroLinea = 0;
            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                String codigo = quitarComentario(linea).trim();

                if (codigo.isEmpty()) {
                    continue; // línea en blanco o solo comentario
                }

                try {
                    instrucciones.add(Instruction.parse(codigo, numeroLinea));
                } catch (IllegalArgumentException e) {
                    errores.add(e.getMessage());
                }
            }
        }

        if (instrucciones.isEmpty() && errores.isEmpty()) {
            errores.add("El archivo está vacío o no contiene instrucciones.");
        }

        if (!errores.isEmpty()) {
            throw new AssemblyException(errores);
        }

        return instrucciones;
    }

    private static void validarExtension(File archivo) throws AssemblyException {
        String nombre = archivo.getName().toLowerCase();
        if (!nombre.endsWith(EXTENSION_REQUERIDA)) {
            List<String> errores = new ArrayList<>();
            errores.add("El archivo debe tener extensión " + EXTENSION_REQUERIDA
                    + " (se recibió: \"" + archivo.getName() + "\")");
            throw new AssemblyException(errores);
        }
    }

    private static String quitarComentario(String linea) {
        int idx = -1;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == ';' || c == '#') {
                idx = i;
                break;
            }
        }
        return (idx == -1) ? linea : linea.substring(0, idx);
    }
}