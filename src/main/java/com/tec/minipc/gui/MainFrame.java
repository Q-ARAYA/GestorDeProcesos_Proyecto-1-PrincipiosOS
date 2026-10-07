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
import javax.swing.border.EmptyBorder;
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
    private final JSpinner spVirtual = new JSpinner(new SpinnerNumberModel(SecondaryStorage.MEMORIA_VIRTUAL_POR_DEFECTO, 0, 16354, 1));
    private final JButton btnGuardarConfig = new JButton("Guardar configuración");
    private final JButton btnSiguiente = new JButton("Siguiente (1 s)");
    private final JButton btnEjecutarTodo = new JButton("Ejecutar todo");
    private final JButton btnSuspender = new JButton("Suspender actual");
    private final JButton btnReanudar = new JButton("Reanudar suspendido");
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

    /** Inicializa MainFrame con los recursos y valores recibidos. */
    public MainFrame() {
        super("Mini PC - Gestor de procesos FCFS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle pantalla = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(1100, pantalla.width), Math.min(700, pantalla.height)));
        setSize(Math.min(1480, pantalla.width), Math.min(900, pantalla.height));
        setLocationRelativeTo(null);
        cargarConfiguracionInicial();
        armarLayout();
        registrarAcciones();
        actualizarBotones();
    }

    /** Construye y distribuye los paneles y controles de la ventana principal. */
    private void armarLayout() {
        UIManager.put("TabbedPane.selected", new Color(190, 228, 246));
        UIManager.put("TabbedPane.background", new Color(229, 245, 252));
        UIManager.put("TabbedPane.foreground", new Color(18, 76, 125));
        Color fondo = new Color(229, 245, 252);
        Color tinta = new Color(13, 59, 96);
        getContentPane().setBackground(fondo);

        JPanel encabezado = new JPanel(new BorderLayout(0, 6));
        encabezado.setBackground(new Color(242, 250, 255));
        encabezado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(188, 224, 241)),
                new EmptyBorder(6, 11, 6, 11)));
        JPanel marca = new JPanel(new GridLayout(2, 1, 0, 1));
        marca.setOpaque(false);
        JLabel titulo = new JLabel("Mini PC");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(tinta);
        JLabel subtitulo = new JLabel("Simulador de sistema operativo · FCFS");
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitulo.setForeground(new Color(43, 119, 159));
        marca.add(titulo);
        marca.add(subtitulo);

        JPanel configuracion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        configuracion.setOpaque(false);
        btnCargar.setText("Cargar .asm");
        btnGuardarConfig.setText("Guardar");
        configuracion.add(btnCargar);
        agregarConfiguracion(configuracion, "RAM", spTotal, "Memoria principal total");
        agregarConfiguracion(configuracion, "Kernel %", spKernel, "Porcentaje de memoria reservado al kernel");
        agregarConfiguracion(configuracion, "Disco", spDisco, "Capacidad del disco secundario");
        agregarConfiguracion(configuracion, "Virtual", spVirtual, "Páginas de memoria virtual disponibles");
        configuracion.add(btnGuardarConfig);

        JPanel franjaSuperior = new JPanel(new BorderLayout(12, 0));
        franjaSuperior.setOpaque(false);
        franjaSuperior.add(marca, BorderLayout.WEST);
        franjaSuperior.add(configuracion, BorderLayout.CENTER);
        JPanel ejecucion = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ejecucion.setOpaque(false);
        ejecucion.add(btnSiguiente);
        ejecucion.add(btnEjecutarTodo);
        ejecucion.add(btnSuspender);
        ejecucion.add(btnReanudar);
        ejecucion.add(btnReiniciar);
        encabezado.add(franjaSuperior, BorderLayout.CENTER);
        encabezado.add(ejecucion, BorderLayout.SOUTH);

        JPanel columnaRecursos = new JPanel(new BorderLayout());
        columnaRecursos.setMinimumSize(new Dimension(0, 0));
        JSplitPane divisionRecursos = crearDivision(JSplitPane.VERTICAL_SPLIT,
                seccion("MEMORIA PRINCIPAL", memoriaPanel, new Color(25, 112, 173)),
                seccion("PROGRAMA .ASM", codigoPanel, new Color(31, 147, 190)), 0.53);
        columnaRecursos.add(divisionRecursos, BorderLayout.CENTER);

        JPanel columnaSistema = new JPanel(new BorderLayout());
        columnaSistema.setMinimumSize(new Dimension(0, 0));
        JSplitPane divisionSistema = crearDivision(JSplitPane.VERTICAL_SPLIT,
                seccion("KERNEL · BLOQUES DE CONTROL (BCP)", pcbPanel, new Color(16, 82, 136)),
                seccion("CPU · REGISTROS", new JScrollPane(registrosPanel), new Color(42, 131, 177)), 0.65);
        columnaSistema.add(divisionSistema, BorderLayout.CENTER);

        JSplitPane tablero = crearDivision(JSplitPane.HORIZONTAL_SPLIT,
                columnaRecursos, columnaSistema, 0.55);
        JTabbedPane secundarios = new JTabbedPane();
        secundarios.addTab("Dispositivos E/S", dispositivosPanel);
        secundarios.addTab("Lista de trabajos", trabajosPanel);
        secundarios.addTab("Disco y memoria virtual", almacenamientoPanel);
        secundarios.addTab("Estadísticas", estadisticasPanel);
        secundarios.addTab("Seguridad", seguridadPanel);
        JSplitPane espacioTrabajo = crearDivision(JSplitPane.VERTICAL_SPLIT, tablero, secundarios, 0.73);
        espacioTrabajo.setResizeWeight(0.73);

        JPanel barraEstado = new JPanel(new BorderLayout());
        barraEstado.setBackground(new Color(242, 250, 255));
        barraEstado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(188, 224, 241)),
                new EmptyBorder(5, 12, 5, 12)));
        lblMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMensaje.setForeground(new Color(24, 86, 128));
        barraEstado.add(lblMensaje, BorderLayout.CENTER);

        setLayout(new BorderLayout(0, 5));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(6, 7, 0, 7));
        add(encabezado, BorderLayout.NORTH);
        add(espacioTrabajo, BorderLayout.CENTER);
        add(barraEstado, BorderLayout.SOUTH);
        estilizarComponentes(getContentPane());
        Color azul = new Color(31, 112, 173);
        Color celeste = new Color(54, 151, 193);
        estilizarBoton(btnCargar, azul);
        estilizarBoton(btnSiguiente, azul);
        estilizarBoton(btnEjecutarTodo, new Color(24, 126, 177));
        estilizarBoton(btnSuspender, celeste);
        estilizarBoton(btnReanudar, new Color(38, 137, 180));
        estilizarBoton(btnReiniciar, new Color(18, 76, 125));
        estilizarBoton(btnGuardarConfig, new Color(69, 159, 196));
        SwingUtilities.invokeLater(() -> {
            divisionRecursos.setDividerLocation(0.53);
            divisionSistema.setDividerLocation(0.65);
            tablero.setDividerLocation(0.55);
            espacioTrabajo.setDividerLocation(0.74);
        });
    }

    /**
     * Añade al panel de configuración una etiqueta, un control numérico y su ayuda.
     * @param panel panel al que se agrega la configuración
     * @param texto texto mostrado
     * @param spinner control numérico de la opción
     * @param ayuda texto de ayuda
     */
    private void agregarConfiguracion(JPanel panel, String texto, JSpinner spinner, String ayuda) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(new Font("Segoe UI", Font.BOLD, 11));
        etiqueta.setForeground(new Color(24, 86, 128));
        etiqueta.setToolTipText(ayuda);
        spinner.setPreferredSize(new Dimension(texto.equals("Kernel %") ? 62 : 68, 27));
        spinner.setToolTipText(ayuda);
        panel.add(etiqueta);
        panel.add(spinner);
    }

    /**
     * Construye un divisor redimensionable con la proporción inicial indicada.
     * @param orientacion orientación del divisor
     * @param superior componente superior
     * @param inferior componente inferior
     * @param proporcion proporción inicial
     * @return divisor que contiene ambos componentes.
     */
    private JSplitPane crearDivision(int orientacion, Component superior, Component inferior, double proporcion) {
        JSplitPane division = new JSplitPane(orientacion, superior, inferior);
        division.setResizeWeight(proporcion);
        division.setContinuousLayout(true);
        division.setBorder(BorderFactory.createEmptyBorder());
        division.setDividerSize(8);
        division.setOneTouchExpandable(true);
        return division;
    }

    /**
     * Crea un panel de sección con título, contenido y color de énfasis.
     * @param titulo texto que encabeza la sección
     * @param contenido componente visual que ocupará el centro de la sección
     * @param color color aplicado
     * @return panel compuesto con encabezado y contenido.
     */
    private JPanel seccion(String titulo, Component contenido, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setMinimumSize(new Dimension(0, 0));
        JLabel encabezado = new JLabel("  " + titulo);
        encabezado.setOpaque(true);
        encabezado.setBackground(color);
        encabezado.setForeground(Color.WHITE);
        encabezado.setFont(new Font("Segoe UI", Font.BOLD, 12));
        encabezado.setBorder(new EmptyBorder(7, 5, 7, 5));
        panel.add(encabezado, BorderLayout.NORTH);
        if (contenido instanceof JComponent jComponent) {
            jComponent.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }
        panel.add(contenido, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createLineBorder(new Color(185, 221, 239)));
        return panel;
    }

    /**
     * Aplica formato a boton.
     * @param boton botón cuyo estilo se configura
     * @param color color aplicado
     */
    private void estilizarBoton(JButton boton, Color color) {
        boton.setBackground(color);
        boton.setForeground(Color.WHITE);
        boton.setOpaque(true);
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(18, 76, 125)), new EmptyBorder(5, 9, 5, 9)));
    }

    /**
     * Aplica formato a componentes.
     * @param componente componente visual que se procesa
     */
    private void estilizarComponentes(Component componente) {
        if (componente instanceof JTable tabla) {
            tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            tabla.setRowHeight(24);
            tabla.setShowVerticalLines(false);
            tabla.setGridColor(new Color(205, 231, 244));
            tabla.setSelectionBackground(new Color(190, 228, 246));
            tabla.setSelectionForeground(new Color(13, 59, 96));
            tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            tabla.getTableHeader().setBackground(new Color(218, 241, 251));
            tabla.getTableHeader().setForeground(new Color(18, 76, 125));
        } else if (componente instanceof JButton boton) {
            boton.setBackground(new Color(54, 151, 193));
            boton.setForeground(Color.WHITE);
            boton.setOpaque(true);
            boton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            boton.setFocusPainted(false);
            boton.setMargin(new Insets(7, 11, 7, 11));
        } else if (componente instanceof JLabel etiqueta && !etiqueta.isOpaque()) {
            etiqueta.setForeground(new Color(24, 86, 128));
        } else if (componente instanceof JSpinner spinner) {
            spinner.setBackground(Color.WHITE);
            spinner.setForeground(new Color(18, 76, 125));
        } else if (componente instanceof JTabbedPane pestanas) {
            pestanas.setFont(new Font("Segoe UI", Font.BOLD, 12));
            pestanas.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
            pestanas.setBackground(new Color(229, 245, 252));
            pestanas.setForeground(new Color(18, 76, 125));
        }
        if (componente instanceof Container contenedor) {
            for (Component hijo : contenedor.getComponents()) estilizarComponentes(hijo);
        }
    }

    /** Asocia los controles de la interfaz con sus manejadores de eventos. */
    private void registrarAcciones() {
        btnCargar.addActionListener(e -> onCargar());
        btnSiguiente.addActionListener(e -> onSiguiente());
        btnEjecutarTodo.addActionListener(e -> onEjecutarTodo());
        btnSuspender.addActionListener(e -> onSuspender());
        btnReanudar.addActionListener(e -> onReanudar());
        btnReiniciar.addActionListener(e -> onReiniciar());
        btnGuardarConfig.addActionListener(e -> guardarConfiguracionDesdeInterfaz());
    }

    /** Atiende la acción de interfaz relacionada con cargar. */
    private void onCargar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos ensamblador (*.asm)", "asm"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        cargarProgramas(chooser.getSelectedFiles());
    }

    /**
     * Lee los archivos seleccionados y los admite individualmente en el gestor de procesos.
     * @param archivos colección de archivos
     */
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

    /** Atiende la acción de interfaz relacionada con siguiente. */
    private void onSiguiente() {
        if (gestor == null || !gestor.puedeAvanzar()) return;
        int entradasEsperandoAntes = gestor.getCantidadEsperandoEntrada();
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
        solicitarEntradasSiNecesario(entradasEsperandoAntes);
        avisarSiLaCpuQuedoSinTrabajo();
    }

    /** Atiende la acción de interfaz relacionada con ejecutar todo. */
    private void onEjecutarTodo() {
        if (gestor == null || !gestor.puedeAvanzar()) return;
        int entradasEsperandoAntes = gestor.getCantidadEsperandoEntrada();
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
        if (gestor.haySuspendidos()) {
            lblMensaje.setText("Ejecución pausada. Hay " + gestor.getCantidadSuspendidos()
                    + " proceso(s) suspendido(s); reanude uno para continuar.");
        } else if (gestor.hayEsperandoEntrada()) {
            lblMensaje.setText("Ejecución pausada: " + gestor.getCantidadEsperandoEntrada() + " proceso(s) esperan INT 09H.");
        } else {
            lblMensaje.setText("Ejecución automática: " + pasos + " segundos de CPU procesados.");
        }
        actualizarBotones();
        solicitarEntradasSiNecesario(entradasEsperandoAntes);
        avisarSiLaCpuQuedoSinTrabajo();
    }

    /** Abre un cuadro solo cuando aparecen entradas nuevas o la CPU queda detenida por ellas. */
    private void solicitarEntradasSiNecesario(int entradasEsperandoAntes) {
        if (gestor == null) return;
        int actuales = gestor.getCantidadEsperandoEntrada();
        int nuevas = Math.max(0, actuales - entradasEsperandoAntes);
        int solicitudes = Math.max(nuevas, gestor.puedeAvanzar() ? 0 : actuales);
        for (int i = 0; i < solicitudes; i++) {
            if (!solicitarEntradaTeclado()) return;
        }
    }

    /**
     * Muestra el diálogo de entrada y valida el valor que se entregará al dispositivo simulado.
     * @return resultado generado por la operación.
     */
    private boolean solicitarEntradaTeclado() {
        ProcessManager.Proceso esperando = gestor.getProcesos().stream()
                .filter(p -> p.getPcb().getEstado() == PCB.Estado.ESPERA
                        && p.getPcb().getEstadoDescripcion().contains("Teclado (INT 09H)"))
                .findFirst().orElse(null);
        if (esperando == null) return false;

        String titulo = "Entrada requerida · teclado simulado";
        while (true) {
            String texto = JOptionPane.showInputDialog(this,
                    "El proceso PID " + esperando.getPcb().getPid() + " ("
                    + esperando.getPcb().getNombrePrograma() + ") llegó a INT 09H y espera una entrada.\n"
                    + "Escriba un número entero entre 0 y 255 para continuar.",
                    titulo, JOptionPane.QUESTION_MESSAGE);
            if (texto == null) {
                lblMensaje.setText("Entrada pendiente. Escríbala en la pestaña Dispositivos E/S cuando quiera continuar.");
                return false;
            }
            try {
                int valor = Integer.parseInt(texto.trim());
                if (valor < 0 || valor > 255) throw new NumberFormatException();
                gestor.proveerEntrada(valor);
                refrescarTodo(false);
                actualizarDispositivos();
                lblMensaje.setText("Entrada " + valor + " enviada al PID " + esperando.getPcb().getPid()
                        + ". Pulse Siguiente o Ejecutar todo para continuar.");
                actualizarBotones();
                return true;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Ingrese un número entero entre 0 y 255.", titulo, JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /** Realiza la operación avisar si la CPU quedo sin trabajo en MainFrame. */
    private void avisarSiLaCpuQuedoSinTrabajo() {
        if (gestor == null || !gestor.hayPendientes() || gestor.puedeAvanzar()
                || gestor.hayEsperandoEntrada() || !gestor.haySuspendidos()) return;
        JOptionPane.showMessageDialog(this,
                "No quedan procesos listos para usar la CPU. Hay " + gestor.getCantidadSuspendidos()
                + " proceso(s) suspendido(s). Use «Reanudar suspendido» para seguir.",
                "Simulación en pausa", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Devuelve el último proceso admitido, o null cuando no hay procesos.
     * @return valor, objeto o colección descrita en el resumen del método.
     */
    private ProcessManager.Proceso ultimoProceso() {
        List<ProcessManager.Proceso> lista = gestor.getProcesos();
        return lista.isEmpty() ? null : lista.get(lista.size() - 1);
    }

    /** Atiende la acción de interfaz relacionada con reiniciar. */
    private void onReiniciar() {
        gestor = null; memoria = null; almacenamiento = null; procesoMostrado = null;
        codigoPanel.cargar(new ArrayList<>());
        registrosPanel.actualizar(new Registers());
        pcbPanel.actualizar(List.of(), null);
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

    /**
     * Sincroniza todo.
     * @param mostrarCodigo indica si se debe recargar el código visible
     */
    private void refrescarTodo(boolean mostrarCodigo) {
        if (gestor == null || memoria == null) return;
        ProcessManager.Proceso actual = gestor.getActual();
        if (actual != null) procesoMostrado = actual;
        if (procesoMostrado != null) {
            if (mostrarCodigo) codigoPanel.cargar(procesoMostrado.getInstrucciones());
            Registers r = procesoMostrado.getRegistros();
            registrosPanel.actualizar(r);
            memoriaPanel.actualizar(memoria, r.getPc());
            int base = procesoMostrado.getBase();
            int fin = base + procesoMostrado.getInstrucciones().size();
            int pc = r.getPc();
            codigoPanel.resaltar(base >= 0 && pc >= base && pc < fin ? pc - base : -1);
        } else {
            memoriaPanel.actualizar(memoria, -1);
        }
        almacenamientoPanel.actualizar(almacenamiento, gestor.getSistemaArchivos());
        pcbPanel.actualizar(gestor.getProcesos(), memoria);
        trabajosPanel.actualizar(gestor.getProcesos());
        estadisticasPanel.actualizar(gestor.getProcesos());
        seguridadPanel.actualizar(gestor.getProcesos(), gestor.getEventos(),
                gestor.getSistemaArchivos() == null ? null : gestor.getSistemaArchivos().getEventos());
    }

    /**
     * Muestra el mensaje de error en un diálogo para el usuario.
     * @param mensaje mensaje asociado con el estado
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        lblMensaje.setText("No se pudo completar la operación.");
    }

    /**
     * Atiende la acción de interfaz relacionada con enviar entrada.
     * @param valor valor que se asignará o procesará
     */
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

    /** Actualiza dispositivos. */
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

    /**
     * Convierte el porcentaje configurado en cantidad de celdas del kernel.
     * @param memoriaTotal cantidad total de celdas de RAM
     * @param porcentaje porcentaje reservado
     * @return valor calculado o estado consultado.
     */
    private int calcularTamanoKernel(int memoriaTotal, int porcentaje) {
        return (int) Math.ceil(memoriaTotal * porcentaje / 100.0);
    }

    /** Atiende la acción de interfaz relacionada con suspender. */
    private void onSuspender() {
        if (gestor == null) return;
        ProcessManager.Proceso suspendido = gestor.suspenderActual();
        if (suspendido == null) {
            lblMensaje.setText("No hay un proceso ejecutándose para suspender.");
            return;
        }
        refrescarTodo(true);
        actualizarDispositivos();
        lblMensaje.setText("PID " + suspendido.getPcb().getPid() + " suspendido; su contexto y memoria se conservaron.");
        actualizarBotones();
        avisarSiLaCpuQuedoSinTrabajo();
    }

    /** Atiende la acción de interfaz relacionada con reanudar. */
    private void onReanudar() {
        if (gestor == null) return;
        ProcessManager.Proceso reanudado = gestor.reanudarSiguiente();
        if (reanudado == null) {
            lblMensaje.setText("No hay procesos suspendidos para reanudar.");
            return;
        }
        refrescarTodo(true);
        actualizarDispositivos();
        lblMensaje.setText("PID " + reanudado.getPcb().getPid() + " volvió al final de la cola FCFS.");
        actualizarBotones();
        JOptionPane.showMessageDialog(this,
                "El PID " + reanudado.getPcb().getPid() + " volvió al final de la cola FCFS.\n"
                + "Cuando le corresponda usar la CPU, podrá continuar desde su instrucción guardada.",
                "Proceso reanudado", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Carga configuracion inicial. */
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

    /** Guarda configuracion desde interfaz. */
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

    /**
     * Guarda configuracion silenciosa.
     * @param total capacidad total configurada
     * @param kernel porcentaje reservado al kernel
     * @param disco almacenamiento secundario que se presentará
     * @param virtual tamaño de memoria virtual
     */
    private void guardarConfiguracionSilenciosa(int total, int kernel, int disco, int virtual) {
        try {
            SimulatorConfig config = SimulatorConfig.cargar();
            config.setValores(total, kernel, disco, virtual);
            config.guardar();
        } catch (IOException | IllegalArgumentException ex) {
            lblMensaje.setText("Simulación iniciada; no se pudo persistir la configuración: " + ex.getMessage());
        }
    }

    /** Actualiza botones. */
    private void actualizarBotones() {
        boolean pendientes = gestor != null && gestor.puedeAvanzar();
        btnSiguiente.setEnabled(pendientes);
        btnEjecutarTodo.setEnabled(pendientes);
        btnSuspender.setEnabled(gestor != null && gestor.getActual() != null);
        btnReanudar.setEnabled(gestor != null && gestor.haySuspendidos());
        btnReiniciar.setEnabled(gestor != null);
    }
}
