# Subsanación y Actualización — Capítulos I y II: Delimitación, Hipótesis y Marco Legal

> **Documento de destino**: `Proyecto de Investigación v5.91 (1) (1).docx`  
> **Sección a sustituir**: Capítulo I (secciones 1.4.1, 1.6; págs. 7-10) y Capítulo II (secciones 2.2.3, 2.2.4 y 2.4.6; págs. 25-30).  
> **Estado**: Listo para copiar y pegar directamente en el documento de tesis.  
> **Fundamentación técnica**: Decisiones arquitectónicas [ADR-0001](../adr/0001-sin-login-id-anonimo.md), [ADR-0003](../adr/0003-litert-interpreter-como-runtime-de-ia.md), [ADR-0009](../adr/0009-focos-detectados-al-pedir-con-dbscan.md) y catálogo de correcciones [`docs/correcciones-documento.md`](../correcciones-documento.md) (`CORR-001`, `CORR-007`).

---

## RESUMEN DE CAMBIOS EN LOS CAPÍTULOS I Y II

| Elemento en v5.91 | Problema en el borrador original | Corrección y Reemplazo Definitivo |
|---|---|---|
| **1.4.1 Delimitación espacial (pág. 7)** | En algunos párrafos se mencionaba ambiguamente "Cusco" o la provincia, generando confusión sobre el alcance territorial de la muestra y el filtro de GPS. | Se ratifica la delimitación espacial estricta en el **distrito de Wanchaq** (superficie de $6.38\text{ km}^2$, población aproximada de 65,584 hab.), precisando que el filtro satelital del aplicativo rechaza cualquier coordenada que caiga fuera de sus límites administrativos. |
| **1.6 y 3.1 Hipótesis del estudio (págs. 9 y 31)** | Había una aparente contradicción: en 3.1 se decía *"Aunque no se presente una hipótesis formal..."*, mientras que la sección 1.6 y la Matriz de Consistencia sí contemplaban hipótesis general y específicas. | Se armoniza la redacción metodológica: se definen formalmente las hipótesis como conjeturas de desempeño tecnológico evaluables en un estudio descriptivo-propositivo y tecnológico. |
| **2.2.3 y 2.2.4 DBSCAN y Visión Artificial (págs. 25-26)** | Las bases teóricas eran genéricas y no detallaban los hiperparámetros del agrupamiento ni las clases del modelo. | Se especifican los parámetros exactos: DBSCAN con $\varepsilon = 50\text{ m}$ geodésico y $MinSamples = 4$; y MobileNetV2 FP16 en LiteRT para 4 clases de residuos entrenadas (`plástico`, `cartón`, `vidrio`, `orgánico`) más 2 categorías manuales (`escombros`, `mixto`). |
| **2.4.6 Ley N.º 29733 de Protección de Datos Personales (pág. 29)** | No explicaba por qué el sistema prescindió del login formal con DNI y contraseñas. | Se enriquece el marco legal demostrando cómo la arquitectura con identificador pseudoaleatorio (UUID) en el dispositivo y la ausencia de captura de rostros o placas cumple los principios de **minimización, finalidad y proporcionalidad**, garantizando la privacidad ciudadana. |

---

# CONTENIDO DEFINITIVO PARA LOS CAPÍTULOS I Y II DEL DOCUMENTO WORD

---

## 1.4 Delimitación de la investigación

### 1.4.1 Delimitación espacial

La investigación se desarrolla de manera exclusiva y estricta en el **distrito de Wanchaq**, uno de los ocho distritos que conforman la provincia del Cusco, en el departamento del Cusco, Perú. Wanchaq posee una superficie territorial de aproximadamente **6.38 kilómetros cuadrados** y una población estimada de **65,584 habitantes** (INEI, 2022). Se caracteriza por ser un distrito neurálgico con intensa dinámica comercial (Mercado de Túpac Amaru, Mercado de Ttio, ferias sabatinas), institucional (sede de la Municipalidad, Universidad Continental, dependencias públicas) y residencial de media y alta densidad.

Esta delimitación territorial no es meramente descriptiva, sino que se encuentra integrada algorítmicamente en el software desarrollado: el aplicativo móvil incorpora un **validador geoespacial** en el cliente y en el backend que descarta y rechaza de forma automática cualquier intento de reporte cuyas coordenadas satelitales caigan fuera del polígono administrativo distrital de Wanchaq (envolvente en latitud $[-13.545, -13.515]$ y longitud $[-71.990, -71.950]$).

---

## 1.6 Hipótesis y Variables de la Investigación

En concordancia con el carácter tecnológico y descriptivo-propositivo del estudio, las hipótesis formulan expectativas de desempeño técnico, usabilidad y precisión métrica del artefacto desarrollado frente a estándares de referencia y requerimientos municipales.

### 1.6.1 Hipótesis general
El desarrollo e implementación de un sistema móvil de georreferenciación colaborativa optimiza el monitoreo de focos de acumulación de residuos sólidos urbanos en el distrito de Wanchaq, Cusco, alcanzando una usabilidad percibida superior al estándar de referencia (SUS $\ge 68$ puntos) y una disponibilidad operativa de sincronización $\ge 95\%$.

