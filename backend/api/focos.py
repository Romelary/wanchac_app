"""Detección de focos: agrupamiento DBSCAN de reportes cercanos (ADR-0009). Lógica pura, sin base de datos ni Django.

Un foco es un grupo de al menos `MIN_REPORTES` reportes en los que cada uno tiene, dentro de `RADIO_METROS`, a otros
suficientes (DBSCAN clásico: ε = 50 m, MinSamples = 4, contando el propio punto). Cuenta REPORTES, no personas distintas.
"""

import math
from collections import Counter, defaultdict
from dataclasses import dataclass
from datetime import datetime

from api.constantes import CATEGORIAS

RADIO_METROS = 50.0
MIN_REPORTES = 4
RADIO_TERRESTRE_M = 6_371_000.0
CELDA_GRADOS = 0.0006  # ≈ 65 m: mayor que ε en latitud y longitud, así los vecinos están en las 9 celdas de alrededor


@dataclass(frozen=True)
class PuntoDeReporte:
    id: int  # clave primaria del reporte: solo sirve para dar un id estable al foco (nunca es el seudónimo)
    latitud: float
    longitud: float
    categoria: str
    fecha_hora: datetime


@dataclass(frozen=True)
class Foco:
    id: int
    latitud: float
    longitud: float
    num_reportes: int
    categoria_predominante: str
    ultimo_reporte: datetime


def distancia_m(lat1, lon1, lat2, lon2):
    """Distancia de Haversine, en metros."""
    p1, p2 = math.radians(lat1), math.radians(lat2)
    a = math.sin((p2 - p1) / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(math.radians(lon2 - lon1) / 2) ** 2
    return 2 * RADIO_TERRESTRE_M * math.asin(math.sqrt(a))


def _celda(punto):
    return math.floor(punto.latitud / CELDA_GRADOS), math.floor(punto.longitud / CELDA_GRADOS)


def _vecinos(puntos, rejilla, i):
    fila, columna = _celda(puntos[i])
    p = puntos[i]
    vecinos = []
    for f in (fila - 1, fila, fila + 1):
        for c in (columna - 1, columna, columna + 1):
            for j in rejilla.get((f, c), ()):
                if distancia_m(p.latitud, p.longitud, puntos[j].latitud, puntos[j].longitud) <= RADIO_METROS:
                    vecinos.append(j)
    return vecinos


def _grupos(puntos):
    """DBSCAN: devuelve listas de índices, una por grupo. El ruido (puntos sin grupo) se descarta."""
    rejilla = defaultdict(list)
    for i, punto in enumerate(puntos):
        rejilla[_celda(punto)].append(i)
    asignado = [False] * len(puntos)
    grupos = []
    for i in range(len(puntos)):
        if asignado[i]:
            continue
        vecinos = _vecinos(puntos, rejilla, i)
        if len(vecinos) < MIN_REPORTES:
            continue  # ruido por ahora: aún puede resultar punto de borde de otro grupo
        grupo, pendientes = [], [i]
        asignado[i] = True
        while pendientes:
            actual = pendientes.pop()
            grupo.append(actual)
            cerca = _vecinos(puntos, rejilla, actual)
            if len(cerca) >= MIN_REPORTES:  # punto núcleo: su vecindad se une al grupo
                for j in cerca:
                    if not asignado[j]:
                        asignado[j] = True
                        pendientes.append(j)
        grupos.append(grupo)
    return grupos


def _categoria_predominante(puntos):
    cuentas = Counter(p.categoria for p in puntos)
    return max(cuentas, key=lambda c: (cuentas[c], -CATEGORIAS.index(c)))  # ante un empate, la primera de CATEGORIAS


def detectar_focos(puntos):
    """Los focos de una lista de [PuntoDeReporte], ordenados por id. Determinista: el mismo conjunto da los mismos focos.

    El id de un foco es el menor id de reporte que lo integra: se mantiene mientras ese reporte exista y el foco no
    cambie de miembro más antiguo.
    """
    puntos = sorted(puntos, key=lambda p: p.id)
    focos = []
    for grupo in _grupos(puntos):
        miembros = [puntos[i] for i in grupo]
        focos.append(
            Foco(
                id=min(p.id for p in miembros),
                latitud=sum(p.latitud for p in miembros) / len(miembros),
                longitud=sum(p.longitud for p in miembros) / len(miembros),
                num_reportes=len(miembros),
                categoria_predominante=_categoria_predominante(miembros),
                ultimo_reporte=max(p.fecha_hora for p in miembros),
            )
        )
    return sorted(focos, key=lambda f: f.id)
