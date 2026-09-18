# ADR-0003 — LiteRT (API `Interpreter`, 1.4.2) como runtime del clasificador de IA

- **Estado**: aceptada
- **Fecha**: 2026-09-18
- **Contexto**: Módulo 3b (integración del modelo en la app)

## Contexto

El documento de investigación habla de "TensorFlow Lite". Ese runtime pasó a llamarse **LiteRT**: TensorFlow ya avisa que `tf.lite.Interpreter` está en desuso a favor de LiteRT (lo vimos en las pruebas del cuaderno), y el artefacto clásico de Maven Central `org.tensorflow:tensorflow-lite` no se actualiza desde enero de 2025 (última versión 2.17.0). Había que elegir qué dependencia usar, y no se eligió de memoria: se verificó en las fuentes oficiales el 2026-09-18.

## Qué se verificó

| Hecho | Fuente |
|---|---|
| `com.google.ai.edge.litert:litert` tiene versiones hasta **2.2.0**; `litert-support`, `litert-metadata` y `litert-gpu` llegan a 1.4.2 | `maven-metadata.xml` en Google Maven |
| La documentación oficial presenta dos APIs: **`CompiledModel`** (2.x, "el estándar moderno", minSdk 23) e **`Interpreter`** ("se mantiene por compatibilidad", 1.4.2, minSdk 21, solo CPU) | developers.google.com/edge/litert/android |
| `litert:1.4.2` (AAR de 8,3 MB) trae `org.tensorflow.lite.Interpreter` y las librerías nativas de `arm64-v8a`, `armeabi-v7a`, `x86` y `x86_64`; `litert:2.2.0` (11,8 MB) también conserva `Interpreter` y trae `arm64-v8a`, `armeabi-v7a` y `x86_64` | Se descargaron los AAR de Google Maven y se leyeron |
| **Todas** esas librerías nativas están alineadas a **16 KB** (`p_align = 0x4000` en los segmentos `PT_LOAD` del ELF), requisito de Google Play para apps que apuntan a Android 15+ | Lectura de las cabeceras ELF de cada `.so` |

(La documentación muestra `2.1.0` en su ejemplo de Gradle aunque la última es `2.2.0`: la página va un poco por detrás del repositorio.)

## Decisión

Usar **`com.google.ai.edge.litert:litert:1.4.2`** con la API `Interpreter`, detrás de la interfaz `MotorInferencia` (`ml/clasificador/`).

## Por qué esta y no `CompiledModel` 2.2.0

- No se puede ejecutar inferencia en este entorno (LiteRT solo corre en Android y no hay emulador ni dispositivo), así que **no hay forma de probar un cambio de API aquí**. La API `Interpreter` es la que ya se conoce bien y equivale a la que se usó para verificar el `.tflite` en el cuaderno.
- Para este caso (un modelo de imagen pequeño, en CPU) `CompiledModel` no aporta nada que se necesite hoy: su ventaja es la aceleración por GPU/NPU.
- El riesgo de elegir la API "de compatibilidad" queda **acotado**: si Google deja de mantenerla, migrar a `CompiledModel` es reemplazar UNA clase (`MotorLiteRt`) sin tocar el resto, porque la lógica del clasificador solo depende de `MotorInferencia`.

## Consecuencias

- **Tamaño**: la librería nativa pesa ≈ 4,5 MB por ABI (sin comprimir, `arm64-v8a`), más el modelo de 4,5 MB. Con Android App Bundle cada teléfono descarga solo su ABI.
- **`noCompress += "tflite"`** en `build.gradle.kts`: el modelo se lee con memoria mapeada y no puede ir comprimido dentro del APK.
- **Pruebas**: la lógica del clasificador se prueba en la JVM con un motor falso; la inferencia real solo puede probarse en un dispositivo (`ClasificadorResiduoReferenciaTest`, **escrita y compilada, no ejecutada en este entorno**).
- **Para el documento de tesis** (ver `correcciones-documento.md`): mencionar el runtime como LiteRT (antes TensorFlow Lite).
