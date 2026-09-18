package com.wanchaqlimpio.app.data.repository

import com.wanchaqlimpio.app.data.local.db.ReporteDao
import com.wanchaqlimpio.app.data.mapper.aDominio
import com.wanchaqlimpio.app.data.mapper.aEntity
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.Ubicacion
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import com.wanchaqlimpio.app.domain.usecase.ResultadoValidacionReporte
import com.wanchaqlimpio.app.domain.usecase.ValidarNuevoReporte
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReporteRepositoryImpl
    @Inject
    constructor(
        private val dao: ReporteDao,
        private val validar: ValidarNuevoReporte,
    ) : ReporteRepository {
        override suspend fun crearReporte(
            idAnonimo: String,
            categoria: CategoriaResiduo,
            ubicacion: Ubicacion,
            fotoPath: String,
            ahoraEpochMillis: Long,
        ): ResultadoValidacionReporte {
            val previos = dao.obtenerPorDispositivo(idAnonimo).map { it.aDominio() }
            val resultado = validar(ubicacion, previos, ahoraEpochMillis)
            if (resultado == ResultadoValidacionReporte.Valido) {
                val reporte =
                    Reporte(
                        idAnonimo = idAnonimo,
                        categoria = categoria,
                        // Offline-first: siempre se guarda local primero con PENDIENTE.
                        // La sincronización con el backend es del Módulo 4, no de aquí.
                        estado = EstadoReporte.PENDIENTE,
                        latitud = ubicacion.latitud,
                        longitud = ubicacion.longitud,
                        fotoPath = fotoPath,
                        fechaHoraEpochMillis = ahoraEpochMillis,
                    )
                dao.insertar(reporte.aEntity())
            }
            return resultado
        }

        override fun observarReportesDe(idAnonimo: String): Flow<List<Reporte>> =
            dao.observarPorDispositivo(idAnonimo).map { lista -> lista.map { it.aDominio() } }
    }
