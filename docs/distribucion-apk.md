# Distribuir el APK al equipo (antes de Play Store)

> Para repartir la app **directamente** a tu equipo (sin pasar por Play Store) y empezar a recoger datos y a medir la confiabilidad ([`pruebas-de-confiabilidad.md`](./pruebas-de-confiabilidad.md)). Play Store viene después, con la app terminada (Módulo 9, sección E de [`avance.md`](./avance.md)). Es hermana de [`despliegue.md`](./despliegue.md) (servidor) y [`despliegue-mapa.md`](./despliegue-mapa.md) (clave de Google Maps).

## 0. Reglas de seguridad (las de siempre)

| Regla | Por qué |
|---|---|
| **La contraseña de la llave de firma la escribes tú**, solo en `local.properties` y en tu gestor de contraseñas. **Nunca** en el chat, en git ni en la documentación. | Quien tenga la llave y su contraseña puede firmar una app falsa como si fuera la tuya. |
| **El archivo de llave (`.jks`) va fuera del repositorio** y con **copia de seguridad en dos sitios**. `.gitignore` ya ignora `*.jks` y `*.keystore`. | **Si la pierdes, los teléfonos que ya tienen la app no podrán actualizarla**: habría que desinstalarla y se perderían sus reportes pendientes. |
| El APK lleva dentro la clave de Google Maps. Lo que la protege es su **restricción** (paquete + huella SHA-1 + solo Maps SDK for Android). | Ver [`despliegue-mapa.md`](./despliegue-mapa.md) §0. |

## 1. Una sola vez (lo haces tú)

1. **Servidor listo**: `git push` → Render construye → `verificar_servicios.py --remoto URL --reporte-de-prueba` **todo OK** → `https://TU-SERVICIO.onrender.com/puntos-criticos` responde una lista vacía ([`despliegue.md`](./despliegue.md) §7). Sin esto el APK guardaría reportes pero **no podría enviarlos**.
2. **`baseUrl`** en `local.properties` (no es un secreto; termina con `/`; **tiene que ser `https://`** o `assembleRelease` falla a propósito):
   ```properties
   baseUrl=https://TU-SERVICIO.onrender.com/
   ```
3. **`contactEmail`** en `gradle.properties` (el de la raíz del proyecto; es **público**: sale en Ajustes y es el correo al que el ciudadano escribe para ejercer sus derechos, Ley N.º 29733). Usa una dirección **dedicada al proyecto**, no una personal:
   ```properties
   contactEmail=correo-del-proyecto@ejemplo.com
   ```
4. **Crear la llave de firma** (una vez). En una terminal (Android Studio trae `keytool`; si no está en el PATH, usa `"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"`):
   ```bash
   keytool -genkeypair -v -keystore C:\Users\romel\llaves\wanchaq-limpio-release.jks -alias wanchaq -keyalg RSA -keysize 2048 -validity 10000
   ```
   Te pide la contraseña (**dos veces**: la del archivo y la de la llave; puedes usar la misma) y unos datos (nombre, organización…: pon lo que quieras). **Anota la contraseña en tu gestor y copia el `.jks` a otro sitio** (un USB, tu nube personal cifrada…).
5. Añade en `local.properties` **cuatro líneas** (ruta con `/`, sin comillas ni espacios; **la contraseña la escribes tú**):
   ```properties
   RELEASE_STORE_FILE=C:/Users/romel/llaves/wanchaq-limpio-release.jks
   RELEASE_STORE_PASSWORD=(tu contraseña)
   RELEASE_KEY_ALIAS=wanchaq
   RELEASE_KEY_PASSWORD=(tu contraseña)
   ```
6. **Añadir la huella de esta llave a la clave de Google Maps** (si no, el mapa sale **gris** en el APK repartido). Obtén la SHA-1 (es pública, no un secreto; te pide la contraseña del `.jks`):
   ```bash
   keytool -list -v -keystore C:\Users\romel\llaves\wanchaq-limpio-release.jks -alias wanchaq
   ```
   Copia la línea `SHA1:` y, en Google Cloud → *APIs y servicios → Credenciales* → tu clave → *Restricciones de la aplicación → Apps para Android*, **añade otra entrada**: paquete `com.wanchaqlimpio.app` + esa SHA-1 (**deja también la de depuración**). Los cambios pueden tardar unos minutos.

## 2. Compilar cada versión

```bash
./gradlew assembleRelease -PversionCode=2 -PversionName=1.0
```

- **`-PversionCode` sube en cada APK que repartas** (2, 3, 4…): Android no instala encima una versión con el mismo número o menor. Si no lo pones, vale 1.
- El APK queda en `app/build/outputs/apk/release/app-release.apk`. Cópialo con un nombre que diga qué es (la carpeta `distribucion/` está ignorada por git) y saca su suma de control para que el equipo compruebe que el archivo llegó entero:
  ```bash
  copy app\build\outputs\apk\release\app-release.apk distribucion\wanchaq-limpio-1.0-v2.apk
  certutil -hashfile distribucion\wanchaq-limpio-1.0-v2.apk SHA256
  ```
