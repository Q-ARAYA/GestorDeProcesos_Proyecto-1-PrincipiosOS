package com.tec.minipc;

import com.tec.minipc.gui.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada del Mini PC. Lanza la interfaz gráfica en el
 * Event Dispatch Thread de Swing, como corresponde para cualquier
 * aplicación Swing.
 */
public class Main {

    /**
     * Inicia Swing en el Event Dispatch Thread y crea la ventana principal del simulador.
     * @param args argumentos de la aplicación
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame ventana = new MainFrame();
            ventana.setVisible(true);
        });
    }
}