package com.tec.minipc.core;

import com.tec.minipc.model.Memory;
import com.tec.minipc.model.SecondaryStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Configuración persistente del simulador en simulador.properties (directorio de ejecución). */
public final class SimulatorConfig {
    private static final Path ARCHIVO = Path.of("simulador.properties");
    private int memoriaPrincipal = 256;
    private int kernelPorcentaje = 25;
    private int discoSecundario = SecondaryStorage.TAMANO_POR_DEFECTO;
    private int memoriaVirtual = SecondaryStorage.MEMORIA_VIRTUAL_POR_DEFECTO;

    /** Inicializa SimulatorConfig con los recursos y valores recibidos. */
    private SimulatorConfig() { }

    /**
     * Carga el estado actual.
     * @return resultado generado por la operación.
     * @throws IOException si ocurre un error durante la operación.
     */
    public static SimulatorConfig cargar() throws IOException {
        SimulatorConfig config = new SimulatorConfig();
        if (Files.exists(ARCHIVO)) {
            Properties propiedades = new Properties();
            try (InputStream entrada = Files.newInputStream(ARCHIVO)) { propiedades.load(entrada); }
            config.memoriaPrincipal = Integer.parseInt(propiedades.getProperty("memoria.principal", "256").trim());
            config.kernelPorcentaje = Integer.parseInt(propiedades.getProperty("kernel.porcentaje", "25").trim());
            config.discoSecundario = Integer.parseInt(propiedades.getProperty("disco.capacidad", "512").trim());
            config.memoriaVirtual = Integer.parseInt(propiedades.getProperty("disco.memoriaVirtual", "64").trim());
        }
        config.validar();
        return config;
    }

    /**
     * Guarda el estado actual.
     * @throws IOException si ocurre un error durante la operación.
     */
    public void guardar() throws IOException {
        validar();
        Properties propiedades = new Properties();
        propiedades.setProperty("memoria.principal", String.valueOf(memoriaPrincipal));
        propiedades.setProperty("kernel.porcentaje", String.valueOf(kernelPorcentaje));
        propiedades.setProperty("disco.capacidad", String.valueOf(discoSecundario));
        propiedades.setProperty("disco.memoriaVirtual", String.valueOf(memoriaVirtual));
        try (OutputStream salida = Files.newOutputStream(ARCHIVO)) {
            propiedades.store(salida, "Configuracion del simulador Mini PC");
        }
    }

    /** Valida el estado actual. */
    private void validar() {
        if (memoriaPrincipal < Memory.TAMANO_MINIMO || memoriaPrincipal > 4096)
            throw new IllegalArgumentException("La memoria principal debe estar entre 128 y 4096 celdas.");
        if (kernelPorcentaje < 25 || kernelPorcentaje > 80)
            throw new IllegalArgumentException("El kernel debe ocupar entre 25% y 80% de la memoria.");
        if (discoSecundario < 128 || discoSecundario > 16384)
            throw new IllegalArgumentException("El disco secundario debe estar entre 128 y 16384 celdas.");
        if (memoriaVirtual < 0 || memoriaVirtual > discoSecundario - SecondaryStorage.CELDAS_INDICE)
            throw new IllegalArgumentException("La memoria virtual excede el espacio disponible en el disco.");
    }

    /**
     * Devuelve la capacidad configurada de memoria principal.
     * @return valor numérico producido por la operación.
     */
    public int getMemoriaPrincipal() { return memoriaPrincipal; }
    /**
     * Devuelve el porcentaje de memoria reservado al kernel.
     * @return valor numérico producido por la operación.
     */
    public int getKernelPorcentaje() { return kernelPorcentaje; }
    /**
     * Devuelve la capacidad configurada del almacenamiento secundario.
     * @return valor numérico producido por la operación.
     */
    public int getDiscoSecundario() { return discoSecundario; }
    /**
     * Devuelve el número de páginas virtuales configuradas.
     * @return valor numérico producido por la operación.
     */
    public int getMemoriaVirtual() { return memoriaVirtual; }
    /**
     * Valida y actualiza en conjunto la memoria principal, kernel, disco y región virtual.
     * @param principal capacidad total de memoria principal
     * @param kernel porcentaje reservado al kernel
     * @param disco disco simulado
     * @param virtual tamaño de memoria virtual
     */
    public void setValores(int principal, int kernel, int disco, int virtual) {
        memoriaPrincipal = principal; kernelPorcentaje = kernel;
        discoSecundario = disco; memoriaVirtual = virtual;
        validar();
    }
}
