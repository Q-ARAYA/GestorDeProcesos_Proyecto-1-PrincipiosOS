# Proyecto 1 - Gestor de procesos MiniPC

Simulador de una minicomputadora desarrollado para el curso **IC-6600 Principios de Sistemas Operativos**. El programa permite cargar hasta cinco programas en ensamblador, ejecutar sus instrucciones y observar en la interfaz cómo interactúan los procesos, la CPU, la memoria y los dispositivos simulados.

## Integrante

- **Quiriat Mata** - **Carnet: 2023379891**

## Objetivo

Representar de forma visual conceptos fundamentales de un sistema operativo: carga y ejecucion de programas, bloques de control de procesos (BCP), planificacion, memoria, registros, pila, llamadas al sistema y operaciones de entrada/salida. La aplicacion es una simulacion educativa; no controla hardware real.

## Funcionalidades Alcanzadas

- Carga y validacion de programas `.asm` mediante un ensamblador sencillo.
- Ejecucion paso a paso o continua, con resaltado sincronizado de la instruccion actual en el codigo y en la memoria.
- Administracion de hasta cinco procesos y planificacion **FCFS**.
- BCP enlazados dentro del area de kernel, con estado, PC, registros, pila, direcciones, tiempos, archivos abiertos y enlace al siguiente BCP.
- Estados de proceso: nuevo, listo, ejecutando, espera, suspendido, error y terminado.
- Suspension y reanudacion de procesos, espera de entrada por teclado y ejecucion de otro trabajo listo.
- CPU simulada con registros, instrucciones aritmeticas, comparaciones, saltos, interrupciones y llamadas al sistema.
- Memoria principal dividida entre kernel y usuario, con controles de limites y memoria asignada por programa.
- Pila independiente por proceso, con capacidad cinco y deteccion de desbordamiento y subdesbordamiento.
- Simulacion de pantalla, teclado, dispositivos, almacenamiento secundario y memoria virtual.
- Llamadas de archivo `INT 21H`, estadisticas de ejecucion y panel de seguridad.
- Interfaz Swing con paneles para programa, registros, BCP, memoria, trabajos, dispositivos, almacenamiento, estadisticas y seguridad.

## Interfaz y diseno

Los paneles principales permiten ver simultaneamente el programa cargado, registros, procesos y memoria. Las demas vistas se encuentran organizadas en pestañas. Los cuadros de dialogo notifican cuando el simulador necesita una entrada o una accion del usuario.

El diagrama de paquetes y la explicacion del diseno del sistema operativo se encuentran en [output/pdf/Diagrama_paquetes_diseno_y_guion_video.pdf](output/pdf/Diagrama_paquetes_diseno_y_guion_video.pdf).

## Arquitectura del codigo

```text
src/main/java/com/tec/minipc/
|-- Main.java                 Entrada de la aplicacion
|-- gui/                      Ventana principal y paneles Swing
|-- core/                     CPU, procesos, PCB, ensamblador y servicios
`-- model/                    Instrucciones, registros, memoria y almacenamiento

