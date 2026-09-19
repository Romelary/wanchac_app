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
- **Sincronización:** hay tres formas de probar el envío. **(a)** Contra el **servidor real**: Render con la Fase 1 desplegada y `baseUrl` en `local.properties` (escenario C10). **(b)** Contra el **servidor falso local** de la sección 2c, que provoca a voluntad fallos difíciles de repetir en la calle (servidor caído, respuesta perdida, red lenta) sin tocar la base de datos real. **(c)** Sin servidor: con la dirección por defecto (`http://10.0.2.2:8000/`, que solo vale en un emulador) los reportes se guardan y quedan **"Pendiente de envío"**, con el motivo del último fallo.

## 2. Instalar y ejecutar

```bash
./gradlew installDebug                  # instala la app en el dispositivo conectado
./gradlew connectedDebugAndroidTest     # ejecuta las 26 pruebas de dispositivo; AL TERMINAR DESINSTALA LA APP y se borran sus datos
```

**Aviso (medido el 2026-09-19):** `connectedDebugAndroidTest` **desinstala la app al terminar**, y con ella la base de datos, las fotos y el consentimiento. No correrlo en un teléfono con reportes que importen, y volver a instalar con `installDebug` después. La pantalla debe estar **encendida y desbloqueada** durante la corrida (las pruebas abren pantallas de verdad); con el bloqueo puesto, fallarían por eso.

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
- **Reinicios de la app:** la cuenta de pasos vuelve a cero cada vez que arranca la app, y Android puede arrancarla solo (p. ej. para enviar pendientes). Por eso cada arranque usa además una **columna distinta** (≈ 33 m al este por cada minuto del reloj); sin eso, los reportes hechos tras un reinicio repetirían los puntos de los anteriores y saldría «Ya reportaste este mismo lugar…» (se vio en la tablet el 2026-09-19 y se corrigió).
- **Lo que NO prueba:** el GPS real, ni el filtro de 15 m con lecturas reales. Eso solo se prueba con la variante normal, en exteriores dentro de Wanchaq (escenario C8).

## 2c. Servidor falso local (probar el envío sin Render)

Para lo que Render no permite provocar a voluntad: un servidor que se cae, que **recibe el reporte y pierde la respuesta**, que tarda o que no contesta nunca. Es una herramienta de **prueba**: **no sustituye** a la prueba contra el servidor real (C10), no usa ninguna cuenta ni secreto y solo funciona con compilaciones de **depuración** (la de publicación bloquea todo lo que no sea `https`).

```bash
python herramientas/servidor_falso.py                                            # 1. el servidor, en el puerto 8000 del PC
"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" reverse tcp:8000 tcp:8000     # 2. el dispositivo lo ve como «localhost»
./gradlew installDebug -PubicacionSimulada=true -PbaseUrl=http://localhost:8000  # 3. la app apuntando a él
```

Su comportamiento se cambia **sin reiniciar**, escribiendo una palabra en `herramientas/modo.txt`:

| Modo | Qué hace el servidor | Qué debe hacer la app |
|---|---|---|
| `ok` | Responde 201 (y **200** si ya tenía ese reporte) | «Enviado» |
| `503` (o `500`, `429`…) | Ese código HTTP | Sigue «Pendiente de envío», «Intentos: N · último fallo: el servidor no está disponible», y reintenta a los 30 s, 60 s, 90 s… |
| `422` (o `400`, `413`…) | Ese código HTTP | «No se pudo enviar», con **Reintentar** y **Eliminar**; no reintenta sola |
| `lento:20` | Espera 20 s y responde 201 | Espera; «Enviado» al terminar |
| `corta` | Corta la conexión a mitad del envío | «sin conexión» y reintenta |
| `perdida` | **Guarda** el reporte y corta **sin responder** | Cree que falló y reintenta; con `ok` el servidor contesta 200 y **no duplica** |
| `colgado` | Acepta y no contesta nunca | A los **90 s** «el servidor tardó demasiado en responder» |
| `focos` | `GET /puntos-criticos` devuelve **2 focos INVENTADOS** dentro de Wanchaq (los `POST` se portan como `ok`) | El mapa dibuja 2 marcadores y dice «2 focos confirmados» (escenario G7). **Son datos de prueba: no enseñarlos como reales** |

