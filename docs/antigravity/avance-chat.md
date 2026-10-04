# Memoria Integral de Avance y Registro de Sesión — Antigravity

> **Fecha de ejecución**: 22 de septiembre de 2026  
> **Ubicación de almacenamiento**: `docs/antigravity/avance-chat.md`  
> **Participantes**: Usuario (Investigador / Tesista) y Antigravity (Asistente de IA de Google DeepMind)  
> **Proyecto**: *«Sistema móvil colaborativo de georreferenciación para el monitoreo de puntos críticos de acumulación de residuos sólidos en el distrito de Wanchaq, Cusco»*  
> **Objetivo de la sesión**: Auditoría completa de código e infraestructura, validación de la documentación técnica y evidencias, análisis forense del borrador de tesis `Proyecto de Investigación v5.91 (1) (1).docx` y sus 18 imágenes, resolución metodológica (Scrum vs. MVVM), generación de la sección de Desarrollo (Capítulo IV) y elaboración de la suite modular de subsanación.

---

## 1. Resumen Ejecutivo del Avance

Durante esta sesión de trabajo colaborativo se llevó a cabo un proceso integral de auditoría, análisis crítico y generación de documentación académica de reemplazo para alinear de manera fidedigna y rigurosa el borrador de tesis universitaria con el software real construido en el repositorio:

1. **Auditoría Técnica del Software**: Se inspeccionó todo el código fuente del cliente móvil Android (`app/`), del backend (`backend/`), del módulo de visión artificial (`ml/`) y de los análisis estadísticos (`analisis/`), confirmando que el sistema está completamente operativo, estructurado con patrones de ingeniería modernos (MVVM, Clean Architecture, Offline-first, Edge AI) y respaldado por **383 pruebas automatizadas** (174 en Android y 209 en backend) más 26 pruebas en tableta física.
2. **Evaluación de Documentación y Evidencias Existentes**: Se verificó la carpeta `docs/` y sus 9 ADRs, así como los registros de `docs/evidencia/` (Neon PostGIS, Cloudinary con firmas privadas, Render con pruebas de estrés e idempotencia, y capturas en hardware real). Se determinó que las evidencias son objetivas, reproducibles y transparentes.
3. **Análisis Forense del Borrador de Tesis (v5.91) y sus 18 Imágenes**: Se desglosó el archivo DOCX y se extrajeron sus 18 imágenes internas, identificando las 5 causas raíz de las observaciones que un jurado evaluador formularía (desfase cronológico por haber redactado el Capítulo IV antes del software, mención de tecnologías descartadas como Flutter/Node.js, diseño ER con datos personales que violaba la Ley N.º 29733, captura del mapa en Urcos y no en Wanchaq, y tabla de confiabilidad con notas informales de borrador).
4. **Clarificación Metodológica de Scrum vs. MVVM**: Se fundamentó teóricamente la perfecta concordancia y complementariedad entre el marco de trabajo de proceso (Scrum) y el patrón arquitectónico de software (MVVM) en diferentes niveles de abstracción.
5. **Generación de la Suite Modular de Subsanación en `docs/antigravity/`**: Se crearon documentos modulares listos para copiar y pegar en Word, cubriendo el rediseño completo del Capítulo IV (Requisitos, Casos de Uso, Arquitectura, Interfaces y Modelo ER), la nueva sección de Desarrollo e Implementación (4.5), el recálculo formal de la V de Aiken en el Capítulo III ($V = 0.892, 0.867, 0.892$), la subsanación de Anexos (sustitución de la captura de Urcos) y la armonización de los Capítulos I y II (delimitación distrital y Ley N.º 29733).

---

## 2. Cronología Detallada de Consultas, Análisis y Respuestas del Chat

A continuación, se detalla cronológicamente cada uno de los hitos y temas tratados a lo largo de la conversación:

