"""Pruebas de `GET /puntos-criticos` (el mapa público; contrato en docs/diseno.md §4.2; agrupamiento en api/focos.py).

Los focos se calculan al pedir, sobre los reportes de la base (SQLite en memoria en la suite): se comprueba el contrato
GeoJSON que espera la app, que los reportes de prueba no cuentan y que la respuesta no filtra ningún identificador.
"""

import uuid
from datetime import datetime, timedelta, timezone
from unittest import mock

from django.db import OperationalError
from django.test import TestCase

from api.models import Reporte

COLECCION_VACIA = {"type": "FeatureCollection", "features": []}
BASE = datetime(2026, 9, 20, 12, 0, tzinfo=timezone.utc)
ID = "3f2b8c1e-5a7d-4e0b-9c64-1d2e3f4a5b6c"


def crear(n, categoria="PLASTICO", latitud=-13.53, longitud=-71.9475, es_prueba=False, minutos=0):
    """`n` reportes casi en el mismo punto (a pocos metros)."""
    for i in range(n):
        Reporte.objects.create(
            id_anonimo=uuid.UUID(ID),
            id_local=Reporte.objects.count() + 1,
            categoria=categoria,
            fecha_hora=BASE + timedelta(minutes=minutos + i),
            latitud=latitud + i * 1e-5,
            longitud=longitud,
            foto_public_id="f",
            es_prueba=es_prueba,
        )


class PuntosCriticosTest(TestCase):
    def test_sin_reportes_devuelve_una_coleccion_geojson_vacia(self):
        respuesta = self.client.get("/puntos-criticos")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json(), COLECCION_VACIA)

    def test_cuatro_reportes_juntos_forman_un_foco_con_el_contrato_de_la_app(self):
        crear(4, categoria="VIDRIO")

        (foco,) = self.client.get("/puntos-criticos").json()["features"]

        self.assertEqual(foco["type"], "Feature")
        self.assertEqual(foco["geometry"]["type"], "Point")
        longitud, latitud = foco["geometry"]["coordinates"]  # GeoJSON: [longitud, latitud]
        self.assertAlmostEqual(longitud, -71.9475, places=5)
        self.assertAlmostEqual(latitud, -13.53 + 1.5e-5, places=5)
        propiedades = foco["properties"]
        self.assertEqual(set(propiedades), {"id", "numReportes", "categoriaPredominante", "fechaDeteccionEpochMillis"})
        self.assertEqual((propiedades["numReportes"], propiedades["categoriaPredominante"]), (4, "VIDRIO"))
        self.assertEqual(propiedades["fechaDeteccionEpochMillis"], int((BASE + timedelta(minutes=3)).timestamp() * 1000))

    def test_tres_reportes_no_bastan(self):
        crear(3)

        self.assertEqual(self.client.get("/puntos-criticos").json(), COLECCION_VACIA)

    def test_los_reportes_de_prueba_no_cuentan(self):
        crear(3)
        crear(2, es_prueba=True)  # cerca de los otros 3: sin la exclusión serían 5

        self.assertEqual(self.client.get("/puntos-criticos").json(), COLECCION_VACIA)

    def test_dos_lugares_lejanos_son_dos_focos_con_id_estable(self):
        crear(4, categoria="CARTON", latitud=-13.53)
        crear(5, categoria="ORGANICO", latitud=-13.52)

        primero = self.client.get("/puntos-criticos").json()["features"]
        segundo = self.client.get("/puntos-criticos").json()["features"]

        self.assertEqual([f["properties"]["numReportes"] for f in primero], [4, 5])
        self.assertEqual(primero, segundo)  # el id no cambia entre una petición y otra

    def test_el_tipo_de_contenido_es_geojson(self):
        respuesta = self.client.get("/puntos-criticos")

        self.assertTrue(respuesta["Content-Type"].startswith("application/geo+json"), respuesta["Content-Type"])

    def test_se_puede_guardar_en_cache_un_minuto(self):
        self.assertEqual(self.client.get("/puntos-criticos")["Cache-Control"], "public, max-age=60")

    def test_acepta_estado_confirmado_por_compatibilidad_con_el_plan(self):
        respuesta = self.client.get("/puntos-criticos?estado=confirmado")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json(), COLECCION_VACIA)

    def test_cualquier_otro_estado_es_400_y_no_repite_lo_enviado(self):
        for valor in ("pendiente", "CONFIRMADO", "", "todos"):
            with self.subTest(estado=valor):
                respuesta = self.client.get("/puntos-criticos", {"estado": valor})

                self.assertEqual(respuesta.status_code, 400)
                self.assertEqual(respuesta.json()["error"], "parametro_invalido")
                if valor:
                    self.assertNotIn(valor, respuesta.content.decode())
                self.assertNotIn("Cache-Control", respuesta)  # un error no se guarda en cache

    def test_solo_acepta_get(self):
        for metodo in ("post", "put", "delete", "head"):
            with self.subTest(metodo=metodo):
                self.assertEqual(getattr(self.client, metodo)("/puntos-criticos").status_code, 405)

    def test_la_respuesta_no_incluye_identificadores_de_dispositivo_ni_de_reporte(self):
        crear(4)

        respuesta = self.client.get("/puntos-criticos", headers={"X-Id-Anonimo": ID})

        cuerpo = respuesta.content.decode()
        self.assertEqual(respuesta.status_code, 200)
        for prohibido in (ID, "idAnonimo", "idLocal", "foto"):
            self.assertNotIn(prohibido, cuerpo)
        self.assertNotIn("X-Id-Anonimo", respuesta.headers)

    def test_si_la_base_falla_es_503_y_no_se_guarda_en_cache(self):
        with mock.patch("api.puntos_criticos.Reporte.objects") as objetos:
            objetos.filter.side_effect = OperationalError("caída")
            respuesta = self.client.get("/puntos-criticos")

        self.assertEqual(respuesta.status_code, 503)
        self.assertEqual(respuesta.json()["error"], "servicio_no_disponible")
        self.assertNotIn("Cache-Control", respuesta)
