package com.tec.minipc.gui;

import com.tec.minipc.core.PCB;
import com.tec.minipc.core.ProcessManager;
import com.tec.minipc.model.Memory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/** Muestra los bloques de control persistidos en el kernel, seis celdas por BCP. */
public final class PcbPanel extends JPanel {
    private static final Color TINTA = new Color(13, 59, 96);
    private static final Color SUAVE = new Color(43, 119, 159);
    private static final Color BORDE = new Color(188, 224, 241);
    private static final Color AZUL = new Color(31, 112, 173);
    private final JPanel lista = new JPanel();
    private final JLabel resumen = new JLabel("Kernel listo para recibir procesos");

    /** Inicializa PcbPanel con los recursos y valores recibidos. */
    public PcbPanel() {
        super(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Color.WHITE);
        resumen.setFont(new Font("Segoe UI", Font.BOLD, 13));
        resumen.setForeground(TINTA);
        add(resumen, BorderLayout.NORTH);
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setBackground(new Color(235, 248, 254));
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));
        scroll.getViewport().setBackground(lista.getBackground());
        add(scroll, BorderLayout.CENTER);
        actualizar(List.of(), null);
    }

    /** Redibuja todos los BCP ocupados y refleja el tamaño real reservado en memoria. */
    public void actualizar(List<ProcessManager.Proceso> procesos, Memory memoria) {
        lista.removeAll();
        int ocupados = procesos == null ? 0 : procesos.size();
        int celdasBcp = ocupados * Memory.CELDAS_POR_BCP;
        int tamanoKernel = memoria == null ? 0 : memoria.getOsSize();
        resumen.setText("KERNEL  ·  " + ocupados + " BCP  ·  " + celdasBcp + "/" + tamanoKernel
                + " celdas ocupadas  ·  " + Math.max(0, tamanoKernel - celdasBcp) + " libres");

        if (ocupados == 0) {
            JPanel vacio = new JPanel(new BorderLayout(0, 8));
            vacio.setOpaque(false);
            vacio.setBorder(BorderFactory.createEmptyBorder(36, 20, 36, 20));
            JLabel icono = new JLabel("▦", SwingConstants.CENTER);
            icono.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 38));
            icono.setForeground(new Color(77, 165, 204));
            JLabel mensaje = new JLabel("Los BCP aparecerán aquí al admitir procesos.", SwingConstants.CENTER);
            mensaje.setForeground(SUAVE);
            vacio.add(icono, BorderLayout.CENTER);
            vacio.add(mensaje, BorderLayout.SOUTH);
            vacio.setAlignmentX(LEFT_ALIGNMENT);
            lista.add(vacio);
        } else {
            for (ProcessManager.Proceso proceso : procesos) {
                lista.add(crearBloque(proceso.getPcb(), memoria));
                lista.add(Box.createVerticalStrut(10));
            }
        }
        lista.revalidate();
        lista.repaint();
    }

    /**
     * Construye la tarjeta visual con los atributos de un BCP.
     * @param pcb bloque de control del proceso
     * @param memoria memoria principal
     * @return valor calculado o estado consultado.
     */
    private JPanel crearBloque(PCB pcb, Memory memoria) {
        JPanel bloque = new JPanel(new BorderLayout(0, 8));
        bloque.setBackground(Color.WHITE);
        bloque.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE), BorderFactory.createEmptyBorder(10, 11, 10, 11)));
        bloque.setAlignmentX(LEFT_ALIGNMENT);
        bloque.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JPanel cabecera = new JPanel(new BorderLayout(8, 0));
        cabecera.setOpaque(false);
        JLabel nombre = new JLabel("PID " + pcb.getPid() + "  ·  " + pcb.getNombrePrograma());
        nombre.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nombre.setForeground(TINTA);
        JLabel estado = new JLabel(pcb.getEstadoDescripcion());
        estado.setOpaque(true);
        estado.setBackground(colorEstado(pcb.getEstado()));
        estado.setForeground(Color.WHITE);
        estado.setFont(new Font("Segoe UI", Font.BOLD, 11));
        estado.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        cabecera.add(nombre, BorderLayout.CENTER);
        cabecera.add(estado, BorderLayout.EAST);
        bloque.add(cabecera, BorderLayout.NORTH);

        JPanel celdas = new JPanel();
        celdas.setLayout(new BoxLayout(celdas, BoxLayout.Y_AXIS));
        celdas.setBackground(Color.WHITE);
        int base = pcb.getDireccionBcp();
        for (int i = 0; i < Memory.CELDAS_POR_BCP; i++) {
            int direccion = base + i;
            String valor = memoria == null || direccion < 0 || direccion >= memoria.getTotalSize()
                    ? "" : memoria.getDisplayValue(direccion);
            celdas.add(crearCelda(direccion, i + 1, etiquetaCelda(i), valor));
        }
        bloque.add(celdas, BorderLayout.CENTER);
        JLabel pie = new JLabel("Bloque: " + base + "–" + (base + Memory.CELDAS_POR_BCP - 1)
                + "  ·  Programa: " + pcb.getDireccionBase() + "–" + pcb.getDireccionLimite()
                + "  ·  " + pcb.getTamanoInstrucciones() + " instrucciones");
        pie.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pie.setForeground(SUAVE);
        bloque.add(pie, BorderLayout.SOUTH);
        return bloque;
    }

    /**
     * Crea una celda de atributo con alineación y estilo uniforme.
     * @param direccion dirección consultada
     * @param numero número que se presentará en la celda
     * @param etiqueta texto de la etiqueta
     * @param valor valor que se asignará
     * @return valor calculado o estado consultado.
     */
    private JPanel crearCelda(int direccion, int numero, String etiqueta, String valor) {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        fila.setBackground(numero % 2 == 0 ? new Color(235, 248, 254) : Color.WHITE);
        fila.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        fila.setAlignmentX(LEFT_ALIGNMENT);
        JLabel addr = new JLabel(String.format("%04d", direccion));
        addr.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        addr.setForeground(AZUL);
        addr.setPreferredSize(new Dimension(42, 18));
        JLabel campo = new JLabel(etiqueta);
        campo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        campo.setForeground(new Color(43, 119, 159));
        campo.setPreferredSize(new Dimension(104, 18));
        JLabel dato = new JLabel(valor == null || valor.isBlank() ? "—" : valor);
        dato.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dato.setForeground(new Color(24, 86, 128));
        fila.add(addr, BorderLayout.WEST);
        fila.add(campo, BorderLayout.CENTER);
        fila.add(dato, BorderLayout.EAST);
        fila.setToolTipText("Celda " + direccion + ": " + valor);
        return fila;
    }

    /**
     * Genera el texto compacto usado para presentar un campo del BCP.
     * @param indice posición en el índice
     * @return valor calculado o estado consultado.
     */
    private String etiquetaCelda(int indice) {
        return switch (indice) {
            case 0 -> "Identificación";
            case 1 -> "Estado";
            case 2 -> "PC · AC · AX · BX";
            case 3 -> "CX · DX · IR · flags";
            case 4 -> "Región de memoria";
            default -> "Pila · tiempo · enlaces";
        };
    }

    /**
     * Selecciona el color asociado con el estado del proceso.
     * @param estado estado del proceso
     * @return valor calculado o estado consultado.
     */
    private Color colorEstado(PCB.Estado estado) {
        return switch (estado) {
            case EJECUTANDO -> new Color(18, 76, 125);
            case LISTO -> new Color(31, 112, 173);
            case ESPERA -> new Color(45, 128, 171);
            case SUSPENDIDO -> new Color(58, 145, 184);
            case ERROR -> new Color(21, 91, 145);
            case TERMINADO -> new Color(73, 155, 190);
            default -> new Color(41, 120, 163);
        };
    }
}
