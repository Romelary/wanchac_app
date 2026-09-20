# Corrección propuesta del Cap. III (v5.91): confiabilidad de los instrumentos y V de Aiken

> **Estado**: propuesta para revisar y pegar en el `.docx`; **no cambia el documento original**. La sección de confiabilidad se aplica **cuando existan los datos reales** (los corchetes `[ ]` se llenan con la salida de las mediciones, nunca con datos simulados); la V de Aiken la revisa el usuario con el asesor.
> Base: [`plan-confiabilidad-instrumentos.md`](../plan-confiabilidad-instrumentos.md) (§2, §5, §10, §12), [CORR-011 y CORR-012](../correcciones-documento.md) y la [evidencia](../evidencia/2026-09-19_verificacion-tesis-v591.txt). Fecha: 2026-09-19.

## 1. Qué cambiar (resumen)

| Dónde (v5.91) | Qué dice hoy | Qué poner |
|---|---|---|
| Cap. III, «Confiabilidad de los instrumentos», párrafo inicial | «pruebas… con datos de una muestra piloto simulada» | El método del §2, en pasado, cuando las mediciones estén hechas |
| Tabla 5 | Columna «Qué necesitas» (nota de trabajo) | La Tabla 5 del §3 |
| Confiabilidad del SUS | «alfa superior a 0.80», sin n ni intervalo | Plantilla del §4 (I1) |
| Confiabilidad de la Ficha de campo | Acuerdo 66,7 % → κ «moderado-sustancial»; 83,3 % → κ «casi perfecta (> 0,80)», con 6 focos | Plantilla del §4 (I2). Con esos acuerdos el κ **máximo posible** es 0,54 y 0,78 (315 y 672 tablas enumeradas) |
| Confiabilidad del GPS | Tres puntos (GPS-1 a GPS-3), errores 4,63 / 8,23 / 11,47 m | Plantilla del §4 (I3): la ficha define **8 puntos × 5 mediciones** |
| Texto bajo la Tabla 3 | «dispersión… de 59.2 a 94.0» | «de 60.1 a 94.0 en los promedios por juez (el puntaje individual más bajo fue 50)» |
| Tabla 4 y Anexos 10 a 12 | V = 0.83 / 0.85 / 0.84 | Valores recalculados del §5 (a revisar con el asesor) |

## 2. Método (para pegar, en pasado, **solo cuando las mediciones ya se hicieron**)

> La confiabilidad de los instrumentos se evaluó con datos reales recogidos durante la ejecución del estudio, con el estadístico que corresponde a la naturaleza de cada instrumento (Tabla 5). Para el Instrumento 1 se calculó la consistencia interna mediante el alfa de Cronbach sobre los diez ítems, previa inversión de los ítems de enunciado negativo (los pares: 2, 4, 6, 8 y 10). Para el Instrumento 2 se calculó la concordancia entre dos observadores independientes, ciegos a la sugerencia del sistema, mediante el índice kappa de Cohen (ponderado en las variables ordinales) sobre las mismas agrupaciones. Para el Instrumento 3 se calculó la repetibilidad de las mediciones mediante la desviación estándar de las cinco lecturas por punto y el límite de repetibilidad, junto con la concordancia entre el filtro de precisión y el error medido respecto de la coordenada de referencia (fórmula de Haversine). Los criterios de aceptación (alfa ≥ 0,70; kappa ≥ 0,61) se fijaron antes de recolectar los datos, y cada coeficiente se reporta con su intervalo de confianza del 95 % y el tamaño de muestra en que se calculó. Los criterios que no se cumplieron se reportan como tales, junto con su análisis.

**Antes de medir** (para que «se fijaron antes» sea verdad): decidir con el asesor los criterios (plan §12, D1 a D10; tarea G1) y guardarlos en git con fecha.

## 3. Tabla 5 corregida (sin la columna «Qué necesitas»)

