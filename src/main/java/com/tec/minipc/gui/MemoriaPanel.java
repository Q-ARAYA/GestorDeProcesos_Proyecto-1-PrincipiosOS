package com.tec.minipc.gui;

import com.tec.minipc.model.Instruction;
import com.tec.minipc.model.Memory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

/**
 * Panel que muestra el contenido completo de la memoria como una tabla de
 * dos columnas: posición y valor (igual al formato "Pos / Valor en memoria").
 * Las celdas de la zona de S.O. se marcan en gris claro, y la posición
 * donde apunta el PC actualmente se resalta en amarillo.
 */
public class MemoriaPanel extends JPanel {

    private static final String[] COLUMNAS = {"Pos", "Valor en memoria"};
    private static final Color COLOR_SO = new Color(225, 225, 225);

    private final DefaultTableModel modelo;
    private final JTable tabla;
    private int direccionResaltada = -1;
    private Memory memoriaActual;

    public MemoriaPanel() {
        setBorder(BorderFactory.createTitledBorder("Memoria"));
        setLayout(new BorderLayout());

        modelo = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // la memoria solo se modifica ejecutando instrucciones
            }
        };
        tabla = new JTable(modelo);
        tabla.setDefaultRenderer(Object.class, new ResaltadorCeldaActual());
        tabla.setRowHeight(20);

        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /**
     * Carga (o recarga por completo) el contenido de la tabla a partir de la memoria dada.
     * @param memoria la memoria del Mini PC a mostrar
     */
    public void cargar(Memory memoria) {
        this.memoriaActual = memoria;
        modelo.setRowCount(0);
        for (int direccion = 0; direccion < memoria.getTotalSize(); direccion++) {
            Instruction instruccion = memoria.read(direccion);
            String valor = (instruccion == null) ? "" : instruccion.getSourceLine();
            modelo.addRow(new Object[]{direccion, valor});
        }
    }

    /**
     * Recarga la tabla y resalta la posición actual (donde está el PC).
     * @param memoria la memoria del Mini PC a mostrar
     * @param direccionActual la dirección a resaltar (normalmente registros.getPc())
     */
    public void actualizar(Memory memoria, int direccionActual) {
        this.direccionResaltada = direccionActual;
        cargar(memoria);
        if (direccionActual >= 0 && direccionActual < tabla.getRowCount()) {
            tabla.scrollRectToVisible(tabla.getCellRect(direccionActual, 0, true));
        }
    }

    /** Marca en gris la zona de S.O. y en amarillo la posición del PC actual. */
    private class ResaltadorCeldaActual extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int direccionFila = (int) table.getValueAt(row, 0);

            if (direccionFila == direccionResaltada) {
                c.setBackground(Color.YELLOW);
            } else if (memoriaActual != null && memoriaActual.isOsAddress(direccionFila)) {
                c.setBackground(COLOR_SO);
            } else {
                c.setBackground(Color.WHITE);
            }
            return c;
        }
    }
}