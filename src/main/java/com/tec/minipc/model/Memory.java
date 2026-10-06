package com.tec.minipc.model;

import java.util.List;
import com.tec.minipc.core.PCB;

/**
 * Memoria principal del Mini PC.
 *
 * Se divide en dos zonas contiguas:
 *   [0 .. osSize-1]        -> espacio del Sistema Operativo (reservado, ej. para el BCP)
 *   [osSize .. totalSize-1] -> espacio de Usuario (donde se carga el programa .asm)
 *
 * Tal como lo pide el enunciado, CADA LÍNEA DEL PROGRAMA OCUPA UNA SOLA POSICIÓN
 * de memoria (no dos). Esa posición guarda la instrucción ya parseada; su
 * representación en binario (2 bytes: opcode+registro, y el operando) se calcula
 * bajo demanda con Instruction.encode() solo para mostrarla en la interfaz.
 *
 * Una celda en null se considera "vacía" (nunca escrita).
 */
public class Memory {

    public static final int TAMANO_MINIMO = 128;
    public static final int CELDAS_POR_BCP = 6;
    public static final int MAX_BCPS = 5;

    private final int totalSize;
    private final int osSize; // cantidad de celdas reservadas para el S.O. (desde 0)
    private final Object[] celdas;

    public Memory(int totalSize, int osSize) {
        if (totalSize < TAMANO_MINIMO) {
            throw new IllegalArgumentException(
                    "El tamaño de memoria debe ser al menos " + TAMANO_MINIMO + " (recibido: " + totalSize + ")");
        }
        int minimoKernel = CELDAS_POR_BCP * MAX_BCPS;
        if (osSize < minimoKernel || osSize >= totalSize) {
            throw new IllegalArgumentException("El kernel necesita al menos " + minimoKernel
                    + " celdas para guardar hasta " + MAX_BCPS + " BCP (recibido: " + osSize + ").");
        }
        this.totalSize = totalSize;
        this.osSize = osSize;
        this.celdas = new Object[totalSize];
    }

    public void clear() {
        for (int i = 0; i < celdas.length; i++) {
            celdas[i] = null;
        }
    }

    public int getTotalSize() {
        return totalSize;
    }

    public int getOsSize() { return osSize; }

    public int getOsStart() {
        return 0;
    }

    public int getOsEnd() {
        return osSize - 1;
    }

    public int getUserStart() {
        return osSize;
    }

    public int getUserEnd() {
        return totalSize - 1;
    }

    public boolean isValidAddress(int address) {
        return address >= 0 && address < totalSize;
    }

    public boolean isOsAddress(int address) {
        return address >= getOsStart() && address <= getOsEnd();
    }

    public boolean isUserAddress(int address) {
        return address >= getUserStart() && address <= getUserEnd();
    }

    public void write(int address, Instruction instruccion) {
        requireValid(address);
        celdas[address] = instruccion;
    }

    /** @return la instrucción guardada en esa dirección, o null si está vacía. */
    public Instruction read(int address) {
        requireValid(address);
        return celdas[address] instanceof Instruction ? (Instruction) celdas[address] : null;
    }

    public String getDisplayValue(int address) {
        requireValid(address);
        Object value = celdas[address];
        if (value == null) return "";
        return value instanceof Instruction ? ((Instruction) value).getSourceLine() : String.valueOf(value);
    }

    public boolean isEmpty(int address) {
        requireValid(address);
        return celdas[address] == null;
    }

    /** Reserva un bloque contiguo del kernel y escribe la imagen del BCP. */
    public int guardarBcp(PCB pcb) {
        for (int base = getOsStart(); base + CELDAS_POR_BCP - 1 <= getOsEnd(); base++) {
            boolean libre = true;
            for (int i = 0; i < CELDAS_POR_BCP; i++) {
                if (celdas[base + i] != null) { libre = false; break; }
            }
            if (libre) {
                pcb.setDireccionBcp(base);
                actualizarBcp(pcb);
                return base;
            }
        }
        throw new IllegalStateException("No hay espacio libre en el kernel para almacenar otro BCP.");
    }

