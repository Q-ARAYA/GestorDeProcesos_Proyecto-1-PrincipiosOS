; Ejemplo 1: operaciones aritmeticas con el acumulador AC.
; Resultado final: AC = 3 y CX = 10.

MOV AX, 7
MOV BX, 3
LOAD AX
ADD BX
STORE CX
SUB AX
