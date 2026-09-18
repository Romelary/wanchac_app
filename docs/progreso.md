# Progreso del desarrollo — Wanchaq Limpio

> Registro vivo de avances. Se actualiza al cierre de cada sesión de trabajo. No reemplaza al documento de investigación (`Proyecto de Investigación v5.91`); registra decisiones técnicas, su fecha y su motivo.

## Resumen del proyecto

- **Sistema**: app móvil Android de georreferenciación colaborativa de focos de acumulación de residuos sólidos, distrito de Wanchaq, Cusco.
- **Nombre del sistema**: Wanchaq Limpio.
- **Basado en**: Proyecto de Investigación v5.91, Universidad Continental, 2026 (Ramos Yupanqui, Ramírez Tisoc, Nuñez Marroquin).
- **Alcance de esta fase**: cliente Android. El backend (Django REST + PostgreSQL/PostGIS) y el dashboard (React/Leaflet) mencionados en el documento quedan fuera del scope inmediato salvo que se indique lo contrario.
- **Fuente de verdad del diseño**: Capítulos I, II y III del documento de investigación (problema, marco teórico, metodología, variables, ética, marco legal). El Capítulo IV del documento contiene contradicciones con esos capítulos y **no se sigue al 100%** — el detalle de cada corrección vive en [`diseno.md`](./diseno.md).

## Stack definido

| Componente | Elección | Estado |
|---|---|---|
| Lenguaje | Kotlin | ✅ definido |
| UI | Jetpack Compose | ✅ definido (ver nota de desviación abajo) |
| Arquitectura | MVVM + Repository, offline-first | ✅ definido |
| Persistencia local | Room sobre SQLite | ⏳ por implementar |
| Comunicación | Retrofit + GeoJSON (RFC 7946) contra API REST Django | ⏳ por implementar |
| Clasificación de residuos | MobileNetV2 vía TensorFlow Lite | ⏳ transfer learning pendiente |
| Clustering de focos | DBSCAN (scikit-learn), ε inicial 50 m, MinSamples 4 | ⏳ backend pendiente |
| Hosting API Django | Render (plan Free + pinger anti-cold-start) | ⏳ backend pendiente |
| Base de datos | Neon (PostgreSQL + PostGIS, serverless) | ⏳ backend pendiente |
| Almacenamiento de imágenes | Cloudinary (plan Free) | ⏳ backend pendiente |

## Estado del proyecto Android

- **Creado**: 2026-09-17, en `C:\Users\romel\wanchac_app`
- **Nombre del proyecto**: `Wanchaq_Limpio`
- **Plantilla usada**: Empty Activity con Jetpack Compose
- **`applicationId` / `namespace` actual**: `com.wanchaqlimpio.app` (definitivo, fijado el 2026-09-17 — ver Sesión 8. No cambiar nunca más, Play Store lo prohíbe tras publicar)
- **`minSdk`**: 26 → ✅ coincide con el documento (API mínima 26, Android 8.0 Oreo)
- **`compileSdk`**: 37 (subido desde 36 el 2026-09-17: OkHttp 5.5.0 y AndroidX Core 1.19.0 lo exigen)
- **`targetSdk`**: 36 → ⚠️ el documento especifica API 34 (Android 14) como target. Falta decidir si se homologa a 34 o se documenta como actualización deliberada en la tesis.
- **AGP**: 9.2.1 · **Kotlin**: 2.2.10 · **Compose BOM**: 2025.12.00

### Nota de desviación: Compose vs. vistas XML
El documento de investigación (apartado 3.6.3) describe el desarrollo con Android Studio/Kotlin sin especificar Jetpack Compose explícitamente, y el estilo general de la sección de diseño (wireframes, pantallas) es compatible con vistas XML clásicas. El proyecto se creó con **Jetpack Compose** por ser el estándar actual recomendado por Google. Esto no contradice ningún objetivo de la investigación (la tecnología de UI no es una variable ni un indicador evaluado), pero se documenta aquí para que quede trazable si se actualiza el documento de tesis.

## Registro de sesiones

