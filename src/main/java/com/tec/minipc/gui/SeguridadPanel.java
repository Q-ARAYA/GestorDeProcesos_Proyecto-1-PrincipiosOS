package com.tec.minipc.gui;

import com.tec.minipc.core.ProcessManager;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/** Describe las barreras del simulador y expone fallos y llamadas al sistema observables. */
public final class SeguridadPanel extends JPanel {
    private final JTextArea texto = new JTextArea();

    /** Inicializa SeguridadPanel con los recursos y valores recibidos. */
    public SeguridadPanel() {
        setBorder(BorderFactory.createTitledBorder("Protección y seguridad"));
        setLayout(new BorderLayout());
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        add(new JScrollPane(texto), BorderLayout.CENTER);
        actualizar(List.of(), List.of(), null);
    }

    /**
     * Actualiza la vista con el estado recibido.
     * @param procesos procesos que se van a representar
     * @param eventos eventos de proceso que se presentan
     * @param eventosArchivos eventos del sistema de archivos
     */
    public void actualizar(List<ProcessManager.Proceso> procesos, List<String> eventos,
            List<String> eventosArchivos) {
        StringBuilder contenido = new StringBuilder();
        contenido.append("ESTRATEGIA DE PROTECCIÓN\n")
                .append("• Memoria separa kernel y usuario; los programas solo se cargan en direcciones de usuario.\n")
                .append("• El ensamblador valida sintaxis y destinos de salto antes de admitir procesos.\n")
                .append("• CPU y memoria validan límites; un salto fuera del programa finaliza ese proceso.\n")
                .append("• La pila por proceso tiene capacidad 5; desbordamiento y subdesbordamiento reportan error.\n")
                .append("• El teclado limita entradas a 0–255 y el disco aplica límites de capacidad.\n")
                .append("• INT 21H restringe los nombres y valida función, archivo abierto y espacio disponible.\n\n")
                .append("INCIDENTES DE PROCESOS\n");
        boolean alguno = false;
        for (ProcessManager.Proceso proceso : procesos) {
            if (!proceso.getPcb().getMensajeError().isEmpty()) {
                contenido.append("PID ").append(proceso.getPcb().getPid()).append(" ")
                        .append(proceso.getPcb().getNombrePrograma()).append(": ")
                        .append(proceso.getPcb().getMensajeError()).append('\n');
                alguno = true;
            }
        }
        for (String evento : eventos) { contenido.append(evento).append('\n'); alguno = true; }
        if (!alguno) contenido.append("Sin errores registrados.\n");
        contenido.append("\nACTIVIDAD DE ARCHIVOS INT 21H\n");
        if (eventosArchivos == null || eventosArchivos.isEmpty()) contenido.append("Sin llamadas todavía.\n");
        else for (String evento : eventosArchivos) contenido.append(evento).append('\n');
        texto.setText(contenido.toString());
        texto.setCaretPosition(0);
    }
}
