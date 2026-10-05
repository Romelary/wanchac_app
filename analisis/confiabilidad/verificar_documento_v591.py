# -*- coding: utf-8 -*-
"""Comprobaciones aritméticas sobre «Proyecto de Investigación v5.91» (evidencia del diagnóstico de
docs/plan-confiabilidad-instrumentos.md, §2). Solo biblioteca estándar; no lee el .docx: los datos de los Anexos 10-12
(Tablas 34-36) están copiados aquí tal cual (fuente: el documento v5.91, leído el 2026-09-19).

  A. ¿La «V de Aiken» de los Anexos 10-12 es la V de Aiken? Se recalculan (a) el promedio de los tres porcentajes / 100 y
     (b) la V de Aiken como la describe el texto (V = S / [n (c - 1)], recodificando cada porcentaje a las 5 bandas de la
     ficha institucional: 0-20 deficiente, 21-40 regular, 41-60 bueno, 61-80 muy bueno, 81-100 excelente), y la probabilidad
     EXACTA de cada V si los 3 jueces eligieran al azar entre las 5 categorías.
  B. ¿Es posible el kappa que se reporta en «Confiabilidad de la Ficha de Observación de Campo»? Con 6 focos (F01-F06) y el
     acuerdo que el texto declara, se enumeran TODAS las tablas de contingencia posibles y se ve el rango de kappa.
  C. Tabla 3: promedios por juez y la «dispersión de 59.2 a 94.0» que menciona el texto.

Esto NO evalúa la confiabilidad de ningún instrumento: solo comprueba si las cifras del documento son coherentes entre sí.

Uso:  python verificar_documento_v591.py
"""

import itertools

# (criterio, (juez 1 %, juez 2 %, juez 3 %), «V de Aiken» que muestra el anexo)
ANEXO_10_SUS = [
    ("Claridad", (90, 95, 65), 0.83),
    ("Objetividad", (85, 92, 65), 0.81),
    ("Actualidad", (95, 94, 60), 0.83),
    ("Organización", (90, 91, 70), 0.84),
    ("Suficiencia", (90, 90, 68), 0.83),
    ("Intencionalidad", (95, 96, 70), 0.87),
    ("Consistencia", (90, 93, 60), 0.81),
    ("Coherencia", (90, 95, 60), 0.82),
    ("Metodología", (90, 92, 75), 0.86),
    ("Conveniencia", (85, 94, 75), 0.85),
]

ANEXO_11_FICHA_DE_CAMPO = [
    ("Claridad", (95, 95, 75), 0.85),
    ("Objetividad", (95, 91, 60), 0.84),
    ("Actualidad", (95, 94, 75), 0.83),
    ("Organización", (95, 92, 50), 0.86),
    ("Suficiencia", (90, 95, 65), 0.84),
    ("Intencionalidad", (95, 93, 50), 0.86),
    ("Consistencia", (95, 94, 50), 0.83),
    ("Coherencia", (95, 93, 55), 0.83),
    ("Metodología", (95, 96, 60), 0.89),
    ("Conveniencia", (90, 90, 61), 0.85),
]

ANEXO_12_FICHA_GPS = [
    ("Claridad", (85, 94, 62), 0.81),
    ("Objetividad", (95, 93, 65), 0.84),
    ("Actualidad", (95, 95, 65), 0.83),
    ("Organización", (85, 92, 61), 0.82),
    ("Suficiencia", (85, 91, 66), 0.81),
    ("Intencionalidad", (95, 94, 65), 0.86),
    ("Consistencia", (90, 96, 55), 0.82),
    ("Coherencia", (90, 93, 60), 0.81),
    ("Metodología", (95, 95, 61), 0.88),
    ("Conveniencia", (90, 92, 60), 0.86),
]

ANEXOS = (
    ("Anexo 10 · Instrumento 1 (SUS)", ANEXO_10_SUS),
    ("Anexo 11 · Instrumento 2 (Ficha de campo)", ANEXO_11_FICHA_DE_CAMPO),
    ("Anexo 12 · Instrumento 3 (Ficha GPS)", ANEXO_12_FICHA_GPS),
)


def banda(p):
    """Banda de la ficha institucional (1 = deficiente ... 5 = excelente)."""
    return 1 if p <= 20 else 2 if p <= 40 else 3 if p <= 60 else 4 if p <= 80 else 5


def v_aiken(categorias, c=5):
    return sum(x - 1 for x in categorias) / (len(categorias) * (c - 1))


