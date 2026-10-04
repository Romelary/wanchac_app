# Subsanación y Actualización — Capítulo III: Metodología

> **Documento de destino**: `Proyecto de Investigación v5.91 (1) (1).docx`  
> **Sección a sustituir**: Capítulo III (secciones 3.2, 3.3.3, 3.3.4 y 3.5; páginas 33 a 54 del documento Word).  
> **Estado**: Listo para copiar y pegar directamente en el documento de tesis.  
> **Fundamentación técnica**: Script de verificación [`analisis/confiabilidad/verificar_documento_v591.py`](../../analisis/confiabilidad/verificar_documento_v591.py), dimensionamiento muestral [`analisis/confiabilidad/dimensionamiento.py`](../../analisis/confiabilidad/dimensionamiento.py), y plan metodológico [`docs/plan-confiabilidad-instrumentos.md`](../plan-confiabilidad-instrumentos.md).

---

## RESUMEN DE CAMBIOS EN EL CAPÍTULO III

| Elemento en v5.91 | Problema en el borrador original | Corrección y Reemplazo Definitivo |
|---|---|---|
| **Tabla 3 y Tabla 4 (Validez de Juicio de Expertos - pág. 42-43)** | La "V de Aiken" consignada en la Tabla 4 (0.83, 0.85, 0.84) correspondía en realidad al promedio aritmético simple de los porcentajes dividido entre 100, no a la fórmula formal de Aiken. Además, la dispersión decía "59.2 a 94.0" cuando el mínimo real era 60.1. | Se recalculan formalmente los coeficientes mediante la ecuación $V = \frac{S}{n(c-1)}$, obteniendo $V = 0.89$ (SUS), $V = 0.87$ (Ficha de campo) y $V = 0.89$ (Ficha GPS), demostrando una validez de contenido aún más sólida ($V \ge 0.70$). Se corrige el valor mínimo a 60.1. |
| **Tabla 5 y Sección 3.3.4 (Confiabilidad de los Instrumentos - pág. 43-45)** | La Tabla 5 contenía una columna informal de recordatorio (*«Qué necesitas: Piloto real con al menos quince participantes…»*). El texto presentaba una "muestra piloto simulada" con valores de Kappa estadísticamente imposibles para 6 focos ($4/6$ acuerdos arrojaban falsamente $\kappa > 0.80$, cuando el máximo teórico es 0.54). | Se elimina la columna informal de la Tabla 5. Se retira la muestra simulada y se redacta la especificación metodológica rigurosa de cómo se medirá la confiabilidad en campo: Alfa de Cronbach con ítems invertidos ($n=38$), Kappa ponderado interobservador ($15-30$ agrupaciones) y repetibilidad métrica intra-punto (8 puntos $\times$ 5 mediciones). |
| **Sección 3.5.2 (Recursos de Software para el Desarrollo - pág. 47-49)** | Citaba fragmentariamente Flutter, React, Node.js y versiones desactualizadas de SDKs. | Se actualiza la especificación técnica exacta: Kotlin 1.9+, Android SDK 34/36, Room 2.6+, LiteRT, Python 3.12, Django REST Framework 3.15, Neon PostgreSQL 16 con PostGIS y Cloudinary. |

---

# TEXTO DEFINITIVO PARA EL CAPÍTULO III DEL DOCUMENTO WORD

---

### 3.3.3 Validez de instrumentos

Para validar los instrumentos se empleó el método de **juicio de expertos**, el cual permite evaluar la pertinencia, claridad, objetividad y relevancia de los reactivos en relación con las variables y objetivos de la investigación. Tres especialistas con grado académico y amplia experiencia en desarrollo de software, sistemas de información y tecnologías digitales evaluaron de forma independiente los tres instrumentos propuestos (las fichas de validación firmadas constan en los Anexos 6, 7 y 8):

* **Mtro. Ing. Nick Eugeny Pumasupa Paro**: Especialista en Desarrollo de Software (Firma digital certificada).
* **Ing. Néstor Gutiérrez Huamán**: Ingeniero de Sistemas y Especialista en Tecnologías de Información (Firma colegiada).
* **Dr. Rodrigo Hernán Delgado Victorio**: Docente e Investigador del Laboratorio de Fabricación Digital FabLab Cusco (Visto Bueno institucional).

La Tabla 3 resume las valoraciones cuantitativas globales otorgadas por cada juez sobre una escala centesimal.

**Tabla 3**  
*Resultados cuantitativos de la validación por juicio de expertos*

| Experto Evaluador | Especialidad Académica | Instrumento 1 (Cuestionario SUS) | Instrumento 2 (Ficha de Campo) | Instrumento 3 (Ficha Geoespacial) |
|---|---|:---:|:---:|:---:|
| Mtro. Ing. Nick Pumasupa Paro | Desarrollo de Software | 90.0 % | 94.0 % | 90.5 % |
| Ing. Néstor Gutiérrez Huamán | Ingeniería de Sistemas | 93.2 % | 93.3 % | 93.5 % |
| Dr. Rodrigo Delgado Victorio | FabLab / Sistemas | 66.8 % | 60.1 % | 62.0 % |
| **Promedio Global** | — | **83.3 %** | **82.5 %** | **82.0 %** |

