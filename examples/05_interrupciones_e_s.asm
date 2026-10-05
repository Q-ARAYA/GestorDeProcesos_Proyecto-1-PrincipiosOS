; Ejemplo 5: INT 10H muestra DX, INT 09H espera entrada del teclado, INT 20H termina.
; Escriba un entero entre 0 y 255 en el panel de teclado/consola.

MOV DX, 72
INT 10H
INT 09H
INT 10H
INT 20H