| Instrumento | Estadístico | Diseño de la medición | Criterio de aceptación |
|---|---|---|---|
| I1. Cuestionario SUS | Alfa de Cronbach, con IC 95 % (Feldt) | Piloto de al menos 15 participantes que usaron la app (mejor 20), aparte de los 38; alfa final con los 38; ítems 2, 4, 6, 8 y 10 invertidos | α ≥ 0,70 |
| I2. Ficha de Observación de Campo | Kappa de Cohen entre dos observadores (ponderado en ordinales), con IC 95 % | Dos observadores independientes, ciegos a la IA, sobre las mismas agrupaciones (todas; mínimo 10). κ por separado para confirmación del foco, tipo de residuo y volumen | κ ≥ 0,61 |
| I3. Ficha de Verificación Geoespacial | Repetibilidad (DE de las 5 mediciones por punto y límite de repetibilidad) y concordancia entre el filtro y el error medido | 8 puntos × 5 mediciones, en al menos 2 teléfonos, con ubicación real (nunca simulada) | Anexo 2: mediana del error ≤ 10 m; P90 ≤ 15 m; aceptadas por el filtro ≥ 90 % |

## 4. Plantillas de resultados (llenar con datos reales)

> **Instrumento 1.** Con [n] participantes, el alfa de Cronbach fue [α] (IC 95 %: [inf; sup]); la correlación ítem-total corregida osciló entre [mín] y [máx]; el error estándar de medida fue de [SEM] puntos SUS. [Cumple / No cumple] el criterio de 0,70.
>
> **Instrumento 2.** Con [n] agrupaciones observadas por dos observadores independientes, el kappa fue de [κ] (IC 95 %: [inf; sup]) para la confirmación del foco, [κ] para el tipo de residuo y [κ ponderado] para el volumen. [Cumple / No cumple] el criterio de 0,61 en [variables].
>
> **Instrumento 3.** Con 8 puntos y 5 mediciones por punto en [k] teléfonos, la desviación estándar agrupada fue de [s_w] m (IC 95 %: [inf; sup]) y el límite de repetibilidad de [r] m; la mediana del error fue de [x] m y el P90 de [y] m; el filtro de 15 m aceptó el [z] % de las mediciones. [Cumple / No cumple] los criterios del Anexo 2.

Tabla de resultados sugerida (una por instrumento): campo · n · estadístico · IC 95 % · criterio · ¿cumple? · observaciones (desacuerdos y su causa).

**No escribir**: cifras que no salgan de las mediciones; un kappa deducido del porcentaje de acuerdo (depende de las distribuciones marginales); «supera ampliamente» sin el intervalo; el alfa publicado del SUS (0,91) como si fuera del cuestionario adaptado (hay que medirlo); κ «casi perfecto» con 6 unidades.

## 5. V de Aiken recalculada (para revisar con el asesor · G14, CORR-012)

**Método** (el que ya describe el documento): cada porcentaje se recodifica a las 5 bandas de la ficha (0-20 deficiente = 1, 21-40 regular = 2, 41-60 bueno = 3, 61-80 muy bueno = 4, 81-100 excelente = 5) y `V = S / [n (c − 1)]`, con `S` = suma de (valor − 1) de los 3 jueces, `n` = 3 y `c` = 5. Los porcentajes están copiados de los Anexos 10 a 12; se reproduce con `python analisis/confiabilidad/verificar_documento_v591.py`.

**Hallazgo**: la «V del anexo» **no es la V de Aiken**: coincide con el promedio de los tres porcentajes dividido entre 100 (10 de 10 criterios en el Anexo 10; 0 de 10 en el 11; 3 de 10 en el 12). La V de Aiken recalculada es **más alta**, así que la conclusión «es aplicable» (V ≥ 0,70) no cambia; cambian las cifras y el método que se declara.

