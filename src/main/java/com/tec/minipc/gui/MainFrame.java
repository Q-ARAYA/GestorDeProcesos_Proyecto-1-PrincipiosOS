package com.tec.minipc.gui;

import com.tec.minipc.core.Assembler;
import com.tec.minipc.core.AssemblyException;
import com.tec.minipc.core.PCB;
import com.tec.minipc.core.ProcessManager;
import com.tec.minipc.model.Instruction;
import com.tec.minipc.model.Memory;
import com.tec.minipc.model.Registers;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Ventana principal del simulador y coordinación de la ejecución FCFS. */
public class MainFrame extends JFrame {
    private final JButton btnCargar = new JButton("Cargar .asm...");
    private final JSpinner spTotal = new JSpinner(new SpinnerNumberModel(256, Memory.TAMANO_MINIMO, 256, 1));
    private final JSpinner spKernel = new JSpinner(new SpinnerNumberModel(25, 25, 80, 5));
    private final JButton btnSiguiente = new JButton("Siguiente (1 s)");
    private final JButton btnEjecutarTodo = new JButton("Ejecutar todo");
    private final JButton btnReiniciar = new JButton("Reiniciar simulación");
    private final JLabel lblMensaje = new JLabel("Cargue hasta cinco programas .asm.");
    private final CodigoPanel codigoPanel = new CodigoPanel();
    private final RegistrosPanel registrosPanel = new RegistrosPanel();
    private final PcbPanel pcbPanel = new PcbPanel();
    private final MemoriaPanel memoriaPanel = new MemoriaPanel();
    private final TrabajosPanel trabajosPanel = new TrabajosPanel();
    private final DispositivosPanel dispositivosPanel = new DispositivosPanel(this::onEnviarEntrada);

    private Memory memoria;
    private ProcessManager gestor;
    private int tamanoConfigurado;
    private int kernelPorcentajeConfigurado;
    private ProcessManager.Proceso procesoMostrado;

