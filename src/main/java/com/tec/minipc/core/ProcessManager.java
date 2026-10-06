package com.tec.minipc.core;

import com.tec.minipc.model.Instruction;
import com.tec.minipc.model.Memory;
import com.tec.minipc.model.Opcode;
import com.tec.minipc.model.RegisterName;
import com.tec.minipc.model.Registers;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** Admisión, cola FCFS, espera por memoria y despacho con contexto por proceso. */
public class ProcessManager {
    public static final int MAX_PROCESOS = 5;
    private static final int MAX_SEGUNDOS_POR_EJECUCION_AUTOMATICA = 10000;

    public static final class Proceso {
        private final PCB pcb;
        private final Registers registros = new Registers();
        private final ProcessStack pila = new ProcessStack();
        private final List<Instruction> instrucciones;
        private Cpu cpu;
        private Integer entradaPendiente;

        private Proceso(PCB pcb, List<Instruction> instrucciones) {
            this.pcb = pcb;
            this.instrucciones = Collections.unmodifiableList(new ArrayList<>(instrucciones));
            registros.setPc(-1);
        }

        public PCB getPcb() { return pcb; }
        public Registers getRegistros() { return registros; }
        public ProcessStack getPila() { return pila; }
        public int getBase() { return pcb.getDireccionBase(); }
        public int getTamano() { return pcb.getTamanoInstrucciones(); }
        public List<Instruction> getInstrucciones() { return instrucciones; }
        public int getDireccionInstruccionActual() {
            return cpu != null && cpu.hayInstruccionEnCurso()
                    ? cpu.getDireccionInstruccionEnCurso() : registros.getPc();
        }
    }

    private final Memory memoria;
    private final SimulatedFileSystem sistemaArchivos;
    private final List<Proceso> procesos = new ArrayList<>();
    private final Deque<Proceso> listos = new ArrayDeque<>();
    private final Deque<Proceso> esperaMemoria = new ArrayDeque<>();
    private final Deque<Proceso> esperaEntrada = new ArrayDeque<>();
    private final Deque<Integer> entradasTeclado = new ArrayDeque<>();
    private final List<String> salidaPantalla = new ArrayList<>();
    private final List<String> eventos = new ArrayList<>();
    private Proceso actual;
    private int siguientesPid = 1;

    public ProcessManager(Memory memoria) { this(memoria, null); }

    public ProcessManager(Memory memoria, com.tec.minipc.model.SecondaryStorage almacenamiento) {
        this.memoria = memoria;
        this.sistemaArchivos = almacenamiento == null ? null : new SimulatedFileSystem(almacenamiento);
    }

    /** Registra el trabajo y su BCP; si no hay hueco, queda en ESPERA hasta liberar memoria. */
    public Proceso admitir(String nombre, List<Instruction> instrucciones) {
        if (procesos.size() >= MAX_PROCESOS) {
            throw new IllegalStateException("El simulador admite como máximo " + MAX_PROCESOS + " procesos.");
        }
        int capacidadUsuario = memoria.getUserEnd() - memoria.getUserStart() + 1;
        if (instrucciones.isEmpty() || instrucciones.size() > capacidadUsuario) {
            throw new IllegalStateException("El programa " + nombre + " excede la capacidad total de memoria de usuario ("
                    + capacidadUsuario + " celdas).");
        }

        PCB pcb = new PCB(siguientesPid++, nombre, -1, -1, instrucciones.size());
        Proceso proceso = new Proceso(pcb, instrucciones);
        memoria.guardarBcp(pcb);
        if (!procesos.isEmpty()) {
            PCB anterior = procesos.get(procesos.size() - 1).getPcb();
            anterior.setDireccionSiguienteBcp(pcb.getDireccionBcp());
            memoria.actualizarBcp(anterior);
        }
        procesos.add(proceso);

        if (cargarEnMemoria(proceso)) {
            listos.addLast(proceso);
        } else {
            pcb.setEstado(PCB.Estado.ESPERA);
            pcb.setMotivoEspera("Memoria principal");
            memoria.actualizarBcp(pcb);
            esperaMemoria.addLast(proceso);
        }
        return proceso;
    }

    private int buscarHueco(int requerido) {
        int inicio = memoria.getUserStart();
        int ultimoInicio = memoria.getUserEnd() - requerido + 1;
        for (int base = inicio; base <= ultimoInicio; base++) {
            boolean libre = true;
            for (int i = 0; i < requerido; i++) {
                if (!memoria.isEmpty(base + i)) { libre = false; break; }
            }
            if (libre) return base;
        }
        return -1;
    }

    private boolean cargarEnMemoria(Proceso proceso) {
        int base = buscarHueco(proceso.getTamano());
        if (base < 0) return false;
        memoria.loadProgramAt(proceso.instrucciones, base);
        proceso.pcb.asignarRegionMemoria(base);
        proceso.registros.reset();
        proceso.registros.setPc(base);
        proceso.cpu = new Cpu(memoria, proceso.registros, proceso.pcb, proceso.pila, sistemaArchivos);
        memoria.actualizarBcp(proceso.pcb);
        return true;
    }

