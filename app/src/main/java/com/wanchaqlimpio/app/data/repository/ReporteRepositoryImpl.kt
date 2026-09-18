package com.wanchaqlimpio.app.data.repository

import android.util.Log
import com.wanchaqlimpio.app.core.sync.ProgramadorEliminacion
import com.wanchaqlimpio.app.core.sync.ProgramadorSincronizacion
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val ETIQUETA_LOG = "ReporteRepository"

class ReporteRepositoryImpl
    @Inject
    constructor(
        private val dao: ReporteDao,
        private val validar: ValidarNuevoReporte,
        private val programadorSincronizacion: ProgramadorSincronizacion,
        private val programadorEliminacion: ProgramadorEliminacion,
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
                // Encola la sincronización; WorkManager espera a que haya red
                // (Módulo 4). Si ya está encolada, no se duplica (ver
                // ProgramadorSincronizacionWorkManager).
                programadorSincronizacion.programarSincronizacion()
            }
            return resultado
        }

        override fun observarReportesDe(idAnonimo: String): Flow<List<Reporte>> =
            dao.observarPorDispositivo(idAnonimo).map { lista -> lista.map { it.aDominio() } }

        override suspend fun eliminarReporte(id: Long) {
            val entity = dao.obtenerPorId(id) ?: return

            dao.eliminar(id)
            borrarFoto(entity.fotoPath)
            // Se solicita la baja remota SIEMPRE, incluso si el reporte figura como
            // PENDIENTE: el servidor pudo haberlo recibido y la respuesta perderse
            // (red inestable), y el cliente no tiene forma de saberlo. Si el servidor
            // no lo tiene, responde 404 y el worker lo da por cumplido (ver
            // EliminarReporteWorker).
            programadorEliminacion.programarEliminacionRemota(entity.idAnonimo, id)
        }

        // La foto es evidencia del reporte: si el usuario lo elimina, la imagen no debe
        // quedar en el almacenamiento de la app.
        private suspend fun borrarFoto(fotoPath: String) =
            withContext(Dispatchers.IO) {
                val archivo = File(fotoPath)
                if (archivo.exists() && !archivo.delete()) {
                    Log.w(ETIQUETA_LOG, "No se pudo borrar la foto del reporte eliminado")
                }
            }
    }
