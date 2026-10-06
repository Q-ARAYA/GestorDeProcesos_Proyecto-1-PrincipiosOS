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

    private SimulatorConfig() { }

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

    public int getMemoriaPrincipal() { return memoriaPrincipal; }
    public int getKernelPorcentaje() { return kernelPorcentaje; }
    public int getDiscoSecundario() { return discoSecundario; }
    public int getMemoriaVirtual() { return memoriaVirtual; }
    public void setValores(int principal, int kernel, int disco, int virtual) {
        memoriaPrincipal = principal; kernelPorcentaje = kernel;
        discoSecundario = disco; memoriaVirtual = virtual;
        validar();
    }
}
