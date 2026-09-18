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
| `ReporteRepositoryImplTest.kt` (Robolectric, Room en memoria) | `un reporte valido se guarda como PENDIENTE y aparece al observar` | Persistencia offline-first real, y que se encola la sincronización (Módulo 4) tras guardar | RF-03, RNF-01 |
| | `un reporte rechazado no se persiste` | Un reporte inválido no llega a la base de datos ni se encola sincronización | Integridad del filtro antes de guardar |
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

## Módulo 4 — Sincronización diferida

Alcance: solo el cliente Android, contra un backend simulado (`MockWebServer3`, ver `plan.md` §4) — el backend real todavía no existe.

| Archivo | Caso de prueba | Qué verifica | Requisito / dimensión relacionada |
|---|---|---|---|
| `GeoJsonMapperTest.kt` | `el feature usa el orden longitud, latitud exigido por GeoJSON` | El orden de coordenadas es `[lon, lat]`, al revés del orden habitual — error clásico y fácil de cometer sin esta prueba | RFC 7946, diseno.md §3.6.4 |
| | `las propiedades reflejan idAnonimo, idLocal, categoria y fecha del reporte` | El resto del payload GeoJSON se arma correctamente; desde el Módulo 6 incluye `idLocal`, clave con la que el servidor identifica el reporte para la baja y para no duplicarlo en un reenvío | RF-03, diseno.md §4.2 |
| `SyncWorkerTest.kt` (Robolectric + `MockWebServer3` + Room en memoria) | `sin reportes pendientes el worker termina exitoso sin llamar a la red` | No se hacen peticiones innecesarias cuando no hay nada que sincronizar | RNF-06 (rendimiento) |
| | `un reporte pendiente que el servidor acepta queda SINCRONIZADO` | Camino feliz completo: Room → GeoJSON multipart → servidor → actualización de estado | RF-03, RNF-01 |
| | `si el servidor falla el reporte queda PENDIENTE y el worker pide reintentar` | El reporte no se pierde ni se marca falsamente como sincronizado ante un error del servidor (500) | RNF-01 (disponibilidad operativa) |
| | `si la foto ya no existe en disco se pide reintentar - limitacion conocida, ver progreso md` | Documenta el comportamiento actual (no ideal) ante un archivo de foto borrado: se reintenta indefinidamente en vez de marcarse como fallo permanente | Ver `progreso.md`, pendientes — limitación conocida, no arreglada en este módulo |

**Estado**: 6 pruebas unitarias en verde. No hay prueba de integración contra un backend real (todavía no existe) ni prueba manual de comportamiento de red real (avión, wifi inestable, etc.) — queda pendiente para cuando el backend esté desplegado.

## Módulo 6 — Mis reportes (con eliminación, ADR-0002)

