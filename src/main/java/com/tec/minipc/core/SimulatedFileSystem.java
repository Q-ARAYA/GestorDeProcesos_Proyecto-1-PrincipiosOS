package com.tec.minipc.core;

import com.tec.minipc.model.SecondaryStorage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Servicio de archivos simulado para INT 21H. DX identifica un archivo con un número de 0 a 255. */
public final class SimulatedFileSystem {
    public static final int MAX_ARCHIVOS = 10;
    private static final class Archivo {
        final int id;
        final String nombre;
        final List<Integer> bytes = new ArrayList<>();
        Archivo(int id) { this.id = id; this.nombre = String.format("archivo_%03d.dat", id); }
    }
    private static final class Descriptor {
        final Archivo archivo;
        int posicion;
        Descriptor(Archivo archivo) { this.archivo = archivo; }
    }

    private final SecondaryStorage disco;
    private final Map<Integer, Archivo> archivos = new LinkedHashMap<>();
    private final Map<Integer, Descriptor> abiertosPorPid = new LinkedHashMap<>();
    private final List<String> eventos = new ArrayList<>();

    public SimulatedFileSystem(SecondaryStorage disco) { this.disco = disco; }

    /** Ejecuta las funciones AH de creación, apertura, cierre, lectura, escritura y eliminación. */
    public int invocar(int pid, int funcion, int idArchivo, int datoAl) {
        if (idArchivo < 0 || idArchivo > 255) throw new IllegalStateException("DX debe contener un ID de archivo de 0 a 255.");
        switch (funcion) {
            case 0x3C: return crear(pid, idArchivo);
            case 0x3D: return abrir(pid, idArchivo);
            case 0x3E: return cerrar(pid);
            case 0x4D: return leer(pid);
            case 0x40: return escribir(pid, datoAl);
            case 0x41: return eliminar(pid, idArchivo);
            default: throw new IllegalStateException(String.format("Función INT 21H no implementada: AH=%02XH.", funcion));
        }
    }

    private int crear(int pid, int id) {
        if (abiertosPorPid.containsKey(pid)) throw new IllegalStateException("Cierre o termine el archivo actual antes de crear otro.");
        if (archivos.containsKey(id)) throw new IllegalStateException("El archivo " + nombre(id) + " ya existe.");
        if (archivos.size() >= MAX_ARCHIVOS) throw new IllegalStateException("El sistema admite hasta " + MAX_ARCHIVOS + " archivos simulados.");
        Archivo archivo = new Archivo(id);
        archivos.put(id, archivo);
        abiertosPorPid.put(pid, new Descriptor(archivo));
        registrar(pid, "creó y abrió " + archivo.nombre);
        return 0;
    }

    private int abrir(int pid, int id) {
        if (abiertosPorPid.containsKey(pid)) throw new IllegalStateException("El proceso ya tiene un archivo abierto.");
        Archivo archivo = archivos.get(id);
        if (archivo == null) throw new IllegalStateException("El archivo " + nombre(id) + " no existe.");
        abiertosPorPid.put(pid, new Descriptor(archivo));
        registrar(pid, "abrió " + archivo.nombre);
        return 0;
    }

    private int leer(int pid) {
        Descriptor descriptor = descriptor(pid);
        if (descriptor.posicion >= descriptor.archivo.bytes.size()) {
            registrar(pid, "leyó EOF de " + descriptor.archivo.nombre);
            return 0;
        }
        int dato = descriptor.archivo.bytes.get(descriptor.posicion++);
        registrar(pid, "leyó " + dato + " de " + descriptor.archivo.nombre);
        return dato;
    }

    private int cerrar(int pid) {
        Descriptor descriptor = abiertosPorPid.remove(pid);
        if (descriptor == null) throw new IllegalStateException("El proceso no tiene un archivo abierto.");
        registrar(pid, "cerró " + descriptor.archivo.nombre);
        return 0;
    }

    private int escribir(int pid, int dato) {
        Descriptor descriptor = descriptor(pid);
        if (dato < 0 || dato > 255) throw new IllegalStateException("AL debe contener un byte entre 0 y 255.");
        boolean agrega = descriptor.posicion >= descriptor.archivo.bytes.size();
        if (agrega) {
            if (disco.getCeldasDatosDisponibles() <= 0) throw new IllegalStateException("Disco lleno: no se puede escribir otro byte.");
            disco.reservarBytesSistema(1);
            descriptor.archivo.bytes.add(dato);
        } else {
            descriptor.archivo.bytes.set(descriptor.posicion, dato);
        }
        descriptor.posicion++;
        registrar(pid, "escribió " + dato + " en " + descriptor.archivo.nombre);
        return 1;
    }

    private int eliminar(int pid, int id) {
        Archivo archivo = archivos.get(id);
        if (archivo == null) throw new IllegalStateException("El archivo " + nombre(id) + " no existe.");
        if (abiertosPorPid.values().stream().anyMatch(d -> d.archivo == archivo)) {
            throw new IllegalStateException("Cierre el archivo antes de eliminarlo.");
        }
        archivos.remove(id);
        disco.reservarBytesSistema(-archivo.bytes.size());
        registrar(pid, "eliminó " + archivo.nombre);
        return 0;
    }

    private Descriptor descriptor(int pid) {
        Descriptor descriptor = abiertosPorPid.get(pid);
        if (descriptor == null) throw new IllegalStateException("El proceso no tiene un archivo abierto.");
        return descriptor;
    }

    public void cerrarProceso(int pid) {
        Descriptor descriptor = abiertosPorPid.remove(pid);
        if (descriptor != null) registrar(pid, "cerró automáticamente " + descriptor.archivo.nombre + " al finalizar");
    }

    private void registrar(int pid, String evento) { eventos.add("PID " + pid + " " + evento); }
    private String nombre(int id) { return String.format("archivo_%03d.dat", id); }
    public List<String> getEventos() { return Collections.unmodifiableList(eventos); }
    public List<String> getArchivosAbiertos(int pid) {
        Descriptor descriptor = abiertosPorPid.get(pid);
        return descriptor == null ? Collections.emptyList() : Collections.singletonList(descriptor.archivo.nombre);
    }
    public List<String> getArchivos() {
        List<String> resultado = new ArrayList<>();
        for (Archivo archivo : archivos.values()) {
            StringBuilder contenido = new StringBuilder();
            for (int dato : archivo.bytes) {
                if (dato >= 32 && dato <= 126) contenido.append((char) dato);
                else contenido.append(String.format("\\x%02X", dato));
            }
            resultado.add(archivo.nombre + " · " + archivo.bytes.size() + " bytes · \"" + contenido + "\"");
        }
        return Collections.unmodifiableList(resultado);
    }
}