El servidor escribe una línea por petición (`idLocal`, categoría, tamaño de la foto, código devuelto y cuántos reportes distintos lleva) y guarda las fotos que recibe en `herramientas/recibidas/`. **No imprime coordenadas** ni el código anónimo completo. Después, para volver a la configuración normal: `./gradlew installDebug` (sin `-PbaseUrl`) y `adb reverse --remove tcp:8000`.

**Dos avisos.** (1) Los reportes de esta prueba **no llegan a Neon**: se quedan en el servidor falso, así que **no** cuentan para el indicador del Anexo 2. (2) Si después se instala una compilación que apunta a Render, los reportes que sigan `PENDIENTE` **sí** se enviarán allí con sus coordenadas simuladas: antes, dejarlos «Enviado» (con el servidor falso en `ok`) o eliminarlos.

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
| B6 | Fijarse en los **íconos** de la navegación | Mapa (pin), Reportar (más), Mis reportes (lista) y Ajustes (engranaje). *Resuelto el 2026-09-19 (Sesión 2): antes eran los provisionales del asistente de Android Studio (casa, corazón, cuenta)* |

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
| G7 | Con un foco de prueba en el servidor (hoy el endpoint es **provisional y siempre devuelve vacío**: hace falta la Fase 3 real, `avance.md` A15, o un dato de prueba) | Aparece su marcador; al tocarlo muestra categoría, nº de reportes y fecha; el contador dice "1 foco confirmado"  · *Con el servidor falso en modo `focos` (§2c) se ven 2 focos inventados, sin esperar a la Fase 3.* |
| G8 | Abrir **Mapa** tras un rato sin usar la app (servidor dormido) | La primera petición puede tardar hasta ≈ 1 min (Render despierta) y **no** da error de tiempo agotado |

### H. Confiabilidad del envío y del GPS (protocolo completo en [`pruebas-de-confiabilidad.md`](./pruebas-de-confiabilidad.md))
Los reportes de estas pruebas conviene hacerlos con la variante de **ubicación simulada** (sección 2b) para no depender del GPS, salvo H9 (que **es** del GPS). Los que dicen «servidor falso» usan la sección 2c; los demás, el servidor real con `baseUrl`.

