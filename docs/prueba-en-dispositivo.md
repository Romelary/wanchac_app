# Prueba en dispositivo real — protocolo y registro

> Las pruebas automáticas (ver [`pruebas.md`](./pruebas.md)) no pueden comprobar lo que solo existe en un teléfono o una tablet reales: la cámara, las barras del sistema, la rotación física, la velocidad y que las librerías nativas de LiteRT carguen. Este documento es el **protocolo** para hacerlo y el **registro** de lo que se encontró. Es evidencia de verificación para la tesis: se llena en cada sesión de prueba, incluso cuando todo sale bien.

## 1. Preparar el dispositivo (primera vez)

**En la tablet** (Galaxy Tab S9+, One UI):

1. *Ajustes → Acerca de la tablet → Información de software* → toca **Número de compilación** 7 veces (pide el PIN).
2. *Ajustes → Opciones de desarrollador* → activa **Depuración por USB**.
3. Conecta la tablet a la PC con un cable USB-C **de datos** (no todos los cables cargan y transmiten) y acepta el mensaje *"¿Permitir la depuración por USB?"* (marca "Permitir siempre desde este equipo").
4. **Si la PC no la ve:** revisa *Ajustes → Seguridad y privacidad → Auto Blocker* (One UI 6.1 y posteriores). Con él activo puede bloquear los comandos por cable USB y la instalación de apps desde el equipo; desactívalo mientras pruebas.

**En la PC** (`adb` viene con Android Studio):

```bash
"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" devices -l
```

Debe aparecer la tablet como `device` (no `unauthorized`, no `offline`). Si le pides a Claude que la revise, puede instalar la app, ejecutar las pruebas de dispositivo, tomar capturas y leer el registro (`logcat`) por ti.

**Antes de probar el reporte** — dos cosas que se pasan por alto:

- **Ubicación:** la app **rechaza a propósito** los reportes con error de GPS mayor de 15 m y los que caen fuera del distrito de Wanchaq (RF-02, CORR-007). En interiores el GPS casi nunca llega a 15 m, y fuera de Wanchaq siempre saldrá *"fuera del distrito"*: no es un fallo. Hay dos formas de probar el reporte: **(a) la real**, en exteriores dentro de Wanchaq con cielo despejado, o **(b) el modo de ubicación simulada** de la sección 2b, para ejercitar el resto del flujo estés donde estés.
- **Sincronización:** el servidor todavía no existe (`BASE_URL` apunta a `10.0.2.2`, que solo vale en un emulador). En un dispositivo real los reportes se guardan y quedan **"Pendiente de envío"**: es lo esperado.

## 2. Instalar y ejecutar

```bash
./gradlew installDebug                  # instala la app en el dispositivo conectado
./gradlew connectedDebugAndroidTest     # ejecuta las pruebas de dispositivo (dejan la app instalada)
```

## 2b. Modo de ubicación simulada (solo para pruebas)

Sirve para probar guardar, Mis reportes, duplicados, etc. cuando el dispositivo **no está en Wanchaq o no tiene buena señal GPS**. Es una compilación de **depuración** con la ubicación falsa: cada reporte lleva una coordenada dentro de Wanchaq con 5 m de precisión, que se corre ≈ 33 m al norte en cada uso (así dos reportes seguidos no se descartan como duplicados).

```bash
./gradlew installDebug -PubicacionSimulada=true    # instala la variante de PRUEBA (ubicación falsa)
./gradlew installDebug                             # vuelve a la variante NORMAL (ubicación real)
```

- **Cómo saber cuál está instalada:** *Ajustes → Versión*. La de prueba dice `1.0-ubicacion-simulada`.
- **Desde el botón Run de Android Studio:** agrega la línea `ubicacionSimulada=true` a tu `gradle.properties` **de usuario** (`%USERPROFILE%\.gradle\gradle.properties`), **nunca** al del proyecto (ese sí se sube al repositorio). Quítala al terminar.
- **No puede llegar a una versión de publicación:** en `release` la bandera está fija en `false` y, además, `assembleRelease` y `bundleRelease` **fallan** con un mensaje claro si la propiedad está activa (verificado).
- **Los datos son falsos:** esos reportes no sirven como datos de la investigación. Cuando exista el servidor, una compilación de prueba debe apuntar a un servidor de desarrollo, no al real.
- **Lo que NO prueba:** el GPS real, ni el filtro de 15 m con lecturas reales. Eso solo se prueba con la variante normal, en exteriores dentro de Wanchaq (escenario C8).

