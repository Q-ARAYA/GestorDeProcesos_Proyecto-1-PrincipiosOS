package com.tec.minipc.core;

import com.tec.minipc.model.Instruction;
import com.tec.minipc.model.Memory;
import com.tec.minipc.model.RegisterName;
import com.tec.minipc.model.Registers;

/**
 * Motor de ejecución del Mini PC: implementa el ciclo fetch-decode-execute.
 *
 * Se puede avanzar de dos formas:
 *   - step()   -> un solo ciclo (botón "Siguiente" para ejecución paso a paso)
 *   - runAll() -> ejecuta todo el programa de corrido (botón de ejecución automática)
 *
 * El límite de ejecución se calcula a partir de los datos del PCB (dirección
 * base + tamaño del programa en instrucciones), no solo de si la celda está
 * vacía, para que el CPU sepa con certeza dónde termina el proceso actual.
 */
public class Cpu {

    private final Memory memoria;
    private final Registers registros;
    private final PCB pcb;
    private boolean terminado;

    public Cpu(Memory memoria, Registers registros, PCB pcb) {
        this.memoria = memoria;
        this.registros = registros;
        this.pcb = pcb;
        this.terminado = false;
        pcb.setEstado(PCB.Estado.LISTO);
    }

    public boolean isTerminado() {
        return terminado;
    }

    public Registers getRegistros() {
        return registros;
    }

    public PCB getPcb() {
        return pcb;
    }

    public Memory getMemoria() {
        return memoria;
    }

    /**
     * Ejecuta un solo ciclo fetch-decode-execute.
     * @return la instrucción ejecutada en este paso, o null si el programa ya había terminado.
     */
    public Instruction step() {
        if (terminado) {
            return null;
        }
        pcb.setEstado(PCB.Estado.EJECUTANDO);

        int direccionActual = registros.getPc();
        int limite = pcb.getDireccionBase() + pcb.getTamanoInstrucciones();

        boolean sinMasInstrucciones = direccionActual >= limite
                || !memoria.isValidAddress(direccionActual)
                || memoria.isEmpty(direccionActual);

        if (sinMasInstrucciones) {
            terminado = true;
            pcb.setEstado(PCB.Estado.TERMINADO);
            return null;
        }

        // FETCH: cada posición de memoria guarda una instrucción completa
        Instruction instruccion = memoria.read(direccionActual);

        // DECODE: en este diseño la "decodificación" ya está hecha al guardar en memoria;
        // solo se refleja en el registro IR (su binario se calcula bajo demanda para la UI)
        registros.setIr(instruccion);
        registros.advancePc(1);

        // EXECUTE: se aplica el efecto de la instrucción sobre AC / el registro correspondiente
        ejecutar(instruccion);

        // El BCP siempre refleja el estado más reciente del proceso
        pcb.actualizarDesde(registros);

        if (registros.getPc() >= limite) {
            terminado = true;
            pcb.setEstado(PCB.Estado.TERMINADO);
        } else {
            pcb.setEstado(PCB.Estado.LISTO);
        }

        return instruccion;
    }

    private void ejecutar(Instruction instruccion) {
        RegisterName reg = instruccion.getRegister();
        switch (instruccion.getOpcode()) {
            case MOV:
                registros.set(reg, instruccion.getOperand());
                break;
            case LOAD:
                registros.setAc(registros.get(reg));
                break;
            case STORE:
                registros.set(reg, registros.getAc());
                break;
            case ADD:
                registros.setAc(registros.getAc() + registros.get(reg));
                break;
            case SUB:
                registros.setAc(registros.getAc() - registros.get(reg));
                break;
        }
    }

    /**
     * Ejecuta el programa completo de corrido, llamando a step() hasta terminar.
     * @return la cantidad de instrucciones que se ejecutaron.
     */
    public int runAll() {
        int contador = 0;
        while (!terminado) {
            Instruction i = step();
            if (i != null) {
                contador++;
            }
        }
        return contador;
    }
}