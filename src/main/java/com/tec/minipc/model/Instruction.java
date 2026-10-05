package com.tec.minipc.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Instrucción del mini ensamblador, con codificación de dos bytes. */
public class Instruction {
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^\\s*([A-Za-z]+)(?:\\s+(.+?))?\\s*$");

    private final Opcode opcode;
    private final RegisterName register;
    private final RegisterName registerOperand;
    private final int operand;
    private final List<Integer> parametros;
    private final String sourceLine;

    /** Constructor conservado para instrucciones con un único registro/operando. */
    public Instruction(Opcode opcode, RegisterName register, int operand, String sourceLine) {
        this(opcode, register, null, operand, sourceLine, Collections.emptyList());
    }

    private Instruction(Opcode opcode, RegisterName register, RegisterName registerOperand,
            int operand, String sourceLine) {
        this(opcode, register, registerOperand, operand, sourceLine, Collections.emptyList());
    }

    private Instruction(Opcode opcode, RegisterName register, RegisterName registerOperand,
            int operand, String sourceLine, List<Integer> parametros) {
        this.opcode = opcode;
        this.register = register;
        this.registerOperand = registerOperand;
        this.operand = operand;
        this.parametros = Collections.unmodifiableList(new ArrayList<>(parametros));
        this.sourceLine = sourceLine;
        validar();
    }

    private void validar() {
        if ((opcode == Opcode.MOV || esSalto(opcode)) && (operand < -127 || operand > 127)) {
            throw new IllegalArgumentException("El valor inmediato " + operand
                    + " no cabe en 8 bits signo-magnitud (-127..127): " + sourceLine);
        }
        if ((opcode == Opcode.MOVR || opcode == Opcode.SWAP || opcode == Opcode.CMP) && registerOperand == null) {
            throw new IllegalArgumentException(opcode + " requiere un segundo registro.");
        }
        if (opcode == Opcode.PARAM && (parametros.isEmpty() || parametros.size() > 3)) {
            throw new IllegalArgumentException("PARAM acepta entre uno y tres valores.");
        }
    }

