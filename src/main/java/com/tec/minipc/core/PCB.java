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
 *
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

    public int getPid() {
        return pid;
    }

    public String getNombrePrograma() {
        return nombrePrograma;
    }

    public int getDireccionBase() {
        return direccionBase;
    }

    public int getDireccionLimite() {
        return direccionLimite;
    }

    public void asignarRegionMemoria(int base) {
        this.direccionBase = base;
        this.direccionLimite = base + tamanoInstrucciones - 1;
        this.programCounter = base;
    }

    public int getTamanoInstrucciones() {
        return tamanoInstrucciones;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
        if (estado != Estado.ESPERA) motivoEspera = "";
    }

    public void setMotivoEspera(String motivo) { this.motivoEspera = motivo == null ? "" : motivo; }

    public String getEstadoDescripcion() {
        return estado == Estado.ESPERA && !motivoEspera.isEmpty() ? "ESPERA (" + motivoEspera + ")" : estado.name();
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public int getAc() {
        return ac;
    }

    public int getAx() {
        return ax;
    }

    public int getBx() {
        return bx;
    }

    public int getCx() {
        return cx;
    }

    public int getDx() {
        return dx;
    }
    public String getDxTexto() { return dxTexto; }

    public boolean isZeroFlag() { return zeroFlag; }
    public boolean isOverflowFlag() { return overflowFlag; }

    public String getIrTexto() { return irTexto; }

    public String getPilaTexto() { return pilaTexto; }
    public void setPilaTexto(String pilaTexto) { this.pilaTexto = pilaTexto == null ? "[]" : pilaTexto; }
    public String getMensajeError() { return mensajeError; }
    public void fallar(String mensaje) { this.mensajeError = mensaje; setEstado(Estado.ERROR); }
    public void marcarInicio() { if (horaInicio == null) horaInicio = LocalDateTime.now(); }
    public void marcarFin() { if (horaFin == null) horaFin = LocalDateTime.now(); }
    public long getTiempoCpuSegundos() { return tiempoCpuSegundos; }
    public void setTiempoCpuSegundos(long tiempoCpuSegundos) { this.tiempoCpuSegundos = tiempoCpuSegundos; }
    public String getHoraInicio() { return horaInicio == null ? "-" : horaInicio.format(FORMATO_HORA); }
    public String getHoraFin() { return horaFin == null ? "-" : horaFin.format(FORMATO_HORA); }
    public List<String> getArchivosAbiertos() { return Collections.unmodifiableList(archivosAbiertos); }
    public void setArchivosAbiertos(List<String> archivos) { archivosAbiertos = new ArrayList<>(archivos); }

    public int getDireccionBcp() { return direccionBcp; }

    public void setDireccionBcp(int direccionBcp) { this.direccionBcp = direccionBcp; }

    public int getDireccionSiguienteBcp() { return direccionSiguienteBcp; }

    public void setDireccionSiguienteBcp(int direccionSiguienteBcp) {
        this.direccionSiguienteBcp = direccionSiguienteBcp;
    }

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
