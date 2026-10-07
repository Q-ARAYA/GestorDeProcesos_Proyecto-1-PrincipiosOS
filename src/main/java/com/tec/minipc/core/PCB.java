/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tec.minipc.core;

import com.tec.minipc.model.RegisterName;
import com.tec.minipc.model.Registers;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bloque de Control de Proceso (BCP / PCB).
 * Guarda la información administrativa del proceso que el simulador tiene
 * cargado en un momento dado: identificación, estado, límites de memoria
 * asignados y una copia ("foto") de los registros para poder mostrarla en
 * la interfaz sin exponer el objeto Registers real del CPU.
 */
public class PCB {
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Estados posibles de un proceso dentro del simulador. */
    public enum Estado {
        NUEVO,       // el .asm ya se cargó a memoria pero aún no arrancó
        LISTO,       // esperando el próximo paso de ejecución
        EJECUTANDO,  // se está procesando el paso actual
        ESPERA,      // espera memoria principal para poder ejecutarse
        SUSPENDIDO,
        ERROR,
        TERMINADO    // ya no quedan instrucciones por ejecutar
    }

    private final int pid;
    private final String nombrePrograma;
    private int direccionBase;   // primera celda de memoria del proceso (-1 mientras espera)
    private int direccionLimite; // última celda de memoria del proceso
    private final int tamanoInstrucciones; // cantidad de instrucciones del programa

    private Estado estado;
    private String motivoEspera = "";
    private int programCounter;

    // Foto de los registros al momento de la última actualización
    private int ac;
    private int ax;
    private int bx;
    private int cx;
    private int dx;
    private String dxTexto;
    private boolean zeroFlag;
    private boolean overflowFlag;
    private String irTexto = "-";
    private String pilaTexto = "[]";
    private String mensajeError = "";
    private long tiempoCpuSegundos;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private int direccionBcp = -1;
    private int direccionSiguienteBcp = -1;
    private List<String> archivosAbiertos = new ArrayList<>();

    /**
     * Inicializa PCB con los recursos y valores recibidos.
     * @param pid identificador del proceso
     * @param nombrePrograma nombre del programa
     * @param direccionBase dirección base inicial
     * @param direccionLimite límite inicial de la región
     * @param tamanoInstrucciones cantidad de instrucciones del programa
     */
    public PCB(int pid, String nombrePrograma, int direccionBase, int direccionLimite, int tamanoInstrucciones) {
        this.pid = pid;
        this.nombrePrograma = nombrePrograma;
        this.direccionBase = direccionBase;
        this.direccionLimite = direccionLimite;
        this.tamanoInstrucciones = tamanoInstrucciones;
        this.estado = Estado.NUEVO;
        this.programCounter = direccionBase;
    }

    /**
     * Copia el estado actual de los registros del CPU hacia el BCP.
     * Se debe llamar después de cada paso de ejecución para que el BCP
     * refleje siempre el estado más reciente del proceso.
     * @param registros el banco de registros del CPU en este momento
     */
    public void actualizarDesde(Registers registros) {
        this.programCounter = registros.getPc();
        this.ac = registros.getAc();
        this.ax = registros.get(RegisterName.AX);
        this.bx = registros.get(RegisterName.BX);
        this.cx = registros.get(RegisterName.CX);
        this.dx = registros.get(RegisterName.DX);
        this.dxTexto = registros.getTexto(RegisterName.DX);
        this.zeroFlag = registros.isZeroFlag();
        this.overflowFlag = registros.isOverflowFlag();
        this.irTexto = registros.getIr() == null ? "-" : registros.getIr().getSourceLine();
    }

    /**
     * Devuelve el identificador único del proceso.
     * @return valor numérico producido por la operación.
     */
    public int getPid() {
        return pid;
    }

    /**
     * Devuelve el nombre del programa asociado al proceso.
     * @return texto asociado con el estado o valor consultado.
     */
    public String getNombrePrograma() {
        return nombrePrograma;
    }

    /**
     * Devuelve la primera dirección del programa en memoria de usuario.
     * @return valor numérico producido por la operación.
     */
    public int getDireccionBase() {
        return direccionBase;
    }

    /**
     * Devuelve la dirección límite exclusiva del programa.
     * @return valor numérico producido por la operación.
     */
    public int getDireccionLimite() {
        return direccionLimite;
    }

    /**
     * Calcula y guarda el intervalo de direcciones asignado al proceso.
     * @param base dirección base
     */
    public void asignarRegionMemoria(int base) {
        this.direccionBase = base;
        this.direccionLimite = base + tamanoInstrucciones - 1;
        this.programCounter = base;
    }

    /**
     * Devuelve el número de instrucciones reservadas para el proceso.
     * @return valor numérico producido por la operación.
     */
    public int getTamanoInstrucciones() {
        return tamanoInstrucciones;
    }

    /**
     * Devuelve el estado actual del proceso.
     * @return valor calculado o recurso consultado.
     */
    public Estado getEstado() {
        return estado;
    }

    /**
     * Actualiza el estado actual del proceso.
     * @param estado estado del proceso
     */
    public void setEstado(Estado estado) {
        this.estado = estado;
        if (estado != Estado.ESPERA) motivoEspera = "";
    }

    /**
     * Registra el recurso que mantiene al proceso bloqueado.
     * @param motivo recurso por el que el proceso pasa a espera
     */
    public void setMotivoEspera(String motivo) { this.motivoEspera = motivo == null ? "" : motivo; }

    /**
     * Devuelve el estado junto con el motivo de espera, si corresponde.
     * @return valor calculado o estado consultado.
     */
    public String getEstadoDescripcion() {
        return estado == Estado.ESPERA && !motivoEspera.isEmpty() ? "ESPERA (" + motivoEspera + ")" : estado.name();
    }

