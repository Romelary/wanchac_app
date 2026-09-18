# Pruebas — Wanchaq Limpio

> Registro vivo de todas las pruebas automatizadas del proyecto: qué verifica cada una y a qué requisito o dimensión de la matriz de operacionalización (Anexo 2) se conecta. Se actualiza cada vez que se agregan, modifican o eliminan pruebas — es evidencia directa de verificación para la tesis, no solo un detalle técnico. El estándar de qué pruebas son obligatorias por módulo vive en [`plan.md`](./plan.md) §1.5.

## Cómo ejecutar las pruebas

```bash
# Unitarias (JVM, incluye Robolectric donde haga falta)
./gradlew testDebugUnitTest

# De UI / instrumentadas (requieren emulador o dispositivo conectado)
./gradlew connectedDebugAndroidTest

# Todo junto con lint y compilación, tal como se verifica antes de cada commit
./gradlew assembleDebug assembleDebugAndroidTest ktlintCheck detekt testDebugUnitTest
```

## Notas técnicas de testing (para no repetir diagnósticos)

### DataStore + JVM unit test + Windows → usar Robolectric

Cualquier prueba unitaria que haga una **segunda escritura** sobre el mismo `DataStore<Preferences>` real (no mockeado) debe anotarse con `@RunWith(RobolectricTestRunner::class)`. Sin eso, `android.os.Build.VERSION.SDK_INT` vale `0` en un test JVM puro, DataStore internamente cae en el `File.renameTo()` heredado (en vez de `Files.move()`), y en Windows esa función **falla silenciosamente si el archivo destino ya existe** — la primera escritura funciona (el archivo no existe aún), la segunda falla siempre, de forma determinística. No es una condición de carrera y no se arregla con delays ni cambiando el dispatcher (se probaron y descartaron ambos). Ver `app/src/test/java/com/wanchaqlimpio/app/data/repository/ConsentimientoRepositoryImplTest.kt` para el patrón a replicar, y `app/build.gradle.kts` (`testOptions.unitTests.all.jvmArgs`) para los flags `--add-opens` que Robolectric necesita en JDK 17+/21.

### Compose UI Testing sin Hilt

Las pantallas se separan en un composable "stateless" (recibe callbacks simples, sin `hiltViewModel()`) y un wrapper delgado que sí conecta el ViewModel — así las pruebas de UI no necesitan levantar infraestructura de testing de Hilt (`HiltAndroidRule`, `@HiltAndroidTest`, etc.). Ver `OnboardingContent` (testeable) vs. `OnboardingScreen` (wrapper) como ejemplo a replicar en las próximas pantallas.

---

## Módulo 0 — Fundamentos del proyecto

No se agregaron pruebas de negocio propias; se usó el test placeholder del wizard (`ExampleUnitTest`, `ExampleInstrumentedTest`) únicamente para validar que la infraestructura (Hilt, Room, Retrofit, ktlint, detekt) compila y corre.

## Módulo 1 — Onboarding y consentimiento

| Archivo | Caso de prueba | Qué verifica | Requisito / dimensión relacionada |
|---|---|---|---|
| `ConsentimientoRepositoryImplTest.kt` | `antes de aceptar no hay id anonimo` | Estado inicial: sin `id_anonimo` ni fecha, `haConsentido = false` | Base para RNF-04 (sin datos personales antes de consentir) |
| | `aceptar genera un id anonimo y marca el consentimiento` | Al aceptar, se genera un UUID y se registra la fecha; `haConsentido = true` | ADR-0001 (id anónimo en vez de login) |
| | `aceptar dos veces conserva el mismo id anonimo` | El UUID no se regenera en aceptaciones repetidas (idempotencia) | Consistencia del identificador usado para el filtro de duplicados (RNF-07, `diseno.md`) |
| | `revocar borra el id anonimo` | Al revocar, se eliminan `id_anonimo` y fecha; vuelve a `haConsentido = false` | RNF-05 (derecho de eliminación / revocar consentimiento) |
| `OnboardingContentTest.kt` | `elBotonEmpiezaDeshabilitadoHastaAceptarElCheckbox` | El botón "Aceptar y continuar" está deshabilitado hasta marcar el checkbox, y se habilita al marcarlo | Consentimiento previo, expreso e inequívoco (Ley N.º 29733) |
| | `alAceptarSeInvocaElCallback` | Al presionar el botón ya habilitado, se invoca `onAceptar()` | Flujo completo de aceptación del Anexo 9 |

**Estado**: 4 pruebas unitarias (`ConsentimientoRepositoryImplTest`) en verde vía `testDebugUnitTest`. 2 pruebas de UI (`OnboardingContentTest`) compilan (`assembleDebugAndroidTest`) pero **no se han ejecutado** en este entorno por no haber emulador/dispositivo disponible — pendiente correrlas al menos una vez (ver `progreso.md`, pendientes).