| # | Qué hacer | Esperado |
|---|---|---|
| H1 | **Modo avión y cierre forzado.** Modo avión → guardar 3 reportes → *Ajustes de Android → Apps → Wanchaq Limpio → Forzar detención* → apagar el modo avión → abrir la app y esperar ≈ 2 min | Los 3 pasan de «Pendiente de envío» a **«Enviado»** y en el servidor hay **3**, no más. En la Galaxy Tab S9+ (Android 16) el sistema reinició el envío **por su cuenta** tras un cierre forzado (Sesión 2, abajo): no darlo por hecho en otros equipos; la app además encola un envío al abrirse |
| H2 | **Reinicio del teléfono con pendientes.** Modo avión → guardar 2 reportes → reiniciar el teléfono → desbloquearlo → apagar el modo avión **sin abrir la app** y esperar ≈ 2 min → abrir la app | Siguen pendientes tras el reinicio y se envían solos al volver la red (WorkManager se reprograma al arrancar, pero solo tras el **primer desbloqueo**: la base de datos está en almacenamiento cifrado). Si solo llegan al abrir la app, anotarlo: es el límite del ahorro de energía del fabricante |
| H3 | **Cinco reportes seguidos** (con red y servidor despierto): guardar 5 en menos de 2 min | Los 5 quedan «Enviado» pocos segundos después del último; el servidor tiene 5, sin repetidos; el registro de envíos muestra `intentos` = 1 en todos |
| H4 | **Wi-Fi sin salida a Internet (R4).** Conectarse a un Wi-Fi sin Internet → guardar 2 reportes → esperar 1 min → dar salida a Internet | «Pendiente de envío» mientras no haya Internet (`red_al_guardar` = `SIN_INTERNET` en el registro); se envían solos al recuperar la salida, sin abrir la app |
| H5 | **Eliminar un pendiente.** Modo avión → guardar un reporte → «Eliminar» → apagar el modo avión | No aparece en «Mis reportes» y en Neon **no** queda ese `id_local` (la baja se encola y el servidor deja una lápida: si el reporte llegara después, no se guardaría) |
| H6 | **Servidor dormido (R6).** ≥ 15 min sin tráfico (sin *pinger*) → guardar **un** reporte y cronometrar hasta «Enviado» | Tarda hasta ≈ 1 min (Render despierta) y **termina en «Enviado»**; puede verse un intento con «el servidor tardó demasiado en responder» (la espera de lectura es de 90 s). Anotar los segundos |
| H7 | **Fecha del teléfono mal puesta.** Poner la fecha 2 años atrás (a mano) → guardar → esperar | El servidor rechaza (422): «No se pudo enviar». **«Reintentar» no lo arregla**: la fecha ya quedó guardada en el reporte; hay que eliminarlo y volver a reportar con la fecha automática |
| H8 | **Peso y tamaño de la foto.** Guardar un reporte y ver `adb shell run-as com.wanchaqlimpio.app ls -l files/reportes` | La foto nueva pesa **menos de ≈ 1 MB** (lado largo 1600 px, sin metadatos; en la Galaxy Tab S9+, ≈ 0,2 MB). Las hechas con versiones anteriores pesan ≈ 4 MB |
| H9 | **GPS real** (variante **normal**). Primero en interiores y luego en exteriores dentro de Wanchaq: abrir la cámara, esperar, tomar la foto y guardar | Con la cámara abierta el aviso pasa de «buscando señal» a «listo ±N m» o «impreciso ±N m». Con GPS impreciso, al guardar **espera hasta 30 s** («Precisión actual: ~N m…») y, si no llega a 15 m, dice «El GPS aún no es lo bastante preciso» con **«Reintentar ubicación»** y **conserva la foto**. Anotar los segundos hasta «listo» y la precisión final |
| H10 | **Permisos.** *Ajustes de Android → Apps → Wanchaq Limpio → Permisos*: quitar cámara y ubicación → «Reportar» | Pantalla «Permisos necesarios» con «Conceder permisos» y «Abrir los ajustes de la app»; si solo se concede la ubicación **aproximada** sigue pidiendo (debe ser precisa); al volver de los Ajustes con todo concedido aparece la cámara sola |
| H11 | **Registro de envíos.** *Ajustes → «Compartir registro de envíos»* → mandarlo a un correo o a un chat | Llega texto con líneas `#` (versión de la app, modelo y hora), un encabezado y **un renglón por reporte**; **sin** coordenadas, fotos ni código anónimo |

## 4. Lo que se espera que falle o haya que mirar (hipótesis antes de probar)

Salen de leer el código, **no de haber probado**; cada una se confirma o descarta en la sesión:

1. **Barras del sistema:** la app dibuja de borde a borde (obligatorio con `targetSdk` 36). Todas las pantallas usan `Scaffold` desde el Módulo 3b; si algo queda bajo la barra de estado, es un fallo.
2. **Android 16 en pantallas grandes:** con `targetSdk` 36, Android **ignora** las restricciones de orientación y de tamaño de ventana en pantallas de 600 dp o más (la app llena toda la ventana y gira libremente) y no permite renunciar a dibujar de borde a borde. Es decir, en la tablet la app *tiene* que funcionar girada, en ventana y en pantalla dividida.
3. **Rotación de la cámara:** al girar 180° Android no recrea la actividad; se agregó un aviso a CameraX (`MantenerRotacionDeCaptura`) para que la foto no quede boca abajo. **Sin probar en dispositivo** → escenario C3.
4. **Modelo de IA:** primera vez que corre LiteRT de verdad. Si las librerías nativas no cargan en la tablet, la app sigue con elección manual y lo deja en el registro (`logcat`, etiqueta `ClasificadorResiduo`). **Verificado el 2026-09-19 (Sesión 2, C4):** las librerías nativas cargan en la tablet y el modelo sugirió «Plástico» ante un objeto de plástico; la **latencia** y la **exactitud con fotos de calle** siguen sin medirse.
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
| G1–G8 | **Parcial** (2026-09-19) | **G1, G2 y G7 OK** con la clave real (§5d). G3 no aplica (la clave ya está). **Pendientes**: G4 (límite de desplazamiento), G5 (sin red), G6 (girar y pantalla dividida), G8 (servidor dormido) |
| H1–H11 | ☐ | H9 necesita la variante **normal** y exteriores; H1 a H8 y H11, la de ubicación simulada; H6 y H4, servidor real |
| `connectedDebugAndroidTest` | **OK** (2026-09-19) | 26 ejecutadas en la Galaxy Tab S9+ · **4 fallaron la primera vez** (2 del consentimiento: la casilla y el botón quedan bajo el borde visible en horizontal y había que desplazarse hasta ellos; 2 de la referencia del clasificador: tolerancia demasiado estrecha, ver `pruebas.md`) → corregidas, **26 de 26 en verde** |

## 5b. Sesión 1 — 2026-09-18, Galaxy Tab S9+ (solo lo que se verificó)

Dispositivo: **Galaxy Tab S9+ Wi-Fi (SM-X810)**, Android 16 (API 36), One UI 8.0.5, pantalla de 1752×2800 px a 340 dpi (**824 × 1318 dp**; en horizontal, 1318 × 824 dp). App: compilación de depuración del commit `2d9fced`, instalada por `adb` (`Success`: el *Auto Blocker* no lo impidió). Captura: [`evidencia/2026-09-18_tab-s9plus_consentimiento_horizontal.png`](./evidencia/2026-09-18_tab-s9plus_consentimiento_horizontal.png).

| # | Resultado | Observación |
|---|---|---|
| Instalación y arranque | **OK** | La app abre y no registra avisos ni errores en `logcat`; no hay cierre inesperado |
| A2 (ancho de lectura) | **OK** (medido) | En horizontal, el botón "Aceptar y continuar" mide **592 dp** (= 640 − 24 − 24) y queda **centrado** (margen de 363 dp a cada lado); los párrafos no se estiran a toda la pantalla |
| A1 (barras del sistema) | **Parcial** | El botón inferior no queda tapado por la barra de tareas de Samsung. Falta revisar la parte de arriba, sin desplazar la pantalla, y en vertical |
| Resto de escenarios | *pendiente* | A3–A4, B1–B6, C, D, E y F |

## 5c. Sesión 2 — 2026-09-19, Galaxy Tab S9+ (con servidor falso; solo lo que se verificó)

Dispositivo: el mismo (**SM-X810**, Android 16 / API 36, One UI 8.0.5, pantalla de 2800 × 1752 px en horizontal). App: compilación de depuración con **ubicación simulada** (`1.0-ubicacion-simulada`) del árbol de trabajo posterior al commit `b928066` (los lotes 2 y 3 aún sin confirmar), instalada **sobre la versión de la Sesión 1** con `adb` (mismo firmante: conserva los datos). Servidor: **`herramientas/servidor_falso.py`** en el PC, alcanzado con `adb reverse` (§2c). **Nada de esto tocó Render ni Neon.** Las horas de abajo son las del dispositivo (UTC−5).

