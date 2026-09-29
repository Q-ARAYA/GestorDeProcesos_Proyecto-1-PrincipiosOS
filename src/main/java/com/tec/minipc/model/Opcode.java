/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tec.minipc.model;

/**
 * Código de operación del set de instrucciones del Mini PC.
 * Ocupa los 4 bits altos (bits 0-3) de la primera palabra de la instrucción.
 *
 * 0001 LOAD
 * 0010 STORE
 * 0011 MOV
 * 0100 SUB
 * 0101 ADD
 */
public enum Opcode {
    LOAD(0b0001),
    STORE(0b0010),
    MOV(0b0011),
    SUB(0b0100),
    ADD(0b0101);

    private final int code;

    Opcode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * Busca el Opcode a partir de su valor de 4 bits.
     * @throws IllegalArgumentException si el código no corresponde a ninguna operación válida.
     */
    public static Opcode fromCode(int code) {
        for (Opcode op : values()) {
            if (op.code == code) {
                return op;
            }
        }
        throw new IllegalArgumentException("Código de operación inválido: " + code);
    }

    /**
     * Busca el Opcode a partir del mnemónico de texto (ej: "LOAD", "mov").
     * @throws IllegalArgumentException si el mnemónico no existe.
     */
    public static Opcode fromMnemonic(String mnemonic) {
        try {
            return Opcode.valueOf(mnemonic.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Instrucción no reconocida: " + mnemonic);
        }
    }
}