# -*- coding: utf-8 -*-
"""Cifras de DIMENSIONAMIENTO del plan de pruebas de confiabilidad (docs/plan-confiabilidad-instrumentos.md).

QUÉ ES Y QUÉ NO ES
  Responde una pregunta de PLANIFICACIÓN: "con tantos participantes / unidades / mediciones, ¿qué tan ancho saldrá el
  intervalo de confianza de cada coeficiente, y con qué probabilidad un estudio 'aprobaría' aunque el acuerdo real fuera
  malo?". Son simulaciones bajo supuestos declarados abajo. NO son resultados del estudio ni datos de la investigación:
  no usan ningún dato real ni simulan respuestas para presentarlas como confiabilidad medida.

REPRODUCIBILIDAD
  Solo biblioteca estándar de Python (sin numpy/scipy). Semilla fija. `python dimensionamiento.py` imprime todas las
  tablas del documento. Las funciones de distribución (F, ji-cuadrado) se comprueban al arrancar contra valores de
  tablas conocidas; si alguna no coincide, el script se detiene en vez de imprimir cifras dudosas.

SUPUESTOS (los que cambian las cifras)
  - Alfa de Cronbach: intervalo exacto de Feldt (1965), válido con ítems esencialmente tau-equivalentes y respuestas
    aproximadamente normales; con escala Likert de 5 puntos y n pequeño es una aproximación.
  - Kappa: dos evaluadores, misma distribución marginal, acuerdo verdadero fijado; unidades independientes.
  - Repetibilidad del GPS: errores gaussianos, sin sesgo, ejes independientes (modelo circular).
"""

import math
import random
import sys

# ---------------------------------------------------------------------------------------------------------------
# Distribuciones (implementadas a mano para no depender de scipy)
# ---------------------------------------------------------------------------------------------------------------


def _betacf(a, b, x, maxit=500, eps=3e-14):
    """Fracción continua de la beta incompleta (método de Lentz, Numerical Recipes 6.4)."""
    tiny = 1e-300
    qab, qap, qam = a + b, a + 1.0, a - 1.0
    c, d = 1.0, 1.0 - qab * x / qap
    d = tiny if abs(d) < tiny else d
    d = 1.0 / d
    h = d
    for m in range(1, maxit + 1):
        m2 = 2 * m
        aa = m * (b - m) * x / ((qam + m2) * (a + m2))
        d = 1.0 + aa * d
        d = tiny if abs(d) < tiny else d
        c = 1.0 + aa / c
        c = tiny if abs(c) < tiny else c
        d = 1.0 / d
        h *= d * c
        aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
        d = 1.0 + aa * d
        d = tiny if abs(d) < tiny else d
        c = 1.0 + aa / c
        c = tiny if abs(c) < tiny else c
        d = 1.0 / d
        delta = d * c
        h *= delta
        if abs(delta - 1.0) < eps:
            return h
    raise RuntimeError("betacf no convergió")


def beta_inc(a, b, x):
    """Beta incompleta regularizada I_x(a, b)."""
    if x <= 0.0:
        return 0.0
    if x >= 1.0:
        return 1.0
    log_bt = math.lgamma(a + b) - math.lgamma(a) - math.lgamma(b) + a * math.log(x) + b * math.log(1.0 - x)
    bt = math.exp(log_bt)
    if x < (a + 1.0) / (a + b + 2.0):
        return bt * _betacf(a, b, x) / a
    return 1.0 - bt * _betacf(b, a, 1.0 - x) / b


def f_cdf(x, d1, d2):
    if x <= 0:
        return 0.0
    return beta_inc(d1 / 2.0, d2 / 2.0, d1 * x / (d1 * x + d2))


def _gamma_inc_reg(a, x):
    """Gamma incompleta inferior regularizada P(a, x) (serie o fracción continua)."""
    if x <= 0:
        return 0.0
    if x < a + 1.0:
        term = 1.0 / a
        total = term
        n = a
        for _ in range(2000):
            n += 1.0
            term *= x / n
            total += term
            if abs(term) < abs(total) * 1e-15:
                break
        return total * math.exp(-x + a * math.log(x) - math.lgamma(a))
    tiny = 1e-300
    b = x + 1.0 - a
    c = 1.0 / tiny
    d = 1.0 / b
    h = d
    for i in range(1, 2000):
        an = -i * (i - a)
        b += 2.0
        d = an * d + b
        d = tiny if abs(d) < tiny else d
        c = b + an / c
        c = tiny if abs(c) < tiny else c
        d = 1.0 / d
        delta = d * c
        h *= delta
        if abs(delta - 1.0) < 1e-15:
            break
    return 1.0 - math.exp(-x + a * math.log(x) - math.lgamma(a)) * h


