package com.wanchaqlimpio.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.MotivoFalloEnvio
import com.wanchaqlimpio.app.domain.model.TipoDeRed

/**
 * Reflejo local (Room) de un Reporte, según diseno.md §6. `idAnonimo`
 * reemplaza al `id_usuario` de cuenta del Cap. IV original (ADR-0001).
 */
@Entity(tableName = "reporte")
data class ReporteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idAnonimo: String,
    val categoria: CategoriaResiduo,
    val estado: EstadoReporte,
    val latitud: Double,
    val longitud: Double,
    val fotoPath: String,
    val fechaHoraEpochMillis: Long,
    // Añadidas en la versión 2 de la base (ver Migraciones.kt): la sugerencia CRUDA del modelo de IA.
    val categoriaIa: CategoriaResiduo? = null,
    val confianzaIa: Float? = null,
    // Añadidas en la versión 3 de la base (ver Migraciones.kt): seguimiento del envío para las pruebas de
    // confiabilidad y para explicar al ciudadano por qué un reporte sigue pendiente.
    @ColumnInfo(defaultValue = "0") val intentosEnvio: Int = 0,
    val ultimoIntentoEpochMillis: Long? = null,
    val enviadoEpochMillis: Long? = null,
    val motivoUltimoFallo: MotivoFalloEnvio? = null,
    val codigoHttpUltimoFallo: Int? = null,
    val redAlGuardar: TipoDeRed? = null,
    val redAlUltimoIntento: TipoDeRed? = null,
    // Añadida en la versión 4 de la base (ver Migraciones.kt): precisión declarada por el GPS al aceptar la lectura.
    val precisionMetros: Float? = null,
)
