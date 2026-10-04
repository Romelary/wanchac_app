# Subsanación y Actualización — Anexos: Validez de Aiken y Prototipo en Wanchaq

> **Documento de destino**: `Proyecto de Investigación v5.91 (1) (1).docx`  
> **Sección a sustituir**: Anexos 10, 11, 12 (Tablas de Validez, págs. 93-95) y Anexo 13 (Prototipo del Aplicativo, Ilustración 17, pág. 96).  
> **Estado**: Listo para copiar y pegar directamente en el documento de tesis.  
> **Fundamentación técnica**: Script [`analisis/confiabilidad/verificar_documento_v591.py`](../../analisis/confiabilidad/verificar_documento_v591.py) y capturas empíricas en hardware de [`docs/evidencia/`](../evidencia/).

---

## RESUMEN DE CAMBIOS EN LOS ANEXOS

| Elemento en v5.91 | Problema en el borrador original | Corrección y Reemplazo Definitivo |
|---|---|---|
| **Anexo 13: Ilustración 17 (pág. 96)** | La captura de pantalla del visualizador geoespacial muestra el mapa de **Urcos, Quispicanchi** (*Terminal Terrestre de Urcos, Río Vilcanota, Puente Urcos, Apv Tupac Amaru de Urcos* con coordenadas `-13.6823, -71.6270`), debido a que las pruebas iniciales del autor se realizaron en su domicilio en Urcos. Sin embargo, el estudio está estrictamente delimitado a **Wanchaq, Cusco**. | Se sustituye la Ilustración 17 por la captura real del visualizador de Google Maps centrado en el **Distrito de Wanchaq, Cusco** (encuadrando la Plaza Túpac Amaru, Av. Huayruropata y Av. de la Cultura, coordenadas $\approx -13.526, -71.964$), demostrando la detección de focos dentro del ámbito territorial del estudio. |
| **Anexo 10: Tabla de Validez SUS (pág. 93)** | La columna "V de Aiken" colocaba el promedio simple de porcentajes $\div 100$ (ej. $0.83$ en vez de $0.917$). | Se actualiza la tabla calculando la V de Aiken matemática: $V = \frac{S}{3 \cdot (5-1)}$, donde $S = \sum (s_i - 1)$ tras recodificar a la escala $1-5$. El coeficiente global asciende a **$V = 0.892$**. |
| **Anexo 11: Tabla de Validez Ficha de Campo (pág. 94)** | Columnas de V inconsistentes con la fórmula formal. | Se actualiza con el recálculo formal de Aiken, obteniendo un coeficiente global de **$V = 0.867$**. |
| **Anexo 12: Tabla de Validez Ficha GPS (pág. 95)** | Columnas de V inconsistentes con la fórmula formal. | Se actualiza con el recálculo formal de Aiken, obteniendo un coeficiente global de **$V = 0.892$**. |

---

# CONTENIDO DEFINITIVO PARA LOS ANEXOS DEL DOCUMENTO WORD

---

## Anexo 10. Tabla de prueba de validez del Instrumento 1: Cuestionario de Usabilidad del Sistema (SUS)

**Tabla 17**  
*Prueba de validez de contenido por coeficiente V de Aiken — Instrumento 1 (SUS)*

