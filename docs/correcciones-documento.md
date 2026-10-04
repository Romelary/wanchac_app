# Correcciones al documento de investigación

> Bitácora de divergencias entre el sistema que estamos construyendo y el documento `Proyecto de Investigación v5.91 (1) (1).docx`. No se edita el `.docx` por cada entrada individual — se aplican en lotes, en los puntos de control que se indican en [`plan.md`](./plan.md) (cierre de módulo o acumulación de 3-4 correcciones relacionadas). Cuando una entrada pasa a `aplicado`, se referencia el archivo `.docx` resultante y la fecha.

## Qué se puede corregir libremente y qué no

| Tipo de contenido | Ejemplos | Regla |
|---|---|---|
| **Flexible** — narrativa de diseño | Capítulo IV completo: Tablas 6-13 (RF, RNF, casos de uso, entidades), wireframes, historias de usuario, diagramas | Se corrige con confianza cuando contradice Cap. I–III. No requiere re-validación externa. |
| **Con validación externa ya emitida** | Anexos 3, 6–12: instrumentos (SUS, ficha de campo, ficha GPS), consentimiento informado (Anexo 9), fichas de juicio de expertos y coeficientes V de Aiken (Tablas 16-35) | **No se edita en silencio.** Cualquier cambio de fondo invalida el juicio de expertos ya emitido. Requiere visto bueno explícito del asesor y, probablemente, una nueva ronda de validación. |

## Estados posibles

`detectado` → `pendiente de aplicar` → `aplicado`

## Estado de la auditoría

**Auditoría completa del Capítulo IV realizada el 2026-09-17**, incluyendo el contenido real de las Tablas 6 a 13 (antes no visible, solo se tenía el texto de párrafos). Resultado: 8 correcciones (CORR-001 a CORR-008), más CORR-009 (idioma, Módulo 7) y CORR-010 (sugerencia de la IA vs. categoría confirmada, Módulo 3b), ambas detectadas el 2026-09-18. No se han aplicado cambios al `.docx` todavía — todas están `pendiente de aplicar`.

**Auditoría de la confiabilidad de los instrumentos (2026-09-19, Cap. III y Anexos 2–12)**: CORR-011 a CORR-013, más la última observación de la lista de abajo. Detalle, evidencia y plan para sustituir lo que no se sostiene en [`plan-confiabilidad-instrumentos.md`](./plan-confiabilidad-instrumentos.md) §2.

---

## Registro de correcciones

### CORR-001 — Eliminación del login / cuenta de usuario

- **Sección afectada**: Tabla 6 (RF-01, RF-07), Tabla 7 (CU-01, CU-02, CU-10), Tabla 8/"Entidad Usuario" (Tabla 9 del documento), wireframe "Pantalla de inicio / autenticación", historia de usuario "cerrar mi sesión".
- **Tipo de contenido**: Flexible.
- **Divergencia**: el Cap. IV especifica registro/login con correo y contraseña (RF-01: *"El sistema debe permitir el registro y autenticación de usuarios mediante correo electrónico y contraseña"*; CU-01/CU-02/CU-10: registrar cuenta, iniciar sesión, cerrar sesión) y una entidad `Usuario` con columnas `nombre`, `apellido`, `correo` (UNIQUE) y **`contraseña` tipo `TEXT`, sin especificar hash ni cifrado**. Esto contradice la Ética (Cap. I, 3.4) y el Anexo 9 ya validado, que exigen minimización de datos y un identificador anónimo generado en el dispositivo, sin nombre, correo ni contraseña.
- **Hallazgo adicional de seguridad**: independientemente del conflicto ético, almacenar la contraseña como `TEXT` plano (sin hash) sería una vulnerabilidad grave si el login se mantuviera. Se documenta aquí por si el asesor decide conservar algún tipo de cuenta — en ese caso, nunca debe guardarse en texto plano.
- **Justificación / referencia**: [`adr/0001-sin-login-id-anonimo.md`](./adr/0001-sin-login-id-anonimo.md), ya reflejado en `diseno.md` §0 (punto 1-3) y §6 (modelo de datos corregido).
- **Corrección propuesta**: ver ADR-0001. En resumen: eliminar RF-01, CU-01, CU-02, CU-10; reescribir RF-07 sin "ID del usuario" de cuenta (usar `id_anonimo`); reescribir la entidad `Usuario` a solo `id_anonimo` + `fecha_consentimiento`.
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-17

