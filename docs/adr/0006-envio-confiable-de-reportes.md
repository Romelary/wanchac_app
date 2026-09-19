# ADR-0006 — Envío confiable de reportes: clasificar cada respuesta, un estado local `RECHAZADO` y seguimiento por reporte

- **Estado**: aceptada
- **Fecha**: 2026-09-19
- **Contexto**: Módulo 4 (sincronización) y las pruebas de confiabilidad del indicador *«Reportes guardados y sincronizados según condición de red, ≥ 95 % en cada condición»* (Anexo 2, `diseno.md` §1)

## Contexto

Antes de usar la app en campo se revisó el envío (`SyncWorker`) contra lo que exige ese indicador. Se encontraron fallos **reales** (por lectura del código y confirmados con pruebas que fallaban antes del cambio), no hipótesis:

1. **Los errores permanentes se reintentaban para siempre.** Cualquier respuesta no 2xx (incluidos 400, 413, 415 y 422) y una foto ausente devolvían «reintentar»: el reporte se reenviaba sin fin (con la foto cada vez) y contaba como «pendiente» en cualquier medición.
2. **Un reporte guardado mientras se enviaba otro podía quedar sin enviar.** `SyncWorker` lee los pendientes al empezar y el envío se encolaba con `KEEP`; si ya había uno corriendo, no se encolaba nada, y el reporte nuevo esperaba hasta el siguiente reporte o reinicio de la app.
3. **Un fallo con un reporte tumbaba el resto** (solo se capturaba `IOException`) y no se anotaba nada de cada intento: sin intentos, sin momento de envío, sin motivo, sin conexión. Con eso **no se puede calcular el indicador** ni saber por qué falló un envío.
4. **Sin reintento al abrir la app** ni botón para probar de nuevo: tras cerrar la app a la fuerza o reiniciar el teléfono, los pendientes esperaban al siguiente reporte.
5. La espera entre reintentos era **exponencial** (10 s, 20 s, 40 s…): tras una racha de fallos llega a horas, justo cuando la conexión vuelve.

## Decisiones

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 1 | **Clasificar cada intento** en *éxito / reintentar / definitivo* según el código o la excepción (tabla en `diseno.md` §4.2). Definitivos: 400, 410, 413, 415, 422 y foto ausente. Se reintenta: 408, 429, 5xx, sin conexión, tiempo agotado. | Seguir reintentando todo; o marcar también 404/401/403 como definitivos | Reintentar todo gasta batería y datos y contamina la medición. Un 404, 401 o 403 **no dice nada del reporte** (dirección equivocada, servicio recién desplegado, proxy): marcarlo rechazado perdería un reporte válido por un problema de configuración. |
| 2 | **Nuevo estado local `RECHAZADO`**, distinto de `DESCARTADO` | Reutilizar `DESCARTADO`; borrar el reporte; dejarlo `PENDIENTE` con una marca | `DESCARTADO` tiene un significado del estudio (ruido espacial o el observador no halló el foco). Borrar pierde el dato y lo hace inmedible; dejarlo `PENDIENTE` lo cuenta como pendiente. El ciudadano lo ve como «No se pudo enviar» y puede eliminarlo. |
| 3 | **Seguimiento del envío en columnas de Room** (intentos, último intento, momento de envío, último fallo y código HTTP), **solo en el teléfono** (migración 2→3 sin pérdida) | Una tabla de eventos por intento; un archivo de registro; mandarlo al servidor | Bastan los últimos valores para explicar el estado y calcular el indicador; una tabla de eventos añade código y espacio sin una pregunta que la necesite. Mandarlo al servidor cambiaría el contrato y el alcance del consentimiento (Anexo 9). |
| 4 | **La conexión como dato técnico** (`SIN_RED`, `WIFI`, `DATOS_MOVILES`, `SIN_INTERNET`, `OTRA`), al guardar y en cada intento; **nunca** el nombre ni la dirección de la red | No anotar la conexión (que la anote quien prueba) | El indicador es «según condición de red»: anotarla automáticamente evita errores de transcripción. `SIN_INTERNET` (una red que Android no valida) es justo el caso que más engaña en campo: el teléfono cree tener conexión y el envío falla. |
| 5 | **Encolado**: `APPEND_OR_REPLACE` tras guardar (encadena detrás del envío en curso), `KEEP` al abrir la app, `REPLACE` en «Enviar ahora»; espera **lineal** de 30 s | `KEEP` siempre (el hueco del punto 2); `REPLACE` tras guardar; espera exponencial | `REPLACE` tras guardar interrumpiría la subida en curso y en redes lentas podría no terminar nunca. La espera lineal tras 20 fallos es de 10 min, no de horas. |
| 6 | **El trabajo procesa cada reporte por separado**, en orden de captura, **relee la lista tras cada vuelta** y tras 5 fallos por una excepción imprevista da el reporte por definitivo | Procesar la lista leída al empezar; reintentar sin límite los errores internos | Cierra el hueco del punto 2 también dentro de la corrida y aísla los fallos. El límite evita que un error nuestro consuma batería para siempre. |
| 7 | **Registro HTTP `BASIC` en depuración** (antes `BODY`) | Dejar `BODY` | `BODY` copia enteras las peticiones (foto y coordenadas en Logcat) y **falsea los tiempos** de las pruebas de campo; no hace falta para el desarrollo. |
| 8 | El ciudadano puede **eliminar** un reporte rechazado (botón existente) y ver por qué | Ocultarlo | Es su dato (RNF-05) y necesita saber que no llegó. |

## Consecuencias y riesgos

- **`RECHAZADO` es un resultado distinto de «pendiente»** en las pruebas de confiabilidad: hay que informarlo aparte y estudiar su causa. Un teléfono con la **fecha mal puesta** produce un 422 permanente (el servidor rechaza fechas anteriores a 2024 o más de un día en el futuro): conviene revisar la fecha de los teléfonos de prueba.
- **Un rechazo por un error del servidor sería permanente** en el teléfono (no se reintenta solo). Se mitiga permitiendo reintentarlo a mano desde «Mis reportes» (ver `pruebas-de-confiabilidad.md`).
- **La red anotada es la que había al empezar el intento**; una red que cambia a mitad de la subida queda anotada como al principio.
- **Es un cambio del modelo de la tesis** (ampliación local de los estados del Cap. IV): hay que reflejarlo en el documento.

## Lo que NO está verificado

Todo se probó con Room en memoria y un servidor de mentira (`SyncWorkerTest`, `ProgramadorSincronizacionWorkManagerTest`). **No se ha visto en un dispositivo real**: que WorkManager reintente al volver la red, que la espera lineal se cumpla en el planificador de Android, ni cómo se comporta con una red realmente lenta o intermitente. Los escenarios H de [`prueba-en-dispositivo.md`](../prueba-en-dispositivo.md) lo cubren.