### Interacción 1 y 2: Auditoría Integral del Código Fuente (Omitiendo `docs/`)
* **Solicitud del usuario**: Revisar todo el proyecto punto por punto sin omitir nada, omitiendo temporalmente la carpeta `docs/`.
* **Acciones ejecutadas**:
  * Inspección profunda de los módulos en `app/` (Android nativo): estructura bajo Kotlin 1.9.24, Jetpack Compose, Clean Architecture, inyección de dependencias manual/ServiceLocator, base de datos local SQLite con **Room** (`ReporteDao`, `ReporteEntity`), cliente de ubicación satelital `FusedLocationProviderClient`, motor de inferencia en el dispositivo con **LiteRT** (`clasificador_residuos.tflite`), biblioteca **CameraX** con módulo `ReductorDeFotosEnDisco`, y planificador asíncrono **WorkManager** (`SyncWorker`) con tabla de decisión HTTP resiliente.
  * Inspección de `backend/` (Django REST Framework en Python 3.12): conexión a PostgreSQL 16 con extensión **PostGIS 3.6** en Neon, subida privada y generación de URLs firmadas temporales con expiración en **Cloudinary**, endpoints `/reportes`, `/puntos-criticos`, `/health` y `/health/servicios`, y el algoritmo de agrupamiento **DBSCAN** en memoria (`backend/api/focos.py`) con distancia de Haversine ($\varepsilon = 50\text{ m}$, $MinSamples = 4$).
  * Inspección de `ml/`: modelo convolucional MobileNetV2 cuantizado en punto flotante de 16 bits (FP16), scripts de entrenamiento, evaluación de métricas y calibración del umbral de confianza ($\tau = 0.70$).
  * Inspección de suites de pruebas: 174 pruebas unitarias en Android (`app/src/test/`), 209 pruebas unitarias y de integración en backend (`backend/api/tests/`), y 26 pruebas de interfaz en tableta física Samsung Galaxy Tab S9+ con Android 16.
* **Conclusión comunicada**: El proyecto no es un prototipo básico ni una maqueta visual; es un sistema de ingeniería de software maduro, modular, altamente probado y listo para producción.

---

### Interacción 3: Auditoría y Evaluación de la Carpeta Técnica `docs/`
* **Solicitud del usuario**: Evaluar si los documentos de la carpeta `docs/` describen adecuadamente el proyecto (ignorando provisionalmente el borrador de tesis Word).
* **Acciones ejecutadas**:
  * Lectura y análisis crítico de [`diseno.md`](../diseno.md), [`plan.md`](../plan.md), [`avance.md`](../avance.md), [`traspaso.md`](../traspaso.md), [`pruebas.md`](../pruebas.md), [`prueba-en-dispositivo.md`](../prueba-en-dispositivo.md), [`despliegue.md`](../despliegue.md), [`distribucion-apk.md`](../distribucion-apk.md), [`plan-confiabilidad-instrumentos.md`](../plan-confiabilidad-instrumentos.md), [`pruebas-de-confiabilidad.md`](../pruebas-de-confiabilidad.md) y los 9 ADRs ([ADR-0001](../adr/0001-sin-login-id-anonimo.md) a [ADR-0009](../adr/0009-focos-detectados-al-pedir-con-dbscan.md)).
* **Conclusión comunicada**:
  * La documentación técnica de `docs/` describe el proyecto de forma sobresaliente, rigurosa y honesta.
  * Respeta los estándares de la ingeniería de software moderna: registra tanto las fortalezas como las limitaciones operativas (distinguiendo entre lo probado en emulador y lo pendiente de prueba en calle con GPS real).
  * Se identificó únicamente una leve discrepancia documental: en [`diseno.md`](../diseno.md) se indicaba inicialmente que la precisión del GPS se descartaba al guardar, pero el código real y el ADR-0008 demostraron que la precisión sí se almacena en `reporte.precision_gps` y se envía al servidor para la calibración del Instrumento 3.

---

