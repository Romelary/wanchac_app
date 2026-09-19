"""Pruebas de `POST /reportes` y `DELETE /reportes/{idAnonimo}/{idLocal}` (contrato de docs/diseno.md §4.2).

Cloudinary está simulado; la base es SQLite en memoria (ver settings_test.py): el candado de PostgreSQL se prueba aparte.
"""

import json
import time
from unittest import mock

from django.core.files.uploadedfile import SimpleUploadedFile
from django.db import OperationalError
from django.test import TestCase, override_settings

from api import fotos, reportes
from api.models import Reporte, ReporteEliminado

ID = "3f2b8c1e-5a7d-4e0b-9c64-1d2e3f4a5b6c"
JPEG = b"\xff\xd8\xff\xe0" + b"contenido-de-una-foto" * 10 + b"\xff\xd9"


def geojson(**propiedades):
    """Un reporte válido con la forma que serializa Gson desde GeoJsonFeatureDto; `None` en un valor lo elimina."""
    props = {
        "idAnonimo": ID,
        "idLocal": 7,
        "categoria": "PLASTICO",
        "fechaHoraEpochMillis": int(time.time() * 1000),
        "categoriaIa": "PLASTICO",
        "confianzaIa": 0.87,
    }
    props.update(propiedades)
    return {
        "type": "Feature",
        "geometry": {"type": "Point", "coordinates": [-71.9475, -13.53]},
        "properties": {k: v for k, v in props.items() if v is not None},
    }


class _Base(TestCase):
    def setUp(self):
        subir = mock.patch.object(fotos, "subir")
        borrar = mock.patch.object(fotos, "borrar")
        self.subir, self.borrar = subir.start(), borrar.start()
        self.addCleanup(subir.stop)
        self.addCleanup(borrar.stop)

    def post(self, documento=None, foto=JPEG, texto=None, **extra):
        datos = {}
        if documento is not None or texto is not None:
            datos["geojson"] = texto if texto is not None else json.dumps(documento)
        if foto is not None:
            datos["foto"] = SimpleUploadedFile("foto.jpg", foto, content_type="image/jpeg")
        return self.client.post("/reportes", datos, **extra)

    def delete(self, id_anonimo=ID, id_local=7):
        return self.client.delete(f"/reportes/{id_anonimo}/{id_local}")


class CrearReporteTest(_Base):
    def test_un_reporte_valido_se_crea_y_se_guarda_todo_lo_del_contrato(self):
        respuesta = self.post(geojson())
        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(respuesta.json(), {"estado": "creado"})
        r = Reporte.objects.get()
        self.assertEqual((str(r.id_anonimo), r.id_local, r.categoria), (ID, 7, "PLASTICO"))
        self.assertEqual((r.longitud, r.latitud), (-71.9475, -13.53))  # GeoJSON [lon, lat]: no se invierten
        self.assertEqual((r.categoria_ia, r.confianza_ia), ("PLASTICO", 0.87))
        self.assertEqual(r.foto_public_id, fotos.public_id_de(ID, 7))
        self.assertIsNotNone(r.recibido_en)

    def test_la_foto_se_sube_una_vez_con_su_contenido_y_su_public_id(self):
        self.post(geojson())
        self.subir.assert_called_once_with(JPEG, fotos.public_id_de(ID, 7))

    def test_sin_datos_de_ia_tambien_se_crea(self):
        self.assertEqual(self.post(geojson(categoriaIa=None, confianzaIa=None)).status_code, 201)
        self.assertEqual((Reporte.objects.get().categoria_ia, Reporte.objects.get().confianza_ia), (None, None))

    def test_la_respuesta_nunca_incluye_el_idanonimo(self):
        cuerpos = [self.post(geojson()).content, self.post(geojson()).content, self.post(geojson(idLocal=None)).content]
        for cuerpo in cuerpos:
            self.assertNotIn(ID.encode(), cuerpo)

    def test_el_public_id_de_la_foto_no_revela_el_idanonimo(self):
        self.post(geojson())
        self.assertNotIn(ID.split("-")[0], Reporte.objects.get().foto_public_id)


