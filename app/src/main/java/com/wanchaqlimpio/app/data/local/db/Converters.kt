package com.wanchaqlimpio.app.data.local.db

import androidx.room.TypeConverter
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.MotivoFalloEnvio
import com.wanchaqlimpio.app.domain.model.TipoDeRed

class Converters {
    // Aceptan nulo porque categoriaIa es opcional; en las columnas obligatorias nunca llega un nulo.
    @TypeConverter
    fun deCategoriaResiduo(valor: CategoriaResiduo?): String? = valor?.name

    @TypeConverter
    fun aCategoriaResiduo(valor: String?): CategoriaResiduo? = valor?.let(CategoriaResiduo::valueOf)

    @TypeConverter
    fun deEstadoReporte(valor: EstadoReporte): String = valor.name

    @TypeConverter
    fun aEstadoReporte(valor: String): EstadoReporte = EstadoReporte.valueOf(valor)

    // Los datos del envío son opcionales: un reporte que nunca falló no tiene motivo ni red del último intento.
    @TypeConverter
    fun deMotivoFalloEnvio(valor: MotivoFalloEnvio?): String? = valor?.name

    @TypeConverter
    fun aMotivoFalloEnvio(valor: String?): MotivoFalloEnvio? = valor?.let(MotivoFalloEnvio::valueOf)

    @TypeConverter
    fun deTipoDeRed(valor: TipoDeRed?): String? = valor?.name

    @TypeConverter
    fun aTipoDeRed(valor: String?): TipoDeRed? = valor?.let(TipoDeRed::valueOf)
}
