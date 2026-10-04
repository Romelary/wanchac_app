# Plan de pruebas de confiabilidad de los instrumentos de investigación

> Cómo medir, **con datos reales**, la confiabilidad de los tres instrumentos del Proyecto de Investigación (Cuestionario SUS, Ficha de Observación de Campo y Ficha de Verificación Geoespacial) y de las piezas del sistema donde el concepto aplica: qué prueba corresponde a cada uno (alfa de Cronbach, kappa, repetibilidad/ICC…), cuántos datos hacen falta, cuándo y con qué reglas. Sale de leer completos el documento `Proyecto de Investigación v5.91 (1) (1).docx` (la copia de *Descargas* y la de *OneDrive* tienen el mismo SHA-256) y el proyecto (código y `docs/`) al **2026-09-19**.
>
> **Es una propuesta de método, pendiente de validar con el asesor.** Los umbrales que fija el documento se respetan tal cual; los que faltan (tamaños, criterio de kappa, reglas de independencia) los propongo yo y van marcados *«propuesta»*.
>
> **Estado: no se ha ejecutado ninguna de estas pruebas.** Los resultados que hoy figuran en la sección «Confiabilidad de los instrumentos» del documento **no vienen de datos reales** (§2): este plan es lo que hay que hacer para sustituirlos. Las cifras de §2 y §7 las calculó un script del repositorio ([`analisis/confiabilidad/`](../analisis/confiabilidad/)); su salida está en [`evidencia/`](./evidencia/).

## 0. Resumen

| Instrumento | Qué debe ser «confiable» | Prueba principal | Pruebas de apoyo | Datos mínimos (propuesta) | Criterio |
|---|---|---|---|---|---|
| **I1 · Cuestionario SUS** (Anexo 3) | Que los 10 ítems midan lo mismo (usabilidad percibida) | **Alfa de Cronbach** con IC 95 % (Feldt), ítems ya invertidos | ω de McDonald, correlación ítem-total, error estándar de medida; alfa por subescala **solo** si se reporta la subescala | Piloto ≥ 15 (mejor 20) + la muestra final, n = 38 | α ≥ 0,70 (el del documento) |
| **I2 · Ficha de Observación de Campo** (Anexo 4) | Que dos observadores independientes llenen lo mismo ante el mismo foco | **Kappa de Cohen** por campo (ponderado en los ordinales), con IC 95 % | PABAK y AC1 de Gwet (prevalencia extrema), kappa intraobservador con fotos, kappa de las fotos (Bloque E) | 2 observadores × **todas** las agrupaciones (15–30; el documento pide ≥ 10) + calibración previa en ≥ 20 sitios | κ ≥ 0,61 *(propuesta; Landis y Koch)* |
| **I3 · Ficha de Verificación Geoespacial** (Anexo 5) | Que repetir la lectura del mismo punto dé casi lo mismo, y que el filtro de 15 m decida bien | **DE intra-punto y límite de repetibilidad** + **concordancia filtro–error real** | ICC(1,1), cobertura de la precisión declarada, fiabilidad de la coordenada de referencia | 8 puntos × 5 mediciones independientes (≥ 2 teléfonos) | Derivado de ε = 50 m *(propuesta, §5.3.5)* |

**No aplican**, y por qué: el *test–retest* del SUS como prueba principal (el diseño es transeccional y la opinión sobre una app cambia con el uso; §5.1.6), y el alfa de Cronbach en I2 e I3 (no son escalas aditivas: la Tabla 5 del propio documento ya lo dice para I3).

**Lo más importante:**

1. **La sección de confiabilidad del documento no se sostiene.** Dice usar una «muestra piloto simulada»; sus kappa son aritméticamente imposibles con los acuerdos que declara (con 4 de 6 coincidencias κ ≤ 0,54, con 5 de 6 κ ≤ 0,78); y sus datos no corresponden a las fichas (§2).
2. **Solo hay confiabilidad con datos reales.** La Tabla 5 del documento ya describe el diseño correcto («piloto real con al menos quince participantes», «dos observadores independientes sobre las mismas diez agrupaciones»); esta propuesta lo concreta y lo refuerza.
3. **Diez agrupaciones no bastan** para estimar un kappa (con κ verdadero = 0,75, prevalencia 0,70 y 10 unidades, el κ observado cae en [0,00; 1,00]; §7). Recomiendo doble observación de **todas** las agrupaciones.
4. **Dos huecos de la app afectan a I3**: no guarda la precisión declarada de cada lectura y las lecturas bloqueadas por el filtro no dejan rastro (§3, §9).
5. **El calendario aprieta**: la calibración y la re-calificación intraobservador tienen que ocurrir **antes** de la Fase 2 (§8).

## 1. Dos sentidos de «confiabilidad» (para no mezclarlos)

| | Del **instrumento de medición** (este documento) | Del **software en campo** ([`pruebas-de-confiabilidad.md`](./pruebas-de-confiabilidad.md)) |
|---|---|---|
| Pregunta | ¿Da el instrumento el mismo resultado si lo repiten otras personas, otras veces o con otros ítems? | ¿El sistema entrega lo que promete (reportes guardados y sincronizados) en cada condición de red? |
| Relación con el Anexo 2 | Es condición para **creer** los indicadores; no es un indicador | Es el indicador *«Reportes guardados y sincronizados según condición de red, ≥ 95 % en cada condición»* |
| Estadísticos | α, κ, ICC, DE intra-punto | Proporciones con intervalo de Wilson |
| Dónde se cruzan | I3, Bloques B y C (`¿Guardado?`, `¿Sincronizado?`): §5.3.7 | |

Y dos cosas que **no** son confiabilidad aunque a veces se calculen con el mismo estadístico: la **exactitud** de la IA (Bloque D de I2: kappa entre sistema y observador) y la **validez** del juicio de expertos (Anexos 6–12). Se tratan aparte (§5.2.5 y §2.3).

## 2. Diagnóstico del documento (v5.91)

### 2.1 Lo que dice la sección de confiabilidad frente a lo que exige el propio documento

| # | Dónde | Lo que dice | El problema (con evidencia) |
|---|---|---|---|
| 1 | Cap. III, «Confiabilidad de los instrumentos», párrafo inicial | «Se llevaron a cabo pruebas de confiabilidad… Se utilizaron datos de una muestra piloto **simulada**» | Los datos simulados solo devuelven lo que quien los simula supuso: no miden confiabilidad. Además el tiempo verbal («se llevaron a cabo») choca con el título de la Tabla 5 («Estadísticos **previstos**») y con la Ética del propio documento («los resultados se reportan tal como se obtengan, incluidos los desfavorables»). |
| 2 | Tabla 5, columna «Qué necesitas» | «Piloto real con al menos quince participantes…», «Reportar el valor numérico», «con las categorías reales de la ficha» | Es una **nota de trabajo** que quedó en el texto. Sí es la especificación correcta del diseño y la adopto como base (§5). Debe salir de la versión final. |
| 3 | Confiabilidad del SUS | «Alfa de Cronbach **superior a 0.80**, lo cual supera ampliamente el umbral… 0.70»; «puntuaciones altas en los ítems positivos y bajas en los negativos» | No da el valor, ni n, ni IC. Si el alfa se calculó con las respuestas crudas mezcla ítems de signo opuesto (los pares 2, 4, 6, 8, 10 miden al revés) y no vale: hay que invertirlos primero (§5.1). Y 0,80 no supera «ampliamente» 0,70: con n = 38 un alfa observado de 0,80 tiene IC 95 % [0,69; 0,88] (§7). |
| 4 | Confiabilidad de la Ficha de campo | Acuerdo 66,7 % en «Magnitud del foco» (Bajo/Medio/Alto) → κ «moderado-sustancial»; acuerdo 83,3 % en «Composición» (4 categorías) → «casi perfecta (κ > 0,80)»; sobre **6 focos** (F01–F06) | (a) **Imposible aritméticamente**: con 6 unidades, 4 coincidencias en 3 categorías dan κ ≤ **0,54** y 5 coincidencias en 4 categorías dan κ ≤ **0,78** (se enumeraron las 315 y las 672 tablas posibles: [evidencia](./evidencia/2026-09-19_verificacion-tesis-v591.txt)). (b) La Tabla 5 pide ≥ 10 agrupaciones. (c) Esos campos **no existen así en la ficha**: «Magnitud» no es «Volumen estimado» (Pequeño/Mediano/Grande) y la «Composición» (Orgánico, Plástico, Papel, Vidrio) no coincide con las 7 opciones del Bloque C. (d) No da κ numérico ni IC: el kappa no se puede deducir del porcentaje de acuerdo, depende de las distribuciones marginales. |
| 5 | Confiabilidad de la Ficha GPS | Mediciones «en tres puntos (GPS-1…3)»; errores promedio 4,63 / 8,23 / 11,47 m; «100 % < 15 m»; «confirman la confiabilidad técnica de la **disponibilidad operativa**» | (a) La ficha define **8 puntos × 5 mediciones** (Tabla 2, Anexo 5). (b) El Anexo 2 fija «mediana ≤ 10 m; P90 ≤ 15 m» y el texto no los evalúa. (c) La **repetibilidad** que pide la Tabla 5 (DE de las cinco mediciones por punto) no aparece. (d) Mezcla la precisión posicional con la disponibilidad operativa (sincronización), que son dimensiones distintas con mediciones distintas. (e) Habla de «un equipo de control», pero la ficha fija como referencia una imagen satelital (Google Earth Pro). |
| 6 | Anexo 3, nota de adaptación | «Los ítems 5, 8 y 9 fueron contextualizados» | Leídos frente al SUS estándar, los **diez** enunciados están reformulados en distinto grado (p. ej. el 2 y el 6 unen dos ideas; el 4 cambia «persona técnica» por «otra persona»; el 10 añade «técnicas»). La confiabilidad publicada del SUS (α = 0,91, Bangor, Kortum y Miller, 2008) **no se hereda**: hay que medirla. |
| 7 | Anexo 2, sub-indicadores del SUS | Facilidad de aprendizaje, de uso e intención de uso con umbral «≥ 3,5» | Son subescalas de 3, 6 y **1** ítem. Con k = 3 el alfa es bajo e impreciso por construcción (con n = 38, un 0,70 observado tiene IC [0,49; 0,83]); con 1 ítem no existe alfa. La literatura desaconseja calcular subescalas del SUS de forma rutinaria (Lewis y Sauro, 2017). Si se reportan, con su alfa y con cautela. |