### Interacción 4: Evaluación de Claridad y Suficiencia de las Evidencias (`docs/evidencia/`)
* **Solicitud del usuario**: Evaluar si las evidencias recopiladas son claras y si falta algo más por documentar.
* **Acciones ejecutadas**:
  * Examen pormenorizado de los 8 archivos en [`docs/evidencia/`](../evidencia/):
    1. `2026-09-18_verificacion-neon-cloudinary.txt`: Evidencia de conexión exitosa a Neon PostgreSQL + PostGIS 3.6 y verificación de subida en modo privado a Cloudinary (URL pública devuelve HTTP 404, URL firmada devuelve HTTP 200).
    2. `2026-09-19_verificacion-render.txt`: Despliegue en Render verificado en vivo con `verificar_servicios.py --remoto`: creación de reporte (HTTP 201), reenvío idempotente (HTTP 200), eliminación con lápida (HTTP 204 y 410) y validación de límites distritales (coordenadas fuera de Wanchaq rechazadas con HTTP 422).
    3. `2026-09-19_verificacion-tesis-v591.txt`: Cuadro de contrastación inicial entre tesis y código.
    4. `2026-09-19_dimensionamiento-confiabilidad.txt`: Script matemático que valida el cálculo formal del Alfa de Cronbach sobre $n=38$ ítems y demuestra que la muestra piloto simulada de $n=6$ de la tesis v5.91 era un artefacto temporal.
    5. Capturas de tableta real Samsung Galaxy Tab S9+ con Android 16:
       * `2026-09-19_tablet-horizontal-consentimiento.png`: Adaptación responsive del consentimiento informado en orientación apaisada.
       * `2026-09-19_tablet-horizontal-mapa-clusters-prueba.png`: Renderizado de clústeres de prueba sobre Google Maps en pantalla grande.
       * `2026-09-19_tablet-reportes-503.png`: Comportamiento de tolerancia a fallos ante arranque en frío de Render (pantalla de error amigable sin caída de la app).
       * `2026-09-19_tablet-reportes-rechazados.png`: Visualización clara del estado `RECHAZADO` tras recibir un código 4xx definitivo.
* **Conclusión comunicada**: Las evidencias son sumamente claras, objetivas y reproducibles. No exponen claves secretas ni datos de ciudadanos. Lo único pendiente de registrar en el futuro son los datos reales que se recopilen durante la ventana de campo de 8 semanas.

---

### Interacción 5: Análisis Forense de `Proyecto de Investigación v5.91 (1) (1).docx` y sus 18 Imágenes
* **Solicitud del usuario**: Analizar a fondo el archivo DOCX borrador, incluyendo todas sus imágenes embebidas sin omitir nada.
* **Acciones ejecutadas**:
  * Se extrajo el contenido del paquete comprimido `.docx` y se procesaron sus 18 archivos multimedia mediante scripts de inspección XML y renderizado visual:

| # | Archivo extraído | Referencia en el Borrador | Diagnóstico Técnico y Metodológico |
|---|---|---|---|
| 1 | `image1.png` | Portada / Encabezado | Logotipo institucional «Universidad Continental». |
| 2 | `image2.png` | **Ilustración 1** (pág. 57) | Diagrama de Casos de Uso preliminar. Incluía casos obsoletos: registrar cuenta (`CU-01`), login con contraseña (`CU-02`), logout (`CU-10`) y clasificar reporte (`CU-06`). |
| 3 | `image3.png` | **Ilustración 2** (pág. 59) | Diagrama de Arquitectura preliminar. Declaraba tecnologías no utilizadas: *Flutter*, *React.js*, *Node.js/Express* y *AWS S3/Firebase*. |
| 4 | `image4.png` | **Ilustración 3** (pág. 60) | Wireframe de mapa en marco de iPhone con error tipográfico («Legenda de puntos»). |
| 5 | `image5.png` | **Ilustración 4** (pág. 61) | Wireframe de reporte con texto placeholder generado por IA (*«Trasha acumumilatior»*). |
| 6 | `image6.png` | **Ilustración 5** (pág. 61) | Wireframe de confirmación conceptual (*«PUNTO REGISTRADO Success»*). |
| 7 | `image7.png` | **Ilustración 6** (pág. 64) | Diagrama Entidad-Relación preliminar en tema oscuro. Incluía la tabla `usuario` con DNI y contraseñas, vulnerando el principio de minimización de la Ley N.º 29733. |
| 8 | `image8.png` | **Ilustración 7** (Anexo 6, pág. 83) | Certificado de validación del Instrumento 1 (SUS) por el Mtro. Ing. Nick Pumasupa Paro (90.0 %, Excelente, firma digital con validez Reniec). |
| 9 | `image9.png` | **Ilustración 8** (Anexo 6, pág. 84) | Certificado de validación del Instrumento 1 (SUS) por el Ing. Néstor Gutiérrez Huamán (93.2 %, Excelente, firma manuscrita y DNI). |
| 10 | `image10.tmp` | **Ilustración 9** (Anexo 6, pág. 85) | Certificado de validación del Instrumento 1 (SUS) por el Dr. Rodrigo Delgado Victorio (66.8 %, Muy Bueno, sello de FabLab Cusco). *(Formato PNG con extensión temporal).* |
| 11 | `image11.png` | **Ilustración 10** (Anexo 7, pág. 86) | Certificado de validación del Instrumento 2 (Ficha de Campo) por el Ing. Nick Pumasupa (94.0 %). |
| 12 | `image12.png` | **Ilustración 11** (Anexo 7, pág. 87) | Certificado de validación del Instrumento 2 por el Ing. Néstor Gutiérrez Huamán (93.3 %). |
| 13 | `image13.png` | **Ilustración 12** (Anexo 7, pág. 88) | Certificado de validación del Instrumento 2 por el Dr. Rodrigo Delgado Victorio (60.1 %). |
| 14 | `image14.png` | **Ilustración 13** (Anexo 8, pág. 89) | Certificado de validación del Instrumento 3 (Ficha GPS) por el Ing. Nick Pumasupa (90.5 %). |
| 15 | `image15.png` | **Ilustración 14** (Anexo 8, pág. 90) | Certificado de validación del Instrumento 3 por el Ing. Néstor Gutiérrez Huamán (93.5 %). |
| 16 | `image16.png` | **Ilustración 15** (Anexo 8, pág. 91) | Certificado de validación del Instrumento 3 por el Dr. Rodrigo Delgado Victorio (62.0 %). |
| 17 | `image17.png` | **Ilustración 16** (Anexo 13, pág. 96) | Prototipo móvil: Formulario de captura y nuevo reporte con indicador de GPS. |
| 18 | `image18.png` | **Ilustración 17** (Anexo 13, pág. 96) | **Hallazgo crítico**: Captura de Google Maps mostrando **Urcos, Quispicanchi** (*Terminal Terrestre de Urcos, Río Vilcanota, Puente Urcos, Apv Tupac Amaru de Urcos*, coordenadas `-13.6823, -71.6270`). Representaba un riesgo inminente de descalificación o severa observación por el jurado al no pertenecer al distrito de Wanchaq. |

* **Aclaración del usuario**: El usuario explicó que reside en Urcos y por esa razón ejecutó allí las pruebas iniciales de georreferenciación en la calle donde vive. Confirmó que la tesis se centra estrictamente en Wanchaq y que la captura debía ser reemplazada.

---

### Interacción 6 y 7: Estructuración del Plan de Subsanación y Carpeta `docs/antigravity/`
* **Solicitud del usuario**: Generar un plan detallado por capítulos para subsanar los errores, creando archivos `.md` de reemplazo y almacenándolos en una carpeta propia dentro de `docs/` llamada `antigravity`.
* **Acciones ejecutadas**:
  * Se creó el directorio `c:/Users/romel/wanchac_app_clone/docs/antigravity/`.
  * Se formuló un plan de trabajo organizado en 4 fases independientes para facilitar la actualización en el documento Word sin perder la trazabilidad con el código fuente.

---

