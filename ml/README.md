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
| `entrenamiento/probar_modelo.py` | Prueba el `.tflite` con **fotos reales de calle**, sin Colab, y ayuda a elegir el umbral de confianza. |
| `entrenamiento/generar_referencias.py` | Genera las imágenes de referencia y las probabilidades esperadas para `ClasificadorResiduoReferenciaTest` (prueba en dispositivo de la app). |
| `resultados/v1/` | `metadata.json` y `metricas.json` **originales** de la primera corrida en Colab (evidencia; no se editan). |

## Cómo entrenar (Colab)

1. Entra a <https://colab.research.google.com> → **Archivo → Subir notebook** → elige
   `ml/entrenamiento/clasificador_residuos.ipynb`.
2. **Entorno de ejecución → Cambiar tipo de entorno de ejecución → GPU (T4)**.
3. Sube tus fotos a Drive con esta estructura exacta (una carpeta por clase):

   ```
   Mi unidad/wanchaq_limpio_ml/
     dataset/                  ← entrena el modelo (una carpeta por clase, nombres exactos)
       plastico/   carton/   vidrio/   organico/
     dataset_fuera_de_clase/   ← SOLO para probar (escombros, muros, veredas, suelo…). No entrena.
   ```

   `plastico`, `carton` y `vidrio` se rellenan solas con TrashNet (celda 2a). **`organico` la tienes que
   aportar tú** (mínimo 50 fotos; lo razonable son 200 o más). Si no las hay, el cuaderno se detiene con un
   mensaje claro en vez de entrenar un modelo engañoso. Formatos válidos:
   `.jpg`, `.jpeg`, `.png`, `.bmp` (las fotos `.heic`/`.webp` del celular conviértelas antes: el cuaderno
   te avisa por nombre de cualquier archivo que ignore o que esté corrupto).

   `dataset_fuera_de_clase/` es opcional pero **muy recomendable**: sin esas fotos el cuaderno avisa que el
   riesgo de confundir escombros o muros con una clase **no está medido** (ver [Guía de fotos](#guía-de-fotos)).
4. **Entorno de ejecución → Ejecutar todo.** Acepta el permiso para montar Drive.
5. Al terminar se descargan `clasificador_residuos.tflite`, `labels.txt`, `metadata.json` y
   `metricas.json`; todo queda también en `Mi unidad/wanchaq_limpio_ml/salida/` junto con las gráficas.
6. Copia `clasificador_residuos.tflite`, `labels.txt` y `metadata.json` a **`app/src/main/assets/clasificador/`** (la app los lee de ahí; la prueba `ContratoModeloAssetsTest` verifica que el par sea consistente). Si corriges algo a mano en `metadata.json`, **regístralo en `correcciones_manuales` dentro del propio archivo** y no toques el original en `ml/resultados/`. Tras cambiar el modelo, regenera las referencias de la prueba de dispositivo con `python generar_referencias.py`. La integración ya está hecha (Módulo 3b): ver [`docs/adr/0003`](../docs/adr/0003-litert-interpreter-como-runtime-de-ia.md).

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
| Salida | `float32 [1, 4]`, probabilidades softmax | `probar_notebook.py` |
| Orden de clases | `plastico, carton, vidrio, organico` = `labels.txt` = los valores de `CategoriaResiduo` en Kotlin **menos** las categorías manuales, en el mismo orden | `probar_notebook.py` (lee el `.kt`) |
| Categorías **manuales** | `escombros` y `mixto` **no** son clases del modelo (`metadata.json → categorias_manuales`). El ciudadano las elige a mano; `mixto` además es lo que la app propone cuando la confianza queda por debajo del umbral | `probar_notebook.py`; Módulo 3b (Android) |
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
| **`escombros` fuera del modelo** (2026-09-18, propuesta del usuario) | Ningún dataset público lo cubre y, siendo material mineral gris, se confunde con muros, veredas y suelo. Quitarlo evita los falsos positivos *hacia* escombros y no exige reunir sus fotos. **Lo que no resuelve:** el modelo sigue siendo cerrado, así que un montón de escombros real se forzará hacia otra clase salvo que la confianza quede bajo el umbral. Por eso existe la celda 7b, que lo **mide**. La categoría sigue en la app como elección manual (es una categoría de la investigación). |
| Piso de 0,50 en el umbral de confianza | Con 4 clases, 0,30 apenas supera el azar (0,25). Un dataset fácil como TrashNet haría que la validación aceptara umbrales absurdamente bajos, y esa validación ya es optimista respecto de la calle. Se recalibrará con datos de campo. |
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
| `organico` | **Ninguna** en el cuaderno. Ver [Datasets evaluados](#datasets-evaluados-y-por-qué-no-son-la-fuente-principal) | Fotos propias del equipo |
| `escombros`, `mixto` | No son clases del modelo (ver contrato): el ciudadano las elige a mano | Fotos `escombros` solo para **probar** (`dataset_fuera_de_clase/`) |

Reglas para las fotos propias (las mismas de la app): **sin rostros, sin placas de vehículos, sin interiores
de viviendas**; variar luz, ángulo y distancia; el residuo bien visible. Las fotos **no se suben a git**
(`.gitignore` excluye `ml/**/dataset/`): viven solo en el Drive del equipo.

Licencia y cita: TrashNet es MIT y su autor pide citar el repositorio
(*Thung, G. y Yang, M. (2016), "Classification of Trash for Recyclability Status", Stanford CS229 —
github.com/garythung/trashnet*). Queda registrado en `metadata.json` → `dataset.fuentes`.

### Cómo declarar las fuentes (`FUENTES_DATASET`)

El cuaderno copia `FUENTES_DATASET` (celda "1. CONFIGURACIÓN") tal cual a `metadata.json → dataset.fuentes`, que se versiona junto con el modelo: es la **constancia de procedencia** de los datos. Una entrada por *fuente*, aunque las fotos acaben mezcladas en la misma carpeta de clase, y da igual que las subas a Drive a mano: lo que importa es declarar de dónde salieron. Los campos son libres (el cuaderno no los valida); plantilla para `organico` con Kaggle + fotos propias:

```python
FUENTES_DATASET = [
    {  # la de TrashNet ya viene escrita; déjala como está
        "clases": ["plastico", "carton", "vidrio"],
        "fuente": "TrashNet, dataset-resized.zip (huggingface.co/datasets/garythung/trashnet; github.com/garythung/trashnet)",
        "licencia": "MIT",
    },
    {
        "clases": ["organico"],
        "fuente": "Kaggle, 'Waste Classification Dataset' de PhenomSG (kaggle.com/datasets/phenomsg/waste-classification), solo la carpeta Organic",
        "licencia": "MIT según la página de Kaggle (declarada por quien lo subió); las imágenes provienen de búsquedas web y sus derechos no se verificaron",
        "version": "1 (última actualización 2023-07-28)",
        "descargado": "AAAA-MM-DD",  # tu fecha real de descarga
        "n_original": 677,  # archivos en Organic/ según el listado de Kaggle
        "n_usadas": 0,  # las que quedaron TRAS filtrar (ver el conteo abajo)
        "filtrado": "descartadas a mano las ilustraciones, las miniaturas y las que no eran comida ni hojas",
    },
    {
        "clases": ["plastico", "carton", "vidrio", "organico"],
        "fuente": "Fotos propias del equipo, tomadas en el distrito de Wanchaq",
        "licencia": "Propias del equipo (sin rostros, placas ni interiores de viviendas)",
        "periodo": "AAAA-MM-DD a AAAA-MM-DD",
        "n_usadas": 0,
    },
]
```

- **`conteo_por_clase` ya lo registra el cuaderno solo**; los `n_usadas` de cada fuente los pones tú, y hay que contarlos *después* de filtrar. Con el prefijo `kaggle_` (ver abajo), agrega una celda tras la 2b y ejecútala: `sum(p.name.startswith("kaggle_") for p in ARCHIVOS["organico"])`.
- Si dejas algo en `COMPLETAR`, así quedará escrito en `metadata.json`: el cuaderno no lo impide.
- **Guarda evidencia el día que descargas** (captura o PDF de la página de Kaggle con la licencia y la descripción): las páginas cambian.
- Para la bibliografía de la tesis (formato APA 7): PhenomSG. (2023). *Waste Classification Dataset* [Conjunto de datos]. Kaggle. https://www.kaggle.com/datasets/phenomsg/waste-classification

**Al subir `Organic` a mano, no copies las 5 subcarpetas a una sola sin renombrar:** hay **9 nombres repetidos** entre ellas (p. ej. `maxresdefault.jpg` ×4, `maxresdefault2.jpg` ×4) y un copiado a ciegas los pisaría. Antepón la subcarpeta al nombre; además deja el rastro `kaggle_` para contarlas después. En PowerShell, desde la carpeta que contiene `Organic\` (probado el 2026-09-18 con nombres repetidos, guion inicial y corchetes; si lo repites no pasa nada, solo vuelve a copiar lo mismo):

```powershell
$dest = "$PWD\organico_kaggle"; New-Item -ItemType Directory -Force $dest | Out-Null
Get-ChildItem -Recurse -File .\Organic | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $dest ("kaggle_" + $_.Directory.Name + "__" + $_.Name))
}
```

Luego sube el contenido de `organico_kaggle` a `dataset/organico/`. Al filtrar: 18 de los 677 archivos son `.webp` (el cuaderno los ignora y los avisa por nombre), y los `maxresdefault*` parecen **miniaturas de YouTube** (inferencia por el nombre; suelen traer texto o personas): revísalos o bórralos, por la regla de no rostros.

## Datasets evaluados (y por qué no son la fuente principal)

Verificados el 2026-09-18 leyendo **solo metadatos públicos** (listado de archivos, README, cuaderno del autor). No se descargó ninguna imagen: Kaggle exige iniciar sesión y las credenciales no las maneja Claude. Lo que sigue es lo comprobado; lo que no se pudo comprobar se dice como tal.

| Fuente | Qué es realmente | Veredicto |
|---|---|---|
| **TrashNet** (`garythung/trashnet`, Hugging Face) | 2 527 fotos (cardboard 403, glass 501, metal 410, paper 594, plastic 482, trash 137), un objeto solo sobre cartulina blanca. MIT; su autor pide citarlo | **Adoptado** para `plastico`, `carton` y `vidrio`, con su sesgo de fondo blanco declarado |
| **Kaggle** `phenomsg/waste-classification` | Ver detalle abajo | **No adoptado como fuente principal.** Solo como parche para `organico`, con condiciones |
| **GitHub** `PhenomSG/CV-based-Waste-Identifier` | Un clasificador **binario** (reciclable / no reciclable), no el multiclase que necesitamos | **No sirve** para este proyecto |

**Kaggle `phenomsg/waste-classification`** — lo declarado frente a lo comprobado:

| Declarado | Comprobado |
|---|---|
| Subtítulo: *"30,000+ images"* | El listado completo (15 páginas de la API pública; la suma de bytes coincide **exactamente** con los 1 111 740 194 B que declara Kaggle) tiene **2 939 archivos**, unas 10 veces menos |
| Descripción: subcarpetas *Chemical-Waste, Medical-Waste, Food-Waste, Green-Waste…* | Las carpetas reales son otras: `batteries, e-waste, paints, pesticides` · `ceramic_product, diapers, platics_bags_wrappers, sanitary_napkin, stroform_product` · `coffee_tea_bags, egg_shells, food_scraps, kitchen_waste, yard_trimmings` · `cans_all_type, glass_containers, paper_products, plastic_bottles` |
| Licencia MIT | Es la que declara quien lo subió. Los **nombres de archivo** indican que las imágenes salen de búsquedas en la web: nombres de agencias de stock (`…-isolated-on-white-background-…-2094399346.jpg`), ilustraciones (`…flat-illustration…`, `…vector-illustration…`, `kisspng-…clip-art…`) y figuras de artículos científicos (`1-s2.0-S…`, ids de ScienceDirect). Solo por el nombre, al menos ≈ 7 % son ilustraciones/objetos aislados en fondo blanco y ≈ 15 % traen marcas o ids de stock (**cota inferior**: la mayoría de los nombres no dicen nada). Es muy probable que las imágenes pertenezcan a terceros; **no se verificaron los derechos** |
| Categorías por reciclabilidad (Peligroso, No reciclable, Orgánico, Reciclable) | No coinciden con las categorías del estudio, que son por material. Mapeo posible: `Organic/*` (5 carpetas, 677 archivos) → `organico`; `plastic_bottles` (130) y `platics_bags_wrappers` (135) → `plastico`; `glass_containers` (142) → `vidrio`; `paper_products` (121) → `carton` (es papel, no necesariamente cartón). `cans_all_type` es metal (no es categoría); el resto no aplica. **No hay nada equivalente a escombros** |

*No verificado:* el contenido real de las imágenes, el ruido de etiquetas y los duplicados (habría que descargarlas).

**Veredicto:** no sirve como fuente principal porque (1) los derechos son inciertos, (2) son fotos de stock e ilustraciones, es decir, **más alejadas de la calle que TrashNet** justo en lo que importa, (3) es pequeño y probablemente ruidoso. Solo lo consideraría como **parche para `organico`**, y con condiciones: descargarlo tú con tu sesión de Kaggle (1,1 GB; basta con las carpetas `Organic`), **filtrar a mano** ilustraciones y renders, declararlo en `metadata.json → dataset.fuentes`, y —si el modelo va a publicarse en Play Store— revisar antes el tema de derechos con el asesor. Las fotos propias siguen siendo preferibles.

**GitHub `PhenomSG/CV-based-Waste-Identifier`:** sin licencia (la API de GitHub devuelve `license: null`, es decir, todos los derechos reservados: el código no es reutilizable); 5 estrellas, último cambio 2023-10-13. Su cuaderno entrena una CNN pequeña **binaria** (`Dense(1, sigmoid)`, 3 capas convolucionales, 5 épocas) sobre "22 564 archivos de 2 clases" (carpetas `r`/`nr`), un conjunto que **no coincide** con el de Kaggle publicado (2 939 archivos, 4 categorías). Sus resultados impresos: precisión 0,80, recall 0,85, exactitud 0,846; la exactitud de validación tocó su máximo (0,854) en la época 4 y luego bajó mientras la de entrenamiento seguía subiendo (sobreajuste). El README dice "alta exactitud" sin cifras. No aporta modelo multiclase ni pesos utilizables; su único valor es `Waste_items_list.txt`, una lista de objetos marcados `-DONE` que sugiere (inferencia) que el dataset se armó buscando cada objeto en la web.

## Guía de fotos

**Regla de oro: que el modelo aprenda el residuo, no el fondo.** TrashNet son objetos sobre cartulina blanca. Si `organico` se fotografía en la calle y las otras tres clases no, el modelo puede resolver el examen con un atajo (*"fondo de calle → orgánico"*) y fallar con una botella sobre una vereda. Por eso:

1. **Fotografía en calle las 4 clases**, no solo `organico` (bolsas y botellas contra un muro, cartón sobre la vereda, restos de comida en una esquina…). Orden de magnitud, no regla: unas 60–100 fotos de calle por clase; y baja `MAX_TRASHNET_POR_CLASE` para que TrashNet no las ahogue.
2. **`organico` con variedad**: restos de comida, cáscaras, hojas y poda, en distintos sitios y con luz distinta. Sin rostros, placas ni interiores de viviendas.
3. **Nombra los archivos con el sitio**, p. ej. `sitioA__001.jpg`. Fotos casi idénticas del mismo montón repartidas entre *train* y *test* inflan la exactitud (fuga de datos); lo correcto es que **todo un sitio caiga en una sola partición**. *(El cuaderno todavía parte por foto; partir por sitio es una mejora propuesta, pendiente de decisión.)*
4. **`dataset_fuera_de_clase/` — solo para probar, nunca para entrenar.** Fotos de lo que el modelo *no* debe sugerir con seguridad: **escombros y desmonte**, muros de concreto, ladrillo, adobe y piedra (Cusco tiene muchos), veredas, empedrado, fachadas, escaleras, suelo desnudo y obras en pie. Tómalas en **sitios distintos** a los de entrenamiento. Orden de magnitud: 30–60 fotos ya dan una idea.
   - *Definición de trabajo de "escombros"*, tomada de la Ficha de Observación de Campo (*"Desmonte y escombros"*, no solo "restos de concreto"): acumulación **suelta** de material de obra en el espacio público (concreto o ladrillo fragmentado, mortero, tejas, tierra o grava de excavación), en montones o sacos; se decide por el material **predominante**. **No** lo es una estructura en pie ni el suelo sin montón.
5. **Fija el tope aceptable antes de ver el resultado** (un ejemplo: "no más de 10 %"; decídelo con el asesor). La celda 7b imprime qué porcentaje de esas fotos supera el umbral y lo guarda en `metricas.json → fuera_de_clase`.

**Si la tasa resulta alta**, en orden de menor a mayor esfuerzo: (a) subir el umbral (menos sugerencias, más `mixto`); (b) agregar una clase `sin_residuo` entrenada con esos negativos (cambia el contrato: 5 salidas, y la app la trataría como "sin sugerencia", nunca para bloquear un reporte); (c) volver a entrenar `escombros` como clase con fotos propias.

Además, la propia app dará evidencia gratis: cuando el Módulo 3b guarde la sugerencia del modelo aparte de la categoría final del ciudadano, se sabrá cuántas veces y hacia qué clase se corrige la sugerencia.

## Probar el modelo con fotos reales de calle

Las métricas del cuaderno usan el mismo tipo de fotos con que se entrenó y **no dicen cómo funcionará en la calle**. `probar_modelo.py` sí lo mide, con tus fotos, sin Colab.

```
ml/prueba_calle/            ← NO se sube a git (pueden traer rostros o placas)
  plastico/  carton/  vidrio/  organico/    ← ~20 fotos reales de cada una
  fuera_de_clase/                           ← 30–60: escombros, muros, veredas, suelo…
```

```bash
cd ml/entrenamiento
python probar_modelo.py ../prueba_calle --tope-fuera 0.10
```

Requiere TensorFlow (usa su mismo decodificado y redimensionado que el entrenamiento, para que la prueba sea fiel). Cómo leer el informe:

- **Acierta / IC 95 %**: el intervalo importa más que el porcentaje. Con ~20 fotos por clase es ancho: *aun con 20 de 20 correctas, el límite inferior es ≈ 84 %*.
- **Sug. ok / sug. mal / mixto**: con el umbral evaluado, cuántas veces la app sugeriría bien, sugeriría mal **con seguridad** (lo más dañino), o no sugeriría (`mixto`).
- **Fuera de clase**: qué % de escombros/muros/suelo supera el umbral, es decir, la app los clasificaría con seguridad como algo que no son.
- **Barrido de umbrales** y **umbral mínimo que cumple** una precisión objetivo (85 % por defecto) y el tope de fuera de clase. **Fija el tope ANTES de ver el resultado** (`--tope-fuera`; 0.10 es solo un ejemplo, decídelo con el asesor).

Reglas: estas fotos son **solo para probar**; si se usan para entrenar, la prueba deja de valer. Y no reemplazan el indicador de la tesis (coincidencia IA–observador, medida en campo). El informe se guarda como JSON (sin fotos) en `ml/resultados/` para citarlo como evidencia.

**Advertencia ilustrativa:** al probar la herramienta con imágenes de *ruido puro*, el modelo v1 respondió "cartón" con 83–89 % de confianza en la mayoría. No son fotos reales y no mide nada de la calle, pero muestra que una confianza alta no garantiza que la foto sea de una clase: el umbral es una defensa parcial.

## Limitaciones honestas

1. **El modelo no reconoce `escombros`.** Es una categoría de la investigación ("Desmonte y escombros" en la ficha de campo) que la app ofrece solo como elección manual. Un montón de escombros real **no tiene clase propia**: el modelo lo forzará hacia una de las 4 salvo que la confianza quede bajo el umbral. El riesgo se mide en la celda 7b, no se supone. Consecuencia para la tesis: el indicador *coincidencia IA–observador* nunca podrá acertar un foco cuyo tipo predominante sea "desmonte y escombros"; hay que acotarlo o declararlo (ver `docs/correcciones-documento.md`).
2. **Sesgo del dataset:** TrashNet fotografía *un objeto solo sobre una cartulina blanca* (según su propio
   README). La basura real en Wanchaq aparece mezclada, sucia, parcial y con fondos complejos. Las métricas
   del cuaderno serán optimistas; la referencia publicada por sus autores para su propia red fue ≈ 75 % en
   *su* test, es decir, ya en condiciones favorables.
3. **Fuga de datos:** TrashNet fotografía cada objeto varias veces; dos fotos casi idénticas pueden caer una
   en *train* y otra en *test* e inflar la exactitud. Lo mismo pasa con ráfagas de fotos propias.
4. **`metal` no es categoría** de la app (Marco Conceptual, Cap. II), así que una lata se clasificará como la
   clase más parecida (probablemente `plastico`). Se documenta como limitación, no se oculta.
5. **El indicador real (Anexo 2) se mide en campo.** Esto es una comprobación de desarrollo.
6. **Reentrenar con fotos de ciudadanos** (mejora futura tentadora) queda **fuera** del propósito que declara
   el consentimiento (Anexo 9). Requiere consultarlo con el asesor antes de siquiera diseñarlo.
7. **El modelo en el teléfono no da exactamente las mismas probabilidades que en Python** (medido el 2026-09-19 en una Galaxy Tab S9+, Android 16, LiteRT). Se comprobó que **la entrada es idéntica**: la que prepara la app y la que prepara Python (PIL) coinciden en las 150 528 cifras de cada una de las 4 imágenes de referencia (diferencia 0,0), y que el `.tflite` es el mismo (`sha256`). Aun así, con esa misma entrada, dos imágenes dan las mismas probabilidades hasta la cuarta cifra y otras dos no: el degradado rojo-azul da 0,17 / 0,14 / **0,68** / 0,00 en Python y 0,23 / 0,16 / **0,60** / 0,00 en el teléfono (hasta 0,08 de diferencia en la confianza), y las franjas marrón-beige, 0,957 frente a 0,984 para `carton`. Es una diferencia de **cómo el intérprete del teléfono calcula el modelo**, no del preprocesado, y es mayor en imágenes sintéticas que el modelo no ha visto. **Consecuencia para la investigación**: el umbral de confianza (hoy 0,70, provisional) debe **calibrarse con las cifras que da la app en el teléfono**, no solo con las de `probar_modelo.py`; los ≈ 0,08 de diferencia son del orden de lo que separa «propone una categoría» de «propone `mixto`». No se investigó la causa (¿precisión de los núcleos del intérprete en ARM?); **sin resolver**.

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
