# ADR-0009 — Los focos se detectan al pedir el mapa, con DBSCAN sencillo y sin confirmación

- **Estado**: aceptada
- **Fecha**: 2026-09-20
- **Contexto**: mapa público (Módulo 5), `GET /puntos-criticos`; ADR-0004 #3 (agrupamiento DBSCAN); `docs/traspaso.md`, decisión 3.

## Decisión

1. `GET /puntos-criticos` **calcula los focos al pedirse**, sobre los reportes guardados: no hay tabla `punto_critico` ni tarea programada. Con el volumen de la fase de campo (cientos o pocos miles de reportes) cuesta milisegundos, y la respuesta ya se guarda en caché 60 s.
2. **Algoritmo** (`backend/api/focos.py`): DBSCAN clásico, distancia de Haversine, **ε = 50 m, MinSamples = 4** (cuenta el propio punto), con una rejilla de ~65 m para no comparar todos contra todos. El ruido se descarta. Es determinista.
3. **Cada foco** devuelve: `id` estable (el menor id de reporte del grupo), centroide (promedio), `numReportes`, `categoriaPredominante` (la más frecuente entre las que **confirmó el ciudadano**; ante un empate, la primera de `CATEGORIAS`) y `fechaDeteccionEpochMillis` (el reporte más reciente del grupo). Es el mismo contrato de antes: la app no cambió, salvo el texto del mapa («focos **detectados**» en lugar de «confirmados»).
4. **Los reportes con `es_prueba` no cuentan** (ADR-0008).
5. Si la base falla, 503 sin caché (la app ya lo muestra como «problema del servidor»).

## Límites que hay que declarar

- **Cuenta reportes, no personas distintas**: una sola persona que reporte cuatro veces el mismo sitio crea un foco. La confirmación por observador queda para después.
- El `id` puede cambiar si se da de baja el reporte más antiguo del foco (o si el foco se une a otro).
- No hay ventana de tiempo: un reporte viejo cuenta igual que uno reciente. Si el estudio la necesita, se añade el filtro por `fecha_hora` en `puntos_criticos.py`.
- ε y MinSamples son propuestas iniciales (constantes `RADIO_METROS` y `MIN_REPORTES`); la silueta y el ruido se calcularán luego con un script sobre datos exportados.
