# ADR-0008 — Se guarda la precisión del GPS de cada reporte y el servidor acepta reportes marcados «de prueba»

- **Estado**: aceptada (servidor y app implementados; la bandera `esPrueba` la enviará el APK de prueba, Paso 3)
- **Fecha**: 2026-09-19
- **Contexto**: confiabilidad del GPS (Anexo 2) y los dos APK de la fase de campo (`docs/traspaso.md`, decisión 1 y 2).

## Decisiones

1. **`precisionMetros` (opcional) en el `geojson`.** Es lo que declara el teléfono al aceptar la lectura (`Location.getAccuracy()`: radio de confianza del 68 %). **No es el error real**: solo permite ver cuánto se confiaba en cada punto y estudiar la relación con los focos. Sin ese dato, un punto a 3 m y uno a 15 m pesan igual.
   - App: Room 3→4 (`MIGRACION_3_4`, columna `precisionMetros REAL` sin valor por defecto: los reportes anteriores quedan en `NULL`, no se inventa), `Reporte`, mapper, DTO y columna `precision_gps_m` del registro CSV.
   - Servidor: columna `error_posicion_m` (nula), válida de 0 a **5000 m** (`ERROR_POSICION_MAXIMO_M`), CHECK en la base y error 400 `precision_invalida` fuera de rango o si no es número. El tope es amplio a propósito: el APK de prueba acepta la primera lectura, que puede ser una ubicación gruesa por antenas; un rechazo 400 dejaría el reporte reintentando en vano.
2. **`esPrueba` (opcional, booleano) en el `geojson`.** Si es `true`, el servidor **acepta el reporte aunque esté fuera de Wanchaq** (sin él sigue el 422 `fuera_de_wanchaq`) y lo guarda con `es_prueba = true` (por defecto `false`). Los reportes de prueba **no deben entrar** en focos ni estadísticas: el Paso 2 los filtra. Un valor que no sea booleano es 400 `es_prueba_invalido`.
3. Ambas columnas van en **una misma migración** del servidor (`0002_precision_y_es_prueba`), que solo añade columnas nulas/por defecto: no toca los datos existentes.

## Consecuencias

- **Orden de despliegue: primero el servidor, luego el APK.** Un servidor antiguo ignora las propiedades desconocidas, así que un APK nuevo no se rompe, pero perdería el dato.
- Es **un dato más que se registra** por reporte: avisar al asesor y reflejarlo en el consentimiento informado.
- Riesgo asumido: `esPrueba` lo declara el cliente, así que cualquiera que conozca la API podría marcar un reporte como de prueba (o ubicarlo fuera del distrito). No hay protección contra abuso todavía (decidido no hacerla ahora); los datos de prueba se pueden purgar.
