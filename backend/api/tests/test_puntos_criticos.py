"""Pruebas de `GET /puntos-criticos` (el mapa público; contrato en docs/diseno.md §4.2).

Se usa SimpleTestCase a propósito, como en `test_health.py`: Django PROHÍBE cualquier consulta a la base dentro de él, así que
si el endpoint llegara a tocarla (despertaría a Neon con cada apertura del mapa) estas pruebas fallarían solas.
"""

from django.test import SimpleTestCase

# Lo que el cliente Android (PuntosCriticosRepositoryImplTest, «una coleccion vacia es un exito con cero puntos») espera.
COLECCION_VACIA = {"type": "FeatureCollection", "features": []}


class PuntosCriticosTest(SimpleTestCase):
    def test_hoy_devuelve_una_coleccion_geojson_vacia(self):
        respuesta = self.client.get("/puntos-criticos")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json(), COLECCION_VACIA)

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

    def test_no_pide_ni_devuelve_identificadores_de_dispositivo(self):
        # Los focos son públicos: aunque el cliente enviara su seudónimo, la respuesta no lo refleja.
        respuesta = self.client.get("/puntos-criticos", headers={"X-Id-Anonimo": "123e4567-e89b-42d3-a456-426614174000"})

        self.assertEqual(respuesta.status_code, 200)
        self.assertNotIn("123e4567", respuesta.content.decode())
        self.assertNotIn("X-Id-Anonimo", respuesta.headers)