| Escenario | Resultado | Observación (medido en el dispositivo) |
|---|---|---|
| Instalar sobre la versión anterior | **OK** | Los datos se conservaron: base de datos **v2 con 4 reportes** `PENDIENTE` de la Sesión 1 (3 fotos de ≈ 4 MB y 1 de ≈ 1 MB) |
| Arranque | **OK** | Inicio en frío de 659 a 783 ms; sin errores ni avisos propios en `logcat` |
| **Migración de Room 2→3 con datos reales** | **OK** | `user_version` 2 → 3; los 4 reportes intactos (categoría, estado, sugerencia de la IA, fecha) y con las columnas nuevas de seguimiento |
| WorkManager con la fábrica de Hilt | **OK** | `Starting work for …SyncWorker` en el primer segundo, **sin** `Could not create Worker` (el arreglo de la Sesión 24 funciona en un dispositivo real) |
| Espera lineal entre reintentos | **OK** | Sin servidor: `ECONNREFUSED` → `SIN_CONEXION`; intentos a las 14:04:53, 14:05:23 (+30 s) y 14:06:23 (+60 s) |
| Envío de los 4 pendientes (C10, con servidor falso) | **OK** | 201 ×4 en un segundo; cuerpos de 4 048 696 / 4 056 316 / 3 967 972 / 974 421 bytes; las fotos recibidas pesan **lo mismo** que en la tablet y son JPEG válidos. Lo que viaja: `categoria`, `categoriaIa`, `confianzaIa`, `fechaHoraEpochMillis`, `idAnonimo`, `idLocal` (**nada del seguimiento local**) |
| C1–C2 Cámara | **OK** | Vista previa nítida y el indicador «**GPS listo · ±5 m**» (ubicación simulada) |
| C3 Foto derecha | **Parcial** | Derecha en la confirmación **en horizontal**; **no se giró la tablet** (vertical y 180° siguen pendientes) |
| C4 Confirmación | **OK** | «Sugerencia automática: Plástico» ante un objeto de plástico (las librerías de LiteRT cargan), las 6 categorías, «Guardar reporte» y «Tomar otra foto» |
| C5 Guardar y «Mis reportes» | **OK** | «¡Reporte guardado!…»; en la lista con «Enviado» y «Enviado el …»; el más nuevo arriba |
| **H8 Peso y tamaño de la foto** | **OK** | Versión anterior: **4128 × 3096 px, ≈ 4,0 MB**, con EXIF. Versión nueva: **1600 × 1200 px, 234 769 bytes (≈ 0,2 MB)**, **sin EXIF** (orientación ya aplicada, sin ubicación). 17 veces menos |
| B6 Íconos | **OK** | Mapa, Reportar, Mis reportes y Ajustes con sus íconos (ya no los provisionales) |
| E2 Eliminar un reporte | **OK** | Pide confirmación; la baja llegó al servidor falso (`DELETE … → 204`) |
| C7 Duplicado (< 30 min y < 20 m) | **OK** | «Ya reportaste este mismo lugar hace menos de 30 minutos.» con «Reintentar» (apareció por el defecto 2, abajo) |
| Servidor con **503** | **OK** | Sigue «Pendiente de envío»: «Intentos: 1 · último fallo: el servidor no está disponible» |
| Servidor con **422** | **OK** | «**No se pudo enviar**: el servidor no aceptó este reporte…» con **Reintentar** y **Eliminar**; no reintenta solo. En la base: `RECHAZADO`, `RECHAZADO_POR_EL_SERVIDOR`, 422, 2 intentos, `red_al_guardar` = `WIFI` |
| **Respuesta perdida** (el servidor guarda y corta) | **OK** | Reintentó (3 envíos en el mismo segundo por la librería de red, y luego cada 30 s y 60 s); al pasar a `ok` el servidor contestó **200** y quedó «Enviado». **El servidor tiene 1 copia**, no varias |
| **Cierre forzado a mitad del envío** (H1, en parte) | **OK** | El reporte llegó **una sola vez** (el segundo envío recibió 200). **El sistema reinició el envío por su cuenta** un instante después del cierre (`Start proc … for service SystemJobService`): buena noticia, pero **no dar por hecho en otros equipos** |
| Servidor que **no contesta** | **OK** | A los **90 009 ms** `SocketTimeoutException` → «TIEMPO_AGOTADO» → «el servidor tardó demasiado en responder». **Mientras tanto el siguiente reporte esperó** (defecto 4) |
| «**Enviar ahora**» con una petición colgada | **OK** | Canceló la petición colgada (`Socket closed` tras 85 s) y **mandó los dos reportes en 3 segundos**; el aviso «2 reportes sin enviar» desapareció. La petición cancelada **no** contó como intento |
| H10 Permisos (en parte) | **Parcial** | Sin cámara y con solo la ubicación **aproximada**: «Permisos necesarios», «Conceder permisos» y la ayuda «La ubicación debe ser «precisa»…»; «Abrir los ajustes de la app» abre los Ajustes de Android; tras conceder (con `adb`) y volver con Atrás, **la cámara aparece sola**. **Falta**: el diálogo del sistema y la doble negativa |
| H3 Cinco reportes seguidos | **No concluyente → OK** | El primer intento reveló el defecto 2 (ubicación simulada repetida tras un reinicio); corregido y **repetido con la versión final: OK** (tabla de abajo) |