def chi2_cdf(x, df):
    return _gamma_inc_reg(df / 2.0, x / 2.0)


def _cuantil(cdf, p, alto=1.0):
    while cdf(alto) < p:
        alto *= 2.0
    bajo = 0.0
    for _ in range(200):
        medio = (bajo + alto) / 2.0
        if cdf(medio) < p:
            bajo = medio
        else:
            alto = medio
    return (bajo + alto) / 2.0


def f_quantil(p, d1, d2):
    return _cuantil(lambda x: f_cdf(x, d1, d2), p)


def chi2_quantil(p, df):
    return _cuantil(lambda x: chi2_cdf(x, df), p, alto=max(4.0, df))


def comprobar_distribuciones():
    """Valores de tablas publicadas; si no coinciden, no se imprime nada."""
    controles = [
        ("F 0.975 (10, 20)", f_quantil(0.975, 10, 20), 2.7737),
        ("F 0.95 (5, 10)", f_quantil(0.95, 5, 10), 3.3258),
        ("chi2 0.975 (4)", chi2_quantil(0.975, 4), 11.1433),
        ("chi2 0.025 (4)", chi2_quantil(0.025, 4), 0.4844),
        ("chi2 0.975 (32)", chi2_quantil(0.975, 32), 49.4804),
        ("chi2 0.025 (32)", chi2_quantil(0.025, 32), 18.2908),
    ]
    for nombre, obtenido, esperado in controles:
        if abs(obtenido - esperado) > 5e-4:
            sys.exit(f"COMPROBACIÓN FALLIDA {nombre}: obtuvo {obtenido:.4f}, esperado {esperado:.4f}")


# ---------------------------------------------------------------------------------------------------------------
# Estadísticos
# ---------------------------------------------------------------------------------------------------------------


def wilson(exitos, n, z=1.96):
    """Intervalo de Wilson del 95 % para una proporción (mismo cálculo que ml/entrenamiento/probar_modelo.py)."""
    p = exitos / n
    den = 1 + z * z / n
    centro = (p + z * z / (2 * n)) / den
    margen = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / den
    return max(0.0, centro - margen), min(1.0, centro + margen)


def alfa_feldt(alfa_hat, n, k, nivel=0.95):
    """IC exacto de Feldt: (1 - alfa)/(1 - alfa_hat) ~ F(n-1, (n-1)(k-1))."""
    d1, d2 = n - 1, (n - 1) * (k - 1)
    g = (1 - nivel) / 2
    inferior = 1 - (1 - alfa_hat) * f_quantil(1 - g, d1, d2)
    superior = 1 - (1 - alfa_hat) * f_quantil(g, d1, d2)
    return inferior, superior


def kappa_2x2(a, b, c, d):
    """Kappa de Cohen con la tabla [[a, b], [c, d]]; None si no está definido (Pe = 1)."""
    n = a + b + c + d
    po = (a + d) / n
    pe = ((a + b) * (a + c) + (c + d) * (b + d)) / (n * n)
    if pe >= 1.0:
        return None
    return (po - pe) / (1 - pe)


def tabla_teorica(prevalencia, kappa_verdadero):
    """Probabilidades de las 4 celdas con la misma marginal en ambos evaluadores y un kappa dado."""
    pe = prevalencia**2 + (1 - prevalencia) ** 2
    po = kappa_verdadero * (1 - pe) + pe
    d = (1 - po) / 2
    p11, p00 = prevalencia - d, 1 - prevalencia - d
    if min(p11, p00, d) < -1e-12:
        raise ValueError("kappa inalcanzable con esa prevalencia")
    return [p11, d, d, p00]