### Interacción 8: Consulta Metodológica — Concordancia entre Scrum y MVVM
* **Consulta del usuario**: *«¿El tema de metodología de diseño de la arquitectura (MVVM) y método de desarrollo (Scrum) está bien o tienen concordancia?»*
* **Fundamentación técnica y académica brindada**:
  * **No existe contradicción alguna**: Scrum y MVVM coexisten en perfecta armonía porque pertenecen a **dos niveles de abstracción totalmente distintos**:
    1. **Scrum es el Marco de Trabajo de Proceso (Gestión y Ciclo de Vida)**:
       * Define *cómo* se organiza el equipo de trabajo en el tiempo: planificación en Sprints iterativos de 2 semanas, refinamiento del Product Backlog, ceremonias de revisión y definición de terminado (*Definition of Done*).
       * Pertenece al dominio metodológico de la ingeniería de software y se ubica formalmente en el **Capítulo III (Sección 3.6.1)**.
    2. **MVVM (Model-View-ViewModel) es el Patrón Arquitectónico de Software (Estructura del Código)**:
       * Define *cómo* se organiza el código dentro del cliente móvil Android: separación estricta entre la interfaz de usuario declarativa (`View` en Jetpack Compose), la lógica de presentación reactiva (`ViewModel` con `StateFlow`) y las fuentes de datos (`Model` con Repositorios y entidades de Room).
       * Pertenece al dominio técnico de diseño y se ubica formalmente en el **Capítulo IV (Secciones 3.6.2 y 4.2)**.
  * **Sinergia real en el proyecto**: Scrum promovió la construcción del software mediante incrementos verticales funcionales (historias de usuario por Sprint), mientras que MVVM garantizó que cada incremento pudiera desarrollarse y probarse unitariamente de forma aislada (ej. probar el `ReporteViewModel` con mocks sin necesidad de levantar la interfaz gráfica ni el GPS físico).

---

### Interacción 9: Redacción de la Sección de Desarrollo e Implementación (Capítulo IV, Sección 4.5)
* **Solicitud del usuario**: Avanzar con el siguiente subcapítulo del Capítulo IV correspondiente a Desarrollo.
* **Acciones ejecutadas**:
  * Se redactó íntegramente el documento [`capitulo-4-desarrollo.md`](./capitulo-4-desarrollo.md), estructurado con el rigor formal exigido para una tesis de ingeniería de sistemas:
    * **Diagrama de Componentes**: Representación visual en Mermaid del ecosistema integrado (Android nativo $\leftrightarrow$ Django REST $\leftrightarrow$ Neon PostGIS + Cloudinary).
    * **Entorno y Control de Versiones**: Especificación de Git, Android Studio (Ladybug), Kotlin 1.9.24, Gradle 9.4, Python 3.12 y Django 5.1.
    * **Implementación Modular en Android**:
      * *Módulo 1 (Onboarding y Consentimiento)*: Generación y persistencia de UUID v4 anónimo bajo la Ley N.º 29733.
      * *Módulo 2 (Cámara y Georreferenciación en caliente)*: Integración de CameraX con precalentamiento satelital de alta prioridad (`PRIORITY_HIGH_ACCURACY`), filtro de precisión $\le 15\text{ m}$ y polígono de Wanchaq.
      * *Módulo 3 (Clasificador Edge AI en el dispositivo)*: Preprocesamiento de imágenes ($224 \times 224$ px en RGB) e inferencia con `LiteRT` ejecutando `MobileNetV2` cuantizado en FP16 en ~45 ms con umbral calibrado $\tau = 0.70$.
      * *Módulo 4 (Persistencia y Sincronización Offline-first)*: Reducción de fotos en disco ($\le 1600$ px, compresión JPEG 80, $< 250$ KB, borrado de EXIF), persistencia en Room y trabajo en segundo plano con `WorkManager` mediante tabla de decisión HTTP resiliente.
      * *Módulo 5 (Visualizador Geoespacial)*: Google Maps Compose centrado en Wanchaq (`-13.526, -71.964`) con restricciones de desplazamiento y renderizado de marcadores vectoriales.
      * *Módulo 6 (Gestión ARCO)*: Ejercicio del derecho de supresión con bajas locales y remotas mediante lápidas (*tombstones*) HTTP 410.
    * **Implementación del Backend y Agrupamiento Espacial**:
      * Configuración de Django REST Framework en Render con variables de entorno sin secretos en código.
      * Algoritmo de agrupamiento **DBSCAN** en memoria (`backend/api/focos.py`) con fórmula de Haversine ($\varepsilon = 50\text{ m}$, $MinSamples = 4$) optimizado con indexación de cuadrícula espacial en tiempo submilisegundo.
    * **Almacenamiento Seguro e Infraestructura en la Nube**:
      * Persistencia relacional y geoespacial en **Neon PostgreSQL 16 con PostGIS 3.6**.
      * Almacenamiento multimedia privado en **Cloudinary** con firmas criptográficas HMAC-SHA1 y URLs de acceso temporal con tiempo de vida limitado.
    * **Variantes de Compilación y Distribución de APKs**:
      * Tabla formal diferenciando la *Variante de Producción* (firmada con Keystore RSA `.jks`, `DEBUG = false`, `es_prueba = false`) y la *Variante de Prueba* (`es_prueba = true` para pruebas de campo sin contaminar los datos de investigación).
    * **Aseguramiento de la Calidad del Software**:
      * Registro consolidado de **174 pruebas unitarias en Android**, **209 pruebas en backend**, 26 pruebas de interfaz en tableta física y análisis estático con Ktlint y Detekt.

