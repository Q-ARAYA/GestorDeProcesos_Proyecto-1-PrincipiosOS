; Ejemplo de llamadas a archivos del sistema simulado.
; DX contiene el nombre de archivo entre comillas; AH selecciona la funcion y AL transporta bytes.
; Se crea notas.txt, se escriben H e i, luego se abre y se lee el primer byte.

MOV DX, "notas.txt"
MOV AH, 60       ; 3CH: crear archivo
INT 21H

MOV AL, 72       ; ASCII H
MOV AH, 64       ; 40H: escribir un byte
INT 21H
MOV AL, 105      ; ASCII i
INT 21H
MOV AH, 62       ; 3EH: cerrar
INT 21H

MOV AH, 61       ; 3DH: abrir
INT 21H
MOV AH, 77       ; 4DH: leer un byte; resultado queda en AL
INT 21H
MOV AH, 62       ; 3EH: cerrar
INT 21H
INT 20H

; Para eliminar en lugar de conservar el archivo:
; MOV AH, 65     ; 41H: eliminar
; INT 21H
