"""Pruebas del servicio de fotos (api/fotos.py) con Cloudinary simulado (sin red ni credenciales)."""

import io
from unittest import mock

from django.test import SimpleTestCase

from api import fotos

ID = "3f2b8c1e-5a7d-4e0b-9c64-1d2e3f4a5b6c"


class PublicIdTest(SimpleTestCase):
    def test_es_determinista_para_la_misma_clave(self):
        self.assertEqual(fotos.public_id_de(ID, 7), fotos.public_id_de(ID, 7))

    def test_cambia_con_el_id_local_y_con_el_id_anonimo(self):
        self.assertNotEqual(fotos.public_id_de(ID, 7), fotos.public_id_de(ID, 8))
        self.assertNotEqual(fotos.public_id_de(ID, 7), fotos.public_id_de("00000000-0000-4000-8000-000000000000", 7))

    def test_no_contiene_el_seudonimo_del_dispositivo(self):
        # Los public_id salen en las URL firmadas que verá quien administre el estudio; idAnonimo es credencial de baja.
        public_id = fotos.public_id_de(ID, 7)
        self.assertNotIn(ID, public_id)
        self.assertNotIn(ID.split("-")[0], public_id)
        self.assertTrue(public_id.startswith(fotos.CARPETA + "/"))


class SubirTest(SimpleTestCase):
    def test_sube_privada_y_sobrescribe(self):
        with mock.patch("cloudinary.uploader.upload") as upload:
            fotos.subir(b"\xff\xd8\xff-bytes", "carpeta/abc")
        args, kwargs = upload.call_args
        self.assertEqual(args[0].read(), b"\xff\xd8\xff-bytes")
        self.assertEqual(kwargs["public_id"], "carpeta/abc")
        self.assertEqual(kwargs["type"], "authenticated")  # PRIVADA: la URL pública no la sirve
        self.assertIs(kwargs["overwrite"], True)  # reenviar la misma foto no la duplica
        self.assertEqual(kwargs["allowed_formats"], ["jpg"])
        self.assertIsInstance(args[0], io.BytesIO)

    def test_un_fallo_de_cloudinary_es_servicio_no_disponible_y_solo_se_registra_el_nombre(self):
        secreto = "api_secret=ABC-no-debe-registrarse"
        with mock.patch("cloudinary.uploader.upload", side_effect=RuntimeError(secreto)):
            with self.assertLogs("api", "WARNING") as registro, self.assertRaises(fotos.ServicioNoDisponible):
                fotos.subir(b"x", "p")
        self.assertIn("RuntimeError", "".join(registro.output))
        self.assertNotIn(secreto, "".join(registro.output))


class BorrarTest(SimpleTestCase):
    def test_borra_la_foto_privada(self):
        with mock.patch("cloudinary.uploader.destroy", return_value={"result": "ok"}) as destroy:
            fotos.borrar("carpeta/abc")
        self.assertEqual(destroy.call_args.args[0], "carpeta/abc")
        self.assertEqual(destroy.call_args.kwargs["type"], "authenticated")

    def test_que_ya_no_existiera_cuenta_como_exito(self):
        with mock.patch("cloudinary.uploader.destroy", return_value={"result": "not found"}):
            fotos.borrar("carpeta/abc")  # no lanza: la baja es idempotente

    def test_una_respuesta_inesperada_es_fallo(self):
        for respuesta in ({"result": "error"}, {}, {"result": None}):
            with self.subTest(respuesta=respuesta), mock.patch("cloudinary.uploader.destroy", return_value=respuesta):
                with self.assertLogs("api", "WARNING"), self.assertRaises(fotos.ServicioNoDisponible):
                    fotos.borrar("p")

    def test_una_excepcion_es_servicio_no_disponible_sin_copiar_su_mensaje(self):
        secreto = "cloud_name=privado"
        with mock.patch("cloudinary.uploader.destroy", side_effect=RuntimeError(secreto)):
            with self.assertLogs("api", "WARNING") as registro, self.assertRaises(fotos.ServicioNoDisponible):
                fotos.borrar("p")
        self.assertNotIn(secreto, "".join(registro.output))
