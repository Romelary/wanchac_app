package com.wanchaqlimpio.app.data.repository

import com.wanchaqlimpio.app.core.network.MonitorDeRed
import com.wanchaqlimpio.app.core.sync.ProgramadorEliminacion
import com.wanchaqlimpio.app.core.sync.ProgramadorSincronizacion
import com.wanchaqlimpio.app.data.local.db.ReporteDao
import com.wanchaqlimpio.app.data.mapper.aDominio
import com.wanchaqlimpio.app.data.mapper.aEntity
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.domain.model.EstadoReporte
import com.wanchaqlimpio.app.domain.model.Reporte
import com.wanchaqlimpio.app.domain.model.SugerenciaIa
import com.wanchaqlimpio.app.domain.model.Ubicacion
import com.wanchaqlimpio.app.domain.repository.AlmacenFotos
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
        private val programadorSincronizacion: ProgramadorSincronizacion,
        private val programadorEliminacion: ProgramadorEliminacion,
        private val almacenFotos: AlmacenFotos,
        private val monitorDeRed: MonitorDeRed,
    ) : ReporteRepository {
        override suspend fun crearReporte(
            idAnonimo: String,
            categoria: CategoriaResiduo,
            ubicacion: Ubicacion,
            fotoPath: String,
            ahoraEpochMillis: Long,
            sugerenciaIa: SugerenciaIa?,
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
                        categoriaIa = sugerenciaIa?.categoria,
                        confianzaIa = sugerenciaIa?.confianza,
                        precisionMetros = ubicacion.precisionMetros,
                        // Dato técnico para las pruebas de confiabilidad: solo se guarda en el teléfono.
                        redAlGuardar = monitorDeRed.actual(),
                    )
                dao.insertar(reporte.aEntity())
                // Encola la sincronización; WorkManager espera a que haya red
                // (Módulo 4). Si ya está encolada, no se duplica (ver
                // ProgramadorSincronizacionWorkManager).
                programadorSincronizacion.programarSincronizacion()
            }
            return resultado
        }

        override fun observarReportesDe(idAnonimo: String): Flow<List<Reporte>> =
            dao.observarPorDispositivo(idAnonimo).map { lista -> lista.map { it.aDominio() } }

        override fun enviarPendientesAhora() = programadorSincronizacion.sincronizarAhora()

        override suspend fun reintentarEnvio(id: Long) {
            val entity = dao.obtenerPorId(id) ?: return
            if (entity.aDominio().sePuedeReintentar) {
                dao.actualizar(entity.copy(estado = EstadoReporte.PENDIENTE))
                programadorSincronizacion.programarSincronizacion()
            }
        }

        override suspend fun eliminarReporte(id: Long) {
            val entity = dao.obtenerPorId(id) ?: return

            dao.eliminar(id)
            // La foto es evidencia del reporte: si el usuario lo elimina, la imagen no
            // debe quedar en el almacenamiento de la app.
            almacenFotos.borrar(entity.fotoPath)
            // Se solicita la baja remota SIEMPRE, incluso si el reporte figura como
            // PENDIENTE: el servidor pudo haberlo recibido y la respuesta perderse
            // (red inestable), y el cliente no tiene forma de saberlo. Si el servidor
            // no lo tiene, responde 404 y el worker lo da por cumplido (ver
            // EliminarReporteWorker).
            programadorEliminacion.programarEliminacionRemota(entity.idAnonimo, id)
        }
    }
