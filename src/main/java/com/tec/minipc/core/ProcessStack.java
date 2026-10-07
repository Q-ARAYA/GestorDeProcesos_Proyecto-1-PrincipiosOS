package com.tec.minipc.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pila privada por proceso, limitada a cinco valores como pide el enunciado. */
public class ProcessStack {
    public static final int CAPACIDAD = 5;
    private final int[] datos = new int[CAPACIDAD];
    private int tamano;

    /**
     * Inserta un valor en el tope de la pila; falla si ya se alcanzó la capacidad máxima.
     * @param valor valor que se asignará
     */
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
        // Primero se valida el tamaño total; así PARAM se inserta completo o no modifica la pila.
        for (int valor : valores) push(valor);
    }

    /**
     * Extrae y devuelve el valor del tope; falla cuando la pila está vacía.
     * @return valor, objeto o colección descrita en el resumen del método.
     */
    public int pop() {
        if (tamano == 0) throw new IllegalStateException("Subdesbordamiento de pila: no hay valores para extraer.");
        // La pila es LIFO: se decrementa el tope y se extrae el último valor insertado.
        return datos[--tamano];
    }

    /**
     * Devuelve cuántos valores contiene actualmente la pila.
     * @return valor calculado o estado consultado.
     */
    public int getTamano() { return tamano; }
    /**
     * Devuelve la capacidad máxima de la pila.
     * @return valor calculado o estado consultado.
     */
    public int getCapacidad() { return CAPACIDAD; }

    /** Devuelve primero el elemento que se extraería en el próximo POP. */
    public List<Integer> getValoresDeArribaAbajo() {
        List<Integer> resultado = new ArrayList<>();
        for (int i = tamano - 1; i >= 0; i--) resultado.add(datos[i]);
        return Collections.unmodifiableList(resultado);
    }

    /**
     * Devuelve los valores de la pila desde el tope hasta la base como texto.
     * @return contenido textual de la pila.
     */
    @Override public String toString() { return getValoresDeArribaAbajo().toString(); }
}