examples/                     Programas ensamblador de demostracion
output/pdf/                   Diagrama de paquetes y diseno del S.O.
pom.xml                       Configuracion Maven
```

Responsabilidades principales:

- **`com.tec.minipc.gui`** presenta el estado y recoge las acciones del usuario.
- **`com.tec.minipc.core`** coordina la ejecucion, los procesos y los servicios simulados.
- **`com.tec.minipc.model`** representa las instrucciones, los registros y los recursos de memoria y almacenamiento.
- **`com.tec.minipc.Main`** inicia la ventana principal.

## Requisitos

- Java **26** (el proyecto configura `maven.compiler.release` en 26).
- Apache Maven 3.9 o una version compatible.
- Apache NetBeans con soporte para proyectos Maven.

## Abrir y ejecutar en NetBeans

1. Abra NetBeans y seleccione **File > Open Project**.
2. Seleccione la carpeta raiz del proyecto, donde esta `pom.xml`.
3. Configure el JDK 26 para el proyecto.
4. Espere a que Maven cargue el proyecto y ejecute la clase principal `com.tec.minipc.Main` usando **Run Project**.

Tambien puede compilar desde una terminal abierta en la carpeta raiz:

```bash
mvn clean package
```

La clase principal configurada es `com.tec.minipc.Main`.

## Configuracion del simulador

La aplicacion lee y guarda preferencias en `simulador.properties`, en el directorio de ejecucion. Valores iniciales:

- Memoria principal: 256 celdas.
- Porcentaje de kernel: 25% de la memoria principal.
- Almacenamiento secundario: 512 celdas.
- Memoria virtual representada: 64 paginas de 8 bytes.

Los valores se pueden ajustar desde la interfaz y guardar con el control de configuracion correspondiente. El BCP ocupa seis celdas; con el limite actual de cinco procesos se reservan hasta treinta celdas para BCP en kernel.

## Ejemplos incluidos

Los programas de demostracion estan en `examples/`:

| Archivo | Demostracion |
| --- | --- |
| `01_suma_y_resta.asm` | Operaciones aritmeticas y resultado en registros. |
| `02_registros_independientes.asm` | Uso separado de registros. |
| `03_mov_inc_dec_swap.asm` | Movimiento e incremento/decremento de valores. |
| `04_cmp_jumps.asm` | Comparaciones y saltos condicionales. |
| `05_interrupciones_e_s.asm` | Salida a pantalla simulada y solicitud de entrada por teclado. |
| `06_param_pila.asm` | Uso de la pila del proceso. |
| `07_archivos_int21.asm` | Creacion, escritura, apertura y lectura de un archivo simulado. |
| `08_prueba_suspension_teclado.asm` | Espera por teclado y prueba de suspension/reanudacion. Carguelo primero para la prueba multiproceso. |
| `09_proceso_acompanante.asm` | Proceso secundario para observar el cambio de trabajo mientras otro espera. Carguelo despues del ejemplo 08. |

En este ensamblador los valores inmediatos se expresan en decimal, salvo que los comentarios del programa indiquen lo contrario.

## Llamadas de archivo `INT 21H`

El nombre de archivo se coloca en `DX`, por ejemplo `MOV DX, "notas.txt"`. `AH` selecciona la funcion y `AL` transporta el byte escrito o leido. Los valores de `AH` son decimales y el codigo hexadecimal asociado se muestra a continuacion.

| `AH` decimal | Codigo | Funcion |
| ---: | ---: | --- |
| 60 | `3CH` | Crear y abrir archivo. |
| 61 | `3DH` | Abrir archivo existente. |
| 62 | `3EH` | Cerrar archivo actual. |
| 77 | `4DH` | Leer un byte en `AL`; devuelve 0 al llegar al final. |
| 64 | `40H` | Escribir el byte de `AL`. |
| 65 | `41H` | Eliminar un archivo cerrado. |

Los nombres aceptan hasta 64 caracteres: letras, numeros, punto, guion y guion bajo. El almacenamiento secundario muestra el indice de archivos y programas, sus datos y la actividad de las operaciones simuladas. Si un proceso termina con un archivo abierto, el simulador lo cierra automaticamente.

## Modelo de memoria y proteccion

- Los programas se cargan en la zona de usuario; el kernel tiene un porcentaje configurable de la memoria principal.
- El ensamblador valida instrucciones, operandos y destinos de salto antes de admitir un programa.
- La CPU comprueba los limites asignados al proceso para evitar accesos fuera de su region.
- Cada proceso tiene una pila propia con capacidad cinco; el desbordamiento o subdesbordamiento produce un error del proceso.
- Las entradas de teclado aceptan valores entre 0 y 255.
- Las operaciones de `INT 21H` validan la funcion, el archivo, el estado de apertura y el espacio disponible.
- El panel de seguridad permite observar la proteccion y los eventos relevantes de la simulacion.

## Alcance conocido

La planificacion implementada es FCFS y se simula un solo CPU. La memoria virtual se presenta para fines didacticos; no implementa paginacion bajo demanda con reemplazo de paginas. Aunque la prioridad se menciona en el enunciado, no se usa actualmente como campo operativo del PCB.

## Video de demostracion

**Enlace:** https://youtu.be/NM324V0kTy8