**Defectos hallados en esta sesión** (ninguno lo habían visto las pruebas automáticas; detalle y correcciones en `progreso.md`, Sesión 25):
1. **«Enviado» se anotaba con la hora de salida de la petición**, no con la de la respuesta (un envío de 25 s quedó anotado 25 s antes). **Corregido**, con prueba.
2. **La ubicación simulada repetía los mismos puntos tras reiniciar la app**, y los reportes salían como duplicados. **Corregido** (cada arranque usa otra columna, según el minuto del reloj), con prueba.
3. **Un intento interrumpido no se cuenta** en `intentos`. **Documentado** (`pruebas-de-confiabilidad.md` §9).
4. **Un envío que no termina retrasa a los demás** hasta 90 s. **Documentado**, con la alternativa.

**Versión final (con `decodificarArchivo`) verificada en la misma sesión**, cuando se pudo desbloquear la tablet:

| Escenario | Resultado | Observación |
|---|---|---|
| Foto nueva, clasificación y guardado | **OK** | Foto de **1600 × 1200 px y 246 769 bytes, sin EXIF**; «Sugerencia automática: Plástico»; el servidor la recibió |
| **H3 Cinco reportes seguidos** | **OK** | Los 5 guardados en ≈ 20 s cada uno (cadencia del script); **los 5 llegaron** (2xx ×5), todos `SINCRONIZADO` con **`intentos` = 1**, sin repetidos y sin el rechazo por «mismo lugar» (el defecto 2 quedó corregido) |
| Ajustes: sección «Registro de envíos» | **OK** | Entre «Contacto» y «Tus datos», con el texto que dice qué no incluye |
| **H11 Compartir el registro** | **Parcial** | Se abre el selector «Compartir texto» de Android y su vista previa muestra las líneas de cabecera (`# Wanchaq Limpio 1.0-ubicacion-simulada`, `# samsung SM-X810, Android 16 (API 36)…`). **No se envió a nadie** ni se comprobó el texto completo en un correo o chat |
| `connectedDebugAndroidTest` (D7) | **OK tras corregir** | 26 pruebas de UI y de referencia del clasificador en el dispositivo: la primera vez 22 pasaron y 4 fallaron; ver `pruebas.md`, «Pruebas instrumentadas en el dispositivo» |

Capturas (sin datos personales): [«Mis reportes» con un rechazo 422 y los enviados](./evidencia/2026-09-19_tab-s9plus_mis-reportes-rechazado-y-enviados.jpg) y [con un pendiente por servidor no disponible](./evidencia/2026-09-19_tab-s9plus_mis-reportes-pendiente-503.jpg).

**Otra consecuencia de esa corrida**: `connectedDebugAndroidTest` **desinstaló la app** de la tablet (y con ella los reportes de prueba). Hubo que reinstalarla con `installDebug`.

**Sin verificar en la tablet** (necesitan al usuario o al GPS): girar el equipo y pantalla dividida (B1, B2, C3), modo avión, Wi-Fi sin salida y reinicio (H1 a H4), servidor dormido (H6), fecha mal puesta (H7), **GPS real** (C8, H9), que el registro de envíos llegue completo a un correo o chat (H11), un teléfono, y **todo contra Render**.