---

### CORR-002 — Rol "Administrador" sin sustento en la población del estudio

- **Sección afectada**: Tabla 7, CU-09 ("Gestionar Base de Datos", actor Administrador); historia de usuario "Como administrador, quiero visualizar todos los puntos en un mapa base"; CU-08 menciona actor "Analista" también sin sustento.
- **Tipo de contenido**: Flexible.
- **Divergencia**: CU-09 describe un actor "Administrador" que *"permite revisar, editar o eliminar registros de imágenes inconsistentes"* directamente sobre la base de datos. Esto contradice: (a) §3.2 "Sobre las entidades gestoras": *"estas entidades no determinan el diseño del sistema ni la definición de la población. La visibilidad pública, no la administración interna, es el principal propósito de los datos"*; (b) la Población 2 del estudio (§3.2) solo define dos perfiles — vecinos residentes y usuarios frecuentes de comercios — ningún "administrador" ni "analista" está contemplado como actor del sistema en los Capítulos I–III.
- **Riesgo adicional**: si un administrador puede "editar" registros, entra en tensión con RNF-05 (*"el sistema debe garantizar que las coordenadas GPS y el tiempo no puedan ser alterados manualmente por el usuario"*) — el documento protege la integridad del dato frente al ciudadano pero no aclara si el administrador queda exento, lo cual es una inconsistencia interna del propio Cap. IV.
- **Corrección propuesta**: eliminar CU-09 y el rol Administrador/Analista. La municipalidad es destinataria de datos públicos agregados (mapa de puntos críticos confirmados), no un rol con acceso de escritura dentro de la app. Ya reflejado en `diseno.md` §0 (punto 4) y §3 (actores corregidos).
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-17

---

### CORR-003 — El mapa expone reportes individuales crudos, no focos verificados

- **Sección afectada**: Tabla 7, CU-08 ("Visualizar Mapa de Puntos": *"Muestra en un mapa los marcadores de basura registrados en la base de datos"*); wireframe "Pantalla Mapa" (*"mostrando únicamente los datos de alertas reales aportados por los ciudadanos"*).
- **Tipo de contenido**: Flexible.
- **Divergencia**: el marco conceptual (Cap. II) define que un "foco" o "punto crítico" solo existe **después** del agrupamiento por densidad (DBSCAN) y de la verificación en campo (Instrumento 2, Fase 2) — no es cada reporte individual. Mostrar "marcadores de basura registrados" tal cual llegan del ciudadano expone reportes no verificados (que podrían ser ruido espacial, error de geolocalización o incluso duplicados) como si fueran focos confirmados.
- **Corrección propuesta**: el mapa público muestra únicamente `Punto_critico` con verificación de campo confirmada, nunca `Reporte` individuales. Ya reflejado en `diseno.md` §0 (punto 5) y §9.
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-17

---

### CORR-004 — El modelo de datos no puede sustentar dos de las cinco dimensiones de la Variable 2