- Si falta algo, la compilación **falla con un mensaje que dice qué**: la llave, `baseUrl` (https), `contactEmail`, `MAPS_API_KEY`, o la ubicación simulada activada (que **nunca** debe llegar a un APK de publicación).
- Esta versión es la **normal, con el GPS real**: es la que produce **datos válidos** (la app exige ≤ 15 m de error y estar dentro de Wanchaq). La de **ubicación simulada** solo existe como compilación de **depuración** (`assembleDebug -PubicacionSimulada=true`, firmada con la llave de depuración de tu PC): sirve para probar el flujo desde casa con **coordenadas falsas**. Márcala con un nombre claro y **no la mezcles con la que da datos válidos**.

## 3. Probar tú antes de repartir (10 minutos)

Instala el APK en tu tablet o teléfono (`adb install -r …`; si ya hay una versión firmada con **otra** llave —p. ej. la de depuración— hay que **desinstalarla antes**, y se pierden sus datos) y comprueba, **con el servidor real**:

- [ ] Ajustes → **Versión** dice `1.0` (sin `-ubicacion-simulada`) y el correo de contacto sale sin el aviso rojo.
- [ ] Consentimiento → permisos (cámara y ubicación **precisa**) → **Reportar**: el aviso de GPS pasa a «GPS listo» (en exteriores) → foto → confirmar → guardar.
- [ ] En «Mis reportes» pasa a **«Enviado»** y en Neon aparece **1** fila (consulta de [`pruebas-de-confiabilidad.md`](./pruebas-de-confiabilidad.md) §6).
- [ ] **Mapa** dibuja el mapa (no gris) con la llave de firma nueva.
- [ ] Ajustes → **Compartir registro de envíos** abre el selector.

## 4. Repartirlo (tú) e instalarlo (el equipo)

Lo subes a Drive o lo mandas por chat (**eso lo haces tú**). Cada persona necesita **Android 8.0 o superior, con cámara, GPS y servicios de Google Play** (sin ellos no funcionan el mapa ni la ubicación). Pasos para instalar:

1. Abrir el archivo `.apk` en el teléfono. Si Android lo pide, permitir **«Instalar apps desconocidas»** a la aplicación desde la que se abrió (Drive, Chrome, WhatsApp…).
2. Si aparece un aviso de **Play Protect**, elegir **«Instalar de todos modos»** (la app no está en la tienda: es lo esperado).
3. Abrirla, leer y **aceptar el consentimiento**, y dar permiso de **cámara** y de ubicación **«precisa»** (la «aproximada» no sirve: el filtro pide ≤ 15 m).
4. **Para actualizar**: instalar el APK nuevo encima (misma llave, `versionCode` mayor). **Se conservan los reportes y los pendientes.**

## 5. Qué pedirle al equipo y cómo llegan los datos

- **Datos válidos** = reportes hechos con esta versión, **en exteriores dentro de Wanchaq**. Sin cielo abierto el GPS casi nunca llega a 15 m: la app espera hasta 30 s y, si no, ofrece «Reintentar ubicación» sin perder la foto.
- **Pruebas de confiabilidad**: seguir [`pruebas-de-confiabilidad.md`](./pruebas-de-confiabilidad.md) (escenarios H de [`prueba-en-dispositivo.md`](./prueba-en-dispositivo.md)). Al terminar cada tanda, cada persona manda **Ajustes → «Compartir registro de envíos»** a un correo tuyo y anota **modelo, Android y condición de red**.
- **Cada persona anota su código anónimo** (Ajustes → «Tu código anónimo») en una hoja **privada**: así distingues después los reportes de **prueba** de los de **ciudadanos** y puedes borrarlos. Para borrar los suyos: *Ajustes → «Borrar todos mis datos»* (pide la baja al servidor) o, en Neon, por `id_anonimo`.
- **Ética**: no fotografiar personas, rostros, placas ni interiores de viviendas.

## 6. Lo que no está resuelto (dicho sin rodeos)

- **La precisión del GPS de cada reporte no se guarda** (solo se usa para filtrar): los reportes que se recojan ahora **no la tendrán, y no se puede recuperar después**. El plan de confiabilidad de los instrumentos la pide ([`plan-confiabilidad-instrumentos.md`](./plan-confiabilidad-instrumentos.md) §9, I-1; tarea G9): **decidirlo antes de repartir el APK**.
- **Sin probar**: el **GPS real** (C8, H9), un **teléfono** (solo se probó la tablet), girar la pantalla, el modo avión y todo **contra Render**. Los primeros reportes del equipo son también estas pruebas.
- **La API es abierta** (sin login, por diseño: ADR-0001): quien tenga la dirección puede enviar o pedir bajas. No publiques la dirección del servidor; la limitación de uso está pendiente (A12).
- **El servidor gratuito se duerme** a los 15 minutos: la primera petición tarda ≈ 1 min. Con el equipo usándolo conviene el *pinger* ([`despliegue.md`](./despliegue.md) §8).
- **Play Store, después**: con *Play App Signing* Google firma con **su** llave; los teléfonos que instalaron este APK tendrían que **desinstalarlo** para pasar a la versión de la tienda, salvo que al crear la app en Play Console **importes esta misma llave**. Además habrá que añadir a la clave de Maps la SHA-1 que muestra Play (tarea E3).
- **Cambiar de llave** después de repartir obliga a desinstalar en todos los teléfonos. Por eso conviene crearla bien y guardarla bien **ahora**.
