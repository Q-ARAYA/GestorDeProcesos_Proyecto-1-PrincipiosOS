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

    /** Describe una imagen de programa guardada en el almacenamiento secundario. */
    public static final class Archivo {
        private final int pid;
        private final String nombre;
        private final int inicio;
        private final int tamano;
        private final int paginasVirtuales;
        private final List<Instruction> codigo;
        /**
         * Inicializa Archivo con los recursos y valores recibidos.
         * @param pid identificador del proceso
         * @param nombre valor inicial usado por la instancia
         * @param inicio valor inicial usado por la instancia
         * @param tamano valor inicial usado por la instancia
         * @param paginasVirtuales valor inicial usado por la instancia
         * @param codigo valor inicial usado por la instancia
         */
        private Archivo(int pid, String nombre, int inicio, int tamano, int paginasVirtuales, List<Instruction> codigo) {
            this.pid = pid; this.nombre = nombre; this.inicio = inicio;
            this.tamano = tamano; this.paginasVirtuales = paginasVirtuales;
            this.codigo = Collections.unmodifiableList(new ArrayList<>(codigo));
        }
        /**
         * Devuelve el valor actual de PID.
         * @return valor numérico producido por la operación.
         */
        public int getPid() { return pid; }
        /**
         * Devuelve el valor actual de nombre.
         * @return texto asociado con el estado o valor consultado.
         */
        public String getNombre() { return nombre; }
        /**
         * Devuelve el valor actual de inicio.
         * @return valor numérico producido por la operación.
         */
        public int getInicio() { return inicio; }
        /**
         * Devuelve el valor actual de tamaño.
         * @return valor numérico producido por la operación.
         */
        public int getTamano() { return tamano; }
        /**
         * Devuelve el valor actual de páginas virtuales.
         * @return valor numérico producido por la operación.
         */
        public int getPaginasVirtuales() { return paginasVirtuales; }
        /**
         * Devuelve la lista inmutable de instrucciones de la imagen.
         * @return vista de los elementos correspondientes.
         */
        public List<Instruction> getCodigo() { return codigo; }
    }

    /** Representa un archivo de usuario almacenado en el disco simulado. */
    public static final class ArchivoSistema {
        private final String nombre;
        private final int indice;
        private int inicio;
        private int celdasReservadas;
        private byte[] contenido = new byte[0];
        /**
         * Inicializa ArchivoSistema con los recursos y valores recibidos.
         * @param nombre valor inicial usado por la instancia
         * @param indice valor inicial usado por la instancia
         * @param inicio valor inicial usado por la instancia
         */
        private ArchivoSistema(String nombre, int indice, int inicio) {
            this.nombre = nombre; this.indice = indice; this.inicio = inicio;
            this.celdasReservadas = 1;
        }
        /**
         * Devuelve el valor actual de nombre.
         * @return texto asociado con el estado o valor consultado.
         */
        public String getNombre() { return nombre; }
        /**
         * Devuelve el valor actual de inicio.
         * @return valor numérico producido por la operación.
         */
        public int getInicio() { return inicio; }
        /**
         * Devuelve el valor actual de tamaño.
         * @return valor numérico producido por la operación.
         */
        public int getTamano() { return contenido.length; }
        /**
         * Devuelve una copia del contenido del archivo.
         * @return valor calculado o recurso consultado.
         */
        public byte[] getContenido() { return contenido.clone(); }
    }

    private final int capacidad;
    private final int memoriaVirtual;
    private final String[] celdas;
    private final List<Archivo> archivos = new ArrayList<>();
    private final List<ArchivoSistema> archivosSistema = new ArrayList<>();
    private final List<String> eventosMemoriaVirtual = new ArrayList<>();
    private int paginasVirtualesUsadas;

    /**
     * Inicializa SecondaryStorage con los recursos y valores recibidos.
     * @param capacidad capacidad total del almacenamiento
     * @param memoriaVirtual cantidad de memoria virtual configurada
     */
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

    /**
     * Indica si se puede guardar.
     * @param instrucciones instrucciones relacionadas con la operación
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean puedeGuardar(List<Instruction> instrucciones) {
        int bytes = contarBytes(instrucciones);
        int paginas = (bytes + BYTES_POR_PAGINA - 1) / BYTES_POR_PAGINA;
        return archivos.size() < MAX_ARCHIVOS && buscarHueco(bytes, -1, 0) >= 0
                && paginasVirtualesUsadas + paginas <= memoriaVirtual;
    }

    /**
     * Guarda el estado actual.
     * @param pid identificador del proceso
     * @param nombre nombre del elemento que se consultará o modificará
     * @param instrucciones instrucciones relacionadas con la operación
     * @return resultado generado por la operación.
     */
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

    /**
     * Valida el nombre y reserva índice y primera celda para un archivo nuevo.
     * @param nombre nombre del elemento
     * @return valor calculado o estado consultado.
     */
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

    /**
     * Redimensiona el bloque del archivo y actualiza sus bytes e información de índice.
     * @param nombre nombre del elemento que se consultará o modificará
     * @param contenido contenido que se leerá o escribirá
     */
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

    /**
     * Devuelve una copia del contenido de un archivo existente.
     * @param nombre nombre del elemento
     * @return valor calculado o estado consultado.
     */
    public byte[] leerArchivoSistema(String nombre) {
        ArchivoSistema archivo = buscarArchivoSistema(nombre);
        if (archivo == null) throw new IllegalStateException("El archivo " + nombre + " no existe en el disco.");
        return archivo.getContenido();
    }

    /**
     * Lee programa.
     * @param pid identificador del proceso
     * @return vista de los elementos correspondientes.
     */
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

    /**
     * Registra liberacion de memoria.
     * @param pid identificador del proceso
     */
    public void registrarLiberacionDeMemoria(int pid) {
        eventosMemoriaVirtual.add("PID " + pid + " libera su región RAM; la imagen de respaldo permanece en el disco.");
    }

    /**
     * Busca la imagen de programa almacenada para el PID indicado.
     * @param pid identificador del proceso
     * @return valor calculado o recurso consultado.
     */
    private Archivo buscarPrograma(int pid) {
        for (Archivo archivo : archivos) if (archivo.pid == pid) return archivo;
        throw new IllegalStateException("No existe una imagen de respaldo en disco para el PID " + pid + ".");
    }

    /**
     * Indica si existe archivo sistema.
     * @param nombre nombre del elemento que se consultará o modificará
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean existeArchivoSistema(String nombre) { return buscarArchivoSistema(nombre) != null; }

    /**
     * Elimina archivo sistema.
     * @param nombre nombre del elemento que se consultará o modificará
     */
    public void eliminarArchivoSistema(String nombre) {
        ArchivoSistema archivo = buscarArchivoSistema(nombre);
        if (archivo == null) throw new IllegalStateException("El archivo " + nombre + " no existe en el disco.");
        limpiarBloque(archivo.inicio, archivo.celdasReservadas);
        celdas[archivo.indice] = "ÍNDICE libre: nombre";
        celdas[archivo.indice + 1] = "ÍNDICE libre: dirección/tamaño";
        archivosSistema.remove(archivo);
    }

    /**
     * Busca la primera entrada libre reservada para archivos de usuario.
     * @return valor calculado o estado consultado.
     */
    private int buscarIndiceSistema() {
        int primerIndiceSistema = MAX_ARCHIVOS * CELDAS_POR_INDICE;
        int finIndice = CELDAS_INDICE;
        for (int indice = primerIndiceSistema; indice < finIndice; indice += CELDAS_POR_INDICE) {
            if (celdas[indice] == null || celdas[indice].startsWith("ÍNDICE libre")) return indice;
        }
        return -1;
    }

    /**
     * Actualiza indice.
     * @param archivo archivo que se procesará
     */
    private void actualizarIndice(ArchivoSistema archivo) {
        celdas[archivo.indice] = archivo.nombre;
        celdas[archivo.indice + 1] = "Dir " + archivo.inicio + " · " + archivo.contenido.length + " bytes";
    }

    /**
     * Busca un archivo de usuario por su nombre.
     * @param nombre nombre del elemento
     * @return valor calculado o estado consultado.
     */
    private ArchivoSistema buscarArchivoSistema(String nombre) {
        for (ArchivoSistema archivo : archivosSistema) if (archivo.nombre.equals(nombre)) return archivo;
        return null;
    }

    /**
     * Busca hueco.
     * @param requerido cantidad de celdas solicitadas
     * @param ignorarInicio inicio del bloque que puede reutilizarse
     * @param ignorarTamano tamaño del bloque que puede reutilizarse
     * @return valor numérico producido por la operación.
     */
    private int buscarHueco(int requerido, int ignorarInicio, int ignorarTamano) {
        int limite = inicioMemoriaVirtual() - requerido;
        // Primera adecuación: busca un bloque contiguo entre el índice y el inicio de la región virtual.
        for (int inicio = CELDAS_INDICE; inicio <= limite; inicio++) {
            boolean libre = true;
            for (int i = 0; i < requerido; i++) {
                int direccion = inicio + i;
                // Al redimensionar un archivo, su bloque anterior se considera reutilizable durante la búsqueda.
                boolean enBloqueIgnorado = direccion >= ignorarInicio && direccion < ignorarInicio + ignorarTamano;
                if (celdas[direccion] != null && !enBloqueIgnorado) { libre = false; break; }
            }
            if (libre) return inicio;
        }
        return -1;
    }

    /**
     * Libera todas las celdas del bloque de disco indicado.
     * @param inicio dirección inicial del bloque
     * @param tamano tamaño en celdas o bytes
     */
    private void limpiarBloque(int inicio, int tamano) {
        for (int i = 0; i < tamano; i++) celdas[inicio + i] = null;
    }

    /**
     * Valida nombre.
     * @param nombre nombre del elemento que se consultará o modificará
     */
    private static void validarNombre(String nombre) {
        if (nombre == null || !nombre.matches("[A-Za-z0-9_.-]{1,64}")) {
            throw new IllegalStateException("El nombre debe tener 1–64 caracteres: letras, números, punto, guion o guion bajo.");
        }
    }

    /**
     * Suma el tamaño codificado de todas las instrucciones.
     * @param instrucciones lista de instrucciones
     * @return valor calculado o estado consultado.
     */
    private int contarBytes(List<Instruction> instrucciones) {
        int total = 0;
        for (Instruction instruccion : instrucciones) total += instruccion.encode().length;
        return total;
    }

    /**
     * Realiza la operación inicio memoria virtual en SecondaryStorage.
     * @return valor numérico producido por la operación.
     */
    private int inicioMemoriaVirtual() { return capacidad - memoriaVirtual; }
    /**
     * Devuelve el valor actual de capacidad.
     * @return valor numérico producido por la operación.
     */
    public int getCapacidad() { return capacidad; }
    /**
     * Devuelve el valor actual de memoria virtual.
     * @return valor numérico producido por la operación.
     */
    public int getMemoriaVirtual() { return memoriaVirtual; }
    /**
     * Devuelve el valor actual de celdas índice.
     * @return valor numérico producido por la operación.
     */
    public int getCeldasIndice() { return CELDAS_INDICE; }
    /**
     * Devuelve el valor actual de inicio memoria virtual.
     * @return valor numérico producido por la operación.
     */
    public int getInicioMemoriaVirtual() { return inicioMemoriaVirtual(); }
    /**
     * Cuenta las celdas ocupadas entre el índice y la región virtual.
     * @return valor calculado o estado consultado.
     */
    public int getCeldasDatosUsadas() {
        int usadas = 0;
        for (int i = CELDAS_INDICE; i < inicioMemoriaVirtual(); i++) if (celdas[i] != null) usadas++;
        return usadas;
    }
    /**
     * Calcula el espacio libre de datos antes de la región virtual.
     * @return valor calculado o estado consultado.
     */
    public int getCeldasDatosDisponibles() { return inicioMemoriaVirtual() - CELDAS_INDICE - getCeldasDatosUsadas(); }
    /**
     * Devuelve el valor actual de páginas virtuales usadas.
     * @return valor numérico producido por la operación.
     */
    public int getPaginasVirtualesUsadas() { return paginasVirtualesUsadas; }
    /**
     * Suma los bytes ocupados por archivos de usuario.
     * @return valor numérico producido por la operación.
     */
    public int getBytesArchivosSistema() {
        int total = 0;
        for (ArchivoSistema archivo : archivosSistema) total += archivo.contenido.length;
        return total;
    }
    /**
     * Devuelve una vista textual de los archivos almacenados.
     * @return vista de los elementos correspondientes.
     */
    public List<Archivo> getArchivos() { return Collections.unmodifiableList(archivos); }
    /**
     * Devuelve el valor actual de archivos sistema.
     * @return vista de los elementos correspondientes.
     */
    public List<ArchivoSistema> getArchivosSistema() { return Collections.unmodifiableList(archivosSistema); }
    /**
     * Devuelve el valor actual de eventos memoria virtual.
     * @return vista de los elementos correspondientes.
     */
    public List<String> getEventosMemoriaVirtual() { return Collections.unmodifiableList(eventosMemoriaVirtual); }
    /**
     * Devuelve el valor actual de valor.
     * @param direccion dirección consultada
     * @return texto asociado con el estado o valor consultado.
     */
    public String getValor(int direccion) {
        if (direccion < 0 || direccion >= capacidad) throw new IndexOutOfBoundsException("Dirección de disco fuera de rango: " + direccion);
        return celdas[direccion] == null ? "" : celdas[direccion];
    }
}
