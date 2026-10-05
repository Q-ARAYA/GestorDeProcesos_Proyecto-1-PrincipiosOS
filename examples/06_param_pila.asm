; Ejemplo 6: PARAM apila tres valores; PUSH/POP manipulan la pila LIFO.
; Al finalizar: AX=30, BX=30, CX=20 y DX=11; la pila queda vacía.

PARAM 10, 20, 30
POP AX
PUSH AX
POP BX
POP CX
POP DX
INC DX
