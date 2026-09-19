"""Pruebas de los endpoints de salud.

Se usa SimpleTestCase a propósito: Django PROHÍBE cualquier consulta a la base dentro de él, así que si `/health` llegara
a tocar la base (lo que despertaría a Neon con cada "pinchazo" del monitor) estas pruebas fallarían solas.
"""

from unittest import mock

from django.test import SimpleTestCase, override_settings

OK = {"ok": True, "detalle": "x"}
FALLA = {"ok": False, "detalle": "no se pudo conectar (OperationalError)"}


class HealthTest(SimpleTestCase):
    def test_health_responde_ok_sin_tocar_la_base(self):
        respuesta = self.client.get("/health")
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json(), {"estado": "ok"})

    def test_health_solo_acepta_get(self):
        self.assertEqual(self.client.post("/health").status_code, 405)


@override_settings(HEALTH_TOKEN="token-de-prueba")
class HealthServiciosTest(SimpleTestCase):
    def _llamar(self, token="token-de-prueba"):
        cabeceras = {} if token is None else {"headers": {"X-Health-Token": token}}
        return self.client.get("/health/servicios", **cabeceras)

    def test_sin_token_en_la_peticion_es_401(self):
        self.assertEqual(self._llamar(token=None).status_code, 401)

    def test_token_incorrecto_es_401(self):
        self.assertEqual(self._llamar(token="otro").status_code, 401)

    def test_token_como_parametro_de_url_no_vale(self):
        # Los secretos en la URL quedan en los registros de Render: solo se acepta la cabecera.
        self.assertEqual(self.client.get("/health/servicios?token=token-de-prueba").status_code, 401)

    @override_settings(HEALTH_TOKEN="")
    def test_sin_token_configurado_el_endpoint_no_existe(self):
        self.assertEqual(self._llamar(token="").status_code, 404)

    def test_todo_ok_es_200(self):
        with mock.patch("api.servicios.comprobar_base_de_datos", return_value=OK), mock.patch(
            "api.servicios.comprobar_cloudinary", return_value=OK
        ):
            respuesta = self._llamar()
        self.assertEqual(respuesta.status_code, 200)
        self.assertTrue(respuesta.json()["ok"])

    def test_un_servicio_caido_es_503_y_dice_cual(self):
        with mock.patch("api.servicios.comprobar_base_de_datos", return_value=FALLA), mock.patch(
            "api.servicios.comprobar_cloudinary", return_value=OK
        ):
            respuesta = self._llamar()
        self.assertEqual(respuesta.status_code, 503)
        cuerpo = respuesta.json()
        self.assertFalse(cuerpo["ok"])
        self.assertFalse(cuerpo["servicios"]["base_de_datos"]["ok"])
        self.assertTrue(cuerpo["servicios"]["cloudinary"]["ok"])
