package com.tec.minipc.gui;

import com.tec.minipc.core.ProcessManager;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;

/** Resumen acumulado de duración simulada y horarios de cada proceso. */
public final class EstadisticasPanel extends JPanel {
    private final JLabel resumen = new JLabel("Aún no hay procesos para resumir.");
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"PID", "Proceso", "Inicio", "Final", "Duración CPU (s)", "Resultado"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    public EstadisticasPanel() {
        setBorder(BorderFactory.createTitledBorder("Estadísticas de ejecución"));
        setLayout(new BorderLayout(4, 4));
        add(resumen, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(modelo)), BorderLayout.CENTER);
    }

    public void actualizar(List<ProcessManager.Proceso> procesos) {
        modelo.setRowCount(0);
        long total = 0;
        int terminados = 0;
        int errores = 0;
        for (ProcessManager.Proceso proceso : procesos) {
            var pcb = proceso.getPcb();
            long segundos = pcb.getTiempoCpuSegundos();
            total += segundos;
            if (pcb.getEstado() == com.tec.minipc.core.PCB.Estado.TERMINADO) terminados++;
            if (pcb.getEstado() == com.tec.minipc.core.PCB.Estado.ERROR) errores++;
            modelo.addRow(new Object[]{pcb.getPid(), pcb.getNombrePrograma(), pcb.getHoraInicio(),
                pcb.getHoraFin(), segundos, pcb.getEstadoDescripcion()});
        }
        resumen.setText("Procesos: " + procesos.size() + "  |  Finalizados: " + terminados
                + "  |  Con error: " + errores + "  |  Tiempo total de CPU simulado: " + total + " s");
    }

    public void limpiar() { actualizar(List.of()); }
}
