package com.tec.minipc.model;

import java.util.List;

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

    private final int totalSize;
    private final int osSize; // cantidad de celdas reservadas para el S.O. (desde 0)
    private final Instruction[] celdas;

    public Memory(int totalSize, int osSize) {
        if (totalSize < TAMANO_MINIMO) {
            throw new IllegalArgumentException(
                    "El tamaño de memoria debe ser al menos " + TAMANO_MINIMO + " (recibido: " + totalSize + ")");
        }
        if (osSize < 0 || osSize >= totalSize) {
            throw new IllegalArgumentException(
                    "El espacio de S.O. (" + osSize + ") debe ser mayor a 0 y menor que el total (" + totalSize + ")");
        }
        this.totalSize = totalSize;
        this.osSize = osSize;
        this.celdas = new Instruction[totalSize];
    }

    public void clear() {
        for (int i = 0; i < celdas.length; i++) {
            celdas[i] = null;
        }
    }

    public int getTotalSize() {
        return totalSize;
    }

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
        return celdas[address];
    }

    public boolean isEmpty(int address) {
        requireValid(address);
        return celdas[address] == null;
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
        int startAddress = getUserStart();
        int requiredCells = instrucciones.size();
        int availableCells = getUserEnd() - getUserStart() + 1;

        if (requiredCells > availableCells) {
            throw new IllegalStateException(
                    "El programa (" + instrucciones.size() + " instrucciones) no cabe en el espacio de Usuario "
                            + "disponible (" + availableCells + " posiciones)");
        }

        int address = startAddress;
        for (Instruction ins : instrucciones) {
            write(address, ins);
            address++;
        }
        return startAddress;
    }
}