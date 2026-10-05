package com.tec.minipc.gui;

import com.tec.minipc.core.ProcessStack;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.util.List;

/** Vista de las cinco posiciones de la pila del proceso seleccionado. */
public class PilaPanel extends JPanel {
    private final JTextArea contenido = new JTextArea(4, 16);
    private final JLabel estado = new JLabel("SP=0/5");

    public PilaPanel() {
        setBorder(BorderFactory.createTitledBorder("Pila (5 posiciones)"));
        setLayout(new BorderLayout(2, 2));
        contenido.setEditable(false);
        add(contenido, BorderLayout.CENTER);
        add(estado, BorderLayout.SOUTH);
        limpiar();
    }

    public void actualizar(ProcessStack pila, String error) {
        List<Integer> valores = pila.getValoresDeArribaAbajo();
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < pila.getCapacidad(); i++) {
            texto.append(i < valores.size() ? "TOP -> " + valores.get(i) : "[vacía]").append('\n');
        }
        contenido.setText(texto.toString());
        estado.setText("SP=" + pila.getTamano() + "/" + pila.getCapacidad()
                + (error == null || error.isEmpty() ? "" : " | " + error));
    }

    public void limpiar() {
        contenido.setText("[vacía]\n[vacía]\n[vacía]\n[vacía]\n[vacía]");
        estado.setText("SP=0/5");
    }
}
