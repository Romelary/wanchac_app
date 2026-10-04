# Resumen de Avance del Proyecto - Octubre 2026

> **Proyecto**: Wanchaq Limpio (Aplicación para reporte y detección de puntos críticos de residuos sólidos)
> **Estado**: Listo para Pruebas de Campo (Versión Release Generada)

Este documento resume las actualizaciones y mejoras integradas en la última fase de desarrollo, enfocadas principalmente en la identidad visual de la aplicación, la optimización de la interfaz de usuario (UX/UI), y la preparación técnica para el despliegue a producción.

---

## 1. Implementación de Identidad Institucional (Universidad Continental)
Se ha reemplazado completamente el diseño genérico de Android por una paleta de colores institucional que refleja la identidad de la **Universidad Continental**, garantizando una apariencia profesional y unificada:

* **Colores Principales**: Se implementó el color morado corporativo (`#5C2D91`) para barras de navegación, botones de acción (Floating Action Buttons) y componentes principales.
* **Modo Claro (Light Theme)**: Se diseñó un fondo sutilmente tintado (`#F7F2FA`) para reemplazar el blanco agresivo o gris estándar, brindando descanso visual y elegancia. Las tarjetas y contenedores usan fondos blancos con contrastes en grises oscuros.
* **Modo Oscuro (Dark Theme)**: Se eliminó el fondo negro absoluto (que daba un aspecto genérico) y se sustituyó por un tono morado muy profundo (`#1B0E2A`) y un lila suave (`#D0BCFF`) para acentos.
* **Personalización del Ícono**: Se incorporó exitosamente el isotipo fotográfico oficial ("ícono de reporte de basura geolocalizada") como el `app_icon` y `roundIcon` en los recursos nativos de Android, redimensionado estrictamente a proporciones de publicación (512x512).

## 2. Mejoras en la Experiencia de Usuario (UX/UI) y Mapa
La navegación y visualización geoespacial han sido optimizadas para el uso en dispositivos de pantalla grande (Tablets) y uso en exteriores:

* **Renderizado Ininterrumpido del Mapa**: Se solucionó un defecto visual (estado de carga infinita sin conexión) separando la capa del mapa (`GoogleMap`) de los estados de interfaz. El mapa ahora se renderiza incondicionalmente en el fondo, mejorando la inmersión.
* **Alertas Colapsables**: Las notificaciones de estado (ej. "Sin internet" o "Aún no hay focos detectados") que ocupaban gran parte de la pantalla fueron rediseñadas para ser "descartables". Al cerrarlas, se minimizan en un pequeño botón flotante de información (`ℹ`) en la parte superior.
* **Control de Capas Simplificado**: Se reemplazó la fila horizontal de botones de capas por un botón flotante desplegable (`☰`), estilo "Google Maps", ubicado en la esquina inferior derecha. Al presionarlo, despliega las opciones: *Focos*, *Míos* y *Calor*, ahorrando valioso espacio de pantalla.
* **Edge-to-Edge Design**: La app respeta las barras transparentes del sistema, logrando una experiencia inmersiva completa en la tablet.

## 3. Preparación para Producción y Despliegue Backend
El ecosistema de desarrollo ha sido migrado de las pruebas locales a un entorno real y en vivo:

* **Conexión a Render (Backend Vivo)**: La aplicación de Android se enlazó definitivamente al servidor REST API desplegado en **Render** (`https://wanchaq-limpio-api.onrender.com`).
* **Base de Datos Neon (PostgreSQL + PostGIS)**: Todas las transacciones (reportes, coordenadas GPS, validación DBSCAN) se están registrando y consultando directamente en la base de datos distribuida de **Neon**, garantizando persistencia en la nube. Se confirmó la operabilidad mediante el script de pruebas `verificar_servicios.py`.
* **Modo Estricto Activado**: Se eliminó la bandera de desarrollo `-PmodoPrueba=true`. La aplicación en su versión actual ahora exige de forma obligatoria que el GPS marque una **precisión menor a 15 metros** y que las coordenadas registradas existan **dentro del polígono oficial del distrito de Wanchaq**.
* **Generación de Entregables (Release)**: Se compiló exitosamente la aplicación utilizando llaves criptográficas de publicación (`wanchaq-limpio-release.jks`), generando dos artefactos finales:
  1. `app-release.aab`: Archivo optimizado (Android App Bundle).
  2. `app-release.apk`: Instalador directo para el equipo de pruebas (Testers).

## 4. Próximos Pasos (Flujo de Pruebas de Campo)
Con la infraestructura backend en la nube y el instalador estricto (`.apk`) listo, el proyecto avanza a la etapa de pruebas de usabilidad y estrés técnico en el terreno:
* Instalación del APK en las tablets y dispositivos Android de los evaluadores.
* Verificación de la lectura de latitud/longitud en exteriores.
* Confirmación de agrupación de reportes cercanos mediante el algoritmo DBSCAN del servidor.
* Generación visual y comprobación del Mapa de Calor para el análisis de los puntos críticos de basura.
