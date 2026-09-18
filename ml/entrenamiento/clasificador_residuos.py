# %% [markdown]
# # Clasificador de residuos para Wanchaq Limpio (Módulo 3)
#
# Este cuaderno entrena, con *transfer learning*, el modelo que la app ejecuta **en el propio
# teléfono** para sugerir la categoría del residuo fotografiado (RF-04). Genera tres archivos que
# luego se copian a `app/src/main/assets/`:
#
# | Archivo | Para qué sirve |
# |---|---|
# | `clasificador_residuos.tflite` | El modelo (MobileNetV2, cuantizado a float16) |
# | `labels.txt` | Las clases, **en el orden exacto** de la salida del modelo |
# | `metadata.json` | Contrato con la app (entrada, salida, umbral) y procedencia de los datos |
#
# ## Cómo usarlo en Colab
# 1. **Entorno de ejecución → Cambiar tipo de entorno → GPU (T4).** Sin GPU también funciona, pero es mucho más lento.
# 2. Prepara tus imágenes en Google Drive: `MyDrive/wanchaq_limpio_ml/dataset/<clase>/*.jpg`, con una
#    carpeta por clase (ver la sección 2). La celda 2a siembra por sí sola 3 de las 4 clases desde un
#    dataset público (TrashNet); se desactiva con `SEMBRAR_TRASHNET = False` en la sección 1.
# 3. Ejecuta las celdas en orden (**Entorno de ejecución → Ejecutar todo**). Te pedirá permiso para acceder a Drive.
# 4. Al final descarga los tres archivos (o búscalos en `MyDrive/wanchaq_limpio_ml/salida/`).
#
# ## Qué NO demuestra este cuaderno (léelo antes de citar cifras)
# * La exactitud que imprime se mide sobre fotos del **mismo dataset** con el que se entrenó. Las fotos de
#   calle reales (luz, ángulo, mezcla de residuos) son otra distribución: esa cifra **sobreestima** el
#   desempeño real.
# * El indicador de la tesis (Anexo 2: *coincidencia IA-observador ≥ 70 %*) se mide en campo contra un
#   observador, no aquí. Lo que sale de este cuaderno es una comprobación de desarrollo, no ese indicador.
# * **El modelo NO reconoce `escombros` ni `mixto`** (decisión del 2026-09-18): la app los ofrece como
#   categorías de elección manual. Por eso una foto de escombros, de un muro o de suelo desnudo *no tiene
#   clase propia* y el modelo la forzará hacia una de las 4; lo único que lo evita es que la confianza quede
#   bajo el umbral. La sección 7b **mide** ese riesgo con fotos "fuera de clase" (solo para probar).
# * Ningún dataset público cubre bien `organico` en la calle: exige fotos propias del equipo (sin rostros
#   ni placas).

# %%
# 1. CONFIGURACIÓN ------------------------------------------------------------------------------
import collections
import contextlib
import datetime
import hashlib
import io
import json
import os
import pathlib
import random
import shutil
import subprocess
import sys

import numpy as np

# Solo para las pruebas automáticas del autor (probar_notebook.py); en Colab NO se define.
SMOKE = os.environ.get("SMOKE_TEST") == "1"

# El ORDEN importa: es el orden de la salida del modelo, de labels.txt y del enum
# CategoriaResiduo de la app (que las usa en mayúsculas). No lo cambies sin cambiar ambos lados.
CLASES = ["plastico", "carton", "vidrio", "organico"]

# Categorías de la app que el modelo NO predice: las elige el ciudadano a mano (o `mixto` cuando el modelo
# duda). `escombros` salió del modelo el 2026-09-18: ningún dataset público lo cubre y, siendo material
# mineral gris, se confundiría con muros y veredas. Se guarda en metadata.json para que la app lo sepa.
CATEGORIAS_MANUALES = ["escombros", "mixto"]

IMG_SIZE = 224  # MobileNetV2 con pesos de ImageNet espera 224x224
SEMILLA = 42
BATCH = 32
EPOCHS_CABEZA = 8  # fase 1: solo se entrena la capa final
EPOCHS_AJUSTE = 10  # fase 2: se ajustan (fine-tuning) las últimas capas de MobileNetV2
CAPAS_A_DESCONGELAR = 30
MIN_IMAGENES_POR_CLASE = 50  # por debajo de esto se detiene: entrenar así sería engañarse
PRECISION_OBJETIVO_UMBRAL = 0.85  # precisión que se exige a las predicciones que la app acepte

# TrashNet (MIT) sirve para 3 de las 4 clases. Ponlo en False si prefieres usar solo tus propias fotos.
SEMBRAR_TRASHNET = True
MAX_TRASHNET_POR_CLASE = 400  # tope por clase: evita que TrashNet ahogue a las clases con fotos propias

