package com.wanchaqlimpio.app.data.local.db

import androidx.room.TypeConverter
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte

class Converters {
    @TypeConverter
    fun deCategoriaResiduo(valor: CategoriaResiduo): String = valor.name

    @TypeConverter
    fun aCategoriaResiduo(valor: String): CategoriaResiduo = CategoriaResiduo.valueOf(valor)

    @TypeConverter
    fun deEstadoReporte(valor: EstadoReporte): String = valor.name

    @TypeConverter
    fun aEstadoReporte(valor: String): EstadoReporte = EstadoReporte.valueOf(valor)
}
