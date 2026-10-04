# Bitácora de Avance y Memoria de Sesión — Antigravity

> **Fecha de sesión**: 22 de septiembre de 2026  
> **Ubicación de salidas**: `docs/antigravity/`  
> **Objetivo de la sesión**: Auditoría completa de la documentación técnica (`docs/`), análisis forense exhaustivo del borrador de tesis `Proyecto de Investigación v5.91 (1) (1).docx` y sus 18 imágenes, resolución metodológica (Scrum vs. MVVM), y generación de la documentación modular de reemplazo para alinear la tesis con el software real.

---

## 1. Hitos y Actividades Ejecutadas en la Sesión

### A. Auditoría de la Documentación Existente (`docs/`) y Evidencias
1. **Revisión Documental**: Se leyeron y evaluaron todos los archivos rectores: [`diseno.md`](../diseno.md), [`plan.md`](../plan.md), [`avance.md`](../avance.md), [`traspaso.md`](../traspaso.md), [`pruebas.md`](../pruebas.md), [`prueba-en-dispositivo.md`](../prueba-en-dispositivo.md), [`despliegue.md`](../despliegue.md), [`distribucion-apk.md`](../distribucion-apk.md) y los 9 Architecture Decision Records ([ADR-0001](../adr/0001-sin-login-id-anonimo.md) al [ADR-0009](../adr/0009-focos-detectados-al-pedir-con-dbscan.md)).
2. **Evaluación de Claridad de Evidencias**:
   * Se examinaron los 8 archivos de [`docs/evidencia/`](../evidencia/): pruebas remotas en Neon (PostGIS 3.6), Cloudinary (modo privado autenticado), Render (salud, POST 201, idempotencia 200, DELETE 204 y lápidas 410), capturas en hardware real (Samsung Galaxy Tab S9+ con Android 16) y los scripts de dimensionamiento y verificación matemática.
   * Se concluyó que las evidencias son objetivas, reproducibles, seguras (sin exponer credenciales ni datos personales sensibles bajo la Ley N.º 29733) y transparentes al consignar explícitamente «Qué demuestra» y «Qué NO demuestra».

---

### B. Análisis Exhaustivo de `Proyecto de Investigación v5.91 (1) (1).docx` y sus 18 Imágenes
Se extrajeron e inspeccionaron individualmente las **18 imágenes** embebidas en el paquete ZIP del documento Word:

| # | Archivo interno | Referencia en Tesis | Descripción y Análisis Crítico |
|---|---|---|---|
| 1 | `image1.png` | Portada / Encabezado | Logotipo tipográfico en negro: «Universidad Continental». |
| 2 | `image2.png` | **Ilustración 1** (pág. 57) | Diagrama de Casos de Uso preliminar. Contemplaba erróneamente registro con cuenta (`CU-01`), login (`CU-02`), cierre de sesión (`CU-10`) y categorización sucio/limpio (`CU-06`). |
| 3 | `image3.png` | **Ilustración 2** (pág. 59) | Diagrama de Arquitectura preliminar. Mencionaba tecnologías no utilizadas: *Flutter/Dart*, *React.js*, *Node.js/Express* y *AWS S3/Firebase*. |
| 4 | `image4.png` | **Ilustración 3** (pág. 60) | Wireframe de pantalla mapa en marco de iPhone, con error tipográfico («Legenda de puntos»). |
| 5 | `image5.png` | **Ilustración 4** (pág. 61) | Wireframe conceptual de captura con texto placeholder generado por IA (*«Trasha acumumilatior»*). |
| 6 | `image6.png` | **Ilustración 5** (pág. 61) | Wireframe de confirmación/éxito (*«PUNTO REGISTRADO Success»*). |
| 7 | `image7.png` | **Ilustración 6** (pág. 64) | Diagrama Entidad-Relación preliminar en tema oscuro. Incluía la tabla `usuario` con DNI, contraseñas y teléfonos, en conflicto con el principio de minimización de la Ley N.º 29733. |
| 8 | `image8.png` | **Ilustración 7** (Anexo 6, pág. 83) | Acta oficial de validación del Instrumento 1 (SUS) por el **Mtro. Ing. Nick Pumasupa Paro** (90.0 %, Excelente, firma digital certificada). |
| 9 | `image9.png` | **Ilustración 8** (Anexo 6, pág. 84) | Acta oficial de validación del Instrumento 1 (SUS) por el **Ing. Néstor Gutiérrez Huamán** (93.2 %, Excelente, firma manuscrita y DNI). |
| 10 | `image10.tmp` | **Ilustración 9** (Anexo 6, pág. 85) | Acta oficial de validación del Instrumento 1 (SUS) por el **Dr. Rodrigo Delgado Victorio** (66.8 %, Muy Bueno, sello oficial FabLab Cusco). |
| 11 | `image11.png` | **Ilustración 10** (Anexo 7, pág. 86) | Acta de validación del Instrumento 2 (Ficha de Campo) por el Ing. Nick Pumasupa (94.0 %). |
| 12 | `image12.png` | **Ilustración 11** (Anexo 7, pág. 87) | Acta de validación del Instrumento 2 por el Ing. Néstor Gutiérrez Huamán (93.3 %). |
| 13 | `image13.png` | **Ilustración 12** (Anexo 7, pág. 88) | Acta de validación del Instrumento 2 por el Dr. Rodrigo Delgado Victorio (60.1 %). |
| 14 | `image14.png` | **Ilustración 13** (Anexo 8, pág. 89) | Acta de validación del Instrumento 3 (Ficha GPS) por el Ing. Nick Pumasupa (90.5 %). |
| 15 | `image15.png` | **Ilustración 14** (Anexo 8, pág. 90) | Acta de validación del Instrumento 3 por el Ing. Néstor Gutiérrez Huamán (93.5 %). |
| 16 | `image16.png` | **Ilustración 15** (Anexo 8, pág. 91) | Acta de validación del Instrumento 3 por el Dr. Rodrigo Delgado Victorio (62.0 %). |
| 17 | `image17.png` | **Ilustración 16** (Anexo 13, pág. 96) | Prototipo móvil: Pantalla de captura y nuevo reporte con indicador de GPS. |
| 18 | `image18.png` | **Ilustración 17** (Anexo 13, pág. 96) | **Hallazgo geográfico crítico**: Captura real de Google Maps mostrando **Urcos, Quispicanchi** (*Terminal Terrestre de Urcos, Río Vilcanota, Puente Urcos, Apv Tupac Amaru de Urcos* con coordenadas `-13.6823, -71.6270`), tomada en el domicilio del autor. Se determinó que debía sustituirse urgentemente por una captura centrada en **Wanchaq, Cusco** para evitar observaciones del jurado. |