# Procedencia de los datos (evidencia para la tesis). COMPLETA lo que aportes tú.
# TrashNet pide citar su repositorio (README.txt del dataset): github.com/garythung/trashnet
FUENTES_DATASET = [
    {
        "clases": ["plastico", "carton", "vidrio"],
        "fuente": "TrashNet, dataset-resized.zip (huggingface.co/datasets/garythung/trashnet; github.com/garythung/trashnet)",
        "licencia": "MIT",
    },
    {"clases": ["organico"], "fuente": "COMPLETAR (fotos propias del equipo, u otra fuente que declares aquí)", "licencia": "COMPLETAR"},
]

if SMOKE:
    BATCH, EPOCHS_CABEZA, EPOCHS_AJUSTE, MIN_IMAGENES_POR_CLASE = 8, 1, 1, 10
    MAX_TRASHNET_POR_CLASE = 15

# Formatos que TensorFlow sabe decodificar. Fotos de celular en .webp/.heic: conviértelas antes a .jpg.
EXTENSIONES = {".jpg", ".jpeg", ".png", ".bmp"}


def preparar_rutas():
    """Devuelve la carpeta raíz de trabajo (en Drive si estamos en Colab)."""
    if SMOKE:
        raiz = pathlib.Path(os.environ["SMOKE_DIR"])
    else:
        try:
            from google.colab import drive

            drive.mount("/content/drive")
            raiz = pathlib.Path("/content/drive/MyDrive/wanchaq_limpio_ml")
        except ImportError:
            raiz = pathlib.Path("wanchaq_limpio_ml")  # ejecución local, fuera de Colab
    (raiz / "dataset").mkdir(parents=True, exist_ok=True)
    (raiz / "salida").mkdir(parents=True, exist_ok=True)
    return raiz


RAIZ = preparar_rutas()
DATASET_DIR = RAIZ / "dataset"
SALIDA_DIR = RAIZ / "salida"
print("Carpeta de trabajo:", RAIZ)

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

random.seed(SEMILLA)
np.random.seed(SEMILLA)
tf.random.set_seed(SEMILLA)

GPUS = tf.config.list_physical_devices("GPU")
print("TensorFlow", tf.__version__, "| GPU:", GPUS if GPUS else "ninguna")
if not GPUS and not SMOKE:
    print("AVISO: sin GPU el entrenamiento será lento. Entorno de ejecución → Cambiar tipo de entorno → T4 GPU.")

# %% [markdown]
# ## 2. Datos
# Estructura esperada (una carpeta por clase, nombres exactos):
# ```
# wanchaq_limpio_ml/dataset/
#   plastico/  carton/  vidrio/  organico/
# ```
# La celda 2a descarga **TrashNet** (MIT, `dataset-resized.zip`, 43 MB) y toma hasta 400 fotos por clase para
# `plastico`, `carton` (cartón + papel, igual que la ficha de campo: *"Papel y cartón"*) y `vidrio`. Las
# carpetas `metal` y `trash` de TrashNet no se usan porque no son categorías de la app. **No** hay
# `organico` en TrashNet: esa carpeta la llenas tú.
#
# Si usas TrashNet en la tesis, cítalo (lo pide su autor): *Thung, G. y Yang, M. (2016). "Classification of
# Trash for Recyclability Status", Stanford CS229* — repositorio `github.com/garythung/trashnet`.
#
# Ojo: las fotos de TrashNet son de un objeto solo sobre una **cartulina blanca**. Sirven para arrancar, pero
# el modelo sacará peores resultados con basura real en la calle: por eso conviene sumar fotos propias en
# todas las clases, no solo en las dos que faltan.
#
# Recomendaciones para las fotos propias: variedad de luz, ángulo y distancia; el residuo bien visible;
# **sin rostros, placas ni interiores de viviendas** (misma regla ética que la app).

# %%
# 2a. Sembrar plastico/carton/vidrio desde TrashNet ---------------------------------------------
def instalar(paquete):
    subprocess.run([sys.executable, "-m", "pip", "install", "-q", paquete], check=True)


# Carpetas de TrashNet -> nuestras clases. "metal" y "trash" no se usan: no son categorías de la app.
MAPA_TRASHNET = {"plastic": "plastico", "cardboard": "carton", "paper": "carton", "glass": "vidrio"}


