package com.wanchaqlimpio.app.domain.model

/**
 * Categorías de residuo del Marco Conceptual (Cap. II). En el diseño original
 * del documento (Cap. IV) esto era una tabla de catálogo (Categoria_residuo);
 * en el cliente se simplifica a un enum porque el conjunto es fijo y pequeño
 * (la normalización completa con FK sí tiene sentido en el backend/PostgreSQL).
 *
 * El clasificador de IA on-device solo predice PLASTICO, CARTON, VIDRIO y ORGANICO (ver `ml/README.md`).
 * ESCOMBROS y MIXTO son de elección manual: ESCOMBROS salió del modelo el 2026-09-18 (ningún dataset
 * público lo cubre y se confundiría con muros y veredas) pero sigue siendo una categoría de la
 * investigación ("Desmonte y escombros" en la ficha de campo); MIXTO es el "no sé" cuando la
 * confianza del modelo queda bajo el umbral. El ORDEN de los cuatro primeros valores debe coincidir con
 * `labels.txt` del modelo: lo comprueba `ml/entrenamiento/probar_notebook.py`.
 */
enum class CategoriaResiduo {
    PLASTICO,
    CARTON,
    VIDRIO,
    ORGANICO,
    ESCOMBROS,
    MIXTO,
}
