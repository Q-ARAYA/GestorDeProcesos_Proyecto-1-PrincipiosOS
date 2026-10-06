; Prueba interactiva de suspensión, reanudación y entrada del teclado.
; Cárguelo junto con 09_proceso_acompanante.asm (seleccione primero este archivo).
; Cuando el proceso llegue a INT 09H, escriba un valor de 0 a 255 en Dispositivos E/S.
; El valor leído se mostrará en la pantalla simulada mediante INT 10H.

MOV AX, 1
INC AX
INC BX
INC CX
INC DX
INC
INC AX
INT 09H

; DX contiene el valor que se recibió del teclado.
INT 10H
INC AX
INC AX
INC AX
INC AX
INC AX
INT 20H
