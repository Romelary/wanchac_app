# Despliegue del backend — Neon + Cloudinary + Render

> Guía paso a paso para dejar funcionando la infraestructura del backend de Wanchaq Limpio: la base de datos (Neon), las fotos (Cloudinary) y el servidor (Render). Es también evidencia de tesis: registra **qué se configuró, por qué así y cómo se comprobó**. Decisiones técnicas: [ADR-0004](./adr/0004-backend-django-sin-geodjango.md). Diseño general: [`diseno.md`](./diseno.md) §4.1.
>
> Estado (2026-09-18): el **esqueleto** del backend existe y está probado (100 pruebas); **Neon y Cloudinary están verificados con cuentas reales** (§11, evidencia en [`evidencia/`](./evidencia/2026-09-18_verificacion-neon-cloudinary.txt)). **Falta desplegarlo en Render** (§7) y el pinger (§8). La lógica de reportes (Fase 1) viene después.

## 0. Reglas de seguridad (léelas primero)

| Regla | Por qué |
|---|---|
| **Nunca pegues en el chat, en la documentación, en capturas ni en commits** una contraseña, la cadena `postgresql://…`, la `CLOUDINARY_URL`, la clave o el secreto de API, ni el `HEALTH_TOKEN`. | El chat y los commits se guardan; un secreto publicado hay que darlo por perdido y **cambiarlo**. |
| **Los secretos los escribes tú**, en dos únicos sitios: el archivo `backend/.env` (tu computadora) y el panel de Render (variables de entorno). | Claude (el asistente) no crea cuentas ni maneja credenciales: si un paso te pide una, es tuyo. |
| `backend/.env` **no se sube a git** (está en `.gitignore`, y una prueba automática lo vigila). `.env.example` sí se sube, pero solo lleva marcadores de posición. | Para que el repositorio pueda ser revisado sin exponer nada. |
| Activa la **verificación en dos pasos** en las tres cuentas (y en GitHub). | Es el mayor punto débil de una infraestructura gratuita: si te roban la cuenta, controlan tus datos. |
| El script de verificación **solo imprime OK/FALLO/AVISO/INFO**, nunca valores. La única cifra parcial: cuando Cloudinary falla muestra los **4 últimos dígitos de la clave de API** (un identificador que sin el secreto no autentica nada) para reconocerla en la consola; el secreto jamás se imprime. Puedes pegar su salida completa en el chat. | Está diseñado y probado para eso (ver `pruebas.md`). |

**Si un secreto se filtra** (lo pegaste donde no debías, se subió a git): no basta con borrarlo, hay que **cambiarlo**: Neon → restablecer la contraseña del rol; Cloudinary → generar un par de claves nuevo y revocar el anterior; Render → editar la variable; y actualizar tu `.env`. En git, un secreto subido queda en el historial aunque luego se borre el archivo.

## 1. Cómo encajan las piezas

```mermaid
flowchart LR
    APP["App Android<br/>(offline-first)"] -->|"POST /reportes<br/>(HTTPS)"| RENDER["Render<br/>API Django<br/>plan Free"]
    PING["cron-job.org<br/>cada 10 min"] -->|"GET /health"| RENDER
    RENDER -->|"SQL + PostGIS"| NEON[("Neon<br/>PostgreSQL")]
    RENDER -->|"fotos privadas<br/>(API firmada)"| CLOUD[("Cloudinary")]
```

- La **app nunca habla con Neon ni con Cloudinary**: solo con el servidor de Render. Por eso no hay claves de servicios en la app.
- **`/health` no toca la base de datos**: lo pincha un monitor cada 10 min para que Render no se duerma; si tocara Neon, cada pinchazo lo despertaría y gastaría su cuota de horas de cómputo sin ningún beneficio.
- Render (plan gratuito) **borra su disco en cada reinicio**: por eso los datos van a Neon y las fotos a Cloudinary; el servidor no guarda nada localmente.

## 2. Límites del plan gratuito (verificados en la documentación oficial el 2026-09-18)

