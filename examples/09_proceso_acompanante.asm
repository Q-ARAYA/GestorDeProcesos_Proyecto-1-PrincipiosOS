; Proceso breve para observar que la CPU pasa a otro trabajo
; cuando se suspende el proceso 08_prueba_suspension_teclado.asm.

MOV DX, 66
INT 10H
INC AX
INC AX
INC AX
INC AX
INC AX
INT 20H