### 2.2 Por qué una «muestra piloto simulada» no sirve, y qué hacer

La confiabilidad es una propiedad **empírica** de los puntajes que producen personas u observadores reales con un instrumento concreto. Un piloto simulado solo muestra los supuestos con los que se lo generó: pasa cualquier umbral que se le ponga. El texto sí declara que la muestra es simulada (eso es honesto), pero después concluye como si fuera evidencia («esto confirma que el cuestionario es altamente confiable…», «estos resultados demuestran que la Ficha de Observación de Campo es un instrumento confiable, estandarizado y reproducible»). Presentarlo así es un problema de integridad del trabajo, no de estilo, y un evaluador atento lo detectará (falta el valor de alfa, los kappa son imposibles, los focos F01–F06 y los puntos GPS-1…3 no son los del diseño).

**Qué hacer** (tareas G1 y G2 del [tablero](./avance.md)): (1) mientras no haya datos, la sección debe decir **lo que se hará** (texto propuesto en §10, versión A), no lo que se hizo; (2) al terminar la recolección se reemplaza por los resultados reales con su intervalo (versión B); (3) si se quiere conservar algún ejercicio simulado, solo como **ensayo del método o dimensionamiento**, rotulado así y en un anexo, nunca en resultados. Queda registrado como [CORR-011](./correcciones-documento.md).

### 2.3 Hallazgos colaterales sobre *validez* (fuera de esta prueba, pero verificados)

Son del juicio de expertos, no de confiabilidad, pero salieron al recalcular las Tablas 3 y 4 y conviene que el asesor los vea ([evidencia](./evidencia/2026-09-19_verificacion-tesis-v591.txt); [CORR-012](./correcciones-documento.md)):

- **La «V de Aiken» de los Anexos 10–12 no es la V de Aiken.** En el Anexo 10 (SUS) coincide con el **promedio de los tres porcentajes ÷ 100** en los 10 criterios; en el Anexo 11 no coincide en ninguno (ni con el promedio ni con la V), y en el 12 en 3 de 10. Calculada como describe el texto (V = S / [n(c−1)], recodificando cada porcentaje a las cinco bandas de la ficha institucional), da ≈ 0,89 / 0,87 / 0,89, no 0,83 / 0,85 / 0,84.
- **Con 3 jueces y 5 categorías** la probabilidad exacta, si eligieran al azar, de V ≥ 0,833 es 0,08 y de V ≥ 0,917 es 0,03: el «≥ 0,70» es una convención práctica, no una prueba estadística de acuerdo.
- El **tercer juez** puntúa sistemáticamente más bajo (50–75 %) que los otros dos (90–96 %); una V global lo promedia y lo esconde.
- La nota de la Tabla 3 habla de una dispersión «de 59.2 a 94.0»; el mínimo de los promedios es **60,1** (el 59,2 no aparece en ningún lado).

Como los Anexos 6–12 son instrumentos con validación externa ya emitida, **no se editan en silencio**: requiere visto bueno del asesor.

## 3. Diagnóstico del proyecto: qué puede aportar la app y qué falta

### 3.1 Datos que necesita el plan frente a lo que existe hoy (2026-09-19)

| Dato que pide el plan | Estado | Consecuencia |
|---|---|---|
| Respuestas del SUS (10 ítems por participante) | La app **no** tiene encuesta; el cuestionario es del investigador (papel o formulario) | Plantilla de captura (Apéndice A). La app no interviene. |
| Categoría de la IA y confianza por reporte | ✅ En el teléfono (Room) y en el servidor (`categoria_ia`, `confianza_ia`), **aparte** de la categoría confirmada (CORR-010) | Base del acuerdo IA–observador. **Congelar** el modelo (`sha256_tflite` en `metadata.json`) y el umbral (hoy 0,70, provisional) durante la ventana. |
| Agrupaciones (centroide, n.º de reportes, categoría predominante) | ❌ La Fase 2 (DBSCAN, A14) y `Punto_critico` no existen | El Bloque A y el D de la ficha no se pueden preparar aún; falta la regla de «categoría predominante» (§5.2.5). |
| Cómo se registra la ficha de campo | ❌ Hueco A18 sin resolver | Captura estructurada por observador (Apéndice A) para poder calcular κ. |
| Fotos de los reportes | ✅ Cloudinary (privadas, URL firmada) | Calificación por dos evaluadores (Bloque E) sin exponer el código anónimo. |
| **Precisión declarada** de la lectura GPS de cada reporte | ❌ Se usa para filtrar y se **descarta**: ni `Reporte` ni el servidor la guardan (el ER de `diseno.md` §6 prevé `error_posicion_m`, no está implementado) | I3 (Bloque B, columna «Precisión declarada») solo se puede llenar leyéndola en pantalla (`IndicadorGps`). Ver §9, I-1. |
| Lecturas GPS **bloqueadas** por el filtro | ❌ No dejan rastro (solo el mensaje) | No se puede evaluar cuántas lecturas buenas se bloquean sin un registro de lecturas. Ver §9, I-3. |
| Coordenadas de cada medición de I3 | Parcial: `latitud/longitud` del reporte guardado, en Room y en Neon (por `id_local`) | Se sacan de Neon (consulta de [`pruebas-de-confiabilidad.md`](./pruebas-de-confiabilidad.md) §6). Solo de las lecturas **aceptadas**. |
| Seguimiento del envío por reporte | ✅ Guardado en el teléfono ([ADR-0006](./adr/0006-envio-confiable-de-reportes.md)) y, desde la Sesión 25 (2026-09-19), exportable como CSV desde Ajustes → «Compartir registro de envíos» (`RegistroDeEnvios.kt`, escenario H11 de `prueba-en-dispositivo.md`; **sin ejecutar en un dispositivo**). Columnas: `id_local`, `capturado_utc`, `categoria`, `categoria_ia`, `confianza_ia`, `estado`, `intentos`, `ultimo_intento_utc`, `enviado_utc`, `segundos_hasta_envio`, `motivo_ultimo_fallo`, `codigo_http`, `red_al_guardar`, `red_ultimo_intento`. **Sin coordenadas ni precisión, a propósito** | Basta para la disponibilidad (`¿Sincronizado?`); no para el error de posición. |
| Retardo captura → recepción | ✅ `fecha_hora` y `recibido_en` en Neon | Se obtiene de todos los reportes, sin exportar nada del teléfono. |
| Categorías de red de la ficha (4G-5G, 3G-H+, Sin red) | ❌ La app distingue `WIFI`, `DATOS_MOVILES`, `SIN_INTERNET`, `SIN_RED`, `OTRA`; no la generación de la red | Equivalencias en §5.3.7; la generación se anota a mano ([CORR-013](./correcciones-documento.md)). |

### 3.2 Cosas de `pruebas-de-confiabilidad.md` que hay que alinear

Ese documento (aún sin confirmar en git al escribir esto) cubre la confiabilidad **del software**; no lo duplico, pero para que ambos encajen:

- Cita los escenarios **H1–H10** de `prueba-en-dispositivo.md`: esta mañana no existían; la Sesión 25 los añadió el 2026-09-19 (con H11, el registro de envíos). Ya están alineados.
- Sus condiciones de red **R1–R6**, las tres categorías de la ficha y `TipoDeRed` no coinciden: §5.3.7 propone las equivalencias.
- Propone **N = 40 por condición**; la ficha suma 40 observaciones **en total**: con tres condiciones son ≈ 13 por condición (§7.4).
- Los intervalos de Wilson de su §7 los recalculé: coinciden.