def p_bajo_azar(s, n=3, c=5):
    """P(S >= s) si cada uno de los n jueces eligiera al azar entre c categorías (todas las combinaciones)."""
    cuenta = sum(1 for combo in itertools.product(range(c), repeat=n) if sum(combo) >= s)
    return cuenta / c**n


def comprobacion_a():
    print("=" * 96)
    print("A. «V de Aiken» de los Anexos 10-12: ¿es la V de Aiken?")
    print("=" * 96)
    for titulo, filas in ANEXOS:
        print()
        print(titulo)
        print(f"   {'criterio':<16}{'jueces (%)':<18}{'V del anexo':>12}{'promedio/100':>14}{'V de Aiken':>12}")
        iguales = 0
        for criterio, p, v_anexo in filas:
            promedio = sum(p) / 3 / 100
            iguales += round(promedio, 2) == v_anexo
            v = v_aiken([banda(x) for x in p])
            print(f"   {criterio:<16}{str(p):<18}{v_anexo:>12.2f}{promedio:>14.3f}{v:>12.3f}")
        v_anexo_medio = sum(f[2] for f in filas) / len(filas)
        v_real_medio = sum(v_aiken([banda(x) for x in f[1]]) for f in filas) / len(filas)
        print(f"   El valor del anexo coincide con el promedio/100 en {iguales} de {len(filas)} criterios.")
        print(f"   V global: anexo = {v_anexo_medio:.3f} | V de Aiken recalculada = {v_real_medio:.3f}")
    print()
    print("   Probabilidad exacta de cada V con 3 jueces y 5 categorías si eligieran al azar:")
    for s in range(12, 7, -1):
        print(f"   V >= {s / 12:.3f}: p = {p_bajo_azar(s):.3f}")


def _composiciones(total, partes):
    if partes == 1:
        yield (total,)
        return
    for i in range(total + 1):
        for resto in _composiciones(total - i, partes - 1):
            yield (i,) + resto


def _kappa(tabla, c):
    n = sum(tabla)
    m = [[tabla[i * c + j] for j in range(c)] for i in range(c)]
    po = sum(m[i][i] for i in range(c)) / n
    fila = [sum(m[i]) / n for i in range(c)]
    col = [sum(m[i][j] for i in range(c)) / n for j in range(c)]
    pe = sum(fila[i] * col[i] for i in range(c))
    return None if pe >= 1 else (po - pe) / (1 - pe)


def rango_kappa(n, categorias, coincidencias):
    ks = []
    for tabla in _composiciones(n, categorias * categorias):
        if sum(tabla[i * categorias + i] for i in range(categorias)) != coincidencias:
            continue
        k = _kappa(tabla, categorias)
        if k is not None:
            ks.append(k)
    return min(ks), max(ks), len(ks)


def comprobacion_b():
    print()
    print("=" * 96)
    print("B. ¿Es posible el kappa que dice el documento? (6 focos, F01-F06)")
    print("=" * 96)
    casos = (
        ("Magnitud del foco (Bajo/Medio/Alto): 3 categorías, acuerdo 66,7 % = 4 de 6", 3, 4, "«kappa moderado-sustancial»"),
        ("Composición del residuo: 4 categorías, acuerdo 83,3 % = 5 de 6", 4, 5, "«casi perfecta (kappa > 0,80)»"),
    )
    for etiqueta, categorias, coincidencias, dice in casos:
        lo, hi, n_tablas = rango_kappa(6, categorias, coincidencias)
        print()
        print(f"   {etiqueta}")
        print(f"   El texto dice: {dice}")
        print(f"   Tablas de contingencia posibles: {n_tablas} | kappa mínimo = {lo:.3f} | kappa máximo = {hi:.3f}")
    print()
    print("   Bandas de Landis y Koch (1977): 0,41-0,60 moderado; 0,61-0,80 sustancial; 0,81-1,00 casi perfecto")


def comprobacion_c():
    print()
    print("=" * 96)
    print("C. Tabla 3: promedios por juez")
    print("=" * 96)
    todos = []
    for titulo, filas in ANEXOS:
        medias = [sum(f[1][j] for f in filas) / len(filas) for j in range(3)]
        todos += medias
        print(f"   {titulo:<44} jueces 1, 2, 3: {medias[0]:.1f} | {medias[1]:.1f} | {medias[2]:.1f}")
    print(f"   Mínimo de los 9 promedios = {min(todos):.1f} (el texto habla de una dispersión «de 59.2 a 94.0»)")
    minimo = min(p for _, filas in ANEXOS for f in filas for p in f[1])
    print(f"   Puntaje individual más bajo de todos los anexos = {minimo:.0f}")


if __name__ == "__main__":
    comprobacion_a()
    comprobacion_b()
    comprobacion_c()
