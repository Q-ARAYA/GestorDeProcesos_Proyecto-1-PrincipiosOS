/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tec.minipc.model;

/**
 * Registros de propósito general del Mini PC.
 * Ocupan los 4 bits bajos (bits 4-7) de la primera palabra de la instrucción.
 *
 * NONE se usa para instrucciones que no referencian ningún registro (no debería
 * darse con el set actual, pero se deja como salvaguarda).
 */
public enum RegisterName {
    NONE(0b0000),
    AX(0b0001),
    BX(0b0010),
    CX(0b0011),
    DX(0b0100);

    private final int code;

    RegisterName(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * Busca el registro a partir de su código de 4 bits.
     * @param code código binario del registro (0-4)
     * @return el RegisterName correspondiente
     * @throws IllegalArgumentException si el código no corresponde a ningún registro válido.
     */
    public static RegisterName fromCode(int code) {
        for (RegisterName r : values()) {
            if (r.code == code) {
                return r;
            }
        }
        throw new IllegalArgumentException("Código de registro inválido: " + code);
    }

    /**
     * Busca el registro a partir del mnemónico de texto (ej: "AX", "bx").
     * @param mnemonic el texto del registro tal como viene en el archivo .asm
     * @return el RegisterName correspondiente
     * @throws IllegalArgumentException si el mnemónico no existe.
     */
    public static RegisterName fromMnemonic(String mnemonic) {
        try {
            return RegisterName.valueOf(mnemonic.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Registro no reconocido: " + mnemonic);
        }
    }
}