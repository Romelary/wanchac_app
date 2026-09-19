"""Pruebas de la validación del `geojson` (api/validacion.py) contra el contrato de docs/diseno.md §4.2."""

import json
from datetime import datetime, timedelta, timezone

from django.test import SimpleTestCase

from api.validacion import ErrorDeValidacion, validar_geojson

AHORA = datetime(2026, 9, 18, 15, 0, tzinfo=timezone.utc)
AHORA_MS = int(AHORA.timestamp() * 1000)
ID = "3f2b8c1e-5a7d-4e0b-9c64-1d2e3f4a5b6c"


def base():
    """Un reporte válido con la MISMA forma que serializa Gson desde GeoJsonFeatureDto (Android)."""
    return {
        "type": "Feature",
        "geometry": {"type": "Point", "coordinates": [-71.9475, -13.53]},  # GeoJSON: [longitud, latitud]
        "properties": {
            "idAnonimo": ID,
            "idLocal": 7,
            "categoria": "PLASTICO",
            "fechaHoraEpochMillis": AHORA_MS,
            "categoriaIa": "PLASTICO",
            "confianzaIa": 0.87,
        },
    }


def validar(documento, ahora=AHORA):
    return validar_geojson(documento if isinstance(documento, str) else json.dumps(documento), ahora)


