# Resultados de la corrida v1 (Colab, 2026-09-18)

Archivos **tal como los generó el cuaderno**. Son evidencia: no se editan. Si algo hay que corregir, se anota
en `docs/progreso.md` y se corrige la copia que usa la app, no estos originales.

| Archivo | Contenido |
|---|---|
| `metadata.json` | Contrato con la app, umbral, conteo por clase y **fuentes declaradas** del dataset |
| `metricas.json` | Exactitud en prueba, métricas por clase, concordancia `.tflite` vs Keras, tabla de umbrales |

El modelo en sí (`clasificador_residuos.tflite`, sha256 `201484af0bfd699ce4effe8432da034bb69bda41a9a1bd95165ed32942c7a9d1`) vive en `app/src/main/assets/`
cuando se integra (Módulo 3b); el `sha256_tflite` de `metadata.json` permite comprobar que es el mismo.

**Léelo con estas reservas** (detalle en `docs/progreso.md`, Sesión 17): `fuera_de_clase` está en `null` (el riesgo
con escombros/muros no se midió); las clases de TrashNet y `organico` provienen de fuentes distintas, así que el
modelo podría estar reconociendo la procedencia de la foto; y `organico` tiene solo 16 fotos de prueba.
