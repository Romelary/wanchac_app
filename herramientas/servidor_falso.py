#!/usr/bin/env python3
"""Servidor FALSO de Wanchaq Limpio: para probar el envio de reportes de la app en un dispositivo real.

No usa Render, Neon ni Cloudinary y no necesita ningun secreto. Sirve para provocar a voluntad los fallos que en la
calle son dificiles de repetir (servidor caido, respuesta perdida, red lenta) y ver que la app se porta bien; NO
sustituye a la prueba contra el servidor real (ver docs/pruebas-de-confiabilidad.md).

Uso (con el dispositivo conectado por USB y la depuracion USB activa):

    1. python herramientas/servidor_falso.py                      # escucha en el puerto 8000 del PC
    2. adb reverse tcp:8000 tcp:8000                              # el dispositivo alcanza al PC como "localhost"
    3. ./gradlew installDebug -PbaseUrl=http://localhost:8000     # (+ -PubicacionSimulada=true para no depender del GPS)

Solo las compilaciones de DEPURACION permiten http:// (y solo hacia localhost, 127.0.0.1 y 10.0.2.2: ver
app/src/debug/res/xml/network_security_config_debug.xml); la version de publicacion exige https.

El comportamiento se cambia SIN reiniciar, escribiendo una palabra en `herramientas/modo.txt`:

    ok         201 Created (lo normal)                          duplicado  200 (ya lo tenia)
    500 / 503 / 422 / 404 / 400 / 413 / 429 ...  ese codigo HTTP, tal cual
    lento:20   espera 20 s y luego responde 201 (red lenta / servidor gratuito despertando)
    corta      lee un poco del cuerpo y CIERRA la conexion (se cae la red a mitad del envio)
    perdida    RECIBE y GUARDA el reporte y cierra SIN responder (la respuesta se pierde: el reporte ya esta en el
               servidor pero la app cree que fallo; al reintentar con `ok` debe contestar 200 y NO duplicar)
    colgado    acepta y no responde nunca (para probar los tiempos de espera de la app: 90 s de lectura)
    focos      GET /puntos-criticos devuelve 2 focos de PRUEBA (para ver los marcadores del mapa); POST se porta como `ok`

Registra una linea por peticion (idLocal, categoria, tamano de la foto, codigo devuelto y cuantos reportes distintos
lleva) y guarda las fotos recibidas en `herramientas/recibidas/`. NO imprime coordenadas ni el codigo anonimo
completo. Las fotos recibidas pueden mostrar lo que vio la camara: no se suben a git (ver .gitignore).
"""

import http.server
import json
import os
import re
import socketserver
import sys
import threading
import time
from datetime import datetime

AQUI = os.path.dirname(os.path.abspath(__file__))
RUTA_MODO = os.path.join(AQUI, "modo.txt")
CARPETA_FOTOS = os.path.join(AQUI, "recibidas")
PUERTO = int(os.environ.get("PUERTO", "8000"))

def focos_de_prueba():
    """Dos focos INVENTADOS dentro de Wanchaq (formato de GET /puntos-criticos, docs/diseno.md 4.2)."""
    hace_dos_dias = int((time.time() - 2 * 86400) * 1000)
    def foco(id_, lon, lat, n, categoria):
        return {"type": "Feature", "geometry": {"type": "Point", "coordinates": [lon, lat]},
                "properties": {"id": id_, "numReportes": n, "categoriaPredominante": categoria,
                               "fechaDeteccionEpochMillis": hace_dos_dias}}
    return {"type": "FeatureCollection", "features": [foco(1, -71.9475, -13.5300, 7, "PLASTICO"),
                                                      foco(2, -71.9400, -13.5250, 4, "CARTON")]}


vistos = {}  # (idAnonimo, idLocal) -> nombre de la foto guardada
candado = threading.Lock()


def modo_actual():
    try:
        with open(RUTA_MODO, encoding="utf-8") as f:
            return f.read().strip().lower() or "ok"
    except OSError:
        return "ok"


def registrar(texto):
    print(f"{datetime.now():%H:%M:%S} {texto}", flush=True)


def leer_cuerpo(manejador):
    """Lee el cuerpo completo, con Content-Length o con Transfer-Encoding: chunked."""
    if "chunked" in manejador.headers.get("Transfer-Encoding", "").lower():
        trozos = []
        while True:
            tamano = int(manejador.rfile.readline().strip().split(b";")[0] or b"0", 16)
            if tamano == 0:
                manejador.rfile.readline()
                break
            trozos.append(manejador.rfile.read(tamano))
            manejador.rfile.readline()
        return b"".join(trozos)
    largo = int(manejador.headers.get("Content-Length", "0"))
    return manejador.rfile.read(largo) if largo else b""


def partes_multipart(cuerpo, tipo_contenido):
    """Devuelve {nombre: bytes} de un multipart/form-data (lo justo para este servidor)."""
    coincide = re.search(r"boundary=([^;]+)", tipo_contenido)
    if not coincide:
        return {}
    limite = b"--" + coincide.group(1).strip('"').encode()
    partes = {}
    for trozo in cuerpo.split(limite)[1:]:
        if trozo.startswith(b"--"):
            break
        cabecera, _, datos = trozo.partition(b"\r\n\r\n")
        nombre = re.search(rb'name="([^"]+)"', cabecera)
        if nombre:
            partes[nombre.group(1).decode()] = datos.rsplit(b"\r\n", 1)[0]
    return partes