class ValidacionTest(SimpleTestCase):
    def assertRechaza(self, documento, codigo, http):
        with self.assertRaises(ErrorDeValidacion) as contexto:
            validar(documento)
        self.assertEqual((contexto.exception.codigo, contexto.exception.http), (codigo, http))
        return contexto.exception

    def con(self, **propiedades):
        d = base()
        d["properties"].update(propiedades)
        return d

    # --- caso válido ----------------------------------------------------------------------------------------------
    def test_un_reporte_valido_se_interpreta_con_longitud_y_latitud_en_su_sitio(self):
        datos = validar(base())
        self.assertEqual(str(datos.id_anonimo), ID)
        self.assertEqual((datos.id_local, datos.categoria), (7, "PLASTICO"))
        self.assertEqual((datos.longitud, datos.latitud), (-71.9475, -13.53))  # [lon, lat] → no se invierten
        self.assertEqual(datos.fecha_hora, AHORA)
        self.assertEqual((datos.categoria_ia, datos.confianza_ia), ("PLASTICO", 0.87))

    def test_sin_datos_de_ia_es_valido_porque_gson_omite_los_nulos(self):
        d = base()
        del d["properties"]["categoriaIa"], d["properties"]["confianzaIa"]
        datos = validar(d)
        self.assertEqual((datos.categoria_ia, datos.confianza_ia), (None, None))

    def test_un_uuid_en_mayusculas_se_normaliza(self):
        self.assertEqual(str(validar(self.con(idAnonimo=ID.upper())).id_anonimo), ID)

    def test_se_ignoran_propiedades_desconocidas(self):
        self.assertEqual(validar(self.con(otraCosa="x")).id_local, 7)

    # --- forma: 400 ---------------------------------------------------------------------------------------------
    def test_json_invalido(self):
        for texto in ("no es json", "", "{", "[1, 2"):
            with self.subTest(texto=texto):
                self.assertRechaza(texto, "geojson_invalido", 400)
        with self.assertRaises(ErrorDeValidacion):
            validar_geojson(None, AHORA)

    def test_json_que_no_es_un_feature(self):
        for documento in ([], "texto", {"type": "FeatureCollection"}, {"geometry": {}}):
            with self.subTest(documento=str(documento)):
                self.assertRechaza(json.dumps(documento), "geojson_invalido", 400)

    def test_geometria_que_no_es_un_punto(self):
        for geometria in (None, {}, {"type": "LineString", "coordinates": [[0, 0], [1, 1]]}, "Point"):
            d = base()
            d["geometry"] = geometria
            with self.subTest(geometria=str(geometria)):
                self.assertRechaza(d, "geometria_invalida", 400)

    def test_coordenadas_invalidas(self):
        for coordenadas in (None, "x", [1], [1, 2, 3], ["a", "b"], [True, False], [-71.9, None], [-71.9, float("nan")], [200, -13.5], [-71.9, 95]):
            d = base()
            d["geometry"]["coordinates"] = coordenadas
            with self.subTest(coordenadas=str(coordenadas)):
                self.assertRechaza(d, "coordenadas_invalidas", 400)

    def test_nan_e_infinito_escritos_como_texto_json_se_rechazan(self):
        # Python acepta NaN/Infinity en json.loads: no deben colarse como coordenadas
        texto = json.dumps(base()).replace("-13.53", "NaN")
        self.assertRechaza(texto, "coordenadas_invalidas", 400)

    def test_sin_propiedades(self):
        d = base()
        del d["properties"]
        self.assertRechaza(d, "propiedades_invalidas", 400)

    def test_id_anonimo_invalido(self):
        for valor in (None, 5, "", "no-es-un-uuid", "{" + ID + "}", "urn:uuid:" + ID, ID.replace("-", ""), [ID]):
            with self.subTest(valor=str(valor)):
                self.assertRechaza(self.con(idAnonimo=valor), "id_anonimo_invalido", 400)

    def test_id_local_invalido(self):
        for valor in (None, 0, -1, "7", 1.5, True, 2**63, [7]):
            with self.subTest(valor=str(valor)):
                self.assertRechaza(self.con(idLocal=valor), "id_local_invalido", 400)

    def test_id_local_en_los_extremos_validos(self):
        self.assertEqual(validar(self.con(idLocal=1)).id_local, 1)
        self.assertEqual(validar(self.con(idLocal=2**63 - 1)).id_local, 2**63 - 1)

    def test_categoria_invalida(self):
        for valor in (None, "plastico", "OTRO", "", 3, ["PLASTICO"]):
            with self.subTest(valor=str(valor)):
                self.assertRechaza(self.con(categoria=valor), "categoria_invalida", 400)

    def test_las_seis_categorias_del_estudio_son_validas(self):
        for categoria in ("PLASTICO", "CARTON", "VIDRIO", "ORGANICO", "ESCOMBROS", "MIXTO"):
            self.assertEqual(validar(self.con(categoria=categoria)).categoria, categoria)

    def test_fecha_que_no_es_un_entero(self):
        for valor in (None, "1789743600000", 1.5, True, [1]):
            with self.subTest(valor=str(valor)):
                self.assertRechaza(self.con(fechaHoraEpochMillis=valor), "fecha_invalida", 400)

    # --- forma de los datos de IA (CORR-010) ---------------------------------------------------------------------
    def test_los_datos_de_ia_van_juntos_o_ninguno(self):
        d = base()
        del d["properties"]["confianzaIa"]
        self.assertRechaza(d, "ia_incompleta", 400)
        d = base()
        del d["properties"]["categoriaIa"]
        self.assertRechaza(d, "ia_incompleta", 400)

    def test_la_ia_nunca_predice_escombros_ni_mixto(self):
        for valor in ("ESCOMBROS", "MIXTO", "plastico", 1, ""):
            with self.subTest(valor=str(valor)):
                self.assertRechaza(self.con(categoriaIa=valor), "categoria_ia_invalida", 400)

    def test_confianza_fuera_de_rango_o_de_otro_tipo(self):
        for valor in (1.5, -0.1, "0.8", True, float("nan"), [0.5]):
            with self.subTest(valor=str(valor)):
                self.assertRechaza(self.con(confianzaIa=valor), "confianza_ia_invalida", 400)

    def test_confianza_en_los_extremos_es_valida(self):
        for valor in (0, 0.0, 1, 1.0):
            self.assertEqual(validar(self.con(confianzaIa=valor)).confianza_ia, float(valor))

    # --- reglas del estudio: 422 ---------------------------------------------------------------------------------
    def test_fecha_en_el_futuro_lejano_o_anterior_a_la_app(self):
        futuro = int((AHORA + timedelta(days=2)).timestamp() * 1000)
        antes = int(datetime(2023, 12, 31, tzinfo=timezone.utc).timestamp() * 1000)
        for valor in (futuro, antes, 10**30, -10**30):
            with self.subTest(valor=valor):
                self.assertRechaza(self.con(fechaHoraEpochMillis=valor), "fecha_fuera_de_rango", 422)

    def test_se_tolera_un_reloj_del_telefono_adelantado_hasta_un_dia(self):
        casi_un_dia = int((AHORA + timedelta(hours=23)).timestamp() * 1000)
        self.assertEqual(validar(self.con(fechaHoraEpochMillis=casi_un_dia)).fecha_hora, AHORA + timedelta(hours=23))

    def test_fuera_de_wanchaq_es_422(self):
        for lon, lat in ((0, 0), (-71.9475, -12.0), (-70.0, -13.53), (-71.9475, -13.6), (-71.9475, -13.5)):
            d = base()
            d["geometry"]["coordinates"] = [lon, lat]
            with self.subTest(lon=lon, lat=lat):
                self.assertRechaza(d, "fuera_de_wanchaq", 422)

    def test_latitud_y_longitud_intercambiadas_quedan_fuera(self):
        d = base()
        d["geometry"]["coordinates"] = [-13.53, -71.9475]  # el error clásico: [lat, lon]
        self.assertRechaza(d, "fuera_de_wanchaq", 422)

    def test_los_bordes_del_rectangulo_son_validos_y_un_paso_mas_afuera_no(self):
        # Mismo rectángulo que LimiteDistritalWanchaq.kt (con `in`, es decir, inclusivo)
        for lon, lat in ((-71.965, -13.545), (-71.930, -13.515), (-71.965, -13.515), (-71.930, -13.545)):
            d = base()
            d["geometry"]["coordinates"] = [lon, lat]
            self.assertEqual((validar(d).longitud, validar(d).latitud), (lon, lat))
        for lon, lat in ((-71.9651, -13.53), (-71.9299, -13.53), (-71.9475, -13.5451), (-71.9475, -13.5149)):
            d = base()
            d["geometry"]["coordinates"] = [lon, lat]
            with self.subTest(lon=lon, lat=lat):
                self.assertRechaza(d, "fuera_de_wanchaq", 422)

    def test_la_forma_se_comprueba_antes_que_la_regla_del_distrito(self):
        # Categoría inválida Y fuera de Wanchaq: gana el 400 (un 422 significa "bien formado pero fuera del distrito")
        d = self.con(categoria="OTRO")
        d["geometry"]["coordinates"] = [0, 0]
        self.assertRechaza(d, "categoria_invalida", 400)

    # --- privacidad de los mensajes ------------------------------------------------------------------------------
    def test_los_errores_nunca_repiten_lo_que_envio_el_cliente(self):
        secreto = "valor-distintivo-que-no-debe-volver"
        casos = [
            self.con(idAnonimo=secreto), self.con(categoria=secreto), self.con(categoriaIa=secreto),
            self.con(idLocal=secreto), self.con(fechaHoraEpochMillis=secreto), self.con(confianzaIa=secreto),
        ]
        for documento in casos:
            with self.subTest(documento=str(documento)[:60]), self.assertRaises(ErrorDeValidacion) as contexto:
                validar(documento)
            self.assertNotIn(secreto, contexto.exception.detalle + contexto.exception.codigo)
        with self.assertRaises(ErrorDeValidacion) as contexto:
            validar("{" + secreto)
        self.assertNotIn(secreto, contexto.exception.detalle)

    def test_ni_siquiera_el_uuid_valido_aparece_en_un_error_posterior(self):
        d = base()
        d["geometry"]["coordinates"] = [0, 0]
        with self.assertRaises(ErrorDeValidacion) as contexto:
            validar(d)
        self.assertNotIn(ID, contexto.exception.detalle)