    /** Admite trabajos en espera en orden FCFS cuando un bloque contiguo queda libre. */
    private void admitirEnEspera() {
        while (!esperaMemoria.isEmpty()) {
            Proceso siguiente = esperaMemoria.peekFirst();
            if (!cargarEnMemoria(siguiente)) return; // FCFS: no adelantar un trabajo más pequeño
            esperaMemoria.removeFirst();
            listos.addLast(siguiente);
        }
    }

    private void despacharSiguiente() {
        admitirEnEspera();
        actual = listos.pollFirst();
        if (actual != null) {
            actual.getPcb().setEstado(PCB.Estado.EJECUTANDO);
            memoria.actualizarBcp(actual.getPcb());
        }
    }

    /** Ejecuta una instrucción; FCFS mantiene la CPU hasta finalizar el trabajo. */
    public Instruction step() {
        if (actual == null) despacharSiguiente();
        if (actual == null) return null;
        Instruction siguiente = memoria.read(actual.registros.getPc());
        if (!actual.cpu.hayInstruccionEnCurso() && siguiente != null
                && siguiente.getOpcode() == Opcode.INT && siguiente.getOperand() == 0x09) {
            Integer entrada = actual.entradaPendiente;
            if (entrada == null && !entradasTeclado.isEmpty()) entrada = entradasTeclado.removeFirst();
            if (entrada == null) {
                actual.getPcb().setEstado(PCB.Estado.ESPERA);
                actual.getPcb().setMotivoEspera("Teclado (INT 09H)");
                memoria.actualizarBcp(actual.getPcb());
                esperaEntrada.addLast(actual);
                actual = null;
                despacharSiguiente();
                return null;
            }
            actual.entradaPendiente = null;
            actual.cpu.setEntradaTeclado(entrada);
        }
        Instruction ejecutada = actual.cpu.step();
        if (ejecutada != null && actual.cpu.inicioInstruccionEsteTick()
                && ejecutada.getOpcode() == Opcode.INT && ejecutada.getOperand() == 0x10) {
            salidaPantalla.add(String.valueOf(actual.registros.get(RegisterName.DX)));
        }
        memoria.actualizarBcp(actual.getPcb());
        if (actual.cpu.isTerminado()) {
            if (!actual.cpu.getMensajeError().isEmpty()) {
                eventos.add(actual.pcb.getNombrePrograma() + " terminó con error: " + actual.cpu.getMensajeError());
            }
            if (sistemaArchivos != null) {
                sistemaArchivos.cerrarProceso(actual.pcb.getPid());
                actual.pcb.setArchivosAbiertos(sistemaArchivos.getArchivosAbiertos(actual.pcb.getPid()));
                memoria.actualizarBcp(actual.pcb);
            }
            memoria.release(actual.getBase(), actual.getTamano());
            actual = null;
            admitirEnEspera();
            despacharSiguiente();
        }
        return ejecutada;
    }

    /** Entrega un valor del teclado al proceso en espera más antiguo o lo deja en búfer. */
    public void proveerEntrada(int valor) {
        if (valor < 0 || valor > 255) throw new IllegalArgumentException("La entrada debe estar entre 0 y 255.");
        if (esperaEntrada.isEmpty()) {
            entradasTeclado.addLast(valor);
            return;
        }
        Proceso proceso = esperaEntrada.removeFirst();
        proceso.entradaPendiente = valor;
        proceso.getPcb().setEstado(PCB.Estado.LISTO);
        memoria.actualizarBcp(proceso.getPcb());
        listos.addLast(proceso);
    }

    public List<String> getSalidaPantalla() { return Collections.unmodifiableList(salidaPantalla); }
    public List<String> getEventos() { return Collections.unmodifiableList(eventos); }
    public SimulatedFileSystem getSistemaArchivos() { return sistemaArchivos; }
    public boolean hayEsperandoEntrada() { return !esperaEntrada.isEmpty(); }
    public int getCantidadEsperandoEntrada() { return esperaEntrada.size(); }
    public boolean puedeAvanzar() { return actual != null || !listos.isEmpty() || !esperaMemoria.isEmpty(); }

    public int runAll() {
        int pasos = 0;
        while (actual != null || !listos.isEmpty() || !esperaMemoria.isEmpty()) {
            if (pasos >= MAX_SEGUNDOS_POR_EJECUCION_AUTOMATICA) {
                throw new IllegalStateException("La ejecución automática alcanzó 10000 segundos simulados; revise si hay un ciclo sin salida.");
            }
            if (step() != null) pasos++;
            else if (actual == null && listos.isEmpty() && !esperaMemoria.isEmpty()) {
                // Evita un ciclo infinito ante una región libre insuficiente o fragmentada.
                throw new IllegalStateException("Hay procesos en espera que no pueden cargarse en memoria.");
            }
        }
        return pasos;
    }

    public List<Proceso> getProcesos() { return Collections.unmodifiableList(procesos); }
    public Proceso getActual() { return actual; }
    public boolean hayPendientes() { return actual != null || !listos.isEmpty() || !esperaMemoria.isEmpty() || !esperaEntrada.isEmpty(); }
    public boolean estaVacio() { return procesos.isEmpty(); }
}
