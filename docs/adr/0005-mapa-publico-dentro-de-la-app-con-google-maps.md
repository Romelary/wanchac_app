# ADR-0005 — El mapa público vive dentro de la app, con Google Maps SDK (Maps Compose)

- **Estado**: aceptada
- **Fecha**: 2026-09-19
- **Contexto**: Módulo 5 (mapa público de focos confirmados)

## Contexto

`plan.md` (Módulo 5) pone el mapa **dentro de la app** ("Google Maps SDK o Mapbox"), y el caso de uso UC3 del diseño es de la app móvil. La tesis, en cambio, menciona un **panel web con React/Leaflet**. Al detectarlo se registró la ambigüedad como tarea F12 y se dieron al usuario tres opciones con datos verificados (ver `plan.md`, Módulo 5). La recomendación del asistente fue una **página web con Leaflet + teselas de OpenStreetMap** (costo cero, sin claves ni tarjeta, coherente con la tesis).

**El usuario decidió el 2026-09-19: el mapa se muestra en la propia app, con Google Maps, porque ya tiene cuenta de Google Cloud.** No es un error respecto a la recomendación: era una recomendación por costo y por coherencia con la tesis, y la decisión de producto es suya.

## Decisión

Un destino **"Mapa"** en la barra de navegación de la app, con **Google Maps SDK para Android** a través de **Maps Compose `7.0.0`**. Solo lectura; solo **focos confirmados en campo** (nunca reportes individuales, CORR-003); encuadrado en Wanchaq y con el desplazamiento limitado al distrito más un margen de ≈ 1 km.

## Qué se verificó (2026-09-19) y qué no

| Hecho | Fuente | Estado |
|---|---|---|
| Google exige un proyecto con **cuenta de facturación**, la SDK "Maps SDK for Android" habilitada y una clave que se restringe por **nombre de paquete + huella SHA-1** y por API | [documentación de Google](https://developers.google.com/maps/documentation/android-sdk/get-api-key) | ✅ verificado |
| El costo a esta escala | [tabla de precios](https://developers.google.com/maps/billing-and-pricing/pricing): la lectura es **ambigua** (aparece con cuota gratuita, pero no se pudo confirmar) | ⚠️ **no verificado**: por eso se recomienda un presupuesto con alertas |
| `maps-compose` **7.0.0** se compiló contra el **mismo BOM de Compose que usa el proyecto** (2025.12.00) | POM en Maven Central | ✅ y **la compilación real lo confirmó** (`compileDebugKotlin`: BUILD SUCCESSFUL) |
| `maps-compose` 8.4 a 8.6 declaran `kotlin-stdlib 2.4.10` (se compilaron con Kotlin 2.4); el proyecto usa **Kotlin 2.2.10** | POM en Maven Central | ⚠️ **no se probó compilarlas**: se evitaron por precaución. 9.0.0 sigue en candidato a release |

## Diseño

- **Capa fina sobre Google**: `MapaGoogle` solo dibuja. La lógica (estados cargando / sin focos / con focos / error, textos, reintentos) vive en `MapaContent` (sin estado, probable con Compose en la JVM), `MapaViewModel` y el repositorio, que sí tienen pruebas. El dibujo real de Google Maps **no se puede probar en la JVM** (necesita clave y Google Play services): se comprueba en un dispositivo (escenario G de `prueba-en-dispositivo.md`).
- **La clave de Maps nunca se escribe en el código ni va a git**: se lee de `local.properties` (ignorado por git) y Gradle la inyecta en el manifiesto (`manifestPlaceholders`). No se usó el *Secrets Gradle Plugin* de Google porque no se verificó su compatibilidad con AGP 9.2.1; leer `local.properties` en Gradle es más simple y no añade una dependencia sin comprobar.
- **Seguros de publicación** (mismo estilo que `contactEmail`): `assembleRelease`/`bundleRelease` **fallan** si falta `MAPS_API_KEY` o si `baseUrl` no es `https`. En depuración, si falta la clave, la pantalla del mapa muestra un **aviso rojo** en vez de un mapa gris inexplicable.
- **Contrato del servidor**: `GET /puntos-criticos` (`diseno.md` §4.2): GeoJSON `FeatureCollection`, solo confirmados, **sin ningún identificador de dispositivo**.
- **Tolerancia**: un foco mal formado se descarta sin tumbar la lista, y una categoría desconocida deja el foco sin categoría.
- **Red** (tarea B11): tiempos de espera de OkHttp de 30/90/60/150 s (conexión/lectura/escritura/llamada), porque el arranque en frío de Render tarda ≈ 1 min y los 10 s por defecto harían fallar siempre la primera petición.

## Consecuencias y riesgos

- **La clave viaja dentro del APK.** Lo que la protege es la **restricción** por paquete y SHA-1, no el secreto. Hay que añadir la huella de la **firma de publicación** (o la de Play App Signing) a la restricción antes de publicar (tarea E3), y una clave sin restringir es un riesgo de **cargos a tu cuenta**.
- **Requiere cuenta de facturación (tarjeta)** y el costo no está confirmado → presupuesto y alertas en Google Cloud.
- **Dispositivos sin Google Play services** (p. ej. algunos de Huawei) no mostrarán el mapa; no se verificó cómo se ve exactamente.
- **`maps-compose` queda fijado en 7.0.0**: subirlo a 8.4+ exige subir Kotlin (y probablemente el BOM de Compose y otras dependencias).
- **El mapa estará vacío durante semanas por diseño**: solo muestra focos confirmados en campo, y hoy nada los produce (faltan la Fase 2, la Fase 3 y **una forma de confirmar un foco**; tareas A14, A15 y A18 del tablero). La pantalla muestra un mensaje que lo explica. El servidor tiene un endpoint **provisional** (siempre vacío, [ADR-0004 #17](./0004-backend-django-sin-geodjango.md)) para que el mapa se dibuje y diga «Aún no hay focos confirmados» en vez de mostrar un error 404 sin mapa.
- **La tesis menciona un panel web con React/Leaflet** que sigue sin hacerse: hay que **avisar al asesor** de que el mapa público quedó en la app (tarea F13 del tablero).
- **Solo Android**: no hay forma de ver el mapa sin instalar la app (la opción web lo habría permitido).
