package com.tec.minipc.core;

import com.tec.minipc.model.Instruction;
import com.tec.minipc.model.Memory;
import com.tec.minipc.model.RegisterName;
import com.tec.minipc.model.Registers;

/**
 * Motor de ejecución del Mini PC: implementa el ciclo fetch-decode-execute.
 * Se puede avanzar de dos formas:
 * - step()   -> un solo ciclo (botón "Siguiente" para ejecución paso a paso)
 * - runAll() -> ejecuta todo el programa de corrido (botón de ejecución automática)
 * El límite de ejecución se calcula a partir de los datos del PCB (dirección
 * base + tamaño del programa en instrucciones), no solo de si la celda está
 * vacía, para que el CPU sepa con certeza dónde termina el proceso actual.
 */
public class Cpu {

    private final Memory memoria;
    private final Registers registros;
    private final PCB pcb;
    private final ProcessStack pila;
    private final SimulatedFileSystem sistemaArchivos;
    private boolean terminado;
    private Integer entradaTeclado;
    private Instruction instruccionEnCurso;
    private int direccionInstruccionEnCurso = -1;
    private int ticksRestantes;
    private boolean finalizarSolicitado;
    private boolean instruccionIniciadaEsteTick;
    private long tiempoCpuSegundos;

    /**
     * Inicializa Cpu con los recursos y valores recibidos.
     * @param memoria memoria principal asociada
     * @param registros registros que conservará la CPU
     * @param pcb bloque de control del proceso
     */
    public Cpu(Memory memoria, Registers registros, PCB pcb) {
        this(memoria, registros, pcb, new ProcessStack(), null);
    }

    /**
     * Inicializa Cpu con los recursos y valores recibidos.
     * @param memoria memoria principal asociada
     * @param registros registros que conservará la CPU
     * @param pcb bloque de control del proceso
     * @param pila pila privada del proceso
     */
    public Cpu(Memory memoria, Registers registros, PCB pcb, ProcessStack pila) {
        this(memoria, registros, pcb, pila, null);
    }

    /**
     * Inicializa Cpu con los recursos y valores recibidos.
     * @param memoria memoria principal asociada
     * @param registros registros que conservará la CPU
     * @param pcb bloque de control del proceso
     * @param pila pila privada del proceso
     * @param sistemaArchivos servicio de archivos simulado
     */
    public Cpu(Memory memoria, Registers registros, PCB pcb, ProcessStack pila, SimulatedFileSystem sistemaArchivos) {
        this.memoria = memoria;
        this.registros = registros;
        this.pcb = pcb;
        this.pila = pila;
        this.sistemaArchivos = sistemaArchivos;
        this.terminado = false;
        pcb.setEstado(PCB.Estado.LISTO);
    }

    /**
     * Indica si terminado.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean isTerminado() {
        return terminado;
    }

    /**
     * Devuelve el conjunto de registros modificado por esta CPU.
     * @return valor, objeto o colección descrita en el resumen del método.
     */
    public Registers getRegistros() {
        return registros;
    }

    /**
     * Devuelve el BCP actualizado durante la ejecución.
     * @return valor, objeto o colección descrita en el resumen del método.
     */
    public PCB getPcb() {
        return pcb;
    }

    /**
     * Devuelve la memoria principal utilizada por esta CPU.
     * @return valor, objeto o colección descrita en el resumen del método.
     */
    public Memory getMemoria() {
        return memoria;
    }

    /**
     * Establece entrada teclado con el valor recibido.
     * @param valor valor que se asignará o procesará
     */
    public void setEntradaTeclado(int valor) { this.entradaTeclado = valor; }