- **Sección afectada**: Tabla 12 ("Entidad Punto_critico": campos `id_punto`, `ubicacion`, `frecuencia_acumulacion`, `nivel_criticidad`, `ultima_actualizacion`).
- **Tipo de contenido**: Flexible.
- **Divergencia**: el Anexo 2 (Tabla 15/16 de operacionalización) exige medir, con indicadores concretos, la dimensión **"calidad del agrupamiento"** (coeficiente de silueta ≥ 0.50; % de ruido espacial ≤ 30%, fuente: "Salida del módulo de análisis, Cap. IV") y la dimensión **"identificación del foco"** (agrupaciones confirmadas en campo respecto de las detectadas, ≥ 70%). La entidad `Punto_critico`, tal como está definida en la Tabla 12, **no tiene ningún campo** para registrar el coeficiente de silueta, la proporción de ruido, ni el estado de verificación en campo (confirmado/descartado). Sin esos campos, el propio sistema no puede producir la evidencia que su Anexo 2 promete reportar "desde el Cap. IV".
- **Corrección propuesta**: agregar a `Punto_critico` los campos `coeficiente_silueta`, `proporcion_ruido` y `estado_verificacion` (pendiente/confirmado/descartado). Ya reflejado en `diseno.md` §6 (diagrama entidad-relación corregido).
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-17

---

### CORR-005 — RF-05 no especifica que la clasificación es automática (IA)

- **Sección afectada**: Tabla 6, RF-05 (*"El sistema debe registrar la categoría del tipo de residuo reportado"*) vs. Tabla 7, CU-06 (*"Categorizar punto de acumulación", actor "Sistema", "de forma automática dependiendo de la imagen"*).
- **Tipo de contenido**: Flexible.
- **Divergencia**: RF-05 es ambiguo — no dice si la categoría la asigna el usuario manualmente o el sistema automáticamente. CU-06 sí lo deja explícito (actor "Sistema", automático). Esto es relevante porque "Exactitud de clasificación mediante inteligencia artificial" es una dimensión completa de la Variable 1, con su propio indicador (Tabla 15: "Coincidencia IA-observador ≥ 70%") y todo un subcapítulo teórico dedicado a MobileNetV2/TensorFlow Lite (Cap. II, III). Un RF tan genérico no sustenta esa dimensión con la fuerza que amerita.
- **Corrección propuesta**: reescribir RF-05 para que declare explícitamente la clasificación automática on-device mediante el modelo de IA, con opción de corrección manual por el usuario. Ya reflejado en `diseno.md` §1 (RF-04).
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-17

---

### CORR-006 — Ningún requisito no funcional cubre la operación offline

- **Sección afectada**: Tabla 7 (RNF-01 a RNF-06) — ninguno menciona continuidad sin conexión.
- **Tipo de contenido**: Flexible.
- **Divergencia**: "Disponibilidad operativa" es una dimensión completa de la Variable 1, con indicador propio (Tabla 15: *"Reportes guardados y sincronizados según condición de red, ≥ 95% en cada condición"*), y el Cap. III dedica una sección extensa a la estrategia offline-first (Room sobre SQLite, patrón Repositorio, sincronización diferida). Sin embargo, la lista de RNF del Cap. IV no incluye ningún requisito de continuidad operativa sin red — es una omisión, no una contradicción activa, pero dejaría al Anexo 2 sin respaldo explícito en la lista de requisitos.
- **Corrección propuesta**: agregar un RNF explícito de disponibilidad operativa offline-first. Ya reflejado en `diseno.md` §2 (RNF-01).
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-17

---

### CORR-007 — Validación geográfica a nivel de "Cusco" en vez de "Wanchaq"

- **Sección afectada**: Tabla 7, CU-05 (*"El sistema verifica que... las coordenadas GPS pertenezcan al rango geográfico de Cusco"*).
- **Tipo de contenido**: Flexible.
- **Divergencia**: los criterios de inclusión (§3.2) exigen coordenadas *"dentro del límite administrativo del **distrito de Wanchaq**"* (6.38 km²), no de la provincia/ciudad del Cusco completa. Validar contra el límite de Cusco es un alcance mucho más amplio que el delimitado en la investigación (§1.4.1) y permitiría ingresar reportes de otros ocho distritos de la provincia.
- **Corrección propuesta**: cambiar la validación a el polígono del distrito de Wanchaq específicamente. Pendiente de reflejar como requisito explícito en `diseno.md` (RF-02).
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-17

---

### CORR-008 — Vocabulario de `Estado_reporte` no refleja el pipeline real (menor)

