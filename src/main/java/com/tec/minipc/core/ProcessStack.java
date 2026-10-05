package com.tec.minipc.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pila privada por proceso, limitada a cinco valores como pide el enunciado. */
public class ProcessStack {
    public static final int CAPACIDAD = 5;
    private final int[] datos = new int[CAPACIDAD];
    private int tamano;

    public void push(int valor) {
        if (tamano == CAPACIDAD) throw new IllegalStateException("Desbordamiento de pila: capacidad máxima de 5 valores.");
        datos[tamano++] = valor;
    }

    /** Inserta un grupo completo o falla sin dejar una carga parcial. */
    public void pushAll(List<Integer> valores) {
        if (tamano + valores.size() > CAPACIDAD) {
            throw new IllegalStateException("Desbordamiento de pila: PARAM necesita " + valores.size()
                    + " espacios y solo quedan " + (CAPACIDAD - tamano) + ".");
        }
        for (int valor : valores) push(valor);
    }

    public int pop() {
        if (tamano == 0) throw new IllegalStateException("Subdesbordamiento de pila: no hay valores para extraer.");
        return datos[--tamano];
    }

    public int getTamano() { return tamano; }
    public int getCapacidad() { return CAPACIDAD; }

    /** Devuelve primero el elemento que se extraería en el próximo POP. */
    public List<Integer> getValoresDeArribaAbajo() {
        List<Integer> resultado = new ArrayList<>();
        for (int i = tamano - 1; i >= 0; i--) resultado.add(datos[i]);
        return Collections.unmodifiableList(resultado);
    }

    @Override public String toString() { return getValoresDeArribaAbajo().toString(); }
}
