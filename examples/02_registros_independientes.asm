; Ejemplo 2: usa otros registros para comparar el estado entre procesos.
; Resultado final: AC = 4, DX = 2 y CX = 4.

MOV AX, 2
MOV BX, 4
MOV CX, 6
LOAD CX
SUB BX
STORE DX
ADD AX
STORE CX