### 3.3 Lo que condiciona el calendario

La ventana (21-sep → 15-nov) empieza en dos días y siguen pendientes la ruta crítica del [tablero](./avance.md) (A10, A19, D9, D5, F2) y las piezas de la Fase 2 (A14, A18). El piloto del SUS necesita personas que **ya usaron la app**; las mediciones de I3, el GPS real (D5); la doble observación de I2, las agrupaciones. Por eso el plan (§8) se expresa en semanas relativas al inicio **real** de la ventana.

## 4. Reglas comunes (el «reglamento» de las pruebas)

1. **Datos reales o nada.** Ningún dato simulado se presenta como resultado (§2.2).
2. **Congelar el plan antes de medir.** Criterios, tamaños, reglas de exclusión y de desempate se fijan y se confirman en git (fecha visible) **antes** de la primera medición. Es la misma lógica de *«fijar el tope antes de ver el resultado»* que ya usa el proyecto para el umbral de la IA.
3. **Todo coeficiente con su IC 95 % y su n.** Un punto solo no dice nada con estos tamaños (§7). Reportar con la lista GRRAS (Kottner et al., 2011).
4. **Independencia.** Los observadores no se consultan ni ven la ficha del otro; miden en la misma ventana de tiempo; los evaluadores de fotos trabajan por separado.
5. **Ciego respecto de la IA.** Quien observa en el terreno **no** ve la categoría de la IA; el Bloque D lo completa un analista en gabinete con una regla fija. Si el observador la conoce, se infla tanto el acuerdo IA–observador como el interobservador (anclaje).
6. **Sin descartes a posteriori.** Solo se excluye una unidad con una regla escrita de antemano (p. ej. «el foco fue recogido entre las dos visitas») y se informa cuántas y por qué.
7. **Un fracaso es un resultado.** Si un κ < 0,61 no se «arregla» el dato: se revisa la definición, se capacita, se repite con **unidades nuevas** y se reportan las dos rondas.
8. **Congelar versiones.** App (commit), modelo (`sha256`), teléfonos y sistema operativo se anotan en cada tanda; un cambio abre una tanda nueva. **Nunca** con la variante de ubicación simulada: devuelve coordenadas fijas con 5 m de precisión y produciría una repetibilidad perfecta falsa.
9. **Datos crudos fuera de git.** Fotos, fichas con códigos de participantes y coordenadas viven en una carpeta privada (OneDrive); en git van los scripts y las tablas agregadas. (Los reportes de prueba llegan a la misma base que los reales: ver la lista de preparación de `pruebas-de-confiabilidad.md`.)
10. **Confiabilidad ≠ validez ≠ exactitud.** Un instrumento puede ser muy consistente y medir mal; cada resultado se etiqueta con lo que es.

## 5. Protocolo por instrumento

### 5.1 Instrumento 1 · SUS: consistencia interna (alfa de Cronbach)

**5.1.1 Qué se mide.** Si los 10 ítems se comportan como una sola escala de usabilidad percibida. El alfa es apropiado porque el puntaje es la suma de ítems Likert.

**5.1.2 Participantes.** Los del Bloque 0 del Anexo 3 (mayores de 18, Wanchaq, usaron la app al menos una vez, no trabajan en limpieza pública, con consentimiento).

**5.1.3 Procedimiento.**

1. **Piloto** (tarea D10/G6): 15 personas como mínimo (la Tabla 5), **20 recomendado**. Después de **al menos un reporte real**, aplican el cuestionario y una **entrevista cognitiva de 5 minutos**: «con sus palabras, ¿qué le pregunta el ítem 2? ¿y el 6?» (los ítems que unen dos ideas: 2, 3, 6, 8). Se anota el tiempo de llenado. **No se modifica el instrumento**: si aparece un problema, va al asesor (un cambio invalida el juicio de expertos ya emitido; ver la tabla de tipos de contenido en `correcciones-documento.md`).
2. ¿Cuentan los del piloto dentro de los 38? *Propuesta*: **no** (probaron una versión anterior de la app); si la app y el cuestionario no cambian entre piloto y ventana, pueden sumarse, pero **se decide antes** y se reportan las dos cifras.
3. **Aplicación final** (n = 38, muestreo por conveniencia): encuestador que **no** explica los ítems ni influye; una sola vez por participante; código P-###; se anotan versión de la app y n.º aproximado de reportes.
4. **Captura**: plantilla CSV (Apéndice A) con las 10 respuestas crudas (1–5); se valida rango y que estén los diez (si falta uno, el cuestionario es *Nulo*, Anexo 3).

**5.1.4 Análisis** (en este orden):

1. Puntaje SUS de cada participante (procedimiento del Anexo 3) y estadísticos descriptivos.
2. **Recodificar**: contribución cᵢ = xᵢ − 1 en los ítems impares y 5 − xᵢ en los pares (son las «sumas parciales» del propio Anexo 3, en escala 0–4). **El alfa se calcula sobre c₁…c₁₀**, nunca sobre las respuestas crudas.
3. **α de Cronbach** con **IC 95 % de Feldt** (y, como control, IC por *bootstrap*).
4. **ω de McDonald** (robustez: el alfa supone ítems equivalentes; Hayes y Coutts, 2020).
5. **Correlación ítem-total corregida** (deseable ≥ 0,30), **alfa si se elimina el ítem** y correlación media entre ítems (0,15–0,50 es lo habitual). Sirve para *diagnosticar*, no para quitar ítems: el SUS exige los diez.
6. **Error estándar de medida** SEM = DE × √(1 − α), en puntos SUS.
7. Subescalas: solo si el documento las va a reportar como puntajes (aprendizaje = 4, 7, 10; uso = 2, 3, 5, 6, 8, 9): su alfa; «intención» (ítem 1) no tiene. Si no, se presentan como medias por ítem, sin hablar de confiabilidad.
8. Sesgo de aquiescencia: comparar la media cruda de los ítems positivos con la de los negativos.

**5.1.5 Criterio.** α ≥ 0,70 (el del documento; Nunnally, 1978). Se lee **con el intervalo**: con n = 38, para que el *límite inferior* llegue a 0,70 el alfa observado tiene que ser ≈ **0,81** (§7.1); si sale 0,75 el resultado correcto es «cumple el criterio puntual; el IC incluye valores < 0,70». Valores > 0,95 sugieren ítems redundantes (Tavakol y Dennick, 2011). El SUS estándar suele dar ≈ 0,9; si esta versión adaptada da mucho menos, es un dato sobre la adaptación.

**Si α < 0,70** (decidido de antemano): se reporta tal cual; se mira ítem por ítem con §5.1.4-5; se discute (n pequeño, heterogeneidad de participantes, ítems reformulados); **no** se eliminan ítems ni se rehace el puntaje; y se anota como limitación de la usabilidad medida.

**5.1.6 Sobre el test–retest.** *No* lo propongo como prueba principal: (a) el diseño es **transeccional** y el documento dice que no evalúa cambios en el tiempo; (b) el SUS mide la experiencia con la app, que **cambia con el uso** (aprendizaje, memoria del primer llenado); (c) volver a citar a los mismos vecinos en 7–14 días es costoso y pierde participantes. Si el asesor lo exige: un subgrupo (≥ 15–20), **7–14 días** después, **ICC de acuerdo absoluto** (Koo y Li, 2016) y gráfico de Bland–Altman del puntaje SUS, declarando que mide la *estabilidad condicionada al uso continuado*, no la confiabilidad del instrumento en sentido estricto.

### 5.2 Instrumento 2 · Ficha de Observación de Campo: acuerdo interobservador (kappa)

**5.2.1 Qué campos se someten a la prueba** (todos categóricos: el estadístico es el kappa; la Tabla 5 pide tres, aquí van todos los que alimentan un indicador o describen el foco):

| Bloque · campo | Escala | Estadístico | Papel |
|---|---|---|---|
| **B4** «AGRUPACIÓN CONFIRMADA» (≤ 50 m del centroide) | binaria | κ, PABAK, AC1 | **Principal** (indicador «≥ 70 %») |
| B1 «¿Se observa acumulación…?» | binaria | κ, PABAK, AC1 | Apoyo (B4 se deriva de B1 y B2: comprobar que sean coherentes) |
| B2 distancia al centroide (< 10 / 10–25 / 25–50 / > 50 m) | ordinal, 4 | κ ponderado | Apoyo |
| **C1** tipo de residuo predominante (7 opciones) | nominal | κ | **Principal** (base de la comparación con la IA) |
| C2 volumen estimado (pequeño / mediano / grande) | ordinal, 3 | κ ponderado (cuadrático) | Descriptivo, pero se necesita confiable para interpretarlo |
| C3 entorno urbano (6 opciones) | nominal | κ | Descriptivo |
| **E** por foto: «permite identificar el residuo», «sin rostros ni placas» | binaria, **por foto** | κ | Base de «fotografías válidas ≥ 95 %» |
| E «registros completos» | objetivo (coordenadas, fecha, hora, categoría) | — (se cuenta desde la base, sin juicio) | No requiere kappa |