- **Sección afectada**: Tabla 10 ("Entidad Estado_reporte", ejemplo de valores: *"pendiente, atendido"*).
- **Tipo de contenido**: Flexible.
- **Divergencia**: "atendido" sugiere una acción municipal directa sobre el reporte (el municipio "atiende" la denuncia), rol que el propio estudio excluye del diseño del sistema (ver CORR-002). El pipeline real descrito en Cap. II-III es: captura → sincronización → agrupamiento (DBSCAN) → verificación en campo (Instrumento 2) → confirmado/descartado. El vocabulario de la tabla no refleja ese flujo.
- **Corrección propuesta**: usar los estados `pendiente, sincronizado, agrupado, verificado, descartado`. Ya reflejado en `diseno.md` §6 y §7 (diagrama de estados).
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-17

---

### CORR-009 — "Cambio de idioma" descrito en el Cap. IV pero no implementado

- **Sección afectada**: Capítulo IV — "Pantalla de perfil" (*"...el cambio de idioma y cerrar sesión"*).
- **Tipo de contenido**: Flexible.
- **Divergencia**: el Cap. IV describe que la app permite cambiar de idioma. La versión implementada es **solo en español**. Decisión del usuario (2026-09-18, Módulo 7): el cambio de idioma no es parte de ningún indicador de la investigación, exige traducciones ya revisadas (incluido el texto del Anexo 9, instrumento validado) y, en Android < 13, requiere infraestructura adicional de AppCompat.
- **Corrección propuesta**: retirar la mención del "cambio de idioma" del Cap. IV (o declararla explícitamente como trabajo futuro). Si más adelante se implementa, el idioma pertinente para Cusco (quechua) necesitaría traducción revisada por un hablante, y cualquier traducción del consentimiento debería pasar por el asesor. Ya reflejado en `diseno.md` §9.
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-18

---

### CORR-010 — La entidad `Reporte` no puede separar lo que dijo la IA de lo que confirmó el ciudadano

- **Sección afectada**: Capítulo IV, entidad `Reporte` (con una sola categoría) y RF-04 / CU-06 (*"Categorizar punto de acumulación"*, actor "Sistema").
- **Tipo de contenido**: Flexible.
- **Divergencia**: el indicador *Exactitud de clasificación IA* (Anexo 2) mide la coincidencia entre **la categoría que asigna el sistema** y la que registra el observador de campo. Pero RF-04 también exige "opción de corrección manual": si el ciudadano puede corregir la sugerencia y solo se guarda **una** categoría, lo que se compara con el observador ya no es lo que dijo la IA sino lo que decidió la persona, y el indicador mediría a los ciudadanos, no al modelo. Además se perdería la evidencia gratuita de cuánto y hacia dónde corrige la gente.
- **Corrección propuesta**: la entidad `Reporte` guarda, además de la categoría **confirmada**, la respuesta **cruda** del modelo: `categoria_ia` y `confianza_ia` (opcionales: nulos si el modelo no estuvo disponible). Ya reflejado en `diseno.md` §4.2, §6 y §8, y implementado en el Módulo 3b. Además el flujo cambia: la categoría se confirma **después** de tomar la foto (paso de confirmación con la sugerencia visible), no antes.
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-18

---

### CORR-011 — La sección «Confiabilidad de los instrumentos» reporta resultados de una «muestra piloto simulada» que contradicen la Tabla 5 y las fichas

