package com.wanchaqlimpio.app.data.local.db

import androidx.room.TypeConverter
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte

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
}
