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
 * 0110 MOV registro, registro
 * 0111 INC
 * 1000 DEC
 * 1001 SWAP
 * 1010 JMP/JE/JNE (bits de registro seleccionan condición)
 * 1011 CMP
 * 1100 INT
 * 1101 PARAM
 * 1110 PUSH
 * 1111 POP
 */
public enum Opcode {
    LOAD(0b0001),
    STORE(0b0010),
    MOV(0b0011),
    SUB(0b0100),
    ADD(0b0101),
    MOVR(0b0110),
    INC(0b0111),
    DEC(0b1000),
    SWAP(0b1001),
    JMP(0b1010),
    CMP(0b1011),
    JE(0b1010),
    JNE(0b1010),
    INT(0b1100),
    PARAM(0b1101),
    PUSH(0b1110),
    POP(0b1111);

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
        if (code == JMP.code) return JMP; // JE/JNE se distinguen con los bits del registro.
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