    /** Parsea una línea y valida operandos y aridad según el mnemónico. */
    public static Instruction parse(String rawLine, int lineNumber) {
        String line = rawLine == null ? "" : rawLine.trim();
        Matcher m = LINE_PATTERN.matcher(line);
        if (!m.matches()) throw error(lineNumber, "formato inválido -> \"" + rawLine + "\"");

        String mnemonic = m.group(1).toUpperCase();
        String operandText = m.group(2);
        String[] args = operandText == null ? new String[0] : operandText.split(",", -1);
        for (int i = 0; i < args.length; i++) args[i] = args[i].trim();
        for (String arg : args) if (arg.isEmpty()) throw error(lineNumber, "operando vacío");
        String first = args.length > 0 ? args[0] : null;
        String second = args.length > 1 ? args[1] : null;
        if (args.length > 2 && !mnemonic.equals("PARAM")) {
            throw error(lineNumber, mnemonic + " no admite más de dos operandos");
        }
        RegisterName reg;
        RegisterName reg2 = null;
        int value = 0;
        Opcode opcode;

        switch (mnemonic) {
            case "MOV":
                if (first == null || second == null) {
                    throw error(lineNumber, "MOV requiere destino y fuente, ej. MOV AX, 5 o MOV AX, BX");
                }
                reg = parseRegister(first, lineNumber);
                if (second.matches("[+-]?\\d+")) {
                    opcode = Opcode.MOV;
                    try { value = Integer.parseInt(second); }
                    catch (NumberFormatException ex) { throw error(lineNumber, "valor inmediato fuera de rango: " + second); }
                } else {
                    opcode = Opcode.MOVR;
                    reg2 = parseRegister(second, lineNumber);
                }
                break;
            case "LOAD": case "STORE": case "ADD": case "SUB":
                requireOneRegister(mnemonic, first, second, lineNumber);
                opcode = Opcode.fromMnemonic(mnemonic);
                reg = parseRegister(first, lineNumber);
                break;
            case "INC": case "DEC":
                if (second != null) throw error(lineNumber, mnemonic + " acepta cero o un registro, ej. " + mnemonic + " AX");
                opcode = Opcode.valueOf(mnemonic);
                reg = first == null ? RegisterName.NONE : parseRegister(first, lineNumber);
                break;
            case "SWAP":
                if (first == null || second == null) throw error(lineNumber, "SWAP requiere dos registros, ej. SWAP AX, BX");
                opcode = Opcode.SWAP;
                reg = parseRegister(first, lineNumber);
                reg2 = parseRegister(second, lineNumber);
                break;
            case "CMP":
                if (first == null || second == null) throw error(lineNumber, "CMP requiere dos registros, ej. CMP AX, BX");
                opcode = Opcode.CMP;
                reg = parseRegister(first, lineNumber);
                reg2 = parseRegister(second, lineNumber);
                break;
            case "JMP": case "JE": case "JNE":
                if (first == null || second != null || !first.matches("[+-]?\\d+")) {
                    throw error(lineNumber, mnemonic + " requiere un desplazamiento entero, ej. " + mnemonic + " +2");
                }
                opcode = Opcode.valueOf(mnemonic);
                reg = mnemonic.equals("JMP") ? RegisterName.NONE
                        : (mnemonic.equals("JE") ? RegisterName.AX : RegisterName.BX);
                try { value = Integer.parseInt(first); }
                catch (NumberFormatException ex) { throw error(lineNumber, "desplazamiento fuera de rango: " + first); }
                break;
            case "PUSH": case "POP":
                requireOneRegister(mnemonic, first, second, lineNumber);
                opcode = Opcode.valueOf(mnemonic);
                reg = parseRegister(first, lineNumber);
                break;
            case "PARAM":
                if (args.length < 1 || args.length > 3) throw error(lineNumber, "PARAM requiere entre uno y tres valores numéricos");
                opcode = Opcode.PARAM;
                reg = RegisterName.NONE;
                List<Integer> parametros = new ArrayList<>();
                for (String arg : args) {
                    try {
                        int parametro = Integer.parseInt(arg);
                        if (parametro < 0 || parametro > 255) throw new NumberFormatException();
                        parametros.add(parametro);
                    } catch (NumberFormatException ex) {
                        throw error(lineNumber, "PARAM solo admite valores enteros de 0 a 255: " + arg);
                    }
                }
                return new Instruction(opcode, reg, null, 0, line, parametros);
            case "INT":
                if (first == null || second != null || !first.matches("(?i)[0-9a-f]{1,2}H")) {
                    throw error(lineNumber, "INT requiere un código hexadecimal, ej. INT 10H");
                }
                try { value = Integer.parseInt(first.substring(0, first.length() - 1), 16); }
                catch (NumberFormatException ex) { throw error(lineNumber, "código de interrupción inválido: " + first); }
                if (value != 0x09 && value != 0x10 && value != 0x20) {
                    throw error(lineNumber, "interrupción no implementada: INT " + first.toUpperCase());
                }
                opcode = Opcode.INT;
                reg = RegisterName.NONE;
                break;
            default:
                throw error(lineNumber, "instrucción no reconocida: " + mnemonic);
        }
        return new Instruction(opcode, reg, reg2, value, line);
    }

    private static void requireOneRegister(String mnemonic, String first, String second, int lineNumber) {
        if (first == null || second != null) {
            throw error(lineNumber, mnemonic + " requiere exactamente un registro, ej. " + mnemonic + " AX");
        }
    }

    private static RegisterName parseRegister(String text, int lineNumber) {
        try {
            RegisterName register = RegisterName.fromMnemonic(text);
            if (register == RegisterName.NONE) throw new IllegalArgumentException();
            return register;
        } catch (IllegalArgumentException ex) {
            throw error(lineNumber, "registro no reconocido: " + text);
        }
    }

    private static IllegalArgumentException error(int lineNumber, String message) {
        return new IllegalArgumentException("Línea " + lineNumber + ": " + message);
    }

    /** Byte 0 contiene opcode/registro; byte 1 contiene inmediato o segundo registro. */
    public int[] encode() {
        if (opcode == Opcode.PARAM) {
            int[] bytes = new int[parametros.size() + 1];
            bytes[0] = (opcode.getCode() << 4) | parametros.size();
            for (int i = 0; i < parametros.size(); i++) bytes[i + 1] = parametros.get(i);
            return bytes;
        }
        int selector = register.getCode();
        int byte0 = (opcode.getCode() << 4) | selector;
        int byte1 = 0;
        if (opcode == Opcode.MOV) byte1 = encodeSignMagnitude(operand);
        else if (opcode == Opcode.MOVR || opcode == Opcode.SWAP || opcode == Opcode.CMP) byte1 = registerOperand.getCode();
        else if (esSalto(opcode)) byte1 = encodeSignMagnitude(operand);
        else if (opcode == Opcode.INT) byte1 = operand;
        return new int[]{byte0 & 0xFF, byte1 & 0xFF};
    }

    public static Instruction decode(int byte0, int byte1) {
        return decode(new int[]{byte0, byte1});
    }

