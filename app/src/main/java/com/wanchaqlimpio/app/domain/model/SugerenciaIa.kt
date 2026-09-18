package com.wanchaqlimpio.app.domain.model

/**
 * Lo que el modelo de IA on-device opina de una foto (RF-04). Solo SUGIERE: el ciudadano confirma o corrige.
 *
 * Se conservan los dos niveles a propósito:
 *  - [categoria] y [confianza] son la respuesta CRUDA del modelo (la clase más probable, sin umbral). Se
 *    guardan en el reporte para medir la exactitud de la IA (Anexo 2) separada de las correcciones humanas y
 *    para recalibrar el umbral con datos de campo sin haber perdido información.
 *  - [propuesta] es lo que la pantalla preselecciona: [categoria] si [confianza] alcanza el umbral del modelo
 *    y [CategoriaResiduo.MIXTO] si no ("no estoy seguro"). El umbral vive en `metadata.json` del modelo.
 *
 * [categoria] es siempre una clase que el modelo predice (nunca `ESCOMBROS` ni `MIXTO`, que son manuales).
 */
data class SugerenciaIa(
    val categoria: CategoriaResiduo,
    val confianza: Float,
    val propuesta: CategoriaResiduo,
)
