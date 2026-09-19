# Pruebas de confiabilidad de la app en campo

> Protocolo para medir, con la app instalada en un teléfono real, el indicador **«Reportes guardados y sincronizados según condición de red, ≥ 95 % en cada condición»** (Anexo 2, [`diseno.md`](./diseno.md) §1, *Disponibilidad operativa*) y para comprobar que el envío, el GPS y la cámara resisten lo que pasa en la calle.
>
> **Es una propuesta de método, pendiente de validar con el asesor.** El Anexo 2 fija el indicador y el umbral (≥ 95 % en cada condición), pero no el tamaño de muestra, ni cuánto esperar, ni cómo se provoca cada condición: eso lo propongo yo (§2, §3, §7). Lo que la app hace por su cuenta —anotar cada intento— está en [ADR-0006](./adr/0006-envio-confiable-de-reportes.md).
>
> **Estado (2026-09-19)**: la app ya registra todo lo necesario, y su comportamiento ante fallos se comprobó **en la Galaxy Tab S9+ contra un servidor falso local** (servidor caído, respuesta perdida, cierre forzado, red que se cuelga…; ver [`prueba-en-dispositivo.md`](./prueba-en-dispositivo.md) §5c y §2c). **Todavía no se ha ejecutado nada contra el servidor real (Render) ni con condiciones de red reales** (Wi-Fi sin salida, datos móviles, señal débil), **ni el GPS real**: sin eso **no hay una tasa de sincronización que informar**. Lo que se comprobó con pruebas automáticas (y lo que no) está en [`pruebas.md`](./pruebas.md).

## 1. Qué se mide y qué no

| Se mide aquí | Cómo |
|---|---|
| **Tasa de sincronización por condición de red** (el indicador del Anexo 2) | Reportes «Enviado» ÷ reportes guardados, en cada condición (§3) |
| Tiempo desde que se guarda hasta que el servidor lo recibe | El registro de la app y `recibido_en` en Neon (§6) |
| **Por qué** no llegó un reporte | El motivo del último fallo y el código HTTP que guarda la app |
| Que **nada se pierda** al cerrar la app, reiniciar el teléfono o guardar varios reportes seguidos | Escenarios H1 a H5 de [`prueba-en-dispositivo.md`](./prueba-en-dispositivo.md) |
| Que el GPS llegue a 15 m sin obligar a repetir la foto | Escenario H9 (y H10, los permisos) |

**No se mide aquí**: la exactitud de la IA (necesita al observador de campo, Instrumento 2), la usabilidad (SUS, Instrumento 1) ni el agrupamiento (Fase 2 del backend, aún sin hacer).

## 2. Definiciones (propuestas)

- **Guardado**: un reporte que pasó las reglas (≤ 15 m, dentro de Wanchaq, sin duplicado) y aparece en «Mis reportes».
- **Sincronizado**: la app lo muestra **«Enviado»** **y** existe en la base de Neon, con el mismo `(id_anonimo, id_local)`. Si la app dice «Enviado» y el servidor no lo tiene (o al revés), es un **fallo grave**: se investiga antes de seguir.
- **Resultado de un reporte**: *Enviado*, *Pendiente* (sigue esperando) o *No se pudo enviar* (rechazado; se cuenta aparte, con su motivo).
- **Tasa** = Enviados ÷ Guardados en esa condición. **Criterio**: ≥ 95 % **en cada** condición.
- **Ventana de espera**: cuánto se espera, con la conexión ya restablecida, antes de dar un pendiente por «no sincronizado». **Propongo 10 minutos** (el servidor gratuito puede tardar ≈ 1 min en despertar y la espera entre reintentos crece de 30 s en 30 s).
- **Condición de red**: la que **anota la app**, no la que se quiso provocar. Si se quería probar datos móviles y el registro dice `WIFI`, esa tanda no vale.

## 3. Condiciones de red

