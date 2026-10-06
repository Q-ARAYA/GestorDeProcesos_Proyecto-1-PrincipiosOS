package com.tec.minipc.gui;

import com.tec.minipc.core.Assembler;
import com.tec.minipc.core.AssemblyException;
import com.tec.minipc.core.PCB;
import com.tec.minipc.core.ProcessManager;
import com.tec.minipc.core.SimulatorConfig;
import com.tec.minipc.model.Instruction;
import com.tec.minipc.model.Memory;
import com.tec.minipc.model.Registers;
import com.tec.minipc.model.SecondaryStorage;
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
    private final JSpinner spTotal = new JSpinner(new SpinnerNumberModel(256, Memory.TAMANO_MINIMO, 4096, 1));
    private final JSpinner spKernel = new JSpinner(new SpinnerNumberModel(25, 25, 80, 5));
    private final JSpinner spDisco = new JSpinner(new SpinnerNumberModel(SecondaryStorage.TAMANO_POR_DEFECTO, 128, 16384, 1));
    private final JSpinner spVirtual = new JSpinner(new SpinnerNumberModel(SecondaryStorage.MEMORIA_VIRTUAL_POR_DEFECTO, 0, 16374, 1));
    private final JButton btnGuardarConfig = new JButton("Guardar configuración");
    private final JButton btnSiguiente = new JButton("Siguiente (1 s)");
    private final JButton btnEjecutarTodo = new JButton("Ejecutar todo");
    private final JButton btnReiniciar = new JButton("Reiniciar simulación");
    private final JLabel lblMensaje = new JLabel("Cargue hasta cinco programas .asm.");
    private final CodigoPanel codigoPanel = new CodigoPanel();
    private final RegistrosPanel registrosPanel = new RegistrosPanel();
    private final PcbPanel pcbPanel = new PcbPanel();
    private final MemoriaPanel memoriaPanel = new MemoriaPanel();
    private final TrabajosPanel trabajosPanel = new TrabajosPanel();
    private final EstadisticasPanel estadisticasPanel = new EstadisticasPanel();
    private final SeguridadPanel seguridadPanel = new SeguridadPanel();
    private final DispositivosPanel dispositivosPanel = new DispositivosPanel(this::onEnviarEntrada);
    private final AlmacenamientoPanel almacenamientoPanel = new AlmacenamientoPanel();

    private Memory memoria;
    private SecondaryStorage almacenamiento;
    private ProcessManager gestor;
    private int tamanoConfigurado;
    private int kernelPorcentajeConfigurado;
    private int discoConfigurado;
    private int memoriaVirtualConfigurada;
    private ProcessManager.Proceso procesoMostrado;

    public MainFrame() {
        super("Mini PC - Gestor de procesos FCFS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1250, 760);
        setLocationRelativeTo(null);
        cargarConfiguracionInicial();
        armarLayout();
        registrarAcciones();
        actualizarBotones();
    }

    private void armarLayout() {
        JPanel controles = new JPanel(new GridLayout(2, 1));
        JPanel configuracion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        configuracion.add(btnCargar);
        configuracion.add(new JLabel("Memoria total:")); configuracion.add(spTotal);
        configuracion.add(new JLabel("Kernel (% de memoria):")); configuracion.add(spKernel);
        configuracion.add(new JLabel("Disco:")); configuracion.add(spDisco);
        configuracion.add(new JLabel("Memoria virtual (páginas):")); configuracion.add(spVirtual);
        configuracion.add(btnGuardarConfig);
        JPanel ejecucion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ejecucion.add(btnSiguiente); ejecucion.add(btnEjecutarTodo); ejecucion.add(btnReiniciar);
        controles.add(configuracion);
        controles.add(ejecucion);

        JPanel derecha = new JPanel(new BorderLayout(4, 4));
        JPanel datos = new JPanel();
        datos.setLayout(new BoxLayout(datos, BoxLayout.Y_AXIS));
        datos.add(registrosPanel); datos.add(pcbPanel);
        JTabbedPane monitoreo = new JTabbedPane();
        monitoreo.addTab("CPU y BCP", new JScrollPane(datos));
        monitoreo.addTab("Lista de trabajos", trabajosPanel);
        monitoreo.addTab("Estadísticas", estadisticasPanel);
        monitoreo.addTab("Seguridad", seguridadPanel);
        derecha.add(monitoreo, BorderLayout.CENTER);
        derecha.setPreferredSize(new Dimension(460, 420));

        codigoPanel.setPreferredSize(new Dimension(300, 420));
        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.add(memoriaPanel, BorderLayout.CENTER);
        dispositivosPanel.setPreferredSize(new Dimension(500, 190));
        JTabbedPane recursos = new JTabbedPane();
        recursos.addTab("Dispositivos E/S", dispositivosPanel);
        recursos.addTab("Disco y memoria virtual", almacenamientoPanel);
        recursos.setPreferredSize(new Dimension(500, 220));
        centro.add(recursos, BorderLayout.SOUTH);
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
        btnGuardarConfig.addActionListener(e -> guardarConfiguracionDesdeInterfaz());
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
        int tamanoDisco = (Integer) spDisco.getValue();
        int tamanoVirtual = (Integer) spVirtual.getValue();
        if (gestor != null && (total != tamanoConfigurado || porcentajeKernel != kernelPorcentajeConfigurado
                || tamanoDisco != discoConfigurado || tamanoVirtual != memoriaVirtualConfigurada)) {
            mostrarError("La configuración no puede cambiar mientras hay una simulación activa. Reinicie primero.");
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
                almacenamiento = new SecondaryStorage(tamanoDisco, tamanoVirtual);
                gestor = new ProcessManager(memoria, almacenamiento);
                tamanoConfigurado = total;
                kernelPorcentajeConfigurado = porcentajeKernel;
                discoConfigurado = tamanoDisco;
                memoriaVirtualConfigurada = tamanoVirtual;
                guardarConfiguracionSilenciosa(total, porcentajeKernel, tamanoDisco, tamanoVirtual);
            } catch (IllegalArgumentException ex) { mostrarError(ex.getMessage()); return; }
        }
        int admitidos = 0;
        for (int i = 0; i < validos.size(); i++) {
            try {
                if (!almacenamiento.puedeGuardar(programas.get(i))) {
                    throw new IllegalStateException("No hay espacio disponible en el disco secundario para guardar el programa.");
                }
                ProcessManager.Proceso proceso = gestor.admitir(validos.get(i).getName(), programas.get(i));
                almacenamiento.guardar(proceso.getPcb().getPid(), validos.get(i).getName(), programas.get(i));
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
        gestor = null; memoria = null; almacenamiento = null; procesoMostrado = null;
        codigoPanel.cargar(new ArrayList<>());
        registrosPanel.actualizar(new Registers());
        pcbPanel.limpiar();
        trabajosPanel.actualizar(new ArrayList<>());
        estadisticasPanel.limpiar();
        seguridadPanel.actualizar(List.of(), List.of(), null);
        dispositivosPanel.limpiar();
        try {
            int total = (Integer) spTotal.getValue();
            int porcentajeKernel = (Integer) spKernel.getValue();
            int tamanoDisco = (Integer) spDisco.getValue();
            int tamanoVirtual = (Integer) spVirtual.getValue();
            memoria = new Memory(total, calcularTamanoKernel(total, porcentajeKernel));
            almacenamiento = new SecondaryStorage(tamanoDisco, tamanoVirtual);
            tamanoConfigurado = total;
            kernelPorcentajeConfigurado = porcentajeKernel;
            discoConfigurado = tamanoDisco;
            memoriaVirtualConfigurada = tamanoVirtual;
            almacenamientoPanel.actualizar(almacenamiento);
            guardarConfiguracionSilenciosa(total, porcentajeKernel, tamanoDisco, tamanoVirtual);
            memoriaPanel.actualizar(memoria, -1);
        } catch (IllegalArgumentException ex) {
            almacenamientoPanel.actualizar(null);
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
        almacenamientoPanel.actualizar(almacenamiento, gestor.getSistemaArchivos());
        trabajosPanel.actualizar(gestor.getProcesos());
        estadisticasPanel.actualizar(gestor.getProcesos());
        seguridadPanel.actualizar(gestor.getProcesos(), gestor.getEventos(),
                gestor.getSistemaArchivos() == null ? null : gestor.getSistemaArchivos().getEventos());
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

    private void cargarConfiguracionInicial() {
        try {
            SimulatorConfig config = SimulatorConfig.cargar();
            spTotal.setValue(config.getMemoriaPrincipal());
            spKernel.setValue(config.getKernelPorcentaje());
            spDisco.setValue(config.getDiscoSecundario());
            spVirtual.setValue(config.getMemoriaVirtual());
        } catch (IOException | IllegalArgumentException ex) {
            lblMensaje.setText("Configuración inválida: " + ex.getMessage() + ". Se usarán los valores predeterminados.");
        }
    }

    private void guardarConfiguracionDesdeInterfaz() {
        int total = (Integer) spTotal.getValue();
        int kernel = (Integer) spKernel.getValue();
        int disco = (Integer) spDisco.getValue();
        int virtual = (Integer) spVirtual.getValue();
        try {
            SimulatorConfig config = SimulatorConfig.cargar();
            config.setValores(total, kernel, disco, virtual);
            config.guardar();
            lblMensaje.setText("Configuración guardada en simulador.properties.");
        } catch (IOException | IllegalArgumentException ex) {
            mostrarError("No se pudo guardar la configuración: " + ex.getMessage());
        }
    }

    private void guardarConfiguracionSilenciosa(int total, int kernel, int disco, int virtual) {
        try {
            SimulatorConfig config = SimulatorConfig.cargar();
            config.setValores(total, kernel, disco, virtual);
            config.guardar();
        } catch (IOException | IllegalArgumentException ex) {
            lblMensaje.setText("Simulación iniciada; no se pudo persistir la configuración: " + ex.getMessage());
        }
    }

    private void actualizarBotones() {
        boolean pendientes = gestor != null && gestor.puedeAvanzar();
        btnSiguiente.setEnabled(pendientes);
        btnEjecutarTodo.setEnabled(pendientes);
        btnReiniciar.setEnabled(gestor != null);
    }
}
