# Tablero de avance — Wanchaq Limpio

> Lista de tareas para ver de un vistazo **qué está hecho y qué falta**. Es una **vista**: el detalle, las decisiones y la evidencia de cada punto viven en [`progreso.md`](./progreso.md) (registro por sesiones y pendientes), [`plan.md`](./plan.md) (módulos), [`pruebas.md`](./pruebas.md), [`despliegue.md`](./despliegue.md) y los ADR. Se actualiza al cierre de cada sesión, junto con `progreso.md`.
>
> **Corte: 2026-09-18 (Sesión 23).** Fechas según el reloj del PC (ver la tarea F11: está en UTC-8, no en hora de Perú).

**Leyenda.** Quién: *Usuario* · *Claude* · *Asesor* (decisión que hay que consultar). Prioridad: 🔴 bloquea otras tareas o la ventana de observación · 🟡 importante · ⚪ pulido u opcional. `[x]` hecho · `[ ]` pendiente. Una casilla marcada con *(dicho por el usuario)* no tiene evidencia que yo haya visto.

## Resumen

<!-- RESUMEN:INICIO -->
| Área | Hechas | Total | Avance |
|---|---:|---:|---|
| **A.** Infraestructura y backend | 7 | 18 | `████░░░░░░` 39 % |
| **B.** App Android | 9 | 21 | `████░░░░░░` 43 % |
| **C.** Modelo de IA | 4 | 9 | `████░░░░░░` 44 % |
| **D.** Pruebas en dispositivo y calidad | 3 | 10 | `███░░░░░░░` 30 % |
| **E.** Publicación en Play Store | 1 | 9 | `█░░░░░░░░░` 11 % |
| **F.** Asesor y documento de tesis | 0 | 12 | `░░░░░░░░░░` 0 % |
| **Total** | **24** | **79** | `███░░░░░░░` **30 %** |

**Pendientes: 55**, de las cuales **15 son 🔴** (bloquean otras tareas o la ventana de observación). Por responsable (una tarea compartida cuenta para cada uno): **Usuario 29** · **Claude 22** · **Asesor 15**.
<!-- RESUMEN:FIN -->

> Es un **conteo de tareas, no de esfuerzo**: una tarea como "Fase 2 (DBSCAN)" pesa mucho más que "quitar un aviso". Sirve para ver el rumbo, no para medir horas.

## Ruta crítica (en este orden)

La ventana de observación del estudio (**21-sep → 15-nov-2026, 8 semanas**) empieza en días; esto es lo que decide si se puede empezar:

1. **A8 + A9** — cerrar Render y la seguridad de Cloudinary (falta la evidencia de Render y confirmar la rotación de la clave).
2. **A10** — desplegar la Fase 1 y ejecutar `--reporte-de-prueba`: es la primera vez que la API real guarda un reporte.
3. **B10 + B11** — que la app apunte a Render y aguante su arranque en frío (~1 min).
4. **D9** — primer reporte real de punta a punta: teléfono → Render → Neon.
5. **D5** — GPS real en exteriores dentro de Wanchaq (hoy solo se probó con ubicación simulada).
6. **C5 → C7** — fotos de calle, medir el modelo y fijar el umbral (hoy es provisional: 0,70).
7. **F2** — hablar del cronograma con el asesor **antes** de que empiece la ventana.
8. **A14 + A18 + B14** — agrupamiento, cómo se *confirma* un foco y el mapa: sin ellos no se pueden medir las dimensiones de la Variable 2.
9. **E1 a E7** — correo de contacto, cuenta de Play, política de privacidad y **prueba cerrada de 14 días** (empezarla con antelación).

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
- [ ] **A15** Fase 3 — `GET /puntos-criticos?estado=confirmado` para el mapa — *Claude* · 🟡
- [ ] **A16** Columna `geography` + índice GiST y **prueba de integración contra PostgreSQL real** (rama de desarrollo de Neon), al llegar la primera consulta espacial — *Claude* · ⚪ (Fase 2/3)
- [ ] **A17** Rama de desarrollo en Neon para no probar contra producción cuando haya datos reales — *Usuario* · ⚪
- [ ] **A18** **Hueco de planificación: cómo se confirma un foco (RF-07 / Instrumento 2).** El diseño dice que el observador registra `estado_verificacion` y el mapa solo muestra los `confirmado`, pero **no hay ninguna tarea, módulo ni fase** que lo construya (¿script de importación de la ficha de campo, formulario web, panel mínimo?). Sin esto el Módulo 5 quedaría vacío — *Asesor + Claude* · 🔴 · [diseno.md](./diseno.md) (RF-07 y secuencia del observador)

## B. App Android

