package com.wanchaqlimpio.app.data.mapper

import com.wanchaqlimpio.app.data.local.entity.ReporteEntity
import com.wanchaqlimpio.app.domain.model.FalloDeEnvio
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.SeguimientoDeEnvio

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
        categoriaIa = categoriaIa,
        confianzaIa = confianzaIa,
        precisionMetros = precisionMetros,
        redAlGuardar = redAlGuardar,
        envio =
            SeguimientoDeEnvio(
                intentos = intentosEnvio,
                ultimoIntentoEpochMillis = ultimoIntentoEpochMillis,
                enviadoEpochMillis = enviadoEpochMillis,
                ultimoFallo = motivoUltimoFallo?.let { FalloDeEnvio(it, codigoHttpUltimoFallo) },
                redAlUltimoIntento = redAlUltimoIntento,
            ),
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
        categoriaIa = categoriaIa,
        confianzaIa = confianzaIa,
        intentosEnvio = envio.intentos,
        ultimoIntentoEpochMillis = envio.ultimoIntentoEpochMillis,
        enviadoEpochMillis = envio.enviadoEpochMillis,
        motivoUltimoFallo = envio.ultimoFallo?.motivo,
        codigoHttpUltimoFallo = envio.ultimoFallo?.codigoHttp,
        redAlGuardar = redAlGuardar,
        redAlUltimoIntento = envio.redAlUltimoIntento,
        precisionMetros = precisionMetros,
    )
