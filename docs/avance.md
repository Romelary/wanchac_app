# Tablero de avance — Wanchaq Limpio

> Lista de tareas para ver de un vistazo **qué está hecho y qué falta**. Es una **vista**: el detalle, las decisiones y la evidencia de cada punto viven en [`progreso.md`](./progreso.md) (registro por sesiones y pendientes), [`plan.md`](./plan.md) (módulos), [`pruebas.md`](./pruebas.md), [`despliegue.md`](./despliegue.md) y los ADR. Se actualiza al cierre de cada sesión, junto con `progreso.md`.
>
> **Corte: 2026-09-19 (Sesión 25; el área G la añadió la Sesión 25-B, paralela a la 25).** Fechas según el reloj del PC (ver la tarea F11: está en UTC-8, no en hora de Perú).

**Leyenda.** Quién: *Usuario* · *Claude* · *Asesor* (decisión que hay que consultar). Prioridad: 🔴 bloquea otras tareas o la ventana de observación · 🟡 importante · ⚪ pulido u opcional. `[x]` hecho · `[ ]` pendiente. Una casilla marcada con *(dicho por el usuario)* no tiene evidencia que yo haya visto.

## Resumen

<!-- RESUMEN:INICIO -->
| Área | Hechas | Total | Avance |
|---|---:|---:|---|
| **A.** Infraestructura y backend | 8 | 20 | `████░░░░░░` 40 % |
| **B.** App Android | 18 | 25 | `███████░░░` 72 % |
| **C.** Modelo de IA | 4 | 9 | `████░░░░░░` 44 % |
| **D.** Pruebas en dispositivo y calidad | 5 | 13 | `████░░░░░░` 38 % |
| **E.** Publicación en Play Store | 1 | 9 | `█░░░░░░░░░` 11 % |
| **F.** Asesor y documento de tesis | 1 | 13 | `█░░░░░░░░░` 8 % |
| **G.** Confiabilidad de los instrumentos | 0 | 14 | `░░░░░░░░░░` 0 % |
| **Total** | **37** | **103** | `████░░░░░░` **36 %** |

**Pendientes: 66**, de las cuales **18 son 🔴** (bloquean otras tareas o la ventana de observación). Por responsable (una tarea compartida cuenta para cada uno): **Usuario 41** · **Claude 23** · **Asesor 18**.
<!-- RESUMEN:FIN -->

> Es un **conteo de tareas, no de esfuerzo**: una tarea como "Fase 2 (DBSCAN)" pesa mucho más que "quitar un aviso". Sirve para ver el rumbo, no para medir horas.

## Ruta crítica (en este orden)

La ventana de observación del estudio (**21-sep → 15-nov-2026, 8 semanas**) empieza en días; esto es lo que decide si se puede empezar:

1. **A8 + A9** — cerrar Render y la seguridad de Cloudinary (falta la evidencia de Render y confirmar la rotación de la clave).
2. **A10** — desplegar la Fase 1 y ejecutar `--reporte-de-prueba`: es la primera vez que la API real guarda un reporte. El mismo `git push` publica el endpoint provisional del mapa (A20).
3. **A19** — configurar en `local.properties` la dirección del servidor (`baseUrl`) y la clave de Google Maps (`MAPS_API_KEY`): la app **ya** apunta a donde se le diga y aguanta el arranque en frío de Render (B10 y B11 hechos el 2026-09-19).
4. **D9** — primer reporte real de punta a punta: teléfono → Render → Neon.
5. **D5** — GPS real en exteriores dentro de Wanchaq (hoy solo se probó con ubicación simulada).
6. **C5 → C7** — fotos de calle, medir el modelo y fijar el umbral (hoy es provisional: 0,70).
7. **F2** — hablar del cronograma con el asesor **antes** de que empiece la ventana.
8. **A14 + A15 + A18** — agrupamiento, el endpoint del mapa y cómo se *confirma* un foco (la pantalla del mapa ya existe, B14, pero se verá **vacía** sin ellos): sin esto no se pueden medir las dimensiones de la Variable 2.
9. **E1 a E7** — correo de contacto, cuenta de Play, política de privacidad y **prueba cerrada de 14 días** (empezarla con antelación).
10. **G1 + G2 + G9** — decidir y congelar el plan de confiabilidad, retirar de la tesis la «muestra piloto simulada» y decidir si la app guarda la precisión declarada **antes** de que existan reportes reales: sin eso la confiabilidad de los instrumentos no se puede medir ni reportar ([plan](./plan-confiabilidad-instrumentos.md)).

