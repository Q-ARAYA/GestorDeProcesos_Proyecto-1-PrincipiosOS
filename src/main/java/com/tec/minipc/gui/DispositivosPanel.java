package com.tec.minipc.gui;

import com.tec.minipc.core.ProcessStack;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.IntConsumer;

/** Consola visible: pantalla de salida INT 10H y teclado numérico INT 09H. */
public class DispositivosPanel extends JPanel {
    private final JTextArea pantalla = new JTextArea(5, 32);
    private final JTextField campoEntrada = new JTextField(8);
    private final JLabel estado = new JLabel("INT 10H imprime DX; INT 09H lee un entero de 0 a 255.");
    private final PilaPanel pilaPanel = new PilaPanel();
    private final IntConsumer enviarEntrada;

    public DispositivosPanel(IntConsumer enviarEntrada) {
        this.enviarEntrada = enviarEntrada;
        setBorder(BorderFactory.createTitledBorder("Dispositivos de entrada / salida"));
        setLayout(new BorderLayout(6, 4));
        pantalla.setEditable(false);
        pantalla.setLineWrap(true);
        pantalla.setWrapStyleWord(true);
        JPanel panelPantalla = new JPanel(new BorderLayout());
        panelPantalla.setBorder(BorderFactory.createTitledBorder("Pantalla simulada"));
        panelPantalla.add(new JScrollPane(pantalla), BorderLayout.CENTER);

        JButton btnEnviar = new JButton("Enviar / Enter");
        btnEnviar.addActionListener(e -> enviar());
        campoEntrada.addActionListener(e -> enviar());
        JPanel panelTeclado = new JPanel(new GridLayout(2, 1, 3, 3));
        panelTeclado.setBorder(BorderFactory.createTitledBorder("Teclado / consola"));
        JPanel fila = new JPanel();
        fila.add(new JLabel("Valor (0-255):"));
        fila.add(campoEntrada);
        fila.add(btnEnviar);
        panelTeclado.add(fila);
        panelTeclado.add(estado);

        JPanel panelLateral = new JPanel(new BorderLayout(3, 3));
        panelLateral.add(panelTeclado, BorderLayout.NORTH);
        panelLateral.add(pilaPanel, BorderLayout.CENTER);
        add(panelPantalla, BorderLayout.CENTER);
        add(panelLateral, BorderLayout.EAST);
    }

    private void enviar() {
        String texto = campoEntrada.getText().trim();
        try {
            int valor = Integer.parseInt(texto);
            if (valor < 0 || valor > 255) throw new NumberFormatException();
            enviarEntrada.accept(valor);
            campoEntrada.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese un número entero entre 0 y 255.",
                    "Entrada inválida", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void actualizar(List<String> salida, int procesosEsperando) {
        pantalla.setText(String.join(System.lineSeparator(), salida));
        if (procesosEsperando > 0) {
            estado.setText(procesosEsperando + " proceso(s) esperando una entrada INT 09H.");
        } else {
            estado.setText("Entrada preparada. INT 10H imprime DX; INT 09H lee un entero de 0 a 255.");
        }
        pantalla.setCaretPosition(pantalla.getDocument().getLength());
    }

    public void actualizarPila(ProcessStack pila, String error) {
        if (pila == null) pilaPanel.limpiar();
        else pilaPanel.actualizar(pila, error);
    }

    public void limpiar() {
        pantalla.setText("");
        campoEntrada.setText("");
        estado.setText("INT 10H imprime DX; INT 09H lee un entero de 0 a 255.");
        pilaPanel.limpiar();
    }
}