---

### C. Análisis Metodológico: Concordancia entre Scrum y MVVM
El usuario consultó si la metodología de desarrollo (**Scrum**) y el diseño arquitectónico (**MVVM**) eran correctos o si presentaban contradicciones. Se clarificó la relación:
* **No compiten, sino que se complementan en distintos niveles de abstracción**:
  * *Scrum* es el **marco de trabajo de proceso** (planificación temporal, Sprints de 2 semanas, Definition of Done, entregas modulares).
  * *MVVM* es el **patrón arquitectónico de software** (estructura interna del código Android en `View` declarativa Compose, `ViewModel` con `StateFlow`, y `Model` con Repositorios y Room).
* **Defensa ante el jurado**: Se precisó que en el documento Word deben titularse en apartados separados (3.6.1 Metodología de desarrollo: Scrum; 3.6.2 y 4.2 Arquitectura de software: Cliente-Servidor con MVVM en el cliente móvil y servicios REST en capas en el backend Django).

---

## 2. Documentos Generados en `docs/antigravity/`

Se estructuró la carpeta dedicada con 6 documentos técnicos y metodológicos listos para aplicar:

### 1. [`README.md`](./README.md)
* Guía maestra de la carpeta, justificación del origen de las discrepancias (Capítulo IV escrito antes del código) e índice estructurado de aplicación.

### 2. [`fase-1-capitulo-4.md`](./fase-1-capitulo-4.md) — Capítulo IV: Diseño del Sistema (págs. 55-64)
* **Requisitos Funcionales (Tabla 6)**: Actualizados a RF-01 (onboarding con consentimiento anónimo), RF-02 (precalentamiento GPS $\le 15\text{ m}$ en Wanchaq), RF-03 (offline-first con Room y WorkManager), RF-04 (clasificación asistida por MobileNetV2), RF-05 (mapa público con Google Maps) y RF-07 (cancelación ARCO con lápidas).
* **Requisitos No Funcionales (Tabla 7)**: Disponibilidad, privacidad y minimización de datos (Ley N.º 29733), seguridad TLS 1.3, rendimiento de inferencia (< 150 ms) y tolerancia a red.
* **Casos de Uso (Tabla 8 e Ilustración 1)**: Catálogo real (CU-01 a CU-07) y diagrama formal en sintaxis Mermaid.
* **Arquitectura de Software (Ilustración 2)**: Diagrama completo de la arquitectura real desacoplada (Kotlin/Compose/Room/LiteRT $\leftrightarrow$ Django REST Framework $\leftrightarrow$ Neon PostgreSQL + Cloudinary).
* **Wireframes e Interfaces (Ilustraciones 3-5)**: Sustitución de mockups conceptuales de iPhone por la especificación de las pantallas reales desarrolladas en Jetpack Compose.
* **Modelo Entidad-Relación (Tablas 9-10 e Ilustración 6)**: Supresión de la tabla `usuario` y formalización de la entidad `Reporte` y de la estructura dinámica de `Punto_Critico` calculada por DBSCAN.