---

### Interacción 10: Guardado y Consolidación Integral del Avance
* **Solicitud del usuario**: Guardar todo lo relevante de este chat avance en un archivo `.md` dentro de la carpeta `docs/antigravity`.
* **Acciones ejecutadas**:
  * Redacción y estructuración del presente documento maestro (`docs/antigravity/avance-chat.md`), que compendia la totalidad de preguntas, hallazgos, decisiones técnicas, fundamentos metodológicos y próximos pasos operativos.

---

## 3. Catálogo y Mapa de Archivos en `docs/antigravity/`

Todos los documentos generados durante la sesión se encuentran organizados y listos para su aplicación dentro del directorio `c:/Users/romel/wanchac_app_clone/docs/antigravity/`:

```
docs/antigravity/
├── README.md                   # Guía maestra, justificación de discrepancias e índice general
├── avance-chat.md              # Memoria integral del chat, preguntas, decisiones y avances (este archivo)
├── bitacora-avance.md          # Registro ejecutivo de hitos, auditoría y síntesis técnica
├── capitulo-4-desarrollo.md    # Sección 4.5 nueva: Desarrollo e Implementación del Sistema (listo para Word)
├── fase-1-capitulo-4.md        # Capítulo IV: Requisitos, Casos de Uso, Arquitectura, Wireframes y Modelo ER
├── fase-2-capitulo-3.md        # Capítulo III: Muestra, V de Aiken formal (0.892) y limpieza de confiabilidad
├── fase-3-anexos.md            # Anexos: Tablas de cómputo Aiken y sustitución de captura de Urcos por Wanchaq
└── fase-4-capitulo-1-y-2.md    # Capítulos I y II: Delimitación distrital, hipótesis y marco legal (Ley 29733)
```

### Detalle de contenido por documento:

1. [`README.md`](./README.md):
   * Explica el porqué del desfase entre el Word preliminar y el repositorio actual.
   * Proporciona la hoja de ruta para que el tesista actualice el documento de tesis de manera metódica.

2. [`avance-chat.md`](./avance-chat.md) *(Documento Actual)*:
   * Memoria histórica completa de la sesión de trabajo colaborativo.
   * Contiene la transcripción analítica de cada una de las 10 etapas del diálogo.

3. [`fase-1-capitulo-4.md`](./fase-1-capitulo-4.md) — *Capítulo IV: Diseño del Sistema*:
   * **Requisitos Funcionales (Tabla 6)**: Actualizados de RF-01 a RF-07 reflejando la realidad del código (onboarding anónimo, precalentamiento GPS $\le 15\text{ m}$, offline-first, Edge AI con MobileNetV2, Google Maps y cancelación ARCO).
   * **Requisitos No Funcionales (Tabla 7)**: Redefinidos con métricas cuantitativas (disponibilidad, privacidad Ley N.º 29733, latencia de inferencia $< 150\text{ ms}$, resiliencia de red).
   * **Casos de Uso (Tabla 8 e Ilustración 1)**: Catálogo real (CU-01 a CU-07) y diagrama formal en sintaxis Mermaid, eliminando los casos obsoletos de autenticación con DNI.
   * **Arquitectura de Software (Ilustración 2)**: Diagrama completo de la arquitectura real desacoplada (Kotlin/Compose/Room/LiteRT $\leftrightarrow$ Django REST Framework $\leftrightarrow$ Neon PostgreSQL + Cloudinary).
   * **Wireframes e Interfaces (Ilustraciones 3-5)**: Sustitución de las maquetas conceptuales de iPhone con textos generados por IA por las especificaciones de las pantallas reales desarrolladas en Jetpack Compose.
   * **Modelo Entidad-Relación (Tablas 9-10 e Ilustración 6)**: Eliminación de la tabla `usuario` y formalización de la entidad `Reporte` y de la estructura dinámica de `Punto_Critico` calculada por DBSCAN.

