# Correcciones y Actualizaciones de Tesis — Wanchaq Limpio
### Carpeta de Salidas y Documentación de Antigravity (`docs/antigravity/`)

> Esta carpeta contiene la documentación técnica y metodológica completa para subsanar, corregir y actualizar el documento académico **«Proyecto de Investigación v5.91 (1) (1).docx»**, alineándolo de manera fidedigna con el software real desarrollado en el repositorio y con la normativa peruana vigente (D.L. 1278, D.S. 001-2022-MINAM y Ley N.º 29733 de Protección de Datos Personales).

---

## 1. Justificación y Origen de las Discrepancias

Durante la revisión exhaustiva del borrador de tesis (v5.91) frente al código del proyecto, se constató que:
1. **El Capítulo IV (y partes del Capítulo III) fue escrito como una propuesta preliminar previa a la construcción del software.** Por esta razón, el borrador mencionaba tecnologías que no se usaron (Flutter, React.js, Node.js, Express, AWS S3) y diagramas conceptuales basados en mockups de iPhone con texto placeholder (*"Trasha acumumilatior"*).
2. **El diseño preliminar de base de datos violaba la Ley N.º 29733:** Contemplaba una tabla `usuario` obligatoria con DNI, nombres, teléfono y contraseñas. Durante el desarrollo del proyecto, se tomó la decisión arquitectónica formal ([ADR-0001](../adr/0001-sin-login-id-anonimo.md)) de prescindir del login y adoptar un identificador anónimo (UUID en el dispositivo), eliminando cualquier dato sensible y facilitando la adopción ciudadana.
3. **Incongruencia geográfica del prototipo:** La Ilustración 17 (Anexo 13) del borrador mostraba una captura de pantalla real tomada en **Urcos (Provincia de Quispicanchi)** en la *Apv Tupac Amaru de Urcos* (lugar de residencia del autor durante las pruebas iniciales), mientras que la tesis está estrictamente delimitada al **Distrito de Wanchaq, Cusco**.
4. **La sección de confiabilidad contenía una "muestra piloto simulada":** La Tabla 5 del documento v5.91 contenía notas de trabajo informales (*«Qué necesitas: Piloto real con al menos quince participantes…»*) y valores de Kappa estadísticamente imposibles para 6 observaciones ($4/6$ coincidencias no pueden dar $\kappa > 0.80$).

---

## 2. Estructura de Documentos de Corrección

Para facilitar la edición del documento Word de manera ordenada y precisa, las correcciones se dividen en los siguientes archivos independientes:

| Archivo | Ámbito de Corrección en el Documento | Estado |
|---|---|---|
| [`avance-chat.md`](./avance-chat.md) | **Memoria Integral de Sesión**: Registro cronológico de preguntas, análisis, decisiones y síntesis del avance. | Maestro |
| [`bitacora-avance.md`](./bitacora-avance.md) | **Bitácora Ejecutiva**: Resumen de auditoría de evidencias, imágenes y estados de consistencia. | Referencia |
| [`fase-1-capitulo-4.md`](./fase-1-capitulo-4.md) | **Capítulo IV (Secciones 4.1 a 4.4):** Requisitos, Casos de Uso (Ilustración 1), Arquitectura (Ilustración 2), Wireframes reales (Ilustraciones 3-5) y Modelo ER (Ilustración 6). | Listo para aplicar |
| [`capitulo-4-desarrollo.md`](./capitulo-4-desarrollo.md) | **Capítulo IV (Sección 4.5 nueva):** Desarrollo e Implementación del sistema (Módulos Android Kotlin/Compose, LiteRT MobileNetV2, Backend Django REST, DBSCAN en memoria, variantes de APK y pruebas automatizadas). | Listo para aplicar |
| [`fase-2-capitulo-3.md`](./fase-2-capitulo-3.md) | **Capítulo III (págs. 31-54):** Población/Muestra censal y SUS ($n=38$), recálculo formal de V de Aiken (Tablas 3-4), limpieza de Confiabilidad (Tabla 5) y Materiales de software. | Listo para aplicar |
| [`fase-3-anexos.md`](./fase-3-anexos.md) | **Anexos (págs. 93-96):** Tablas de cómputo de Aiken (Anexos 10-12) y sustitución de la captura de Urcos por el mapa centrado en Wanchaq (Anexo 13, Ilustración 17). | Listo para aplicar |
| [`fase-4-capitulo-1-y-2.md`](./fase-4-capitulo-1-y-2.md) | **Capítulos I y II:** Delimitación distrital en Wanchaq, coherencia de hipótesis y fundamentación de la Ley N.º 29733 (anonimización y minimización). | Listo para aplicar |

---

## 3. Guía de Uso para el Usuario
Cada archivo `.md` de esta carpeta sigue un formato estándar:
- **Ubicación exacta**: Página, sección, tabla o figura en el docx v5.91.
- **Texto / Gráfico Actual**: Lo que dice actualmente el Word.
- **Justificación**: Por qué se modifica, respaldado en el código y en los ADRs.
- **Texto / Gráfico de Reemplazo**: Texto redactado en tono académico listo para copiar y pegar en Word, junto con la especificación de diagramas en Mermaid y capturas recomendadas.
