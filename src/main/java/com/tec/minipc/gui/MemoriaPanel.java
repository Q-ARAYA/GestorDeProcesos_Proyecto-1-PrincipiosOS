package com.tec.minipc.gui;

import com.tec.minipc.model.Memory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;

/**
 * Panel que muestra el contenido completo de la memoria como una tabla de
 * dos columnas: posición y valor (igual al formato "Pos / Valor en memoria").
 * Las celdas distinguen kernel, usuario y la dirección activa del PC
 * mediante tonos de azul y celeste.
 */
public class MemoriaPanel extends JPanel {

    private static final String[] COLUMNAS = {"Pos", "Valor en memoria"};
    private static final Color COLOR_SO = new Color(204,204,204);
    private static final Color COLOR_USUARIO = Color.WHITE;
    private static final Color COLOR_PC = new Color(173, 224, 246);

    private final DefaultTableModel modelo;
    private final JTable tabla;
    private int direccionResaltada = -1;
    private Memory memoriaActual;

    /** Inicializa MemoriaPanel con los recursos y valores recibidos. */
    public MemoriaPanel() {
        setLayout(new BorderLayout());

        modelo = new DefaultTableModel(COLUMNAS, 0) {
            /**
             * Indica si la celda puede editarse directamente desde la tabla.
             * @param row índice de fila
             * @param column índice de columna
             * @return true si se cumple la condición indicada; de lo contrario, false.
             */
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // la memoria solo se modifica ejecutando instrucciones
            }
        };
        tabla = new JTable(modelo);
        tabla.setDefaultRenderer(Object.class, new ResaltadorCeldaActual());
        tabla.setRowHeight(20);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        tabla.getColumnModel().getColumn(0).setMinWidth(48);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(56);
        tabla.getColumnModel().getColumn(0).setMaxWidth(64);
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 4));
        leyenda.setBackground(Color.WHITE);
        leyenda.add(etiquetaLeyenda("Kernel", COLOR_SO, new Color(24, 86, 128)));
        leyenda.add(etiquetaLeyenda("Usuario", COLOR_USUARIO, new Color(43, 119, 159)));
        leyenda.add(etiquetaLeyenda("PC actual", COLOR_PC, new Color(13, 59, 96)));
        add(leyenda, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(188, 224, 241)));
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Crea una etiqueta de leyenda con los colores de una región de memoria.
     * @param texto texto mostrado
     * @param fondo color de fondo
     * @param tinta color de texto
     * @return valor calculado o estado consultado.
     */
    private JLabel etiquetaLeyenda(String texto, Color fondo, Color tinta) {
        JLabel etiqueta = new JLabel("  " + texto + "  ");
        etiqueta.setOpaque(true);
        etiqueta.setBackground(fondo);
        etiqueta.setForeground(tinta);
        etiqueta.setFont(etiqueta.getFont().deriveFont(java.awt.Font.BOLD, 11f));
        etiqueta.setBorder(BorderFactory.createLineBorder(new Color(169, 213, 234)));
        return etiqueta;
    }

    /**
     * Carga (o recarga por completo) el contenido de la tabla a partir de la memoria dada.
     * @param memoria la memoria del Mini PC a mostrar
     */
    public void cargar(Memory memoria) {
        this.memoriaActual = memoria;
        modelo.setRowCount(0);
        for (int direccion = 0; direccion < memoria.getTotalSize(); direccion++) {
            String valor = memoria.getDisplayValue(direccion);
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

    /** Distingue el kernel, el espacio de usuario y la dirección actual del PC. */
    private class ResaltadorCeldaActual extends DefaultTableCellRenderer {
        /**
         * Configura el aspecto de la celda según su contenido y estado.
         * @param table tabla mostrada
         * @param value valor de la celda
         * @param isSelected indica si la fila está seleccionada
         * @param hasFocus indica si la celda tiene el foco
         * @param row fila de la tabla
         * @param column columna de la tabla
         * @return valor calculado o recurso consultado.
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int direccionFila = (int) table.getValueAt(row, 0);

            if (!isSelected) {
                if (direccionFila == direccionResaltada) {
                    c.setBackground(COLOR_PC);
                } else if (memoriaActual != null && memoriaActual.isOsAddress(direccionFila)) {
                    c.setBackground(COLOR_SO);
                } else {
                    c.setBackground(row % 2 == 0 ? COLOR_USUARIO : new Color(242, 250, 255));
                }
                c.setForeground(column == 0 ? new Color(31, 112, 173) : new Color(24, 86, 128));
            }
            if (c instanceof JLabel etiqueta) {
                etiqueta.setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                etiqueta.setFont(column == 0
                        ? new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 11)
                        : table.getFont());
            }
            return c;
        }
    }
}