class Manejador(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, formato, *args):  # silencia el registro por defecto
        pass

    def responder(self, codigo, cuerpo=b"", tipo="application/json"):
        self.send_response(codigo)
        self.send_header("Content-Type", tipo)
        self.send_header("Content-Length", str(len(cuerpo)))
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(cuerpo)

    def cortar(self):
        self.close_connection = True
        try:
            self.connection.shutdown(2)
        except OSError:
            pass

    def do_GET(self):
        if self.path.split("?")[0].rstrip("/") == "/puntos-criticos":
            if modo_actual() == "focos":
                registrar("GET  /puntos-criticos -> 200 (2 focos de prueba)")
                self.responder(200, json.dumps(focos_de_prueba()).encode())
            else:
                registrar("GET  /puntos-criticos -> 200 (vacio)")
                self.responder(200, b'{"type":"FeatureCollection","features":[]}')
        else:
            registrar(f"GET  {self.path} -> 404")
            self.responder(404, b'{"detail":"no existe"}')

    def do_DELETE(self):
        coincide = re.match(r"/reportes/([^/]+)/(\d+)/?$", self.path)
        if not coincide:
            self.responder(404, b'{"detail":"no existe"}')
            return
        with candado:
            existia = vistos.pop((coincide.group(1), int(coincide.group(2))), None) is not None
        registrar(f"DELETE /reportes/{coincide.group(1)[:8]}.../{coincide.group(2)} -> 204 (existia: {existia})")
        self.responder(204)

    def do_POST(self):
        if self.path.rstrip("/") != "/reportes":
            self.responder(404, b'{"detail":"no existe"}')
            return
        modo = modo_actual()
        if modo == "focos":
            modo = "ok"
        inicio = time.monotonic()
        if modo == "corta":
            self.rfile.read(min(int(self.headers.get("Content-Length", "0")), 20000))
            registrar("POST /reportes modo=corta -> conexion cortada a mitad del cuerpo")
            self.cortar()
            return
        if modo == "colgado":
            self.rfile.read(int(self.headers.get("Content-Length", "0")))
            registrar("POST /reportes modo=colgado -> recibido, no respondo (espero 10 min)")
            time.sleep(600)
            self.cortar()
            return

        cuerpo = leer_cuerpo(self)
        partes = partes_multipart(cuerpo, self.headers.get("Content-Type", ""))
        foto = partes.get("foto", b"")
        try:
            feature = json.loads(partes.get("geojson", b"{}").decode("utf-8"))
        except (ValueError, UnicodeDecodeError):
            feature = {}
        propiedades = feature.get("properties", {}) if isinstance(feature, dict) else {}
        clave = (str(propiedades.get("idAnonimo")), propiedades.get("idLocal"))
        es_jpeg = foto[:2] == b"\xff\xd8"
        resumen = (
            f"idLocal={propiedades.get('idLocal')} categoria={propiedades.get('categoria')} "
            f"foto={len(foto)}B jpeg={es_jpeg} cuerpo={len(cuerpo)}B "
            f"propiedades={sorted(propiedades)}"
        )

        if modo.startswith("lento:"):
            time.sleep(float(modo.split(":", 1)[1]))
            modo = "ok"

        if modo in ("ok", "perdida", "duplicado"):
            with candado:
                ya_estaba = clave in vistos
                if not ya_estaba and foto:
                    os.makedirs(CARPETA_FOTOS, exist_ok=True)
                    nombre = f"{datetime.now():%H%M%S}_{propiedades.get('idLocal')}.jpg"
                    with open(os.path.join(CARPETA_FOTOS, nombre), "wb") as f:
                        f.write(foto)
                    vistos[clave] = nombre
            demora = time.monotonic() - inicio
            if modo == "perdida":
                registrar(f"POST /reportes modo=perdida ({'REPETIDO' if ya_estaba else 'nuevo'}) {resumen} "
                          "-> guardado y CIERRO sin responder")
                self.cortar()
                return
            codigo = 200 if (ya_estaba or modo == "duplicado") else 201
            registrar(f"POST /reportes modo={modo} {resumen} -> {codigo} en {demora:.1f}s "
                      f"(total en servidor: {len(vistos)})")
            self.responder(codigo, b'{"status":"ok"}')
            return

        if modo.isdigit():
            registrar(f"POST /reportes modo={modo} {resumen} -> {modo}")
            self.responder(int(modo), json.dumps({"detail": f"error simulado {modo}"}).encode())
            return

        registrar(f"POST /reportes modo desconocido '{modo}' -> 500")
        self.responder(500, b'{"detail":"modo desconocido"}')


class Servidor(socketserver.ThreadingMixIn, http.server.HTTPServer):
    daemon_threads = True
    allow_reuse_address = True


if __name__ == "__main__":
    if not os.path.exists(RUTA_MODO):
        with open(RUTA_MODO, "w", encoding="utf-8") as f:
            f.write("ok")
    registrar(f"Servidor falso escuchando en el puerto {PUERTO} (modo en {RUTA_MODO}: '{modo_actual()}')")
    try:
        Servidor(("0.0.0.0", PUERTO), Manejador).serve_forever()
    except KeyboardInterrupt:
        sys.exit(0)