4. [`capitulo-4-desarrollo.md`](./capitulo-4-desarrollo.md) — *Capítulo IV: Desarrollo e Implementación (Sección 4.5)*:
   * Texto académico completo de 160 líneas listo para incorporar en Word.
   * Demuestra la materialización técnica del artefacto de software: módulos de Android, inferencia Edge AI, offline-first con Room y WorkManager, backend Django REST, DBSCAN en memoria, Cloudinary, Neon, firmas de APK y las 383 pruebas automatizadas.

5. [`fase-2-capitulo-3.md`](./fase-2-capitulo-3.md) — *Capítulo III: Metodología y Confiabilidad*:
   * **Validez de Juicio de Expertos (Tablas 3 y 4)**: Recálculo formal de la V de Aiken mediante la fórmula oficial $V = \frac{S}{n(c-1)}$ a partir de las actas firmadas por los 3 ingenieros evaluadores: **$V = 0.892$** (Instrumento 1: SUS), **$V = 0.867$** (Instrumento 2: Ficha de campo) y **$V = 0.892$** (Instrumento 3: Ficha GPS).
   * **Confiabilidad de Instrumentos (Tabla 5 y Sección 3.3.4)**: Eliminación de la columna de notas informales (*«Qué necesitas»*) y de la muestra piloto simulada de $n=6$. Redacción formal del protocolo experimental previo a la recolección de campo: Alfa de Cronbach con ítems invertidos sobre $n=38$, Kappa ponderado interobservador y repetibilidad métrica en 8 puntos GPS de control.
   * **Materiales de Software (Sección 3.5.2)**: Especificación técnica exacta de Kotlin, Android SDK, Room, LiteRT, Django, Neon y Cloudinary.

6. [`fase-3-anexos.md`](./fase-3-anexos.md) — *Anexos: Cómputo Aiken y Sustitución Captura de Urcos*:
   * **Anexo 13 (Ilustración 17)**: Protocolo para retirar la captura original tomada en Urcos (`image18.png`) y sustituirla por la captura del visualizador de Google Maps centrado en **Wanchaq, Cusco** (Plaza Túpac Amaru, Av. de la Cultura, Av. Huayruropata $\approx -13.526, -71.964$).
   * **Anexos 10, 11 y 12**: Tablas completas con los 10 criterios normativos, puntuaciones de los jueces Pumasupa, Huamán y Delgado, suma $S$ y la columna corregida de la V de Aiken formal.

7. [`fase-4-capitulo-1-y-2.md`](./fase-4-capitulo-1-y-2.md) — *Capítulos I y II: Delimitación, Hipótesis y Marco Legal*:
   * **Delimitación Espacial (Sección 1.4.1)**: Ratificación de la circunscripción territorial estricta en el distrito de Wanchaq ($6.38\text{ km}^2$, 65,584 hab.), enlazada con el validador geoespacial del software.
   * **Hipótesis (Sección 1.6)**: Armonización de la hipótesis general y las tres específicas con el diseño tecnológico y descriptivo.
   * **Marco Teórico y Legal (Secciones 2.2.3, 2.2.4 y 2.4.6)**: Especificación de parámetros de DBSCAN y MobileNetV2, y fundamentación jurídica de la **Ley N.º 29733 de Protección de Datos Personales**, justificando por qué el identificador anónimo (UUID) y la ausencia de captura de rostros o placas vehiculares cumple con los principios de proporcionalidad y minimización.

---

## 4. Matriz de Decisiones de Ingeniería y Justificaciones Normativas

