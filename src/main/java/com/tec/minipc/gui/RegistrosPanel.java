/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tec.minipc.gui;

import com.tec.minipc.model.RegisterName;
import com.tec.minipc.model.Registers;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Panel que muestra el estado actual de los registros del Mini PC.
 * No tiene lógica propia: solo expone actualizar(Registers) para refrescar
 * las etiquetas cada vez que el Cpu ejecuta un paso.
 */
public class RegistrosPanel extends JPanel {

    private final JLabel lblAc = crearValor();
    private final JLabel lblAx = crearValor();
    private final JLabel lblBx = crearValor();
    private final JLabel lblCx = crearValor();
    private final JLabel lblDx = crearValor();
    private final JLabel lblPc = crearValor();
    private final JLabel lblIr = crearValor();

    public RegistrosPanel() {
        setBorder(BorderFactory.createTitledBorder("Registros"));
        setLayout(new GridLayout(7, 2, 5, 5));

        agregarFila("AC:", lblAc);
        agregarFila("AX:", lblAx);
        agregarFila("BX:", lblBx);
        agregarFila("CX:", lblCx);
        agregarFila("DX:", lblDx);
        agregarFila("PC:", lblPc);
        agregarFila("IR:", lblIr);

        actualizar(new Registers()); // valores iniciales en 0
    }

    private void agregarFila(String etiqueta, JLabel valor) {
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        add(lbl);
        add(valor);
    }

    private JLabel crearValor() {
        JLabel lbl = new JLabel("0");
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 13f));
        return lbl;
    }

    /**
     * Refresca las etiquetas con los valores actuales del banco de registros.
     * @param registros el estado actual de los registros del Cpu
     */
    public void actualizar(Registers registros) {
        lblAc.setText(String.valueOf(registros.getAc()));
        lblAx.setText(String.valueOf(registros.get(RegisterName.AX)));
        lblBx.setText(String.valueOf(registros.get(RegisterName.BX)));
        lblCx.setText(String.valueOf(registros.get(RegisterName.CX)));
        lblDx.setText(String.valueOf(registros.get(RegisterName.DX)));
        lblPc.setText(String.valueOf(registros.getPc()));
        lblIr.setText(registros.getIr() == null ? "-" : registros.getIr().getSourceLine());
    }
}