### 1.6.2 Hipótesis específicas
* **$H_1$**: La arquitectura desacoplada y offline-first del sistema móvil permite registrar y sincronizar reportes ciudadanos georreferenciados con una tolerancia al error de posicionamiento horizontal $\le 15\text{ metros}$ en el distrito de Wanchaq.
* **$H_2$**: La integración de un modelo de visión artificial ligero (MobileNetV2) ejecutado in situ en el dispositivo alcanza un porcentaje de coincidencia con la observación directa $\ge 70\%$ en la categorización de residuos sólidos.
* **$H_3$**: El algoritmo de agrupamiento espacial por densidad (DBSCAN con $\varepsilon = 50\text{ m}$ y $MinSamples = 4$) permite consolidar reportes ciudadanos dispersos en centroides representativos de puntos críticos de acumulación, filtrando reportes aislados como ruido espacial.

---

## 2.2 Bases Teóricas Específicas

### 2.2.3 Agrupamiento espacial por densidad (DBSCAN)
Los reportes generados por la ciudadanía constituyen una nube de puntos desestructurada y dispersa. Convertir esos registros individuales en «focos de acumulación» o puntos críticos consolidados exige un procedimiento de agrupamiento no supervisado capaz de identificar formas geométricas arbitrarias y descartar reportes no corroborados. 

El algoritmo **DBSCAN** (*Density-Based Spatial Clustering of Applications with Noise*), propuesto por Ester, Kriegel, Sander y Xu (1996), fue seleccionado debido a que no requiere prefijar el número de conglomerados (a diferencia de K-Means) y maneja de manera nativa el concepto de ruido. En la presente investigación, el algoritmo se parametriza de la siguiente manera:
1. **Radio de vecindad ($\varepsilon = 50\text{ metros}$)**: Distancia geodésica máxima calculada mediante la fórmula de Haversine sobre el elipsoide WGS84. Este valor de 50 metros se adoptó porque cubre la dispersión habitual de residuos a lo largo de una esquina o frente a un mercado sin fusionar acumulaciones que pertenecen a cuadras distintas.
2. **Número mínimo de puntos ($MinSamples = 4$)**: Se exige la concurrencia de al menos cuatro reportes independientes dentro del radio $\varepsilon$ para constituir un núcleo de acumulación. Aquellos reportes aislados que no alcancen esta densidad son etiquetados como ruido espacial y excluidos del mapa público de focos, actuando como un filtro automático contra reportes erróneos o malintencionados.

### 2.2.4 Visión artificial en el dispositivo (Edge Computing)
La inferencia de visión computacional se ejecuta directamente en el procesador del teléfono inteligente mediante el motor **LiteRT / TensorFlow Lite**, evitando transferir imágenes pesadas a un servidor para su procesamiento preliminar. 

El modelo implementado corresponde a una red neuronal convolucional **MobileNetV2** con cuantización a punto flotante de 16 bits (FP16). Esta arquitectura utiliza bloques residuales invertidos y cuellos de botella lineales, lo que permite un tiempo de inferencia inferior a 100 milisegundos en procesadores móviles. El modelo asiste al ciudadano sugiriendo automáticamente una de cuatro categorías de residuos con alta reciclabilidad o descomposición (`plástico`, `cartón`, `vidrio`, `orgánico`), permitiendo además la selección manual de categorías adicionales (`escombros`, `mixto`) cuando el residuo no corresponde a las clases entrenadas.

---

## 2.4 Marco Legal

### 2.4.6 Ley N.º 29733, Ley de Protección de Datos Personales

La Ley N.º 29733 y su Reglamento (D.S. N.º 003-2013-JUS) rigen el tratamiento de cualquier dato susceptible de identificar a personas naturales. Dado que los sistemas colaborativos convencionales suelen exigir el registro de nombres, números de DNI, correos electrónicos y números telefónicos, frecuentemente generan bancos de datos personales que demandan inscripción formal ante el Registro Nacional de Protección de Datos Personales del Ministerio de Justicia y Derechos Humanos (MINJUS).

En la presente investigación, la arquitectura tecnológica se diseñó bajo los principios de **Consentimiento, Finalidad, Proporcionalidad y Minimización de Datos** (Artículos 5, 6 y 7 de la Ley N.º 29733):

1. **Minimización y Anonimización (Sin Login)**: El sistema elimina por completo el formulario de registro de usuario. No se solicita ni se almacena el nombre, DNI, teléfono ni correo electrónico del ciudadano. En su lugar, tras la aceptación del consentimiento informado en el primer arranque, la aplicación genera localmente un identificador anónimo pseudoaleatorio (UUID v4 de 128 bits) que identifica al dispositivo emisor sin vincularlo a ninguna identidad civil.
2. **Tratamiento Ético de la Evidencia Visual**: El sistema establece como norma operativa que la fotografía no debe registrar rostros de transeúntes ni placas de rodaje de vehículos. Asimismo, las imágenes son reescaladas en el terminal y despojadas de sus metadatos EXIF (que podrían contener números de serie del sensor o miniaturas) antes de su envío.
3. **Almacenamiento Seguro y No Público**: Las fotografías se almacenan en un repositorio privado en la nube (`authenticated`), siendo accesibles únicamente por el investigador y el sistema mediante enlaces firmados temporalmente.
4. **Derecho ARCO de Cancelación y Olvido**: La aplicación implementa el mecanismo de «Borrar todos mis datos», el cual remueve el almacenamiento local y transmite una instrucción de baja remota al servidor (`DELETE /reportes/{id_anonimo}/{id_local}`), registrando una lápida lógica (*tombstone*) para asegurar que ningún registro asociado a ese identificador anónimo permanezca en el sistema.

Mediante estas salvaguardas técnicas, el sistema garantiza un tratamiento de datos plenamente apegado a la legalidad peruana, protegiendo al ciudadano y eliminando cualquier barrera burocrática para la participación ciudadana.