- **Sección afectada**: Cap. III, «Confiabilidad de los instrumentos» (párrafo inicial y los apartados del SUS, de la Ficha de Observación de Campo y del GPS) y Tabla 5 (columna «Qué necesitas»).
- **Tipo de contenido**: Flexible (narrativa metodológica, no es un instrumento validado), pero lo que debe decir depende de datos que **aún no existen**.
- **Divergencia**: (a) dice que se «llevaron a cabo» pruebas con «datos de una muestra piloto **simulada**», mientras la Tabla 5 habla de estadísticos **previstos** y pide un «piloto real»: los datos simulados no miden confiabilidad; (b) los kappa son **aritméticamente imposibles** con los acuerdos que declara: con 6 focos, 4 coincidencias en 3 categorías dan κ ≤ 0,54 (el texto dice «moderado-sustancial») y 5 coincidencias en 4 categorías dan κ ≤ 0,78 (el texto dice «> 0,80»); se enumeraron todas las tablas posibles ([evidencia](./evidencia/2026-09-19_verificacion-tesis-v591.txt)); (c) no coinciden con el diseño: 6 focos (F01–F06) frente a ≥ 10 agrupaciones de la Tabla 5; «Magnitud del foco» y «Composición» (Orgánico, Plástico, Papel, Vidrio) frente a «Volumen estimado» y las 7 opciones del Bloque C; tres puntos GPS frente a 8 × 5 de la ficha; «equipo de control» frente a la referencia de Google Earth Pro; (d) el alfa del SUS se da como «superior a 0.80» sin valor, n ni intervalo, y sin decir que los ítems negativos se invirtieron; (e) atribuye a la precisión del GPS la «confiabilidad técnica de la disponibilidad operativa», que es otra dimensión; (f) la Tabla 5 conserva una columna de notas de trabajo.
- **Justificación / referencia**: [`plan-confiabilidad-instrumentos.md`](./plan-confiabilidad-instrumentos.md) §2.1 y §2.2; la Ética del propio documento («los resultados se reportan tal como se obtengan, incluidos los desfavorables»).
- **Corrección propuesta**: **ahora**, sustituir el texto por la versión «prevista» del plan (§10-A) y quitar la columna «Qué necesitas»; **después de recolectar**, escribir los resultados reales con su intervalo de confianza (plan §10-B). Si se conserva algún ejercicio simulado, solo como ensayo del método, rotulado, en un anexo.
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-19

---

### CORR-012 — La «V de Aiken» de los Anexos 10–12 no se reproduce con los datos del propio anexo

- **Sección afectada**: Tabla 3 (nota «dispersión… de 59.2 a 94.0»), Tabla 4 y Anexos 10, 11 y 12 (Tablas 34–36).
- **Tipo de contenido**: **Con validación externa ya emitida.** **No se edita en silencio**: requiere visto bueno del asesor.
- **Divergencia**: el texto dice que los porcentajes se recodificaron a las cinco bandas de la ficha y que V = S / [n(c−1)]. Con los datos de los anexos ([evidencia](./evidencia/2026-09-19_verificacion-tesis-v591.txt)): en el Anexo 10 la «V» coincide con el **promedio de los tres porcentajes ÷ 100** en 10 de 10 criterios; en el Anexo 11, en 0 de 10 (ni con el promedio ni con la V), y en el 12, en 3 de 10. La V calculada como describe el texto sale ≈ 0,89 / 0,87 / 0,89 (no 0,83 / 0,85 / 0,84). Con 3 jueces y 5 categorías, la probabilidad exacta al azar de V ≥ 0,833 es 0,08 y de V ≥ 0,917 es 0,03: el «≥ 0,70» es una convención, no una prueba. El tercer juez puntúa 50–75 % frente a 90–96 % de los otros dos y la V global lo promedia. Y el «59.2» de la nota de la Tabla 3 no aparece: el mínimo de los promedios es 60,1.
- **Corrección propuesta**: recalcular con una regla de recodificación **declarada en el texto**, reportar la V por criterio (con el intervalo de Penfield y Giacobbi, 2004, si se puede) y corregir la nota de la Tabla 3. Con las bandas de la ficha la V correcta sale **más alta**, así que probablemente no invalida el juicio, pero los valores cambian y eso lo decide el asesor.
- **Estado**: `pendiente de aplicar` (con visto bueno)
- **Detectado**: 2026-09-19

---

### CORR-013 — La Ficha de Verificación Geoespacial pide datos que la app no registra o que su diseño no permite demostrar