## A. Infraestructura y backend

- [x] **A1** Neon (PostgreSQL + PostGIS 3.6) verificado con la cuenta real — *Usuario* · [evidencia](./evidencia/2026-09-18_verificacion-neon-cloudinary.txt)
- [x] **A2** Cloudinary verificado: subida privada, URL pública 404, URL firmada 200 — *Usuario* · misma evidencia
- [x] **A3** Esqueleto Django: `/health`, `/health/servicios`, ajustes que fallan fuerte, `verificar_servicios.py` (Fase 0) — *Claude* · [ADR-0004](./adr/0004-backend-django-sin-geodjango.md)
- [x] **A4** `render.yaml` (Blueprint sin secretos, `migrate` al construir, Python fijado, región por zona) — *Claude*
- [x] **A5** Repositorio privado en GitHub y primer `push` — *Usuario* *(dicho por el usuario)*
- [x] **A6** Fase 1 — `POST /reportes`: 201/200 idempotente, foto privada, validación en servidor, `categoriaIa`/`confianzaIa` aceptadas — *Claude* · [diseno.md §4.2](./diseno.md)
- [x] **A7** Fase 1 — `DELETE` con lápida, candado por clave y baja atómica — *Claude*
- [ ] **A8** Render desplegado (Fase 0): pegar la salida de `verificar_servicios.py --remoto URL` para registrarlo como verificado — *Usuario* · 🔴 *(el usuario dijo que estaba listo; falta la evidencia)*
- [ ] **A9** Confirmar que el par de claves de Cloudinary pegado por error en el chat está **desactivado o borrado**, y volver a correr el verificador — *Usuario* · 🔴 seguridad
- [ ] **A10** Desplegar la Fase 1: `git push` → Render migra → `verificar_servicios.py --remoto URL --reporte-de-prueba`. Es también la **única prueba en vivo** del candado de PostgreSQL y de la migración (la suite corre sobre SQLite) — *Usuario* · 🔴 · [despliegue.md §7](./despliegue.md)
- [ ] **A11** Pinger en cron-job.org sobre `/health` cada 10 min (activarlo cuando la app se use de verdad) — *Usuario* · 🟡 · [despliegue.md §8](./despliegue.md)
- [ ] **A12** Decidir la **limitación de uso** del `POST`/`DELETE` sin login (por IP, clave embebida, alertas de cuota): hoy cualquiera puede enviar basura — *Asesor* · 🟡 · [ADR-0004](./adr/0004-backend-django-sin-geodjango.md)
- [ ] **A13** Polígono oficial del distrito de Wanchaq (catastro/INEI) y validación con `ST_Within` en el servidor — *Usuario + Claude* · 🟡 (hoy es un rectángulo, igual en app y servidor)
- [ ] **A14** Fase 2 — agrupamiento **DBSCAN** (ε 50 m, MinSamples 4), coeficiente de silueta y proporción de ruido (RF-05/RF-06), tabla `Punto_critico` (CORR-004) — *Claude* · 🔴 para la tesis
- [ ] **A15** Fase 3 — `GET /puntos-criticos?estado=confirmado` **con focos reales** (hoy el endpoint es provisional y devuelve siempre una colección vacía: A20) — *Claude* · 🟡
- [x] **A20** `GET /puntos-criticos` **provisional**: siempre `200` con una colección GeoJSON vacía y sin tocar la base, para que la app muestre «Aún no hay focos confirmados» en vez de un error 404 (sin la ruta, la app **no dibujaría el mapa**); 7 pruebas y 11 mutaciones detectadas — *Claude* · **falta desplegarlo** (el `git push` de A10) · [ADR-0004 #17](./adr/0004-backend-django-sin-geodjango.md)
- [ ] **A16** Columna `geography` + índice GiST y **prueba de integración contra PostgreSQL real** (rama de desarrollo de Neon), al llegar la primera consulta espacial — *Claude* · ⚪ (Fase 2/3)
- [ ] **A17** Rama de desarrollo en Neon para no probar contra producción cuando haya datos reales — *Usuario* · ⚪
- [ ] **A18** **Hueco de planificación: cómo se confirma un foco (RF-07 / Instrumento 2).** El diseño dice que el observador registra `estado_verificacion` y el mapa solo muestra los `confirmado`, pero **no hay ninguna tarea, módulo ni fase** que lo construya (¿script de importación de la ficha de campo, formulario web, panel mínimo?). Sin esto el Módulo 5 quedaría vacío — *Asesor + Claude* · 🔴 · [diseno.md](./diseno.md) (RF-07 y secuencia del observador)
- [ ] **A19** **Clave de Google Maps y dirección del servidor**: en Google Cloud, un proyecto con **cuenta de facturación**, la API *Maps SDK for Android* y una clave **restringida** (paquete `com.wanchaqlimpio.app` + huella SHA-1 + solo esa API), con **presupuesto y alertas**; luego `MAPS_API_KEY` y `baseUrl` en `local.properties` (**nunca** al chat ni a git) — *Usuario* · 🔴 sin ella el mapa sale gris · [despliegue-mapa.md](./despliegue-mapa.md)

## B. App Android

- [x] **B1** Módulo 0 — Fundamentos (Hilt, Room, Retrofit, ktlint/detekt, estructura) — *Claude*
- [x] **B2** Módulo 1 — Onboarding y consentimiento informado, `id_anonimo` (ADR-0001) — *Claude*
- [x] **B3** Módulo 2 — Captura offline-first (cámara, GPS, filtro de 15 m, distrito, duplicados) — *Claude*
- [x] **B4** Módulo 3b — Clasificador de IA dentro de la app (LiteRT, sugerencia guardada aparte, CORR-010) — *Claude* · *ejecutado en la tablet el 2026-09-19: carga, sugiere y coincide con Python en la entrada; ver D7*
- [x] **B5** Módulo 4 — Sincronización diferida (cliente, WorkManager) — *Claude*
- [x] **B6** Módulo 6 — Mis reportes, con eliminación (ADR-0002) — *Claude*
- [x] **B7** Módulo 7 — Ajustes y "borrar todos mis datos" — *Claude*
- [x] **B8** Adaptación a pantallas grandes (ancho de lectura, barras del sistema, rotación), verificada en la Galaxy Tab S9+ — *Claude + Usuario*
- [x] **B9** Modo de ubicación simulada, solo depuración, con doble seguro contra `release` — *Claude*
- [x] **B10** `BASE_URL` configurable: `baseUrl` en `local.properties` o `-PbaseUrl=…`; en depuración, si falta, la del emulador; **`release` falla** si no es `https` — *Claude*
- [x] **B11** Tiempos de espera de OkHttp de 30/90/60/150 s (conexión/lectura/escritura/llamada) para aguantar el arranque en frío de Render (~1 min) — *Claude*
- [x] **B12** `SyncWorker`: **rechazo definitivo** (400, 410, 413, 415, 422 y foto ausente → estado local `RECHAZADO`, no se reenvía para siempre), seguimiento **por reporte** (intentos, cuándo, motivo, código HTTP, red; migración de Room 2→3), «Enviar ahora» y «Reintentar» ([ADR-0006](./adr/0006-envio-confiable-de-reportes.md)) — *Claude* · **verificado en la tablet** con un servidor falso (no con Render)
- [x] **B13** Reducir la foto al tomarla (lado largo 1 600 px, JPEG 85, sin metadatos): **4,0 MB → ≈ 0,2 MB medido en la tablet** ([ADR-0007](./adr/0007-gps-en-caliente-y-foto-reducida.md)) — *Claude*
- [x] **B14** Módulo 5 — **mapa público en la app con Google Maps** ([ADR-0005](./adr/0005-mapa-publico-dentro-de-la-app-con-google-maps.md)): destino "Mapa", cliente de `GET /puntos-criticos`, estados cargando / sin focos / con focos / error, y pruebas en la JVM — *Claude* · **sin datos reales** (faltan A14, A15 y A18) **y sin ver en un dispositivo** (D11)
- [ ] **B15** Ícono de la app (los íconos de la navegación ya son los definitivos, de Material, verificados en la tablet el 2026-09-19) — *Claude* · ⚪
- [ ] **B16** Revisión de **accesibilidad**: contraste, tamaños táctiles, lector de pantalla — *Claude* · 🟡
- [ ] **B17** Estados de error y vacío en todas las pantallas — *Claude* · 🟡
- [ ] **B18** Migrar `hiltViewModel()` al nuevo paquete (solo un aviso de obsolescencia) — *Claude* · ⚪
- [ ] **B19** Miniatura de la foto en "Mis reportes" (solo si las pruebas de usabilidad lo piden) — *Claude* · ⚪
- [ ] **B20** Cambio de idioma (diferido, CORR-009; el quechua exigiría traducción revisada por un hablante) — *Asesor* · ⚪
- [ ] **B21** Decidir `targetSdk` 36 (hoy) frente a 34 (el documento) y documentar la razón — *Asesor* · 🟡
- [x] **B22** Manifiesto: **quitar el inicializador automático de WorkManager**. `WanchaqLimpioApp` entrega la fábrica de Hilt, pero sin esto WorkManager arrancaba con la configuración por defecto y la ignoraba: `SyncWorker` y `EliminarReporteWorker` (`@HiltWorker`) **no se habrían podido crear en un dispositivo real**. Fallo del Módulo 4 hallado por el lint de `release` — *Claude* · **sin verificar en un dispositivo** (escenario C10, tarea D9) · [pruebas.md](./pruebas.md)
- [x] **B23** **GPS en caliente**: con la cámara abierta se muestra «GPS listo ±N m», la lectura de la foto viaja con la confirmación, se espera hasta 30 s a una lectura ≤ 15 m (sin relajar el criterio), **la foto se conserva** si el GPS no converge («Reintentar ubicación») y los permisos (precisa + aproximada) tienen salida a los Ajustes ([ADR-0007](./adr/0007-gps-en-caliente-y-foto-reducida.md)) — *Claude* · **sin GPS real** (ver D5)
- [x] **B24** **Registro de envíos**: Ajustes → «Compartir registro de envíos» manda un CSV (un renglón por reporte: intentos, cuándo llegó, red, motivo del fallo) **sin coordenadas, fotos ni código anónimo** — *Claude* · *el selector de compartir no se ha probado en el dispositivo* (H11)
- [x] **B25** **Servidor falso local** (`herramientas/servidor_falso.py`) para provocar fallos de red a voluntad, y configuración de red **solo de depuración** que permite `http://` únicamente hacia `localhost`, `127.0.0.1` y `10.0.2.2` — *Claude* · [prueba-en-dispositivo.md §2c](./prueba-en-dispositivo.md)

## C. Modelo de IA (clasificador de residuos)

- [x] **C1** Pipeline de entrenamiento en Colab: cuaderno, pruebas de humo, contrato del modelo — *Claude* · [ml/README.md](../ml/README.md)
- [x] **C2** Primer entrenamiento (v1 provisional, 4 clases; exactitud de prueba 0,89 sobre un dataset fácil, **no** es el indicador de la tesis) — *Usuario*
- [x] **C3** Decisión: `escombros` sale del modelo y queda como categoría manual — *Usuario*
- [x] **C4** Procedencia de las 7 fotos de `organico` declarada (Kaggle) y `metadata.json` corregido — *Usuario + Claude*
- [ ] **C5** Fotos reales de calle para probar el modelo (~20 por clase; sin rostros ni placas; no se suben a git) — *Usuario* · 🔴
- [ ] **C6** 30–60 fotos **"fuera de clase"** (escombros, muros, veredas, suelo) para medir la sobreconfianza — *Usuario* · 🔴
- [ ] **C7** Correr `probar_modelo.py`, fijar el tope aceptable **antes** de ver el resultado y decidir el umbral definitivo (hoy 0,70 provisional) — *Usuario + Asesor* · 🔴
- [ ] **C8** Decidir si hace falta un **v2** (más datos, una clase `sin_residuo` o reentrenar `escombros`) según C7 — *Asesor* · 🟡
- [ ] **C9** Cuantificar la diferencia de redimensionado entre la app y Python con fotos reales grandes — *Claude* · 🟡

## D. Pruebas en dispositivo y calidad

- [x] **D1** Instalar en la Galaxy Tab S9+ y comprobar la adaptación de pantalla (botón de 592 dp centrado en horizontal) — *Usuario + Claude*
- [x] **D2** Suite de la app en la JVM: **288 pruebas unitarias en verde** (174 al cerrar la Sesión 24; el resto son del envío confiable, el GPS en caliente, la foto reducida y el registro de envíos), ktlint y detekt limpios — *Claude* · [pruebas.md](./pruebas.md) · *sin prueba de mutación de los lotes de la Sesión 25 (sí del mapa: 30 mutaciones, 29 detectadas y 1 equivalente)*
- [x] **D3** Suite del backend: 209 pruebas y 152 mutaciones deliberadas — *Claude* · [pruebas.md](./pruebas.md)
- [ ] **D4** **Protocolo completo en la tablet (32 escenarios A–F)**: solo se verificó el consentimiento en horizontal — *Usuario* · 🟡 · [prueba-en-dispositivo.md](./prueba-en-dispositivo.md) · *avance: en la Sesión 25 se verificaron los escenarios C1 a C6, C9, C10 (contra un servidor falso), E2 y H8 y H10 en parte; ver [§5c](./prueba-en-dispositivo.md)*
- [ ] **D5** **GPS real en exteriores dentro de Wanchaq** (escenario C8): variante normal, cielo despejado, ~30 s; si aún rechaza por precisión, hacer que la app espere una lectura ≤ 15 m — *Usuario* · 🔴
- [ ] **D6** Probar en un **teléfono real**, no solo la tablet — *Usuario* · 🟡
- [x] **D7** `./gradlew connectedDebugAndroidTest` ejecutado por primera vez en la tablet: **26 de 26 en verde** tras corregir 4 fallos (2 de la prueba del consentimiento por no desplazarse en horizontal; 2 de la referencia del clasificador, cuya tolerancia era estrecha: el modelo en el teléfono difiere hasta 0,08 de Python con la misma entrada) — *Usuario + Claude* · [pruebas.md](./pruebas.md)
- [ ] **D8** Medir la latencia del clasificador y comprobar que las librerías nativas cargan en el dispositivo — *Usuario* · 🟡 · *avance (2026-09-19): las librerías nativas **cargan** y el modelo corre en la tablet; la **latencia** sigue sin medirse*
- [ ] **D9** **Primer reporte real de punta a punta**: teléfono → Render → Neon, visible en la base (escenario **C10** del protocolo; es también la **primera vez** que WorkManager crea un trabajo de sincronización en un dispositivo, B22) — *Usuario + Claude* · 🔴 · necesita A10 y `baseUrl` (A19); B10 y B11 ya están hechos
- [ ] **D10** Ronda de prueba piloto interna antes de aplicar el Instrumento 1 (SUS, 38 participantes) — *Usuario* · 🟡
- [ ] **D11** **Ver el mapa en un dispositivo** con la clave real (escenarios G1 a G8 del protocolo): que no salga gris, el encuadre en Wanchaq, el límite de desplazamiento, el error sin red y el marcador de un foco de prueba — *Usuario* · 🟡 · necesita A19 · [prueba-en-dispositivo.md](./prueba-en-dispositivo.md)
- [x] **D12** **Sesión 2 en la tablet (2026-09-19)**: migración de la base con datos reales (4 reportes), WorkManager con la fábrica de Hilt, envío y fallos contra un servidor falso (503, 422, respuesta perdida, cierre forzado, servidor que no contesta, «Enviar ahora», baja), foto reducida, permisos — *Usuario + Claude* · [prueba-en-dispositivo.md §5c](./prueba-en-dispositivo.md)
- [ ] **D13** **Escenarios H1 a H11** que necesitan a una persona con la tablet: modo avión, Wi-Fi sin salida, reinicio con pendientes, servidor dormido, fecha del teléfono mal puesta, **girar la tablet** (B1, C3) y compartir el registro de envíos — *Usuario + Claude* · 🟡 · [prueba-en-dispositivo.md](./prueba-en-dispositivo.md)

## E. Publicación en Play Store (Módulo 9, al final)

- [x] **E0** `applicationId` definitivo fijado: `com.wanchaqlimpio.app` (no se puede cambiar tras publicar) — *Claude*
- [ ] **E1** **Correo de contacto público** en `gradle.properties` (`contactEmail=`): la compilación `release` falla sin él — *Usuario* · 🔴
- [ ] **E2** Cuenta de desarrollador de Google Play (pago único de US$25) — *Usuario* · 🟡
- [ ] **E3** Keystore de firma y Play App Signing (la clave **nunca** a git). **Además, añadir su huella SHA-1 a la restricción de la clave de Google Maps**: sin ella el mapa funciona en depuración pero sale gris en la app publicada — *Usuario + Claude* · 🟡
- [ ] **E4** Política de privacidad pública, derivada del texto validado del Anexo 9 — *Claude + Asesor* · 🟡
- [ ] **E5** Formulario *Data Safety* (ubicación y fotos; eliminación ya cubierta por RNF-05) y clasificación de contenido — *Usuario* · 🟡
- [ ] **E6** Recursos de la ficha: ícono, capturas, descripciones corta y larga — *Usuario + Claude* · ⚪
- [ ] **E7** **Prueba cerrada**: mínimo 12 testers durante 14 días; se propone usar a los 38 del SUS y correrla en paralelo a la ventana de observación — *Usuario* · 🔴 empezar con antelación
- [ ] **E8** Activar y revisar la minificación (R8), hoy apagada — *Claude* · ⚪

## F. Asesor y documento de tesis

- [ ] **F1** Validar con el asesor el diseño corregido (se aparta del Capítulo IV) — *Asesor* · 🟡
- [ ] **F2** **Revisar el cronograma**: la ventana 21-sep → 15-nov empieza en días y faltan varias piezas — *Asesor* · 🔴 · [correcciones-documento.md](./correcciones-documento.md)
- [ ] **F3** Alcance del indicador *coincidencia IA–observador* ahora que el modelo no reconoce `escombros` (acotarlo a las 4 clases o declararlo como limitación) — *Asesor* · 🟡
- [ ] **F4** Redacción "anónimo" → "seudónimo" (Ética 3.4, Anexo 9) y reconocer los límites de la eliminación sin login — *Asesor* · 🟡
- [ ] **F5** Política de eliminación tras el agrupamiento: qué pasa con un `Punto_critico` si se da de baja un reporte que lo integraba — *Asesor* · 🟡
- [ ] **F6** Decidir cuándo aplicar CORR-001 a CORR-010 al `.docx` y si se rehacen las Ilustraciones 1, 2 y 6 — *Usuario + Asesor* · 🟡
- [ ] **F7** Reflejar en el `.docx` que no hay login (ADR-0001) y que el runtime se llama **LiteRT** (antes TensorFlow Lite) — *Usuario* · ⚪
- [ ] **F8** Citar **TrashNet** (Thung y Yang, 2016) y declarar la procedencia y licencia de los datos de entrenamiento — *Usuario* · 🟡
- [ ] **F9** Declarar en *Limitaciones* los límites de la infraestructura gratuita y suavizar **RNF-02** (99 % de disponibilidad, que esa infraestructura no puede sostener) — *Usuario + Asesor* · 🟡 · [despliegue.md §2](./despliegue.md)
- [ ] **F10** Declarar en la metodología el **pinger** que evita el arranque en frío (riesgo sobre el indicador de sincronización) — *Usuario* · 🟡 · [diseno.md §4.1](./diseno.md)
- [x] **F12** Decidido **dónde vive el mapa público**: **en la app, con Google Maps** (decisión del usuario, 2026-09-19; ya tiene cuenta de Google Cloud) — [ADR-0005](./adr/0005-mapa-publico-dentro-de-la-app-con-google-maps.md). Se recomendó la opción web con Leaflet; la elección es del usuario
- [ ] **F13** **Avisar al asesor** de que el mapa público quedó en la app con Google Maps: la tesis menciona un panel web con React/Leaflet que no se hará — *Usuario + Asesor* · 🟡
- [ ] **F11** Poner la **zona horaria del PC en hora de Perú (UTC-5)**: hoy está en UTC-8 y las fechas de los commits (evidencia de tesis) salen 3 h atrasadas respecto a Perú — *Usuario* · ⚪

## G. Confiabilidad de los instrumentos

> Plan completo, cifras y decisiones en [`plan-confiabilidad-instrumentos.md`](./plan-confiabilidad-instrumentos.md). Distinto de las pruebas de confiabilidad **del software** ([`pruebas-de-confiabilidad.md`](./pruebas-de-confiabilidad.md)): aquí se mide si los **instrumentos** (SUS, ficha de campo, ficha GPS) dan resultados repetibles.

- [ ] **G1** **Decidir con el asesor** los criterios y el diseño (D1–D10 del plan §12: α ≥ 0,70, κ ≥ 0,61, doble observación de **todas** las agrupaciones, cómo se aplican «≥ 70 %» y «≥ 95 %») y **congelarlo en git**, con fecha, antes de la primera medición — *Asesor + Usuario* · 🔴 · [plan §12](./plan-confiabilidad-instrumentos.md)
- [ ] **G2** **Sustituir** en el Cap. III la sección «Confiabilidad de los instrumentos», que reporta resultados de una «muestra piloto **simulada**» (κ aritméticamente imposibles; focos y puntos que no son los del diseño), por el texto «previsto» del plan §10-A, y quitar la columna «Qué necesitas» de la Tabla 5 — *Usuario + Asesor* · 🔴 · [CORR-011](./correcciones-documento.md)
- [ ] **G3** Plantillas de captura (CSV o formularios con opciones cerradas) para SUS, ficha de campo, fotos, sistema-por-agrupación y GPS — *Claude* · 🟡 · [plan, Apéndice A](./plan-confiabilidad-instrumentos.md)
- [ ] **G4** Scripts de análisis en `analisis/confiabilidad/` (α con IC de Feldt y *bootstrap*, ω, κ / κ ponderado / PABAK / AC1 con IC, ICC, DE agrupada y límite de repetibilidad, Wilson), **validados con ejemplos publicados** y con pruebas — *Claude* · 🟡 · (hoy solo existen `dimensionamiento.py` y `verificar_documento_v591.py`)
- [ ] **G5** Manual de codificación de la Ficha de Observación de Campo (definiciones operativas, reglas de desempate, fotos de ejemplo), **sin cambiar la ficha validada** — *Usuario + Claude* · 🟡
- [ ] **G6** **Piloto del SUS** (≥ 15, mejor 20) con entrevista cognitiva: es la tarea D10 vista desde la confiabilidad — *Usuario* · 🟡 · necesita la app en manos de participantes
- [ ] **G7** **Calibración de la Ficha de Observación de Campo** (2 observadores × ≥ 20 sitios, con fotos estándar) y **re-calificación intraobservador** a los ≥ 14 días — *Usuario* · 🟡 · no necesita la app
- [ ] **G8** **Mediciones del Instrumento 3**: 8 puntos × 5 mediciones independientes, ≥ 2 teléfonos, variante **normal** (nunca la de ubicación simulada) — *Usuario* · 🟡 · necesita D5
- [ ] **G9** Decidir y, si se aprueba, guardar la **precisión declarada** de cada lectura aceptada (Room + servidor, con ADR) y un **registro de lecturas GPS** solo en depuración; **si se quiere en los reportes reales, antes de la ventana** (después no se puede recuperar) — *Usuario + Claude* · 🔴 · [plan §9 (I-1, I-3)](./plan-confiabilidad-instrumentos.md)
- [ ] **G10** Aplicación final del SUS (n = 38) y alfa final con su intervalo de confianza — *Usuario* · 🟡
- [ ] **G11** **Fase 2 de la ficha de campo**: doble observación de **todas** las agrupaciones (observadores ciegos a la IA), calificación de fotos por dos evaluadores y kappa final — *Usuario* · 🔴 · necesita A14 y A18
- [ ] **G12** Estabilidad del agrupamiento (sensibilidad de ε y MinSamples, Jaccard por remuestreo, mitades temporales) y pureza de categoría por agrupación — *Claude* · ⚪ · dentro de A14
- [ ] **G13** Redactar los resultados reales de confiabilidad (con IC y lista GRRAS) en el Cap. III y en el de resultados — *Usuario + Claude* · 🟡
- [ ] **G14** Revisar con el asesor la **V de Aiken** de los Anexos 10–12 (lo que muestran no es la V de Aiken) — *Asesor* · 🟡 · [CORR-012](./correcciones-documento.md)
