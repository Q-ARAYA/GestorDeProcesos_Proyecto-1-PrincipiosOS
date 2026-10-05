/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tec.minipc.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Banco de registros del Mini PC:
 *   AC          -> Acumulador
 *   AX,BX,CX,DX -> Registros de propósito general
 *   PC          -> Program Counter (dirección de la próxima instrucción a buscar)
 *   IR          -> Instruction Register (última instrucción decodificada, para mostrar en la UI)
 */
public class Registers {

    private int ac;
    private final Map<RegisterName, Integer> general = new EnumMap<>(RegisterName.class);
    private int pc;
    private Instruction ir;
    private boolean zeroFlag;
    private boolean overflowFlag;

    public Registers() {
        reset();
    }

    /**
     * Reinicia todos los registros a su valor inicial (0) y limpia el IR.
     * Útil para "reiniciar" la ejecución sin recargar el programa.
     */
    public void reset() {
        ac = 0;
        pc = 0;
        ir = null;
        zeroFlag = false;
        overflowFlag = false;
        for (RegisterName r : RegisterName.values()) {
            if (r != RegisterName.NONE) {
                general.put(r, 0);
            }
        }
    }

    public int getAc() {
        return ac;
    }

    public void setAc(int value) {
        this.ac = value;
    }

    /**
     * @param reg el registro a consultar (AX, BX, CX o DX)
     * @return el valor actual de ese registro
     */
    public int get(RegisterName reg) {
        Integer v = general.get(reg);
        return (v == null) ? 0 : v;
    }

    /**
     * @param reg el registro a modificar
     * @param value el nuevo valor
     */
    public void set(RegisterName reg, int value) {
        general.put(reg, value);
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    /**
     * Avanza el PC la cantidad de celdas indicada (normalmente 2, el tamaño de una instrucción).
     * @param cells cantidad de celdas a avanzar
     */
    public void advancePc(int cells) {
        this.pc += cells;
    }

    public Instruction getIr() {
        return ir;
    }

    public void setIr(Instruction ir) {
        this.ir = ir;
    }

    public boolean isZeroFlag() { return zeroFlag; }
    public void setZeroFlag(boolean zeroFlag) { this.zeroFlag = zeroFlag; }
    public boolean isOverflowFlag() { return overflowFlag; }
    public void setOverflowFlag(boolean overflowFlag) { this.overflowFlag = overflowFlag; }

    /** Restaura una copia completa del estado de otro banco de registros. */
    public void copyFrom(Registers source) {
        this.ac = source.ac;
        this.pc = source.pc;
        this.ir = source.ir;
        this.zeroFlag = source.zeroFlag;
        this.overflowFlag = source.overflowFlag;
        for (RegisterName r : RegisterName.values()) {
            if (r != RegisterName.NONE) {
                this.general.put(r, source.get(r));
            }
        }
    }
}
