package com.tec.minipc;

import com.tec.minipc.gui.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada del Mini PC. Lanza la interfaz gráfica en el
 * Event Dispatch Thread de Swing, como corresponde para cualquier
 * aplicación Swing.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame ventana = new MainFrame();
            ventana.setVisible(true);
        });
    }
}