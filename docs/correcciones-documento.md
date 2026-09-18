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

**Auditoría completa del Capítulo IV realizada el 2026-09-17**, incluyendo el contenido real de las Tablas 6 a 13 (antes no visible, solo se tenía el texto de párrafos). Resultado: 8 correcciones (CORR-001 a CORR-008). No se han aplicado cambios al `.docx` todavía — todas están `pendiente de aplicar`.

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

## Observaciones adicionales (no son correcciones formales)

- **RNF-02** (*"la base de datos debe estar disponible el 99% del tiempo"*) no está respaldado por ningún indicador del Anexo 2, y es un compromiso de infraestructura poco realista para el alcance y presupuesto de un proyecto de tesis (la propia Delimitación del estudio reconoce restricciones de tiempo y presupuesto). No se recomienda tratarlo como corrección formal, pero sí evaluar si conviene suavizar la redacción antes de la sustentación.
- **RNF-04** (escalabilidad "sin degradar el rendimiento" ante "crecimiento masivo") es una afirmación amplia sin métrica ni umbral verificable — riesgo similar al anterior.
- **Terminología "anónimo" (Ética 3.4, Anexo 9, Cap. IV)** *(detectada 2026-09-18, ver ADR-0002)*: el documento llama "código anónimo" / "identificador anónimo" al `id_anonimo`. Técnica y jurídicamente es un **seudónimo**: no identifica por sí solo, pero enlaza todos los reportes del mismo teléfono, y junto con ubicación GPS precisa y fecha puede hacer identificable a una persona. Un evaluador atento podría objetar "anónimo", y la Ley N.º 29733 (que el marco legal cita) usa el criterio de persona *identificable*. Se sugiere hablar de "código seudónimo" o "identificador no personal" y **no** afirmar en la tesis que los datos son "completamente anónimos". Como el texto del Anexo 9 es un instrumento ya validado, **no se edita en silencio**: requiere visto bueno del asesor y evaluar si el cambio de una palabra amerita re-validación. Además, el documento debería reconocer explícitamente los límites de la eliminación sin login (reinstalación, datos ya agrupados) — ver ADR-0002.

## Próxima revisión pendiente

Auditoría del Capítulo IV completada. Queda pendiente, cuando se decida aplicar el primer lote de cambios al `.docx`: revisar si las Ilustraciones 1, 2 y 6 (diagramas de casos de uso, arquitectura y entidad-relación) necesitan rehacerse para reflejar las correcciones anteriores, ya que son imágenes y no se pudieron auditar como texto.
