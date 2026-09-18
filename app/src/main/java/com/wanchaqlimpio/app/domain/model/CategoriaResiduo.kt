package com.wanchaqlimpio.app.domain.model

/**
 * Categorías de residuo del Marco Conceptual (Cap. II). En el diseño original
 * del documento (Cap. IV) esto era una tabla de catálogo (Categoria_residuo);
 * en el cliente se simplifica a un enum porque el conjunto es fijo y pequeño
 * (la normalización completa con FK sí tiene sentido en el backend/PostgreSQL).
 */
enum class CategoriaResiduo {
    PLASTICO,
    CARTON,
    VIDRIO,
    ORGANICO,
    ESCOMBROS,
    MIXTO,
}