## 3. Escenarios de prueba

Marca cada uno con **OK**, **FALLA** o **N/A** y anota lo observado en la sección 5. Los números de la columna "Esperado" son las expectativas de diseño, no medidas.

### A. Primer arranque y consentimiento
| # | Qué hacer | Esperado |
|---|---|---|
| A1 | Abrir la app recién instalada | Aparece el consentimiento; nada oculto tras la barra de estado ni la cámara frontal |
| A2 | Leer el texto en horizontal y en vertical | Las líneas **no** se estiran de borde a borde (ancho de lectura ≈ 640 dp, centrado) |
| A3 | Sin marcar la casilla, tocar "Aceptar y continuar" | Botón deshabilitado; se habilita al marcar |
| A4 | Aceptar | Pasa a la app con la navegación (barra inferior en vertical estrecho, riel lateral en pantallas anchas) |

### B. Adaptación de pantalla (la razón de usar una tablet)
| # | Qué hacer | Esperado |
|---|---|---|
| B1 | Girar la tablet a 0°, 90°, 180° y 270° en **cada** pantalla | Nada se corta, se solapa ni queda tras una barra del sistema; no se reinicia lo que el usuario estaba haciendo |
| B2 | Pantalla dividida con otra app (y ventana flotante si hay) | La app se adapta al nuevo tamaño y sigue usable |
| B3 | *Ajustes → Pantalla → Tamaño de fuente y estilo*: fuente grande | Los textos crecen sin cortarse; los botones siguen alcanzables |
| B4 | Modo oscuro | Texto legible, sin bloques ilegibles |
| B5 | Riel de navegación vs. barra inferior | En horizontal ancho aparece un riel lateral; los tres destinos funcionan |
| B6 | Fijarse en los **íconos** de la navegación | Ojo: son provisionales del asistente de Android Studio (casa, corazón, cuenta) y **no** corresponden a Reportar / Mis reportes / Ajustes: es un pendiente de pulido, no una regresión |

### C. Reporte de punta a punta
| # | Qué hacer | Esperado |
|---|---|---|
| C1 | Abrir "Reportar" y conceder permisos de cámara y ubicación | Pide ambos; si se niegan, muestra la explicación y el botón para reintentar |
| C2 | Vista previa de la cámara en vertical y en horizontal | Imagen derecha, sin estirarse ni verse girada |
| C3 | Tomar una foto (vertical, horizontal y **girando la tablet 180°**) | En la confirmación la foto se ve **derecha** en todos los casos (no acostada ni boca abajo) |
| C4 | Pantalla de confirmación | Foto, categoría sugerida (o "No pude sugerir…"), las 6 categorías y los botones "Guardar reporte" / "Tomar otra foto" |
| C5 | Cambiar la categoría y guardar | Mensaje de éxito; aparece en "Mis reportes" con la categoría **que elegiste** |
| C6 | "Tomar otra foto" | Vuelve a la cámara; la foto descartada no queda guardada |
| C7 | Reportar fuera de Wanchaq / con GPS impreciso / dos veces el mismo lugar en < 30 min | Mensaje de rechazo claro y botón "Reintentar" (con la variante **normal**) |
| C8 | **GPS real:** variante normal, **en exteriores dentro de Wanchaq** con cielo despejado; esperar unos 30 s con la cámara abierta y reportar | Se acepta (error ≤ 15 m). Anota los segundos que tardó y, si se rechaza, la precisión que dice el mensaje |
| C9 | **Ubicación simulada:** variante de prueba, guardar dos reportes seguidos | Ambos se guardan (el segundo no es duplicado) y aparecen en "Mis reportes" |
| C10 | **Sincronización real** (necesita `baseUrl` en `local.properties` apuntando a Render **con la Fase 1 desplegada**, y red): guardar un reporte con conexión y esperar hasta ≈ 2 min (Render puede estar despertando) | En "Mis reportes" pasa de "Pendiente de envío" a **"Enviado"** y el reporte aparece en la base de Neon. **Si se queda en "Pendiente de envío"**: mirar Logcat por `Could not create Worker` (señal de que WorkManager no usa la fábrica de Hilt: ver `pruebas.md`, «Inicialización de WorkManager») y por errores HTTP |