| Instrumento | V en la Tabla 4 | V de Aiken recalculada | Menor V por criterio |
|---|---|---|---|
| I1. SUS (Anexo 10) | 0.83 | **0.89** | 0.833 |
| I2. Ficha de campo (Anexo 11) | 0.85 | **0.87** | 0.833 |
| I3. Ficha GPS (Anexo 12) | 0.84 | **0.89** | 0.833 |

Detalle por criterio (jueces 1, 2 y 3 en %; V del anexo → V de Aiken):

| Criterio | Anexo 10 (SUS) | Anexo 11 (Ficha de campo) | Anexo 12 (Ficha GPS) |
|---|---|---|---|
| Claridad | 90, 95, 65 · 0.83 → 0.917 | 95, 95, 75 · 0.85 → 0.917 | 85, 94, 62 · 0.81 → 0.917 |
| Objetividad | 85, 92, 65 · 0.81 → 0.917 | 95, 91, 60 · 0.84 → 0.833 | 95, 93, 65 · 0.84 → 0.917 |
| Actualidad | 95, 94, 60 · 0.83 → 0.833 | 95, 94, 75 · 0.83 → 0.917 | 95, 95, 65 · 0.83 → 0.917 |
| Organización | 90, 91, 70 · 0.84 → 0.917 | 95, 92, 50 · 0.86 → 0.833 | 85, 92, 61 · 0.82 → 0.917 |
| Suficiencia | 90, 90, 68 · 0.83 → 0.917 | 90, 95, 65 · 0.84 → 0.917 | 85, 91, 66 · 0.81 → 0.917 |
| Intencionalidad | 95, 96, 70 · 0.87 → 0.917 | 95, 93, 50 · 0.86 → 0.833 | 95, 94, 65 · 0.86 → 0.917 |
| Consistencia | 90, 93, 60 · 0.81 → 0.833 | 95, 94, 50 · 0.83 → 0.833 | 90, 96, 55 · 0.82 → 0.833 |
| Coherencia | 90, 95, 60 · 0.82 → 0.833 | 95, 93, 55 · 0.83 → 0.833 | 90, 93, 60 · 0.81 → 0.833 |
| Metodología | 90, 92, 75 · 0.86 → 0.917 | 95, 96, 60 · 0.89 → 0.833 | 95, 95, 61 · 0.88 → 0.917 |
| Conveniencia | 85, 94, 75 · 0.85 → 0.917 | 90, 90, 61 · 0.85 → 0.917 | 90, 92, 60 · 0.86 → 0.833 |

Puntos para el asesor: (a) con solo 3 jueces y 5 categorías, un acuerdo al azar alcanzaría V ≥ 0,917 con probabilidad 0,032 y V ≥ 0,833 con 0,080 (evidencia); (b) el tercer juez puntúa de forma sistemáticamente más baja (promedios 66.8, 60.1 y 62.0 frente a 90 a 94 de los otros dos): conviene decir cómo se incorporaron sus observaciones; (c) la etiqueta cualitativa «Muy bueno» de la Tabla 4 habría que revisarla con la escala que se use.

## 6. Qué necesita cada prueba de la app (estado al 2026-09-19)

| Prueba | Necesita | Estado |
|---|---|---|
| SUS (I1) | APK de producción en manos de los participantes | Paso 3 (APK), pendiente |
| Ficha de campo (I2) | Focos detectados por el sistema y cómo se confirman | Detección hecha ([ADR-0009](../adr/0009-focos-detectados-al-pedir-con-dbscan.md)), sin desplegar; la confirmación (A18) está pendiente |
| Ficha GPS (I3) | GPS real dentro de Wanchaq y la precisión declarada de cada lectura | Precisión guardada ([ADR-0008](../adr/0008-precision-del-gps-y-bandera-de-prueba.md)); falta probar dentro del distrito (D5). Las lecturas **bloqueadas** por el filtro no se guardan (CORR-013, plan §9) |
