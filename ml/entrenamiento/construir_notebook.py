"""Genera `clasificador_residuos.ipynb` a partir de `clasificador_residuos.py`.

El código fuente vive en formato de celdas ("percent format"): cada `# %%` abre una
celda de código y `# %% [markdown]` una celda de texto (sus líneas van comentadas con
`# `). Se mantiene así, y no directamente como .ipynb, porque el .py se revisa y se
compara en git sin el ruido de JSON, y porque el MISMO archivo lo ejecutan las pruebas
automáticas (`probar_notebook.py`): lo que se prueba es exactamente lo que corre en Colab.

Uso (desde esta carpeta):  python construir_notebook.py
Requiere:                  pip install nbformat
"""

from pathlib import Path

import nbformat

AQUI = Path(__file__).parent
FUENTE = AQUI / "clasificador_residuos.py"
DESTINO = AQUI / "clasificador_residuos.ipynb"


def _limpiar(lineas):
    while lineas and not lineas[-1].strip():
        lineas.pop()
    while lineas and not lineas[0].strip():
        lineas.pop(0)
    return "\n".join(lineas)


def leer_celdas(texto):
    celdas, tipo, lineas = [], None, []

    def cerrar():
        if tipo is None:
            return
        if tipo == "markdown":
            cuerpo = _limpiar([l[2:] if l.startswith("# ") else l.lstrip("#") for l in lineas])
            if cuerpo:
                celdas.append(nbformat.v4.new_markdown_cell(cuerpo))
        else:
            cuerpo = _limpiar(lineas)
            if cuerpo:
                celdas.append(nbformat.v4.new_code_cell(cuerpo))

    for linea in texto.splitlines():
        if linea.startswith("# %%"):
            cerrar()
            tipo = "markdown" if "[markdown]" in linea else "code"
            lineas = []
        elif tipo is not None:
            lineas.append(linea)
    cerrar()
    return celdas


def main():
    celdas = leer_celdas(FUENTE.read_text(encoding="utf-8"))
    cuaderno = nbformat.v4.new_notebook(cells=celdas)
    cuaderno.metadata = {
        "colab": {"provenance": [], "gpuType": "T4"},
        "accelerator": "GPU",
        "kernelspec": {"display_name": "Python 3", "name": "python3"},
        "language_info": {"name": "python"},
    }
    nbformat.validate(cuaderno)
    nbformat.write(cuaderno, DESTINO)
    print(f"{DESTINO.name}: {len(celdas)} celdas, válido según nbformat.")


if __name__ == "__main__":
    main()
