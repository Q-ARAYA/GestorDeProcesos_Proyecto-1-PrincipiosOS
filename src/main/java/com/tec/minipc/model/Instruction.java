/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tec.minipc.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representa una instrucción del Mini PC ya parseada.
 *
 * Formato de instrucción (2 bytes / 2 palabras de memoria):
 *   Byte 0: [bits 0-3 Opcode][bits 4-7 Registro]
 *   Byte 1: Valor entero con signo (bit 0 = signo, bits 1-7 = magnitud).
 *           Solo se usa en MOV (valor inmediato); en el resto vale 0.
 *
 * Semántica de ejecución (máquina de acumulador AC):
 *   MOV   reg, N  -> reg = N
 *   LOAD  reg      -> AC = reg
 *   STORE reg      -> reg = AC
 *   ADD   reg      -> AC = AC + reg
 *   SUB   reg      -> AC = AC - reg
 */
public class Instruction {

    // Ej: "MOV AX, 5"   "LOAD AX"   "ADD BX"   "MOV BX, -8"
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^\\s*([A-Za-z]+)\\s+([A-Za-z]+)\\s*(?:,\\s*(-?\\d+))?\\s*$"
    );

    private final Opcode opcode;
    private final RegisterName register;
    private final int operand;       // valor inmediato (solo válido para MOV)
    private final String sourceLine; // línea original del .asm, para mostrar en la UI

    public Instruction(Opcode opcode, RegisterName register, int operand, String sourceLine) {
        this.opcode = opcode;
        this.register = register;
        this.operand = operand;
        this.sourceLine = sourceLine;
        validarOperando();
    }

    private void validarOperando() {
        if (opcode == Opcode.MOV) {
            if (operand < -127 || operand > 127) {
                throw new IllegalArgumentException(
                        "El valor inmediato " + operand + " no cabe en 8 bits signo-magnitud (-127..127): "
                                + sourceLine);
            }
        }
    }

    /**
     * Parsea una línea de texto del archivo .asm y construye la Instruction.
     * @param rawLine la línea tal como viene en el archivo
     * @param lineNumber número de línea (para mensajes de error claros)
     * @return la Instruction resultante
     * @throws IllegalArgumentException con un mensaje claro si el formato no es válido.
     */
    public static Instruction parse(String rawLine, int lineNumber) {
        String line = rawLine == null ? "" : rawLine.trim();

        // ignorar líneas vacías o comentarios (';' o '#') antes de llegar aquí:
        // esa responsabilidad es del Assembler; aquí asumimos que ya es una línea de código.
        Matcher m = LINE_PATTERN.matcher(line);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Línea " + lineNumber + ": formato inválido -> \"" + rawLine + "\"");
        }

        String mnemonic = m.group(1);
        String regText = m.group(2);
        String immText = m.group(3);

        Opcode opcode;
        RegisterName register;
        try {
            opcode = Opcode.fromMnemonic(mnemonic);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Línea " + lineNumber + ": " + e.getMessage());
        }
        try {
            register = RegisterName.fromMnemonic(regText);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Línea " + lineNumber + ": " + e.getMessage());
        }

        int operand = 0;
        if (opcode == Opcode.MOV) {
            if (immText == null) {
                throw new IllegalArgumentException(
                        "Línea " + lineNumber + ": MOV requiere un valor inmediato, ej. \"MOV AX, 5\"");
            }
            operand = Integer.parseInt(immText);
        } else if (immText != null) {
            throw new IllegalArgumentException(
                    "Línea " + lineNumber + ": " + mnemonic + " no admite valor inmediato -> \"" + rawLine + "\"");
        }

        return new Instruction(opcode, register, operand, rawLine.trim());
    }

    /**
     * Codifica la instrucción en sus 2 bytes de memoria.
     * @return arreglo de 2 enteros (0-255), cada uno representa un byte
     */
    public int[] encode() {
        int byte0 = (opcode.getCode() << 4) | register.getCode();
        int byte1 = (opcode == Opcode.MOV) ? encodeSignMagnitude(operand) : 0;
        return new int[]{byte0 & 0xFF, byte1 & 0xFF};
    }

    /**
     * Decodifica una instrucción a partir de sus 2 bytes crudos de memoria.
     * @param byte0 primer byte (opcode + registro)
     * @param byte1 segundo byte (operando con signo, si aplica)
     * @return la Instruction reconstruida
     */
    public static Instruction decode(int byte0, int byte1) {
        int opcodeBits = (byte0 >> 4) & 0x0F;
        int regBits = byte0 & 0x0F;
        Opcode opcode = Opcode.fromCode(opcodeBits);
        RegisterName register = RegisterName.fromCode(regBits);
        int operand = (opcode == Opcode.MOV) ? decodeSignMagnitude(byte1) : 0;
        String text = reconstruirTexto(opcode, register, operand);
        return new Instruction(opcode, register, operand, text);
    }

    private static String reconstruirTexto(Opcode opcode, RegisterName register, int operand) {
        if (opcode == Opcode.MOV) {
            return opcode.name() + " " + register.name() + ", " + operand;
        }
        return opcode.name() + " " + register.name();
    }

    /**
     * Codifica un entero en formato signo-magnitud de 8 bits.
     * Bit más significativo = signo (1=negativo), 7 bits restantes = magnitud (0-127).
     * @param value valor a codificar (-127..127)
     * @return el byte codificado (0-255)
     */
    public static int encodeSignMagnitude(int value) {
        int sign = (value < 0) ? 1 : 0;
        int magnitude = Math.abs(value) & 0x7F;
        return (sign << 7) | magnitude;
    }

    /**
     * Decodifica un byte en formato signo-magnitud a su valor entero real.
     * @param byteValue el byte (0-255)
     * @return el valor entero con signo correspondiente
     */
    public static int decodeSignMagnitude(int byteValue) {
        int sign = (byteValue >> 7) & 0x01;
        int magnitude = byteValue & 0x7F;
        return (sign == 1) ? -magnitude : magnitude;
    }

    /**
     * Representa un byte como cadena binaria de 8 dígitos, con ceros a la izquierda.
     * @param value el byte a convertir
     * @return cadena de 8 caracteres ('0'/'1')
     */
    public static String toBinaryByte(int value) {
        StringBuilder sb = new StringBuilder(Integer.toBinaryString(value & 0xFF));
        while (sb.length() < 8) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public RegisterName getRegister() {
        return register;
    }

    public int getOperand() {
        return operand;
    }

    public String getSourceLine() {
        return sourceLine;
    }

    @Override
    public String toString() {
        int[] bytes = encode();
        return sourceLine + "  [" + toBinaryByte(bytes[0]) + " " + toBinaryByte(bytes[1]) + "]";
    }
}