package com.tec.minipc.core;

import com.tec.minipc.model.Instruction;
import com.tec.minipc.model.Memory;
import com.tec.minipc.model.Opcode;
import com.tec.minipc.model.RegisterName;
import com.tec.minipc.model.Registers;
import com.tec.minipc.model.SecondaryStorage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** Admisión, cola FCFS, espera por memoria y despacho con contexto por proceso. */
public class ProcessManager {
    public static final int MAX_PROCESOS = 5;
    private static final int MAX_SEGUNDOS_POR_EJECUCION_AUTOMATICA = 10000;

    /** Agrupa el BCP, los registros, la pila y el contexto de CPU pertenecientes a un trabajo. */
    public static final class Proceso {
        private final PCB pcb;
        private final Registers registros = new Registers();
        private final ProcessStack pila = new ProcessStack();
        private final List<Instruction> instrucciones;
        private Cpu cpu;
        private Integer entradaPendiente;

        /**
         * Inicializa Proceso con los recursos y valores recibidos.
         * @param pcb bloque de control del proceso
         * @param instrucciones instrucciones que se conservarán
         */
        private Proceso(PCB pcb, List<Instruction> instrucciones) {
            this.pcb = pcb;
            this.instrucciones = Collections.unmodifiableList(new ArrayList<>(instrucciones));
            registros.setPc(-1);
        }

        /**
         * Devuelve el BCP que conserva el estado de este proceso.
         * @return valor, objeto o colección descrita en el resumen del método.
         */
        public PCB getPcb() { return pcb; }
        /**
         * Devuelve los registros asociados exclusivamente a este proceso.
         * @return valor, objeto o colección descrita en el resumen del método.
         */
        public Registers getRegistros() { return registros; }
        /**
         * Devuelve la pila privada del proceso.
         * @return valor, objeto o colección descrita en el resumen del método.
         */
        public ProcessStack getPila() { return pila; }
        /**
         * Devuelve la dirección base asignada al código del proceso.
         * @return valor calculado o estado consultado.
         */
        public int getBase() { return pcb.getDireccionBase(); }
        /**
         * Devuelve el número de instrucciones del programa.
         * @return valor calculado o estado consultado.
         */
        public int getTamano() { return pcb.getTamanoInstrucciones(); }
        /**
         * Devuelve la lista inmutable de instrucciones del proceso.
         * @return vista de los elementos correspondientes.
         */
        public List<Instruction> getInstrucciones() { return instrucciones; }
        /**
         * Devuelve la dirección que debe marcarse en las vistas de programa y memoria.
         * @return valor calculado o estado consultado.
         */
        public int getDireccionInstruccionActual() {
            return cpu != null && cpu.hayInstruccionEnCurso()
                    ? cpu.getDireccionInstruccionEnCurso() : registros.getPc();
        }
    }

    private final Memory memoria;
    private final SecondaryStorage almacenamientoSecundario;
    private final SimulatedFileSystem sistemaArchivos;
    private final List<Proceso> procesos = new ArrayList<>();
    private final Deque<Proceso> listos = new ArrayDeque<>();
    private final Deque<Proceso> esperaMemoria = new ArrayDeque<>();
    private final Deque<Proceso> esperaEntrada = new ArrayDeque<>();
    private final Deque<Proceso> suspendidos = new ArrayDeque<>();
    private final Deque<Integer> entradasTeclado = new ArrayDeque<>();
    private final List<String> salidaPantalla = new ArrayList<>();
    private final List<String> eventos = new ArrayList<>();
    private Proceso actual;
    private int siguientesPid = 1;

    /**
     * Inicializa ProcessManager con los recursos y valores recibidos.
     * @param memoria memoria principal asociada
     */
    public ProcessManager(Memory memoria) { this(memoria, null); }