    /** Actualiza en memoria la representación persistida del estado del BCP. */
    public void actualizarBcp(PCB pcb) {
        int base = pcb.getDireccionBcp();
        if (base < getOsStart() || base + CELDAS_POR_BCP - 1 > getOsEnd()) {
            throw new IllegalStateException("El BCP no tiene una dirección válida dentro del kernel.");
        }
        celdas[base] = "BCP PID=" + pcb.getPid() + " " + pcb.getNombrePrograma();
        celdas[base + 1] = "Estado=" + pcb.getEstadoDescripcion();
        celdas[base + 2] = "PC=" + pcb.getProgramCounter() + " AC=" + pcb.getAc() + " AX=" + pcb.getAx() + " BX=" + pcb.getBx();
        celdas[base + 3] = "CX=" + pcb.getCx() + " DX=" + pcb.getDx() + " IR=" + pcb.getIrTexto()
                + " ZF=" + pcb.isZeroFlag() + " OF=" + pcb.isOverflowFlag();
        if (pcb.getDxTexto() != null) celdas[base + 3] += " DX(texto)=\"" + pcb.getDxTexto() + "\"";
        celdas[base + 4] = "Base=" + pcb.getDireccionBase() + " Limite=" + pcb.getDireccionLimite() + " Tamaño=" + pcb.getTamanoInstrucciones();
        celdas[base + 5] = "Pila=" + pcb.getPilaTexto() + " CPU(s)=" + pcb.getTiempoCpuSegundos()
                + " Inicio=" + pcb.getHoraInicio() + " Fin=" + pcb.getHoraFin()
                + " Abiertos=" + pcb.getArchivosAbiertos()
                + " Siguiente BCP=" + pcb.getDireccionSiguienteBcp();
    }

    private void requireValid(int address) {
        if (!isValidAddress(address)) {
            throw new IndexOutOfBoundsException(
                    "Dirección de memoria fuera de rango: " + address + " (0.." + (totalSize - 1) + ")");
        }
    }

    /**
     * Carga una lista de instrucciones ya parseadas a partir de userStart,
     * usando UNA celda por instrucción. Valida que el programa quepa en el
     * espacio de Usuario disponible.
     *
     * @param instrucciones lista de instrucciones ya parseadas del .asm
     * @return la dirección (PC) donde debe iniciar la ejecución.
     */
    public int loadProgram(List<Instruction> instrucciones) {
        int requiredCells = instrucciones.size();
        int availableCells = getUserEnd() - getUserStart() + 1;

        if (requiredCells > availableCells) {
            throw new IllegalStateException(
                    "El programa (" + instrucciones.size() + " instrucciones) no cabe en el espacio de Usuario "
                            + "disponible (" + availableCells + " posiciones)");
        }

        return loadProgramAt(instrucciones, getUserStart());
    }

    /** Carga un programa en una dirección libre indicada por el gestor de procesos. */
    public int loadProgramAt(List<Instruction> instrucciones, int startAddress) {
        int requiredCells = instrucciones.size();
        if (startAddress < getUserStart() || startAddress + requiredCells - 1 > getUserEnd()) {
            throw new IllegalStateException("El programa no cabe en el espacio de usuario disponible.");
        }
        for (int i = 0; i < requiredCells; i++) {
            if (!isEmpty(startAddress + i)) {
                throw new IllegalStateException("La región de memoria solicitada ya está ocupada.");
            }
        }
        int address = startAddress;
        for (Instruction ins : instrucciones) {
            write(address, ins);
            address++;
        }
        return startAddress;
    }

    /** Libera una región de usuario cuando su proceso termina. */
    public void release(int startAddress, int size) {
        for (int i = 0; i < size; i++) {
            int address = startAddress + i;
            if (isUserAddress(address)) {
                celdas[address] = null;
            }
        }
    }
}