| Archivo | Caso de prueba | Qué verifica | Requisito / dimensión relacionada |
|---|---|---|---|
| `ReporteRepositoryImplTest.kt` (Robolectric, Room en memoria) | `eliminar borra el reporte, su foto del disco y solicita la baja remota` | Las tres consecuencias de eliminar: fila borrada, **foto borrada del almacenamiento de la app** y solicitud de baja con la clave `(idAnonimo, idLocal)` correcta | RNF-05, ADR-0002, Ley N.º 29733 (derecho de cancelación) |
| | `eliminar solicita la baja remota tambien si el reporte figuraba como PENDIENTE` | Cierra el hueco "el servidor lo recibió pero la respuesta se perdió": el cliente no puede saberlo, así que la baja se pide siempre | RNF-05, RNF-01 |
| | `eliminar un reporte que no existe no hace nada` | Idempotencia local: no falla ni encola una baja fantasma | Robustez |
| | `eliminar tolera que la foto ya no exista en disco` | Un archivo ya borrado no impide eliminar el registro ni pedir la baja | Robustez |
| `EliminarReporteWorkerTest.kt` (Robolectric + `MockWebServer3`) | `envia un DELETE a reportes idAnonimo idLocal y termina exitoso si el servidor confirma` | Contrato de la API: método `DELETE` y ruta `/reportes/{idAnonimo}/{idLocal}` exactos (verificados sobre la petición real recibida, no solo el resultado) | diseno.md §4.2 |
| | `un 404 cuenta como exito porque el dato ya no existe en el servidor` | Idempotencia remota: si el servidor no tiene el reporte, el objetivo (que no exista) ya se cumple | diseno.md §4.2 |
| | `si el servidor falla se pide reintentar para no perder la solicitud de baja` | Un error `500` no descarta la baja: WorkManager la reintenta con backoff | RNF-05, RNF-01 |
| | `una solicitud sin datos de entrada falla sin llamar a la red` | Una solicitud mal formada no se reintenta indefinidamente ni toca la red | Robustez |
| `MisReportesViewModelTest.kt` (fakes + Turbine) | `muestra los reportes del dispositivo identificado por su id anonimo` | La lista se consulta por `id_anonimo`, no por una cuenta | ADR-0001 |
| | `sin consentimiento aceptado no hay reportes que mostrar ni se consulta el repositorio` | Sin `id_anonimo` no se consulta nada (no se filtra información de otro dispositivo) | ADR-0001, RNF-04 |
| | `eliminar delega en el repositorio y el reporte desaparece de la lista` | Flujo completo del ViewModel: eliminar → repositorio → la lista se actualiza sola (flujo reactivo de Room) | RNF-05 |
| `MisReportesContentTest.kt` (androidTest, Compose UI Testing) | `sinReportesSeMuestraElMensajeVacio` | Estado vacío visible | Usabilidad percibida |
| | `eliminarPideConfirmacionYSoloAlConfirmarSeInvocaElCallback` | Eliminar exige confirmación explícita: nada se borra al primer toque | RNF-05, prevención de borrado accidental |
| | `cancelarElDialogoNoEliminaNada` | Cancelar no elimina | RNF-05 |

**Estado**: 11 pruebas unitarias nuevas en verde (4 en `ReporteRepositoryImplTest`, 4 en `EliminarReporteWorkerTest`, 3 en `MisReportesViewModelTest`). Las 3 pruebas de UI (`MisReportesContentTest`) compilan pero no se ejecutaron por no haber emulador — se suman a las pendientes de correr en un dispositivo real (`OnboardingContentTest`, `ReporteEstadoContentTest`). **No hay prueba contra un backend real**: la eliminación remota solo se verificó contra `MockWebServer3`, así que el comportamiento real (lápida, `410 Gone`, etc.) depende de que el backend implemente `diseno.md` §4.2.

## Módulo 7 — Ajustes y "borrar todos mis datos"