### 2026-09-17 — Sesión 1: análisis del documento de investigación
- Se extrajo y analizó el documento completo (`Proyecto de Investigación v5.91 (1) (1).docx`, 749 párrafos, 36 tablas, 4 capítulos + 13 anexos).
- Se identificó la arquitectura tecnológica propuesta: Kotlin/Android nativo, Django REST, PostgreSQL+PostGIS, DBSCAN para clustering, MobileNetV2/TFLite para clasificación on-device, React/Leaflet para dashboard.
- Se detectaron inconsistencias entre el **Capítulo IV** (diseño) y los **Capítulos I–III** (ética, variables, población), en particular: la pantalla de login del Cap. IV pide nombre completo + correo + contraseña, mientras que el apartado de ética (3.4) exige minimización de datos y un identificador anónimo generado en el dispositivo, sin nombre, DNI ni teléfono. Se decidió priorizar Cap. I–III como fuente de verdad para el diseño real.

### 2026-09-17 — Sesión 2: creación del proyecto Android
- Proyecto creado manualmente en Android Studio siguiendo guía paso a paso (plantilla, package name, min SDK).
- Configuración registrada arriba en "Estado del proyecto Android".

### 2026-09-17 — Sesión 3: necesidad de entrenamiento del modelo de IA
- Se determinó que **DBSCAN no requiere entrenamiento** (algoritmo no supervisado, solo requiere fijar ε y MinSamples).
- Se determinó que **MobileNetV2 sí requiere transfer learning** sobre un dataset de residuos, porque ImageNet no cubre las 5 categorías del estudio (plástico, cartón, vidrio, orgánico, escombros).
- Estimado acordado: 2–4 horas de trabajo en Google Colab para un primer modelo `.tflite` funcional (no definitivo — el modelo final depende de datos de campo reales recolectados durante la ventana de observación de 8 semanas que define la metodología).

### 2026-09-17 — Sesión 4: documentación de progreso y diseño
- Se crea `docs/progreso.md` (este archivo) y `docs/diseno.md`.
- Se elabora el diseño detallado del sistema (requisitos derivados de variables/dimensiones, casos de uso, arquitectura, modelo de datos, diagramas de estado y de secuencia), corrigiendo las contradicciones detectadas en el Capítulo IV del documento original.

### 2026-09-17 — Sesión 5: decisión sobre login y plan de desarrollo
- Se discutió y confirmó la eliminación del login/cuenta de usuario del Cap. IV, documentada formalmente en [`docs/adr/0001-sin-login-id-anonimo.md`](./adr/0001-sin-login-id-anonimo.md).
- Se crea `docs/plan.md`: plan de desarrollo dividido en 9 módulos (0 a 8), con dependencias, requisitos que satisface cada uno, pruebas mínimas y checklist de cierre.
- Se define el estándar de buenas prácticas del proyecto (código, estructura de paquetes, control de versiones, documentación y pruebas) — ver `plan.md` §1.
- Se crea `docs/adr/` como carpeta de Architecture Decision Records.
- Se aclaró el objetivo del proyecto: no seguir el documento de investigación al pie de la letra, sino corregirlo en paralelo al desarrollo. Se crea `docs/correcciones-documento.md` como bitácora de divergencias entre lo construido y el `.docx`, con la corrección del login (CORR-001) como primera entrada, en estado `pendiente de aplicar`.

### 2026-09-17 — Sesión 6: auditoría completa del Capítulo IV
- Se extrajo el contenido real de las 36 tablas del `.docx` (antes invisible, solo se tenía el texto de párrafos) usando `python-docx`.
- Auditoría completa del Cap. IV contra Cap. I–III. Resultado: 8 correcciones formales (CORR-001 a CORR-008) documentadas en `docs/correcciones-documento.md`, todas en estado `pendiente de aplicar`. Hallazgos más relevantes:
  - CORR-001 (login) se reforzó con evidencia concreta: la entidad `Usuario` original guarda la contraseña como `TEXT` **sin hash**, un riesgo de seguridad real además del conflicto ético.
  - CORR-002: rol "Administrador" (CU-09, edita/elimina la BD) no tiene sustento en la población del estudio y contradice §3.2.
  - CORR-003: el mapa (CU-08) expone reportes individuales crudos en vez de focos ya verificados en campo.
  - CORR-004 (la más estructural): la entidad `Punto_critico` del documento no tiene campos para `coeficiente_silueta` ni `estado_verificacion` — sin esos campos el sistema no puede sustentar 2 de las 5 dimensiones de la Variable 2 del Anexo 2.
  - CORR-005 a CORR-008: ambigüedad sobre clasificación automática (RF-05), ausencia de RNF de operación offline, validación geográfica a nivel "Cusco" en vez de "Wanchaq" (CU-05), y vocabulario de `Estado_reporte` que no refleja el pipeline real.