def sembrar_desde_trashnet(dataset_dir, max_por_clase=MAX_TRASHNET_POR_CLASE, ruta_zip=None):
    """Copia hasta `max_por_clase` fotos por clase desde dataset-resized.zip (43 MB, fotos de 512x384).

    Estructura verificada el 2026-09-18 leyendo el directorio central del zip en Hugging Face:
    `dataset-resized/<carpeta>/<carpeta><n>.jpg` (cardboard 403, glass 501, metal 410, paper 594,
    plastic 482, trash 137) más una carpeta `__MACOSX` de basura que se descarta. Se mapea por NOMBRE de
    carpeta, no por número de etiqueta (el orden numérico de TrashNet varía según la fuente).

    Es repetible: con la misma semilla elige y nombra siempre las mismas fotos (`trashnet_0000.jpg`...),
    así que volver a ejecutarla no duplica nada. `ruta_zip` solo lo usa la prueba automática.
    """
    import zipfile

    if ruta_zip is None:
        try:
            from huggingface_hub import hf_hub_download
        except ImportError:
            instalar("huggingface_hub")
            from huggingface_hub import hf_hub_download
        ruta_zip = hf_hub_download("garythung/trashnet", "dataset-resized.zip", repo_type="dataset")

    candidatas = {clase: [] for clase in sorted(set(MAPA_TRASHNET.values()))}
    with zipfile.ZipFile(ruta_zip) as z:
        for nombre in z.namelist():
            partes = nombre.split("/")
            if nombre.startswith("__MACOSX") or len(partes) != 3 or partes[2].startswith("._"):
                continue
            if partes[2].lower().endswith((".jpg", ".jpeg", ".png")) and partes[1] in MAPA_TRASHNET:
                candidatas[MAPA_TRASHNET[partes[1]]].append(nombre)

        azar = random.Random(SEMILLA)
        for clase, nombres in candidatas.items():
            if not nombres:
                raise RuntimeError(f"El zip de TrashNet no trae fotos para '{clase}': ¿cambió su estructura?")
            nombres.sort()
            azar.shuffle(nombres)  # mezcla cartón y papel, en vez de tomar solo uno de los dos
            carpeta = dataset_dir / clase
            carpeta.mkdir(parents=True, exist_ok=True)
            for n, nombre in enumerate(nombres[:max_por_clase]):
                (carpeta / f"trashnet_{n:04d}.jpg").write_bytes(z.read(nombre))
            print(f"TrashNet -> {clase}: {min(len(nombres), max_por_clase)} de {len(nombres)} disponibles")


def generar_dataset_sintetico(dataset_dir, clases, por_clase=15):
    """Solo para la prueba automática: imágenes de ruido con un color distinto por clase."""
    from PIL import Image

    rng = np.random.default_rng(SEMILLA)
    for i, clase in enumerate(clases):
        carpeta = dataset_dir / clase
        carpeta.mkdir(parents=True, exist_ok=True)
        color = np.array([(i * 50) % 256, (i * 90) % 256, (i * 130) % 256])
        for n in range(por_clase):
            pixeles = np.clip(rng.normal(color, 40, size=(64, 64, 3)), 0, 255).astype("uint8")
            Image.fromarray(pixeles).save(carpeta / f"sintetica_{n:03d}.jpg")


def generar_zip_trashnet_falso(ruta):
    """Solo para la prueba automática: un zip con la MISMA estructura que dataset-resized.zip (incluida
    la basura de __MACOSX), para ejercitar el código real de `sembrar_desde_trashnet` sin internet."""
    import io
    import zipfile

    from PIL import Image

    rng = np.random.default_rng(SEMILLA)
    cantidades = {"plastic": 12, "cardboard": 8, "paper": 8, "glass": 12, "metal": 5, "trash": 5}
    with zipfile.ZipFile(ruta, "w") as z:
        z.writestr("dataset-resized/", "")
        z.writestr("dataset-resized/.DS_Store", b"basura")
        for carpeta, cantidad in cantidades.items():
            for n in range(1, cantidad + 1):
                buffer = io.BytesIO()
                pixeles = rng.integers(0, 255, size=(64, 64, 3), dtype="uint8")
                Image.fromarray(pixeles).save(buffer, format="JPEG")
                z.writestr(f"dataset-resized/{carpeta}/{carpeta}{n}.jpg", buffer.getvalue())
                z.writestr(f"__MACOSX/dataset-resized/{carpeta}/._{carpeta}{n}.jpg", b"basura")


if SMOKE:
    # Prueba automática: TrashNet falso para 3 clases + ruido sintético para las otras 2.
    zip_falso = RAIZ / "trashnet_falso.zip"
    generar_zip_trashnet_falso(zip_falso)
    sembrar_desde_trashnet(DATASET_DIR, ruta_zip=zip_falso)
    generar_dataset_sintetico(DATASET_DIR, ["organico"])
    # Un archivo corrupto y otro de formato no soportado: el primero debe descartarse e informarse, el segundo ignorarse.
    (DATASET_DIR / "organico" / "corrupta.jpg").write_bytes(b"esto no es una imagen")
    (DATASET_DIR / "organico" / "foto.heic").write_bytes(b"formato no soportado")
elif SEMBRAR_TRASHNET:
    sembrar_desde_trashnet(DATASET_DIR)

# %%
# 2b. Inventario del dataset: se detiene si alguna clase tiene muy pocas imágenes -----------------
def es_decodificable(ruta):
    """Prueba la imagen con el MISMO decodificador que usará el entrenamiento, para que un archivo
    corrupto se descubra (y se informe) aquí y no rompa o distorsione el entrenamiento después."""
    try:
        tf.io.decode_image(tf.io.read_file(str(ruta)), channels=3, expand_animations=False)
        return True
    except tf.errors.InvalidArgumentError:
        return False


