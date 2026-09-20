# Traspaso para el chat nuevo (2026-09-19)

> Para retomar sin releer la conversación anterior. Lee esto, la memoria del proyecto y solo lo que cada paso indique. El detalle vive en [`progreso.md`](./progreso.md) (Sesiones 24 a 27), [`avance.md`](./avance.md), [`distribucion-apk.md`](./distribucion-apk.md) y los ADR 0006 y 0007.

## Meta
La app debe funcionar **como si fuera el producto final, para pruebas**: el equipo la usará para recoger datos y medir la confiabilidad. Los datos que queden en el servidor real **se purgan si hace falta** (dicho por el usuario). Play Store va después, con la app terminada.

## Estado (todo confirmado en git; último commit de esta línea de trabajo: `366f724`, más el de este traspaso)
- **Hecho y verificado en la Galaxy Tab S9+:** captura con GPS en caliente y foto reducida, envío confiable (ADR-0006), migración de la base, mapa con la clave real de Google Maps (G1, G2, G7), registro de envíos (CSV), GPS real (rechaza «fuera del distrito»), 26 pruebas de UI. **288 pruebas unitarias** en verde, ktlint y detekt limpios.
- **Firma del APK de publicación** lista en `app/build.gradle.kts` (llave y contraseñas en `local.properties`, que escribe el usuario); verificada solo con una llave de mentira.
- **Correo de contacto** ya definido en `gradle.properties`. `baseUrl` ya está en `local.properties` (apunta a Render): **una compilación sin `-PbaseUrl=http://localhost:8000` manda los reportes al servidor real**.
- **Render ya tiene la Fase 1 y está verificado** (2026-09-19, `verificar_servicios.py --remoto --reporte-de-prueba`: TODO OK, con POST, reenvío, baja, lápida y rechazo fuera de Wanchaq; evidencia en `docs/evidencia/2026-09-19_verificacion-render.txt`). Ese script **no prueba** `GET /puntos-criticos`: abrirlo en el navegador.

## Decisiones ya tomadas (no reabrir)
1. **Dos APK al final, ambos de publicación, firmados con la misma llave y con el mismo paquete** (no conviven: para cambiar de uno a otro se instala el otro encima con un `versionCode` mayor; no hace falta otra entrada en la clave de Maps). (a) **Producción**: `./gradlew assembleRelease -PversionCode=N`, GPS real **con** las restricciones (≤ 15 m y dentro de Wanchaq): da los datos válidos. (b) **Prueba**: `./gradlew assembleRelease -PmodoPrueba=true -PversionCode=N`, GPS **real, donde esté la persona**, **sin** la restricción de distrito (**decidido: tampoco exige los 15 m**; acepta la primera lectura disponible, sin esperar, y guarda su precisión; el filtro de 15 m se prueba con el APK de producción en exteriores de Wanchaq), Ajustes dice «prueba» y el nombre es «Wanchaq Limpio (prueba)». **El servidor también rechaza fuera de Wanchaq (422)**, así que (b) marca cada reporte como `esPrueba` en el `geojson`; el servidor (columna `es_prueba`, por defecto falso, en la **misma migración** que la precisión) lo acepta aunque esté fuera del distrito y **lo excluye** de los focos y de cualquier estadística. Implementar `modoPrueba` como propiedad de Gradle (como `ubicacionSimulada`), sin *flavors*, para no romper las tareas de prueba. `-PubicacionSimulada=true` (depuración, coordenadas falsas dentro de Wanchaq) queda solo para desarrollo.
2. **Precisión del GPS (Paso 1):** guardar `precisionMetros` de la lectura aceptada. App: Room 3→4 con migración, `Reporte`, mapper, dato **opcional** en el `geojson`, columna en el registro CSV. Servidor: columna opcional `error_posicion_m` con validación (0 a unos cientos de metros), migración y pruebas. ADR corto. Es la precisión **declarada por el teléfono** (radio del 68 % de `Location.getAccuracy()`, de memoria), no el error real. Conviene avisar al asesor porque es un dato más que se registra (consentimiento). **Orden de despliegue: primero el servidor, luego el APK** (si no, el servidor podría rechazar el dato nuevo).
3. **Focos automáticos, sin confirmación (Paso 2):** el servidor calcula los focos **al pedir** `GET /puntos-criticos`: DBSCAN sencillo en Python (distancia de Haversine, ε = 50 m, MinSamples = 4), sin tabla ni tarea programada. Cada foco: id estable, centroide, n.º de reportes, categoría más frecuente (la confirmada por el ciudadano) y fecha del último reporte. En la app solo cambia el texto del mapa: «focos **detectados**» en vez de «confirmados». Ojo: **cuenta reportes, no personas distintas**. Con puntos sintéticos se prueba la agrupación.
4. **No se hace ahora:** confirmación por observador, silueta y ruido (se calculan luego con un script sobre datos exportados), polígono oficial del distrito, **IA** (espera las pruebas de campo) y protección contra abuso del servidor.