def simular_kappa(n, prevalencia, kappa_verdadero, repeticiones, semilla):
    """Devuelve (percentil 2.5, percentil 97.5, P(kappa_hat >= 0.61), proporción indefinida)."""
    azar = random.Random(semilla)
    celdas = tabla_teorica(prevalencia, kappa_verdadero)
    acumulada = [sum(celdas[: i + 1]) for i in range(4)]
    valores, indefinidos = [], 0
    for _ in range(repeticiones):
        cuenta = [0, 0, 0, 0]
        for _ in range(n):
            u = azar.random()
            for i, tope in enumerate(acumulada):
                if u <= tope:
                    cuenta[i] += 1
                    break
            else:
                cuenta[3] += 1
        k = kappa_2x2(*cuenta)
        if k is None:
            indefinidos += 1
        else:
            valores.append(k)
    valores.sort()
    if not valores:
        return None, None, 0.0, 1.0
    lo = valores[int(0.025 * (len(valores) - 1))]
    hi = valores[int(0.975 * (len(valores) - 1))]
    aprueba = sum(v >= 0.61 for v in valores) / repeticiones  # los indefinidos cuentan como NO aprobados
    return lo, hi, aprueba, indefinidos / repeticiones


def icc_1_1_ic(icc, a, k, nivel=0.95):
    """IC "típico" del ICC(1,1) de un diseño de a sujetos x k réplicas, suponiendo que el F observado es el que
    implica el ICC verdadero (Shrout y Fleiss, 1979)."""
    f_obs = (1 + (k - 1) * icc) / (1 - icc)
    g = (1 - nivel) / 2
    fl = f_obs / f_quantil(1 - g, a - 1, a * (k - 1))
    fu = f_obs * f_quantil(1 - g, a * (k - 1), a - 1)
    return (fl - 1) / (fl + k - 1), (fu - 1) / (fu + k - 1)


# ---------------------------------------------------------------------------------------------------------------
# Informe
# ---------------------------------------------------------------------------------------------------------------


