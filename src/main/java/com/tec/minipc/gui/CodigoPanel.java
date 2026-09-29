/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tec.minipc.gui;

import com.tec.minipc.model.Instruction;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

/**
 * Panel que muestra el programa .asm cargado como una tabla de dos columnas:
 * la instrucción en texto y su representación binaria de 2 bytes
 * (igual al formato "CODIGO ASM / COD BINARIO" del enunciado).
 * Resalta en amarillo la fila de la instrucción que se ejecutó en el último paso.
 */
public class CodigoPanel extends JPanel {

    private static final String[] COLUMNAS = {"Instrucción", "Binario"};

    private final DefaultTableModel modelo;
    private final JTable tabla;
    private int indiceResaltado = -1;

    public CodigoPanel() {
        setBorder(BorderFactory.createTitledBorder("Programa cargado"));
        setLayout(new BorderLayout());

        modelo = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setDefaultRenderer(Object.class, new ResaltadorInstruccionActual());

        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /**
     * Carga (o recarga por completo) la tabla a partir del programa parseado.
     * @param instrucciones lista de instrucciones tal como las devuelve el Assembler
     */
    public void cargar(List<Instruction> instrucciones) {
        modelo.setRowCount(0);
        for (Instruction instruccion : instrucciones) {
            int[] bytes = instruccion.encode();
            String binario = Instruction.toBinaryByte(bytes[0]) + " " + Instruction.toBinaryByte(bytes[1]);
            modelo.addRow(new Object[]{instruccion.getSourceLine(), binario});
        }
        indiceResaltado = -1;
    }

    /**
     * Resalta la instrucción en la posición dada (0 = primera línea del programa).
     * Pasar -1 quita el resaltado.
     * @param indice índice de la instrucción a resaltar
     */
    public void resaltar(int indice) {
        this.indiceResaltado = indice;
        tabla.repaint();
        if (indice >= 0 && indice < tabla.getRowCount()) {
            tabla.scrollRectToVisible(tabla.getCellRect(indice, 0, true));
        }
    }

    /** Resalta en amarillo toda la fila cuyo índice coincide con indiceResaltado. */
    private class ResaltadorInstruccionActual extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBackground(row == indiceResaltado ? Color.YELLOW : Color.WHITE);
            return c;
        }
    }
}