## Pasos del chat nuevo (uno por etapa; commit al terminar cada una)
1. **Precisión del GPS** (decisión 2) + pruebas + instalar en la tablet y comprobar que los reportes existentes siguen. Incluye la bandera `esPrueba` y `es_prueba` (decisión 1) en la misma migración y el mismo despliegue del servidor.
2. **Focos automáticos** (decisión 3) + pruebas del servidor + prueba visual del mapa con el servidor falso (modo `focos`).
3. **Los dos APK** (decisión 1), cuando el usuario ya tenga la llave: compilar, prueba de humo en la tablet, ruta y suma de control. Guía: `distribucion-apk.md`.
4. Documentación **breve** (un ADR corto y unas líneas en `progreso.md` y `avance.md`).

## Lo que depende del usuario
crear la llave de firma y poner sus 4 líneas `RELEASE_*` en `local.properties` · añadir la SHA-1 de esa llave a la clave de Maps (una entrada más, mismo paquete).

## Reglas de trabajo
- **Nunca** manejar credenciales: no leer `local.properties` ni `backend/.env` (solo contar líneas si hace falta); las contraseñas las escribe el usuario; la SHA-1 solo en el chat. No imprimir líneas de `logcat` con la clave de Maps.
- **Cuota del usuario muy justa:** respuestas cortas, una etapa a la vez, sin verificaciones de más.
- Commits en español (Conventional Commits) con `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`; el mensaje va a un archivo y se usa `git commit -F`.
- **PC lenta:** Gradle en segundo plano, `--no-problems-report`, una sola corrida por etapa, pedir que cierre Android Studio; si el demonio se cuelga más de 10 minutos, `./gradlew --stop`. Nombres de prueba sin `: . ; [ ] / < > \`; líneas de 120 caracteres como máximo; detekt marca 11 funciones por clase o archivo.
- **Otra sesión (25-B)** escribe en el mismo repositorio (`avance.md`, `progreso.md`): leer antes de editar y **no confirmar sus archivos** (`plan-confiabilidad-instrumentos.md`, `analisis/`, `docs/evidencia/*dimensionamiento*` y `*verificacion-tesis*`, y los cambios de `correcciones-documento.md`).
- **Tablet** (`adb` en `%LOCALAPPDATA%\Android\Sdk\platform-tools`): nunca desbloquearla ni cambiar sus ajustes; `connectedDebugAndroidTest` **desinstala la app**; no abrir el selector de «Compartir» (muestra sus contactos). El servidor falso (`herramientas/servidor_falso.py` + `adb reverse tcp:8000 tcp:8000`) tiene modos `ok`, `503`, `422`, `perdida`, `colgado`, `lento:N`, `focos`.

## Primer mensaje para pegar
> Continúa wanchac_app. Lee `docs/traspaso.md` y mi memoria. Empieza por el Paso 1 (precisión del GPS). Respuestas cortas y documentación breve: mi cuota semanal está casi agotada.
