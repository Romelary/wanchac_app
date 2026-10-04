# Resumen General del Proyecto: Wanchaq Limpio

## 1. Descripción General
**Wanchaq Limpio** es un sistema distribuido y moderno diseñado para la identificación, reporte y análisis espacial de focos de acumulación de residuos sólidos en el distrito de Wanchaq. El proyecto se destaca por su enfoque ciudadano, permitiendo realizar reportes anónimos apoyados por inteligencia artificial y geolocalización, incluso sin conexión a internet.

## 2. Arquitectura del Sistema
El sistema se divide en tres pilares tecnológicos altamente acoplados en su funcionamiento, pero desacoplados en su desarrollo:
* **Frontend (Aplicación Móvil):** Desarrollada de forma nativa en **Kotlin** para Android.
* **Backend (Servidor):** Construido en **Django 5.1** (Python) exponiendo una API REST.
* **Base de Datos y Almacenamiento:** Utiliza **PostgreSQL** con la extensión **PostGIS** para posibles futuras consultas espaciales, y delega el almacenamiento de fotografías en la nube mediante **Cloudinary**.
* **Inteligencia Artificial:** Implementación en el borde (Edge AI) utilizando **TensorFlow Lite**.

---

## 3. Funcionamiento de la Aplicación Móvil (Android)
La aplicación es el punto de interacción con el ciudadano y funciona bajo los principios de **Clean Architecture**, asegurando un código mantenible y testeable. Toda la interfaz de usuario está construida con **Jetpack Compose** para ser fluida y reactiva.

Sus funciones principales son:
* **Operación Offline-First:** Los usuarios pueden hacer reportes de basura sin necesidad de tener internet en ese momento. La app guarda temporalmente todos los datos, fotos y coordenadas en una base de datos local usando **Room**.
* **Sincronización Diferida:** A través de **WorkManager** (mediante `SyncWorker` y `ProgramadorSincronizacion`), el teléfono detecta en segundo plano cuándo vuelve a tener conectividad y envía automáticamente todos los reportes almacenados al servidor.
* **Georreferenciación Automática:** Extrae las coordenadas exactas usando los servicios de ubicación de Google (Fused Location Provider). Además, la app filtra automáticamente las capturas validando mediante un límite geoespacial (`LimiteDistritalWanchaq`) que el usuario realmente se encuentre dentro del distrito de Wanchaq.

---

## 4. Funcionamiento de la Inteligencia Artificial (ML)
Para agilizar el reporte y minimizar el error humano, la app integra un modelo de Visión Artificial directamente en el teléfono:
* **Clasificación On-Device:** Cuando el usuario toma la foto de los residuos, un modelo **MobileNetV2** (optimizado con TensorFlow Lite) analiza la imagen al instante en el mismo celular para sugerir la categoría (plásticos, cartón, orgánico, escombros, etc.).
* **Carga Perezosa (Lazy Loading):** Para evitar que la app se ponga lenta o consuma mucha batería, el modelo solo se carga en memoria de manera diferida justo cuando se necesita.
* **Toma de Decisiones del Usuario:** Si la inteligencia artificial se equivoca o el modelo tiene dudas, el ciudadano siempre tiene el poder de sobrescribir la sugerencia manualmente antes de enviar el reporte.

---

## 5. Funcionamiento del Servidor (Backend en Django)
El servidor es el cerebro centralizador que recibe la información de todos los ciudadanos y genera conocimiento a partir de los datos crudos.

Sus funciones y lógica de trabajo son:
* **Recepción y Almacenamiento Seguro:** Recibe los datos, sube las evidencias fotográficas a **Cloudinary** y guarda los registros en PostgreSQL. Destaca su diseño enfocado en la privacidad: no obliga a los usuarios a crear cuentas, manejando identificadores anónimos en cumplimiento con las leyes de protección de datos.
* **Manejo de Borrados Asíncronos:** Utiliza un ingenioso sistema de "lápidas" lógicas (`ReporteEliminado`) para sincronizar de manera perfecta si un usuario decide cancelar un reporte en su teléfono antes o durante el proceso de envío de la red.
* **Procesamiento y Agrupamiento Espacial (DBSCAN):** Esta es la función analítica más importante del backend. En lugar de ver reportes aislados, el servidor usa un algoritmo **DBSCAN** modificado con cálculos de distancia Haversine. Analiza todos los reportes y busca agrupaciones (focos). Si encuentra al menos **4 reportes** a un radio menor de **50 metros**, los consolida como un único "Foco de Acumulación", calculando su centroide geográfico y extrayendo la categoría predominante de basura. Esto filtra el ruido (reportes falsos o aislados) y da a las autoridades puntos exactos de limpieza.

---

## 6. Estado Actual y Conclusión
El proyecto funciona de manera integral y completa. Es una evolución altamente optimizada de su propio marco teórico inicial. Se han suprimido complejidades burocráticas del software (como registros de usuarios y roles) para priorizar una experiencia de recolección de datos masiva, anónima, asistida por inteligencia artificial y sin dependencia constante de internet (offline-first).