**5.2.2 Diseño.**

1. **Manual de codificación** (tarea G5): definiciones operativas de cada categoría con ejemplos fotográficos y reglas de desempate (p. ej. *predominante* = el material que ocupa más de la mitad del volumen visible; si ninguno, *Mixto*; cómo estimar «≤ 50 m»). **No cambia la redacción de la ficha** (validada): la complementa.
2. **Capacitación** conjunta (≈ 2 h) con 5 sitios de práctica que **no** entran en el cálculo.
3. **Estudio de calibración**, *antes* de la Fase 2: 2 observadores × **≥ 20 sitios** con acumulación visible. No necesita agrupaciones ni la app: sirven los puntos críticos del inventario municipal (si el listado trae ubicación: no lo verifiqué) u otros sitios. Se calcula κ; si algún campo principal queda < 0,61, se ajusta el manual, se recapacita y se repite con sitios **nuevos** (máximo 2 rondas; se reportan todas). Las mismas visitas dejan **fotos estándar** (panorámica, media, detalle) para el paso 5.
4. **Estudio final (Fase 2)**: los mismos 2 observadores sobre **todas** las agrupaciones (15–30 según la Tabla 2). Este es el κ que se reporta como confiabilidad de la ficha. Si de verdad no se pudiera, el mínimo del documento es 10 (*propuesta*: no menos de 20; §7.2).
5. **Intraobservador (test–retest de la ficha)**: cada observador vuelve a llenar el Bloque C (C1–C3: tipo, volumen y entorno, lo único que se puede juzgar desde una foto; la distancia al centroide no) sobre las **fotos estándar de la calibración** tras **≥ 14 días**, sin ver su primera respuesta → κ intraobservador. Hay que hacerlo con la calibración y no con las agrupaciones finales: no habría tiempo (§8).
6. **Fotos (Bloque E)**: 2 evaluadores califican por separado **todas** las fotos de las agrupaciones (o al menos 100) → κ por criterio. No hay confusión temporal (la foto no cambia) y es la parte más barata y la de mayor n. Es la misma «curaduría» que el documento ya prevé en Ética.

**5.2.3 Reglas de conducta en el terreno.**

- **Independientes**: cada uno llena su ficha sin consultar; si van juntos, no hablan hasta entregar ambas.
- **Misma condición temporal**: mismo día y ventana de ≤ 2 h. Si entre visitas hubo un cambio real (recojo, camión), se anota y la unidad se **excluye del κ** (regla previa, §4.6).
- **Ciegos a la IA** (§4.5): no consultan la categoría del sistema; el **analista** llena el Bloque D desde la base.
- **Mismos dos observadores en todas las unidades** (así corresponde Cohen). Si no, Fleiss o α de Krippendorff.
- *Sugerencia*: que los observadores no sean quien entrenó o configuró el clasificador, o al menos que no conozcan su salida.
- **Medir** la distancia al centroide con la misma herramienta (pin del centroide en el mapa del teléfono + medidor), no «a ojo» cada quien.

**5.2.4 Análisis.** Para cada campo: tabla de contingencia, % de acuerdo, **κ de Cohen** (ponderado cuadrático en los ordinales) con **IC 95 %** (*bootstrap* por unidades, B = 5 000, y el intervalo analítico como control), distribuciones marginales de cada observador, y lista de los desacuerdos con su causa. **Si un campo tiene una categoría con > 80 % o < 20 %**, o un observador usó una sola categoría (κ indefinido), se añaden **PABAK** y **AC1 de Gwet** y se explica por qué (la «paradoja del kappa»: acuerdo alto, kappa bajo; Feinstein y Cicchetti, 1990). Sensibilidad: α de Krippendorff.

**5.2.5 Criterio** *(propuesta)*: **κ ≥ 0,61** («sustancial», Landis y Koch, 1977) para los campos principales (B4, C1); objetivo deseable ≥ 0,70 (Terwee et al., 2007, para κ ponderado e ICC). Entre 0,41 y 0,60: «moderado», se usa con reserva y se dice. < 0,41: el campo **no es confiable** con las definiciones actuales. Se informa siempre el límite inferior del IC: «κ = 0,68, IC [0,42; 0,87]» no es lo mismo que «κ = 0,68».

**Acuerdo IA–observador (Bloque D, indicador «≥ 70 %»): es *exactitud*, no confiabilidad**, pero el documento usa kappa aquí y se calcula con la misma tabla:

- Unidad: la agrupación. «Sistema» = categoría **cruda** de la IA (`categoriaIa`) predominante en la agrupación; «observador» = la categoría en la que **coinciden** los dos observadores (si discrepan, se resuelve con un tercero según una regla fijada antes).
- **Regla de «predominante» a fijar de antemano** *(propuesta)*: la moda de `categoriaIa` (empates → mayor confianza media); si la moda no llega al 50 % de los reportes, «Mixto».
- La IA **nunca** puede decir Escombros, Mixto ni Otro (`categoriaIa` solo tiene 4 clases): el máximo alcanzable es la fracción de agrupaciones observadas en esas cuatro. Se reportan **dos** análisis (todas las agrupaciones / solo las observadas en las 4 clases): decisión F3 con el asesor.
- Se informan % de coincidencia, κ y matriz de confusión con IC de Wilson. **Ojo con la regla de decisión**: con 20 agrupaciones, «justo 70 %» (14/20) tiene IC 95 % [48,1 %; 85,5 %], y para que el *límite inferior* llegue a 70 % harían falta 19/20 (§7.2). Hay que acordar antes si el criterio se aplica al valor observado o al límite inferior.
- Para ganar precisión (y sin más trabajo de campo), una muestra de **≥ 150 fotos** de reportes calificadas por dos evaluadores da la exactitud **por foto** con IC estrecho. Sin reentrenar el modelo con ellas (el Anexo 9 no lo cubre).
- Los errores aleatorios de una foto se **diluyen** al tomar la categoría predominante; los **sistemáticos no** (un muro que siempre se llama «escombros» sigue ganando la votación): por eso conviene mirar también la matriz de confusión por foto.

**5.2.6 Límites** (dichos sin rodeos): n pequeño y IC ancho; los observadores son autores del estudio (no son ajenos a él); un foco puede cambiar entre visitas; con prevalencia extrema el kappa engaña; el kappa mide acuerdo **entre estos dos observadores**, no que la ficha sea «correcta».

### 5.3 Instrumento 3 · Ficha de Verificación Geoespacial: repetibilidad y concordancia del filtro

**5.3.1 Qué se mide.** La Tabla 5 pide «repetibilidad, no consistencia interna». Son cuatro preguntas distintas, con su estadístico:

| Pregunta | Estadístico |
|---|---|
| **P1 · Repetibilidad**: si mido el mismo punto varias veces, ¿cuánto varían las lecturas? | DE intra-punto por eje, DE agrupada de los 8 puntos, **límite de repetibilidad**; ICC(1,1) del error |
| **P2 · Concordancia del filtro**: ¿el filtro de 15 m acepta lo que de verdad está a ≤ 15 m y bloquea lo demás? | % de acuerdo, κ, VPP (de lo aceptado, cuánto está de verdad a ≤ 15 m) |
| **P3 · Honestidad de la precisión declarada**: ¿el radio que informa el teléfono contiene al error real con la frecuencia que promete? | **Cobertura**: % de lecturas con error real ≤ precisión declarada |
| **P4 · Confiabilidad de la referencia**: ¿la coordenada «verdadera» (Google Earth Pro) es repetible? | Dispersión entre fechas de imagen y entre dos lectores; repetición de las líneas base con wincha |

**5.3.2 Diseño de las mediciones.**

- **8 puntos** de representatividad territorial (Anexo 5); *propuesta*: 3 a cielo abierto, 3 parcialmente obstruidos y 2 en cañón urbano (la ficha ya registra la obstrucción), a nivel del suelo.
- **5 mediciones independientes por punto** (Anexo 5). «Independiente» debe significar algo: **≥ 10 min entre una y otra**, en **al menos dos franjas horarias**, y **cerrando la app y esperando una lectura nueva** cada vez (no reutilizar una lectura en caché). Cinco lecturas seguidas subestiman la variabilidad porque la geometría de los satélites casi no cambia.
- **≥ 2 modelos de teléfono** *(recomendado)*: la sección de Limitaciones del documento reconoce que la precisión varía por equipo. Con un solo equipo, la repetibilidad vale para **ese** equipo y se dice. La Galaxy Tab S9+ es una tableta: úsese además un teléfono.
- **Variante normal de la app** (nunca la de ubicación simulada), con permiso de ubicación precisa, misma versión y commit en todas las tandas.
- Se anota también los **segundos hasta que la app acepta una lectura** (desde [ADR-0007](./adr/0007-gps-en-caliente-y-foto-reducida.md) la app espera hasta 30 s a una lectura ≤ 15 m: `RastreadorDeUbicacion`): es parte de la confiabilidad que percibe el ciudadano y la ficha no lo pide (va en la columna de observaciones).
- **Referencia**: la coordenada se lee sobre un rasgo a nivel del suelo en **≥ 2 fechas de imagen** y por **dos lectores** independientes; se registra la dispersión (ya está en el Bloque A).

