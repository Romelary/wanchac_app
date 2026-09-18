package com.wanchaqlimpio.app.domain.usecase

import com.wanchaqlimpio.app.domain.repository.AlmacenFotos
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import com.wanchaqlimpio.app.domain.repository.ReporteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * "Borrar todos mis datos" (RNF-05, ADR-0002): elimina todos los reportes del
 * dispositivo (cada uno borra su foto y encola su baja en el backend), limpia
 * cualquier foto sin reporte asociado y, al final, revoca el consentimiento.
 *
 * La revocación va **al final** a propósito: si algo falla a mitad de camino, el
 * usuario sigue con su consentimiento y puede volver a intentarlo, porque el
 * `id_anonimo` (necesario para listar sus reportes) aún existe. Todos los pasos
 * son idempotentes.
 */
class BorrarTodosMisDatos
    @Inject
    constructor(
        private val reporteRepository: ReporteRepository,
        private val consentimientoRepository: ConsentimientoRepository,
        private val almacenFotos: AlmacenFotos,
    ) {
        suspend operator fun invoke() {
            val idAnonimo = consentimientoRepository.estado.first().idAnonimo
            if (idAnonimo != null) {
                reporteRepository
                    .observarReportesDe(idAnonimo)
                    .first()
                    .forEach { reporteRepository.eliminarReporte(it.id) }
            }
            // Fotos sin reporte (p. ej. una captura interrumpida por el cierre de la app).
            almacenFotos.borrarTodas()
            consentimientoRepository.revocarConsentimiento()
        }
    }
