package com.tec.minipc.gui;

import com.tec.minipc.model.SecondaryStorage;
import com.tec.minipc.core.SimulatedFileSystem;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

/** Vista del índice de archivos, los datos guardados y la partición de memoria virtual. */
public final class AlmacenamientoPanel extends JPanel {
    private final JLabel resumen = new JLabel("Disco secundario sin configurar");
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Zona", "Dirección", "Contenido"}, 0) {
    /**
     * Indica si la celda puede editarse directamente desde la tabla.
     * @param row índice de fila
     * @param column índice de columna
     * @return true si se cumple la condición indicada; de lo contrario, false.
     */
    @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    /** Inicializa AlmacenamientoPanel con los recursos y valores recibidos. */
    public AlmacenamientoPanel() {
        setBorder(BorderFactory.createTitledBorder("Almacenamiento secundario"));
        setLayout(new BorderLayout(4, 4));
        add(resumen, BorderLayout.NORTH);
        JTable tabla = new JTable(modelo);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /**
     * Refresca la vista con los datos actuales del modelo recibido.
     * @param disco almacenamiento secundario que se presentará
     */
    public void actualizar(SecondaryStorage disco) { actualizar(disco, null); }

    /**
     * Refresca la vista con los datos actuales del modelo recibido.
     * @param disco almacenamiento secundario que se presentará
     * @param sistemaArchivos sistema de archivos simulado
     */
    public void actualizar(SecondaryStorage disco, SimulatedFileSystem sistemaArchivos) {
        modelo.setRowCount(0);
        if (disco == null) {
            resumen.setText("Disco secundario sin configurar");
            return;
        }
        resumen.setText("Total: " + disco.getCapacidad() + " celdas  |  Índice: " + disco.getCeldasIndice()
                + "  |  Programas: " + disco.getArchivos().size() + "/" + SecondaryStorage.MAX_ARCHIVOS
                + "  |  Archivos: " + disco.getArchivosSistema().size() + "/" + SecondaryStorage.MAX_ARCHIVOS_SISTEMA
                + "  |  Datos libres: " + disco.getCeldasDatosDisponibles()
                + "  |  Datos INT 21H: " + disco.getBytesArchivosSistema() + " bytes"
                + "  |  Páginas virtuales: " + disco.getPaginasVirtualesUsadas() + "/" + disco.getMemoriaVirtual());
        for (int i = 0; i < disco.getCeldasIndice(); i++) {
            String contenido = disco.getValor(i);
            if (!contenido.isEmpty()) modelo.addRow(new Object[]{"Índice", i, contenido});
        }
        for (int i = disco.getCeldasIndice(); i < disco.getInicioMemoriaVirtual(); i++) {
            String contenido = disco.getValor(i);
            if (!contenido.isEmpty()) modelo.addRow(new Object[]{"Archivo", i, "Byte " + contenido});
        }
        for (int i = disco.getInicioMemoriaVirtual(); i < disco.getCapacidad(); i++) {
            String contenido = disco.getValor(i);
            modelo.addRow(new Object[]{"Memoria virtual", i, contenido.isEmpty() ? "Página libre" : contenido});
        }
        for (String evento : disco.getEventosMemoriaVirtual()) {
            modelo.addRow(new Object[]{"Transferencia virtual", "RAM/disco", evento});
        }
        if (sistemaArchivos != null) {
            for (String archivo : sistemaArchivos.getArchivos()) {
                modelo.addRow(new Object[]{"INT 21H", "disco", archivo});
            }
            for (String evento : sistemaArchivos.getEventos()) {
                modelo.addRow(new Object[]{"Evento INT 21H", "CPU", evento});
            }
        }
    }
}
