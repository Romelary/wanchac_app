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

## Módulo 3a — Pipeline de entrenamiento del clasificador (`ml/`)

Estas pruebas **no son de la app Android** sino del código Python que entrena el modelo. Se ejecutan con `python probar_notebook.py` desde `ml/entrenamiento/` (requiere TensorFlow, scikit-learn, matplotlib, pillow y nbformat; sin GPU, sin internet, sin Drive). El propósito es que un error a mitad del cuaderno —típicamente en la exportación a TFLite, que ocurre al final— **no se descubra después de haber gastado el tiempo de entrenamiento en Colab**.

Cómo funciona: `probar_notebook.py` ejecuta el **mismo** `clasificador_residuos.py` que se convierte en el cuaderno, con `SMOKE_TEST=1`. En ese modo se reemplazan los datos reales por (a) un zip falso con la misma estructura que `dataset-resized.zip` de TrashNet —incluida la carpeta `__MACOSX` de basura— para ejercitar el código real de la siembra, y (b) imágenes de ruido para `organico`, más 6 imágenes de ruido en `dataset_fuera_de_clase/` para ejercitar la celda 7b; el entrenamiento se reduce a 1 época por fase y el modelo arranca sin pesos de ImageNet (para no depender de la red). Todo lo demás (particiones, augmentación, entrenamiento en 2 fases, evaluación, umbral, exportación float16, verificación del `.tflite`, escritura de archivos) corre igual que en Colab.

| Clase (`probar_notebook.py`) | Caso de prueba | Qué verifica | Requisito / dimensión relacionada |
|---|---|---|---|
| `PruebaDeHumoDelCuaderno` | `test_el_cuaderno_corre_de_principio_a_fin` | El script termina con código 0 (imprime el final de la salida si falla) | RF-04 |
| | `test_genera_todos_los_archivos` | Existen y no están vacíos: `.tflite`, `labels.txt`, `metadata.json`, `metricas.json`, curvas y matriz de confusión | Trazabilidad (evidencia para la tesis) |
| | `test_labels_coincide_con_el_enum_de_kotlin` | **Contrato más frágil:** `labels.txt` es idéntico, y en el mismo orden, a los valores de `CategoriaResiduo` del código Kotlin **menos las categorías manuales `ESCOMBROS` y `MIXTO`** (que además deben figurar en `metadata.json → categorias_manuales` y no repetirse en `labels.txt`). Si difirieran, la app etiquetaría mal *todas* las predicciones sin dar ningún error | RF-04, `diseno.md` §5 |
| | `test_la_siembra_de_trashnet_mapea_las_carpetas_y_descarta_la_basura` | `plastic→plastico`; `cardboard`+`paper→carton` (con tope); `glass→vidrio`; `metal`, `trash` y `__MACOSX` no entran | RF-04, procedencia de datos |
| | `test_un_archivo_corrupto_se_descarta_y_se_informa_y_un_formato_no_soportado_se_avisa` | El dataset de prueba trae `corrupta.jpg` (bytes que no son imagen) y `foto.heic` (formato que TensorFlow no lee): el primero queda en `metadata.json → archivos_descartados` y no cuenta; el segundo se avisa por consola. Ninguno se ignora **en silencio** ni rompe el entrenamiento | Calidad de datos |
| | `test_la_evaluacion_fuera_de_clase_se_ejecuta_y_se_registra` | La celda 7b evalúa las fotos de `dataset_fuera_de_clase/` (6 de ruido en la prueba) y deja en `metricas.json → fuera_de_clase` el conteo, la tasa (0–1, coherente con el conteo), y los peores casos ordenados del más al menos seguro. Es la medición de que el modelo, sin clase `escombros`, no sugiera con aparente seguridad una categoría equivocada | RF-04, Anexo 2 (riesgo de la métrica) |
| | `test_probar_modelo_mide_fotos_reales_con_el_tflite_generado` | `probar_modelo.py` (la herramienta que decidirá el umbral con fotos de calle) corre contra el `.tflite` recién generado con 3 fotos de ruido por clase + 3 fuera de clase + 1 corrupta: cuenta 12 y 3 fotos, **informa** el archivo ilegible en vez de omitirlo en silencio, produce el barrido de 10 umbrales (0,50–0,95) y la cobertura **nunca sube** al subir el umbral. No evalúa calidad (son ruido), solo que la herramienta funciona | RF-04, Anexo 2 (riesgo de la métrica) |
| | `test_probar_modelo_falla_con_claridad_si_la_carpeta_no_existe` | Con una carpeta inexistente termina con error y el mensaje "No existe la carpeta de fotos", no con un rastro de excepción | Robustez |
| | `test_los_json_son_json_estandar` | `metadata.json` y `metricas.json` no contienen `NaN`/`Infinity` (Python los escribe sin quejarse, pero no son JSON válido y un lector estricto —como el de la app— fallaría). La tabla de umbrales guarda `null` cuando no hay predicciones aceptadas | Robustez del contrato |
| | `test_metadata_es_coherente` | `metadata.json` coincide con `labels.txt`, declara entrada `[1,224,224,3]` y salida `[1,4]`, umbral entre 0,50 (el piso) y 1, bandera `umbral_alcanza_precision_objetivo` booleana, y su `sha256` corresponde al `.tflite` guardado | Trazabilidad |
| | `test_el_tflite_tiene_la_entrada_y_salida_que_la_app_espera` | Cargando el `.tflite` con el intérprete: entrada `float32 [1,224,224,3]`, salida `[1,4]`, y una inferencia devuelve probabilidades válidas (suman 1, en 0–1) con píxeles en 0–255 | RF-04, contrato `diseno.md` §5 |
| `PruebaDelNotebook` | `test_el_notebook_generado_es_valido_y_compila` | El `.ipynb` que se genera valida contra `nbformat` y **cada celda de código compila** (un `#%%` mal cortado sería un `SyntaxError`) | Mantenibilidad |
| | `test_el_ipynb_versionado_esta_al_dia` | El `.ipynb` guardado en git coincide celda por celda con el `.py`: no se sube a Colab una versión distinta de la que se probó | Mantenibilidad |