**5.3.3 Cálculos** (con las coordenadas de cada lectura y la de referencia, pasadas a metros en un plano local: Δnorte = R·Δφ, Δeste = R·Δλ·cos φ₀):

1. **Error** de cada medición: distancia de Haversine a la referencia (Bloque C de la ficha).
2. **Por punto**: desplazamiento medio (**sesgo**, la «exactitud» de ese punto), **DE por eje** de las cinco lecturas (**precisión** de ese punto), mediana y máximo del error.
3. **DE agrupada** s_w = √(promedio de las varianzas por punto), con 8 × 4 = 32 grados de libertad, e IC 95 % por ji-cuadrado (§7.3). *No* se interpreta punto por punto: una DE de 5 lecturas tiene un IC de 0,6× a 2,9× su valor.
4. **Límite de repetibilidad**: la diferencia esperada entre dos lecturas independientes del mismo lugar, con 95 % de probabilidad. Por eje: r = 2,77 · s_w (Bland y Altman; ISO 5725 usa 2,8). Como distancia en el plano (Rayleigh): **P95 ≈ 3,46 · σ_eje**.
5. **Lo que dice literalmente la Tabla 5**: la **DE de las cinco mediciones por punto** (del *error*, en m). Se reporta también, junto a la anterior, y se explica la diferencia (el error es una distancia, siempre positiva y asimétrica).
6. **ICC(1,1)** del error entre los 8 puntos (un solo factor aleatorio, cinco réplicas): cuánto de la variación del error es *del lugar* y cuánto es ruido de repetición. IC amplio con 8 puntos (§7.3). Bandas de Koo y Li (2016).
7. **Indicadores del Anexo 2** con estos mismos datos: mediana ≤ 10 m, P90 ≤ 15 m (con IC por *bootstrap*), % aceptado por el filtro ≥ 90 %, error de distancia relativa ≤ 10 % (Bloque D, 4 líneas base).

**5.3.4 Concordancia del filtro (P2) y cobertura (P3).**

- Filtro «acepta» ⇔ precisión declarada ≤ 15 m. Verdad ⇔ error real ≤ 15 m. Tabla 2 × 2 → % de acuerdo, κ, sensibilidad, especificidad y **VPP**, con IC de Wilson.
- **Cobertura**: la documentación de Android define la precisión de `Location.getAccuracy()` como el **radio del 68 % de confianza**; si esa precisión fuera honesta, ≈ 68 % de las lecturas tendrían un error real menor o igual que la declarada. Se reporta el % observado con su IC. *(Esa definición no la pude releer en la página oficial desde aquí: verificar antes de citarla.)*
- **Por qué importa para la tesis**: el radio de agrupamiento ε = 50 m supone que dos reportes del mismo foco quedan a menos de 50 m. Con el filtro en su límite (15 m declarados ⇒ σ por eje ≈ 9,9 m), dos lecturas independientes del mismo sitio están a más de 34 m solo el 5 % de las veces y a más de 50 m ≈ 0,1 % (modelo circular, sin sesgo; §7.3). **Pero si el error real fuera el doble del declarado** (declaración optimista, típica en cañón urbano), la separación típica se duplica (P95 ≈ 69 m) y los reportes de un mismo foco **se partirían en dos agrupaciones**. La cobertura es el dato que dice en qué caso estamos.
- Los bloqueos **solo se pueden evaluar** si se registran (§9, I-3); con lo que hay hoy, solo se ve la mitad aceptada.

**5.3.5 Criterios** *(propuesta; el documento no fija ninguno para la repetibilidad)*:

- **R1 (repetibilidad)**: σ por eje agrupada ≤ **10 m** (equivale a un P95 de la separación entre dos lecturas ≤ 35 m, cómodamente bajo ε = 50 m, y no peor que lo que el propio filtro de 15 m supone).
- **R2 (concordancia)**: VPP del filtro ≥ **90 %** (de lo aceptado, al menos el 90 % está de verdad a ≤ 15 m).
- **R3 (cobertura)**: se **reporta** sin umbral; si es < 50 % se marca la precisión declarada como optimista y se discute endurecer el filtro (decisión del asesor: el documento fija 15 m).
- **R4 (referencia)**: dispersión entre fechas de imagen y entre lectores ≤ **3 m**; si no, el punto no sirve como referencia.

**5.3.6 Líneas base (Bloque D).** Cada línea con wincha **dos veces** (dos personas) y las coordenadas de sus extremos con **≥ 3 lecturas** cada uno; se compara la distancia calculada con la medida (≤ 10 %). Con 4 líneas es un chequeo, no una estimación.

**5.3.7 Disponibilidad operativa (Bloques B y C: `¿Guardado?`, `¿Sincronizado?`).** Es la confiabilidad **del software**: el método está en [`pruebas-de-confiabilidad.md`](./pruebas-de-confiabilidad.md). Solo hay que alinear las condiciones:

| Ficha (Anexo 5) | La app anota (`TipoDeRed`) | Condición de `pruebas-de-confiabilidad.md` |
|---|---|---|
| 4G-5G | `DATOS_MOVILES` (+ generación **a mano**) | R2 |
| 3G-H+ | `DATOS_MOVILES` (+ generación **a mano**) | R2 / R5 (señal débil) |
| Sin red | `SIN_RED` | R3 (después, con red) |
| *(no existe en la ficha)* | `WIFI` | R1 |
| *(no existe en la ficha)* | `SIN_INTERNET` | R4 |

Con 40 observaciones repartidas en tres condiciones quedan ≈ 13 por condición: **13 de 13 no basta** para *demostrar* «≥ 95 %» (límite inferior de Wilson 77 %); harían falta 73 de 73 (§7.4). Por eso la propuesta de ese documento (N = 40 **por condición**) va **además** de la ficha, y hay que fijar de antemano si «≥ 95 %» se aplica al valor observado o a su límite inferior.

### 5.4 Pruebas complementarias (según corresponda)

| Prueba | Qué responde | Cómo | Costo | Cuándo |
|---|---|---|---|---|
| **Estabilidad del agrupamiento** (Variable 2, «calidad del agrupamiento») | ¿Las agrupaciones son un rasgo de los datos o del azar/de los parámetros? | (a) Sensibilidad: ε ∈ {30, 40, 50, 60, 75} m × MinSamples ∈ {3, 4, 5} → n.º de agrupaciones, % ruido, silueta. (b) Remuestreo: 200 submuestras del 80 % de los reportes → **Jaccard medio** por agrupación (Hennig, 2007): ≥ 0,75 estable, ≥ 0,85 muy estable, ≤ 0,5 se disuelve. (c) Mitades temporales (semanas 1–4 frente a 5–8): % de agrupaciones que reaparecen (centroide a ≤ ε) | Cero campo; código en la Fase 2 (A14) | Con A14 |
| **Pureza de la categoría por agrupación** | ¿Reportes independientes del mismo foco reciben la misma categoría de la IA? | % de reportes de cada agrupación cuya `categoriaIa` coincide con la predominante. Descriptivo: una pila mixta *puede* ser heterogénea de verdad | Cero campo | Con A14 |
| **Reproducibilidad del clasificador** | ¿La misma foto da la misma clase en la app dos veces, en otro teléfono y con la referencia en Python? | ≥ 60 fotos reales de calle (C5): % de coincidencia de la clase superior, κ y máxima diferencia de probabilidad. Incluir **foto original frente a foto reducida** (1600 px, JPEG 85, sin EXIF: [ADR-0007](./adr/0007-gps-en-caliente-y-foto-reducida.md) declara que no se comparó). Enlaza con C9 y con `ClasificadorResiduoReferenciaTest` | Bajo | Con C5/D7 |
| **Acuerdo entre jueces** (validez) | ¿Los expertos coinciden? | Recalcular la V de Aiken (§2.3) y, opcional, W de Kendall | Cero | Antes de la defensa |

## 6. Estadísticos: definiciones y cómo leerlos