- **Sección afectada**: Anexo 5 (Bloque B: «Precisión declarada por la app», «Condición de red» con 4G-5G / 3G-H+ / Sin red, «Filtro: Aceptó/Bloqueó») y Tabla 2 (8 puntos × 5 = 40 observaciones; criterio «≥ 95 % en cada condición», Anexo 2).
- **Tipo de contenido**: **Con validación externa ya emitida.** **No se edita en silencio.**
- **Divergencia**: (1) la app usa la precisión declarada para filtrar y **la descarta**: no la guarda (ni en el teléfono ni en el servidor) y las lecturas bloqueadas no dejan rastro; (2) la app distingue `WIFI`, `DATOS_MOVILES`, `SIN_INTERNET`, `SIN_RED`, no la generación de la red (4G-5G frente a 3G-H+), y «Wi-Fi» no es una opción de la ficha; (3) 40 observaciones repartidas en tres condiciones dejan ≈ 13 por condición: 13 de 13 no basta para **demostrar** «≥ 95 %» (límite inferior de Wilson 77 %; harían falta 73 de 73).
- **Justificación / referencia**: [`plan-confiabilidad-instrumentos.md`](./plan-confiabilidad-instrumentos.md) §3.1, §5.3.7 y §9; [`pruebas-de-confiabilidad.md`](./pruebas-de-confiabilidad.md).
- **Corrección propuesta**: **no cambiar la ficha**; complementarla: anotar la generación de red a mano con la tabla de equivalencias del plan §5.3.7, decidir si la app guarda la precisión declarada y registra las lecturas en depuración (tarea G9), y fijar con el asesor cómo se aplica «≥ 95 %» (valor observado o límite inferior) y con qué n por condición.
- **Estado**: `pendiente de aplicar`
- **Detectado**: 2026-09-19

---

## Observaciones adicionales (no son correcciones formales)

- **RNF-02** (*"la base de datos debe estar disponible el 99% del tiempo"*) no está respaldado por ningún indicador del Anexo 2, y es un compromiso de infraestructura poco realista para el alcance y presupuesto de un proyecto de tesis (la propia Delimitación del estudio reconoce restricciones de tiempo y presupuesto). No se recomienda tratarlo como corrección formal, pero sí evaluar si conviene suavizar la redacción antes de la sustentación.
- **RNF-04** (escalabilidad "sin degradar el rendimiento" ante "crecimiento masivo") es una afirmación amplia sin métrica ni umbral verificable — riesgo similar al anterior.
- **Terminología "anónimo" (Ética 3.4, Anexo 9, Cap. IV)** *(detectada 2026-09-18, ver ADR-0002)*: el documento llama "código anónimo" / "identificador anónimo" al `id_anonimo`. Técnica y jurídicamente es un **seudónimo**: no identifica por sí solo, pero enlaza todos los reportes del mismo teléfono, y junto con ubicación GPS precisa y fecha puede hacer identificable a una persona. Un evaluador atento podría objetar "anónimo", y la Ley N.º 29733 (que el marco legal cita) usa el criterio de persona *identificable*. Se sugiere hablar de "código seudónimo" o "identificador no personal" y **no** afirmar en la tesis que los datos son "completamente anónimos". Como el texto del Anexo 9 es un instrumento ya validado, **no se edita en silencio**: requiere visto bueno del asesor y evaluar si el cambio de una palabra amerita re-validación. Además, el documento debería reconocer explícitamente los límites de la eliminación sin login (reinstalación, datos ya agrupados) — ver ADR-0002.