ARCHIVOS = {}  # clase -> rutas válidas, ordenadas
DESCARTADOS = []  # "clase/archivo" de lo que no se pudo leer (queda registrado en metadata.json)
for _clase in CLASES:
    _todos = [p for p in (DATASET_DIR / _clase).glob("*") if p.is_file()]
    _candidatas = sorted(p for p in _todos if p.suffix.lower() in EXTENSIONES)
    ARCHIVOS[_clase] = [p for p in _candidatas if es_decodificable(p)]
    _validas = set(ARCHIVOS[_clase])
    DESCARTADOS += [f"{_clase}/{p.name}" for p in _candidatas if p not in _validas]
    _sin_soporte = sorted(p.name for p in _todos if p.suffix.lower() not in EXTENSIONES)
    if _sin_soporte:  # no se ignora en silencio: quien subió 100 fotos .heic debe enterarse de por qué "faltan"
        print(f"AVISO: {_clase}: {len(_sin_soporte)} archivo(s) de formato no soportado, se ignoran "
              f"(conviértelos a .jpg): {_sin_soporte[:5]}")
if DESCARTADOS:
    print(f"AVISO: {len(DESCARTADOS)} archivo(s) ilegible(s) o corrupto(s), se omiten: {DESCARTADOS[:10]}")


def manifiesto_dataset():
    return {
        clase: {
            "total": len(ARCHIVOS[clase]),
            "de_trashnet": sum(p.name.startswith("trashnet_") for p in ARCHIVOS[clase]),
        }
        for clase in CLASES
    }


MANIFIESTO = manifiesto_dataset()
print(f"{'clase':<12}{'total':>8}{'de TrashNet':>14}")
for clase, datos in MANIFIESTO.items():
    print(f"{clase:<12}{datos['total']:>8}{datos['de_trashnet']:>14}")

insuficientes = [c for c, d in MANIFIESTO.items() if d["total"] < MIN_IMAGENES_POR_CLASE]
if insuficientes:
    raise SystemExit(
        f"Faltan imágenes (mínimo {MIN_IMAGENES_POR_CLASE} por clase) en: {insuficientes}. "
        f"Agrégalas a {DATASET_DIR}/<clase>/ y vuelve a ejecutar esta celda."
    )

# %%
# 2c. Partición train/val/test (70/15/15), estratificada y con semilla fija -----------------------
from sklearn.model_selection import train_test_split

rutas = np.array([str(p) for clase in CLASES for p in ARCHIVOS[clase]])
etiquetas = np.array([i for i, clase in enumerate(CLASES) for _ in ARCHIVOS[clase]])

r_tv, r_test, y_tv, y_test = train_test_split(rutas, etiquetas, test_size=0.15, stratify=etiquetas, random_state=SEMILLA)
r_train, r_val, y_train, y_val = train_test_split(r_tv, y_tv, test_size=0.15 / 0.85, stratify=y_tv, random_state=SEMILLA)
print(f"train={len(r_train)}  val={len(r_val)}  test={len(r_test)}")

# %% [markdown]
# **Ojo con la fuga de datos.** TrashNet fotografía cada objeto varias veces; si dos fotos casi idénticas
# caen una en *train* y otra en *test*, la exactitud sale inflada. Con tus fotos propias, si tomas
# ráfagas del mismo residuo, es mejor separarlas por "sesión" antes de mezclarlas.

# %%
# 3. PIPELINE DE DATOS ----------------------------------------------------------------------------
AUTOTUNE = tf.data.AUTOTUNE

# La augmentación se aplica solo a los datos de entrenamiento y VA FUERA del modelo exportado,
# para que el .tflite quede limpio (solo normalización -> MobileNetV2 -> clasificador).
AUGMENTACION = keras.Sequential(
    [
        layers.RandomFlip("horizontal"),
        layers.RandomRotation(0.08),
        layers.RandomZoom(0.15),
        layers.RandomContrast(0.15),
        layers.RandomBrightness(0.15, value_range=(0, 255)),
    ],
    name="augmentacion",
)


def _cargar(ruta, etiqueta):
    datos = tf.io.read_file(ruta)
    imagen = tf.image.decode_image(datos, channels=3, expand_animations=False)
    imagen.set_shape([None, None, 3])
    imagen = tf.image.resize(imagen, (IMG_SIZE, IMG_SIZE))  # float32, píxeles 0..255
    return imagen, etiqueta


def construir_dataset(rutas_, etiquetas_, entrenamiento):
    ds = tf.data.Dataset.from_tensor_slices((rutas_, etiquetas_))
    if entrenamiento:
        ds = ds.shuffle(len(rutas_), seed=SEMILLA, reshuffle_each_iteration=True)
    # Los archivos corruptos ya se descartaron en 2b; por eso NO se usa ignore_errors(), que dejaría el
    # tamaño del dataset "desconocido" y haría que Keras avise "ran out of data" al final de cada época.
    ds = ds.map(_cargar, num_parallel_calls=AUTOTUNE)
    if entrenamiento:
        ds = ds.map(lambda x, y: (AUGMENTACION(x, training=True), y), num_parallel_calls=AUTOTUNE)
    return ds.batch(BATCH).prefetch(AUTOTUNE)


