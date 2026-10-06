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
    private static final class Descriptor {
        final String nombre;
        int posicion;
        Descriptor(String nombre) { this.nombre = nombre; }
    }

    private final SecondaryStorage disco;
    private final Map<Integer, Descriptor> abiertosPorPid = new LinkedHashMap<>();
    private final List<String> eventos = new ArrayList<>();

    public SimulatedFileSystem(SecondaryStorage disco) { this.disco = disco; }

    /** Funciones: AH=3Ch crear, 3Dh abrir, 3Eh cerrar, 4Dh leer, 40h escribir, 41h eliminar. */
    public int invocar(int pid, int funcion, String nombre, int datoAl) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalStateException("DX debe contener un nombre; use MOV DX, \"archivo.txt\".");
        }
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

    private int crear(int pid, String nombre) {
        exigirSinArchivoAbierto(pid);
        disco.crearArchivoSistema(nombre);
        abiertosPorPid.put(pid, new Descriptor(nombre));
        registrar(pid, "creó y abrió " + nombre);
        return 0;
    }

    private int abrir(int pid, String nombre) {
        exigirSinArchivoAbierto(pid);
        if (!disco.existeArchivoSistema(nombre)) throw new IllegalStateException("El archivo " + nombre + " no existe.");
        abiertosPorPid.put(pid, new Descriptor(nombre));
        registrar(pid, "abrió " + nombre);
        return 0;
    }

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

    private int cerrar(int pid) {
        Descriptor descriptor = abiertosPorPid.remove(pid);
        if (descriptor == null) throw new IllegalStateException("El proceso no tiene un archivo abierto.");
        registrar(pid, "cerró " + descriptor.nombre);
        return 0;
    }

    private int escribir(int pid, int dato) {
        Descriptor descriptor = descriptor(pid);
        if (dato < 0 || dato > 255) throw new IllegalStateException("AL debe contener un byte entre 0 y 255.");
        byte[] anterior = disco.leerArchivoSistema(descriptor.nombre);
        int nuevaLongitud = Math.max(anterior.length, descriptor.posicion + 1);
        byte[] nuevo = new byte[nuevaLongitud];
        System.arraycopy(anterior, 0, nuevo, 0, anterior.length);
        nuevo[descriptor.posicion] = (byte) dato;
        disco.actualizarArchivoSistema(descriptor.nombre, nuevo);
        descriptor.posicion++;
        registrar(pid, "escribió byte " + dato + " en " + descriptor.nombre);
        return 1;
    }

    private int eliminar(int pid, String nombre) {
        if (abiertosPorPid.values().stream().anyMatch(d -> d.nombre.equals(nombre))) {
            throw new IllegalStateException("Cierre el archivo antes de eliminarlo.");
        }
        disco.eliminarArchivoSistema(nombre);
        registrar(pid, "eliminó " + nombre);
        return 0;
    }

    private void exigirSinArchivoAbierto(int pid) {
        if (abiertosPorPid.containsKey(pid)) throw new IllegalStateException("El proceso ya tiene un archivo abierto.");
    }

    private Descriptor descriptor(int pid) {
        Descriptor descriptor = abiertosPorPid.get(pid);
        if (descriptor == null) throw new IllegalStateException("El proceso no tiene un archivo abierto.");
        return descriptor;
    }

    public void cerrarProceso(int pid) {
        Descriptor descriptor = abiertosPorPid.remove(pid);
        if (descriptor != null) registrar(pid, "cerró automáticamente " + descriptor.nombre + " al finalizar");
    }

    private void registrar(int pid, String evento) { eventos.add("PID " + pid + " " + evento); }
    public List<String> getEventos() { return Collections.unmodifiableList(eventos); }
    public List<String> getArchivosAbiertos(int pid) {
        Descriptor descriptor = abiertosPorPid.get(pid);
        return descriptor == null ? Collections.emptyList() : Collections.singletonList(descriptor.nombre);
    }
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