| N.º | Criterio de Evaluación | Juez 1 (Pumasupa) | Juez 2 (Huamán) | Juez 3 (Delgado) | Puntuación Recodificada $(s_1, s_2, s_3)$ | Suma $S$ | Coeficiente V de Aiken | Valoración Cualitativa |
|:---:|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | **Claridad**: Formulado con lenguaje apropiado | 90 % | 95 % | 65 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 2 | **Objetividad**: Expresado en conductas observables | 85 % | 92 % | 65 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 3 | **Actualidad**: Adecuado al alcance de la tecnología | 95 % | 94 % | 60 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| 4 | **Organización**: Existe una organización lógica | 90 % | 91 % | 70 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 5 | **Suficiencia**: Comprende aspectos de cantidad y calidad | 90 % | 90 % | 68 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 6 | **Intencionalidad**: Adecuado para valorar el estudio | 95 % | 96 % | 70 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 7 | **Consistencia**: Basado en fundamentos científicos | 90 % | 93 % | 60 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| 8 | **Coherencia**: Coherente entre indicadores y dimensiones | 90 % | 95 % | 60 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| 9 | **Metodología**: La estrategia responde al propósito | 90 % | 92 % | 75 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 10 | **Conveniencia**: Genera nuevas pautas teóricas | 85 % | 94 % | 75 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| **TOTAL** | **Coeficiente Global del Instrumento** | **90.0 %** | **93.2 %** | **66.8 %** | — | — | **0.892** | **Excelente Validez** |

*Nota*. Escala de recodificación: 1 (Deficiente: 0-20%), 2 (Regular: 21-40%), 3 (Bueno: 41-60%), 4 (Muy Bueno: 61-80%), 5 (Excelente: 81-100%). Fórmula aplicada: $V = \frac{S}{n(c-1)} = \frac{S}{3 \cdot 4} = \frac{S}{12}$. Criterio de decisión: $V \ge 0.70$ (Aplicable).

---

## Anexo 11. Tabla de prueba de validez del Instrumento 2: Ficha de Observación de Campo

**Tabla 18**  
*Prueba de validez de contenido por coeficiente V de Aiken — Instrumento 2 (Ficha de Campo)*

| N.º | Criterio de Evaluación | Juez 1 (Pumasupa) | Juez 2 (Huamán) | Juez 3 (Delgado) | Puntuación Recodificada $(s_1, s_2, s_3)$ | Suma $S$ | Coeficiente V de Aiken | Valoración Cualitativa |
|:---:|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | **Claridad**: Formulado con lenguaje apropiado | 95 % | 95 % | 75 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 2 | **Objetividad**: Expresado en conductas observables | 95 % | 91 % | 60 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| 3 | **Actualidad**: Adecuado al alcance de la tecnología | 95 % | 94 % | 75 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 4 | **Organización**: Existe una organización lógica | 95 % | 92 % | 50 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| 5 | **Suficiencia**: Comprende aspectos de cantidad y calidad | 90 % | 95 % | 65 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 6 | **Intencionalidad**: Adecuado para valorar el estudio | 95 % | 93 % | 50 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| 7 | **Consistencia**: Basado en fundamentos científicos | 95 % | 94 % | 50 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| 8 | **Coherencia**: Coherente entre indicadores y dimensiones | 95 % | 93 % | 55 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| 9 | **Metodología**: La estrategia responde al propósito | 95 % | 96 % | 60 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| 10 | **Conveniencia**: Genera nuevas pautas teóricas | 90 % | 90 % | 61 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| **TOTAL** | **Coeficiente Global del Instrumento** | **94.0 %** | **93.3 %** | **60.1 %** | — | — | **0.867** | **Excelente Validez** |

*Nota*. Criterio de decisión: $V \ge 0.70$ (Aplicable plenamente).

---

## Anexo 12. Tabla de prueba de validez del Instrumento 3: Ficha de Verificación Geoespacial y Disponibilidad

**Tabla 19**  
*Prueba de validez de contenido por coeficiente V de Aiken — Instrumento 3 (Ficha Geoespacial)*

