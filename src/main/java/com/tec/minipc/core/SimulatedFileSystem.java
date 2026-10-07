package com.tec.minipc.core;

import com.tec.minipc.model.SecondaryStorage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Servicio de archivos para INT 21H; el nombre se obtiene del registro DX. */
public final class SimulatedFileSystem {
    public static final int MAX_ARCHIVOS = SecondaryStorage.MAX_ARCHIVOS_SISTEMA;
    /** Conserva el estado de apertura y la posición de lectura/escritura de un archivo por proceso. */
    private static final class Descriptor {
        final String nombre;
        int posicion;
        /**
         * Inicializa Descriptor con los recursos y valores recibidos.
         * @param nombre valor inicial usado por la instancia
         */
        Descriptor(String nombre) { this.nombre = nombre; }
    }

    private final SecondaryStorage disco;
    private final Map<Integer, Descriptor> abiertosPorPid = new LinkedHashMap<>();
    private final List<String> eventos = new ArrayList<>();

    /**
     * Inicializa SimulatedFileSystem con los recursos y valores recibidos.
     * @param disco almacenamiento secundario utilizado
     */
    public SimulatedFileSystem(SecondaryStorage disco) { this.disco = disco; }

    /** Funciones: AH=3Ch crear, 3Dh abrir, 3Eh cerrar, 4Dh leer, 40h escribir, 41h eliminar. */
    public int invocar(int pid, int funcion, String nombre, int datoAl) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalStateException("DX debe contener un nombre; use MOV DX, \"archivo.txt\".");
        }
        // AH selecciona la operación de archivo; el servicio la despacha a la rutina correspondiente.
        switch (funcion) {
            case 0x3C: return crear(pid, nombre);
            case 0x3D: return abrir(pid, nombre);
            case 0x3E: return cerrar(pid);
            case 0x4D: return leer(pid);
            case 0x40: return escribir(pid, datoAl);
            case 0x41: return eliminar(pid, nombre);
            default: throw new IllegalStateException(String.format("Función INT 21H no implementada: AH=%02XH.", funcion));
        }
    }

    /**
     * Crea un archivo en el disco simulado y lo deja abierto para este proceso.
     * @param pid identificador del proceso
     * @param nombre nombre del elemento
     * @return valor calculado o estado consultado.
     */
    private int crear(int pid, String nombre) {
        exigirSinArchivoAbierto(pid);
        disco.crearArchivoSistema(nombre);
        abiertosPorPid.put(pid, new Descriptor(nombre));
        registrar(pid, "creó y abrió " + nombre);
        return 0;
    }

    /**
     * Abre un archivo existente y crea su descriptor de lectura/escritura.
     * @param pid identificador del proceso
     * @param nombre nombre del elemento
     * @return valor calculado o estado consultado.
     */
    private int abrir(int pid, String nombre) {
        exigirSinArchivoAbierto(pid);
        if (!disco.existeArchivoSistema(nombre)) throw new IllegalStateException("El archivo " + nombre + " no existe.");
        abiertosPorPid.put(pid, new Descriptor(nombre));
        registrar(pid, "abrió " + nombre);
        return 0;
    }

    /**
     * Lee el byte de la posición actual y avanza el descriptor; devuelve cero al llegar al final.
     * @param pid identificador del proceso
     * @return valor, objeto o colección descrita en el resumen del método.
     */
    private int leer(int pid) {
        Descriptor descriptor = descriptor(pid);
        byte[] contenido = disco.leerArchivoSistema(descriptor.nombre);
        if (descriptor.posicion >= contenido.length) {
            registrar(pid, "leyó EOF de " + descriptor.nombre);
            return 0;
        }
        int dato = contenido[descriptor.posicion++] & 0xFF;
        registrar(pid, "leyó byte " + dato + " de " + descriptor.nombre);
        return dato;
    }

    /**
     * Cierra y elimina el descriptor abierto por el proceso.
     * @param pid identificador del proceso
     * @return valor calculado o estado consultado.
     */
    private int cerrar(int pid) {
        Descriptor descriptor = abiertosPorPid.remove(pid);
        if (descriptor == null) throw new IllegalStateException("El proceso no tiene un archivo abierto.");
        registrar(pid, "cerró " + descriptor.nombre);
        return 0;
    }

    /**
     * Escribe un byte en la posición actual y avanza el descriptor.
     * @param pid identificador del proceso
     * @param dato byte que se escribirá en el archivo
     * @return valor calculado o estado consultado.
     */
    private int escribir(int pid, int dato) {
        Descriptor descriptor = descriptor(pid);
        if (dato < 0 || dato > 255) throw new IllegalStateException("AL debe contener un byte entre 0 y 255.");
        byte[] anterior = disco.leerArchivoSistema(descriptor.nombre);
        // Se conserva el contenido previo y se amplía únicamente hasta incluir la posición escrita.
        int nuevaLongitud = Math.max(anterior.length, descriptor.posicion + 1);
        byte[] nuevo = new byte[nuevaLongitud];
        System.arraycopy(anterior, 0, nuevo, 0, anterior.length);
        nuevo[descriptor.posicion] = (byte) dato;
        disco.actualizarArchivoSistema(descriptor.nombre, nuevo);
        descriptor.posicion++;
        registrar(pid, "escribió byte " + dato + " en " + descriptor.nombre);
        return 1;
    }

    /**
     * Elimina el estado actual.
     * @param pid identificador del proceso
     * @param nombre nombre del elemento que se consultará o modificará
     * @return resultado generado por la operación.
     */
    private int eliminar(int pid, String nombre) {
        if (abiertosPorPid.values().stream().anyMatch(d -> d.nombre.equals(nombre))) {
            throw new IllegalStateException("Cierre el archivo antes de eliminarlo.");
        }
        disco.eliminarArchivoSistema(nombre);
        registrar(pid, "eliminó " + nombre);
        return 0;
    }

    /**
     * Impide que el proceso abra otro archivo antes de cerrar el descriptor actual.
     * @param pid identificador del proceso
     */
    private void exigirSinArchivoAbierto(int pid) {
        if (abiertosPorPid.containsKey(pid)) throw new IllegalStateException("El proceso ya tiene un archivo abierto.");
    }

    /**
     * Obtiene el descriptor del proceso o informa que no tiene archivo abierto.
     * @param pid identificador del proceso
     * @return valor calculado o estado consultado.
     */
    private Descriptor descriptor(int pid) {
        Descriptor descriptor = abiertosPorPid.get(pid);
        if (descriptor == null) throw new IllegalStateException("El proceso no tiene un archivo abierto.");
        return descriptor;
    }

    /**
     * Cierra el descriptor que pertenezca al proceso.
     * @param pid identificador del proceso
     */
    public void cerrarProceso(int pid) {
        Descriptor descriptor = abiertosPorPid.remove(pid);
        if (descriptor != null) registrar(pid, "cerró automáticamente " + descriptor.nombre + " al finalizar");
    }

    /**
     * Registra.
     * @param pid identificador del proceso
     * @param evento descripción del suceso que se agrega al historial
     */
    private void registrar(int pid, String evento) { eventos.add("PID " + pid + " " + evento); }
    /**
     * Devuelve una vista no modificable del historial de operaciones.
     * @return vista de los elementos correspondientes.
     */
    public List<String> getEventos() { return Collections.unmodifiableList(eventos); }
    /**
     * Devuelve los nombres de archivo abiertos por el proceso.
     * @param pid identificador del proceso
     * @return vista de los elementos correspondientes.
     */
    public List<String> getArchivosAbiertos(int pid) {
        Descriptor descriptor = abiertosPorPid.get(pid);
        return descriptor == null ? Collections.emptyList() : Collections.singletonList(descriptor.nombre);
    }
    /**
     * Devuelve una vista textual de los archivos almacenados.
     * @return vista de los elementos correspondientes.
     */
    public List<String> getArchivos() {
        List<String> resultado = new ArrayList<>();
        for (SecondaryStorage.ArchivoSistema archivo : disco.getArchivosSistema()) {
            String contenido = new String(archivo.getContenido(), StandardCharsets.ISO_8859_1);
            String visible = contenido.replaceAll("[^\\x20-\\x7E]", "?");
            resultado.add(archivo.getNombre() + " · dir " + archivo.getInicio() + " · "
                    + archivo.getTamano() + " bytes · \"" + visible + "\"");
        }
        return Collections.unmodifiableList(resultado);
    }
}