### D. Clasificador de IA (modelo v1 provisional)
Ver la nota de la sección 4. Para cada foto anota: qué era, qué sugirió, qué categoría propuso la app y cuánto tardó.

| # | Foto | Esperado |
|---|---|---|
| D1 | Una botella de plástico | Sugiere plástico (o duda → "Mixto"); **no es una garantía**: el modelo no se ha medido con fotos de calle |
| D2 | Cartón, vidrio y restos de comida | Ídem |
| D3 | Un muro, la vereda o escombros (fuera de las 4 clases) | Lo ideal es "Mixto"/"No pude sugerir"; **si sugiere algo con seguridad, es el riesgo que se quiere medir** (ver `ml/README.md`) |
| D4 | Poca luz o foto movida | No debe romper la app |
| D5 | Latencia entre tomar la foto y ver la confirmación | Debería sentirse inmediata; anota los segundos aproximados |

### E. Mis reportes y Ajustes
| # | Qué hacer | Esperado |
|---|---|---|
| E1 | "Mis reportes" con y sin reportes | Lista con categoría, fecha y estado; mensaje vacío si no hay |
| E2 | Eliminar un reporte | Pide confirmación; desaparece con su foto |
| E3 | Ajustes → código anónimo, correo, versión | Se ven completos y el código se puede seleccionar |
| E4 | "Borrar todos mis datos" | Pide confirmación; deja la app como recién instalada (vuelve al consentimiento) |

### F. Robustez
| # | Qué hacer | Esperado |
|---|---|---|
| F1 | Modo avión y reportar | Se guarda igual ("Pendiente de envío") |
| F2 | Girar la tablet mientras dice "Analizando la foto…" | No se pierde la foto ni se cierra la app |
| F3 | En la confirmación, salir a otra app y volver | Sigue en la confirmación con la misma foto |
| F4 | En la confirmación, **cerrar la app a la fuerza** y reabrirla | La app arranca normal; la foto sin confirmar se borra sola en el siguiente arranque (verificable con `adb shell run-as com.wanchaqlimpio.app ls files/reportes`) |
### G. Mapa público (necesita la clave de Google Maps: ver [`despliegue-mapa.md`](./despliegue-mapa.md))
| # | Qué hacer | Esperado |
|---|---|---|
| G1 | Tocar la pestaña **Mapa** (la primera de la barra) con la clave configurada | Se ve el **mapa de Wanchaq** con calles, **no un fondo gris**; controles de zoom visibles |
| G2 | Sin ningún foco confirmado (lo normal hoy; **necesita el endpoint provisional desplegado**, `despliegue-mapa.md` §3) | El mapa del distrito y una tarjeta "Aún no hay focos confirmados"; el botón **Actualizar** responde |
| G3 | Sin `MAPS_API_KEY` en `local.properties`, en una compilación de depuración | Aviso **rojo** arriba diciendo que falta la clave; el resto de la app funciona |
| G4 | Arrastrar y alejar el mapa hacia fuera del distrito | No deja irse a más de ≈ 1 km de Wanchaq ni alejar más allá del zoom mínimo |
| G5 | Modo avión y tocar **Mapa** (o **Actualizar**) | "No se pudo conectar con el servidor…" con **Reintentar**; al volver la red, Reintentar carga |
| G6 | Girar la tablet y usar pantalla dividida con el mapa abierto | El mapa y las tarjetas se adaptan; nada queda tras una barra del sistema; las tarjetas no se estiran de borde a borde |
| G7 | Con un foco de prueba en el servidor (hoy el endpoint es **provisional y siempre devuelve vacío**: hace falta la Fase 3 real, `avance.md` A15, o un dato de prueba) | Aparece su marcador; al tocarlo muestra categoría, nº de reportes y fecha; el contador dice "1 foco confirmado" |
| G8 | Abrir **Mapa** tras un rato sin usar la app (servidor dormido) | La primera petición puede tardar hasta ≈ 1 min (Render despierta) y **no** da error de tiempo agotado |