    public MainFrame() {
        super("Mini PC - Gestor de procesos FCFS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1250, 760);
        setLocationRelativeTo(null);
        armarLayout();
        registrarAcciones();
        actualizarBotones();
    }

    private void armarLayout() {
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controles.add(btnCargar);
        controles.add(new JLabel("Memoria total:")); controles.add(spTotal);
        controles.add(new JLabel("Kernel (% de memoria):")); controles.add(spKernel);
        controles.add(btnSiguiente); controles.add(btnEjecutarTodo); controles.add(btnReiniciar);

        JPanel derecha = new JPanel(new BorderLayout(4, 4));
        JPanel datos = new JPanel();
        datos.setLayout(new BoxLayout(datos, BoxLayout.Y_AXIS));
        datos.add(registrosPanel); datos.add(pcbPanel);
        derecha.add(datos, BorderLayout.CENTER);
        derecha.add(trabajosPanel, BorderLayout.SOUTH);
        derecha.setPreferredSize(new Dimension(460, 420));

        codigoPanel.setPreferredSize(new Dimension(300, 420));
        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.add(memoriaPanel, BorderLayout.CENTER);
        dispositivosPanel.setPreferredSize(new Dimension(500, 190));
        centro.add(dispositivosPanel, BorderLayout.SOUTH);
        setLayout(new BorderLayout(4, 4));
        add(controles, BorderLayout.NORTH);
        add(codigoPanel, BorderLayout.WEST);
        add(derecha, BorderLayout.EAST);
        add(centro, BorderLayout.CENTER);
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
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos ensamblador (*.asm)", "asm"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        cargarProgramas(chooser.getSelectedFiles());
    }

    private void cargarProgramas(File[] archivos) {
        int total = (Integer) spTotal.getValue();
        int porcentajeKernel = (Integer) spKernel.getValue();
        if (gestor != null && (total != tamanoConfigurado || porcentajeKernel != kernelPorcentajeConfigurado)) {
            mostrarError("La configuración de memoria no puede cambiar mientras hay una simulación activa. Reinicie primero.");
            return;
        }
        List<File> validos = new ArrayList<>();
        List<List<Instruction>> programas = new ArrayList<>();
        StringBuilder errores = new StringBuilder();
        for (File archivo : archivos) {
            try {
                validos.add(archivo);
                programas.add(Assembler.loadFromFile(archivo));
            } catch (IOException | AssemblyException | RuntimeException ex) {
                validos.remove(archivo);
                errores.append(archivo.getName()).append(": ").append(ex.getMessage()).append("\n\n");
            }
        }
        if (validos.isEmpty()) {
            if (errores.length() > 0) mostrarError(errores.toString());
            return;
        }
        if (gestor == null) {
            try {
                memoria = new Memory(total, calcularTamanoKernel(total, porcentajeKernel));
                gestor = new ProcessManager(memoria);
                tamanoConfigurado = total;
                kernelPorcentajeConfigurado = porcentajeKernel;
            } catch (IllegalArgumentException ex) { mostrarError(ex.getMessage()); return; }
        }
        int admitidos = 0;
        for (int i = 0; i < validos.size(); i++) {
            try {
                gestor.admitir(validos.get(i).getName(), programas.get(i));
                admitidos++;
            } catch (IllegalStateException ex) {
                errores.append(validos.get(i).getName()).append(": ").append(ex.getMessage()).append("\n\n");
            }
        }
        if (procesoMostrado == null && gestor.getActual() == null) {
            // Se selecciona el primero de la cola para inicializar los paneles; el despacho ocurre al avanzar.
            procesoMostrado = gestor.getProcesos().isEmpty() ? null : gestor.getProcesos().get(0);
        }
        refrescarTodo(true);
        actualizarDispositivos();
        lblMensaje.setText("Programas admitidos: " + admitidos + ". Cola FCFS: " + gestor.getProcesos().size()
                + "/" + ProcessManager.MAX_PROCESOS + ". Kernel: " + kernelPorcentajeConfigurado + "% ("
                + memoria.getOsSize() + "/" + memoria.getTotalSize() + " celdas).");
        if (errores.length() > 0) JOptionPane.showMessageDialog(this, errores.toString(), "Carga parcial", JOptionPane.WARNING_MESSAGE);
        actualizarBotones();
    }

    private void onSiguiente() {
        if (gestor == null || !gestor.hayPendientes()) return;
        ProcessManager.Proceso anterior = gestor.getActual();
        if (anterior == null) anterior = gestor.getProcesos().stream()
                .filter(p -> p.getPcb().getEstado() == PCB.Estado.LISTO).findFirst().orElse(null);
        Instruction ejecutada = gestor.step();
        ProcessManager.Proceso actual = gestor.getActual();
        boolean cambio = actual != anterior && actual != null;
        if (cambio) procesoMostrado = actual;
        else if (actual != null) procesoMostrado = actual;
        refrescarTodo(cambio);
        actualizarDispositivos();
        if (ejecutada != null) lblMensaje.setText("CPU: " + (anterior == null ? "" : anterior.getPcb().getNombrePrograma())
                + " · " + ejecutada.getSourceLine() + " · "
                + (anterior == null ? 0 : anterior.getPcb().getTiempoCpuSegundos()) + " s de CPU.");
        else if (gestor.hayEsperandoEntrada()) lblMensaje.setText("Proceso detenido en INT 09H; escriba un valor en el teclado simulado.");
        if (!gestor.hayPendientes()) lblMensaje.setText("Todos los procesos finalizaron.");
        actualizarBotones();
    }

    private void onEjecutarTodo() {
        if (gestor == null || !gestor.hayPendientes()) return;
        int pasos;
        try {
            pasos = gestor.runAll();
        } catch (IllegalStateException ex) {
            refrescarTodo(false);
            actualizarDispositivos();
            mostrarError(ex.getMessage());
            actualizarBotones();
            return;
        }
        procesoMostrado = ultimoProceso();
        refrescarTodo(true);
        actualizarDispositivos();
        if (gestor.hayEsperandoEntrada()) {
            lblMensaje.setText("Ejecución pausada: " + gestor.getCantidadEsperandoEntrada() + " proceso(s) esperan INT 09H.");
        } else {
            lblMensaje.setText("Ejecución automática: " + pasos + " segundos de CPU procesados.");
        }
        actualizarBotones();
    }

    private ProcessManager.Proceso ultimoProceso() {
        List<ProcessManager.Proceso> lista = gestor.getProcesos();
        return lista.isEmpty() ? null : lista.get(lista.size() - 1);
    }

    private void onReiniciar() {
        gestor = null; memoria = null; procesoMostrado = null;
        codigoPanel.cargar(new ArrayList<>());
        registrosPanel.actualizar(new Registers());
        pcbPanel.limpiar();
        trabajosPanel.actualizar(new ArrayList<>());
        dispositivosPanel.limpiar();
        try {
            int total = (Integer) spTotal.getValue();
            int porcentajeKernel = (Integer) spKernel.getValue();
            memoria = new Memory(total, calcularTamanoKernel(total, porcentajeKernel));
            memoriaPanel.actualizar(memoria, -1);
        } catch (IllegalArgumentException ex) {
            memoriaPanel.actualizar(new Memory(256, 64), -1);
        }
        lblMensaje.setText("Simulación reiniciada. Cargue programas .asm.");
        actualizarBotones();
    }

    private void refrescarTodo(boolean mostrarCodigo) {
        if (gestor == null || memoria == null) return;
        ProcessManager.Proceso actual = gestor.getActual();
        if (actual != null) procesoMostrado = actual;
        if (procesoMostrado != null) {
            if (mostrarCodigo) codigoPanel.cargar(procesoMostrado.getInstrucciones());
            Registers r = procesoMostrado.getRegistros();
            registrosPanel.actualizar(r);
            pcbPanel.actualizar(procesoMostrado.getPcb());
            memoriaPanel.actualizar(memoria, r.getPc());
            int base = procesoMostrado.getBase();
            int fin = base + procesoMostrado.getInstrucciones().size();
            int pc = r.getPc();
            codigoPanel.resaltar(base >= 0 && pc >= base && pc < fin ? pc - base : -1);
        } else {
            memoriaPanel.actualizar(memoria, -1);
        }
        trabajosPanel.actualizar(gestor.getProcesos());
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        lblMensaje.setText("No se pudo completar la operación.");
    }

    private void onEnviarEntrada(int valor) {
        if (gestor == null) {
            mostrarError("Cargue un programa que utilice INT 09H antes de enviar datos.");
            return;
        }
        gestor.proveerEntrada(valor);
        actualizarDispositivos();
        lblMensaje.setText(gestor.hayEsperandoEntrada()
                ? "Entrada enviada al proceso en espera. Quedan " + gestor.getCantidadEsperandoEntrada() + "."
                : "Entrada " + valor + " enviada al búfer del teclado simulado.");
        actualizarBotones();
    }

    private void actualizarDispositivos() {
        if (gestor == null) {
            dispositivosPanel.actualizar(new ArrayList<>(), 0);
            dispositivosPanel.actualizarPila(null, "");
        } else {
            dispositivosPanel.actualizar(gestor.getSalidaPantalla(), gestor.getCantidadEsperandoEntrada());
            dispositivosPanel.actualizarPila(procesoMostrado == null ? null : procesoMostrado.getPila(),
                    procesoMostrado == null ? "" : procesoMostrado.getPcb().getMensajeError());
        }
    }

    private int calcularTamanoKernel(int memoriaTotal, int porcentaje) {
        return (int) Math.ceil(memoriaTotal * porcentaje / 100.0);
    }

    private void actualizarBotones() {
        boolean pendientes = gestor != null && gestor.puedeAvanzar();
        btnSiguiente.setEnabled(pendientes);
        btnEjecutarTodo.setEnabled(pendientes);
        btnReiniciar.setEnabled(gestor != null);
    }
}
