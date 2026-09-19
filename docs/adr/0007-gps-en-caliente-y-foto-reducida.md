# ADR-0007 — GPS en caliente con espera acotada, foto conservada ante un GPS impreciso y foto reducida al tomarla

- **Estado**: aceptada (el GPS **real** sigue sin probarse: ver «Lo que NO está verificado»)
- **Fecha**: 2026-09-19
- **Contexto**: captura del reporte (Módulos 2 y 3b), RF-02 (error de GPS ≤ 15 m y dentro de Wanchaq) y el indicador de confiabilidad del Anexo 2 (`diseno.md` §1). Prepara el uso de la app en campo (`docs/pruebas-de-confiabilidad.md`).

## Contexto

Al revisar la app para usarla en la calle se encontraron cuatro problemas **reales** (de lectura del código y de la primera sesión en la tablet, no hipótesis):

1. **Se pedía una sola lectura del GPS al pulsar «Guardar»**, y la primera lectura de un GPS en frío suele ser la peor. Si pasaba de 15 m, el reporte se rechazaba **y se borraba la foto**: el ciudadano tenía que repetirlo todo. En una calle estrecha entre edificios eso ocurre muy a menudo, y cada rechazo así es un reporte perdido que no aparece en ningún indicador.
2. **Nada le decía al ciudadano si el GPS estaba listo** antes de tomar la foto: solo se enteraba al final.
3. **Los permisos no tenían salida.** Android 12 y posteriores ofrecen ubicación «precisa» o «aproximada» en el mismo diálogo y exigen pedir las dos; la app pedía solo la precisa. Y si el ciudadano negaba el permiso dos veces, Android ya no vuelve a preguntar y la pantalla de permisos **no tenía ningún botón que llevara a los Ajustes**.
4. **La foto se guardaba y se subía tal cual sale de la cámara.** Medido en la Galaxy Tab S9+: **4128 × 3096 px y ≈ 4,0 MB** por foto, con metadatos EXIF. Subir 4 MB por datos móviles flojos es justo lo que agota los tiempos de espera y gasta los datos del ciudadano; el servidor admite hasta 10 MB, así que una cámara de más resolución podría además superarlo (413, rechazo definitivo).

## Decisiones

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 1 | **Mantener el GPS encendido mientras la cámara está abierta** (`RastreadorDeUbicacion.vigilar`) y mostrar su estado arriba: *buscando* → *listo ±N m* / *impreciso ±N m* | Pedir el GPS solo al guardar (lo anterior) | Cuando el ciudadano toma la foto ya hay una lectura buena, y ve cuándo esperar. Solo gasta batería con la cámara abierta: **no hay rastreo en segundo plano** (la Ética, Anexo 9, exige que la app «no registre su recorrido»; no se pide `ACCESS_BACKGROUND_LOCATION`). |
| 2 | **La lectura del momento de la captura viaja con la confirmación** (`Confirmando`) y se usa si ya era ≤ 15 m | Volver a pedir el GPS al guardar | El reporte queda en el lugar donde se tomó la foto, no donde el ciudadano estaba cuando terminó de elegir la categoría. |
| 3 | **Si no la había, esperar hasta 30 s a una lectura ≤ 15 m**, mostrando la precisión que se va logrando, y quedarse con la mejor | Rendirse con la primera; esperar sin tope | 30 s es una **propuesta** (el tiempo medio hasta el primer fijo de esa tablet es de ≈ 6 s, pero con mala señal puede tardar mucho más); vive en una constante (`ESPERA_MAXIMA_DEL_GPS`) y se ajusta con datos del escenario C8/H9. |
| 4 | **El criterio de 15 m NO se relaja** | Aceptar 20–30 m «para que pase más» | Es el criterio del protocolo (RF-02). Cambiarlo alteraría la variable medida; lo que se mejora es **cuánto se espera** y **no perder la foto**, no cuán exigente es la regla. |
| 5 | **Con GPS impreciso o sin señal, la foto se conserva**: estado `GpsImpreciso` con el botón «Reintentar ubicación». La foto solo se borra ante un **rechazo real** (fuera del distrito, duplicado) | Borrarla siempre que se rechaza (lo anterior) | Un GPS que aún no converge no es un problema del reporte; sí lo son estar fuera de Wanchaq o repetir un lugar. |
| 6 | **Pedir precisa y aproximada juntas, exigir la precisa, explicar y ofrecer «Abrir los ajustes de la app»** | Pedir solo la precisa; no mostrar salida | Android 12+ lo exige; sin el botón, quien negó dos veces queda atrapado. Al volver de los Ajustes se vuelven a comprobar los permisos. |
| 7 | **Reducir la foto justo al tomarla** (antes de que la analice la IA): lado largo ≤ **1600 px**, JPEG calidad **85**, con la orientación EXIF ya aplicada a los píxeles y **sin metadatos** (tampoco ubicación). Si no se puede reducir (archivo ilegible, sin memoria), se conserva y se envía el original | Reducir al enviar (en `SyncWorker`); subir el original; una sola calidad fija sin tope de lado | La foto original no llega a quedarse en el disco de la app ni se reduce más de una vez; el clasificador de IA reescala a 224 px, así que **no debería perder nada**; y quitar los metadatos evita que la foto lleve una ubicación distinta a la validada. Un fallo al reducir **no debe perder el reporte** (por eso se envía el original). |

