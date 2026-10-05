package com.tec.minipc.gui;

import com.tec.minipc.core.ProcessManager;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;

/** Tabla de procesos admitidos y su ubicación/estado en el sistema. */
public class TrabajosPanel extends JPanel {
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"PID", "Programa", "Estado", "BCP", "Base", "Tamaño", "CPU s", "Inicio", "Fin"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    public TrabajosPanel() {
        setBorder(BorderFactory.createTitledBorder("Lista de trabajos (FCFS)"));
        setLayout(new BorderLayout());
        JTable tabla = new JTable(modelo);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    public void actualizar(List<ProcessManager.Proceso> procesos) {
        modelo.setRowCount(0);
        for (ProcessManager.Proceso p : procesos) {
            modelo.addRow(new Object[]{p.getPcb().getPid(), p.getPcb().getNombrePrograma(),
                p.getPcb().getEstadoDescripcion(), p.getPcb().getDireccionBcp(), p.getBase(), p.getTamano(),
                p.getPcb().getTiempoCpuSegundos(), p.getPcb().getHoraInicio(), p.getPcb().getHoraFin()});
        }
    }
}