| Servicio | Límite relevante | Qué implica para el proyecto |
|---|---|---|
| **Neon** Free | 0,5 GB de almacenamiento por proyecto · 100 horas de cómputo (CU-h) al mes · el cómputo se **duerme a los 5 min** sin consultas · 5 GB de transferencia | Sobra para texto y coordenadas de ~cientos/miles de reportes. Cada despertar tarda unos segundos: por eso `connect_timeout` = 15 s. |
| **Render** Free | **0,1 CPU y 512 MB de RAM** · se **duerme a los 15 min** sin tráfico y tarda **~1 min** en despertar · 750 horas de instancia al mes por cuenta (≈ un solo servicio activo 24/7) · disco efímero · sin SSH | Un solo servicio web, con un solo proceso de servidor. El pinger lo mantiene despierto (744 h en un mes de 31 días < 750 h). Con 0,1 CPU no conviene cargar librerías pesadas (p. ej. scikit-learn) en el servidor: ver ADR-0004. |
| **Cloudinary** Free | 25 créditos al mes (1 crédito = 1 000 transformaciones **o** 1 GB de almacenamiento **o** 1 GB de ancho de banda) | Una foto de ~3 MB × 1 000 reportes ≈ 3 GB ≈ 3 créditos. No es el cuello de botella, pero conviene reducir la foto antes de subirla (pendiente, ver §9). |

> **Para la tesis (limitaciones)**: la infraestructura gratuita puede dormirse o agotar su cuota; el diseño offline-first de la app hace que ningún reporte se pierda por ello (queda en el teléfono y se reintenta). El riesgo sobre el indicador de sincronización está en `diseno.md` §4.1.

## 3. Preparar tu computadora

Requisitos: **Python 3.12 o superior** (Django 6.1 lo exige; el proyecto se probó con 3.12.2) y Git.

```powershell
cd C:\Users\romel\wanchac_app\backend
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe manage.py test
```

La última línea debe terminar en `OK` (100 pruebas; no usan red ni tus claves). Se usa siempre `.\.venv\Scripts\python.exe` para no tener que "activar" el entorno (PowerShell suele bloquear el script de activación).

Luego crea tu archivo de variables:

```powershell
Copy-Item .env.example .env
notepad .env
```

Guárdalo como **UTF-8** (Bloc de notas → Guardar como → Codificación: UTF-8). No lo crees con `>` de PowerShell: sale en UTF-16 y el programa avisará que no puede leerlo. Los valores van **sin comillas**.

Genera el `HEALTH_TOKEN` (un secreto aleatorio que solo tú conoces) y pégalo en `.env`:

```powershell
.\.venv\Scripts\python.exe -c "import secrets; print(secrets.token_urlsafe(32))"
```

## 4. Neon (base de datos)