*Nota*. Elaboración propia a partir de las actas de validación de los Anexos 6, 7 y 8. Los tres expertos emitieron dictamen unánime de **«Aplicable»**.

La dispersión en las puntuaciones medias osciló entre 60.1 % y 94.0 % (con una calificación individual mínima de 50 % en criterios puntuales del tercer evaluador). Las observaciones cualitativas formuladas por los expertos fueron atendidas e integradas en los instrumentos finales antes de su despliegue operativo.

Para cuantificar el grado de consenso inter-jueces de manera formal, las puntuaciones porcentuales de cada uno de los 10 criterios normativos fueron recodificadas sobre la escala ordinal institucional de cinco categorías ($1 = \text{Deficiente [0-20\%]}$, $2 = \text{Regular [21-40\%]}$, $3 = \text{Bueno [41-60\%]}$, $4 = \text{Muy Bueno [61-80\%]}$, $5 = \text{Excelente [81-100\%]}$) y se calculó el **coeficiente V de Aiken** (Aiken, 1985):

$$V = \frac{S}{n \cdot (c - 1)}$$

Donde:
* $S = \sum (s_i - 1)$, siendo $s_i$ la calificación ordinal asignada por el juez $i$.
* $n$ es el número de jueces expertos evaluadores ($n = 3$).
* $c$ es el número de categorías de la escala de valoración ($c = 5$).

La Tabla 4 expone los coeficientes V de Aiken recalculados de forma rigurosa para cada instrumento.

**Tabla 4**  
*Coeficiente V de Aiken formal por instrumento de investigación*

| Instrumento Evaluado | V de Aiken Global | Criterio con menor acuerdo ($V_{\min}$) | Valoración Cualitativa | Decisión Metodológica |
|---|:---:|:---:|:---:|:---:|
| **Instrumento 1**: Cuestionario SUS (Anexo 10) | **0.892** | 0.833 (Actualidad, Consistencia, Coherencia) | Excelente validez de contenido | **Aplicable plenamente** ($V \ge 0.70$) |
| **Instrumento 2**: Ficha de Observación de Campo (Anexo 11) | **0.867** | 0.833 (Objetividad, Organización, Metodología) | Muy alta validez de contenido | **Aplicable plenamente** ($V \ge 0.70$) |
| **Instrumento 3**: Ficha de Verificación Geoespacial (Anexo 12) | **0.892** | 0.833 (Consistencia, Coherencia, Conveniencia) | Excelente validez de contenido | **Aplicable plenamente** ($V \ge 0.70$) |

*Nota*. Cálculo exacto reproducido a partir de las matrices de los Anexos 10, 11 y 12. En todos los casos se supera ampliamente el umbral psicométrico de aceptación ($V \ge 0.70$).

---

### 3.3.4 Confiabilidad de los instrumentos

Para garantizar la reproducibilidad y el rigor en la recolección de datos, la confiabilidad de los tres instrumentos se evalúa mediante estadísticos específicos adaptados a la naturaleza matemática de cada variable, conforme se detalla en la Tabla 5.

**Tabla 5**  
*Estadísticos previstos y diseño de evaluación de confiabilidad*

| Instrumento | Propiedad a evaluar | Estadístico principal | Diseño experimental y tamaño de muestra | Criterio de aceptación |
|---|---|---|---|:---:|
| **I1. Cuestionario SUS** (Anexo 3) | Consistencia interna de los 10 reactivos de usabilidad percibida | **Alfa de Cronbach ($\alpha$)** con IC 95% (fórmula de Feldt) | Muestra final de $n = 38$ usuarios que hayan utilizado el aplicativo en condiciones reales. Previa inversión de los 5 ítems negativos (pares: 2, 4, 6, 8 y 10). | $\alpha \ge 0.70$ |
| **I2. Ficha de Observación de Campo** (Anexo 4) | Concordancia interobservador ante los mismos focos | **Índice Kappa de Cohen ($\kappa$)** (ponderado en variables ordinales) | Doble observación independiente y ciega sobre la totalidad de agrupaciones detectadas por DBSCAN ($15 \le n \le 30$ focos). Cálculo para: confirmación del foco, composición y volumen. | $\kappa \ge 0.61$ *(Acuerdo sustancial)* |
| **I3. Ficha de Verificación Geoespacial** (Anexo 5) | Repetibilidad técnica del posicionamiento y sincronización | **Desviación Estándar intra-punto ($s_w$)** y tasa de concordancia del filtro | 8 puntos de control distribuidos territorialmente $\times$ 5 mediciones réplica en terminales físicos (40 observaciones). Contraste del error métrico (Haversine) frente a satélite. | Mediana $\le 10\text{ m}$; $P_{90} \le 15\text{ m}$; Sincronización $\ge 95\%$ |

*Nota*. Elaboración propia.

#### Protocolo de Evaluación por Instrumento

