# Configurar Google Maps para la app — paso a paso

> Guía para que el destino **Mapa** de la app se vea (no gris). Decisión y motivos: [ADR-0005](./adr/0005-mapa-publico-dentro-de-la-app-con-google-maps.md). Es hermana de [`despliegue.md`](./despliegue.md) (Neon, Cloudinary y Render).

## 0. Reglas de seguridad (las mismas que en `despliegue.md`)

| Regla | Por qué |
|---|---|
| **La clave de Google Maps es un secreto con consecuencias económicas**: nunca en el chat, en git, en capturas ni en la documentación. | Una clave sin restringir que se filtre puede generar **cargos a tu cuenta de Google**. |
| **La escribes tú**, solo en `local.properties` (en la raíz del proyecto). Ese archivo **ya está ignorado por git**. | Claude no crea cuentas ni maneja credenciales. |
| La clave se **restringe** (paquete + huella SHA-1 + solo la API de Maps para Android). | Va dentro del APK: lo que la protege es la restricción, no el secreto. |
| Si la pegaste donde no debías: en la consola de Google Cloud, **regenérala** (y borra la vieja) y actualiza `local.properties`. | Un secreto publicado hay que darlo por perdido. |

## 1. En Google Cloud (lo haces tú)

Los nombres de los menús pueden variar un poco; los pasos son los de la [documentación de Google](https://developers.google.com/maps/documentation/android-sdk/get-api-key).

1. Entra en [console.cloud.google.com](https://console.cloud.google.com) y **crea o elige un proyecto** (p. ej. `wanchaq-limpio`).
2. **Facturación**: vincula una cuenta de facturación al proyecto. **Google la exige** para usar Maps aunque el uso sea bajo, y **el costo a esta escala no se pudo confirmar** en su tabla de precios: por eso, en *Facturación → Presupuestos y alertas*, **crea un presupuesto pequeño con alertas por correo** (p. ej. al 50 %, 90 % y 100 %).
3. **APIs y servicios → Biblioteca** → busca **"Maps SDK for Android"** → **Habilitar**.
4. **APIs y servicios → Credenciales → Crear credenciales → Clave de API**.
5. Edita esa clave y **restríngela**:
   - **Restricciones de la aplicación** → *Apps para Android* → **Añadir**: nombre de paquete `com.wanchaqlimpio.app` y la **huella SHA-1** de tu firma de depuración (paso 2 de la sección siguiente).
   - **Restricciones de la API** → *Restringir clave* → marca solo **Maps SDK for Android**.
6. Copia la clave (**no la pegues en el chat**).

## 2. En tu computadora

1. **Huella SHA-1 de depuración** (es pública, no es un secreto): en Android Studio, panel **Gradle** → `Wanchaq_Limpio` → `app` → *Tasks* → *android* → **signingReport**; busca la variante `debug` y copia la línea `SHA1`. También sirve `./gradlew signingReport` en una terminal. *(Es la de **tu** PC: en otro equipo cambia y hay que añadirla a la restricción.)*
2. Abre `local.properties` (raíz del proyecto, junto a `sdk.dir`) y añade **una línea por valor, sin comillas ni espacios**:

   ```properties
   MAPS_API_KEY=(tu clave)
   baseUrl=https://TU-SERVICIO.onrender.com/
   ```

   - `MAPS_API_KEY`: la clave del paso 6 de arriba.
   - `baseUrl`: la dirección de tu servidor en Render (no es secreta). **Sin ella, la compilación de depuración apunta al emulador** (`http://10.0.2.2:8000/`) y un teléfono real no llegaría al servidor. Termina con `/`.
3. Vuelve a **compilar e instalar** (Android Studio → Run). Los cambios en `local.properties` **solo se aplican al volver a compilar**.

## 3. Comprobar que funciona

**Paso previo: que Render tenga el endpoint del mapa.** Abre en el navegador `https://TU-SERVICIO.onrender.com/puntos-criticos` (la primera vez puede tardar ≈ 1 min): debe mostrar `{"type": "FeatureCollection", "features": []}`. Si da **404**, todavía no desplegaste la versión que lo trae (`git push` y esperar a que Render construya): en la app verías «El servidor tuvo un problema» con *Reintentar* y **el mapa no se dibujaría**, aunque la clave esté bien.

1. Abre la app y toca **Mapa** (icono de alfiler, primera pestaña de la barra).
2. Debes ver el **mapa de Wanchaq** (calles, no un fondo gris). Con la app recién instalada, sin ningún foco confirmado, aparece además la tarjeta **"Aún no hay focos confirmados"**: es lo **normal** (ver ADR-0005: hoy nada produce focos confirmados).
3. Si ves un **aviso rojo** arriba diciendo que falta la clave, `MAPS_API_KEY` no llegó a la compilación (línea mal escrita o no se recompiló).

## 4. Si el mapa sale gris

**Si en vez del mapa ves «El servidor tuvo un problema»**, no es un problema de la clave: la app no pudo leer la lista de focos (Render no tiene todavía `/puntos-criticos`, está dormido o está caído). Prueba esa dirección en el navegador (sección 3, paso previo) y toca *Reintentar*: el mapa solo se dibuja cuando la lista llega.

Si el mapa se ve **gris** (o sin calles):

| Causa probable | Qué revisar |
|---|---|
| Falta la clave | Aparece el aviso rojo. Comprueba `MAPS_API_KEY=` en `local.properties` y recompila. |
| La restricción no coincide | Nombre de paquete exacto `com.wanchaqlimpio.app` y la SHA-1 **de esta computadora** (debug). |
| API no habilitada | *APIs y servicios → Biblioteca → Maps SDK for Android* debe decir "Habilitada". |
| Sin facturación | El proyecto debe tener una cuenta de facturación vinculada. |
| La clave está restringida a otra API | Debe permitir **Maps SDK for Android**. |
| Cambios recientes | Los cambios de restricciones en Google pueden tardar unos minutos. |

Para ver el motivo exacto: Android Studio → **Logcat** → filtra por `Google Maps Android API` o `Authorization failure`. **No pegues en el chat** líneas de Logcat que contengan tu clave; la salida de un error de autorización normalmente no la incluye, pero revísalo antes.

## 5. Para publicar (Módulo 9)

- `assembleRelease` y `bundleRelease` **fallan** si falta `MAPS_API_KEY` o si `baseUrl` no es `https://`: es a propósito, para no publicar una app con el mapa gris o apuntando al emulador.
- La clave necesita la **huella SHA-1 de la firma de publicación** en su restricción (o la de *Play App Signing*, que Google Play muestra en la consola de Play). Sin ella, el mapa **funciona en depuración pero sale gris en la app publicada**. Está en la tarea E3.
- Antes de publicar, confirma el presupuesto y las alertas de facturación (paso 1.2).