**Lo que la prueba de humo ya cazó** (sirve de evidencia de que vale la pena): al inspeccionar la salida real del cuaderno se encontraron y corrigieron (1) `ignore_errors()` dejaba el tamaño del dataset "desconocido" y Keras avisaba *"Your input ran out of data"* al final de cada época —un mensaje alarmante pero inofensivo—, ahora las imágenes se validan por adelantado; (2) el umbral de confianza podía quedar en 0,30 (casi azar con 5 clases) si el dataset era fácil, ahora tiene un piso de 0,50; (3) `tf.lite.Interpreter` está en desuso (TensorFlow avisa que lo eliminará), ahora se prefiere LiteRT con respaldo; (4) la exportación imprimía ~150 líneas de ruido; (5) `metricas.json` podía contener `NaN`, que no es JSON válido.

**Estado**: **13 pruebas de humo en verde** (11 de `PruebaDeHumoDelCuaderno` + 2 de `PruebaDelNotebook`; las 2 últimas en añadirse cubren `probar_modelo.py`; se añadió la de fuera de clase y se adaptaron las de contrato al modelo de 4 clases; ~2 min en CPU) con Python 3.12.2, TensorFlow 2.21.0, Keras 3.15.1, scikit-learn 1.9.1.

Además, el propio cuaderno lleva **verificaciones en tiempo de ejecución** (no son JUnit, pero protegen la corrida real): se detiene si alguna clase tiene menos de 50 fotos; compara el `.tflite` contra el modelo Keras original sobre imágenes reales de *test* y **aborta si la concordancia es < 97 %** (o si la probabilidad difiere más de 0,05); y fija todas las semillas para que la corrida sea reproducible.

**Qué NO comprueban estas pruebas** (dicho sin rodeos, para que nadie cite lo que no se midió):
- **La calidad del modelo.** Los datos de la prueba son ruido: sirven para probar la *tubería*, no el *aprendizaje*. Las métricas reales solo se obtienen en Colab con fotos verdaderas.
- **Que corra en Colab: verificado el 2026-09-18.** El cuaderno corrió de punta a punta en Colab (TensorFlow 2.20.0) y produjo un `.tflite` cuyo `sha256` coincide con `metadata.json` y que concuerda con el modelo Keras en 99,0 % de las fotos de prueba. Sigue **sin** probarse: el camino LiteRT (`ai_edge_litert`), el comportamiento con fotos de calle y con fotos fuera de clase.
- **La descarga real de TrashNet.** La estructura del zip se verificó el 2026-09-18 leyendo su directorio central en Hugging Face (2 527 imágenes; cardboard 403, glass 501, metal 410, paper 594, plastic 482, trash 137), pero la descarga de 43 MB en sí ocurre solo en Colab.
- **El indicador del Anexo 2.** La coincidencia IA–observador ≥ 70 % se mide en campo, no aquí.