1. Entra en [console.neon.tech](https://console.neon.tech) y activa la verificación en dos pasos de tu cuenta.
2. **Create project** → nombre `wanchaq-limpio` → versión de Postgres: la que venga por defecto → **región: AWS US East 1 (N. Virginia)**.
   - ⚠️ **La región no se puede cambiar después** (documentación de Neon). Si ya creaste el proyecto en otra región y aún está vacío, bórralo y créalo de nuevo. Se elige la costa este de EE. UU. porque es la más cercana a Perú entre las regiones de Render (Virginia u Ohio). **Neon y Render deben estar en la misma zona geográfica**: así cada consulta a la base tarda milisegundos; entre zonas lejanas, decenas o cientos de ellos, y un reporte hace varias consultas.
   - Si eliges otra zona, cambia `region:` en [`render.yaml`](../render.yaml) a una de la misma zona: `virginia` u `ohio` (costa este), `oregon` (oeste), `frankfurt` o `singapore`. Render **no documenta** a qué región de AWS equivale cada una, por eso se compara por **zona** y no por nombre exacto; `verificar_servicios.py` imprime `INFO … misma zona` o un `AVISO` si no coinciden (lo lee del nombre del servidor de Neon, sin mostrar el resto de la cadena).
3. **SQL Editor** (menú lateral) → ejecuta:

   ```sql
   CREATE EXTENSION IF NOT EXISTS postgis;
   SELECT postgis_version();
   ```

   La segunda línea debe devolver un número de versión (3.x).
4. Botón **Connect** → **desactiva** el interruptor *Connection pooling* (necesitas la conexión **directa**: el host no lleva `-pooler`) → copia la cadena `postgresql://…`. **Contiene tu contraseña.**
5. Pégala en `backend/.env`, sin comillas:

   ```
   DATABASE_URL=postgresql://…(la cadena copiada)…
   ```

**Por qué la directa y no la *pooled***: las migraciones y `CREATE EXTENSION` deben usar conexión directa (recomendación de Neon), y con 1 proceso de servidor no necesitamos el agrupador de conexiones. El script de verificación **avisa** si detecta `-pooler`.

**El *prompt* para agentes de IA que la consola de Neon muestra** (`npm i -g neon`, `neon login`, `neon skills`, `neon mcp`, `neon link`, `neon config init`, `neon deploy`): **no lo uses en este proyecto**. Es el asistente de Neon para conectar un agente de IA a tu cuenta y no hace falta, porque de Neon solo necesitamos **PostGIS y la cadena de conexión directa** (pasos 3–5). Según la documentación de Neon:

- `neon mcp` escribe la configuración de tus agentes de IA para que operen **directamente sobre tu cuenta de Neon** (autenticación incluida). Contradice la regla de §0: el asistente no maneja credenciales.
- `neon deploy` aplica cambios al proyecto y **copia credenciales a un archivo local** (`.env.local`).
- `neon config init` añade `neon.ts` y dependencias de Node a un repositorio que es Kotlin + Django.
- `neon login` abre tu navegador para iniciar sesión: eso solo lo haces tú.

Fuentes: [neon mcp](https://neon.com/docs/cli/mcp), [neon skills](https://neon.com/docs/cli/skills), [Just landed in the Neon CLI](https://neon.com/blog/just-landed-in-the-neon-cli). El identificador de proyecto que aparece en ese prompt no es un secreto, pero tampoco hace falta compartirlo.

## 5. Cloudinary (fotos)

1. Entra en la consola de Cloudinary y activa la verificación en dos pasos.
2. Icono de **Settings** (barra lateral) → **API Keys**. Ahí verás el *Cloud name*, la *API Key* y el *API Secret*.
3. Recomendado: **genera un par de claves nuevo solo para el backend** (Cloudinary permite varios pares) y ponle un nombre reconocible, p. ej. `render-backend`. Así, si algún día se filtra, lo revocas sin tocar nada más.
4. En `backend/.env`, sin comillas y con el formato `cloudinary://CLAVE:SECRETO@NOMBRE_DE_NUBE`:

   ```
   CLOUDINARY_URL=cloudinary://…(tu clave)…:…(tu secreto)…@…(tu nombre de nube)…
   ```

   **Ojo con la plantilla**: la consola de Cloudinary muestra `CLOUDINARY_URL=cloudinary://<your_api_key>:<your_api_secret>@tu_nube`. Los textos entre `< >` son **marcadores**: hay que reemplazarlos por tu clave y tu secreto reales **y quitar también los símbolos `< >`**. La línea de `.env` lleva el nombre **una sola vez**: `CLOUDINARY_URL=cloudinary://…` (no `CLOUDINARY_URL=CLOUDINARY_URL=cloudinary://…`, error real que ocurrió al copiar la línea de la consola) y sin guion bajo delante. El *cloud name* (lo que va tras la `@`) **no es secreto**: aparece en todas las URL de tus imágenes; la clave y el secreto sí. El secreto suele mostrarse oculto en la consola: revélalo desde allí o genera un par nuevo. El verificador reconoce la plantilla sin rellenar y lo dice.
5. **No crees *upload presets* "sin firmar"**: permitirían a cualquiera subir a tu cuenta. La app Android no sube a Cloudinary; sube al backend, que sube con firma.
6. Las fotos de los reportes se guardarán **privadas** (`type=authenticated`, decisión de la Fase 1): su URL pública no las sirve, solo una URL firmada. Es coherente con el consentimiento (Anexo 9) y con `diseno.md` §4.2. Que tu cuenta respeta ese modo se comprueba desde ya con `--subida` (siguiente sección).

## 6. Verificar tu configuración local

```powershell
.\.venv\Scripts\python.exe verificar_servicios.py --subida
```

Qué hace, en orden, y qué debe verse:

| Línea | Significa |
|---|---|
| `OK backend/.env encontrado` y `OK … definida` (×3) | Tus variables están cargadas (no muestra sus valores). |
| `OK Neon: conectado; PostGIS 3.x operativo` | Llegas a la base, con TLS, y PostGIS mide en metros. |
| `OK Cloudinary: credenciales aceptadas` | La clave y el secreto son válidos. |
| `OK subida aceptada y guardada como privada (type=authenticated)` | Cloudinary respetó el modo privado. |
| `OK la URL pública NO sirve la foto (HTTP 404)` | **La prueba clave**: una foto de un reporte no es accesible por dirección pública. |
| `OK la URL firmada SÍ sirve la foto` | El backend/administrador sí podrá verla. |
| `OK recurso de prueba borrado` | No queda basura (sube un PNG de 1×1 px y lo borra siempre, incluso si algo falla). |
| `RESULTADO: TODO OK` | Listo para el siguiente paso. |

Puedes **pegar la salida completa en el chat**: nunca imprime valores ni mensajes de error de los servicios (solo el nombre de la clase de la excepción). Si algo falla, la línea `FALLO` dice qué revisar; más abajo hay una tabla de problemas (§10).

## 7. GitHub y Render (servidor)

Render construye el servicio desde un repositorio de GitHub, y el proyecto todavía **no tiene repositorio remoto**.

1. En GitHub crea un repositorio **privado** (p. ej. `wanchaq-limpio`), **vacío** (sin README ni `.gitignore`). Antes de subir se escaneó **todo el historial** (2026-09-18, 20 commits): no hay contraseñas, cadenas de conexión, claves privadas ni archivos sensibles (`.env`, `.jks`, `.keystore`, `local.properties`, `google-services.json`…), y todos los commits usan el correo `noreply` de GitHub (no exponen tu correo personal). Ese escaneo vale para esa fecha: no pegues secretos en commits futuros.
2. En una terminal, dentro de `C:\Users\romel\wanchac_app`:

   ```powershell
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/wanchaq-limpio.git
   git push -u origin main
   ```

   Se abrirá una ventana del navegador para iniciar sesión en GitHub (Git Credential Manager). **No pegues ningún token en el chat.** Se pasa la rama a `main` porque así lo asume `plan.md` (PR hacia `main`).
3. Antes de crear nada en Render, corre otra vez `verificar_servicios.py` y mira la línea de **región**: `INFO … misma zona` = bien; un `AVISO` = corrige `region:` en `render.yaml` **antes** de desplegar (una vez creado el servicio, su región tampoco se puede cambiar).
4. En [dashboard.render.com](https://dashboard.render.com) (con verificación en dos pasos): **New > Blueprint** → junto a tu repositorio pulsa **Connect** (la primera vez autoriza a Render en GitHub **solo sobre este repositorio**) → en el formulario pon un nombre (p. ej. `wanchaq-limpio`), elige la rama `main` y deja vacío *Blueprint Path* (usa `render.yaml`) → Render muestra la lista de cambios (el servicio `wanchaq-limpio-api`, plan **Free**) → revísala ([documentación de Render](https://render.com/docs/infrastructure-as-code)).
5. Render te pedirá el valor de las variables marcadas `sync: false`. Escríbelas **tú**, en el panel:
   - `DATABASE_URL` → la misma cadena de Neon de tu `.env`.
   - `CLOUDINARY_URL` → la misma de tu `.env`.
   - `HEALTH_TOKEN` → **el mismo** valor que pusiste en tu `.env`.

   `SECRET_KEY` la genera Render sola (no hace falta conocerla) y `DEBUG` **no** se define (por defecto está apagado). Si Render llegara a pedir un método de pago para un servicio del plan gratuito, es una decisión tuya: confírmalo en su documentación antes de aceptar.
6. Pulsa **Deploy Blueprint**. La primera construcción tarda unos minutos (instala las dependencias, que están todas disponibles ya compiladas para Linux: se comprobó en PyPI el 2026-09-18) y luego Render consulta `/health` (con el `Host` del subdominio `onrender.com` del servicio, que tu `ALLOWED_HOSTS` permite gracias a `RENDER_EXTERNAL_HOSTNAME`). Cuando termine, el panel muestra la URL pública del servicio (algo como `https://wanchaq-limpio-api.onrender.com`).
7. Comprueba el servicio **desplegado** (usa tu `HEALTH_TOKEN` del `.env`):

   ```powershell
   .\.venv\Scripts\python.exe verificar_servicios.py --remoto https://TU-SERVICIO.onrender.com
   ```

   Verás `OK /health responde (N s)`; si tardó más de 15 s, avisa de que fue el arranque en frío (esperado). Luego `OK base_de_datos (en Render): …` y `OK cloudinary (en Render): …`: eso prueba que **el servidor desplegado**, y no solo tu computadora, llega a Neon y a Cloudinary. El token viaja solo en una cabecera HTTPS, nunca en la URL.

## 8. Mantener el servicio despierto (pinger)

Render duerme el servicio a los 15 min. Para que no ocurra durante la ventana de observación (motivo y riesgo metodológico: `diseno.md` §4.1):

1. Crea una cuenta en [cron-job.org](https://cron-job.org) (gratuita).
2. **Create cronjob** → *URL*: `https://TU-SERVICIO.onrender.com/health` → *Schedule*: cada **10 minutos** → activa el aviso por correo si falla.
3. **Solo `/health`.** Nunca `/health/servicios`: exige token y tocaría Neon en cada llamada.

No hace falta activarlo hasta que la app empiece a usarse de verdad; mientras desarrollas, un arranque en frío de ~1 min es tolerable. **Decláralo en la metodología** (limitaciones), como ya prevé `diseno.md` §4.1.

## 9. Pendientes que esta infraestructura deja para la Fase 1

- **Tiempos de espera del cliente Android**: OkHttp usa 10 s por defecto y un arranque en frío de Render tarda ~60 s; la primera sincronización fallaría (WorkManager reintenta, pero conviene subir los tiempos del cliente de sincronización a ~60 s). Cambio en `NetworkModule.kt`, junto con la Fase 1.
- **Reducir la foto antes de subirla** (p. ej. lado largo 1 600 px, JPEG 85): menos datos móviles del ciudadano, subidas que no fallan por tiempo y menos cuota de Cloudinary. Decidir con la Fase 1.
- **Migraciones y modelo de datos**: se crearán en la Fase 1 (tabla `reporte` con columna `geography(Point,4326)` generada e índice GiST; ver ADR-0004). Por eso el `buildCommand` aún no ejecuta `migrate`.
- **Rama de desarrollo en Neon**: Neon permite ramas de la base; cuando haya datos reales conviene desarrollar contra una rama, no contra la de producción.

## 10. Si algo falla

| Síntoma (línea que verás) | Causa probable | Qué hacer |
|---|---|---|
| `los ajustes no arrancan: Falta la variable de entorno SECRET_KEY` | En tu `.env` falta `DEBUG=1` (en local no hace falta `SECRET_KEY` con `DEBUG=1`). | Añade `DEBUG=1` a `backend/.env`. En **Render** nunca lo pongas. |
| `los ajustes no arrancan: backend/.env no está guardado como UTF-8` | El archivo está en UTF-16 (creado con `>`). | Ábrelo en el Bloc de notas y guárdalo como UTF-8. |
| `FALLO no existe backend/.env` | No copiaste la plantilla. | `Copy-Item .env.example .env` y rellénalo. |
| `FALLO Neon: no se pudo conectar (OperationalError)` | Contraseña o host mal copiados; comillas sobrantes; proyecto pausado; o sin internet. | Vuelve a copiar la cadena desde **Connect** (con *Connection pooling* apagado). Tarda hasta 15 s en rendirse (límite de conexión). |
| `FALLO Neon: … PostGIS no está habilitado` | Falta la extensión. | En el **SQL Editor** de Neon: `CREATE EXTENSION IF NOT EXISTS postgis;`. |
| `FALLO Cloudinary: CLOUDINARY_URL ausente o mal formada` | Falta la variable o no tiene el formato `cloudinary://clave:secreto@nube`. | Revisa que no haya espacios ni comillas. |
| `FALLO Cloudinary: CLOUDINARY_URL mal formada: debe empezar por cloudinary://` | La línea repite `CLOUDINARY_URL=` dentro del valor, empieza por `https://` o el valor incluye comillas. | En `.env` debe quedar `CLOUDINARY_URL=cloudinary://clave:secreto@nube`, con el nombre una sola vez y sin comillas (§5). |
| `FALLO Cloudinary: CLOUDINARY_URL todavía tiene una plantilla` | Se dejó `<your_api_key>`/`<your_api_secret>` de la consola o los marcadores de `.env.example`. | Reemplázalos por los valores reales, **sin los símbolos `< >`** (§5). |
| `FALLO Neon: DATABASE_URL todavía tiene los marcadores de .env.example` | Se dejó `USUARIO:CONTRASENA@HOST_DE_NEON…`. | Pega la cadena real de Neon (§4). |
| `FALLO Cloudinary: … (401 AuthorizationRequired): Cloudinary no conoce esa clave de API` | La clave no existe: mal copiada, de otra cuenta, o del par que se borró. | Copia la **clave** entera desde **Settings → API Keys** (o genera un par nuevo) y actualiza `.env`. |
| `FALLO Cloudinary: … (401 …): esa clave de API existe pero está desactivada` | El par está desactivado en la consola (¿el que desactivaste al rotar?). | Usa el par **nuevo y activo** en `.env`. |
| `FALLO Cloudinary: … (401 …): la clave existe, pero el SECRETO no le corresponde` | El secreto está incompleto, es de otro par o tiene un carácter de más/de menos. | Copia el secreto con el **botón de copiar** de la consola (no seleccionando el texto). Las *Pistas sobre la forma* que acompañan al mensaje indican si su longitud es anómala. |
| `FALLO Cloudinary: CLOUDINARY_URL lleva el secreto OCULTO (asteriscos)` | Se copió el texto enmascarado (`*****`) de la pantalla. | Usa el botón de copiar o revela el secreto y vuelve a pegarlo. |
| `AVISO CLOUDINARY_URL (u otra) existe TAMBIÉN en el entorno de Windows con otro valor` | **La trampa de "corrijo el `.env` y sigue el mismo error"**: si la variable ya existe en el entorno de Windows o en tu ventana de PowerShell, **gana sobre el `.env`** (por diseño el `.env` no pisa lo ya definido: en Render manda el panel). Suele ocurrir tras seguir una instrucción de "`set CLOUDINARY_URL=…`" o "`$env:CLOUDINARY_URL=…`". | Ver más abajo, *Quitar una variable del entorno de Windows*. |
| `AVISO … aparece varias veces en backend/.env: se usa la ÚLTIMA` | Quedó una línea vieja además de la nueva. | Borra las líneas viejas y deja una sola por variable. |
| `INFO la clave de API de Cloudinary en uso termina en ...NNNN` | Aparece cuando Cloudinary falla: son los **4 últimos dígitos** de la clave que está leyendo el programa. | Compárala con la clave **activa** de *Settings → API Keys*. Si termina igual que la del par que borraste, sigues usando el viejo. |
| `FALLO Cloudinary: … no aceptó las credenciales (OtraExcepcion)` | Fallo distinto de un 401 (red, servicio caído…). | Reintenta; si persiste, avísame con el nombre de la excepción. |
| `FALLO la URL pública SÍ sirve la foto` | El recurso se subió como público. | **No sigas**: avísame; es un fallo de seguridad de la configuración. |
| `FALLO /health no respondió 200 (código 0)` | Sin conexión, o Render aún está construyendo/arrancando. | Espera 1–2 min y reintenta; revisa **Logs** en Render. |
| `FALLO token rechazado … no coincide` | El `HEALTH_TOKEN` de tu `.env` y el de Render son distintos. | Iguálalos (el de Render se edita en *Environment*). |
| `FALLO /health/servicios no existe` | Falta `HEALTH_TOKEN` en Render. | Añádelo en *Environment* y redespliega. |
| Render: error de compilación con la versión de Python | `PYTHON_VERSION` de `render.yaml` no disponible. | Cámbiala por otra 3.12.x publicada (Django 6.1 exige ≥ 3.12). |
| `AVISO Neon está en X (zona A) pero render.yaml usa 'Y' (zona B)` | Neon y Render están en zonas distintas: cada consulta a la base pagaría esa distancia. | Cambia `region:` en `render.yaml` a una de la zona de Neon **antes** de desplegar (después ya no se puede). |
| Render: *Deploy failed* en el chequeo de salud, o `DisallowedHost` / 400 en el registro | Django rechazó el `Host` con el que Render consulta `/health`. Render usa el subdominio `onrender.com` del servicio (o un dominio propio verificado, si lo tuvieras). | Con el subdominio de Render funciona solo (`RENDER_EXTERNAL_HOSTNAME`). Si añades un dominio propio, ponlo en la variable `ALLOWED_HOSTS` (separado por comas) en el panel. |
| Render: `ImproperlyConfigured: Falta … SECRET_KEY / DATABASE_URL` en el registro | Falta una variable en el panel (*Environment*). | Añádela y redespliega (**Manual Deploy**). Es el fallo rápido que buscamos: un servidor sin base de datos **no debe** arrancar. |
| Render: `Could not find a version that satisfies the requirement Django==6.1.1` | Render usa un Python anterior a 3.12. | Comprueba que la variable `PYTHON_VERSION` del panel sea `3.12.10` (la de `render.yaml`). |

### Quitar una variable del entorno de Windows

Solo si el verificador dice que existe *también en el entorno de Windows*. Estos comandos **no muestran** el valor. Para comprobar si existe en tu ventana actual y en tu usuario de Windows:

```powershell
$env:CLOUDINARY_URL -ne $null
```

```powershell
[Environment]::GetEnvironmentVariable("CLOUDINARY_URL", "User") -ne $null
```

Para quitarla de la ventana actual y, si también estaba en tu usuario, de forma permanente (después **cierra y vuelve a abrir PowerShell**):

```powershell
Remove-Item Env:CLOUDINARY_URL
```

```powershell
[Environment]::SetEnvironmentVariable("CLOUDINARY_URL", $null, "User")
```

Cambia `CLOUDINARY_URL` por el nombre de la variable que indique el aviso. Es un cambio en **tu** sistema, y solo afecta a tu usuario.

## 11. Registro de verificaciones de esta guía

| Qué | Cómo se comprobó | Resultado |
|---|---|---|
| El código del backend, la configuración y el script | `python manage.py test` (100 pruebas) + 86 mutaciones deliberadas | ✅ en verde (2026-09-18) |
| La aplicación WSGI real (la que carga gunicorn) por HTTP | Script de una sola vez con `wsgiref`: `/health` 200, `/health/servicios` 401 sin token y 503 con la base inalcanzable, sin filtrar la cadena de conexión | ✅ 2026-09-18 |
| **Neon** y **Cloudinary** reales | El usuario ejecutó `verificar_servicios.py --subida` con sus credenciales: conexión con TLS, **PostGIS 3.6** operativo, credenciales aceptadas, subida **privada** (`authenticated`), URL pública **404**, URL firmada **200**, recurso borrado. Salida completa en [`evidencia/2026-09-18_verificacion-neon-cloudinary.txt`](./evidencia/2026-09-18_verificacion-neon-cloudinary.txt) | ✅ TODO OK (2026-09-18) |
| Que se use el par de claves **nuevo** de Cloudinary | Pendiente: confirmar en *Settings → API Keys* que el par que se pegó por error en el chat está **desactivado o borrado** y volver a correr el verificador (si sigue en TODO OK, se usa el nuevo) | ⏳ |
| **Render** (servicio desplegado) | **Pendiente**: GitHub privado + Blueprint + `verificar_servicios.py --remoto` (§7) | ⏳ |
| Comando de arranque `gunicorn …` | Sin comprobar localmente (gunicorn no corre en Windows); se comprobará en el primer despliegue | ⏳ |
