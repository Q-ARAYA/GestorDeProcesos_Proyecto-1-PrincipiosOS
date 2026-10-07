/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tec.minipc.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Banco de registros del Mini PC:
 * AC          -> Acumulador
 * AX,BX,CX,DX -> Registros de propósito general
 * PC          -> Program Counter (dirección de la próxima instrucción a buscar)
 * IR          -> Instruction Register (última instrucción decodificada, para mostrar en la UI)
 */
public class Registers {

    private int ac;
    private final Map<RegisterName, Integer> general = new EnumMap<>(RegisterName.class);
    private final Map<RegisterName, String> textos = new EnumMap<>(RegisterName.class);
    private int pc;
    private Instruction ir;
    private boolean zeroFlag;
    private boolean overflowFlag;

    /** Inicializa Registers con los recursos y valores recibidos. */
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
        textos.clear();
        zeroFlag = false;
        overflowFlag = false;
        for (RegisterName r : RegisterName.values()) {
            if (r != RegisterName.NONE) {
                general.put(r, 0);
            }
        }
    }

    /**
     * Devuelve el contenido actual del acumulador AC.
     * @return valor numérico producido por la operación.
     */
    public int getAc() {
        return ac;
    }

    /**
     * Actualiza el acumulador AC.
     * @param value valor de la celda
     */
    public void setAc(int value) {
        this.ac = value;
    }

    /**
     * Devuelve el contenido del registro indicado.
     * @return el valor actual de ese registro
     */
    public int get(RegisterName reg) {
        if (reg == RegisterName.AH) return (get(RegisterName.AX) >>> 8) & 0xFF;
        if (reg == RegisterName.AL) return get(RegisterName.AX) & 0xFF;
        Integer v = general.get(reg);
        return (v == null) ? 0 : v;
    }

    /**
     * Actualiza el registro indicado.
     * @param value el nuevo valor
     */
    public void set(RegisterName reg, int value) {
        if (reg == RegisterName.AH) {
            int ax = get(RegisterName.AX) & 0xFFFF;
            general.put(RegisterName.AX, (ax & 0x00FF) | ((value & 0xFF) << 8));
            return;
        }
        if (reg == RegisterName.AL) {
            int ax = get(RegisterName.AX) & 0xFFFF;
            general.put(RegisterName.AX, (ax & 0xFF00) | (value & 0xFF));
            return;
        }
        if (reg == RegisterName.DX) textos.remove(RegisterName.DX);
        general.put(reg, value);
    }

    /**
     * Devuelve el texto asociado con un registro.
     * @param reg registro que se consultará o actualizará
     * @return valor calculado o estado consultado.
     */
    public String getTexto(RegisterName reg) { return textos.get(reg); }

    /**
     * Asocia un literal textual con el registro indicado.
     * @param reg registro que se consultará o actualizará
     * @param texto texto mostrado
     */
    public void setTexto(RegisterName reg, String texto) {
        if (reg != RegisterName.DX) throw new IllegalArgumentException("Solo DX admite texto en este simulador.");
        if (texto == null) {
            textos.remove(reg);
        } else {
            textos.put(reg, texto);
            general.put(reg, texto.hashCode() & 0xFFFF);
        }
    }

    /**
     * Devuelve el contador de programa.
     * @return valor numérico producido por la operación.
     */
    public int getPc() {
        return pc;
    }

    /**
     * Establece la próxima dirección que ejecutará la CPU.
     * @param pc dirección asignada al contador de programa
     */
    public void setPc(int pc) {
        this.pc = pc;
    }

    /**
     * Avanza el contador de programa la cantidad indicada.
     * @param cells cantidad de celdas a avanzar
     */
    public void advancePc(int cells) {
        this.pc += cells;
    }

    /**
     * Devuelve la instrucción guardada en el registro IR.
     * @return instrucción que se leyó o ejecutó, o null si no se ejecutó ninguna.
     */
    public Instruction getIr() {
        return ir;
    }

    /**
     * Carga la instrucción actual en el registro IR.
     * @param ir instrucción que se cargará en IR
     */
    public void setIr(Instruction ir) {
        this.ir = ir;
    }

    /**
     * Indica si la última comparación o cálculo produjo cero.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean isZeroFlag() { return zeroFlag; }
    /**
     * Actualiza la bandera de cero.
     * @param zeroFlag nuevo valor de la bandera de cero
     */
    public void setZeroFlag(boolean zeroFlag) { this.zeroFlag = zeroFlag; }
    /**
     * Indica si el último cálculo excedió el rango entero simulado.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean isOverflowFlag() { return overflowFlag; }
    /**
     * Actualiza la bandera de desbordamiento.
     * @param overflowFlag nuevo valor de la bandera de desbordamiento
     */
    public void setOverflowFlag(boolean overflowFlag) { this.overflowFlag = overflowFlag; }

    /** Restaura una copia completa del estado de otro banco de registros. */
    public void copyFrom(Registers source) {
        String textoDx = source.getTexto(RegisterName.DX);
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
        textos.clear();
        if (textoDx != null) setTexto(RegisterName.DX, textoDx);
    }
}
