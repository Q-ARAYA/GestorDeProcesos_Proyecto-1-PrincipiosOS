; Ejemplo 3: movimiento entre registros, incrementos, decrementos e intercambio.
; Al finalizar: AC=0, AX=9, BX=1 y CX=3.

MOV AX, 2
MOV BX, 9
MOV CX, AX
INC CX
DEC AX
SWAP AX, BX
INC
DEC