    /**
     * Devuelve el último error registrado en el BCP.
     * @return valor calculado o estado consultado.
     */
    public String getMensajeError() { return pcb.getMensajeError(); }
    /**
     * Indica si hay instruccion en curso.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean hayInstruccionEnCurso() { return ticksRestantes > 0; }
    /**
     * Indica si la instrucción actual empezó en este tick.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean inicioInstruccionEsteTick() { return instruccionIniciadaEsteTick; }
    /**
     * Devuelve la dirección de la instrucción que se está ejecutando.
     * @return valor calculado o estado consultado.
     */
    public int getDireccionInstruccionEnCurso() { return direccionInstruccionEnCurso; }
    /**
     * Devuelve los segundos simulados consumidos por este proceso.
     * @return cantidad de tiempo acumulado en segundos simulados.
     */
    public long getTiempoCpuSegundos() { return tiempoCpuSegundos; }

    /**
     * Ejecuta un solo ciclo fetch-decode-execute.
     * @return la instrucción ejecutada en este paso, o null si el programa ya había terminado.
     */
    public Instruction step() {
        instruccionIniciadaEsteTick = false;
        if (terminado) return null;
        int limite = pcb.getDireccionBase() + pcb.getTamanoInstrucciones();

        // Las instrucciones con peso mayor que uno consumen varios ticks; su efecto se ejecuta una sola vez al iniciarlas.
        if (ticksRestantes > 0) {
            ticksRestantes--;
            tiempoCpuSegundos++;
            pcb.setTiempoCpuSegundos(tiempoCpuSegundos);
            if (ticksRestantes == 0 && finalizarSolicitado) terminarProceso();
            else if (pcb.getEstado() != PCB.Estado.ERROR) pcb.setEstado(PCB.Estado.EJECUTANDO);
            pcb.actualizarDesde(registros);
            return instruccionEnCurso;
        }

        // Se valida el PC contra el límite del PCB y la memoria antes de leer la instrucción.
        int direccionActual = registros.getPc();
        if (direccionActual >= limite || !memoria.isValidAddress(direccionActual)
                || memoria.isEmpty(direccionActual)) {
            terminarProceso();
            return null;
        }

        instruccionEnCurso = memoria.read(direccionActual);
        direccionInstruccionEnCurso = direccionActual;
        instruccionIniciadaEsteTick = true;
        registros.setIr(instruccionEnCurso);
        registros.advancePc(1);
        finalizarSolicitado = false;
        pcb.marcarInicio();
        tiempoCpuSegundos++;
        pcb.setTiempoCpuSegundos(tiempoCpuSegundos);

        try {
            ejecutar(instruccionEnCurso);
        } catch (IllegalStateException ex) {
            pcb.fallar(ex.getMessage());
            finalizarSolicitado = true;
        }
        pcb.setPilaTexto(pila.toString());
        pcb.actualizarDesde(registros);

        ticksRestantes = instruccionEnCurso.getPeso() - 1;
        finalizarSolicitado |= registros.getPc() >= limite;
        if (ticksRestantes == 0 && finalizarSolicitado) terminarProceso();
        else if (pcb.getEstado() != PCB.Estado.ERROR) pcb.setEstado(PCB.Estado.EJECUTANDO);
        return instruccionEnCurso;
    }

    /** Realiza la operación terminar proceso en Cpu. */
    private void terminarProceso() {
        terminado = true;
        if (pcb.getEstado() != PCB.Estado.ERROR) pcb.setEstado(PCB.Estado.TERMINADO);
        pcb.marcarFin();
    }