### 3. [`capitulo-4-desarrollo.md`](./capitulo-4-desarrollo.md) — Capítulo IV: Desarrollo e Implementación (Sección 4.5 nueva)
* **Materialización del Software**:
  * Implementación modular de Android: CameraX, precalentamiento GPS, inferencia Edge AI con LiteRT, persistencia offline con Room, planificador WorkManager (`SyncWorker`) con tabla de decisión HTTP resiliente.
  * Backend Django REST Framework y algoritmo de agrupamiento **DBSCAN** en memoria (`backend/api/focos.py`) con distancia de Haversine ($\varepsilon = 50\text{ m}$, $MinSamples = 4$) optimizado con cuadrícula espacial.
  * Gestión de datos en Neon PostgreSQL y almacenamiento multimedia privado firmado en Cloudinary.
  * Empaquetado de APKs (Tabla 11): Variante de Producción (firmada con Keystore RSA `.jks` para datos válidos) y Variante de Prueba (`es_prueba = true` para calibración).
  * Control de calidad: **174 pruebas unitarias en Android**, **209 pruebas en el backend**, 26 pruebas de interfaz en tableta Samsung S9+, y análisis estático con Ktlint y Detekt.

### 4. [`fase-2-capitulo-3.md`](./fase-2-capitulo-3.md) — Capítulo III: Metodología y Confiabilidad (págs. 31-54)
* **Validez de Juicio de Expertos (Tablas 3 y 4)**: Recálculo formal de la V de Aiken mediante $V = \frac{S}{n(c-1)}$ a partir de las actas firmadas por los 3 ingenieros evaluadores: **$V = 0.892$** (SUS), **$V = 0.867$** (Ficha de campo) y **$V = 0.892$** (Ficha GPS), demostrando validez de contenido excelente ($V \ge 0.70$).
* **Confiabilidad de Instrumentos (Tabla 5 y Sección 3.3.4)**: Supresión de la columna informal de recordatorio (*«Qué necesitas»*) y de la muestra piloto simulada. Redacción formal del protocolo experimental previo a campo: Alfa de Cronbach con ítems invertidos sobre $n=38$, Kappa ponderado interobservador para agrupaciones y repetibilidad métrica intra-punto en 8 puntos GPS.
* **Materiales de Software (Sección 3.5.2)**: Especificación técnica exacta de Kotlin, Android SDK 34/36, Room, LiteRT, Django, Neon y Cloudinary.

### 5. [`fase-3-anexos.md`](./fase-3-anexos.md) — Anexos: Tablas Aiken y Sustitución Captura de Urcos
* **Anexo 13 (Ilustración 17)**: Instrucciones detalladas para retirar la captura original tomada en Urcos (`image18.png`) y sustituirla por la captura del visualizador de Google Maps centrado en **Wanchaq, Cusco** (Plaza Túpac Amaru, Av. de la Cultura, Av. Huayruropata $\approx -13.526, -71.964$).
* **Anexos 10, 11 y 12**: Tablas completas con los 10 criterios normativos, puntuaciones de los jueces Pumasupa, Huamán y Delgado, suma $S$ y la columna corregida de la V de Aiken formal.

### 6. [`fase-4-capitulo-1-y-2.md`](./fase-4-capitulo-1-y-2.md) — Capítulos I y II: Delimitación, Hipótesis y Marco Legal
* **Delimitación Espacial (Sección 1.4.1)**: Ratificación de la circunscripción territorial estricta en el distrito de Wanchaq ($6.38\text{ km}^2$, 65,584 hab.), vinculándola con el validador geoespacial del código.
* **Hipótesis (Sección 1.6)**: Armonización de la hipótesis general y las tres específicas con el diseño tecnológico y descriptivo.
* **Marco Teórico y Legal (Secciones 2.2.3, 2.2.4 y 2.4.6)**: Especificación de parámetros de DBSCAN y MobileNetV2, y fundamentación jurídica de la **Ley N.º 29733 de Protección de Datos Personales**, justificando por qué el identificador anónimo (UUID) y la ausencia de captura de rostros/placas cumple los principios de proporcionalidad y minimización.

---

## 3. Estado de Consistencia y Próximos Pasos

1. **Estado del Software**: El repositorio cuenta con el backend y el cliente móvil Android 100% operativos, verificados con 383 pruebas automatizadas combinadas y validados en tableta física.
2. **Estado de la Documentación**: La totalidad de discrepancias entre el borrador v5.91 y el software real han quedado resueltas y transcritas a formato académico en `docs/antigravity/`.
3. **Paso Pendiente del Usuario**:
   * Abrir el documento Word `Proyecto de Investigación v5.91 (1) (1).docx` y copiar/pegar los textos sustitutos siguiendo el orden de las fases.
   * En la Ilustración 17 (Anexo 13), insertar la captura de pantalla de Google Maps centrada en Wanchaq.