| Estadístico | Definición | Cómo se lee (fuente) |
|---|---|---|
| **Alfa de Cronbach** | α = k/(k−1) · (1 − Σσ²ᵢ / σ²ₜ) | ≥ 0,70 aceptable (Nunnally, 1978); 0,70–0,95 recomendado (Tavakol y Dennick, 2011). **IC de Feldt**: (1−α)/(1−α̂) ~ F(n−1, (n−1)(k−1)), o sea [1 − (1−α̂)·F₁₋γ/₂ ; 1 − (1−α̂)·Fγ/₂] |
| **Error estándar de medida** | SEM = DE · √(1 − α) | Cuánto se mueve un puntaje individual por error de medida, en puntos SUS |
| **ω de McDonald** | Confiabilidad de la suma bajo un modelo de un factor | No exige ítems equivalentes (Hayes y Coutts, 2020) |
| **Kappa de Cohen** | κ = (Pₒ − Pₑ)/(1 − Pₑ) | 0,41–0,60 moderado, 0,61–0,80 sustancial, 0,81–1,00 casi perfecto (Landis y Koch, 1977). Depende de prevalencia y sesgo (Sim y Wright, 2005) |
| **Kappa ponderado** | Penaliza más los desacuerdos lejanos; pesos cuadráticos wᵢⱼ = (i−j)²/(c−1)² | Con pesos cuadráticos ≈ ICC (Fleiss y Cohen, 1973) |
| **PABAK / AC1** | PABAK = 2Pₒ − 1 (binario); AC1 de Gwet corrige la prevalencia extrema | Se usan **junto** al kappa, no en su lugar (Byrt et al., 1993; Gwet, 2008) |
| **ICC(1,1)** | (MSB − MSW)/(MSB + (k−1)MSW), un factor aleatorio | < 0,5 pobre; 0,5–0,75 moderado; 0,75–0,9 bueno; > 0,9 excelente (Koo y Li, 2016). Se declara la forma usada |
| **DE intra-punto y límite de repetibilidad** | s_w = √(media de varianzas por punto); r = 2,77·s_w | Diferencia esperada entre dos mediciones repetidas con 95 % de probabilidad (Bland y Altman, 1996; ISO 5725) |
| **Cobertura** | % de lecturas con error ≤ precisión declarada | Ideal ≈ 68 % si la precisión es un radio del 68 % |
| **Intervalo de Wilson** | Para una proporción; el mismo de `probar_modelo.py` | Válido con n pequeño y cerca de 0 % o 100 % |
| **Jaccard medio (estabilidad)** | Solapamiento de cada agrupación con la más parecida en las remuestras | ≥ 0,75 estable (Hennig, 2007) |

**Herramientas.** R (`psych`, `irr`, `irrCAC`), jamovi o JASP, o los scripts del repositorio (tarea G4). Con cualquiera: **comprobar la herramienta con un ejemplo publicado** antes de usarla (p. ej. la tabla de Shrout y Fleiss, 1979, para los ICC; el 2 × 2 clásico [[20, 5], [10, 15]] ⇒ κ = 0,40) y guardar la versión y el código usados.

## 7. Cuántos datos hacen falta

Cifras de [`evidencia/2026-09-19_dimensionamiento-confiabilidad.txt`](./evidencia/2026-09-19_dimensionamiento-confiabilidad.txt), generadas por [`analisis/confiabilidad/dimensionamiento.py`](../analisis/confiabilidad/dimensionamiento.py) (biblioteca estándar de Python, semilla fija, distribuciones comprobadas contra tablas conocidas). Son **planificación bajo supuestos declarados**, no resultados.

### 7.1 Alfa de Cronbach (k = 10): IC 95 % de Feldt según n y el alfa observado

| n | α observado 0,70 | α observado 0,80 | α observado 0,90 |
|---:|---|---|---|
| 15 | [0,41; 0,88] (±0,24) | [0,61; 0,92] (±0,16) | [0,80; 0,96] (±0,08) |
| 20 | [0,46; 0,86] (±0,20) | [0,64; 0,91] (±0,14) | [0,82; 0,95] (±0,07) |
| 30 | [0,51; 0,84] (±0,16) | [0,67; 0,89] (±0,11) | [0,84; 0,95] (±0,05) |
| **38** | [0,53; 0,82] (±0,14) | [0,69; 0,88] (±0,10) | [0,84; 0,94] (±0,05) |
| 50 | [0,56; 0,81] (±0,13) | [0,71; 0,87] (±0,08) | [0,85; 0,94] (±0,04) |

- El piloto de 15 apenas orienta: con un alfa de 0,80 el IC llega a 0,61. **Sirve para detectar ítems mal entendidos, no para certificar confiabilidad.**
- Con n = 38, el alfa observado debe ser **≥ 0,81** para que el límite inferior llegue a 0,70 (≥ 0,85 con n = 15).
- Subescalas (n = 38, alfa observado 0,70): aprendizaje (k = 3) [0,49; 0,83]; uso (k = 6) [0,52; 0,83].
- SEM del SUS con DE = 12,5 (la del documento): 6,8 / 5,6 / 4,0 puntos para α = 0,70 / 0,80 / 0,90.
- El tamaño de 38 del documento (n = (1,96·12,5/4)² = 37,5) es para estimar la **media** del SUS con margen de 4 puntos, no para el alfa; comprobado.

### 7.2 Kappa de Cohen entre dos observadores (binario, prevalencia de «Sí» = 0,70)

Cada celda: rango que cubre el **95 % de los κ observados** si el κ verdadero fuera el de la fila | probabilidad de observar κ ≥ 0,61 (10 000 estudios simulados; los κ indefinidos cuentan como «no aprueba»).

| κ verdadero | n = 10 | n = 20 | n = 30 | n = 50 |
|---:|---|---|---|---|
| 0,40 (malo) | [−0,25; 1,00] · 23 % | [−0,10; 0,78] · 15 % | [0,01; 0,73] · 12 % | [0,11; 0,66] · 6 % |
| 0,60 | [−0,15; 1,00] · 46 % | [0,13; 0,90] · 46 % | [0,25; 0,87] · 47 % | [0,34; 0,82] · 46 % |
| 0,75 (sustancial) | [0,00; 1,00] · 70 % | [0,33; 1,00] · 78 % | [0,43; 1,00] · 83 % | [0,52; 0,94] · 89 % |
| 0,90 | [0,38; 1,00] · 91 % | [0,62; 1,00] · 98 % | [0,67; 1,00] · 99 % | [0,74; 1,00] · 100 % |

Lecturas: (1) con **10 unidades el kappa casi no informa**; (2) aun con un acuerdo verdaderamente sustancial (0,75), la probabilidad de ver ≥ 0,61 es 70–83 % con 10–30 unidades: **hay un 17–30 % de riesgo de un «fallo» falso**; (3) con un acuerdo malo (0,40) hay 12–23 % de riesgo de un «aprobado» falso; (4) con prevalencia 0,85 y n = 20 el κ queda indefinido en 1–3 % de los estudios y, con κ verdadero 0,75, su rango es [0,00; 1,00] (tabla completa en la evidencia): ahí mandan PABAK y AC1. **Consecuencia**: doble observación de **todas** las agrupaciones y decidir con el IC, no con el punto; y no reentrenar ni «reparar» la ficha por un solo valor por debajo de 0,61.

**Regla de decisión de los indicadores «≥ 70 %»** (confirmación en campo e IA–observador; Wilson): con 20 agrupaciones, 14/20 (70 %) tiene IC [48,1 %; 85,5 %] y el límite inferior llega a 70 % con 19/20; con 30, 21/30 → [52 %; 83 %] y hacen falta 26/30. Hay que acordar antes cuál de las dos lecturas vale.

### 7.3 Repetibilidad del GPS (8 puntos × 5 mediciones)

- **DE de un solo punto** (4 gl): el valor verdadero está entre **0,60×** y **2,87×** la observada. **DE agrupada** (32 gl): entre **0,80×** y **1,32×**. Con 16 puntos o dos teléfonos (64 gl): 0,85×–1,21×.
- **ICC(1,1)** con 8 puntos × 5 réplicas, IC «típico» según el ICC verdadero: 0,50 → [0,19; 0,83]; 0,70 → [0,42; 0,92]; **0,80 → [0,57; 0,95]**; 0,90 → [0,76; 0,98].
- **Precisión declarada (radio del 68 %, A) y separación entre dos lecturas independientes del mismo sitio** (modelo circular sin sesgo; σ por eje = A/1,510; separación ~ Rayleigh):

| A declarada | σ por eje | P95 de la separación | P99 | P99,9 |
|---:|---:|---:|---:|---:|
| 5 m | 3,3 m | 11,5 m | 14,2 m | 17,4 m |
| 10 m | 6,6 m | 22,9 m | 28,4 m | 34,8 m |
| **15 m (el filtro)** | 9,9 m | **34,4 m** | 42,6 m | 52,2 m |
| 20 m | 13,2 m | 45,9 m | 56,9 m | 69,6 m |
| 30 m | 19,9 m | 68,8 m | 85,3 m | 104,5 m |