## Módulo 3b — El modelo de IA dentro de la app

Aquí conviven pruebas de JVM (todas **ejecutadas**) y pruebas de dispositivo (**compiladas, no ejecutadas**: no hay emulador ni teléfono en este entorno). LiteRT solo corre en Android, así que la lógica se probó con dobles y la inferencia real queda para un dispositivo.

| Archivo | Caso de prueba | Qué verifica | Requisito / dimensión relacionada |
|---|---|---|---|
| `ConfiguracionModeloTest.kt` (11) | contrato válido · umbral de la app sobre el sugerido · etiqueta que no es categoría · labels y metadata en distinto orden · categoría manual repetida como clase · clase repetida · entrada no cuadrada / de otro nº de canales · salida que no coincide con las clases · umbral fuera de (0, 1] · metadata vacío | La app **rechaza con un mensaje claro** un modelo cuyo contrato no cuadra, en vez de etiquetar mal *todas* las predicciones sin dar error | RF-04, `ml/README.md` (contrato) |
| `InterpretarSalidaTest.kt` (9) | clase más probable · con poca confianza propone `MIXTO` y **conserva lo que dijo el modelo** · confianza igual al umbral cuenta · empate determinista · nunca `MIXTO`/`ESCOMBROS` como clase del modelo · nº de clases distinto · `NaN`/infinito/negativos · una salida que **no suma 1** (típico de logits) · sin clases | El umbral se aplica bien y una salida que no es una distribución de probabilidad se descarta, porque entonces la "confianza" no significaría lo que el umbral supone | RF-04 |
| `ClasificadorResiduoImplTest.kt` (9) | sugerencia con umbral · `MIXTO` con poca confianza · foto ilegible → `null` **sin invocar al modelo** · fallo del modelo → `null` **y error registrado con su causa** · salida inválida → `null` · una **cancelación** de la corrutina no se confunde con un fallo · el clasificador perezoso no carga hasta usarse y carga una sola vez · si la carga falla (p. ej. `UnsatisfiedLinkError` en un ABI sin librería) se registra **una vez** y la app sigue con elección manual · `ClasificadorNoDisponible` | La IA es una ayuda: **cualquier fallo la deja en "sin sugerencia" y nunca impide reportar** (RNF-01) | RF-04, RNF-01 |
| `PreprocesadorImagenTest.kt` (3) | píxeles como `float` R, G, B en 0–255 sin normalizar · búfer directo, en orden nativo y del tamaño exacto (224·224·3·4 B) · el canal alfa se ignora | La entrada que recibe el modelo respeta el contrato (la normalización va *dentro* del modelo, no se repite aquí) | RF-04 |
| `DecodificadorFotosTest.kt` (7) | una foto de 12 MP se reduce sin bajar de 224 px · el lado menor manda (foto vertical) · imagen pequeña no se reduce · siempre potencia de 2 · las **8 orientaciones EXIF** → giro y espejo · orientación desconocida no transforma · el caso real de CameraX (giro 90°) | Lógica pura del decodificador. La orientación EXIF importa: sin ella el modelo vería las fotos verticales acostadas | RF-04 |
| `ContratoModeloAssetsTest.kt` (6) | los 3 archivos existen · labels y metadata **reales** cumplen el contrato · orden de clases = enum `CategoriaResiduo` menos las manuales (espejo de la prueba de Python) · umbral 0,70 · **el `sha256` del `.tflite` empaquetado coincide con `metadata.json`** · cada corrección manual deja campo, antes, después y motivo | Los archivos que viajan dentro de la app son un par consistente; si alguien cambia el modelo sin actualizar el contrato, falla aquí | RF-04, trazabilidad |
| `MigracionesTest.kt` (2, Robolectric) | se construye una base con el esquema **exacto de la versión 1** y un reporte, y se abre con la versión 2: el reporte se **conserva** y queda sin sugerencia · tras migrar se guardan reportes con sugerencia | La migración no pierde reportes de quien ya tenía la app; Room además valida que el esquema resultante coincide con las entidades | RNF-01 |
| `ReporteViewModelTest.kt` (21; antes 9) | se analiza y se pide confirmar con lo que propone el modelo · mientras analiza el estado es `Clasificando` · si el modelo duda se preselecciona `MIXTO` pero se conserva la sugerencia · sin modelo se elige a mano · el ciudadano corrige la categoría · **confirmar guarda la categoría del ciudadano y la sugerencia cruda por separado** · sin modelo se guarda sin sugerencia · confirmar sin foto no hace nada · **un doble toque en guardar crea un solo reporte** · descartar borra la foto · GPS impreciso / sin señal / sin consentimiento · las fotos de reportes rechazados o descartados **se borran** · al iniciar se **limpian las fotos huérfanas** conservando las de los reportes existentes (con límite de tiempo) y no se limpia nada sin consentimiento | RF-04 (corrección manual), RNF-05 y Ética (ninguna foto queda huérfana), Anexo 2 (medir la IA aparte) |
| `ReporteRepositoryImplTest.kt` (+2) · `GeoJsonMapperTest.kt` (+2) · `AlmacenFotosImplTest.kt` (+2) | la sugerencia cruda se guarda aparte de la categoría confirmada, y queda vacía sin modelo · en el JSON viaja aparte y, sin sugerencia, **esos campos no se envían** (Gson omite los nulos) · `borrarHuerfanas` borra solo las fotos sin reporte y anteriores al límite, **nunca una en uso**, y tolera que la carpeta no exista | Persistencia y contrato con el backend (`diseno.md` §4.2); privacidad de las fotos | Anexo 2, RNF-05 |
| `ReporteConfirmacionContentTest.kt` (6, androidTest, **no ejecutada**) | con sugerencia segura dice cuál es y la deja seleccionada · si el modelo dudó **no afirma nada** y pide elegir · sin modelo se elige a mano · se ofrecen las 6 categorías (incluidas las manuales) · al tocar una se avisa cuál · "Guardar reporte" y "Tomar otra foto" invocan sus callbacks | La pantalla de confirmación (RF-04: "con opción de corrección manual") | RF-04, usabilidad |
| `ReporteEstadoContentTest.kt` (+1, androidTest, **no ejecutada**) | el estado `Clasificando` muestra que se analiza la foto | Estado de espera visible | Usabilidad |
| `ClasificadorResiduoReferenciaTest.kt` (4, androidTest, **no ejecutada**) | el modelo empaquetado es el que usó Python · las **probabilidades de la app coinciden (±0,05) con las de Python** en 4 imágenes de referencia, una con EXIF · el clasificador completo da la misma clase y confianza · una foto inexistente no da sugerencia ni rompe | **Es la única comprobación automática de la inferencia real**: canales RGB, escala 0–255, orientación EXIF y lectura de la salida | RF-04 |

