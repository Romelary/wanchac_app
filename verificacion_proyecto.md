# Verificación del Proyecto: Wanchaq Limpio

Este documento contiene la verificación detallada punto por punto del proyecto actual, contrastando el código fuente implementado con los requerimientos establecidos en los Capítulos 1 al 3 del documento de investigación (`Proyecto de Investigación v5.91.docx`). El objetivo es servir como base para la redacción desde cero del Capítulo 4 (Diseño del Sistema).

> [!NOTE]
> Se han ignorado los apartados del Capítulo 4 original del documento de investigación y se ha analizado estrictamente lo que está construido actualmente en el repositorio.

---

## 1. Arquitectura General y Tecnologías

El documento de investigación (Capítulo 3) especifica una arquitectura basada en:
* **Cliente (Frontend):** Aplicación Android en Kotlin.
* **Backend (Servicios):** Servicios REST en Django.
* **Base de Datos:** PostgreSQL con extensión PostGIS.
* **Modelo de Machine Learning:** MobileNetV2 en el dispositivo.

### ✅ Estado en el Proyecto
* **Cumplimiento:** **Alto**. 
* **Evidencia:** 
  * La aplicación Android está desarrollada nativamente en **Kotlin** usando **Jetpack Compose** y arquitectura limpia (Clean Architecture).
  * El backend está desarrollado en **Django 5.1** y expone servicios REST.
  * Existen referencias claras a PostgreSQL y PostGIS (en `servicios.py` y `settings.py`), aunque temporalmente el modelo espacial está diferido en la base de datos (según ADR-0004) y el cálculo de distancias se realiza en memoria en Python.
  * Se utiliza **TensorFlow Lite** (`libs.litert` en `build.gradle.kts`) en la app para inferencia on-device de MobileNetV2 (`ClasificadorResiduoImpl.kt`).

---

## 2. Aplicación Móvil (Cliente)

### 2.1. Almacenamiento local (Offline-first)
* **Requerimiento:** La app debe operar sin conexión y tener almacenamiento local.
* **Evidencia:** Implementado utilizando **Room** (`AppDatabase.kt`, `ReporteDao.kt`, `ReporteEntity.kt`).
* **Sincronización:** Se emplea **WorkManager** (`SyncWorker.kt`, `ProgramadorSincronizacion.kt`) para sincronizar datos en diferido cuando la conexión a red se restablece.

### 2.2. Visión Artificial en el Dispositivo
* **Requerimiento:** Clasificar residuos (plásticos, cartón, vidrio, orgánicos, escombros) al momento de la captura en el teléfono sin depender de la red, usando MobileNetV2.
* **Evidencia:** Implementado en la capa `ml` de la aplicación (`ClasificadorResiduoImpl.kt`, `MotorInferencia.kt`). La carga del modelo es perezosa (`ClasificadorPerezoso`) para no bloquear el hilo principal y permite gracefully fallbacks (selección manual) si la IA falla. 

### 2.3. Georreferenciación y Captura
* **Requerimiento:** Capturar coordenadas, fecha, hora, foto y error de precisión. Excluir capturas fuera del distrito.
* **Evidencia:** La capa `location` y `util/GeoUtils.kt` gestionan las coordenadas usando Play Services Location (`ProveedorUbicacionFused.kt`). El límite distrital se verifica usando el dominio (`LimiteDistritalWanchaq.kt`). Además, en debug existe un modo de "Ubicación Simulada" configurable en `gradle.properties` para pruebas de campo.

---

## 3. Backend (Servidor REST)

### 3.1. Recepción y Gestión de Reportes
* **Requerimiento:** Recibir los datos de campo y guardarlos centralizados.
* **Evidencia:** Django gestiona los reportes a través de `Reporte` y `ReporteEliminado` en `models.py`. Se valida el tamaño máximo de fotografía (10MB) y se manejan atributos como el error de posición.