- **Datos de entrenamiento del clasificador de IA** *(detectada 2026-09-18, Módulo 3; actualizada el mismo día)*: ningún dataset público cubre las categorías del estudio tal como las define la investigación. **TrashNet** (MIT) aporta plástico, cartón/papel y vidrio, pero son objetos sueltos sobre cartulina blanca, así que **sobreestima** el desempeño con basura real en la calle. El dataset de Kaggle `phenomsg/waste-classification` **se evaluó y no se adoptó como fuente principal**: declara "30,000+ images" pero contiene 2 939 archivos; sus imágenes provienen, por los nombres de archivo, de búsquedas web (stock, ilustraciones, figuras de artículos) con derechos no verificados pese a la licencia MIT declarada por quien lo subió; y su taxonomía es por reciclabilidad, no por material (detalle en [`ml/README.md`](../ml/README.md), "Datasets evaluados"). El documento debería (a) declarar la procedencia y licencia de los datos de entrenamiento y citar TrashNet, como su autor pide, y (b) **no presentar la exactitud de laboratorio como el indicador del Anexo 2**: el indicador *coincidencia IA–observador ≥ 70 %* se mide en campo. `metal` (que TrashNet sí trae) no es categoría del Marco Conceptual: una lata se clasificará como la clase más parecida; conviene reconocerlo como limitación.
- **El modelo de IA no reconoce "Desmonte y escombros"** *(decisión 2026-09-18, a propuesta del usuario)*: para evitar que el clasificador confunda muros, veredas o suelo con escombros (material mineral gris, sin dataset público), el modelo se entrena con **4 clases** (plástico, cartón, vidrio, orgánico); "escombros" y "mixto" quedan como **categorías de elección manual** en la app. Consecuencias que el documento debe recoger: (1) el Cap. II (Marco Teórico, apartado "Visión artificial en el dispositivo") describe una cámara que "segmenta y categoriza plásticos, cartón, vidrio, orgánicos y **escombros**" — esa descripción ya no es exacta (además, el sistema **clasifica**, no segmenta); (2) el indicador *Exactitud de clasificación IA* (coincidencia con el observador) **no puede acertar** un foco cuyo tipo predominante sea "desmonte y escombros": hay que **acotarlo a las categorías que el modelo cubre** o declararlo como limitación; (3) quitar la clase **no elimina** el riesgo (el modelo es cerrado y forzará un montón de escombros hacia otra clase salvo que la confianza quede bajo el umbral), por eso se mide con fotos "fuera de clase" (celda 7b del cuaderno) en vez de suponerlo. Decisión de alcance pendiente con el asesor.
- **Reentrenar el modelo con fotos de ciudadanos** *(detectada 2026-09-18)*: mejora tentadora a futuro, pero el Anexo 9 (consentimiento validado) declara un propósito acotado; usar las fotos de la app para entrenar un modelo excede ese propósito. No se implementa; si se quisiera, requiere consulta al asesor y un consentimiento distinto.
- **Riesgo de cronograma** *(detectada 2026-09-18)*: la ventana de observación del documento (8 semanas, del 21-sep al 15-nov-2026) empieza en días, y a esta fecha aún no existen backend real, modelo entrenado, pruebas en dispositivo real ni publicación en Play Store (cuya prueba cerrada exige varios testers durante ≥ 14 días — verificar requisitos vigentes en el Módulo 9). Conviene revisar las fechas con el asesor antes de que la ventana empiece, en vez de recortar el rigor del estudio para cumplirlas.

- **Categorías de la Ficha de Observación de Campo (bloque D) vs. las del sistema** *(detectada 2026-09-18, Módulo 3)*: el observador elige entre *Orgánico o restos de alimentos, Papel y cartón, Plástico y PET, Vidrio, **Desmonte y escombros**, Mixto* y **Otro**; la "categoría predominante asignada por el sistema" ofrece *Orgánico, Papel y cartón, Plástico y PET, Vidrio, **Desmonte**, Mixto* — sin "Otro" y con "Desmonte" a secas. Para que la *coincidencia IA–observador* (indicador "Exactitud de clasificación IA") compare lo mismo, ambas listas deberían usar los mismos nombres y decidir qué pasa con "Otro" (el modelo no tiene esa salida; hoy caería en `MIXTO` o en la corrección manual). Si la ficha es un instrumento ya validado, **no se edita en silencio**: consultarlo con el asesor.
- **La clase es "desmonte y escombros", no solo "restos de concreto"** *(detectada 2026-09-18)*: la categoría del documento incluye el desmonte (material de excavación: tierra, grava), lo que amplía las confusiones posibles del clasificador (suelo desnudo, caminos de tierra, muros, veredas) y obliga a **definir por escrito qué fotos cuentan como esa clase** antes de etiquetar el dataset. Propuesta de definición y guía de fotos en [`ml/README.md`](../ml/README.md).
- **El indicador compara categorías predominantes por agrupación, no foto por foto** *(detectada 2026-09-18)*: los errores aleatorios de una foto se diluyen con varios reportes, los **sistemáticos no** (un muro que el modelo siempre llama "escombros" seguirá ganando la mayoría). Y para medir la IA, no a los ciudadanos, hay que guardar **por separado** la sugerencia del modelo y la categoría final elegida (hoy `Reporte` guarda una sola `categoria`): ver `plan.md`, Módulo 3b.

