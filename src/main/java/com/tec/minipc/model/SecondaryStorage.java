package com.tec.minipc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Disco secundario simulado con índice, archivos binarios y tabla de páginas al final. */
public final class SecondaryStorage {
    public static final int TAMANO_POR_DEFECTO = 512;
    public static final int MEMORIA_VIRTUAL_POR_DEFECTO = 64;
    public static final int MAX_ARCHIVOS = 5;
    public static final int MAX_ARCHIVOS_SISTEMA = 10;
    public static final int CELDAS_POR_INDICE = 2;
    public static final int CELDAS_INDICE = (MAX_ARCHIVOS + MAX_ARCHIVOS_SISTEMA) * CELDAS_POR_INDICE;
    public static final int BYTES_POR_PAGINA = 8;

    public static final class Archivo {
        private final int pid;
        private final String nombre;
        private final int inicio;
        private final int tamano;
        private final int paginasVirtuales;
        private final List<Instruction> codigo;
        private Archivo(int pid, String nombre, int inicio, int tamano, int paginasVirtuales, List<Instruction> codigo) {
            this.pid = pid; this.nombre = nombre; this.inicio = inicio;
            this.tamano = tamano; this.paginasVirtuales = paginasVirtuales;
            this.codigo = Collections.unmodifiableList(new ArrayList<>(codigo));
        }
        public int getPid() { return pid; }
        public String getNombre() { return nombre; }
        public int getInicio() { return inicio; }
        public int getTamano() { return tamano; }
        public int getPaginasVirtuales() { return paginasVirtuales; }
        public List<Instruction> getCodigo() { return codigo; }
    }

    public static final class ArchivoSistema {
        private final String nombre;
        private final int indice;
        private int inicio;
        private int celdasReservadas;
        private byte[] contenido = new byte[0];
        private ArchivoSistema(String nombre, int indice, int inicio) {
            this.nombre = nombre; this.indice = indice; this.inicio = inicio;
            this.celdasReservadas = 1;
        }
        public String getNombre() { return nombre; }
        public int getInicio() { return inicio; }
        public int getTamano() { return contenido.length; }
        public byte[] getContenido() { return contenido.clone(); }
    }

    private final int capacidad;
    private final int memoriaVirtual;
    private final String[] celdas;
    private final List<Archivo> archivos = new ArrayList<>();
    private final List<ArchivoSistema> archivosSistema = new ArrayList<>();
    private final List<String> eventosMemoriaVirtual = new ArrayList<>();
    private int paginasVirtualesUsadas;

    public SecondaryStorage(int capacidad, int memoriaVirtual) {
        if (capacidad < CELDAS_INDICE + 1) {
            throw new IllegalArgumentException("El disco secundario debe tener al menos " + (CELDAS_INDICE + 1) + " celdas.");
        }
        if (memoriaVirtual < 0 || memoriaVirtual > capacidad - CELDAS_INDICE) {
            throw new IllegalArgumentException("La memoria virtual debe estar entre 0 y " + (capacidad - CELDAS_INDICE) + " páginas.");
        }
        this.capacidad = capacidad;
        this.memoriaVirtual = memoriaVirtual;
        this.celdas = new String[capacidad];
        for (int entrada = 0; entrada < CELDAS_INDICE / CELDAS_POR_INDICE; entrada++) {
            celdas[entrada * CELDAS_POR_INDICE] = "ÍNDICE libre: nombre";
            celdas[entrada * CELDAS_POR_INDICE + 1] = "ÍNDICE libre: dirección/tamaño";
        }
    }

    public boolean puedeGuardar(List<Instruction> instrucciones) {
        int bytes = contarBytes(instrucciones);
        int paginas = (bytes + BYTES_POR_PAGINA - 1) / BYTES_POR_PAGINA;
        return archivos.size() < MAX_ARCHIVOS && buscarHueco(bytes, -1, 0) >= 0
                && paginasVirtualesUsadas + paginas <= memoriaVirtual;
    }