class IdempotenciaTest(_Base):
    def test_reenviar_el_mismo_reporte_no_lo_duplica_ni_vuelve_a_subir_la_foto(self):
        self.assertEqual(self.post(geojson()).status_code, 201)
        segunda = self.post(geojson())
        self.assertEqual((segunda.status_code, segunda.json()), (200, {"estado": "ya_recibido"}))
        self.assertEqual(Reporte.objects.count(), 1)
        self.assertEqual(self.subir.call_count, 1)  # el reenvío por "respuesta perdida" no gasta Cloudinary

    def test_mismo_dispositivo_otro_reporte_o_mismo_id_local_de_otro_dispositivo_son_distintos(self):
        self.post(geojson(idLocal=7))
        self.post(geojson(idLocal=8))
        self.post(geojson(idAnonimo="00000000-0000-4000-8000-000000000001", idLocal=7))
        self.assertEqual(Reporte.objects.count(), 3)

    def test_carrera_dos_envios_simultaneos_no_duplican(self):
        # Otro proceso guarda el mismo reporte MIENTRAS este sube la foto: get_or_create debe encontrarlo.
        def otro_proceso(contenido, public_id):
            Reporte.objects.create(
                id_anonimo=ID, id_local=7, categoria="PLASTICO", fecha_hora="2026-09-18T15:00:00Z",
                latitud=-13.53, longitud=-71.9475, foto_public_id=public_id,
            )

        self.subir.side_effect = otro_proceso
        respuesta = self.post(geojson())
        self.assertEqual((respuesta.status_code, respuesta.json()), (200, {"estado": "ya_recibido"}))
        self.assertEqual(Reporte.objects.count(), 1)


class LapidaTest(_Base):
    def test_un_post_tardio_de_un_reporte_dado_de_baja_es_410_y_no_sube_nada(self):
        self.post(geojson())
        self.delete()
        self.subir.reset_mock()
        respuesta = self.post(geojson())
        self.assertEqual(respuesta.status_code, 410)
        self.assertEqual(respuesta.json()["error"], "reporte_eliminado")
        self.subir.assert_not_called()
        self.assertEqual(Reporte.objects.count(), 0)

    def test_la_baja_puede_llegar_antes_que_la_subida(self):
        # Baja de un reporte que el servidor nunca vio: 404, pero queda la lápida y el POST posterior es 410.
        self.assertEqual(self.delete().status_code, 404)
        self.assertEqual(self.post(geojson()).status_code, 410)
        self.assertEqual(Reporte.objects.count(), 0)

    def test_la_baja_llega_mientras_se_sube_la_foto_y_se_deshace_lo_subido(self):
        def baja_en_vuelo(contenido, public_id):
            ReporteEliminado.objects.create(id_anonimo=ID, id_local=7)

        self.subir.side_effect = baja_en_vuelo
        respuesta = self.post(geojson())
        self.assertEqual(respuesta.status_code, 410)
        self.assertEqual(Reporte.objects.count(), 0)  # no "resucita"
        self.borrar.assert_called_once_with(fotos.public_id_de(ID, 7))  # y la foto subida se retira

    def test_si_al_deshacer_la_foto_cloudinary_falla_igual_se_responde_410(self):
        self.subir.side_effect = lambda c, p: ReporteEliminado.objects.create(id_anonimo=ID, id_local=7)
        self.borrar.side_effect = fotos.ServicioNoDisponible
        self.assertEqual(self.post(geojson()).status_code, 410)


