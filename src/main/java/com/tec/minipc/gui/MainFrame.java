/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tec.minipc.gui;

import com.tec.minipc.core.Assembler;
import com.tec.minipc.core.AssemblyException;
import com.tec.minipc.core.Cpu;
import com.tec.minipc.core.PCB;
import com.tec.minipc.model.Instruction;
import com.tec.minipc.model.Memory;
import com.tec.minipc.model.Registers;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Ventana principal del simulador Mini PC. Junta los 4 paneles (código,
 * registros, PCB y memoria) y hace de puente con el motor de ejecución
 * (Assembler + Memory + Registers + PCB + Cpu).
 */
public class MainFrame extends JFrame {

    private final JButton btnCargar = new JButton("Cargar .asm...");
    private final JSpinner spTotal = new JSpinner(new SpinnerNumberModel(256, Memory.TAMANO_MINIMO, 256, 1));
    private final JSpinner spSO = new JSpinner(new SpinnerNumberModel(64, 1, 255, 1));
    private final JButton btnSiguiente = new JButton("Siguiente");
    private final JButton btnEjecutarTodo = new JButton("Ejecutar todo");
    private final JButton btnReiniciar = new JButton("Reiniciar");
    private final JLabel lblMensaje = new JLabel(" ");

    private final CodigoPanel codigoPanel = new CodigoPanel();
    private final RegistrosPanel registrosPanel = new RegistrosPanel();
    private final PcbPanel pcbPanel = new PcbPanel();
    private final MemoriaPanel memoriaPanel = new MemoriaPanel();

    // Estado de la ejecución actual
    private List<Instruction> instrucciones;
    private Memory memoria;
    private Registers registros;
    private PCB pcb;
    private Cpu cpu;
    private int direccionBase;
    private File archivoActual;

    public MainFrame() {
        super("Mini PC - Simulador (Principios de Sistemas Operativos)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        armarLayout();
        registrarAcciones();
        actualizarBotones();
    }

    private void armarLayout() {
        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelControles.add(btnCargar);
        panelControles.add(new JLabel("Memoria total:"));
        panelControles.add(spTotal);
        panelControles.add(new JLabel("Espacio S.O.:"));
        panelControles.add(spSO);
        panelControles.add(btnSiguiente);
        panelControles.add(btnEjecutarTodo);
        panelControles.add(btnReiniciar);

        JPanel panelDerecho = new JPanel();
        panelDerecho.setLayout(new BoxLayout(panelDerecho, BoxLayout.Y_AXIS));
        panelDerecho.add(registrosPanel);
        panelDerecho.add(pcbPanel);

        codigoPanel.setPreferredSize(new java.awt.Dimension(260, 400));
        panelDerecho.setPreferredSize(new java.awt.Dimension(260, 400));

        setLayout(new BorderLayout());
        add(panelControles, BorderLayout.NORTH);
        add(codigoPanel, BorderLayout.WEST);
        add(panelDerecho, BorderLayout.EAST);
        add(memoriaPanel, BorderLayout.CENTER);
        add(lblMensaje, BorderLayout.SOUTH);
    }

    private void registrarAcciones() {
        btnCargar.addActionListener(e -> onCargar());
        btnSiguiente.addActionListener(e -> onSiguiente());
        btnEjecutarTodo.addActionListener(e -> onEjecutarTodo());
        btnReiniciar.addActionListener(e -> onReiniciar());
    }

    private void onCargar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos ensamblador (*.asm)", "asm"));
        int resultado = chooser.showOpenDialog(this);
        if (resultado != JFileChooser.APPROVE_OPTION) {
            return;
        }
        cargarPrograma(chooser.getSelectedFile());
    }

    /**
     * Lee, valida y carga un archivo .asm, y deja todo listo para ejecutar.
     * Separado de onCargar() para poder probarlo sin depender del diálogo de archivos.
     * @param archivo el archivo .asm a cargar
     */
    void cargarPrograma(File archivo) {
        int tamanoTotal = (Integer) spTotal.getValue();
        int tamanoSO = (Integer) spSO.getValue();

        try {
            List<Instruction> nuevasInstrucciones = Assembler.loadFromFile(archivo);
            Memory nuevaMemoria = new Memory(tamanoTotal, tamanoSO);
            int nuevaBase = nuevaMemoria.loadProgram(nuevasInstrucciones);

            // Solo si todo salió bien reemplazamos el estado actual
            this.instrucciones = nuevasInstrucciones;
            this.memoria = nuevaMemoria;
            this.direccionBase = nuevaBase;
            this.archivoActual = archivo;
            this.registros = new Registers();
            this.registros.setPc(direccionBase);
            this.pcb = new PCB(1, archivo.getName(), direccionBase, memoria.getUserEnd(), instrucciones.size());
            this.cpu = new Cpu(memoria, registros, pcb);

            codigoPanel.cargar(instrucciones);
            codigoPanel.resaltar(0);
            memoriaPanel.actualizar(memoria, direccionBase);
            registrosPanel.actualizar(registros);
            pcbPanel.actualizar(pcb);

            lblMensaje.setText("Programa cargado: " + archivo.getName() + " (" + instrucciones.size() + " instrucciones).");
        } catch (IOException e) {
            mostrarError("No se pudo leer el archivo:\n" + e.getMessage());
        } catch (AssemblyException e) {
            mostrarError(mensajeDeErrores(e));
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarError(e.getMessage());
        }

        actualizarBotones();
    }

    private String mensajeDeErrores(AssemblyException e) {
        StringBuilder sb = new StringBuilder("El archivo tiene errores de formato:\n");
        for (String error : e.getErrores()) {
            sb.append(" - ").append(error).append("\n");
        }
        return sb.toString();
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        lblMensaje.setText("No se pudo cargar el programa.");
    }

    private void onSiguiente() {
        if (cpu == null || cpu.isTerminado()) {
            return;
        }
        int indiceEjecutado = (registros.getPc() - direccionBase) + 1;
        Instruction ejecutada = cpu.step();

        refrescarPaneles();

        if (ejecutada != null) {
            codigoPanel.resaltar(indiceEjecutado);
        }

        if (cpu.isTerminado()) {
            lblMensaje.setText("Programa terminado.");
        } else {
            lblMensaje.setText("Se ejecutó: " + ejecutada.getSourceLine());
        }

        actualizarBotones();
    }

    private void onEjecutarTodo() {
        if (cpu == null || cpu.isTerminado()) {
            return;
        }
        int pasos = cpu.runAll();
        refrescarPaneles();
        codigoPanel.resaltar(instrucciones.size());
        lblMensaje.setText("Se ejecutaron " + pasos + " instrucciones. Programa terminado.");
        actualizarBotones();
    }

    private void onReiniciar() {
        if (archivoActual == null) {
            return;
        }
        cargarPrograma(archivoActual); // vuelve a cargar el mismo archivo desde cero
    }

    private void refrescarPaneles() {
        registrosPanel.actualizar(registros);
        pcbPanel.actualizar(pcb);
        memoriaPanel.actualizar(memoria, registros.getPc());
    }

    private void actualizarBotones() {
        boolean hayPrograma = cpu != null;
        boolean terminado = hayPrograma && cpu.isTerminado();
        btnSiguiente.setEnabled(hayPrograma && !terminado);
        btnEjecutarTodo.setEnabled(hayPrograma && !terminado);
        btnReiniciar.setEnabled(hayPrograma);
    }
}