    public Archivo guardar(int pid, String nombre, List<Instruction> instrucciones) {
        if (archivos.size() >= MAX_ARCHIVOS) throw new IllegalStateException("El índice admite hasta " + MAX_ARCHIVOS + " programas.");
        int bytes = contarBytes(instrucciones);
        int paginas = (bytes + BYTES_POR_PAGINA - 1) / BYTES_POR_PAGINA;
        int inicio = buscarHueco(bytes, -1, 0);
        if (inicio < 0) throw new IllegalStateException("No hay espacio contiguo en el disco para " + nombre + ".");
        if (paginasVirtualesUsadas + paginas > memoriaVirtual) {
            throw new IllegalStateException("La región virtual no tiene páginas suficientes para " + nombre + ".");
        }
        int cursor = inicio;
        for (Instruction instruccion : instrucciones) {
            for (int valor : instruccion.encode()) celdas[cursor++] = String.format("%02X", valor);
        }
        int indice = archivos.size() * CELDAS_POR_INDICE;
        celdas[indice] = "PID " + pid + ": " + nombre;
        celdas[indice + 1] = "Dir " + inicio + " · " + bytes + " bytes";
        int inicioPaginaVirtual = inicioMemoriaVirtual() + paginasVirtualesUsadas;
        for (int pagina = 0; pagina < paginas; pagina++) {
            celdas[inicioPaginaVirtual + pagina] = "PID " + pid + " VPN " + pagina + " → disco "
                    + (inicio + pagina * BYTES_POR_PAGINA);
        }
        paginasVirtualesUsadas += paginas;
        Archivo archivo = new Archivo(pid, nombre, inicio, bytes, paginas, instrucciones);
        archivos.add(archivo);
        return archivo;
    }

    public ArchivoSistema crearArchivoSistema(String nombre) {
        validarNombre(nombre);
        if (buscarArchivoSistema(nombre) != null) throw new IllegalStateException("El archivo " + nombre + " ya existe.");
        if (archivosSistema.size() >= MAX_ARCHIVOS_SISTEMA) {
            throw new IllegalStateException("El índice admite hasta " + MAX_ARCHIVOS_SISTEMA + " archivos de usuario.");
        }
        int indice = buscarIndiceSistema();
        int inicio = buscarHueco(1, -1, 0);
        if (indice < 0) throw new IllegalStateException("No hay una entrada libre en el índice del disco.");
        if (inicio < 0) throw new IllegalStateException("No hay celdas libres para crear el archivo.");
        ArchivoSistema archivo = new ArchivoSistema(nombre, indice, inicio);
        celdas[inicio] = "00";
        archivosSistema.add(archivo);
        actualizarIndice(archivo);
        return archivo;
    }

    public void actualizarArchivoSistema(String nombre, byte[] contenido) {
        ArchivoSistema archivo = buscarArchivoSistema(nombre);
        if (archivo == null) throw new IllegalStateException("El archivo " + nombre + " no existe en el disco.");
        byte[] nuevo = contenido == null ? new byte[0] : contenido.clone();
        int requerido = Math.max(1, nuevo.length);
        int inicio = buscarHueco(requerido, archivo.inicio, archivo.celdasReservadas);
        if (inicio < 0) throw new IllegalStateException("No hay celdas contiguas disponibles para guardar " + nombre + ".");
        limpiarBloque(archivo.inicio, archivo.celdasReservadas);
        archivo.inicio = inicio;
        archivo.celdasReservadas = requerido;
        archivo.contenido = nuevo;
        for (int i = 0; i < requerido; i++) {
            celdas[inicio + i] = i < nuevo.length ? String.format("%02X", nuevo[i] & 0xFF) : "00";
        }
        actualizarIndice(archivo);
    }

    public byte[] leerArchivoSistema(String nombre) {
        ArchivoSistema archivo = buscarArchivoSistema(nombre);
        if (archivo == null) throw new IllegalStateException("El archivo " + nombre + " no existe en el disco.");
        return archivo.getContenido();
    }

    public List<Instruction> leerPrograma(int pid) {
        for (Archivo archivo : archivos) if (archivo.pid == pid) return archivo.getCodigo();
        throw new IllegalStateException("No existe una imagen de respaldo en disco para el PID " + pid + ".");
    }

    /** Registra las páginas traídas desde el respaldo al bloque residente de memoria principal. */
    public void registrarEntradaEnMemoria(int pid, int base) {
        Archivo archivo = buscarPrograma(pid);
        int pagina = 0;
        int byteOffset = 0;
        for (int i = 0; i < archivo.codigo.size(); i++) {
            int siguienteOffset = byteOffset + archivo.codigo.get(i).encode().length;
            int primeraPagina = byteOffset / BYTES_POR_PAGINA;
            int ultimaPagina = Math.max(byteOffset, siguienteOffset - 1) / BYTES_POR_PAGINA;
            while (pagina <= ultimaPagina) {
                eventosMemoriaVirtual.add("PAGE IN PID " + pid + " VPN " + pagina + " desde disco "
                        + (archivo.inicio + pagina * BYTES_POR_PAGINA) + " hacia RAM " + (base + i));
                pagina++;
            }
            byteOffset = siguienteOffset;
        }
    }