ds_train = construir_dataset(r_train, y_train, entrenamiento=True)
ds_val = construir_dataset(r_val, y_val, entrenamiento=False)
ds_test = construir_dataset(r_test, y_test, entrenamiento=False)

# %%
# 4. MODELO ---------------------------------------------------------------------------------------
def construir_modelo():
    base = keras.applications.MobileNetV2(
        input_shape=(IMG_SIZE, IMG_SIZE, 3),
        include_top=False,
        weights=None if SMOKE else "imagenet",
    )
    base.trainable = False

    entrada = keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3), name="imagen")  # float32, píxeles 0..255
    # La normalización va DENTRO del modelo (equivale al preprocess_input de MobileNetV2): así la app
    # pasa los píxeles tal cual y no hay que replicar ningún preprocesado en Kotlin (evita un clásico
    # error de "entrenar de una forma y predecir de otra").
    x = layers.Rescaling(1 / 127.5, offset=-1, name="normalizar")(entrada)
    x = base(x, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dropout(0.3)(x)
    salida = layers.Dense(len(CLASES), activation="softmax", name="clase")(x)
    return keras.Model(entrada, salida, name="clasificador_residuos"), base


modelo, base = construir_modelo()
modelo.summary()


def pesos_de_clase(y):
    """Compensa el desbalance: las clases con menos fotos pesan más en la pérdida."""
    conteo = np.bincount(y, minlength=len(CLASES))
    return {i: float(len(y) / (len(CLASES) * max(c, 1))) for i, c in enumerate(conteo)}


PESOS = pesos_de_clase(y_train)
print("Pesos de clase:", {CLASES[i]: round(p, 2) for i, p in PESOS.items()})

# %%
# 5. ENTRENAMIENTO --------------------------------------------------------------------------------
def callbacks():
    return [
        keras.callbacks.EarlyStopping(monitor="val_loss", patience=4, restore_best_weights=True),
        keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.3, patience=2, min_lr=1e-7),
    ]


def compilar(lr):
    modelo.compile(
        optimizer=keras.optimizers.Adam(lr),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )


# Fase 1: base congelada, se entrena solo la cabeza.
compilar(1e-3)
h1 = modelo.fit(
    ds_train, validation_data=ds_val, epochs=EPOCHS_CABEZA, class_weight=PESOS, callbacks=callbacks(), shuffle=False, verbose=2
)  # shuffle=False: la mezcla ya la hace el propio dataset; así Keras no avisa que la ignora

# Fase 2: se descongelan las últimas capas de MobileNetV2 con una tasa de aprendizaje mucho menor.
# Las capas BatchNormalization se dejan congeladas (práctica recomendada por Keras al ajustar).
base.trainable = True
for capa in base.layers[:-CAPAS_A_DESCONGELAR]:
    capa.trainable = False
for capa in base.layers:
    if isinstance(capa, layers.BatchNormalization):
        capa.trainable = False
compilar(1e-5)
h2 = modelo.fit(
    ds_train, validation_data=ds_val, epochs=EPOCHS_AJUSTE, class_weight=PESOS, callbacks=callbacks(), shuffle=False, verbose=2
)

# %%
# Curvas de entrenamiento (se guardan como evidencia) ---------------------------------------------
import matplotlib

if SMOKE:
    matplotlib.use("Agg")
import matplotlib.pyplot as plt


def mostrar():
    """Muestra la figura en el cuaderno; en la prueba automática (sin pantalla) no hace nada."""
    if not SMOKE:
        plt.show()


historia = {k: h1.history[k] + h2.history[k] for k in h1.history}
fig, ejes = plt.subplots(1, 2, figsize=(11, 4))
for eje, metrica in zip(ejes, ["accuracy", "loss"]):
    eje.plot(historia[metrica], label="train")
    eje.plot(historia["val_" + metrica], label="val")
    eje.axvline(len(h1.history["loss"]) - 0.5, color="gray", linestyle="--", label="inicio fine-tuning")
    eje.set_title(metrica)
    eje.set_xlabel("época")
    eje.legend()
fig.tight_layout()
fig.savefig(SALIDA_DIR / "curvas_entrenamiento.png", dpi=120)
mostrar()

# %%
# 6. EVALUACIÓN -----------------------------------------------------------------------------------
from sklearn.metrics import classification_report, confusion_matrix


def predecir(modelo_, ds):
    """Probabilidades y etiquetas reales, tomadas del MISMO recorrido del dataset para que no se descuadren."""
    probs, verdad = [], []
    for x, y in ds:
        probs.append(modelo_(x, training=False).numpy())
        verdad.append(y.numpy())
    return np.concatenate(probs), np.concatenate(verdad)