def main():
    comprobar_distribuciones()
    print("Comprobación de las distribuciones F y ji-cuadrado contra tablas conocidas: OK\n")

    print("=" * 100)
    print("1. TAMAÑO DE MUESTRA DEL SUS (Anexo 2/3, Tabla 1 del documento): n = (Z·σ/E)²")
    print("=" * 100)
    n_sus = (1.96 * 12.5 / 4) ** 2
    print(f"   (1,96 × 12,5 / 4)² = {n_sus:.2f}  ->  {math.ceil(n_sus)} participantes (el documento dice 38: coincide)\n")

    print("=" * 100)
    print("2. ALFA DE CRONBACH: intervalo de confianza del 95 % (Feldt) según n, k y el alfa observado")
    print("=" * 100)
    print("   k = 10 ítems (SUS completo)")
    print(f"   {'n':>4} | " + " | ".join(f"alfa = {a:.2f}: IC 95 % (± media)".ljust(34) for a in (0.70, 0.80, 0.90)))
    for n in (15, 20, 30, 38, 50, 100):
        celdas = []
        for a in (0.70, 0.80, 0.90):
            lo, hi = alfa_feldt(a, n, 10)
            celdas.append(f"[{lo:.2f}; {hi:.2f}]  (±{(hi - lo) / 2:.2f})".ljust(34))
        print(f"   {n:>4} | " + " | ".join(celdas))
    print("\n   Subescalas del documento (n = 38): aprendizaje = ítems 4, 7, 10 (k = 3); uso = ítems 2, 3, 5, 6, 8, 9 (k = 6)")
    for k, nombre in ((3, "aprendizaje (k=3)"), (6, "uso (k=6)")):
        for a in (0.60, 0.70, 0.80):
            lo, hi = alfa_feldt(a, 38, k)
            print(f"   {nombre:<20} alfa observado {a:.2f}: IC 95 % [{lo:.2f}; {hi:.2f}]")
    print("\n   Error estándar de medida del SUS: SEM = DE × √(1 − alfa). Con DE = 12,5 puntos SUS (Tabla 1):")
    for a in (0.70, 0.80, 0.90):
        print(f"   alfa = {a:.2f}  ->  SEM = {12.5 * math.sqrt(1 - a):.1f} puntos SUS")

    print()
    print("=" * 100)
    print("3. KAPPA DE COHEN entre dos observadores: cómo varía el kappa observado de un estudio a otro")
    print("=" * 100)
    repeticiones = 10000
    print(f"   Simulación de {repeticiones} estudios por celda, binario (p. ej. «agrupación confirmada»: Sí/No), misma prevalencia")
    print("   en ambos evaluadores. Columnas: rango que cubre el 95 % de los kappa observados | P(kappa observado ≥ 0,61)")
    print("   | % de estudios donde kappa no se puede calcular (un evaluador usó una sola categoría en todas las unidades).")
    for prevalencia in (0.50, 0.70, 0.85):
        print(f"\n   Prevalencia de «Sí» = {prevalencia:.2f}")
        print(f"   {'kappa verdadero':<16}" + "".join(f"n = {n:<3}".ljust(30) for n in (10, 15, 20, 30, 50)))
        for kv in (0.40, 0.60, 0.75, 0.90):
            try:
                tabla_teorica(prevalencia, kv)
            except ValueError:
                print(f"   {kv:<16.2f}(inalcanzable con esa prevalencia)")
                continue
            fila = f"   {kv:<16.2f}"
            for n in (10, 15, 20, 30, 50):
                lo, hi, ap, ind = simular_kappa(n, prevalencia, kv, repeticiones, semilla=20260919 + n)
                if lo is None:
                    fila += "indefinido".ljust(30)
                else:
                    fila += f"[{lo:.2f};{hi:.2f}] P={ap:.0%} ind={ind:.0%}".ljust(30)
            print(fila)

    print()
    print("=" * 100)
    print("4. REPETIBILIDAD DEL GPS (Instrumento 3): 8 puntos × 5 mediciones")
    print("=" * 100)
    for df, nombre in ((4, "un punto (5 mediciones, gl = 4)"), (32, "los 8 puntos juntos (gl = 8 × 4 = 32)"), (64, "16 puntos o 2 teléfonos (gl = 64)")):
        inf = math.sqrt(df / chi2_quantil(0.975, df))
        sup = math.sqrt(df / chi2_quantil(0.025, df))
        print(f"   DE de {nombre}: el valor verdadero está entre {inf:.2f}× y {sup:.2f}× la DE observada (IC 95 %)")
    print("\n   ICC(1,1) de un diseño 8 puntos × 5 réplicas: IC 95 % «típico» según el ICC verdadero")
    for icc in (0.50, 0.70, 0.80, 0.90):
        lo, hi = icc_1_1_ic(icc, 8, 5)
        print(f"   ICC = {icc:.2f}  ->  [{lo:.2f}; {hi:.2f}]")
    print("\n   Precisión declarada por Android (radio del 68 %, A) y separación entre dos lecturas independientes del mismo sitio")
    print("   (modelo circular: DE por eje σ = A / 1,510; la distancia entre dos lecturas sigue una Rayleigh con escala σ·√2)")
    r68 = math.sqrt(-2 * math.log(0.32))
    print(f"   Factor r68 = √(−2·ln 0,32) = {r68:.4f}")
    print(f"   {'A declarada':>12} {'σ por eje':>10} {'P95 separación':>15} {'P99':>8} {'P99,9':>8}   (ε del agrupamiento = 50 m)")
    for A in (5, 10, 15, 20, 30):
        sigma = A / r68
        def q(p):
            return sigma * math.sqrt(2) * math.sqrt(-2 * math.log(1 - p))
        print(f"   {A:>10} m {sigma:>8.1f} m {q(0.95):>13.1f} m {q(0.99):>6.1f} m {q(0.999):>6.1f} m")
    print("   Si el error REAL fuera el doble del declarado (declaración optimista), multiplicar la separación por 2.")

    print()
    print("=" * 100)
    print("5. INTERVALOS DE WILSON DE UNA PROPORCIÓN (sincronización, concordancia del filtro, etc.)")
    print("=" * 100)
    print(f"   {'exitos/n':>10}  {'proporción':>10}   IC 95 % de Wilson")
    for exitos, n in ((13, 13), (12, 13), (20, 20), (19, 20), (40, 40), (38, 40), (36, 40), (73, 73), (72, 73), (100, 100), (95, 100)):
        lo, hi = wilson(exitos, n)
        print(f"   {exitos:>5}/{n:<4}  {exitos / n:>10.1%}   [{lo:.1%}; {hi:.1%}]")
    print("   n sin fallos necesarios para que el límite inferior de Wilson llegue a 95 %: "
          + str(next(n for n in range(5, 500) if wilson(n, n)[0] >= 0.95)))


if __name__ == "__main__":
    main()