Si el error **real** fuera el doble del declarado, multiplíquese la separación por 2 (ε del agrupamiento = 50 m).

### 7.4 Proporciones (sincronización, filtro): intervalos de Wilson

13/13 → [77,2 %; 100 %] · 40/40 → [91,2 %; 100 %] · 38/40 → [83,5 %; 98,6 %] · 36/40 → [76,9 %; 96,0 %] · 73/73 → [95,0 %; 100 %]. **Se necesitan 73 aciertos sin fallo para que el límite inferior llegue a 95 %.**

## 8. Calendario (semanas relativas al inicio **real** de la ventana)

| Cuándo | Qué | Instrumento | Depende de |
|---|---|---|---|
| **Antes de medir** (ahora) | Decisiones D1–D10 (§12); congelar este plan en git; plantillas (Apéndice A); manual de codificación; scripts | todos | Asesor (G1) |
| Semana 0 | Capacitación de observadores; **calibración de I2 ronda 1** (2 obs. × ≥ 20 sitios, con fotos estándar). No necesita la app | I2 | G5 |
| Semanas 1–2 | **Piloto del SUS** (≥ 15, mejor 20) tras usar la app; **mediciones de I3** (8 puntos × 5, ≥ 2 teléfonos) | I1, I3 | D9 (primer reporte real), D5 (GPS real) |
| Semanas 2–3 | Calibración de I2 ronda 2 si algún campo principal < 0,61 (sitios nuevos) | I2 | Resultado de la ronda 1 |
| Semanas 4–5 | **Re-calificación intraobservador** (≥ 14 días después de la ronda 1) | I2 | Calibración |
| Semanas 3–8 | **SUS final** hasta n = 38; completar I3 (condiciones de red) | I1, I3 | App instalada en los participantes (E7 u otro canal) |
| Semana 8 (cierra el 15-nov) | Cerrar SUS; α final | I1 | |
| **Fase 2** (16-nov → ≈ 4-dic) | DBSCAN → lista **congelada** de agrupaciones → analista llena el Bloque D → **doble observación de campo** de todas → calificación de fotos → estabilidad del agrupamiento | I2, complementarias | A14, A18 |
| ≈ 5–9 dic | Análisis final con los scripts; tablas GRRAS | todos | |
| ≈ 10–12 dic | Redacción de §3.5.3 y resultados | | 12-dic (cierre del documento) |

**Riesgo principal**: si la Fase 2 empieza tarde no hay margen. Por eso lo que necesita esperar 14 días (intraobservador) se hace con la calibración, y lo que no necesita la app (calibración de I2) se hace **ya**. Si la ventana se corre (tarea F2), todo se corre con ella y se mantienen los **intervalos**.

## 9. Lo que se le pide a la app y al backend (instrumentación)

Solo propongo: es código que la Sesión 25 acaba de tocar (GPS en caliente, envíos, foto reducida: ADR-0007) y cada punto que cambie el contrato lleva su ADR.

| ID | Qué | Para qué | Estado / decisión |
|---|---|---|---|
| **I-1** | **Guardar la precisión declarada** de la lectura aceptada en el reporte (Room, migración 3→4; opcional en el payload y en el servidor como `error_posicion_m`, que `diseno.md` §6 ya prevé) | I3, Bloque B; describir la precisión de **todos** los reportes de la ventana (Población 1). **No se puede recuperar después**: los reportes ya creados no la tendrán | 🔴 decidir **antes** de la ventana si se quiere en los reportes reales. Cambia el contrato: ADR. Coordinar con el trabajo de GPS en curso |
| I-2 | Registro de envíos (CSV desde Ajustes) | Disponibilidad: `¿Sincronizado?`, red, intentos, segundos hasta el envío | ✅ Hecho en la Sesión 25 (sin ejecutar en dispositivo). Sin coordenadas ni precisión, a propósito |
| **I-3** | **Registro de lecturas GPS solo en depuración**: cada lectura (hora, lat, lon, precisión, aceptada/bloqueada), con el mismo doble seguro que la ubicación simulada | I3: evaluar los **bloqueos**, la cobertura y el tiempo hasta la lectura aceptada | 🟡 Propuesta. Alternativa sin código: anotar a mano lo que muestra `IndicadorGps` en cada medición |
| I-4 | Generación de red (4G/5G vs 3G) | Ficha 3, condición de red | ⚪ No vale el permiso adicional: anotar a mano |
| I-5 | Exportar por agrupación: centroide, n.º de reportes, `categoriaIa` predominante, regla de desempate | Bloques A y D | Parte de A14/A18 |
| I-6 | Formularios de captura con opciones cerradas y validación (Sheets/Forms o CSV) | Evitar «Otro» libres y celdas vacías | Apéndice A |

## 10. Texto propuesto para la sección «Confiabilidad de los instrumentos» (Cap. III)

**Versión A · para entregar ahora** (sustituye al párrafo con datos simulados; sin cifras inventadas). También hay que quitar la columna «Qué necesitas» de la Tabla 5.

> La confiabilidad de los instrumentos se evaluará con datos reales recogidos durante la ejecución del estudio, con el estadístico que corresponde a la naturaleza de cada uno (Tabla 5). Para el Instrumento 1 se calculará la consistencia interna mediante el alfa de Cronbach sobre los diez ítems, previa inversión de los ítems de enunciado negativo; para el Instrumento 2, la concordancia entre dos observadores independientes mediante el índice kappa de Cohen (ponderado en las variables ordinales) sobre las mismas agrupaciones; y para el Instrumento 3, la repetibilidad de las mediciones mediante la desviación estándar de las cinco lecturas por punto y el límite de repetibilidad, junto con la concordancia entre el filtro de precisión y el error medido. Los criterios de aceptación (alfa ≥ 0,70; kappa ≥ 0,61) se fijan antes de recolectar los datos, y cada coeficiente se reportará con su intervalo de confianza del 95 % y el tamaño de muestra en que se calculó. Si algún criterio no se cumple, se reportará como tal junto con su análisis.

**Versión B · plantilla para después de los datos** (los corchetes se llenan con la salida de los scripts):

> **Instrumento 1.** Con [n] participantes, el alfa de Cronbach fue [α] (IC 95 %: [inf; sup]); la correlación ítem-total corregida osciló entre [min] y [máx]; el error estándar de medida fue de [SEM] puntos SUS. [Cumple / no cumple] el criterio de 0,70. **Instrumento 2.** Con [n] agrupaciones observadas por dos observadores independientes, el kappa fue de [κ] (IC 95 %: [inf; sup]) para la confirmación del foco, [κ] para el tipo de residuo y [κ ponderado] para el volumen. **Instrumento 3.** Con 8 puntos y 5 mediciones por punto en [k] teléfonos, la desviación estándar agrupada por eje fue de [s_w] m (IC 95 %: [inf; sup]) y el límite de repetibilidad de [r] m; el filtro de 15 m tuvo un valor predictivo positivo de [VPP] % (IC 95 %: [inf; sup]).

Tabla de resultados sugerida (una por instrumento): campo · n · estadístico · IC 95 % · criterio · ¿cumple? · observaciones (desacuerdos y su causa).

## 11. Riesgos y límites (dichos sin rodeos)

- **Los n son pequeños.** Los estándares de la literatura piden ≥ 50 sujetos para un estudio de confiabilidad (Terwee et al., 2007); aquí serán 15–38 y 15–30. Los IC serán anchos y hay que decirlo; no se sustituye con simulaciones.
- **Los observadores son autores del estudio.** No son independientes de él; se mitiga con ciego a la IA y con reglas de conducta (§5.2.3), pero la limitación se declara.
- **Un foco cambia entre visitas** (recojo, camión): por eso la misma ventana horaria y la regla de exclusión.
- **La confiabilidad del GPS es de estos equipos, estos puntos y este momento**: la geometría de los satélites y el clima cambian; no se generaliza a «todos los teléfonos de Wanchaq».
- **Feldt supone ítems equivalentes y respuestas aproximadamente normales**: con Likert de 5 puntos es una aproximación (de ahí el ω y el *bootstrap* de control).
- **Todo esto sigue dependiendo de la ruta crítica** (§3.3). Sin app en manos de participantes no hay piloto; sin agrupaciones no hay Fase 2.
- **Confiabilidad no es exactitud.** Un κ alto entre observadores no demuestra que la clasificación de la ficha sea «correcta», solo que es reproducible.

## 12. Decisiones que necesita (asesor / usuario)

