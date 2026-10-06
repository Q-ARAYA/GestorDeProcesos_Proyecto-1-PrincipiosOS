/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tec.minipc.gui;

import com.tec.minipc.core.PCB;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Panel que muestra los atributos del BCP (Bloque de Control de Proceso)
 * del proceso actualmente cargado. Al igual que RegistrosPanel, no tiene
 * lógica propia: solo expone actualizar(PCB) para refrescarse.
 */
public class PcbPanel extends JPanel {

    private final JLabel lblPid = crearValor();
    private final JLabel lblPrograma = crearValor();
    private final JLabel lblEstado = crearValor();
    private final JLabel lblPc = crearValor();
    private final JLabel lblBase = crearValor();
    private final JLabel lblLimite = crearValor();
    private final JLabel lblInstrucciones = crearValor();
    private final JLabel lblBcp = crearValor();
    private final JLabel lblSiguiente = crearValor();
    private final JLabel lblFlags = crearValor();
    private final JLabel lblTiempoCpu = crearValor();
    private final JLabel lblInicio = crearValor();
    private final JLabel lblFin = crearValor();
    private final JLabel lblAc = crearValor();
    private final JLabel lblAx = crearValor();
    private final JLabel lblBx = crearValor();
    private final JLabel lblCx = crearValor();
    private final JLabel lblDx = crearValor();
    private final JLabel lblArchivosAbiertos = crearValor();

    public PcbPanel() {
        setBorder(BorderFactory.createTitledBorder("BCP (Bloque de Control de Proceso)"));
        setLayout(new GridLayout(19, 2, 4, 3));

        agregarFila("PID:", lblPid);
        agregarFila("Programa:", lblPrograma);
        agregarFila("Estado:", lblEstado);
        agregarFila("PC:", lblPc);
        agregarFila("Dirección base:", lblBase);
        agregarFila("Dirección límite:", lblLimite);
        agregarFila("Instrucciones:", lblInstrucciones);
        agregarFila("BCP en kernel:", lblBcp);
        agregarFila("Siguiente BCP:", lblSiguiente);
        agregarFila("ZF / OF:", lblFlags);
        agregarFila("CPU (segundos):", lblTiempoCpu);
        agregarFila("Hora inicio:", lblInicio);
        agregarFila("Hora final:", lblFin);
        agregarFila("AC:", lblAc);
        agregarFila("AX:", lblAx);
        agregarFila("BX:", lblBx);
        agregarFila("CX:", lblCx);
        agregarFila("DX:", lblDx);
        agregarFila("Archivos abiertos:", lblArchivosAbiertos);

        limpiar();
    }

    private void agregarFila(String etiqueta, JLabel valor) {
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        add(lbl);
        add(valor);
    }

    private JLabel crearValor() {
        JLabel lbl = new JLabel("-");
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 13f));
        return lbl;
    }

    /** Deja todas las etiquetas en "-", para cuando todavía no hay proceso cargado. */
    public void limpiar() {
        lblPid.setText("-");
        lblPrograma.setText("-");
        lblEstado.setText("-");
        lblPc.setText("-");
        lblBase.setText("-");
        lblLimite.setText("-");
        lblInstrucciones.setText("-");
        lblBcp.setText("-");
        lblSiguiente.setText("-");
        lblFlags.setText("-");
        lblTiempoCpu.setText("-");
        lblInicio.setText("-");
        lblFin.setText("-");
        lblAc.setText("-");
        lblAx.setText("-");
        lblBx.setText("-");
        lblCx.setText("-");
        lblDx.setText("-");
        lblArchivosAbiertos.setText("-");
    }

    /**
     * Refresca las etiquetas con los valores actuales del PCB.
     * @param pcb el bloque de control de proceso a mostrar
     */
    public void actualizar(PCB pcb) {
        lblPid.setText(String.valueOf(pcb.getPid()));
        lblPrograma.setText(pcb.getNombrePrograma());
        lblEstado.setText(pcb.getEstadoDescripcion());
        lblPc.setText(String.valueOf(pcb.getProgramCounter()));
        lblBase.setText(String.valueOf(pcb.getDireccionBase()));
        lblLimite.setText(String.valueOf(pcb.getDireccionLimite()));
        lblInstrucciones.setText(String.valueOf(pcb.getTamanoInstrucciones()));
        lblBcp.setText(String.valueOf(pcb.getDireccionBcp()));
        lblSiguiente.setText(pcb.getDireccionSiguienteBcp() < 0 ? "-" : String.valueOf(pcb.getDireccionSiguienteBcp()));
        lblFlags.setText((pcb.isZeroFlag() ? "1" : "0") + " / " + (pcb.isOverflowFlag() ? "1" : "0"));
        lblTiempoCpu.setText(String.valueOf(pcb.getTiempoCpuSegundos()));
        lblInicio.setText(pcb.getHoraInicio());
        lblFin.setText(pcb.getHoraFin());
        lblAc.setText(String.valueOf(pcb.getAc()));
        lblAx.setText(String.valueOf(pcb.getAx()));
        lblBx.setText(String.valueOf(pcb.getBx()));
        lblCx.setText(String.valueOf(pcb.getCx()));
        lblDx.setText(String.valueOf(pcb.getDx()));
        lblArchivosAbiertos.setText(pcb.getArchivosAbiertos().isEmpty() ? "-" : String.join(", ", pcb.getArchivosAbiertos()));
    }
}
