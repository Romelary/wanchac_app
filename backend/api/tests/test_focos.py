"""Pruebas del agrupamiento de focos (api/focos.py) con puntos sintéticos: DBSCAN con ε = 50 m y MinSamples = 4."""

from datetime import datetime, timedelta, timezone

from django.test import SimpleTestCase

from api.focos import PuntoDeReporte, detectar_focos, distancia_m

BASE = datetime(2026, 9, 20, 12, 0, tzinfo=timezone.utc)
LAT, LON = -13.53, -71.9475
GRADO_LAT_M = 111_195  # metros por grado de latitud (esfera de 6 371 km)
GRADO_LON_M = GRADO_LAT_M * 0.9713  # coseno de 13,53°


def punto(id_, metros_norte=0.0, metros_este=0.0, categoria="PLASTICO", minutos=0):
    """Un reporte a esos metros del origen (aproximación plana, suficiente para distancias de decenas de metros)."""
    latitud = LAT + metros_norte / GRADO_LAT_M
    longitud = LON + metros_este / GRADO_LON_M
    return PuntoDeReporte(id_, latitud, longitud, categoria, BASE + timedelta(minutes=minutos))


class DistanciaTest(SimpleTestCase):
    def test_un_grado_de_latitud_son_unos_111_km(self):
        self.assertAlmostEqual(distancia_m(0, 0, 1, 0), 111_195, delta=10)

    def test_es_simetrica_y_cero_en_el_mismo_punto(self):
        self.assertEqual(distancia_m(LAT, LON, LAT, LON), 0)
        self.assertAlmostEqual(distancia_m(LAT, LON, LAT + 0.001, LON), distancia_m(LAT + 0.001, LON, LAT, LON))


class DetectarFocosTest(SimpleTestCase):
    def test_sin_puntos_no_hay_focos(self):
        self.assertEqual(detectar_focos([]), [])

    def test_tres_puntos_no_bastan_y_cuatro_si(self):
        tres = [punto(i, metros_norte=i) for i in range(1, 4)]
        self.assertEqual(detectar_focos(tres), [])
        self.assertEqual(len(detectar_focos(tres + [punto(4, metros_norte=4)])), 1)

    def test_el_centroide_es_el_promedio_y_cuenta_los_reportes(self):
        (foco,) = detectar_focos([punto(i, metros_norte=10 * i) for i in range(1, 5)])

        self.assertEqual(foco.num_reportes, 4)
        self.assertAlmostEqual(foco.latitud, LAT + 25 / GRADO_LAT_M, places=8)
        self.assertAlmostEqual(foco.longitud, LON, places=8)

    def test_el_limite_es_de_50_metros(self):
        dentro = [punto(i) for i in range(1, 4)] + [punto(4, metros_norte=49)]
        fuera = [punto(i) for i in range(1, 4)] + [punto(4, metros_norte=52)]

        self.assertEqual(len(detectar_focos(dentro)), 1)
        self.assertEqual(detectar_focos(fuera), [])  # el cuarto queda aparte y los otros tres son solo 3

    def test_dos_lugares_lejanos_son_dos_focos(self):
        a = [punto(i, metros_norte=i) for i in range(1, 5)]
        b = [punto(i + 10, metros_norte=500 + i) for i in range(1, 6)]

        focos = detectar_focos(a + b)

        self.assertEqual([f.num_reportes for f in focos], [4, 5])

    def test_una_cadena_de_puntos_se_une_en_un_solo_foco(self):
        # Cada punto tiene a ±2 vecinos a menos de 50 m (5 con él): son núcleos y el grupo crece a lo largo de la calle.
        cadena = [punto(i, metros_norte=20 * i) for i in range(1, 9)]

        (foco,) = detectar_focos(cadena)

        self.assertEqual(foco.num_reportes, 8)

    def test_una_cadena_demasiado_espaciada_no_forma_foco(self):
        cadena = [punto(i, metros_norte=40 * i) for i in range(1, 9)]  # cada punto ve a 3 con él: no es núcleo

        self.assertEqual(detectar_focos(cadena), [])

    def test_un_punto_de_borde_se_une_y_uno_lejano_es_ruido(self):
        nucleo = [punto(i, metros_norte=i) for i in range(1, 5)]
        borde = punto(5, metros_norte=48)
        ruido = punto(6, metros_norte=400)

        (foco,) = detectar_focos(nucleo + [borde, ruido])

        self.assertEqual(foco.num_reportes, 5)

    def test_la_categoria_predominante_es_la_mas_frecuente(self):
        cats = ["VIDRIO", "PLASTICO", "VIDRIO", "VIDRIO"]
        (foco,) = detectar_focos([punto(i + 1, metros_norte=i, categoria=c) for i, c in enumerate(cats)])

        self.assertEqual(foco.categoria_predominante, "VIDRIO")

    def test_ante_un_empate_gana_la_primera_categoria_de_la_lista(self):
        cats = ["CARTON", "PLASTICO", "CARTON", "PLASTICO"]
        (foco,) = detectar_focos([punto(i + 1, metros_norte=i, categoria=c) for i, c in enumerate(cats)])

        self.assertEqual(foco.categoria_predominante, "PLASTICO")  # PLASTICO va antes que CARTON en CATEGORIAS

    def test_el_ultimo_reporte_es_el_mas_reciente(self):
        (foco,) = detectar_focos([punto(i, metros_norte=i, minutos=m) for i, m in ((1, 5), (2, 40), (3, 0), (4, 9))])

        self.assertEqual(foco.ultimo_reporte, BASE + timedelta(minutes=40))

    def test_el_id_es_el_del_reporte_mas_antiguo_y_no_depende_del_orden(self):
        puntos = [punto(i, metros_norte=i) for i in (7, 3, 9, 5)]

        self.assertEqual(detectar_focos(puntos)[0].id, 3)
        self.assertEqual(detectar_focos(list(reversed(puntos))), detectar_focos(puntos))

    def test_muchos_puntos_dispersos_se_procesan_rapido(self):
        dispersos = [punto(i, metros_norte=(i % 100) * 200, metros_este=(i // 100) * 200) for i in range(1, 3001)]

        self.assertEqual(detectar_focos(dispersos), [])