## 4. Lo que se espera que falle o haya que mirar (hipótesis antes de probar)

Salen de leer el código, **no de haber probado**; cada una se confirma o descarta en la sesión:

1. **Barras del sistema:** la app dibuja de borde a borde (obligatorio con `targetSdk` 36). Todas las pantallas usan `Scaffold` desde el Módulo 3b; si algo queda bajo la barra de estado, es un fallo.
2. **Android 16 en pantallas grandes:** con `targetSdk` 36, Android **ignora** las restricciones de orientación y de tamaño de ventana en pantallas de 600 dp o más (la app llena toda la ventana y gira libremente) y no permite renunciar a dibujar de borde a borde. Es decir, en la tablet la app *tiene* que funcionar girada, en ventana y en pantalla dividida.
3. **Rotación de la cámara:** al girar 180° Android no recrea la actividad; se agregó un aviso a CameraX (`MantenerRotacionDeCaptura`) para que la foto no quede boca abajo. **Sin probar en dispositivo** → escenario C3.
4. **Modelo de IA:** primera vez que corre LiteRT de verdad. Si las librerías nativas no cargan en la tablet, la app sigue con elección manual y lo deja en el registro (`logcat`, etiqueta `ClasificadorResiduo`).
5. **Redimensionado de fotos grandes:** la app reduce con `inSampleSize` + escalado de Android y el entrenamiento usó `tf.image.resize`; es una diferencia pequeña pero sin medir con fotos reales (ver `progreso.md`, Sesión 18).

## 5. Registro de resultados

Copia esta tabla por sesión. Para capturas y registro:

```bash
adb exec-out screencap -p > captura.png       # captura de pantalla
adb logcat -d -s ClasificadorResiduo ReporteScreen AlmacenFotos DecodificadorFotos   # registro de la app
```

**Sesión** — fecha: ____ · dispositivo: Galaxy Tab S9+ · Android / One UI: ____ · build (`versionName`): 1.0 · commit: ____

| # | Resultado | Observación (qué pasó, captura, segundos) |
|---|---|---|
| A1 | ☐ | |
| A2 | ☐ | |
| A3 | ☐ | |
| A4 | ☐ | |
| B1 | ☐ | |
| B2 | ☐ | |
| B3 | ☐ | |
| B4 | ☐ | |
| B5 | ☐ | |
| C1 | ☐ | |
| C2 | ☐ | |
| C3 | ☐ | |
| C4 | ☐ | |
| C5 | ☐ | |
| C6 | ☐ | |
| C7 | ☐ | |
| C8–C10 | ☐ | C10 necesita `baseUrl` (Render con la Fase 1 desplegada) |
| D1–D5 | ☐ | |
| E1–E4 | ☐ | |
| F1–F4 | ☐ | |
| G1–G8 | ☐ | requiere la clave de Google Maps |
| `connectedDebugAndroidTest` | ☐ | pruebas ejecutadas: ___ · fallidas: ___ |

## 5b. Sesión 1 — 2026-09-18, Galaxy Tab S9+ (solo lo que se verificó)

Dispositivo: **Galaxy Tab S9+ Wi-Fi (SM-X810)**, Android 16 (API 36), One UI 8.0.5, pantalla de 1752×2800 px a 340 dpi (**824 × 1318 dp**; en horizontal, 1318 × 824 dp). App: compilación de depuración del commit `2d9fced`, instalada por `adb` (`Success`: el *Auto Blocker* no lo impidió). Captura: [`evidencia/2026-09-18_tab-s9plus_consentimiento_horizontal.png`](./evidencia/2026-09-18_tab-s9plus_consentimiento_horizontal.png).

| # | Resultado | Observación |
|---|---|---|
| Instalación y arranque | **OK** | La app abre y no registra avisos ni errores en `logcat`; no hay cierre inesperado |
| A2 (ancho de lectura) | **OK** (medido) | En horizontal, el botón "Aceptar y continuar" mide **592 dp** (= 640 − 24 − 24) y queda **centrado** (margen de 363 dp a cada lado); los párrafos no se estiran a toda la pantalla |
| A1 (barras del sistema) | **Parcial** | El botón inferior no queda tapado por la barra de tareas de Samsung. Falta revisar la parte de arriba, sin desplazar la pantalla, y en vertical |
| Resto de escenarios | *pendiente* | A3–A4, B1–B6, C, D, E y F |