## Consecuencias y riesgos

- **La foto que llega al servidor no es la original**: 1600 px sobran para ver un foco de residuos y para que un observador lo reconozca en campo, pero **no sirven para un peritaje de alta resolución**. Es reversible (`LADO_MAXIMO_FOTO_PIXELES`, `CALIDAD_JPEG_FOTO`).
- **Medido en la tablet (2026-09-19)**: 4128 × 3096 px y 4,0 MB → **1600 × 1200 px y ≈ 0,2 MB** (17 veces menos), sin EXIF. Una escena con más detalle pesará más (estimación: 0,3–0,6 MB; **sin medir**).
- **El GPS encendido con la cámara abierta gasta batería** solo mientras dura esa pantalla. No se midió el consumo.
- **La espera de 30 s al guardar con mala señal** es una decisión de experiencia de uso sin evidencia de campo todavía: si en exteriores despejados casi nunca se espera, sobra; si en la calle se espera casi siempre, hay que revisar el número (o el criterio, con el asesor).
- **Ubicación aproximada**: el ciudadano que solo concede esa ve la pantalla de permisos otra vez, con la explicación de que debe ser «precisa». Es lo que exige RF-02.
- **El envío de la foto original ante un fallo de reducción** significa que, en ese caso raro, sigue existiendo el riesgo del punto 4 (foto de varios MB). Queda un aviso en Logcat (`ReductorDeFotos`).

## Lo que NO está verificado

- **El GPS real.** Con pruebas automáticas (proveedor simulado y reloj falso) se comprobó la lógica de espera y de mejor lectura, y en la tablet se vio la interfaz con la **ubicación simulada** («GPS listo · ±5 m»), pero **no** el comportamiento con lecturas reales: cuánto tarda en llegar a 15 m, cómo se ve «impreciso» en interiores ni cuántos reportes se ganan (escenarios C8 y H9 de [`prueba-en-dispositivo.md`](../prueba-en-dispositivo.md)).
- **Fotos verticales y giradas** en la tablet: la reducción aplica la orientación EXIF (probado con imágenes reales en la JVM), pero no se giró la tablet en la sesión (escenario C3).
- **Que la reducción no cambie la sugerencia de la IA.** La foto se reduce **antes** de analizarla, así que la IA ve la foto reducida (1600 px con JPEG de calidad 85, que luego el modelo reescala a 224 px). Se espera que no cambie nada, pero **no se comparó**: para hacerlo, correr `ml/probar_modelo.py` sobre las mismas fotos originales y reducidas. Además, la exactitud del modelo con fotos de calle sigue sin medirse (`ml/README.md`).