- Se actualizó `diseno.md` (RF-02 ahora incluye validación de límite distrital) y `plan.md` (Módulo 2 ahora incluye esa validación) para reflejar CORR-007.

### 2026-09-17 — Sesión 7: stack de despliegue del backend
- Se confirmó el stack de despliegue objetivo, ya validado en un prototipo anterior: **Render** (API Django, plan Free) + **Neon** (PostgreSQL + PostGIS, serverless) + **Cloudinary** (imágenes).
- Se identificó un riesgo metodológico: el cold start de Render (plan Free, ~15 min de inactividad) podría contaminar la medición del indicador de "disponibilidad operativa" (Anexo 2, ≥95% de sincronización exitosa) al confundir lentitud de infraestructura con comportamiento real de la app. Mitigación acordada: pinger externo gratuito (cron-job.org) al endpoint `/health` cada 10 minutos durante la ventana de observación de 8 semanas; alternativa de respaldo si no basta: Render Starter (~US$7/mes) solo durante esas 8 semanas.
- Se actualizaron `diseno.md` (§4.1, nueva sección de despliegue) y `plan.md` (§4, backend) con el stack confirmado.
- **Corrección (misma sesión)**: se aclaró que la mención al prototipo anterior era solo de referencia, no una solicitud de reutilizarlo. **Decisión: el backend se construye desde cero**, con el stack confirmado (Render + Neon + Cloudinary) y el diseño ya corregido.

### 2026-09-17 — Sesión 8: publicación en Play Store
- Se confirmó que la app se publicará en Google Play, no solo se entregará como APK a la municipalidad. Esto eleva la prioridad de decisiones antes tratadas como menores:
  - El `applicationId` (hoy placeholder `com.example.wanchaq_limpio`) **no se puede cambiar después de publicar** — queda como decisión bloqueante inmediata.
  - Se identificó que la pista de pruebas cerradas obligatoria de Google Play (≥12 testers, ≥14 días) puede coincidir con la aplicación del Instrumento 1 (SUS, 38 participantes), evitando duplicar esfuerzo.
- Se agrega `Módulo 9 — Publicación en Play Store` a `plan.md`, dependiente del Módulo 8.
- **`applicationId` definitivo fijado: `com.wanchaqlimpio.app`**. Se renombró el paquete Kotlin (`com.example.wanchaq_limpio` → `com.wanchaqlimpio.app`) en todos los archivos fuente (`main`, `androidTest`, `test`) y en `app/build.gradle.kts` (`namespace` y `applicationId`). Se verificó que no quedan referencias al paquete anterior.
- Se verificó con `./gradlew assembleDebug` (corrido en segundo plano) que el proyecto compila correctamente tras el rename — **éxito, exit code 0**.

