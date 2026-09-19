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

- **Ubicación:** la app **rechaza a propósito** los reportes que caen fuera del distrito de Wanchaq (RF-02, CORR-007). Si pruebas en otro distrito verás *"Esta ubicación está fuera del distrito de Wanchaq"*; no es un fallo. Además, la precisión del GPS debe ser ≤ 15 m: en interiores suele no lograrse. Prueba el reporte **en exteriores dentro de Wanchaq**, o dilo y se agrega un modo solo-QA.
- **Sincronización:** el servidor todavía no existe (`BASE_URL` apunta a `10.0.2.2`, que solo vale en un emulador). En un dispositivo real los reportes se guardan y quedan **"Pendiente de envío"**: es lo esperado.

## 2. Instalar y ejecutar

```bash
./gradlew installDebug                  # instala la app en el dispositivo conectado
./gradlew connectedDebugAndroidTest     # ejecuta las pruebas de dispositivo (dejan la app instalada)
```

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
| C7 | Reportar fuera de Wanchaq / con GPS impreciso / dos veces el mismo lugar en < 30 min | Mensaje de rechazo claro y botón "Reintentar" |

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
| D1–D5 | ☐ | |
| E1–E4 | ☐ | |
| F1–F4 | ☐ | |
| `connectedDebugAndroidTest` | ☐ | pruebas ejecutadas: ___ · fallidas: ___ |

## 6. Dispositivos probados

| Dispositivo | Android / One UI | Fecha | Resultado global | Notas |
|---|---|---|---|---|
| Galaxy Tab S9+ | | | *pendiente* | |
| Un teléfono (recomendado además de la tablet: las tabletas no representan a quien reporta en la calle) | | | *pendiente* | |
