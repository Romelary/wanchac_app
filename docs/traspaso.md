# Traspaso para el chat nuevo (2026-09-19)

> Para retomar sin releer la conversación anterior. Lee esto, la memoria del proyecto y solo lo que cada paso indique. El detalle vive en [`progreso.md`](./progreso.md) (Sesiones 24 a 27), [`avance.md`](./avance.md), [`distribucion-apk.md`](./distribucion-apk.md) y los ADR 0006 y 0007.

## Meta
La app debe funcionar **como si fuera el producto final, para pruebas**: el equipo la usará para recoger datos y medir la confiabilidad. Los datos que queden en el servidor real **se purgan si hace falta** (dicho por el usuario). Play Store va después, con la app terminada.

## Estado (todo confirmado en git; último commit de esta línea de trabajo: `366f724`, más el de este traspaso)
- **Hecho y verificado en la Galaxy Tab S9+:** captura con GPS en caliente y foto reducida, envío confiable (ADR-0006), migración de la base, mapa con la clave real de Google Maps (G1, G2, G7), registro de envíos (CSV), GPS real (rechaza «fuera del distrito»), 26 pruebas de UI. **288 pruebas unitarias** en verde, ktlint y detekt limpios.
- **Firma del APK de publicación** lista en `app/build.gradle.kts` (llave y contraseñas en `local.properties`, que escribe el usuario); verificada solo con una llave de mentira.
- **Correo de contacto** ya definido en `gradle.properties`. `baseUrl` ya está en `local.properties` (apunta a Render): **una compilación sin `-PbaseUrl=http://localhost:8000` manda los reportes al servidor real**.
- **Sin `git push` todavía** (`git log origin/main..HEAD`): Render aún no tiene la Fase 1 ni `/puntos-criticos`.

## Decisiones ya tomadas (no reabrir)
1. **Dos APK al final.** (a) **Real**: `assembleRelease`, firmado, con la restricción de ubicación (≤ 15 m y dentro de Wanchaq). (b) **Simulada**: solo puede ser de **depuración** (`assembleDebug -PubicacionSimulada=true`); `release` la rechaza a propósito. **Propuesta a confirmar con el usuario:** darle a la simulada otro `applicationId` (`applicationIdSuffix`) y otro nombre («Wanchaq Limpio (simulada)») para que **convivan** en el mismo teléfono sin borrar datos; eso exige **una entrada más** en la clave de Maps (paquete nuevo + SHA-1 de depuración).
2. **Precisión del GPS (Paso 1):** guardar `precisionMetros` de la lectura aceptada. App: Room 3→4 con migración, `Reporte`, mapper, dato **opcional** en el `geojson`, columna en el registro CSV. Servidor: columna opcional `error_posicion_m` con validación (0 a unos cientos de metros), migración y pruebas. ADR corto. Es la precisión **declarada por el teléfono** (radio del 68 % de `Location.getAccuracy()`, de memoria), no el error real. Conviene avisar al asesor porque es un dato más que se registra (consentimiento). **Orden de despliegue: primero el servidor, luego el APK** (si no, el servidor podría rechazar el dato nuevo).
3. **Focos automáticos, sin confirmación (Paso 2):** el servidor calcula los focos **al pedir** `GET /puntos-criticos`: DBSCAN sencillo en Python (distancia de Haversine, ε = 50 m, MinSamples = 4), sin tabla ni tarea programada. Cada foco: id estable, centroide, n.º de reportes, categoría más frecuente (la confirmada por el ciudadano) y fecha del último reporte. En la app solo cambia el texto del mapa: «focos **detectados**» en vez de «confirmados». Ojo: **cuenta reportes, no personas distintas**. Con puntos sintéticos se prueba la agrupación.
4. **No se hace ahora:** confirmación por observador, silueta y ruido (se calculan luego con un script sobre datos exportados), polígono oficial del distrito, **IA** (espera las pruebas de campo) y protección contra abuso del servidor.

## Pasos del chat nuevo (uno por etapa; commit al terminar cada una)
1. **Precisión del GPS** (decisión 2) + pruebas + instalar en la tablet y comprobar que los reportes existentes siguen.
2. **Focos automáticos** (decisión 3) + pruebas del servidor + prueba visual del mapa con el servidor falso (modo `focos`).
3. **Los dos APK** (decisión 1), cuando el usuario ya tenga la llave: compilar, prueba de humo en la tablet, ruta y suma de control. Guía: `distribucion-apk.md`.
4. Documentación **breve** (un ADR corto y unas líneas en `progreso.md` y `avance.md`).

## Lo que depende del usuario
`git push` y verificar Render (`verificar_servicios.py --remoto URL --reporte-de-prueba`, compartir solo la salida) · crear la llave de firma y poner sus 4 líneas `RELEASE_*` en `local.properties` · añadir la SHA-1 de esa llave (y la del paquete simulado, si se acepta) a la clave de Maps · confirmar la decisión 1.

## Reglas de trabajo
- **Nunca** manejar credenciales: no leer `local.properties` ni `backend/.env` (solo contar líneas si hace falta); las contraseñas las escribe el usuario; la SHA-1 solo en el chat. No imprimir líneas de `logcat` con la clave de Maps.
- **Cuota del usuario muy justa:** respuestas cortas, una etapa a la vez, sin verificaciones de más.
- Commits en español (Conventional Commits) con `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`; el mensaje va a un archivo y se usa `git commit -F`.
- **PC lenta:** Gradle en segundo plano, `--no-problems-report`, una sola corrida por etapa, pedir que cierre Android Studio; si el demonio se cuelga más de 10 minutos, `./gradlew --stop`. Nombres de prueba sin `: . ; [ ] / < > \`; líneas de 120 caracteres como máximo; detekt marca 11 funciones por clase o archivo.
- **Otra sesión (25-B)** escribe en el mismo repositorio (`avance.md`, `progreso.md`): leer antes de editar y **no confirmar sus archivos** (`plan-confiabilidad-instrumentos.md`, `analisis/`, `docs/evidencia/*dimensionamiento*` y `*verificacion-tesis*`, y los cambios de `correcciones-documento.md`).
- **Tablet** (`adb` en `%LOCALAPPDATA%\Android\Sdk\platform-tools`): nunca desbloquearla ni cambiar sus ajustes; `connectedDebugAndroidTest` **desinstala la app**; no abrir el selector de «Compartir» (muestra sus contactos). El servidor falso (`herramientas/servidor_falso.py` + `adb reverse tcp:8000 tcp:8000`) tiene modos `ok`, `503`, `422`, `perdida`, `colgado`, `lento:N`, `focos`.

## Primer mensaje para pegar
> Continúa wanchac_app. Lee `docs/traspaso.md` y mi memoria. Empieza por el Paso 1 (precisión del GPS). Respuestas cortas y documentación breve: mi cuota semanal está casi agotada.