---

## 6. Dispositivos probados

| Dispositivo | Android / One UI | Fecha | Resultado global | Notas |
|---|---|---|---|---|
| Galaxy Tab S9+ (SM-X810) | Android 16 / One UI 8.0.5 | 2026-09-18 | *en curso* (ver 5b) | Instalación y consentimiento en horizontal verificados; falta el resto |
| Un teléfono (recomendado además de la tablet: las tabletas no representan a quien reporta en la calle) | | | *pendiente* | |

## 7. Problemas conocidos al preparar la prueba

### Android Studio muestra "Unresolved reference 'hilt' / 'dagger' / 'AndroidEntryPoint' / 'hiltViewModel' / 'R'" y no deja ejecutar

**Síntoma**: en el editor, `MainActivity.kt` marca en rojo las importaciones de Hilt y todos los `R.drawable…`, y el botón Run no funciona con normalidad.

**Causa (verificada el 2026-09-18 con el registro del IDE)**: el IDE **no volvió a sincronizar** con Gradle. Al abrir el proyecto registró *"Up-to-date models found in the cache. Not invoking Gradle sync"* y trabajó con un modelo de dependencias antiguo (probablemente del 17-sep, anterior a que se agregara Hilt). No es un fallo de compilación: la compilación por Gradle **sí termina** (el propio IDE generó `app-debug.apk` a las 17:04, y la compilación limpia por línea de comandos también).

**Solución**:
1. *File → Sync Project with Gradle Files* (el ícono del elefante). Espera a que termine (la primera vez tarda).
2. Si sigue en rojo: *File → Invalidate Caches… → Invalidate and Restart*.
3. Si el problema persiste, copia el **primer** mensaje de error de la pestaña *Sync* o *Build* (no la lista de "Unresolved reference": esos son consecuencia).

**Alternativa sin el IDE** (siempre funciona): `./gradlew installDebug` con la tablet conectada, o pedirle a Claude que instale el APK por `adb`.

### La app dice que el GPS es insuficiente / no asigna la ubicación

**Qué significa**: el reporte se rechaza porque el error de posición que informa el sistema es **mayor de 15 m** (RF-02). No es que la app "no asigne" la ubicación: la obtiene y la descarta por imprecisa. Se ve **al final** del flujo (al pulsar "Guardar reporte"), después de la foto y de la sugerencia de la IA.

**Qué se encontró en la primera sesión (2026-09-18, Galaxy Tab S9+, dentro de un edificio)** — leído de la propia tablet con `adb shell dumpsys location`:
- La tablet **sí tiene receptor GPS** (`android.hardware.location.gps`), la ubicación estaba en alta precisión y la app tenía el permiso de ubicación precisa.
- Su última lectura GPS tenía **≈ 52 m de error, con solo 5 satélites y señal débil**; la de red (Wi-Fi/celular), ≈ 116 m. Es lo normal bajo techo; con cielo abierto suele bajar de 10 m. El tiempo medio hasta el primer fijo de esa tablet es de ≈ 6 s.
- Además, esa ubicación estaba a **decenas de kilómetros de Wanchaq**: aunque el GPS hubiera sido perfecto, el siguiente rechazo habría sido *"fuera del distrito"*.

**Qué hacer**: para probar el flujo en cualquier lugar, la variante de la sección 2b. Para probar el GPS de verdad, salir a un lugar despejado dentro de Wanchaq (escenario C8).

**Pendiente, decidir con evidencia** (no antes): hoy el código toma **la primera lectura** que devuelve el servicio de ubicación de Google y la valida; no espera a que la precisión mejore. Si en exteriores con cielo despejado el reporte sigue rechazándose por precisión, hay que hacer que la app **espere hasta ≈ 30 s a una lectura ≤ 15 m** (mostrando "Buscando señal GPS…") antes de rechazar, sin cambiar el criterio de 15 m. No se cambió todavía porque no hay evidencia de que haga falta: el fallo observado se explica por estar en interiores.