| Código | Condición | Cómo provocarla | Lo que debe anotar la app (`red al guardar`) |
|---|---|---|---|
| R1 | Wi-Fi estable | Conectado a un Wi-Fi con Internet | `WIFI` |
| R2 | Datos móviles | Wi-Fi apagado, buena señal | `DATOS_MOVILES` |
| R3 | Sin red y después con red | Modo avión → guardar los reportes → apagar el modo avión y esperar | `SIN_RED` (y `WIFI` o `DATOS_MOVILES` en el último intento) |
| R4 | Wi-Fi **sin salida a Internet** | Un router con el cable a Internet desconectado, o el punto de acceso de otro teléfono sin datos → guardar → dar salida | `SIN_INTERNET` |
| R5 | Señal débil o lenta | Una zona de mala cobertura (o limitar el ancho de banda del punto de acceso). **No hay un método uniforme**: se describe cómo se hizo | `DATOS_MOVILES` o `WIFI`, **más la nota de quien prueba** (la app no mide la intensidad de la señal) |
| R6 | Servidor «dormido» (arranque en frío) | Sin pinger (o ≥ 15 min sin tráfico) → guardar **un** reporte y cronometrar | La que haya |

## 4. Preparación (lista de comprobación)

- [ ] **Servidor listo**: Render con la Fase 1 desplegada y `python verificar_servicios.py --remoto URL --reporte-de-prueba` **todo OK** ([`despliegue.md`](./despliegue.md) §7). Anotar si el *pinger* está activo (afecta a R6).
- [ ] **`baseUrl`** en `local.properties` apuntando a Render y la app **recompilada** ([`despliegue-mapa.md`](./despliegue-mapa.md) §2).
- [ ] **Fecha y hora automáticas** en el teléfono: un teléfono con la fecha mal puesta produce reportes que el servidor rechaza (422) y se ven como «No se pudo enviar».
- [ ] **Ubicación precisa** concedida (no «aproximada») y GPS activado.
- [ ] Anotar el **modelo**, la versión de **Android**, la **versión de la app** (Ajustes → Versión) y el **commit**.
- [ ] Anotar el **código anónimo** (Ajustes → «Tu código anónimo»): con él se buscan los reportes en el servidor.
- [ ] **Datos de prueba fuera del estudio**: los reportes de prueba llegan a la **misma base** que los reales. Hacer estas pruebas **antes del 21-sep** o **borrarlos al terminar** (Ajustes → «Borrar todos mis datos», que pide su baja al servidor) y comprobar que en Neon quedan 0 (§6).
- [ ] Para comprobar **cómo se porta la app ante un fallo** sin depender de Render (servidor caído, respuesta perdida, red lenta): el **servidor falso local** (`herramientas/servidor_falso.py`; pasos en [`prueba-en-dispositivo.md`](./prueba-en-dispositivo.md) §2c). **No sirve para el indicador**: sus reportes no llegan a Neon.
- [ ] Para probar **solo la red** sin depender del GPS: compilación de depuración con `-PubicacionSimulada=true` (cada reporte se corre ≈ 33 m; **coordenadas falsas**, solo para prueba; ver [`prueba-en-dispositivo.md`](./prueba-en-dispositivo.md) §2b). El GPS real se prueba aparte (H9 y H10).

## 5. Procedimiento por condición

1. Provocar la condición y comprobar que es la que se quería.
2. **Guardar N reportes.** Propuesta: **N = 40** por condición (ver §7 para el porqué). Con la ubicación simulada no hay duplicados; en campo, ≥ 20 m entre reportes o esperar 30 min (RNF-07).
3. Si la condición lo exige, **restablecer la red** (R3, R4) y esperar la **ventana de espera**.
4. En «Mis reportes» contar *Enviado*, *Pendiente* y *No se pudo enviar*. Si quedan pendientes, anotar el motivo que muestra cada uno.
5. **Ajustes → «Compartir registro de envíos»** y enviarlo a un correo o a un chat: es una tabla con un renglón por reporte (intentos, cuándo se envió, red, motivo del fallo). **No lleva fotos, coordenadas ni el código anónimo.**
6. Contar en Neon los recibidos (§6) y **cotejar**: los «Enviado» de la app deben ser los que hay en el servidor.
7. Limpiar (§4, «Datos de prueba fuera del estudio») y pasar a la siguiente condición.

## 5b. El registro de envíos: columnas y cómo leerlo

**Ajustes → «Compartir registro de envíos»** abre el selector de apps de Android y manda **texto** (a un correo, un chat, una nota…). Empieza con tres líneas `#` (versión de la app, modelo de teléfono con su Android y la hora en que se generó, en UTC), sigue el encabezado y un renglón por reporte, del más antiguo al más nuevo. Con el registro **no** salen fotos, coordenadas ni el código anónimo: para cotejar con Neon basta `id_local`.

