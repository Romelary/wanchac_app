package com.wanchaqlimpio.app.domain.repository

import java.io.File

/**
 * Único lugar que sabe dónde se guardan las fotos de los reportes (almacenamiento
 * privado de la app). Centralizarlo permite garantizar que ninguna foto quede
 * huérfana: la foto es evidencia de un reporte y puede contener datos personales
 * (rostros, placas — Ética, Cap. I), así que si el reporte no se guarda o se
 * elimina, la imagen también debe desaparecer.
 */
interface AlmacenFotos {
    /** Archivo nuevo (aún sin escribir) dentro de la carpeta privada de fotos. */
    fun nuevaFoto(): File

    /** Borra una foto; no falla si el archivo ya no existe. */
    suspend fun borrar(fotoPath: String)

    /** Borra todas las fotos, incluidas las que ya no tienen un reporte asociado. */
    suspend fun borrarTodas()
}
