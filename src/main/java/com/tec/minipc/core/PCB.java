/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tec.minipc.core;

import com.tec.minipc.model.RegisterName;
import com.tec.minipc.model.Registers;

/**
 * Bloque de Control de Proceso (BCP / PCB).
 *
 * Guarda la información administrativa del proceso que el simulador tiene
 * cargado en un momento dado: identificación, estado, límites de memoria
 * asignados y una copia ("foto") de los registros para poder mostrarla en
 * la interfaz sin exponer el objeto Registers real del CPU.
 */
public class PCB {

    /** Estados posibles de un proceso dentro del simulador. */
    public enum Estado {
        NUEVO,       // el .asm ya se cargó a memoria pero aún no arrancó
        LISTO,       // esperando el próximo paso de ejecución
        EJECUTANDO,  // se está procesando el paso actual
        TERMINADO    // ya no quedan instrucciones por ejecutar
    }

    private final int pid;
    private final String nombrePrograma;
    private final int direccionBase;   // primera celda de memoria del proceso
    private final int direccionLimite; // última celda de memoria del proceso
    private final int tamanoInstrucciones; // cantidad de instrucciones del programa

    private Estado estado;
    private int programCounter;

    // Foto de los registros al momento de la última actualización
    private int ac;
    private int ax;
    private int bx;
    private int cx;
    private int dx;

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

    public int getTamanoInstrucciones() {
        return tamanoInstrucciones;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
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