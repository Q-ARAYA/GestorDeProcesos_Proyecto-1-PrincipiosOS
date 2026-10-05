; Ejemplo 4: CMP, JE, JNE y JMP con desplazamientos relativos.
; El desplazamiento se cuenta desde la instruccion siguiente.
; Resultado final: AX=5, BX=8, CX=42, DX=2; ZF=0 y OF=0.

MOV AX, 5
MOV BX, 5
CMP AX, BX
JE +1
MOV CX, 99
MOV CX, 7
MOV BX, 8
CMP AX, BX
JNE +1
MOV CX, 100
MOV CX, 42
JMP +1
MOV DX, 1
MOV DX, 2
