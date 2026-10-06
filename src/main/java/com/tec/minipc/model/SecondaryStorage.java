package com.tec.minipc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Disco secundario simulado: índice fijo al inicio, archivos y región virtual reservada al final. */
public final class SecondaryStorage {
    public static final int TAMANO_POR_DEFECTO = 512;
    public static final int MEMORIA_VIRTUAL_POR_DEFECTO = 64;
    public static final int MAX_ARCHIVOS = 5;
    public static final int CELDAS_POR_INDICE = 2;
    public static final int BYTES_POR_PAGINA = 8;

    public static final class Archivo {
        private final int pid;
        private final String nombre;
        private final int inicio;
        private final int tamano;
        private final int paginasVirtuales;

        private Archivo(int pid, String nombre, int inicio, int tamano, int paginasVirtuales) {
            this.pid = pid;
            this.nombre = nombre;
            this.inicio = inicio;
            this.tamano = tamano;
            this.paginasVirtuales = paginasVirtuales;
        }
        public int getPid() { return pid; }
        public String getNombre() { return nombre; }
        public int getInicio() { return inicio; }
        public int getTamano() { return tamano; }
        public int getPaginasVirtuales() { return paginasVirtuales; }
    }

    private final int capacidad;
    private final int memoriaVirtual;
    private final int celdasIndice = MAX_ARCHIVOS * CELDAS_POR_INDICE;
    private final String[] celdas;
    private final List<Archivo> archivos = new ArrayList<>();
    private int siguienteDato;
    private int paginasVirtualesUsadas;
    private int bytesArchivosSistema;

    public SecondaryStorage(int capacidad, int memoriaVirtual) {
        if (capacidad < 128) throw new IllegalArgumentException("El disco secundario debe tener al menos 128 celdas.");
        if (memoriaVirtual < 0 || memoriaVirtual > capacidad - celdasIndice) {
            throw new IllegalArgumentException("La memoria virtual debe estar entre 0 y " + (capacidad - celdasIndice) + " celdas.");
        }
        this.capacidad = capacidad;
        this.memoriaVirtual = memoriaVirtual;
        this.celdas = new String[capacidad];
        for (int i = 0; i < MAX_ARCHIVOS; i++) {
            celdas[i * CELDAS_POR_INDICE] = "ÍNDICE: nombre";
            celdas[i * CELDAS_POR_INDICE + 1] = "ÍNDICE: dirección/tamaño";
        }
        this.siguienteDato = celdasIndice;
    }

    public boolean puedeGuardar(List<Instruction> instrucciones) {
        int bytes = contarBytes(instrucciones);
        int paginas = (bytes + BYTES_POR_PAGINA - 1) / BYTES_POR_PAGINA;
        return archivos.size() < MAX_ARCHIVOS && siguienteDato + bytes + bytesArchivosSistema <= inicioMemoriaVirtual()
                && paginasVirtualesUsadas + paginas <= memoriaVirtual;
    }

    public void reservarBytesSistema(int diferencia) {
        if (diferencia > getCeldasDatosDisponibles()) {
            throw new IllegalStateException("No hay espacio suficiente en el disco secundario.");
        }
        if (bytesArchivosSistema + diferencia < 0) throw new IllegalStateException("La reserva de disco quedaría inconsistente.");
        bytesArchivosSistema += diferencia;
    }

    public Archivo guardar(int pid, String nombre, List<Instruction> instrucciones) {
        if (archivos.size() >= MAX_ARCHIVOS) throw new IllegalStateException("El índice del disco admite hasta " + MAX_ARCHIVOS + " programas.");
        int bytes = contarBytes(instrucciones);
        int paginas = (bytes + BYTES_POR_PAGINA - 1) / BYTES_POR_PAGINA;
        if (siguienteDato + bytes + bytesArchivosSistema > inicioMemoriaVirtual()) {
            throw new IllegalStateException("No hay espacio en el disco secundario para guardar " + nombre + ".");
        }
        if (paginasVirtualesUsadas + paginas > memoriaVirtual) {
            throw new IllegalStateException("La región de memoria virtual no tiene páginas suficientes para " + nombre + ".");
        }
        int inicio = siguienteDato;
        int cursor = inicio;
        for (Instruction instruccion : instrucciones) {
            for (int valor : instruccion.encode()) celdas[cursor++] = String.format("%02X", valor);
        }
        siguienteDato = cursor;
        int indice = archivos.size() * CELDAS_POR_INDICE;
        celdas[indice] = "PID " + pid + ": " + nombre;
        celdas[indice + 1] = "Dir " + inicio + " · " + bytes + " celdas";
        int inicioPaginaVirtual = inicioMemoriaVirtual() + paginasVirtualesUsadas;
        for (int pagina = 0; pagina < paginas; pagina++) {
            int direccionDisco = inicio + pagina * BYTES_POR_PAGINA;
            celdas[inicioPaginaVirtual + pagina] = "PID " + pid + " VPN " + pagina + " → disco " + direccionDisco;
        }
        paginasVirtualesUsadas += paginas;
        Archivo archivo = new Archivo(pid, nombre, inicio, bytes, paginas);
        archivos.add(archivo);
        return archivo;
    }

    private int contarBytes(List<Instruction> instrucciones) {
        int total = 0;
        for (Instruction instruccion : instrucciones) total += instruccion.encode().length;
        return total;
    }

    private int inicioMemoriaVirtual() { return capacidad - memoriaVirtual; }
    public int getCapacidad() { return capacidad; }
    public int getMemoriaVirtual() { return memoriaVirtual; }
    public int getCeldasIndice() { return celdasIndice; }
    public int getInicioMemoriaVirtual() { return inicioMemoriaVirtual(); }
    public int getCeldasDatosUsadas() { return siguienteDato - celdasIndice + bytesArchivosSistema; }
    public int getCeldasDatosDisponibles() { return inicioMemoriaVirtual() - siguienteDato - bytesArchivosSistema; }
    public int getPaginasVirtualesUsadas() { return paginasVirtualesUsadas; }
    public int getBytesArchivosSistema() { return bytesArchivosSistema; }
    public List<Archivo> getArchivos() { return Collections.unmodifiableList(archivos); }
    public String getValor(int direccion) {
        if (direccion < 0 || direccion >= capacidad) throw new IndexOutOfBoundsException("Dirección de disco fuera de rango: " + direccion);
        return celdas[direccion] == null ? "" : celdas[direccion];
    }
}
