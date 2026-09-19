package com.wanchaqlimpio.app.domain.repository

import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import com.wanchaqlimpio.app.domain.model.Ubicacion
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import kotlinx.coroutines.flow.Flow

interface ReporteRepository {
    /**
     * Valida (ver ValidarNuevoReporte) y, si es válido, guarda el reporte
     * localmente con estado PENDIENTE (offline-first: la sincronización con
     * el backend es responsabilidad del Módulo 4, no de este repositorio).
     *
     * [categoria] es la que el ciudadano confirmó; [sugerenciaIa] es lo que opinó el modelo (`null` si no
     * estuvo disponible) y se guarda aparte, sin alterar la categoría.
     */
    suspend fun crearReporte(
        idAnonimo: String,
        categoria: CategoriaResiduo,
        ubicacion: Ubicacion,
        fotoPath: String,
        ahoraEpochMillis: Long,
        sugerenciaIa: SugerenciaIa? = null,
    ): ResultadoValidacionReporte

    fun observarReportesDe(idAnonimo: String): Flow<List<Reporte>>

    /**
     * Elimina el reporte y su foto del dispositivo y encola la solicitud de baja
     * en el backend (RNF-05, ADR-0002). No hace nada si el reporte no existe.
     */
    suspend fun eliminarReporte(id: Long)

    /** «Enviar ahora»: vuelve a intentar enseguida el envío de los reportes pendientes (si hay red). */
    fun enviarPendientesAhora()

    /**
     * Devuelve a `PENDIENTE` un reporte `RECHAZADO` (si [Reporte.sePuedeReintentar]) y encola su envío. Un rechazo
     * definitivo no se reintenta solo, pero pudo deberse a un error del servidor y no debe dejar el reporte perdido.
     */
    suspend fun reintentarEnvio(id: Long)
}