### 2026-09-17 — Sesión 9: Módulo 0 (fundamentos del proyecto)
- **Git inicializado**: `git init`, `.gitignore` corregido (faltaba excluir `/.idea` completo y `/.kotlin`, el cache de compilación incremental de Kotlin), primer commit creado con el proyecto base (wizard + rename de paquete + `docs/`).
- **Versiones verificadas contra Maven Central / Gradle Plugin Portal** (no asumidas de memoria, dado lo reciente del toolchain AGP 9.2.1 / Kotlin 2.2.10): KSP `2.2.10-2.0.2` (debe calzar exacto con la versión de Kotlin), Hilt `2.60.1`, Room `2.8.5`, Retrofit `3.0.0` (con `converter-gson` y `okhttp-logging-interceptor` a la par), Coroutines `1.11.0`, ktlint-gradle `14.2.0`, detekt `1.23.8`, MockK `1.14.11`, Turbine `1.2.1`.
- **Hilt** configurado: `WanchaqLimpioApp` (`@HiltAndroidApp`), registrada en el `AndroidManifest.xml`.
- **Room** configurado con una entidad de prueba (`PruebaEntity`/`PruebaDao`/`AppDatabase`), como pide el Módulo 0 — se reemplaza en el Módulo 2 por las entidades reales de `diseno.md` §6.
- **Retrofit + OkHttp** configurados vía Hilt (`NetworkModule`), apuntando a `BuildConfig.BASE_URL = "http://10.0.2.2:8000/"` (placeholder de desarrollo — alias de emulador Android hacia `localhost` del host, para cuando exista un mock/backend local).
- **ktlint** y **detekt** aplicados en `app/build.gradle.kts` con configuración mínima (defaults).
- Se creó el resto de la estructura de paquetes de `plan.md` §2 (vacía, con `.gitkeep`): `core/{util,location,sync}`, `data/{remote/api,remote/dto,mapper,repository}`, `domain/{model,repository}`, `ml/clasificador`, `ui/{onboarding,reporte,mapa,misreportes,ajustes}`.
- Se agregó permiso `INTERNET` al manifest (necesario para Retrofit).
- **Verificación exitosa**: `./gradlew assembleDebug ktlintCheck detekt testDebugUnitTest` → **BUILD SUCCESSFUL**. Módulo 0 marcado como completado en `plan.md`.
- **Problemas reales encontrados y resueltos en el camino** (documentados por si vuelven a aparecer):
  1. AGP 9.2.1 trae "built-in Kotlin" habilitado por defecto, que es incompatible con cómo KSP `2.2.10-2.0.2` registra fuentes generadas (`kotlin.sourceSets` DSL) — error: *"Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin"*. Intentar desactivar "built-in Kotlin" (`android.builtInKotlin=false`) y volver al plugin clásico `org.jetbrains.kotlin.android` **tampoco funciona**: ese plugin ya no es compatible con las clases internas de AGP 9.2.1 (`ClassCastException`). La solución real fue actualizar KSP a `2.3.12` (versión que ya arregló el issue google/ksp#2729) y dejar "built-in Kotlin" activado (el default).
  2. OkHttp `5.5.0` y AndroidX Core `1.19.0` exigen compilar contra API 37 → se subió `compileSdk` de 36 a 37 (el SDK 37 ya estaba instalado). El `targetSdk` se mantiene en 36 (decisión aparte, sigue pendiente homologar con el documento).
  3. ktlint por defecto no reconoce la convención de nombres PascalCase de `@Composable` ni el mismo detekt — se agregó `.editorconfig` (`ktlint_function_naming_ignore_when_annotated_with = Composable`) y `config/detekt/detekt.yml` (mismo ignore para `FunctionNaming`, y se desactivó `MagicNumber` porque marcaba como error los literales de color hexadecimal del tema).
- Segundo commit de git creado con todo el Módulo 0.

### 2026-09-17 — Sesión 10: Módulo 1 (onboarding y consentimiento)
- **Entregables**: `ConsentimientoEstado` (dominio), `ConsentimientoRepository`/`ConsentimientoRepositoryImpl` (DataStore Preferences, genera `id_anonimo` UUID v4 al aceptar), `OnboardingViewModel`, `OnboardingScreen`/`OnboardingContent` (texto completo del Anexo 9), y `MainActivity` ahora bloquea la navegación a la app hasta que se acepte el consentimiento (ver `diseno.md` §9 y ADR-0001).
- Se separó `OnboardingContent` (stateless) de `OnboardingScreen` (conecta el ViewModel) específicamente para poder escribir la prueba de UI sin necesitar infraestructura de testing de Hilt.
- Pruebas: 4 unitarias (`ConsentimientoRepositoryImplTest`) en verde + 2 de UI (`OnboardingContentTest`, compilan, pendientes de correr en emulador). Detalle de qué verifica cada una y el diagnóstico completo del bug de DataStore/Windows/Robolectric que hubo que resolver: ver [`pruebas.md`](./pruebas.md).
- Se corrigió un choque de nombres real: la clase `Application` (`WanchaqLimpioApp`) y el composable placeholder del wizard tenían el mismo nombre en el mismo paquete — el composable se renombró a `HomePlaceholder`. De paso se limpiaron los nombres con guion bajo heredados del wizard (`Wanchaq_LimpioTheme` → `WanchaqLimpioTheme`).
- Se creó `docs/plan.md` → Módulo 1 marcado como completado.

### 2026-09-17 — Sesión 11: Módulo 2 (captura y almacenamiento offline-first)
- **Entregables**: entidad Room `ReporteEntity` (reemplaza el placeholder `PruebaEntity` del Módulo 0) + `Converters` para los enums `CategoriaResiduo`/`EstadoReporte`; `ValidarNuevoReporte` (regla de negocio pura: precisión ≤ 15 m, límite distrital de Wanchaq, duplicados < 20 m y < 30 min); `ReporteRepository`/`ReporteRepositoryImpl` (offline-first: siempre guarda local con estado `PENDIENTE`, la sincronización es del Módulo 4); `ProveedorUbicacion`/`ProveedorUbicacionFused` (envoltorio de `FusedLocationProviderClient`); `ReporteViewModel` (máquina de estados: Capturando → ProcesandoUbicacion → Rechazado/Guardado); `ReporteScreen` con cámara real (CameraX: preview + captura a archivo) y manejo de permisos (CÁMARA + ubicación).
- `CategoriaResiduo` y `EstadoReporte`, que en el Cap. IV original eran tablas de catálogo con FK, se modelaron como **enums** en el cliente (simplificación deliberada y documentada: el conjunto es fijo y pequeño; la normalización completa sí tiene sentido en el backend/PostgreSQL, no en la caché local).
- Se conectó `ReporteScreen` en la navegación temporal del wizard (`AppDestinations.HOME` → `REPORTAR`) para que el flujo sea alcanzable de verdad, no código huérfano — placeholder hasta la navegación real del Módulo 5.
- **`LimiteDistritalWanchaq` usa un bounding box aproximado, no el polígono oficial del distrito** — está documentado explícitamente en el código y aquí como pendiente: hay que reemplazarlo por el polígono real (catastro municipal/INEI) antes de la ventana de observación real, e idealmente mover la validación definitiva al backend con PostGIS (`ST_Within`), dejando el chequeo del cliente solo como pre-filtro rápido.
- Pruebas: 15 unitarias en verde + 3 de UI que compilan pero no corrieron (sin emulador). Detalle completo en [`pruebas.md`](./pruebas.md). Se documentó ahí mismo que la captura real de cámara y el GPS real no tienen prueba automatizada por ser wrappers de hardware — falta probarlos manualmente en un dispositivo real, todavía no se ha hecho.
- Se creó `docs/plan.md` → Módulo 2 marcado como completado.

### 2026-09-17 — Sesión 12: Módulo 4 (sincronización diferida)
- **Alcance confirmado con el usuario antes de empezar**: solo el cliente Android, contra un backend simulado (`MockWebServer3`) — el backend real (Django/Neon/Cloudinary) queda para una sesión aparte, ya que requiere que el usuario cree primero la cuenta en Neon (no puedo crear cuentas de terceros por él).
- **Entregables**: `GeoJsonFeatureDto`/`GeoJsonMapper` (Reporte → GeoJSON RFC 7946, con el orden `[longitud, latitud]` que exige el estándar); `ReportesApi` (Retrofit, `POST /reportes` multipart: foto + metadata GeoJSON); `SyncWorker` (`@HiltWorker`, `CoroutineWorker`: sube los reportes `PENDIENTE`, marca `SINCRONIZADO` los que el servidor acepta, pide reintento si falla); `ProgramadorSincronizacion`/`ProgramadorSincronizacionWorkManager` (encola el `SyncWorker` con restricción de red y backoff exponencial tras cada `crearReporte()` exitoso); `WanchaqLimpioApp` ahora implementa `Configuration.Provider` para que WorkManager use el `HiltWorkerFactory`.
- Se verificó la API real de `mockwebserver3` (el artefacto de OkHttp 5.x, distinto del clásico `okhttp3.mockwebserver`) antes de escribir las pruebas, en vez de asumir la API antigua de memoria — evitó código incorrecto.
- **Limitación conocida, documentada y no resuelta en este módulo**: si el archivo de foto de un reporte ya no existe en disco al momento de sincronizar, el worker pide reintento indefinidamente en vez de marcarlo como fallo permanente (no hay un estado "fallo permanente" en `EstadoReporte` distinto de `DESCARTADO`, que ya tiene otro significado en el pipeline). Queda como pendiente para una futura mejora (ver Pendientes).
- Pruebas: 6 unitarias nuevas en verde (`GeoJsonMapperTest` 2, `SyncWorkerTest` 4). Detalle completo en [`pruebas.md`](./pruebas.md). No hay prueba de integración contra un backend real (no existe todavía).
- Se creó `docs/plan.md` → Módulo 4 marcado como completado (con la salvedad explícita de que es solo la parte cliente).

### 2026-09-18 — Sesión 13: Módulo 6 (Mis reportes) y decisión sobre la eliminación
- **Decisión previa (ADR-0002)**: el usuario propuso quitar la opción de eliminar reportes porque, al no haber login, "el dato queda completamente anónimo". Se le expuso que (1) la app ya promete la eliminación en el consentimiento validado (Anexo 9, implementado en el Módulo 1) y quitarla contradiría su propio consentimiento, y (2) los datos son **seudónimos, no anónimos** (GPS preciso + fecha + foto + `id_anonimo` persistente que enlaza reportes). Se presentaron tres opciones y el usuario eligió **mantenerla, versión mínima**. Razonamiento completo y límites asumidos en [`adr/0002-se-mantiene-la-eliminacion-de-reportes.md`](./adr/0002-se-mantiene-la-eliminacion-de-reportes.md).
- **Entregables**: `MisReportesScreen`/`MisReportesContent` (lista + confirmación antes de eliminar; patrón stateless de siempre), `MisReportesViewModel`; `ReporteRepository.eliminarReporte()` (borra la fila **y la foto del almacenamiento de la app**, y encola la baja remota); `EliminarReporteWorker` + `ProgramadorEliminacion` (WorkManager: `DELETE /reportes/{idAnonimo}/{idLocal}`, con reintento y backoff; `404` cuenta como éxito); `idLocal` agregado al GeoJSON de subida para que el servidor pueda identificar el reporte; etiquetas de categoría/estado y formato de fecha movidos a `ui/common/Etiquetas.kt` (compartidos entre pantallas); la navegación temporal reemplaza el placeholder `FAVORITES` por `MIS_REPORTES`.
- **Refinamiento sobre lo acordado**: la baja remota se solicita *siempre*, no solo si el reporte ya figura como sincronizado. Motivo: si el servidor recibió el reporte pero la respuesta se perdió (red inestable, el escenario habitual en Cusco), el cliente lo ve como `PENDIENTE` y, de no pedir la baja, quedaría un dato huérfano en el servidor — justo lo que la función debe evitar. Cuesta una llamada extra inocua (el servidor responde `404`).
- **Contrato de la API documentado** en `diseno.md` §4.2 antes de que exista el backend: baja idempotente y **lápida** en el servidor (rechaza con `410` un `POST` tardío de un reporte ya dado de baja, cerrando las carreras "respuesta perdida" y "subida en vuelo").
- **Observación para la tesis** (`correcciones-documento.md`): el documento llama "anónimo" al `id_anonimo`; técnicamente es un seudónimo. No se edita el Anexo 9 (instrumento validado) — se deja anotado para el asesor.
- Pruebas: 11 unitarias nuevas + 3 de UI (compilan, sin emulador). Total del proyecto: **37 unitarias en verde**. Detalle en [`pruebas.md`](./pruebas.md).
- **Módulo 7 (Ajustes)** ya no necesita diseño propio para "borrar todos mis datos": reutilizará `eliminarReporte()` / `ProgramadorEliminacion` para cada reporte más `revocarConsentimiento()`.
- Se creó `docs/plan.md` → Módulo 6 marcado como completado.

## Pendientes / decisiones abiertas

- [x] Definir `applicationId` definitivo → `com.wanchaqlimpio.app` (2026-09-17)
- [ ] Decidir si homologar `targetSdk`/`compileSdk` a 34 (documento) o mantener 36 y documentar la razón
- [x] Inicializar git en el proyecto → hecho, Módulo 0
- [x] Crear la estructura de paquetes definida en `plan.md` §2 → hecho, Módulo 0
- [x] Configurar Hilt, Retrofit, Room base, ktlint/detekt → hecho, Módulo 0
- [x] Implementar el modelo de datos Room real (`Reporte`, `CategoriaResiduo`, `EstadoReporte`) según `diseno.md` §6, reemplazando `PruebaEntity` → hecho, Módulo 2
- [ ] Iniciar pipeline de entrenamiento del modelo MobileNetV2 en Colab (bloquea Módulo 3)
- [ ] Validar el diseño corregido con el asesor de tesis antes de avanzar en código, dado que se aparta del Capítulo IV del documento entregado
- [ ] Decidir si se actualiza el `.docx` de la tesis para reflejar el ADR-0001 (login eliminado)
- [ ] Decidir cuándo aplicar el primer lote de las 8 correcciones (CORR-001 a CORR-008) al `.docx`, y si las Ilustraciones 1, 2 y 6 (diagramas de casos de uso, arquitectura y entidad-relación) deben rehacerse en consecuencia
- [ ] Configurar el pinger anti-cold-start (cron-job.org → endpoint `/health` del backend) antes de iniciar la ventana de observación de 8 semanas
- [ ] `hiltViewModel()` (usado en `MainActivity.kt` y `OnboardingScreen.kt`) está deprecado en favor de un nuevo paquete `androidx.hilt.lifecycle.viewmodel.compose` — es solo un warning, no bloquea, pero conviene migrar cuando se estabilice esa API
- [ ] Correr `OnboardingContentTest` y `ReporteEstadoContentTest` (androidTest) en un emulador/dispositivo real al menos una vez — compilan pero no se ejecutaron en este entorno por no haber emulador disponible
- [ ] Probar manualmente en un dispositivo real la captura de cámara (CameraX) y la obtención de ubicación (`ProveedorUbicacionFused`) — son wrappers de hardware sin prueba automatizada, ver `pruebas.md`
- [ ] Reemplazar el bounding box aproximado de `LimiteDistritalWanchaq` por el polígono oficial del distrito de Wanchaq (catastro municipal/INEI) antes de la ventana de observación real; idealmente mover la validación definitiva al backend con PostGIS (`ST_Within`)
- [ ] Construir el backend real (Django + PostgreSQL/PostGIS en Neon + Cloudinary + Render) — bloquea probar el Módulo 4 contra un servidor real y bloquea también el Módulo 5 (Mapa). Requiere que el usuario cree primero la cuenta en Neon.
- [ ] Manejar el caso de foto faltante en `SyncWorker` como fallo permanente en vez de reintento infinito (hoy reintenta para siempre si el archivo se borró del dispositivo) — ver `pruebas.md`, Módulo 4
- [ ] Definir con el asesor la **política de eliminación tras el agrupamiento/verificación**: qué pasa con un `Punto_critico` cuando se da de baja un reporte que lo integraba (recalcular, marcar, o congelar tras la ventana de observación) — ver ADR-0002 y `diseno.md` §4.2
- [ ] Consultar al asesor la redacción "anónimo" → "seudónimo" (Ética 3.4 / Anexo 9 / Cap. IV) y que la tesis reconozca los límites de la eliminación sin login (reinstalación, datos ya agrupados) — ver `correcciones-documento.md`
- [ ] Al construir el backend: implementar el contrato de `diseno.md` §4.2 (POST idempotente por `(idAnonimo, idLocal)`, DELETE idempotente con lápida, `idAnonimo` nunca en respuestas públicas ni logs abiertos)
- [ ] "Mis reportes" no muestra la miniatura de la foto (para no sumar una librería de imágenes en una versión mínima) — considerar agregarla si las pruebas de usabilidad (SUS) lo piden

## Cómo usar este archivo

Cada vez que cerremos una sesión de trabajo se añade una entrada nueva en "Registro de sesiones" con la fecha, y se actualizan "Estado del proyecto Android" y "Pendientes" si algo cambió.