probs_test, y_true = predecir(modelo, ds_test)
y_pred = probs_test.argmax(axis=1)
indices = list(range(len(CLASES)))

print(classification_report(y_true, y_pred, labels=indices, target_names=CLASES, zero_division=0))
REPORTE = classification_report(y_true, y_pred, labels=indices, target_names=CLASES, output_dict=True, zero_division=0)
EXACTITUD_TEST = float((y_true == y_pred).mean())
print(f"Exactitud en test: {EXACTITUD_TEST:.3f}   (recuerda: NO es el indicador de campo del Anexo 2)")

matriz = confusion_matrix(y_true, y_pred, labels=indices)
fig, eje = plt.subplots(figsize=(5.5, 5))
eje.imshow(matriz, cmap="Blues")
eje.set_xticks(indices, CLASES, rotation=45, ha="right")
eje.set_yticks(indices, CLASES)
eje.set_xlabel("predicho")
eje.set_ylabel("real")
for i in indices:
    for j in indices:
        eje.text(j, i, int(matriz[i, j]), ha="center", va="center")
fig.tight_layout()
fig.savefig(SALIDA_DIR / "matriz_confusion.png", dpi=120)
mostrar()

# %%
# 7. UMBRAL DE CONFIANZA --------------------------------------------------------------------------
# La app solo debería PROPONER una categoría si el modelo está suficientemente seguro; si no, cae en
# "mixto" y el usuario elige. El umbral se elige con VALIDACIÓN (no con test) para no sesgar el test.
#
# Piso de 0.50: con 4 clases, una confianza de 0.30 apenas supera el azar (0.25). Si el dataset es fácil
# (como TrashNet), la validación aceptaría umbrales absurdamente bajos; y esa validación es optimista
# respecto de la calle. Por eso el umbral nunca baja de 0.50; se recalibrará con datos de campo.
UMBRAL_MINIMO = 0.50
probs_val, y_val_true = predecir(modelo, ds_val)
pred_val = probs_val.argmax(axis=1)
confianza_val = probs_val.max(axis=1)

TABLA_UMBRALES = []
UMBRAL_SUGERIDO = None
print(f"{'umbral':>7}{'cobertura':>11}{'precisión':>11}")
for umbral in np.arange(UMBRAL_MINIMO, 0.96, 0.05):
    aceptadas = confianza_val >= umbral
    cobertura = float(aceptadas.mean())
    precision = float((pred_val[aceptadas] == y_val_true[aceptadas]).mean()) if aceptadas.any() else float("nan")
    # Sin predicciones aceptadas la precisión no existe: se guarda null (NaN no es JSON válido).
    TABLA_UMBRALES.append(
        {"umbral": round(float(umbral), 2), "cobertura": round(cobertura, 3), "precision": None if np.isnan(precision) else precision}
    )
    print(f"{umbral:>7.2f}{cobertura:>11.2f}{precision:>11.2f}")
    if UMBRAL_SUGERIDO is None and aceptadas.any() and precision >= PRECISION_OBJETIVO_UMBRAL:
        UMBRAL_SUGERIDO = round(float(umbral), 2)

UMBRAL_CUMPLE_OBJETIVO = UMBRAL_SUGERIDO is not None
if not UMBRAL_CUMPLE_OBJETIVO:
    UMBRAL_SUGERIDO = 0.70
    print(f"AVISO: ningún umbral alcanza {PRECISION_OBJETIVO_UMBRAL:.0%} de precisión; se usa 0.70 por defecto. "
          "El modelo todavía no es confiable: revisa el dataset antes de integrarlo en la app.")
print("Umbral sugerido:", UMBRAL_SUGERIDO)

# %% [markdown]
# ## 7b. Fotos "fuera de clase": medir la sobreconfianza
# El modelo es **cerrado**: ante *cualquier* foto elige una de sus 4 clases, aunque no haya residuo o sea de un
# tipo que no conoce (escombros, un muro, una vereda, suelo desnudo…). Lo único que evita que la app sugiera
# una categoría equivocada con aparente seguridad es que la confianza quede **bajo el umbral**.
#
# Esta celda lo **mide**. Crea `MyDrive/wanchaq_limpio_ml/dataset_fuera_de_clase/` y pon ahí fotos que *no*
# sean de las 4 clases (escombros, muros de concreto/ladrillo/adobe/piedra, veredas, suelo…). **No se usan para
# entrenar**, solo para probar. Se informa qué porcentaje supera el umbral. Fija el tope aceptable *antes* de
# ver el resultado (p. ej. "no más de 10 %"; es solo un ejemplo, decídelo con tu asesor). Toma esas fotos en
# **sitios distintos** a los de entrenamiento.

# %%
# 7b. Evaluación con fotos fuera de clase -----------------------------------------------------------
FUERA_DIR = RAIZ / "dataset_fuera_de_clase"