    /**
     * Devuelve la dirección que ejecutará el proceso al reanudar.
     * @return valor numérico producido por la operación.
     */
    public int getProgramCounter() {
        return programCounter;
    }

    /**
     * Devuelve el valor guardado en el acumulador AC.
     * @return valor numérico producido por la operación.
     */
    public int getAc() {
        return ac;
    }

    /**
     * Devuelve el valor guardado en AX.
     * @return valor numérico producido por la operación.
     */
    public int getAx() {
        return ax;
    }

    /**
     * Devuelve el valor guardado en BX.
     * @return valor numérico producido por la operación.
     */
    public int getBx() {
        return bx;
    }

    /**
     * Devuelve el valor guardado en CX.
     * @return valor numérico producido por la operación.
     */
    public int getCx() {
        return cx;
    }

    /**
     * Devuelve el valor numérico guardado en DX.
     * @return valor numérico producido por la operación.
     */
    public int getDx() {
        return dx;
    }
    /**
     * Devuelve el operando textual asociado con DX, si existe.
     * @return texto asociado con el estado o valor consultado.
     */
    public String getDxTexto() { return dxTexto; }

    /**
     * Indica si zero flag.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean isZeroFlag() { return zeroFlag; }
    /**
     * Indica si overflow flag.
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    public boolean isOverflowFlag() { return overflowFlag; }

    /**
     * Devuelve el texto de la última instrucción cargada en IR.
     * @return texto asociado con el estado o valor consultado.
     */
    public String getIrTexto() { return irTexto; }

    /**
     * Devuelve la representación de la pila guardada en el BCP.
     * @return texto asociado con el estado o valor consultado.
     */
    public String getPilaTexto() { return pilaTexto; }
    /**
     * Actualiza la representación textual de la pila en el BCP.
     * @param pilaTexto representación textual de la pila
     */
    public void setPilaTexto(String pilaTexto) { this.pilaTexto = pilaTexto == null ? "[]" : pilaTexto; }
    /**
     * Devuelve el mensaje de error asociado al proceso.
     * @return texto asociado con el estado o valor consultado.
     */
    public String getMensajeError() { return mensajeError; }
    /**
     * Marca un error en el estado actual.
     * @param mensaje mensaje asociado con el estado
     */
    public void fallar(String mensaje) { this.mensajeError = mensaje; setEstado(Estado.ERROR); }
    /** Registra inicio. */
    public void marcarInicio() { if (horaInicio == null) horaInicio = LocalDateTime.now(); }
    /** Registra fin. */
    public void marcarFin() { if (horaFin == null) horaFin = LocalDateTime.now(); }
    /**
     * Devuelve el tiempo de CPU acumulado por el proceso.
     * @return cantidad de tiempo acumulado en segundos simulados.
     */
    public long getTiempoCpuSegundos() { return tiempoCpuSegundos; }
    /**
     * Actualiza el tiempo de CPU acumulado.
     * @param tiempoCpuSegundos tiempo acumulado de CPU en segundos simulados
     */
    public void setTiempoCpuSegundos(long tiempoCpuSegundos) { this.tiempoCpuSegundos = tiempoCpuSegundos; }
    /**
     * Devuelve la hora de inicio o un marcador si todavía no ha iniciado.
     * @return texto asociado con el estado o valor consultado.
     */
    public String getHoraInicio() { return horaInicio == null ? "-" : horaInicio.format(FORMATO_HORA); }
    /**
     * Devuelve la hora de finalización o un marcador si sigue activo.
     * @return texto asociado con el estado o valor consultado.
     */
    public String getHoraFin() { return horaFin == null ? "-" : horaFin.format(FORMATO_HORA); }
    /**
     * Devuelve los nombres de archivo abiertos por el proceso.
     * @return vista de los elementos correspondientes.
     */
    public List<String> getArchivosAbiertos() { return Collections.unmodifiableList(archivosAbiertos); }
    /**
     * Guarda en el BCP una copia de los archivos abiertos por el proceso.
     * @param archivos colección de archivos
     */
    public void setArchivosAbiertos(List<String> archivos) { archivosAbiertos = new ArrayList<>(archivos); }

    /**
     * Devuelve la dirección del BCP dentro del área de kernel.
     * @return valor numérico producido por la operación.
     */
    public int getDireccionBcp() { return direccionBcp; }

    /**
     * Guarda la dirección asignada al BCP en el kernel.
     * @param direccionBcp dirección del BCP dentro del kernel
     */
    public void setDireccionBcp(int direccionBcp) { this.direccionBcp = direccionBcp; }

    /**
     * Devuelve la dirección del siguiente BCP enlazado.
     * @return valor numérico producido por la operación.
     */
    public int getDireccionSiguienteBcp() { return direccionSiguienteBcp; }

    /**
     * Actualiza el enlace al siguiente BCP.
     * @param direccionSiguienteBcp dirección del siguiente BCP de la lista
     */
    public void setDireccionSiguienteBcp(int direccionSiguienteBcp) {
        this.direccionSiguienteBcp = direccionSiguienteBcp;
    }

    /**
     * Devuelve una representación textual de los datos principales del proceso.
     * @return valor calculado o estado consultado.
     */
    @Override
    public String toString() {
        return "PCB{" +
                "PID=" + pid +
                ", programa='" + nombrePrograma + '\'' +
                ", estado=" + estado +
                ", PC=" + programCounter +
                ", base=" + direccionBase +
                ", limite=" + direccionLimite +
                ", instrucciones=" + tamanoInstrucciones +
                ", AC=" + ac +
                ", AX=" + ax +
                ", BX=" + bx +
                ", CX=" + cx +
                ", DX=" + dx +
                '}';
    }
}