| # | Decisión | Quién | Propuesta |
|---|---|---|---|
| D1 | Sustituir el párrafo de datos simulados por el texto «previsto» (§10-A) y retirar «Qué necesitas» de la Tabla 5 | Asesor + Usuario | Sí, antes de entregar (G2, CORR-011) |
| D2 | Criterios: α ≥ 0,70 (documento); κ ≥ 0,61 (objetivo 0,70); criterios de I3 (§5.3.5) | Asesor | Los de este plan |
| D3 | I2: doble observación de **todas** las agrupaciones o ≥ 10 | Asesor | **Todas** (el mínimo, 20) |
| D4 | Quiénes observan (dos autores, ciegos a la IA) y si el piloto SUS cuenta dentro de los 38 | Usuario + Asesor | Dos autores que no entrenaron el modelo; piloto **aparte** |
| D5 | Categorías (F3): tratamiento de «Otro»/«Mixto»/«Desmonte» y regla de «categoría predominante» | Asesor | §5.2.5 |
| D6 | Guardar la precisión declarada por reporte (I-1) **antes** de la ventana; registro de lecturas en depuración (I-3) | Usuario | Sí a I-1 si hay tiempo; I-3 en depuración |
| D7 | Cómo se aplican «≥ 70 %» y «≥ 95 %»: valor observado o límite inferior del IC | Asesor | Fijarlo **antes** de medir (§7.2, §7.4) |
| D8 | Test–retest del SUS: no incluirlo (con la justificación de §5.1.6) o subgrupo | Asesor | No incluirlo |
| D9 | Al menos 2 modelos de teléfono para I3 | Usuario | Sí (un teléfono y la tableta) |
| D10 | Revisar la V de Aiken de los Anexos 10–12 (§2.3) | Asesor | CORR-012 |

## 13. Tareas (espejo del área G de [`avance.md`](./avance.md))

G1 decidir y congelar el plan · G2 sustituir la sección con datos simulados · G3 plantillas de captura · G4 scripts de análisis validados · G5 manual de codificación · G6 piloto del SUS · G7 calibración de I2 e intraobservador · G8 mediciones de I3 · G9 precisión declarada y registro de lecturas (I-1, I-3) · G10 SUS final (n = 38) · G11 Fase 2 de I2 · G12 estabilidad del agrupamiento · G13 redactar resultados reales · G14 revisar la V de Aiken con el asesor.

## Apéndice A · Diccionario de datos (plantillas CSV, UTF-8, una fila por lo que se indica)

| Archivo | Una fila por | Columnas (valores permitidos entre paréntesis) |
|---|---|---|
| `sus_respuestas.csv` | participante | `participante` (P-001…), `ronda` (piloto / final), `fecha`, `sector`, `encuestador`, `version_app`, `n_reportes_aprox`, `item01`…`item10` (1–5), `valido` (1/0) |
| `campo_observaciones.csv` | agrupación × observador | `agrupacion` (AG-###), `observador` (A/B), `ronda` (calibracion / final), `fecha_hora`, `b1_acumulacion` (SI/NO), `b2_distancia` (LT10 / 10_25 / 25_50 / GT50), `b4_confirmada` (SI/NO), `c1_tipo` (ORGANICO / PAPEL_CARTON / PLASTICO_PET / VIDRIO / DESMONTE / MIXTO / OTRO), `c1_otro_texto`, `c2_volumen` (PEQUENO / MEDIANO / GRANDE), `c3_entorno` (MERCADO / RESIDENCIAL / PARADERO / PARQUE / ESCUELA_SALUD / OTRO), `c3_otro_texto`, `cambio_entre_visitas` (SI/NO), `observaciones` |
| `campo_fotos.csv` | foto × evaluador | `agrupacion`, `id_reporte` (del servidor; **nunca** el código anónimo), `evaluador` (A/B), `identifica_residuo` (SI/NO), `sin_rostros_placas` (SI/NO) |
| `sistema_por_agrupacion.csv` (analista) | agrupación | `agrupacion`, `n_reportes`, `cat_ia_predominante`, `proporcion_moda`, `regla_desempate`, `centroide_lat`, `centroide_lon` |
| `gps_mediciones.csv` | punto × medición | `punto` (GPS-01…08), `medicion` (1–5), `fecha_hora`, `dispositivo`, `version_app`, `commit`, `lat_app`, `lon_app`, `precision_declarada_m`, `filtro` (ACEPTO/BLOQUEO), `guardado` (SI/NO), `sincronizado` (SI/NO), `hora_sincronizado`, `red_ficha` (4G_5G / 3G_H / SIN_RED), `obstruccion_cielo` (ABIERTO / PARCIAL / CANON), `segundos_hasta_aceptar`, `observaciones` |
| `gps_referencias.csv` | punto × lector × fecha de imagen | `punto`, `lector` (A/B), `fecha_imagen`, `lat_ref`, `lon_ref`, `rasgo` |

## Apéndice B · Referencias

✅ = comprobada en línea el 2026-09-19. ⚠️ = citada de memoria: comprobar antes de ponerla en el documento.

- ✅ Bangor, A., Kortum, P. y Miller, J. (2008). An empirical evaluation of the System Usability Scale. *Int. J. Human-Computer Interaction*, 24(6), 574–594 (α = 0,91).
- ✅ Feldt, L. S., Woodruff, D. J. y Salih, F. A. (1987). Statistical inference for coefficient alpha. *Applied Psychological Measurement*, 11(1) (pivote F con n−1 y (n−1)(k−1) gl).
- ✅ Hennig, C. (2007). Cluster-wise assessment of cluster stability. *Computational Statistics & Data Analysis* (umbrales de Jaccard 0,75 / 0,85).
- ✅ Koo, T. K. y Li, M. Y. (2016). A guideline of selecting and reporting intraclass correlation coefficients for reliability research. *J. Chiropractic Medicine*, 15(2), 155–163.
- ✅ Kottner, J. et al. (2011). Guidelines for Reporting Reliability and Agreement Studies (GRRAS). *J. Clinical Epidemiology*, 64(1), 96–106.
- ✅ Lewis, J. R. y Sauro, J. (2017). Revisiting the factor structure of the System Usability Scale. *J. Usability Studies*, 12(4).
- ✅ Sim, J. y Wright, C. C. (2005). The kappa statistic in reliability studies: use, interpretation, and sample size requirements. *Physical Therapy*, 85(3), 257–268.
- ✅ Terwee, C. B. et al. (2007). Quality criteria were proposed for measurement properties of health status questionnaires. *J. Clinical Epidemiology* (ICC o κ ponderado ≥ 0,70; n ≥ 50).
- ⚠️ Landis, J. R. y Koch, G. G. (1977). The measurement of observer agreement for categorical data. *Biometrics*, 33, 159–174.
- ⚠️ Cohen, J. (1960; 1968). Coeficiente kappa y kappa ponderado. Fleiss, J. L. y Cohen, J. (1973), equivalencia con el ICC.
- ⚠️ Feinstein, A. R. y Cicchetti, D. V. (1990). High agreement but low kappa. Byrt, T., Bishop, J. y Carlin, J. B. (1993), PABAK. Gwet, K. L. (2008), AC1.
- ⚠️ Bland, J. M. y Altman, D. G. (1996). Measurement error. *BMJ*, 313, 744; ISO 5725 (repetibilidad).
- ⚠️ Shrout, P. E. y Fleiss, J. L. (1979). Intraclass correlations: uses in assessing rater reliability. *Psychological Bulletin*, 86, 420–428.
- ⚠️ Nunnally, J. C. (1978). *Psychometric Theory*. Tavakol, M. y Dennick, R. (2011). Making sense of Cronbach's alpha. *Int. J. Medical Education*, 2, 53–55. Hayes, A. F. y Coutts, J. J. (2020), ω frente a α.
- ⚠️ Aiken, L. R. (1985); Penfield, R. D. y Giacobbi, P. R. (2004), intervalos de la V de Aiken.
- ⚠️ Lewis, J. R. y Sauro, J. (2009). The factor structure of the SUS. *HCI International*.

## Apéndice C · Qué está y qué no está verificado

- **Verificado (con el script o releyendo el documento y el código):** los datos de los Anexos 10–12; que la V de Aiken de los anexos no es la V (10/10, 0/10 y 3/10 coinciden con el promedio); los kappa imposibles (315 y 672 tablas enumeradas); los intervalos de §7 (las distribuciones F y ji-cuadrado se comprueban contra tablas al arrancar el script y los Wilson coinciden con `pruebas-de-confiabilidad.md`); qué guarda hoy la app y el servidor (`ReporteEntity`, `GeoJsonMapper`, `models.py`).
- **No verificado:** que `Location.getAccuracy()` sea el radio del 68 % (no se pudo abrir la página oficial; la búsqueda solo lo confirmó para otras APIs); si el inventario municipal de 25 puntos críticos trae coordenadas; la comparación de los ítems del SUS con la versión en español que se tomó como base (la hice contra el texto inglés); que la recodificación de los porcentajes a bandas sea la que usó la ficha institucional (es la que imprime la ficha); el estado del trabajo de la Sesión 25 (envíos, GPS, foto reducida): lo leí en el árbol de trabajo hacia las 11:45 y puede haber cambiado desde entonces.
- **Nada de este plan se ha ejecutado con datos reales.**