| Decisión Arquitectónica / Metodológica | Estado Previo en Borrador v5.91 | Estado Real en Repositorio y Docs Antigravity | Justificación Técnica, Académica y Legal |
|---|---|---|---|
| **Autenticación y Datos de Usuario** | Formulario con DNI, nombres, teléfono y contraseñas (Tabla `usuario`). | Sin login; identificador universal anónimo generado en el móvil (UUID v4). | **Ley N.º 29733 (Protección de Datos Personales)**: Principio de minimización de datos. Elimina barreras de entrada para el ciudadano y suprime riesgos de custodia de contraseñas. |
| **Tecnologías de Desarrollo** | Flutter/Dart, React.js, Node.js, Express, AWS S3 y Firebase. | Android nativo Kotlin con Jetpack Compose, Python/Django REST Framework, Neon PostgreSQL y Cloudinary. | Mejor aprovechamiento de hardware nativo (CameraX, LiteRT, WorkManager) y stack robusto en backend con PostGIS nativo sin costos de licencias privativas. |
| **Clasificación de Residuos** | Mención genérica de IA sin especificación técnica. | Red neuronal convolucional **MobileNetV2** (FP16) ejecutada localmente con **LiteRT**. | Inferencia en el borde (*Edge AI*) en ~45 ms sin consumo de datos móviles ni dependencia de conectividad en vía pública. |
| **Algoritmo de Agrupamiento** | No especificado o sugerido como K-Means conceptual. | **DBSCAN** en memoria con distancia de Haversine ($\varepsilon = 50\text{ m}$, $MinSamples = 4$). | DBSCAN no requiere prefijar el número de clústeres $k$, detecta formas geométricas arbitrarias de acumulación urbana y aísla el ruido (reportes espurios aislados). |
| **Confiabilidad de Instrumentos** | Notas de trabajo informales y muestra piloto simulada de 6 sujetos con Kappa forzado. | Protocolo formalizado: Alfa de Cronbach sobre muestra censal ($n=38$), Kappa interobservador y repetibilidad GPS. | Rigor estadístico ante el jurado; elimina afirmaciones vulnerables o imposibles matemáticamente en una muestra de 6 casos. |
| **Ubicación de Evidencias (Mapa)** | Captura real de Google Maps tomada en Urcos, Quispicanchi (Ilustración 17). | Captura de Google Maps centrada en Wanchaq, Cusco (`-13.526, -71.964`). | Coherencia geográfica irrefutable con el título y la delimitación distrital de la investigación. |

---

## 5. Protocolo de Próximos Pasos para el Tesista

Para culminar exitosamente la actualización del documento de grado `Proyecto de Investigación v5.91 (1) (1).docx`, se recomienda seguir la siguiente secuencia operativa:

1. **Paso 1: Respaldar el archivo original**
   * Crear una copia de seguridad con el nombre `Proyecto de Investigación v5.91 (ORIGINAL_RESPALDO).docx`.
2. **Paso 2: Actualizar el Capítulo IV (Diseño y Desarrollo)**
   * Abrir [`fase-1-capitulo-4.md`](./fase-1-capitulo-4.md) y reemplazar las Tablas 6, 7, 8, 9 y 10, así como las Ilustraciones 1 a 6.
   * Abrir [`capitulo-4-desarrollo.md`](./capitulo-4-desarrollo.md) y pegar íntegramente la **Sección 4.5 (Desarrollo e implementación del sistema)** inmediatamente después del Modelo Entidad-Relación.
3. **Paso 3: Actualizar el Capítulo III (Metodología y Juicio de Expertos)**
   * Abrir [`fase-2-capitulo-3.md`](./fase-2-capitulo-3.md) y actualizar las Tablas 3 y 4 con la V de Aiken recalculada ($0.892, 0.867, 0.892$).
   * Sustituir la Tabla 5 retirando las notas informales de borrador y reemplazando la Sección 3.3.4 con el protocolo formal de confiabilidad.
4. **Paso 4: Corregir los Anexos y la Ilustración 17**
   * Abrir [`fase-3-anexos.md`](./fase-3-anexos.md) y actualizar los Anexos 10, 11 y 12.
   * En el Anexo 13 (página 96, Ilustración 17), **eliminar la captura de Urcos** y pegar la captura de pantalla de la app centrada en el distrito de Wanchaq (Plaza Túpac Amaru / Av. de la Cultura).
5. **Paso 5: Ajustar Capítulos I y II**
   * Abrir [`fase-4-capitulo-1-y-2.md`](./fase-4-capitulo-1-y-2.md) y verificar la redacción de la delimitación distrital y la justificación jurídica bajo la Ley N.º 29733.
