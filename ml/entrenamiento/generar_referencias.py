"""Genera las imágenes de referencia y las probabilidades esperadas ("golden") para la prueba instrumentada de Android.

Por qué existe: en la JVM no se puede ejecutar LiteRT (sus librerías nativas son de Android), así que la ÚNICA forma
de comprobar que la app prepara la foto igual que el entrenamiento (canales RGB, rango 0-255, orientación EXIF) y
que lee bien la salida del modelo es correr, en un dispositivo, el clasificador real sobre imágenes cuyo resultado
ya se conoce. Este script calcula ese resultado con el MISMO `.tflite` y el mismo preprocesado que el cuaderno.

Las imágenes miden justo 224x224 (el tamaño de entrada): así el redimensionado es la identidad en Python y en
Android, y no entra en juego la diferencia entre `tf.image.resize` y el escalado de Android. La prueba compara
probabilidades con una tolerancia (los decodificadores JPEG pueden diferir en ±1 nivel de píxel).

Una de las imágenes lleva EXIF Orientation = 6 ("girar 90° a la derecha para verla derecha"), como las que
guarda CameraX con el teléfono en vertical: si la app no aplica la orientación, su resultado no coincide.

Uso (desde esta carpeta, con TensorFlow y pillow instalados):  python generar_referencias.py
Salida: app/src/androidTest/assets/referencias/  (imágenes + referencias.json). Se versiona: es la referencia.
"""

import json
import os
import pathlib

import numpy as np
from PIL import Image

os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")
import tensorflow as tf  # noqa: E402

RAIZ = pathlib.Path(__file__).resolve().parents[2]
MODELO = RAIZ / "app" / "src" / "main" / "assets" / "clasificador" / "clasificador_residuos.tflite"
LABELS = RAIZ / "app" / "src" / "main" / "assets" / "clasificador" / "labels.txt"
DESTINO = RAIZ / "app" / "src" / "androidTest" / "assets" / "referencias"
LADO = 224
ORIENTACION_GIRAR_90_HORARIO = 6


def degradado_rojo_azul():
    x = np.linspace(0, 1, LADO)[None, :, None]
    rojo, azul = np.array([255, 0, 0]), np.array([0, 0, 255])
    return np.repeat((rojo * (1 - x) + azul * x), LADO, axis=0).astype("uint8")


def bloques_de_color():
    imagen = np.zeros((LADO, LADO, 3), dtype="uint8")
    mitad = LADO // 2
    imagen[:mitad, :mitad] = (40, 160, 60)  # verde
    imagen[:mitad, mitad:] = (230, 210, 40)  # amarillo
    imagen[mitad:, :mitad] = (120, 120, 120)  # gris
    imagen[mitad:, mitad:] = (240, 240, 240)  # casi blanco
    return imagen


def franjas_marron_beige():
    imagen = np.zeros((LADO, LADO, 3), dtype="uint8")
    for i in range(0, LADO, 28):
        imagen[:, i : i + 14] = (110, 70, 40)
        imagen[:, i + 14 : i + 28] = (215, 190, 150)
    return imagen


def izquierda_roja_derecha_azul():
    imagen = np.zeros((LADO, LADO, 3), dtype="uint8")
    imagen[:, : LADO // 2] = (220, 30, 30)
    imagen[:, LADO // 2 :] = (30, 30, 220)
    return imagen


REFERENCIAS = {
    # nombre: (píxeles guardados, orientación EXIF o None)
    "degradado_rojo_azul.jpg": (degradado_rojo_azul(), None),
    "bloques_de_color.jpg": (bloques_de_color(), None),
    "franjas_marron_beige.jpg": (franjas_marron_beige(), None),
    "izquierda_roja_derecha_azul_exif6.jpg": (izquierda_roja_derecha_azul(), ORIENTACION_GIRAR_90_HORARIO),
}


def guardar_jpeg(ruta, pixeles, orientacion):
    imagen = Image.fromarray(pixeles)
    if orientacion is None:
        imagen.save(ruta, format="JPEG", quality=95)
    else:
        exif = Image.Exif()
        exif[0x0112] = orientacion
        imagen.save(ruta, format="JPEG", quality=95, exif=exif)


def main():
    DESTINO.mkdir(parents=True, exist_ok=True)
    clases = [linea.strip() for linea in LABELS.read_text(encoding="utf-8").splitlines() if linea.strip()]
    interprete = tf.lite.Interpreter(model_path=str(MODELO))
    interprete.allocate_tensors()
    entrada, salida = interprete.get_input_details()[0], interprete.get_output_details()[0]

    referencias = {}
    for nombre, (pixeles, orientacion) in REFERENCIAS.items():
        ruta = DESTINO / nombre
        guardar_jpeg(ruta, pixeles, orientacion)

        # Lo que ve el modelo = la imagen DECODIFICADA del JPEG (con su pérdida), como en la app.
        decodificada = tf.image.decode_image(tf.io.read_file(str(ruta)), channels=3, expand_animations=False).numpy()
        if orientacion == ORIENTACION_GIRAR_90_HORARIO:
            decodificada = np.rot90(decodificada, k=-1)  # k=-1: 90° en sentido horario
        interprete.set_tensor(entrada["index"], decodificada.astype(np.float32)[None])
        interprete.invoke()
        probabilidades = interprete.get_tensor(salida["index"])[0]

        referencias[nombre] = {
            "exif_orientacion": orientacion,
            "probabilidades": [round(float(p), 5) for p in probabilidades],
            "top1": clases[int(probabilidades.argmax())],
            "confianza": round(float(probabilidades.max()), 5),
        }

    contenido = {
        "clases": clases,
        "modelo_sha256": __import__("hashlib").sha256(MODELO.read_bytes()).hexdigest(),
        "tolerancia_sugerida": 0.05,
        "referencias": referencias,
    }
    (DESTINO / "referencias.json").write_text(json.dumps(contenido, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for nombre, r in referencias.items():
        print(f"{nombre:<42} {r['top1']:<9} {r['confianza']:.3f}  {r['probabilidades']}")
    print("\nReferencias en", DESTINO)


if __name__ == "__main__":
    main()