    /**
     * Aplica a los registros, la pila o los servicios simulados el efecto definido por el código de operación.
     * @param instruccion instrucción que se procesa
     */
    private void ejecutar(Instruction instruccion) {
        RegisterName reg = instruccion.getRegister();
        // Este switch implementa el decodificador funcional: cada opcode modifica el estado simulado correspondiente.
        switch (instruccion.getOpcode()) {
            case MOV:
                if (instruccion.getStringOperand() != null) registros.setTexto(reg, instruccion.getStringOperand());
                else registros.set(reg, instruccion.getOperand());
                break;
            case MOVR:
                registros.set(reg, registros.get(instruccion.getRegisterOperand()));
                break;
            case LOAD:
                registros.setAc(registros.get(reg));
                break;
            case STORE:
                registros.set(reg, registros.getAc());
                break;
            case ADD: {
                long resultado = (long) registros.getAc() + registros.get(reg);
                registros.setAc((int) resultado);
                actualizarFlags(resultado);
                break;
            }
            case SUB: {
                long resultado = (long) registros.getAc() - registros.get(reg);
                registros.setAc((int) resultado);
                actualizarFlags(resultado);
                break;
            }
            case INC:
                if (reg == RegisterName.NONE) {
                    long resultado = (long) registros.getAc() + 1;
                    registros.setAc((int) resultado);
                    actualizarFlags(resultado);
                } else {
                    long resultado = (long) registros.get(reg) + 1;
                    registros.set(reg, (int) resultado);
                    actualizarFlags(resultado);
                }
                break;
            case DEC:
                if (reg == RegisterName.NONE) {
                    long resultado = (long) registros.getAc() - 1;
                    registros.setAc((int) resultado);
                    actualizarFlags(resultado);
                } else {
                    long resultado = (long) registros.get(reg) - 1;
                    registros.set(reg, (int) resultado);
                    actualizarFlags(resultado);
                }
                break;
            case SWAP: {
                int valor = registros.get(reg);
                registros.set(reg, registros.get(instruccion.getRegisterOperand()));
                registros.set(instruccion.getRegisterOperand(), valor);
                break;
            }
            case CMP: {
                int izquierdo = registros.get(reg);
                int derecho = registros.get(instruccion.getRegisterOperand());
                long diferencia = (long) izquierdo - derecho;
                registros.setZeroFlag(izquierdo == derecho);
                registros.setOverflowFlag(diferencia < Integer.MIN_VALUE || diferencia > Integer.MAX_VALUE);
                break;
            }
            case JMP:
                registros.setPc(registros.getPc() + instruccion.getOperand());
                break;
            case JE:
                if (registros.isZeroFlag()) registros.setPc(registros.getPc() + instruccion.getOperand());
                break;
            case JNE:
                if (!registros.isZeroFlag()) registros.setPc(registros.getPc() + instruccion.getOperand());
                break;
            case INT:
                if (instruccion.getOperand() == 0x09) {
                    if (entradaTeclado == null) throw new IllegalStateException("INT 09H requiere una entrada de teclado.");
                    registros.set(RegisterName.DX, entradaTeclado);
                    entradaTeclado = null;
                } else if (instruccion.getOperand() == 0x20) {
                    finalizarSolicitado = true;
                } else if (instruccion.getOperand() == 0x21) {
                    if (sistemaArchivos == null) throw new IllegalStateException("INT 21H requiere el disco simulado.");
                    int resultado = sistemaArchivos.invocar(pcb.getPid(), registros.get(RegisterName.AH),
                            registros.getTexto(RegisterName.DX), registros.get(RegisterName.AL));
                    if (registros.get(RegisterName.AH) == 0x4D) registros.set(RegisterName.AL, resultado);
                    pcb.setArchivosAbiertos(sistemaArchivos.getArchivosAbiertos(pcb.getPid()));
                }
                break;
            case PARAM:
                pila.pushAll(instruccion.getParametros());
                break;
            case PUSH:
                pila.push(registros.get(reg));
                break;
            case POP:
                registros.set(reg, pila.pop());
                break;
        }
    }

    /**
     * Actualiza flags.
     * @param resultado mensaje combinado con los errores de ensamblado
     */
    private void actualizarFlags(long resultado) {
        registros.setZeroFlag((int) resultado == 0);
        registros.setOverflowFlag(resultado < Integer.MIN_VALUE || resultado > Integer.MAX_VALUE);
    }

    /**
     * Ejecuta el programa completo de corrido, llamando a step() hasta terminar.
     * @return la cantidad de instrucciones que se ejecutaron.
     */
    public int runAll() {
        int contador = 0;
        while (!terminado) {
            if (contador >= 10000) throw new IllegalStateException("La ejecución alcanzó 10000 segundos simulados; revise si hay un ciclo sin salida.");
            Instruction i = step();
            if (i != null) {
                contador++;
            }
        }
        return contador;
    }
}