- **Límites cuantificados de la infraestructura gratuita** *(detectada 2026-09-18, backend; refuerza la observación sobre RNF-02)*: al configurar el backend se verificaron en la documentación oficial los límites reales del stack gratuito, que conviene citar en las *Limitaciones* con su fecha de consulta: **Render Free** — 0,1 CPU y 512 MB, se duerme a los 15 min sin tráfico y tarda ≈ 1 min en despertar, 750 h de instancia al mes, disco efímero; **Neon Free** — 0,5 GB, 100 h de cómputo al mes, el cómputo se duerme a los 5 min; **Cloudinary Free** — 25 créditos al mes. Con esa infraestructura, **RNF-02 (base de datos disponible el 99 % del tiempo) no es un compromiso que se pueda sostener ni medir con honestidad**: se recomienda suavizar su redacción (p. ej. "disponibilidad en horario de operación, con tolerancia a arranques en frío") o declararlo como objetivo no verificado. Lo que sí garantiza el diseño, y es verificable, es que **ningún reporte se pierde** por un servidor dormido (offline-first: queda en el teléfono y se reintenta). Detalle y fuentes en [`despliegue.md`](./despliegue.md) §2.
- **"TensorFlow Lite" ahora se llama LiteRT** *(detectada 2026-09-18, Módulo 3b)*: el documento habla de TensorFlow Lite. Ese runtime pasó a llamarse LiteRT: TensorFlow avisa que `tf.lite.Interpreter` está en desuso a favor de LiteRT y la documentación oficial ya está bajo ese nombre. La app usa `com.google.ai.edge.litert:litert:1.4.2` (ver ADR-0003). Conviene mencionar el runtime como LiteRT (antes TensorFlow Lite) para que el documento no quede desactualizado.

- **Sub-indicadores y adaptación del SUS** *(detectada 2026-09-19, auditoría de confiabilidad)*: (a) el Anexo 2 define *facilidad de aprendizaje*, *facilidad de uso* e *intención de uso* como puntajes con umbral «≥ 3,5», pero son subescalas de 3, 6 y **1** ítem (con un solo ítem no existe alfa) y la literatura desaconseja calcular subescalas del SUS de forma rutinaria (Lewis y Sauro, 2017); si se reportan, con su propio alfa y con cautela; (b) la nota de adaptación del Anexo 3 dice que «los ítems 5, 8 y 9 fueron contextualizados», pero leídos frente al SUS estándar los **diez** enunciados están reformulados (p. ej. el 2 y el 6 unen dos ideas), por lo que la confiabilidad publicada del SUS (α ≈ 0,91) no se hereda y hay que medirla. El Anexo 3 tiene validación externa: **no se edita en silencio**. Detalle en [`plan-confiabilidad-instrumentos.md`](./plan-confiabilidad-instrumentos.md) §2.1 (filas 6 y 7).

## Próxima revisión pendiente

Auditoría del Capítulo IV completada. Queda pendiente, cuando se decida aplicar el primer lote de cambios al `.docx`: revisar si las Ilustraciones 1, 2 y 6 (diagramas de casos de uso, arquitectura y entidad-relación) necesitan rehacerse para reflejar las correcciones anteriores, ya que son imágenes y no se pudieron auditar como texto.