1. **Confiabilidad del Cuestionario SUS (Instrumento 1)**:
   El análisis de consistencia interna evalúa que los reactivos midan coherentemente el constructo de usabilidad. Se aplica la inversión numérica sobre la escala Likert ($X_{\text{invertido}} = 6 - X_{\text{original}}$) en los ítems 2, 4, 6, 8 y 10. Para la muestra dimensionada de $n = 38$ participantes, el coeficiente Alfa de Cronbach se reporta con su intervalo de confianza al 95 % mediante la distribución de Feldt y el Error Estándar de Medida ($SEM = s \cdot \sqrt{1 - \alpha}$). Se adopta el umbral de aceptación $\alpha \ge 0.70$.

2. **Confiabilidad de la Ficha de Observación de Campo (Instrumento 2)**:
   Dado que este instrumento involucra juicio humano en la vía pública, se evalúa la concordancia entre dos observadores independientes que evalúan de manera simultánea pero aislada los mismos focos detectados. Para la variable categórica nominal de composición y la confirmación de foco se computa el Kappa de Cohen simple; para el volumen estimado (Pequeño, Mediano, Grande) se aplica el Kappa ponderado cuadrático. Se adopta el criterio de Landis y Koch ($\kappa \ge 0.61$, acuerdo sustancial).

3. **Confiabilidad de la Ficha de Verificación Geoespacial (Instrumento 3)**:
   La precisión geoespacial no constituye una escala psicométrica aditiva, por lo que no corresponde calcular consistencia interna. Su confiabilidad técnica se evalúa a través de la **repetibilidad instrumental**: la dispersión de las lecturas repetidas sobre 8 puntos de control fijo se cuantifica mediante la desviación estándar intra-punto agrupada ($s_w = \sqrt{\frac{1}{k(m-1)} \sum \sum (x_{ij} - \bar{x}_i)^2}$), el Coeficiente de Correlación Intraclase $ICC(1,1)$ y la tasa de acierto del filtro de $15\text{ m}$ frente a la coordenada de referencia obtenida por ortofotografía satelital de alta resolución.

---

### 3.5.2 Recursos de software para el desarrollo

El ecosistema de software fue seleccionado para garantizar la robustez del procesamiento in situ (Edge computing), la tolerancia a fallos de red y la seguridad en el tratamiento de los datos:

* **Aplicativo Móvil Nativo (Cliente Android)**:
  * *Entorno de Desarrollo*: Android Studio (Ladybug / Koala Feature Drop).
  * *Lenguaje y Plataforma*: Kotlin 1.9.24 ejecutado sobre JDK 17 / 21.
  * *Parámetros del SDK*: Nivel mínimo de API 26 (`minSdk = 26`, Android 8.0 Oreo) garantizando cobertura del 95% del parque móvil distrital, y SDK objetivo `targetSdk = 36` (`compileSdk = 37`).
  * *Interfaz Declarativa*: Jetpack Compose con soporte de Material Design 3 y contenedores responsivos adaptables (`ContenidoCentrado` para tabletas y teléfonos).
  * *Persistencia Local y Asincronía*: SQLite embebido administrado mediante **Room 2.6.1**, corrutinas de Kotlin (`Dispatchers.IO`, `Dispatchers.Default`) y flujos reactivos inmutables (`StateFlow`).
  * *Sincronización en Segundo Plano*: **WorkManager 2.10+** configurado con fábrica de inyección de dependencias Hilt (`@HiltWorker`), con políticas de reintento exponencial y restricciones de red validada.
  * *Captura Fotográfica*: **CameraX 1.4+** con precalentamiento simultáneo del proveedor de ubicación `FusedLocationProviderClient`.
* **Motor de Inferencia de Visión Artificial**:
  * *Runtime en el dispositivo*: **Google LiteRT / TensorFlow Lite 2.16+** en arquitectura ARM64-v8a y armeabi-v7a.
  * *Modelo de Red Neuronal*: MobileNetV2 preentrenado y ajustado mediante transferencia de aprendizaje (*Transfer Learning*), optimizado y cuantizado a formato flatbuffer `.tflite` de punto flotante de 16 bits (FP16).
* **Servidor Backend y Procesamiento Analítico**:
  * *Lenguaje y Framework*: Python 3.12 y **Django 5.1 / Django REST Framework 3.15** alojado en infraestructura de contenedores en Render.
  * *Algoritmo de Agrupamiento*: Implementación de **DBSCAN** en memoria (`api/focos.py`) con cálculo de distancias geodésicas mediante la fórmula del semiverseno (Haversine) y radio $\varepsilon = 50\text{ m}$.
* **Infraestructura de Datos y Almacenamiento**:
  * *Base de Datos Transaccional*: **Neon PostgreSQL 16** con extensión espacial PostGIS 3.6 habilitada, operando bajo conexiones cifradas TLS obligatorias.
  * *Almacenamiento Multimedia Seguro*: **Cloudinary API v2** en modo privado autenticado (`authenticated`), sirviendo imágenes únicamente mediante firmas criptográficas HMAC de expiración temporal para la protección de la privacidad.