- [x] **B1** Módulo 0 — Fundamentos (Hilt, Room, Retrofit, ktlint/detekt, estructura) — *Claude*
- [x] **B2** Módulo 1 — Onboarding y consentimiento informado, `id_anonimo` (ADR-0001) — *Claude*
- [x] **B3** Módulo 2 — Captura offline-first (cámara, GPS, filtro de 15 m, distrito, duplicados) — *Claude*
- [x] **B4** Módulo 3b — Clasificador de IA dentro de la app (LiteRT, sugerencia guardada aparte, CORR-010) — *Claude* · *sin ejecutar en un dispositivo*
- [x] **B5** Módulo 4 — Sincronización diferida (cliente, WorkManager) — *Claude*
- [x] **B6** Módulo 6 — Mis reportes, con eliminación (ADR-0002) — *Claude*
- [x] **B7** Módulo 7 — Ajustes y "borrar todos mis datos" — *Claude*
- [x] **B8** Adaptación a pantallas grandes (ancho de lectura, barras del sistema, rotación), verificada en la Galaxy Tab S9+ — *Claude + Usuario*
- [x] **B9** Modo de ubicación simulada, solo depuración, con doble seguro contra `release` — *Claude*
- [ ] **B10** `BASE_URL` configurable por variante de compilación (hoy fija en `http://10.0.2.2:8000/`, el emulador) — *Claude* · 🔴 sin esto la app no habla con Render
- [ ] **B11** Tiempos de espera de OkHttp (~60 s; por defecto 10 s y el arranque en frío de Render tarda ~1 min) — *Claude* · 🔴
- [ ] **B12** `SyncWorker`: tratar 400/413/415/422 (y la foto faltante) como **rechazo definitivo** en vez de reintentar para siempre — *Claude* · 🟡
- [ ] **B13** Reducir la foto antes de subirla (p. ej. lado largo 1 600 px, JPEG 85): menos datos móviles y menos cuota de Cloudinary — *Claude* · ⚪
- [ ] **B14** Módulo 5 — **Mapa público** de puntos críticos confirmados (Google Maps SDK o Mapbox) — *Claude* · 🟡 · bloqueado por A14, A15, A18 y F12
- [ ] **B15** Íconos definitivos: navegación (hoy provisionales del asistente) e ícono de la app — *Claude* · ⚪
- [ ] **B16** Revisión de **accesibilidad**: contraste, tamaños táctiles, lector de pantalla — *Claude* · 🟡
- [ ] **B17** Estados de error y vacío en todas las pantallas — *Claude* · 🟡
- [ ] **B18** Migrar `hiltViewModel()` al nuevo paquete (solo un aviso de obsolescencia) — *Claude* · ⚪
- [ ] **B19** Miniatura de la foto en "Mis reportes" (solo si las pruebas de usabilidad lo piden) — *Claude* · ⚪
- [ ] **B20** Cambio de idioma (diferido, CORR-009; el quechua exigiría traducción revisada por un hablante) — *Asesor* · ⚪
- [ ] **B21** Decidir `targetSdk` 36 (hoy) frente a 34 (el documento) y documentar la razón — *Asesor* · 🟡

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
- [x] **D2** Suite de la app en la JVM: 128 pruebas unitarias en verde, ktlint y detekt limpios — *Claude*
- [x] **D3** Suite del backend: 202 pruebas y 141 mutaciones deliberadas — *Claude* · [pruebas.md](./pruebas.md)
- [ ] **D4** **Protocolo completo en la tablet (32 escenarios A–F)**: solo se verificó el consentimiento en horizontal — *Usuario* · 🟡 · [prueba-en-dispositivo.md](./prueba-en-dispositivo.md)
- [ ] **D5** **GPS real en exteriores dentro de Wanchaq** (escenario C8): variante normal, cielo despejado, ~30 s; si aún rechaza por precisión, hacer que la app espere una lectura ≤ 15 m — *Usuario* · 🔴
- [ ] **D6** Probar en un **teléfono real**, no solo la tablet — *Usuario* · 🟡
- [ ] **D7** Ejecutar `./gradlew connectedDebugAndroidTest` (las pruebas de UI y de referencia del clasificador compilan pero nunca se ejecutaron) — *Usuario + Claude* · 🟡
- [ ] **D8** Medir la latencia del clasificador y comprobar que las librerías nativas cargan en el dispositivo — *Usuario* · 🟡
- [ ] **D9** **Primer reporte real de punta a punta**: teléfono → Render → Neon, visible en la base — *Usuario + Claude* · 🔴 · necesita A10, B10 y B11
- [ ] **D10** Ronda de prueba piloto interna antes de aplicar el Instrumento 1 (SUS, 38 participantes) — *Usuario* · 🟡

## E. Publicación en Play Store (Módulo 9, al final)

- [x] **E0** `applicationId` definitivo fijado: `com.wanchaqlimpio.app` (no se puede cambiar tras publicar) — *Claude*
- [ ] **E1** **Correo de contacto público** en `gradle.properties` (`contactEmail=`): la compilación `release` falla sin él — *Usuario* · 🔴
- [ ] **E2** Cuenta de desarrollador de Google Play (pago único de US$25) — *Usuario* · 🟡
- [ ] **E3** Keystore de firma y Play App Signing (la clave **nunca** a git) — *Usuario + Claude* · 🟡
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
- [ ] **F12** Decidir **dónde vive el mapa público**: el plan lo pone dentro de la app (Google Maps o Mapbox) pero la tesis menciona un panel web con React/Leaflet; opciones, requisitos y recomendación en [plan.md](./plan.md) (Módulo 5) — *Asesor + Usuario* · 🟡
- [ ] **F11** Poner la **zona horaria del PC en hora de Perú (UTC-5)**: hoy está en UTC-8 y las fechas de los commits (evidencia de tesis) salen 3 h atrasadas respecto a Perú — *Usuario* · ⚪
