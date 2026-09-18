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

---

## Cómo usar este archivo

Al cerrar cada módulo del plan de desarrollo, se agrega aquí su propia sección con la misma estructura de tabla. `progreso.md` solo debe referenciar este archivo, no duplicar el detalle de las pruebas.