class EliminarReporteTest(_Base):
    def test_borrar_un_reporte_existente_es_204_y_retira_fila_y_foto(self):
        self.post(geojson())
        respuesta = self.delete()
        self.assertEqual(respuesta.status_code, 204)
        self.assertEqual(respuesta.content, b"")
        self.assertEqual(Reporte.objects.count(), 0)
        self.borrar.assert_called_once_with(fotos.public_id_de(ID, 7))
        self.assertTrue(ReporteEliminado.objects.filter(id_anonimo=ID, id_local=7).exists())

    def test_borrar_dos_veces_es_idempotente(self):
        self.post(geojson())
        self.delete()
        segunda = self.delete()
        self.assertEqual((segunda.status_code, segunda.json()), (404, {"estado": "no_existia"}))
        self.assertEqual(self.borrar.call_count, 1)  # la segunda no vuelve a tocar Cloudinary
        self.assertEqual(ReporteEliminado.objects.count(), 1)  # ni duplica la lápida

    def test_solo_se_borra_el_reporte_de_esa_clave(self):
        self.post(geojson(idLocal=7))
        self.post(geojson(idLocal=8))
        self.delete(id_local=7)
        self.assertEqual(list(Reporte.objects.values_list("id_local", flat=True)), [8])

    def test_un_fallo_de_cloudinary_al_borrar_lo_deja_todo_como_estaba_y_se_puede_reintentar(self):
        self.post(geojson())
        self.borrar.side_effect = fotos.ServicioNoDisponible
        respuesta = self.delete()
        self.assertEqual(respuesta.status_code, 503)
        self.assertEqual(Reporte.objects.count(), 1)  # nada a medias: el reporte sigue
        self.assertEqual(ReporteEliminado.objects.count(), 0)  # y sin lápida
        self.borrar.side_effect = None
        self.assertEqual(self.delete().status_code, 204)  # el reintento de la app funciona

    def test_la_respuesta_de_la_baja_nunca_incluye_el_idanonimo(self):
        self.post(geojson())
        for respuesta in (self.delete(), self.delete()):
            self.assertNotIn(ID.encode(), respuesta.content)

    def test_una_ruta_mal_formada_es_404_y_no_toca_nada(self):
        for ruta in ("/reportes/no-es-uuid/7", f"/reportes/{ID}/abc", f"/reportes/{ID}/", f"/reportes/{ID}"):
            with self.subTest(ruta=ruta):
                self.assertEqual(self.client.delete(ruta).status_code, 404)
        self.assertEqual(ReporteEliminado.objects.count(), 0)
        self.borrar.assert_not_called()

    def test_solo_delete_en_la_ruta_de_la_baja(self):
        for metodo in ("get", "post", "put", "patch"):
            with self.subTest(metodo=metodo):
                self.assertEqual(getattr(self.client, metodo)(f"/reportes/{ID}/7").status_code, 405)


class ValidacionDePeticionTest(_Base):
    def assertRechazo(self, respuesta, http, codigo):
        self.assertEqual((respuesta.status_code, respuesta.json()["error"]), (http, codigo))
        self.subir.assert_not_called()  # un rechazo jamás llega a Cloudinary
        self.assertEqual(Reporte.objects.count(), 0)  # ni a la base de datos

    def test_falta_la_foto(self):
        self.assertRechazo(self.post(geojson(), foto=None), 400, "parte_faltante")

    def test_falta_el_geojson(self):
        self.assertRechazo(self.post(None), 400, "parte_faltante")

    def test_la_foto_debe_ser_jpeg(self):
        for contenido in (b"\x89PNG\r\n\x1a\n" + b"x" * 50, b"GIF89a" + b"x" * 50, b"", b"texto plano"):
            with self.subTest(contenido=contenido[:6]):
                self.assertRechazo(self.post(geojson(), foto=contenido), 415, "foto_no_jpeg")

    @override_settings(MAX_FOTO_BYTES=100)
    def test_foto_demasiado_grande(self):
        self.assertRechazo(self.post(geojson(), foto=JPEG + b"x" * 200), 413, "foto_demasiado_grande")

    @override_settings(MAX_FOTO_BYTES=100)
    def test_un_content_length_enorme_se_rechaza_sin_leer_el_cuerpo(self):
        # La foto real cabe en el límite (50 bytes < 100): SOLO la comprobación temprana del encabezado puede dar 413.
        respuesta = self.post(geojson(), foto=JPEG[:50], CONTENT_LENGTH=str(10 * 1024 * 1024))
        self.assertRechazo(respuesta, 413, "foto_demasiado_grande")

    def test_un_geojson_que_no_es_json(self):
        self.assertRechazo(self.post(texto="no es json"), 400, "geojson_invalido")

    def test_fuera_de_wanchaq_es_422(self):
        d = geojson()
        d["geometry"]["coordinates"] = [0, 0]
        self.assertRechazo(self.post(d), 422, "fuera_de_wanchaq")

    def test_categoria_invalida_es_400(self):
        self.assertRechazo(self.post(geojson(categoria="OTRO")), 400, "categoria_invalida")

    def test_los_errores_no_repiten_lo_enviado_ni_el_idanonimo(self):
        respuesta = self.post(geojson(categoria="valor-distintivo-que-no-debe-volver"))
        self.assertNotIn(b"valor-distintivo", respuesta.content)
        respuesta = self.post(geojson(idLocal=None))
        self.assertNotIn(ID.encode(), respuesta.content)

    def test_solo_post_en_la_ruta_de_subida(self):
        for metodo in ("get", "put", "delete", "patch"):
            with self.subTest(metodo=metodo):
                self.assertEqual(getattr(self.client, metodo)("/reportes").status_code, 405)