| Columna | Qué es |
|---|---|
| `id_local` | El número del reporte en ese teléfono (es el `id_local` de Neon) |
| `capturado_utc` | Cuándo se guardó, en UTC y al segundo (equivale a `fecha_hora` en Neon) |
| `categoria` / `categoria_ia` / `confianza_ia` | La que confirmó el ciudadano; la que sugirió el modelo y con qué confianza (vacías si no hubo sugerencia) |
| `estado` | `PENDIENTE`, `SINCRONIZADO` o `RECHAZADO` (este último solo existe en el teléfono) |
| `intentos` | Cuántas peticiones al servidor **terminaron** (con éxito o con fallo). Un intento interrumpido no se cuenta (§9) |
| `ultimo_intento_utc` | Cuándo **empezó** el último intento |
| `enviado_utc` | Cuándo el servidor **aceptó** el reporte (vacío si aún no) |
| `segundos_hasta_envio` | `enviado_utc` − `capturado_utc`. **Negativo** = el reloj del teléfono estaba mal: se deja a la vista a propósito |
| `motivo_ultimo_fallo` / `codigo_http` | Por qué falló el último intento (ver §8) y el código HTTP si el servidor llegó a contestar |
| `red_al_guardar` / `red_ultimo_intento` | `SIN_RED`, `WIFI`, `DATOS_MOVILES`, `SIN_INTERNET` u `OTRA`, al guardar y al empezar el último intento. Vacía en los reportes hechos con versiones anteriores |

Para tabularlo, guardar el texto como `registro.csv` y, por ejemplo:

```python
import pandas as pd
df = pd.read_csv("registro.csv", comment="#")            # las líneas «#» se saltan
df["enviado"] = df["estado"].eq("SINCRONIZADO")
print(df.groupby("red_al_guardar", dropna=False)["enviado"].agg(enviados="sum", guardados="count"))
```

## 6. Consultas en Neon

Se ejecutan en la consola SQL de Neon (no llevan ningún secreto). Sustituir `<código anónimo>` por el de Ajustes y `<inicio>` por la hora de inicio de la prueba en **UTC** (Perú es UTC−5).

```sql
-- Cuántos reportes recibió el servidor de ese teléfono, y cuánto tardó cada uno
SELECT id_local,
       fecha_hora   AS capturado,
       recibido_en  AS recibido,
       round(extract(epoch FROM recibido_en - fecha_hora)) AS segundos_hasta_recibirse
FROM reporte
WHERE id_anonimo = '<código anónimo>' AND fecha_hora >= '<inicio>'
ORDER BY fecha_hora;

-- Solo el total
SELECT count(*) FROM reporte WHERE id_anonimo = '<código anónimo>' AND fecha_hora >= '<inicio>';

-- Tras «Borrar todos mis datos»: debe dar 0 (las lápidas de la tabla reporte_eliminado se quedan a propósito)
SELECT count(*) FROM reporte WHERE id_anonimo = '<código anónimo>';
```

`fecha_hora` es el reloj del teléfono y `recibido_en` el del servidor, ambos en UTC: si el reloj del teléfono estuviera adelantado o atrasado, la diferencia sale alterada (otro motivo para la fecha automática).

## 7. Cuántos reportes y qué se puede afirmar

Con el umbral del 95 %, el número de fallos que se tolera es `⌊0,05 · N⌋`. **Pasar con pocos reportes no demuestra que la tasa real sea ≥ 95 %**: con N = 20 y 19 enviados (95 %) el intervalo de confianza al 95 % (Wilson) va de 76 % a 99 %, y hay una probabilidad de 39 % de «aprobar» aunque la tasa real fuera 90 %.

| N por condición | Fallos que se toleran | Intervalo de confianza (95 %) si se pasa justo | Probabilidad de aprobar si la tasa real fuera 90 % |
|---:|---:|---|---:|
| 20 | 1 | 76,4 % – 99,1 % | 39 % |
| 40 | 2 | 83,5 % – 98,6 % | 22 % |
| 100 | 5 | 88,8 % – 97,8 % | 6 % |

**Propuesta**: N = 40 por condición como mínimo razonable, **informar siempre el intervalo de confianza junto a la tasa**, y **dejar la decisión final del tamaño de muestra al asesor**. Cuadro de resultados sugerido, uno por condición:

| Fecha | Teléfono y Android | Versión de la app | Condición | Guardados | Enviados (app) | En el servidor | Pendientes | No se pudo enviar (motivos) | Segundos hasta el envío (mediana / peor) | Tasa e IC 95 % | ¿≥ 95 %? | Observaciones |
|---|---|---|---|---:|---:|---:|---:|---|---|---|---|---|

## 8. Cómo leer un fallo

| El registro dice | Qué significa | Qué hacer |
|---|---|---|
| Pendiente · `SIN_CONEXION` | No hay red o no se llega al servidor | Es lo esperado en R3 y R4; comprobar que se envía al restablecer |
| Pendiente · `TIEMPO_AGOTADO` | La red existe pero es lenta, o el servidor gratuito estaba despertando (≈ 1 min) | Esperar un reintento; si se repite con el servidor despierto, es la red (R5) |
| Pendiente · `SERVIDOR_OCUPADO` (5xx, 429) | El servidor falló | Mirar los registros de Render; **no** es fallo de la app |
| Pendiente · `RESPUESTA_INESPERADA` (404, 401, 403…) | Casi seguro una dirección equivocada (`baseUrl`) o un servicio recién desplegado | Revisar `baseUrl` y el despliegue |
| No se pudo enviar · `RECHAZADO_POR_EL_SERVIDOR` (400, 410, 413, 415, 422) | El servidor dijo que **ese** reporte no se acepta | Ver el código: 422 suele ser fecha del teléfono o fuera del distrito; 413, foto enorme. Si parece un error del servidor, **Reintentar** desde «Mis reportes» |
| No se pudo enviar · `FOTO_NO_DISPONIBLE` | La foto ya no está en el teléfono | Eliminar y volver a reportar |
| No se pudo enviar · `ERROR_INTERNO` | Fallo de la app tras 5 intentos | **Es un error nuestro**: guardar el registro y avisar |

## 9. Límites de estas pruebas (dicho sin rodeos)

- **Ahorro de energía**: algunos fabricantes (Xiaomi, Huawei, Samsung en ahorro máximo) detienen las apps en segundo plano y pueden retrasar el envío hasta que se abre la app; por eso la app encola un envío **al abrirse**. Conviene repetir una tanda con el ahorro de energía activado y **anotarlo**.
- **La intensidad de la señal no se mide**: la app distingue Wi-Fi, datos, sin red y sin Internet, no «buena» o «mala». La condición R5 depende de la descripción de quien prueba.
- **Solo un teléfono a la vez** por cada anotación del servidor: el código anónimo identifica el teléfono, no a la persona.
- **El servidor gratuito** (Render) puede tardar ≈ 1 min en despertar: con el *pinger* activo esa parte no se mide (R6 lo aísla).
- **Un envío que no termina retrasa a los demás.** La sincronización manda los reportes **de uno en uno**: con una red que se **cuelga** en vez de fallar, cada reporte espera su turno hasta 90 s (el tiempo de espera de lectura) antes de que se intente el siguiente. Se vio en la tablet con el servidor falso en modo `colgado`. «Enviar ahora» cancela el que está colgado y manda todo al momento. Si en el campo (condición R5) esto pesa, hay que valorar cortar la pasada tras dos tiempos agotados seguidos.
- **Un intento interrumpido no se anota.** Si la app muere a mitad de una subida (cierre forzado, batería), ese intento no queda en `intentos`; el reporte se reenvía solo. Por eso el servidor puede haber recibido más peticiones que intentos anotados.
- **Una petición puede llegar al servidor varias veces dentro de un mismo intento**: cuando la conexión se corta sin respuesta, la librería de red la repite por su cuenta (se vieron tres envíos en el mismo segundo). Es inofensivo (el servidor es idempotente y no duplica), pero **no se debe usar el número de peticiones del servidor como número de intentos**.
- **La espera entre reintentos crece** (30 s, 60 s, 90 s…) con cada pasada fallida: tras muchas seguidas pueden pasar varios minutos entre intentos. «Enviar ahora» la reinicia. En la ventana de espera de 10 minutos (§2) esto puede hacer que un reporte quede «pendiente» por la espera y no por la red: leer `intentos` y `ultimo_intento_utc` antes de culpar a la red.
- Las cifras de la tabla del §7 son de una fórmula estadística estándar (intervalo de Wilson), calculada con un script; el método completo lo decide el asesor.