    /** Decodifica también PARAM, cuyos valores siguen al byte de control. */
    public static Instruction decode(int[] bytes) {
        if (bytes == null || bytes.length < 2) throw new IllegalArgumentException("Código de instrucción incompleto.");
        int byte0 = bytes[0] & 0xFF;
        int byte1 = bytes[1] & 0xFF;
        int opcodeBits = (byte0 >> 4) & 0x0F;
        int selector = byte0 & 0x0F;
        Opcode opcode;
        if (opcodeBits == Opcode.JMP.getCode()) {
            if (selector == 0) opcode = Opcode.JMP;
            else if (selector == 1) opcode = Opcode.JE;
            else if (selector == 2) opcode = Opcode.JNE;
            else throw new IllegalArgumentException("Código de condición de salto inválido: " + selector);
        } else opcode = Opcode.fromCode(opcodeBits);
        RegisterName register = opcode == Opcode.PARAM ? RegisterName.NONE : RegisterName.fromCode(selector);
        RegisterName registerOperand = null;
        int operand = 0;
        List<Integer> parametros = new ArrayList<>();
        if (opcode == Opcode.PARAM) {
            if (selector < 1 || selector > 3 || bytes.length < selector + 1) {
                throw new IllegalArgumentException("Código PARAM incompleto o con cantidad de parámetros inválida.");
            }
            for (int i = 0; i < selector; i++) parametros.add(bytes[i + 1] & 0xFF);
        }
        if (opcode == Opcode.MOV || esSalto(opcode)) operand = decodeSignMagnitude(byte1);
        else if (opcode == Opcode.MOVR || opcode == Opcode.SWAP || opcode == Opcode.CMP) registerOperand = RegisterName.fromCode(byte1 & 0x0F);
        else if (opcode == Opcode.INT) operand = byte1 & 0xFF;
        String text = reconstruirTexto(opcode, register, registerOperand, operand, parametros);
        return new Instruction(opcode, register, registerOperand, operand, text, parametros);
    }

    private static String reconstruirTexto(Opcode opcode, RegisterName register,
            RegisterName registerOperand, int operand, List<Integer> parametros) {
        switch (opcode) {
            case MOV: return "MOV " + register + ", " + operand;
            case MOVR: return "MOV " + register + ", " + registerOperand;
            case SWAP: return "SWAP " + register + ", " + registerOperand;
            case CMP: return "CMP " + register + ", " + registerOperand;
            case JMP: case JE: case JNE: return opcode + " " + (operand >= 0 ? "+" : "") + operand;
            case INT: return String.format("INT %02XH", operand);
            case PARAM:
                StringBuilder params = new StringBuilder("PARAM ");
                for (int i = 0; i < parametros.size(); i++) {
                    if (i > 0) params.append(", ");
                    params.append(parametros.get(i));
                }
                return params.toString();
            case INC: case DEC: return register == RegisterName.NONE ? opcode.name() : opcode + " " + register;
            default: return opcode + " " + register;
        }
    }

    private static boolean esSalto(Opcode opcode) {
        return opcode == Opcode.JMP || opcode == Opcode.JE || opcode == Opcode.JNE;
    }

    public static int encodeSignMagnitude(int value) {
        int sign = value < 0 ? 1 : 0;
        return (sign << 7) | (Math.abs(value) & 0x7F);
    }

    public static int decodeSignMagnitude(int byteValue) {
        int magnitude = byteValue & 0x7F;
        return ((byteValue >> 7) & 1) == 1 ? -magnitude : magnitude;
    }

    public static String toBinaryByte(int value) {
        StringBuilder sb = new StringBuilder(Integer.toBinaryString(value & 0xFF));
        while (sb.length() < 8) sb.insert(0, '0');
        return sb.toString();
    }

    public Opcode getOpcode() { return opcode; }
    public RegisterName getRegister() { return register; }
    public RegisterName getRegisterOperand() { return registerOperand; }
    public int getOperand() { return operand; }
    public List<Integer> getParametros() { return parametros; }
    public String getSourceLine() { return sourceLine; }

    /** Costo de la instrucción en segundos simulados según la tabla del proyecto. */
    public int getPeso() {
        switch (opcode) {
            case LOAD: case STORE: return 2;
            case ADD: case SUB: return 3;
            case MOV: case MOVR: case INC: case DEC: case SWAP: case PUSH: case POP: return 1;
            case CMP: case JMP: case JE: case JNE: return 2;
            case PARAM: return 3;
            case INT:
                if (operand == 0x20 || operand == 0x10 || operand == 0x09) return 2;
                return 1;
            default: return 1;
        }
    }

    @Override public String toString() {
        int[] bytes = encode();
        StringBuilder binario = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) binario.append(' ');
            binario.append(toBinaryByte(bytes[i]));
        }
        return sourceLine + "  [" + binario + "]";
    }
}