## Módulo 2 — Captura y almacenamiento offline-first

| Archivo | Caso de prueba | Qué verifica | Requisito / dimensión relacionada |
|---|---|---|---|
| `ValidarNuevoReporteTest.kt` | `ubicacion dentro del limite y sin previos es valida` | Caso feliz: dentro de Wanchaq, sin duplicados | RF-02 |
| | `precision mayor a 15 metros se rechaza` | Filtro de precisión geoespacial | RF-02, indicador "reportes aceptados por el filtro de 15 m" (Anexo 2) |
| | `precision de exactamente 15 metros se acepta` | Caso límite del umbral (≤ 15 m, no < 15 m) | RF-02 |
| | `coordenada fuera del distrito de Wanchaq se rechaza` | Validación de límite distrital | CORR-007 (Wanchaq, no Cusco) |
| | `reporte a menos de 20 metros y 30 minutos del mismo dispositivo es duplicado` | Filtro de duplicados por cercanía + tiempo | RNF-07, criterio de exclusión de duplicados (§3.2 del documento) |
| | `mismo lugar pero despues de 30 minutos no es duplicado` | El límite temporal del filtro de duplicados se aplica correctamente | RNF-07 |
| | `dentro de la ventana de tiempo pero a mas de 20 metros no es duplicado` | El límite espacial del filtro de duplicados se aplica correctamente (no solo el temporal) | RNF-07 |
| `ReporteRepositoryImplTest.kt` (Robolectric, Room en memoria) | `un reporte valido se guarda como PENDIENTE y aparece al observar` | Persistencia offline-first real (no solo la regla de validación en aislado) | RF-03, RNF-01 |
| | `un reporte rechazado no se persiste` | Un reporte inválido no llega a la base de datos | Integridad del filtro antes de guardar |
| | `un segundo reporte del mismo dispositivo cerca y rapido se detecta como duplicado y no se guarda` | La regla de duplicados funciona integrada con datos reales ya persistidos, no solo con una lista en memoria pasada a mano | RNF-07 |
| `ReporteViewModelTest.kt` (dependencias falsas) | `estado inicial es Capturando` | Estado inicial correcto de la máquina de estados | — |
| | `al capturar una foto valida el estado termina en Guardado` | Flujo feliz completo: foto → ubicación → validación → guardado | RF-02, RF-03 |
| | `GPS impreciso termina en Rechazado y reintentar vuelve a Capturando` | Caso "GPS impreciso, se reintenta" exigido por `plan.md` Módulo 2 | RF-02 |
| | `sin senal de GPS termina en Rechazado sin llegar a validar en el repositorio` | Manejo del caso `ProveedorUbicacion` devuelve `null` (GPS apagado/sin señal) | RNF-01 (disponibilidad operativa) |
| | `sin consentimiento aceptado no se intenta crear el reporte` | Guarda de seguridad: no se puede reportar sin `id_anonimo` | ADR-0001 |
| `ReporteEstadoContentTest.kt` (androidTest, Compose UI Testing) | `estadoRechazadoMuestraElMensajeYElBotonReintentarFunciona` | El caso "GPS impreciso, se reintenta" también a nivel de UI | RF-02 |
| | `estadoGuardadoMuestraElMensajeDeExito` | Confirmación visual tras guardar | Usabilidad percibida |
| | `estadoProcesandoUbicacionMuestraElTextoDeCarga` | Estado de carga visible mientras se obtiene el GPS | Usabilidad percibida |

**Estado**: 15 pruebas unitarias en verde (`ValidarNuevoReporteTest` 7, `ReporteRepositoryImplTest` 3, `ReporteViewModelTest` 5). 3 pruebas de UI (`ReporteEstadoContentTest`) compilan pero no se ejecutaron por no haber emulador disponible. **No se prueba automáticamente** la captura real de cámara (CameraX) ni la obtención real de GPS (`ProveedorUbicacionFused`) — son wrappers delgados sobre hardware/Google Play Services, por diseño se dejan para prueba manual en dispositivo real (ver la nota de "qué se prueba y qué no" al inicio de este archivo). **Esa prueba manual todavía no se ha hecho** en ningún dispositivo/emulador — pendiente antes de dar el Módulo 2 por verificado end-to-end (ver `progreso.md`, pendientes). Lo que sí queda exhaustivamente probado en automático es toda la lógica de negocio que consume esos datos (validación, persistencia, máquina de estados).

---

## Cómo usar este archivo

Al cerrar cada módulo del plan de desarrollo, se agrega aquí su propia sección con la misma estructura de tabla. `progreso.md` solo debe referenciar este archivo, no duplicar el detalle de las pruebas.