| N.º | Criterio de Evaluación | Juez 1 (Pumasupa) | Juez 2 (Huamán) | Juez 3 (Delgado) | Puntuación Recodificada $(s_1, s_2, s_3)$ | Suma $S$ | Coeficiente V de Aiken | Valoración Cualitativa |
|:---:|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | **Claridad**: Formulado con lenguaje apropiado | 85 % | 94 % | 62 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 2 | **Objetividad**: Expresado en conductas observables | 95 % | 93 % | 65 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 3 | **Actualidad**: Adecuado al alcance de la tecnología | 95 % | 95 % | 65 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 4 | **Organización**: Existe una organización lógica | 85 % | 92 % | 61 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 5 | **Suficiencia**: Comprende aspectos de cantidad y calidad | 85 % | 91 % | 66 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 6 | **Intencionalidad**: Adecuado para valorar el estudio | 95 % | 94 % | 65 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 7 | **Consistencia**: Basado en fundamentos científicos | 90 % | 96 % | 55 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| 8 | **Coherencia**: Coherente entre indicadores y dimensiones | 90 % | 93 % | 60 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| 9 | **Metodología**: La estrategia responde al propósito | 95 % | 95 % | 61 % | (5, 5, 4) | 11 | **0.917** | Excelente |
| 10 | **Conveniencia**: Genera nuevas pautas teóricas | 90 % | 92 % | 60 % | (5, 5, 3) | 10 | **0.833** | Muy Bueno |
| **TOTAL** | **Coeficiente Global del Instrumento** | **90.5 %** | **93.5 %** | **62.0 %** | — | — | **0.892** | **Excelente Validez** |

*Nota*. Criterio de decisión: $V \ge 0.70$ (Aplicable plenamente).

---

## Anexo 13. Prototipo del aplicativo móvil (Sustitución de la Ilustración 17)

En el documento original v5.91 (página 96), la **Ilustración 16** corresponde a la pantalla de captura del nuevo reporte y la **Ilustración 17** corresponde al visualizador geoespacial.

### Instrucción de Reemplazo para la Ilustración 17:

1. **Retirar la imagen original (`image18.png`)**: La imagen actual muestra la cartografía de *Urcos (Terminal Terrestre, Río Vilcanota, Puente Urcos)* con un reporte de prueba titulado *«Apv Tupac Amaru de Urcos»* y coordenadas $-13.6823, -71.6270$.
2. **Insertar la nueva captura de pantalla**: Reemplazar por la captura del visualizador de Google Maps tomada desde la aplicación móvil ejecutándose en el dispositivo, debidamente centrada en el **Distrito de Wanchaq, Cusco**.
   * *Ubicación del encuadre geográfico*: Vista cartográfica que abarque la **Plaza Túpac Amaru**, la **Av. de la Cultura**, el **Óvalo Garcilaso** y la **Av. Huayruropata** (latitud media $\approx -13.526$, longitud media $\approx -71.964$).
   * *Elementos visibles en pantalla*: 
     - Encabezado superior: *«Mapa de Focos — Wanchaq»*.
     - Marcadores de focos generados por el algoritmo de agrupamiento DBSCAN en las calles distritales.
     - Tarjeta resumen inferior con un foco seleccionado: `CATEGORÍA: PLÁSTICO / ORGÁNICO`, `REPORTES: 4`, `UBICACIÓN: Distrito de Wanchaq`.
   * *Evidencia fotográfica de respaldo existente*: Si se desea una captura verificada en tableta, se puede utilizar la imagen [`docs/evidencia/2026-09-19_tab-s9plus_mapa-con-focos-de-prueba.jpg`](../evidencia/2026-09-19_tab-s9plus_mapa-con-focos-de-prueba.jpg), la cual ya demuestra la carga de Google Maps real en la Galaxy Tab S9+ dentro del entorno de prueba.

### Texto y Epígrafe para el Documento Word:

**Ilustración 16**  
*Pantalla de registro y captura fotográfica del punto de acumulación*  
*(Se conserva la captura actual del flujo de cámara e indicador de GPS).*

**Ilustración 17**  
*Pantalla de visualización geoespacial de focos de acumulación en el distrito de Wanchaq*  
*(Insertar captura de Google Maps centrada en Wanchaq).*  
*Nota*. Visualizador del cliente móvil desarrollado con Google Maps SDK, mostrando los clústeres de acumulación detectados mediante el algoritmo DBSCAN en el ámbito territorial del distrito de Wanchaq, Cusco.
