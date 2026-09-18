# `ml/` — Clasificador de residuos (Módulo 3)

Esta carpeta contiene todo lo necesario para **entrenar** el modelo que la app usa para *sugerir* la categoría
del residuo fotografiado (RF-04). El entrenamiento se hace **una vez**, en Google Colab; el resultado es un
archivo `.tflite` que se copia a la app y se ejecuta **en el teléfono, sin internet** (offline-first,
[ADR-0001](../docs/adr/0001-sin-login-id-anonimo.md): la foto no sale del dispositivo para clasificarse).

> **Léelo antes de citar cifras:** la exactitud que imprime el cuaderno se mide con fotos del mismo dataset
> con el que se entrenó y **sobreestima** el desempeño real en la calle. El indicador de la tesis
> (Anexo 2: coincidencia IA–observador ≥ 70 %) se mide **en campo**, contra un observador humano. Ver
> [Limitaciones](#limitaciones-honestas).

## Contenido

| Archivo | Rol |
|---|---|
| `entrenamiento/clasificador_residuos.py` | **Fuente única** del cuaderno, en formato de celdas (`# %%`). Se revisa en git sin ruido de JSON. |
| `entrenamiento/clasificador_residuos.ipynb` | Cuaderno generado a partir del `.py`; es lo que se sube a Colab. |
| `entrenamiento/construir_notebook.py` | Convierte el `.py` en `.ipynb` (y lo valida con `nbformat`). |
| `entrenamiento/probar_notebook.py` | Prueba de humo: ejecuta el cuaderno sin GPU ni internet y verifica el contrato con la app. |

## Cómo entrenar (Colab)

1. Entra a <https://colab.research.google.com> → **Archivo → Subir notebook** → elige
   `ml/entrenamiento/clasificador_residuos.ipynb`.
2. **Entorno de ejecución → Cambiar tipo de entorno de ejecución → GPU (T4)**.
3. Sube tus fotos a Drive con esta estructura exacta (una carpeta por clase):

   ```
   Mi unidad/wanchaq_limpio_ml/dataset/
     plastico/   carton/   vidrio/   organico/   escombros/
   ```

   `plastico`, `carton` y `vidrio` se rellenan solas con TrashNet (celda 2a). **`organico` y `escombros`
   las tienes que aportar tú** (mínimo 50 fotos por clase; lo razonable son 200 o más). Si no las hay, el
   cuaderno se detiene con un mensaje claro en vez de entrenar un modelo engañoso. Formatos válidos:
   `.jpg`, `.jpeg`, `.png`, `.bmp` (las fotos `.heic`/`.webp` del celular conviértelas antes: el cuaderno
   te avisa por nombre de cualquier archivo que ignore o que esté corrupto).
4. **Entorno de ejecución → Ejecutar todo.** Acepta el permiso para montar Drive.
5. Al terminar se descargan `clasificador_residuos.tflite`, `labels.txt`, `metadata.json` y
   `metricas.json`; todo queda también en `Mi unidad/wanchaq_limpio_ml/salida/` junto con las gráficas.
6. Copia `clasificador_residuos.tflite`, `labels.txt` y `metadata.json` a `app/src/main/assets/` y avisa:
   el siguiente paso (integrar el modelo en la app) lo hace el código del Módulo 3.

### ¿Sirve Google One?

Solo en parte. El **almacenamiento** de Google One (Drive) no da más cómputo en Colab. Lo que sí desbloquea
Colab premium (unidades de cómputo, GPU más rápidas) son los planes **Google AI** de pago (según las
preguntas frecuentes oficiales de Colab, sin garantía para cualquier plan de solo almacenamiento). Para este
proyecto **no hace falta**: MobileNetV2 con un par de miles de fotos es un entrenamiento pequeño que el plan
gratuito suele resolver con una GPU T4 (no está garantizada, pero incluso en CPU funciona, solo que más
lento). Si al abrir Colab ves un saldo de "unidades de cómputo", tu plan las incluye; si no, igual sirve.
La única ventaja segura de Google One aquí es el espacio en Drive para guardar las fotos y los resultados.

## Contrato con la app

El modelo y la app se comunican solo por estos archivos; cualquier cambio en uno exige cambiar el otro.

| Aspecto | Valor | Dónde se verifica |
|---|---|---|
| Entrada | `float32 [1, 224, 224, 3]`, RGB, **píxeles 0–255 sin normalizar** (la normalización va *dentro* del modelo) | `probar_notebook.py` |
| Salida | `float32 [1, 5]`, probabilidades softmax | `probar_notebook.py` |
| Orden de clases | `plastico, carton, vidrio, organico, escombros` = `labels.txt` = los 5 primeros valores de `CategoriaResiduo` en Kotlin | `probar_notebook.py` (lee el `.kt`) |
| `MIXTO` | **No** es clase del modelo: es lo que la app propone cuando la confianza queda por debajo del umbral | Módulo 3 (Android) |
| Umbral de confianza | `umbral_confianza_sugerido` de `metadata.json`: el menor umbral **≥ 0,50** con ≥ 85 % de precisión en *validación*. Si ninguno llega a 85 %, usa 0,70 y marca `umbral_alcanza_precision_objetivo: false` (señal de que el modelo aún no es confiable) | `probar_notebook.py` |
| Trazabilidad | `sha256_tflite`, fecha, versión de TensorFlow, conteo por clase y fuentes del dataset | `metadata.json` |

## Decisiones de diseño (y por qué)

| Decisión | Motivo |
|---|---|
| **MobileNetV2** con transfer learning | Es lo que plantea el marco metodológico y es liviano para móvil; con pocas fotos, reutilizar ImageNet rinde mucho más que entrenar desde cero. |
| Entrenamiento en **2 fases** (cabeza; luego afinar las últimas 30 capas con tasa 100× menor) | Práctica estándar de Keras: primero se adapta la capa nueva y solo después se toca la base preentrenada, sin destruirla. |
| Las capas `BatchNormalization` se dejan congeladas al afinar | La guía de transfer learning de Keras advierte que actualizar sus estadísticas durante el ajuste puede deshacer lo que la base ya aprendió. |
| Normalización **dentro** del modelo | La app pasa los píxeles tal cual; no hay que replicar el preprocesado en Kotlin (fuente clásica de errores "entrené de una forma y predigo de otra"). |
| Augmentación **fuera** del modelo exportado | El `.tflite` queda limpio y solo contiene lo que se usa al predecir. |
| Pesos por clase | Compensa el desbalance entre clases. |
| Partición 70/15/15 estratificada con semilla fija | Reproducible; el umbral se elige con *validación*, no con *test*, para no sesgar la medición final. |
| Piso de 0,50 en el umbral de confianza | Con 5 clases, 0,30 apenas supera el azar (0,20). Un dataset fácil como TrashNet haría que la validación aceptara umbrales absurdamente bajos, y esa validación ya es optimista respecto de la calle. Se recalibrará con datos de campo. |
| Validar cada imagen **antes** de entrenar (en vez de `ignore_errors()`) | Un archivo corrupto se informa por nombre y queda en `metadata.json → archivos_descartados`; ignorarlo en silencio ocultaría un problema de datos y además deja el tamaño del dataset "desconocido" para Keras. |
| Cuantización **float16** | Reduce a la mitad el tamaño con una pérdida de precisión que normalmente es despreciable; **no se asume**: el cuaderno comprueba que `.tflite` y modelo original coinciden (≥ 97 %) y se detiene si no. |
| El cuaderno **se detiene** si una clase tiene < 50 fotos | Entrenar con 5 fotos y reportar "100 %" sería autoengaño. |
| `.py` como fuente y `.ipynb` generado | Revisión en git legible; y el **mismo** código que corre en Colab es el que prueba `probar_notebook.py`. |

## Datos: qué cubre y qué falta

| Clase | Fuente en el cuaderno | Pendiente |
|---|---|---|
| `plastico` | TrashNet · carpeta `plastic` (MIT) | Sumar fotos de calle |
| `carton` | TrashNet · `cardboard` + `paper` (MIT) — coincide con la ficha de campo *"Papel y cartón"* | Sumar fotos de calle |
| `vidrio` | TrashNet · `glass` (MIT) | Sumar fotos de calle |
| `organico` | **Ninguna** | Fotos propias del equipo |
| `escombros` | **Ninguna** | Fotos propias del equipo |
| `mixto` | No es clase (ver contrato) | — |

Reglas para las fotos propias (las mismas de la app): **sin rostros, sin placas de vehículos, sin interiores
de viviendas**; variar luz, ángulo y distancia; el residuo bien visible. Las fotos **no se suben a git**
(`.gitignore` excluye `ml/**/dataset/`): viven solo en el Drive del equipo.

Licencia y cita: TrashNet es MIT y su autor pide citar el repositorio
(*Thung, G. y Yang, M. (2016), "Classification of Trash for Recyclability Status", Stanford CS229 —
github.com/garythung/trashnet*). Queda registrado en `metadata.json` → `dataset.fuentes`.

## Limitaciones honestas

1. **Sesgo del dataset:** TrashNet fotografía *un objeto solo sobre una cartulina blanca* (según su propio
   README). La basura real en Wanchaq aparece mezclada, sucia, parcial y con fondos complejos. Las métricas
   del cuaderno serán optimistas; la referencia publicada por sus autores para su propia red fue ≈ 75 % en
   *su* test, es decir, ya en condiciones favorables.
2. **Fuga de datos:** TrashNet fotografía cada objeto varias veces; dos fotos casi idénticas pueden caer una
   en *train* y otra en *test* e inflar la exactitud. Lo mismo pasa con ráfagas de fotos propias.
3. **`metal` no es categoría** de la app (Marco Conceptual, Cap. II), así que una lata se clasificará como la
   clase más parecida (probablemente `plastico`). Se documenta como limitación, no se oculta.
4. **El indicador real (Anexo 2) se mide en campo.** Esto es una comprobación de desarrollo.
5. **Reentrenar con fotos de ciudadanos** (mejora futura tentadora) queda **fuera** del propósito que declara
   el consentimiento (Anexo 9). Requiere consultarlo con el asesor antes de siquiera diseñarlo.

## Cómo probar y regenerar (desarrollo local)

Requiere Python 3.10+ con `tensorflow`, `scikit-learn`, `matplotlib`, `pillow` y `nbformat`.

```bash
cd ml/entrenamiento
python probar_notebook.py        # prueba de humo (sin GPU ni internet; unos minutos en CPU)
python construir_notebook.py     # regenera el .ipynb tras editar el .py
```

Tras editar `clasificador_residuos.py`: correr la prueba de humo, regenerar el `.ipynb` y versionar ambos
(la prueba falla si el `.ipynb` versionado quedó desactualizado). Detalle de qué cubre y qué no cada prueba:
[`docs/pruebas.md`](../docs/pruebas.md).