def generar_fuera_de_clase(carpeta, cantidad=6):
    """Solo para la prueba automática: ruido de un color que ninguna clase usa."""
    from PIL import Image

    carpeta.mkdir(parents=True, exist_ok=True)
    rng = np.random.default_rng(SEMILLA + 1)
    for n in range(cantidad):
        pixeles = np.clip(rng.normal([200, 200, 60], 40, size=(64, 64, 3)), 0, 255).astype("uint8")
        Image.fromarray(pixeles).save(carpeta / f"fuera_{n:02d}.jpg")


def evaluar_fuera_de_clase(carpeta, umbral):
    """Devuelve None si no hay fotos. Si las hay, cuántas superan el umbral (= la app sugeriría una clase
    equivocada con seguridad aparente) y cuáles son los peores casos."""
    candidatas = sorted(p for p in carpeta.glob("*") if p.suffix.lower() in EXTENSIONES) if carpeta.exists() else []
    rutas = [p for p in candidatas if es_decodificable(p)]
    if not rutas:
        return None
    filas = []
    for ruta in rutas:
        imagen, _ = _cargar(str(ruta), 0)
        probabilidades = modelo(tf.expand_dims(imagen, 0), training=False).numpy()[0]
        filas.append((ruta.name, CLASES[int(probabilidades.argmax())], float(probabilidades.max())))
    sobre = [f for f in filas if f[2] >= umbral]
    return {
        "n": len(filas),
        "umbral": umbral,
        "con_confianza_sobre_umbral": len(sobre),
        "tasa": len(sobre) / len(filas),
        "por_clase_predicha_sobre_umbral": dict(collections.Counter(f[1] for f in sobre)),
        "peores_casos": [
            {"archivo": nombre, "clase": clase, "confianza": round(confianza, 3)}
            for nombre, clase, confianza in sorted(filas, key=lambda f: -f[2])[:10]
        ],
    }


if SMOKE:
    generar_fuera_de_clase(FUERA_DIR)
FUERA_DE_CLASE = evaluar_fuera_de_clase(FUERA_DIR, UMBRAL_SUGERIDO)
if FUERA_DE_CLASE is None:
    print(
        f"AVISO: no hay fotos en {FUERA_DIR}. Sin ellas NO se sabe cuánto se equivocaría el modelo con escombros, "
        "muros o suelo. Agrégalas y vuelve a ejecutar esta celda (y las siguientes)."
    )
else:
    print(
        f"{FUERA_DE_CLASE['n']} fotos fuera de clase; {FUERA_DE_CLASE['con_confianza_sobre_umbral']} "
        f"({FUERA_DE_CLASE['tasa']:.0%}) superan el umbral {UMBRAL_SUGERIDO}: la app sugeriría una categoría "
        f"equivocada con seguridad aparente. Por clase: {FUERA_DE_CLASE['por_clase_predicha_sobre_umbral']}"
    )
    for caso in FUERA_DE_CLASE["peores_casos"][:5]:
        print("  más seguro:", caso)

# %%
# 8. EXPORTAR A TFLITE (float16) ------------------------------------------------------------------
saved_model_dir = SALIDA_DIR / "saved_model"
if saved_model_dir.exists():
    shutil.rmtree(saved_model_dir)
if hasattr(modelo, "export"):  # Keras 3
    # Keras imprime unas 150 líneas de "Captures: TensorSpec(...)" que no aportan nada; se silencian.
    with contextlib.redirect_stdout(io.StringIO()):
        modelo.export(str(saved_model_dir))
else:  # Keras 2
    tf.saved_model.save(modelo, str(saved_model_dir))

convertidor = tf.lite.TFLiteConverter.from_saved_model(str(saved_model_dir))
convertidor.optimizations = [tf.lite.Optimize.DEFAULT]
convertidor.target_spec.supported_types = [tf.float16]
tflite_bytes = convertidor.convert()

ruta_tflite = SALIDA_DIR / "clasificador_residuos.tflite"
ruta_tflite.write_bytes(tflite_bytes)
print(f"{ruta_tflite.name}: {len(tflite_bytes) / 1e6:.1f} MB")

# %%
# 9. VERIFICAR EL .TFLITE -------------------------------------------------------------------------
# Comprobación clave: el modelo convertido debe comportarse igual que el original. Si no, la app
# mostraría resultados distintos a los que aquí se midieron.
def cargar_interprete(ruta):
    """LiteRT (ai_edge_litert) es el intérprete vigente; tf.lite.Interpreter está en desuso (TensorFlow
    avisa que lo eliminará). Se prefiere el primero y se recurre al segundo si no está instalado."""
    try:
        from ai_edge_litert.interpreter import Interpreter
    except ImportError:
        Interpreter = getattr(tf.lite, "Interpreter", None)
        if Interpreter is None:  # una versión futura de TensorFlow ya lo eliminó
            instalar("ai-edge-litert")
            from ai_edge_litert.interpreter import Interpreter
    return Interpreter(model_path=str(ruta))