    public void registrarLiberacionDeMemoria(int pid) {
        eventosMemoriaVirtual.add("PID " + pid + " libera su región RAM; la imagen de respaldo permanece en el disco.");
    }

    private Archivo buscarPrograma(int pid) {
        for (Archivo archivo : archivos) if (archivo.pid == pid) return archivo;
        throw new IllegalStateException("No existe una imagen de respaldo en disco para el PID " + pid + ".");
    }

    public boolean existeArchivoSistema(String nombre) { return buscarArchivoSistema(nombre) != null; }

    public void eliminarArchivoSistema(String nombre) {
        ArchivoSistema archivo = buscarArchivoSistema(nombre);
        if (archivo == null) throw new IllegalStateException("El archivo " + nombre + " no existe en el disco.");
        limpiarBloque(archivo.inicio, archivo.celdasReservadas);
        celdas[archivo.indice] = "ÍNDICE libre: nombre";
        celdas[archivo.indice + 1] = "ÍNDICE libre: dirección/tamaño";
        archivosSistema.remove(archivo);
    }

    private int buscarIndiceSistema() {
        int primerIndiceSistema = MAX_ARCHIVOS * CELDAS_POR_INDICE;
        int finIndice = CELDAS_INDICE;
        for (int indice = primerIndiceSistema; indice < finIndice; indice += CELDAS_POR_INDICE) {
            if (celdas[indice] == null || celdas[indice].startsWith("ÍNDICE libre")) return indice;
        }
        return -1;
    }

    private void actualizarIndice(ArchivoSistema archivo) {
        celdas[archivo.indice] = archivo.nombre;
        celdas[archivo.indice + 1] = "Dir " + archivo.inicio + " · " + archivo.contenido.length + " bytes";
    }

    private ArchivoSistema buscarArchivoSistema(String nombre) {
        for (ArchivoSistema archivo : archivosSistema) if (archivo.nombre.equals(nombre)) return archivo;
        return null;
    }

    private int buscarHueco(int requerido, int ignorarInicio, int ignorarTamano) {
        int limite = inicioMemoriaVirtual() - requerido;
        for (int inicio = CELDAS_INDICE; inicio <= limite; inicio++) {
            boolean libre = true;
            for (int i = 0; i < requerido; i++) {
                int direccion = inicio + i;
                boolean enBloqueIgnorado = direccion >= ignorarInicio && direccion < ignorarInicio + ignorarTamano;
                if (celdas[direccion] != null && !enBloqueIgnorado) { libre = false; break; }
            }
            if (libre) return inicio;
        }
        return -1;
    }

    private void limpiarBloque(int inicio, int tamano) {
        for (int i = 0; i < tamano; i++) celdas[inicio + i] = null;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || !nombre.matches("[A-Za-z0-9_.-]{1,64}")) {
            throw new IllegalStateException("El nombre debe tener 1–64 caracteres: letras, números, punto, guion o guion bajo.");
        }
    }

    private int contarBytes(List<Instruction> instrucciones) {
        int total = 0;
        for (Instruction instruccion : instrucciones) total += instruccion.encode().length;
        return total;
    }

    private int inicioMemoriaVirtual() { return capacidad - memoriaVirtual; }
    public int getCapacidad() { return capacidad; }
    public int getMemoriaVirtual() { return memoriaVirtual; }
    public int getCeldasIndice() { return CELDAS_INDICE; }
    public int getInicioMemoriaVirtual() { return inicioMemoriaVirtual(); }
    public int getCeldasDatosUsadas() {
        int usadas = 0;
        for (int i = CELDAS_INDICE; i < inicioMemoriaVirtual(); i++) if (celdas[i] != null) usadas++;
        return usadas;
    }
    public int getCeldasDatosDisponibles() { return inicioMemoriaVirtual() - CELDAS_INDICE - getCeldasDatosUsadas(); }
    public int getPaginasVirtualesUsadas() { return paginasVirtualesUsadas; }
    public int getBytesArchivosSistema() {
        int total = 0;
        for (ArchivoSistema archivo : archivosSistema) total += archivo.contenido.length;
        return total;
    }
    public List<Archivo> getArchivos() { return Collections.unmodifiableList(archivos); }
    public List<ArchivoSistema> getArchivosSistema() { return Collections.unmodifiableList(archivosSistema); }
    public List<String> getEventosMemoriaVirtual() { return Collections.unmodifiableList(eventosMemoriaVirtual); }
    public String getValor(int direccion) {
        if (direccion < 0 || direccion >= capacidad) throw new IndexOutOfBoundsException("Dirección de disco fuera de rango: " + direccion);
        return celdas[direccion] == null ? "" : celdas[direccion];
    }
}