## 5d. Sesión 3 — 2026-09-19, Galaxy Tab S9+ (el mapa con la clave real de Google Maps)

La clave (`MAPS_API_KEY`) la puso el usuario en `local.properties`; **no se leyó ni se imprimió** (solo se comprobó, contando líneas, que existe y tiene el formato de una clave de Google, sin comillas ni espacios). App: compilación de depuración con ubicación simulada, apuntando al servidor falso local (`-PbaseUrl=http://localhost:8000`), porque **Render todavía no tiene `/puntos-criticos`** (falta el `git push`). El mapa no usa la ubicación del teléfono.

| Escenario | Resultado | Observación |
|---|---|---|
| **G1** Pestaña «Mapa» con la clave | **OK** | Se dibuja el **mapa de Google** de Cusco y Wanchaq **con calles y nombres** (no un fondo gris), los controles de zoom, el logotipo «Google» y «©2026 Google». Sin el aviso rojo de «falta la clave» |
| **G2** Sin focos | **OK** | La tarjeta «**Aún no hay focos confirmados**» con su explicación y el botón «**Actualizar**»; el servidor falso registró `GET /puntos-criticos → 200 (vacío)`. «Actualizar» **hace una petición nueva** |
| **G7** Con focos | **OK** | Con el servidor falso en modo `focos` (2 focos **inventados**), «Actualizar» los trae: aparece el aviso «**2 focos confirmados**», el mapa se **encuadra** para mostrar los dos marcadores y, al tocar uno, sale «**Cartón** · 4 reportes · detectado el 17 sept 2026, 4:48 p.m.» |
| Autorización de Google en `logcat` | **OK** | Ningún aviso de autorización (`Authorization failure`…); `MapsInitializer` cargó el renderizador `LATEST`. (Las líneas de `logcat` que traen la clave se **enmascaran** antes de mirarlas.) |

Captura (los focos son inventados, sin datos personales): [mapa con los dos focos de prueba](./evidencia/2026-09-19_tab-s9plus_mapa-con-focos-de-prueba.jpg).

**Sin verificar**: G4 (arrastrar el mapa fuera del distrito: que tope), G5 (modo avión: «No se pudo conectar…» con **Reintentar**), G6 (girar la tablet y pantalla dividida), G8 (servidor dormido: hasta ≈ 1 min sin error) y **todo contra Render** (el servidor real). **Tampoco se puede saber desde aquí** si la clave está bien **restringida** (paquete + SHA-1 + solo *Maps SDK for Android*): el mapa se dibuja igual con una clave sin restringir; eso se revisa en la consola de Google Cloud.

---

## 6. Dispositivos probados

| Dispositivo | Android / One UI | Fecha | Resultado global | Notas |
|---|---|---|---|---|
| Galaxy Tab S9+ (SM-X810) | Android 16 / One UI 8.0.5 | 2026-09-18 y 2026-09-19 | *en curso* (ver 5b, 5c y 5d) | Sesión 1: instalación y consentimiento en horizontal. **Sesión 2**: migración de la base, envío y fallos contra un **servidor falso**, foto reducida, permisos, íconos; **Sesión 3**: el mapa con la clave real de Google Maps (G1, G2, G7); falta girar el equipo, el GPS real, modo avión / Wi-Fi sin salida / reinicio y el servidor real |
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

**Se hizo (2026-09-19, Sesión 25)**: la app ya **mantiene el GPS encendido con la cámara abierta**, muestra su estado («GPS listo · ±5 m»…), usa la lectura del momento de la foto y, si no era buena, **espera hasta 30 s** a una de ≤ 15 m sin borrar la foto (ver [ADR-0007](./adr/0007-gps-en-caliente-y-foto-reducida.md)). El criterio de 15 m no cambió. **Sigue sin probarse con GPS real** (escenarios C8 y H9): el ajuste de esos 30 s depende de lo que se vea en campo.