    /**
     * Inicializa ProcessManager con los recursos y valores recibidos.
     * @param memoria memoria principal asociada
     * @param almacenamiento valor inicial usado por la instancia
     */
    public ProcessManager(Memory memoria, SecondaryStorage almacenamiento) {
        this.memoria = memoria;
        this.almacenamientoSecundario = almacenamiento;
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
        if (almacenamientoSecundario != null && !almacenamientoSecundario.puedeGuardar(instrucciones)) {
            throw new IllegalStateException("No hay espacio suficiente en el disco o en la memoria virtual para " + nombre + ".");
        }

        PCB pcb = new PCB(siguientesPid++, nombre, -1, -1, instrucciones.size());
        if (almacenamientoSecundario != null) almacenamientoSecundario.guardar(pcb.getPid(), nombre, instrucciones);
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

    /**
     * Busca hueco.
     * @param requerido cantidad de celdas solicitadas
     * @return valor numérico producido por la operación.
     */
    private int buscarHueco(int requerido) {
        int inicio = memoria.getUserStart();
        int ultimoInicio = memoria.getUserEnd() - requerido + 1;
        // Primera adecuación: se revisa desde el inicio del área de usuario y se toma el primer bloque contiguo que cabe.
        for (int base = inicio; base <= ultimoInicio; base++) {
            boolean libre = true;
            for (int i = 0; i < requerido; i++) {
                if (!memoria.isEmpty(base + i)) { libre = false; break; }
            }
            if (libre) return base;
        }
        return -1;
    }

    /**
     * Carga la imagen del proceso en un bloque libre y prepara sus registros y su CPU.
     * @param proceso proceso afectado por la operación
     * @return resultado generado por la operación.
     */
    private boolean cargarEnMemoria(Proceso proceso) {
        int base = buscarHueco(proceso.getTamano());
        if (base < 0) return false;
        List<Instruction> imagen = almacenamientoSecundario == null ? proceso.instrucciones
                : almacenamientoSecundario.leerPrograma(proceso.pcb.getPid());
        memoria.loadProgramAt(imagen, base);
        proceso.pcb.asignarRegionMemoria(base);
        proceso.registros.reset();
        proceso.registros.setPc(base);
        proceso.cpu = new Cpu(memoria, proceso.registros, proceso.pcb, proceso.pila, sistemaArchivos);
        if (almacenamientoSecundario != null) almacenamientoSecundario.registrarEntradaEnMemoria(proceso.pcb.getPid(), base);
        memoria.actualizarBcp(proceso.pcb);
        return true;
    }

    /** Admite trabajos en espera en orden FCFS cuando un bloque contiguo queda libre. */
    private void admitirEnEspera() {
        // Se respeta FCFS también en esta cola: si el primero no cabe, no se adelanta a procesos posteriores.
        while (!esperaMemoria.isEmpty()) {
            Proceso siguiente = esperaMemoria.peekFirst();
            if (!cargarEnMemoria(siguiente)) return; // FCFS: no adelantar un trabajo más pequeño
            esperaMemoria.removeFirst();
            listos.addLast(siguiente);
        }
    }

    /** Selecciona de la cabeza de la cola de listos el proceso que usará la CPU. */
    private void despacharSiguiente() {
        admitirEnEspera();
        actual = listos.pollFirst();
        if (actual != null) {
            actual.getPcb().setEstado(PCB.Estado.EJECUTANDO);
            memoria.actualizarBcp(actual.getPcb());
        }
    }

    /** Suspende el proceso que ocupa la CPU y despacha el siguiente listo. */
    public Proceso suspenderActual() {
        if (actual == null) return null;
        Proceso proceso = actual;
        proceso.getPcb().setEstado(PCB.Estado.SUSPENDIDO);
        memoria.actualizarBcp(proceso.getPcb());
        suspendidos.addLast(proceso);
        actual = null;
        despacharSiguiente();
        return proceso;
    }

    /** Reincorpora el proceso suspendido más antiguo al final de la cola FCFS. */
    public Proceso reanudarSiguiente() {
        Proceso proceso = suspendidos.pollFirst();
        if (proceso == null) return null;
        proceso.getPcb().setEstado(PCB.Estado.LISTO);
        memoria.actualizarBcp(proceso.getPcb());
        listos.addLast(proceso);
        return proceso;
    }

    /** Ejecuta una instrucción; FCFS mantiene la CPU hasta finalizar el trabajo. */
    public Instruction step() {
        // El despacho ocurre cuando no existe un proceso actual; FCFS no expulsa al proceso que ya tiene la CPU.
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
            if (almacenamientoSecundario != null) almacenamientoSecundario.registrarLiberacionDeMemoria(actual.pcb.getPid());
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

    /**
     * Devuelve la salida acumulada por las interrupciones de pantalla.
     * @return vista de los elementos correspondientes.
     */
    public List<String> getSalidaPantalla() { return Collections.unmodifiableList(salidaPantalla); }
    /**
     * Devuelve los eventos generados durante la simulación.
     * @return vista de los elementos correspondientes.
     */
    public List<String> getEventos() { return Collections.unmodifiableList(eventos); }
    /**
     * Devuelve el servicio de archivos simulado, si está configurado.
     * @return valor, objeto o colección descrita en el resumen del método.
     */
    public SimulatedFileSystem getSistemaArchivos() { return sistemaArchivos; }
    /**
     * Indica si existe un proceso bloqueado esperando un valor de teclado.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean hayEsperandoEntrada() { return !esperaEntrada.isEmpty(); }
    /**
     * Devuelve cuántos procesos están en la cola de espera por teclado.
     * @return valor numérico producido por la operación.
     */
    public int getCantidadEsperandoEntrada() { return esperaEntrada.size(); }
    /**
     * Indica si se puede avanzar.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean puedeAvanzar() { return actual != null || !listos.isEmpty() || !esperaMemoria.isEmpty(); }
    /**
     * Indica si hay suspendidos.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean haySuspendidos() { return !suspendidos.isEmpty(); }
    /**
     * Devuelve cuántos procesos están suspendidos.
     * @return valor numérico producido por la operación.
     */
    public int getCantidadSuspendidos() { return suspendidos.size(); }

    /**
     * Ejecuta los procesos pendientes respetando FCFS y los límites de seguridad.
     * @return valor calculado o estado consultado.
     */
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

    /**
     * Devuelve la lista no modificable de procesos admitidos.
     * @return vista de los elementos correspondientes.
     */
    public List<Proceso> getProcesos() { return Collections.unmodifiableList(procesos); }
    /**
     * Devuelve el proceso que actualmente ocupa la CPU.
     * @return valor, objeto o colección descrita en el resumen del método.
     */
    public Proceso getActual() { return actual; }
    /**
     * Indica si hay pendientes.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean hayPendientes() { return actual != null || !listos.isEmpty() || !esperaMemoria.isEmpty()
            || !esperaEntrada.isEmpty() || !suspendidos.isEmpty(); }
    /**
     * Indica si está vacio.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean estaVacio() { return procesos.isEmpty(); }
}