| Archivo | Caso de prueba | Qué verifica | Requisito / dimensión relacionada |
|---|---|---|---|
| `BorrarTodosMisDatosTest.kt` (fakes que anotan en una lista común, para verificar el **orden**) | `elimina cada reporte, limpia las fotos y revoca el consentimiento al final` | Secuencia exacta: listar → eliminar cada reporte → borrar fotos sueltas → **revocar al final** | RNF-05, ADR-0002 |
| | `sin id anonimo no lista reportes pero igual limpia fotos y revoca` | Caso sin consentimiento previo: no falla y deja el dispositivo limpio | Robustez |
| | `si eliminar un reporte falla no se revoca el consentimiento para poder reintentar` | Si el borrado se interrumpe, el `id_anonimo` sigue existiendo y el usuario puede repetir la acción (no queda a medias y sin forma de reintentar); el error se propaga, no se silencia | RNF-05 |
| `AlmacenFotosImplTest.kt` (Robolectric, `filesDir` real) | `nuevaFoto devuelve un archivo dentro de la carpeta privada de la app y crea la carpeta` | Las fotos viven en almacenamiento privado (no en la galería/almacenamiento compartido) | Ética (fotos con rostros/placas) |
| | `borrar elimina la foto` · `borrar tolera que el archivo ya no exista` | Borrado individual, idempotente | RNF-05 |
| | `borrarTodas elimina las fotos, incluidas las huerfanas, y no toca otros archivos de la app` | El borrado total llega a fotos sin reporte asociado y no daña el resto de la app | RNF-05, ADR-0002 |
| `AjustesViewModelTest.kt` (fakes + Turbine, con una "compuerta" que mantiene el borrado en curso) | `muestra el codigo anonimo del consentimiento` | Ajustes expone el código que el usuario citará al escribir por sus datos | ADR-0002 (límite: reinstalación) |
| | `borrarTodo ejecuta el borrado completo y no queda en estado borrando` | Flujo completo y el estado vuelve a la normalidad | RNF-05 |
| | `mientras borra queda en estado borrando, un segundo toque se ignora y al terminar vuelve a false` | Protección contra doble toque: un solo borrado, botón bloqueado mientras dura | RNF-05, prevención de errores |
| `ReporteViewModelTest.kt` *(Módulo 2, ampliado en este módulo)* | `un reporte guardado conserva su foto` · `si el reporte es rechazado por la validacion su foto se borra del almacenamiento` · `si no hay GPS la foto tambien se borra...` · `si falta el consentimiento la foto tambien se borra` | **Corrige un hueco del Módulo 2**: la foto se guardaba en disco *antes* de validar, y si el reporte se rechazaba quedaba huérfana en el almacenamiento para siempre. Ahora se borra en todo camino que no termine en "Guardado" | Ética (rostros/placas), Ley N.º 29733 |
| `AjustesContentTest.kt` (androidTest, Compose UI Testing) | `muestraElCodigoAnonimoYElCorreoDeContacto` | Se ven el código y el correo | ADR-0002 |
| | `sinCorreoConfiguradoSeAvisaEnVezDeMostrarUnaDireccionVacia` | Estado sin correo definido: aviso visible (solo alcanzable en debug; ver guardia abajo) | Anexo 9 |
| | `borrarTodoPideConfirmacionYSoloAlConfirmarSeInvocaElCallback` · `cancelarElDialogoNoBorraNada` | Confirmación explícita antes de una acción irreversible | RNF-05 |
| | `mientrasSeBorraElBotonQuedaDeshabilitado` | Estado "borrando" visible y bloqueado en la UI | RNF-05 |

**Verificación adicional, no automatizada en JUnit — guardia de publicación del correo de contacto** (`verificarCorreoContacto` en `app/build.gradle.kts`): se comprobó manualmente con Gradle que (1) con `contactEmail` vacío la tarea **falla** con un mensaje explicativo, (2) con `-PcontactEmail=...` **pasa**, y (3) `assembleRelease`/`bundleRelease` dependen de ella (`--dry-run`). Así el pendiente "definir el correo de contacto" no puede olvidarse: publicar sin él es imposible.

**Estado**: 14 pruebas unitarias nuevas en verde (4 `AlmacenFotosImplTest`, 3 `BorrarTodosMisDatosTest`, 3 `AjustesViewModelTest`, 4 nuevas en `ReporteViewModelTest`). **Total del proyecto: 51 pruebas unitarias** en verde (50 propias + el `ExampleUnitTest` del wizard). Las 5 pruebas de UI de `AjustesContentTest` compilan pero no se ejecutaron (sin emulador). En total hay **4 clases de prueba de UI pendientes de correr en un dispositivo real**: `OnboardingContentTest`, `ReporteEstadoContentTest`, `MisReportesContentTest` y `AjustesContentTest`. Lo que **no** se puede verificar en automático es que el borrado remoto llegue a un servidor real (no existe todavía) ni el envío del correo (`Intent` a una app de correo real).

---

## Cómo usar este archivo

Al cerrar cada módulo del plan de desarrollo, se agrega aquí su propia sección con la misma estructura de tabla. `progreso.md` solo debe referenciar este archivo, no duplicar el detalle de las pruebas.