### 3.2. Agrupamiento Espacial (DBSCAN)
* **Requerimiento:** Agrupar reportes mediante DBSCAN con un radio (ε) y puntos mínimos (MinPts = 4) para identificar los focos de acumulación y descartar ruido espacial.
* **Evidencia:** **Implementado exitosamente**. El archivo `api/focos.py` implementa un algoritmo DBSCAN personalizado utilizando una cuadrícula (grid) y distancia de Haversine. 
  * Los parámetros están configurados a `RADIO_METROS = 50.0` y `MIN_REPORTES = 4`.
  * Se extrae la *categoría predominante* y el centroide de cada foco, lo que cumple estrictamente con el marco teórico de la investigación.

### 3.3. Base de Datos Espacial (PostgreSQL + PostGIS)
* **Requerimiento:** Uso de bases de datos espaciales con PostGIS para almacenamiento.
* **Evidencia:** Parcial/Diferido. La conexión exige PostgreSQL y valida explícitamente la existencia de la función `ST_DWithin` de PostGIS (`servicios.py` -> `comprobar_base_de_datos()`). Sin embargo, los modelos de Django de la Fase 1 guardan latitud y longitud como simples `FloatField` en lugar del tipo Point de GeoDjango para ahorrar complejidad hasta requerir consultas espaciales reales.
* **Recomendación para el Capítulo 4:** Explicar claramente esta decisión arquitectónica (detallada en `ADR-0004`). La base de datos es PostgreSQL con PostGIS habilitado, pero la evaluación de densidad de DBSCAN actualmente se procesa a nivel de aplicación en el servidor (en memoria) al ejecutar `/puntos-criticos`.

### 3.4. Almacenamiento de Imágenes
* **Requerimiento:** Almacenar evidencias documentales fotográficas.
* **Evidencia:** Se utiliza la API externa de **Cloudinary** para almacenar fotos, como se evidencia en `servicios.py` y `settings.py` (`foto_public_id` en `models.py`).

---

## 4. Alineación con las Variables de Estudio

### 4.1. Monitoreo de focos (Identificación, Calidad, Caracterización)
El algoritmo DBSCAN programado en `focos.py` asegura la correcta **identificación del foco** descartando el ruido espacial. El cálculo del centroide y de la categoría predominante cubre directamente la dimensión de **caracterización del foco** mencionada en el documento de investigación.

### 4.2. Sistema Móvil de Georreferenciación Colaborativa
El diseño del sistema cumple con la captura anónima (uso de un UUID como id anónimo para no comprometer datos personales y cumplir con la Ley N.º 29733 de Protección de Datos Personales) y recopila exitosamente toda la información necesaria (timestamp, error de posición, metadatos del modelo IA y categoría elegida).

---

## Conclusiones para la elaboración del Capítulo 4

1. **Diseño de Software Realista:** El Capítulo 4 deberá describir los componentes y clases que realmente se encuentran estructurados en el código (Clean Architecture, Jetpack Compose, MVVM, WorkManager).
2. **Diagrama de Base de Datos (MER):** El diseño de la BD del Capítulo 4 original (Tablas: Usuario, Categoria_residuo, Estado_reporte, etc.) **NO COINCIDE** con la base de datos real. El proyecto actual es mucho más limpio (tablas `reporte` y `reporte_eliminado`), sin registro de usuarios explícito (debido al reporte anónimo) ni catálogos innecesarios. El nuevo diseño de base de datos del Capítulo 4 debe reflejar los modelos de `api/models.py`.
3. **Casos de Uso e Interfaz:** El modelo de operaciones no incluye "Registro de usuarios" ni "Login". El nuevo diseño debe enfocarse en los casos de uso reales: Otorgar Permisos/Consentimiento, Capturar Reporte (Cámara + Inferencia), Seleccionar Clasificación, Enviar/Sincronizar y Ver Mapa Público.