**Sensibilidad de las referencias** (comprobada en Python, `sensibilidad_referencias.py`, no automatizada): con las 4 imágenes, una escala 0–1 en vez de 0–255 cambia las probabilidades en 0,16 a 0,59 (detectable, la tolerancia es 0,05); ignorar la orientación EXIF, en 0,55; y el orden de canales BGR, en 0,09 a 0,58 en 3 de las 4 (las franjas marrón/beige apenas cambian: 0,006). Es decir, la prueba **discrimina** los errores para los que existe.

**Estado**: **116 pruebas unitarias en verde** (antes 51). ktlint y detekt limpios; `assembleDebug` y `assembleDebugAndroidTest` compilan. Las **11 pruebas de dispositivo** de este módulo (6 + 1 + 4) compilan pero **no se han ejecutado**; se suman a las pendientes de correr en un teléfono (`OnboardingContentTest`, `ReporteEstadoContentTest`, `MisReportesContentTest`, `AjustesContentTest`).

**Lo que NO cubren estas pruebas**, dicho sin rodeos: que LiteRT cargue y corra en un teléfono real, la decodificación de una foto real de cámara con EXIF (`BitmapFactory` solo existe en Android), el flujo cámara → confirmar de punta a punta, la latencia y la memoria, y el redimensionado de fotos grandes (las referencias miden justo 224 px; la app reduce con `inSampleSize` + escalado bilineal y Python con `tf.image.resize`: una diferencia pequeña que solo se cuantifica con fotos reales).

---

## Cómo usar este archivo

Al cerrar cada módulo del plan de desarrollo, se agrega aquí su propia sección con la misma estructura de tabla. `progreso.md` solo debe referenciar este archivo, no duplicar el detalle de las pruebas.