interprete = cargar_interprete(ruta_tflite)
interprete.allocate_tensors()
entrada = interprete.get_input_details()[0]
salida = interprete.get_output_details()[0]
print("Entrada:", entrada["shape"], entrada["dtype"].__name__)
print("Salida: ", salida["shape"], salida["dtype"].__name__)

concordancias, diferencia_maxima, n = 0, 0.0, 0
for x, _ in ds_test:
    esperado = modelo(x, training=False).numpy()
    for i in range(x.shape[0]):
        interprete.set_tensor(entrada["index"], x[i : i + 1].numpy().astype(np.float32))
        interprete.invoke()
        obtenido = interprete.get_tensor(salida["index"])[0]
        concordancias += int(obtenido.argmax() == esperado[i].argmax())
        diferencia_maxima = max(diferencia_maxima, float(np.abs(obtenido - esperado[i]).max()))
        n += 1
    if n >= 200:  # con 200 imágenes basta para detectar una conversión defectuosa
        break

CONCORDANCIA = concordancias / n
print(f"Concordancia keras vs tflite: {CONCORDANCIA:.3f} ({n} imágenes) | diferencia máxima de probabilidad: {diferencia_maxima:.4f}")
assert diferencia_maxima < 0.05, "El .tflite difiere demasiado del modelo original: no usar."
if not SMOKE:
    assert CONCORDANCIA >= 0.97, "El .tflite no coincide con el modelo original: no usar."

# %%
# 10. GUARDAR LOS ARCHIVOS PARA LA APP ------------------------------------------------------------
(SALIDA_DIR / "labels.txt").write_text("\n".join(CLASES) + "\n", encoding="utf-8")

sha256 = hashlib.sha256(ruta_tflite.read_bytes()).hexdigest()
metadata = {
    "modelo": "MobileNetV2 (transfer learning desde ImageNet)",
    "fecha": datetime.datetime.now().isoformat(timespec="seconds"),
    "tensorflow": tf.__version__,
    "semilla": SEMILLA,
    "cuantizacion": "float16",
    "sha256_tflite": sha256,
    "entrada": {
        "forma": [1, IMG_SIZE, IMG_SIZE, 3],
        "tipo": "float32",
        "rango": "0..255 (píxeles tal cual; la normalización está dentro del modelo)",
        "orden_canales": "RGB",
    },
    "salida": {"forma": [1, len(CLASES)], "tipo": "float32", "descripcion": "probabilidades softmax, mismo orden que labels.txt"},
    "clases": CLASES,
    "categorias_manuales": CATEGORIAS_MANUALES,
    "umbral_confianza_sugerido": UMBRAL_SUGERIDO,
    "umbral_alcanza_precision_objetivo": UMBRAL_CUMPLE_OBJETIVO,
    "precision_objetivo_umbral": PRECISION_OBJETIVO_UMBRAL,
    "dataset": {
        "conteo_por_clase": MANIFIESTO,
        "archivos_descartados": DESCARTADOS,
        "particion": {"train": int(len(r_train)), "val": int(len(r_val)), "test": int(len(r_test))},
        "fuentes": FUENTES_DATASET,
    },
    "advertencia": (
        "Las métricas se miden sobre fotos del mismo dataset de entrenamiento y sobreestiman el desempeño en "
        "calle. El indicador de la tesis (coincidencia IA-observador) se mide en campo."
    ),
}
(SALIDA_DIR / "metadata.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")

metricas = {
    "exactitud_test": EXACTITUD_TEST,
    "por_clase": {c: REPORTE[c] for c in CLASES},
    "macro_f1": REPORTE["macro avg"]["f1-score"],
    "concordancia_keras_vs_tflite": CONCORDANCIA,
    "fuera_de_clase": FUERA_DE_CLASE,  # None si no se aportaron fotos: entonces el riesgo NO está medido
    "tabla_umbrales_validacion": TABLA_UMBRALES,
}
(SALIDA_DIR / "metricas.json").write_text(json.dumps(metricas, ensure_ascii=False, indent=2), encoding="utf-8")

print("Archivos en", SALIDA_DIR)
for archivo in ["clasificador_residuos.tflite", "labels.txt", "metadata.json", "metricas.json", "curvas_entrenamiento.png", "matriz_confusion.png"]:
    print(" -", archivo)
print("sha256 del modelo:", sha256)

# %%
# 11. (OPCIONAL) Descargar a tu computadora --------------------------------------------------------
try:
    from google.colab import files

    for archivo in ["clasificador_residuos.tflite", "labels.txt", "metadata.json", "metricas.json"]:
        files.download(str(SALIDA_DIR / archivo))
except ImportError:
    pass

# %% [markdown]
# ## Siguiente paso
# Copia `clasificador_residuos.tflite`, `labels.txt` y `metadata.json` a `app/src/main/assets/` del proyecto y
# avísale a Claude: el Módulo 3 integra el modelo en la app (con el umbral de `metadata.json`).
# Guarda también `metricas.json` y las dos imágenes: son evidencia para la tesis.
