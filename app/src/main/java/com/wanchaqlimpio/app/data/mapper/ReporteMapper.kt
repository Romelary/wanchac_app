package com.wanchaqlimpio.app.data.mapper

import com.wanchaqlimpio.app.data.local.entity.ReporteEntity
import com.wanchaqlimpio.app.domain.model.Reporte

fun ReporteEntity.aDominio(): Reporte =
    Reporte(
        id = id,
        idAnonimo = idAnonimo,
        categoria = categoria,
        estado = estado,
        latitud = latitud,
        longitud = longitud,
        fotoPath = fotoPath,
        fechaHoraEpochMillis = fechaHoraEpochMillis,
    )

fun Reporte.aEntity(): ReporteEntity =
    ReporteEntity(
        id = id,
        idAnonimo = idAnonimo,
        categoria = categoria,
        estado = estado,
        latitud = latitud,
        longitud = longitud,
        fotoPath = fotoPath,
        fechaHoraEpochMillis = fechaHoraEpochMillis,
    )