class FalloDeServiciosTest(_Base):
    def test_si_cloudinary_falla_es_503_y_no_se_guarda_nada(self):
        self.subir.side_effect = fotos.ServicioNoDisponible
        respuesta = self.post(geojson())
        self.assertEqual((respuesta.status_code, respuesta.json()["error"]), (503, "servicio_no_disponible"))
        self.assertEqual(Reporte.objects.count(), 0)  # sin foto no hay reporte: la evidencia es obligatoria
        self.subir.side_effect = None
        self.assertEqual(self.post(geojson()).status_code, 201)  # el reintento de la app funciona

    def test_si_la_base_falla_al_consultar_es_503_y_no_sube_la_foto(self):
        with mock.patch.object(Reporte.objects, "filter", side_effect=OperationalError):
            with self.assertLogs("api", "WARNING") as registro:
                respuesta = self.post(geojson())
        self.assertEqual(respuesta.status_code, 503)
        self.subir.assert_not_called()
        self.assertIn("OperationalError", "".join(registro.output))

    def test_si_la_base_falla_al_guardar_es_503(self):
        with mock.patch.object(Reporte.objects, "get_or_create", side_effect=OperationalError):
            with self.assertLogs("api", "WARNING"):
                respuesta = self.post(geojson())
        self.assertEqual(respuesta.status_code, 503)

    def test_si_la_base_falla_en_la_baja_es_503(self):
        with mock.patch.object(ReporteEliminado.objects, "get_or_create", side_effect=OperationalError):
            with self.assertLogs("api", "WARNING"):
                self.assertEqual(self.delete().status_code, 503)


class CandadoTest(_Base):
    """En PostgreSQL, las operaciones sobre una misma clave se serializan con un candado de asesoría (ver _bloquear)."""

    def test_en_postgresql_toma_el_candado_de_esa_clave(self):
        conexion = mock.MagicMock(vendor="postgresql")
        with mock.patch.object(reportes, "connection", conexion):
            reportes._bloquear(ID, 7)
        sql, parametros = conexion.cursor.return_value.__enter__.return_value.execute.call_args.args
        self.assertIn("pg_advisory_xact_lock", sql)
        self.assertEqual(parametros, [f"{ID}:7"])

    def test_en_otros_motores_no_hace_nada(self):
        conexion = mock.MagicMock(vendor="sqlite")
        with mock.patch.object(reportes, "connection", conexion):
            reportes._bloquear(ID, 7)
        conexion.cursor.assert_not_called()

    def test_tanto_el_post_como_el_delete_toman_el_candado_de_su_clave(self):
        with mock.patch.object(reportes, "_bloquear") as bloquear:
            self.post(geojson())
            self.delete()
        self.assertEqual(bloquear.call_count, 2)
        for llamada in bloquear.call_args_list:
            self.assertEqual((str(llamada.args[0]), llamada.args[1]), (ID, 7))


class ContratoCompletoTest(_Base):
    def test_la_secuencia_que_verifica_la_prueba_de_humo_del_despliegue(self):
        # La misma que ejecuta `verificar_servicios.py --remoto … --reporte-de-prueba` contra Render.
        pasos = [
            (self.post(geojson()), 201),
            (self.post(geojson()), 200),
            (self.delete(), 204),
            (self.delete(), 404),
            (self.post(geojson()), 410),
        ]
        self.assertEqual([r.status_code for r, _ in pasos], [esperado for _, esperado in pasos])
        self.assertEqual(self.subir.call_count, 1)
        self.assertEqual(self.borrar.call_count, 1)
