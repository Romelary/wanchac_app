package com.wanchaqlimpio.app.data.repository

import android.content.Context
import android.util.Log
import com.wanchaqlimpio.app.domain.repository.AlmacenFotos
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val ETIQUETA_LOG = "AlmacenFotos"
private const val CARPETA_FOTOS = "reportes"

class AlmacenFotosImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AlmacenFotos {
        private val carpeta: File get() = File(context.filesDir, CARPETA_FOTOS)

        override fun nuevaFoto(): File {
            carpeta.mkdirs()
            return File(carpeta, "reporte_${System.currentTimeMillis()}.jpg")
        }

        override suspend fun borrar(fotoPath: String) {
            withContext(Dispatchers.IO) {
                val archivo = File(fotoPath)
                if (archivo.exists() && !archivo.delete()) {
                    Log.w(ETIQUETA_LOG, "No se pudo borrar una foto")
                }
            }
        }

        override suspend fun borrarHuerfanas(
            conservar: Set<String>,
            modificadasAntesDeEpochMillis: Long,
        ) {
            withContext(Dispatchers.IO) {
                carpeta
                    .listFiles()
                    .orEmpty()
                    .filter { it.absolutePath !in conservar && it.lastModified() < modificadasAntesDeEpochMillis }
                    .forEach { if (!it.delete()) Log.w(ETIQUETA_LOG, "No se pudo borrar una foto huérfana") }
            }
        }

        override suspend fun borrarTodas() {
            withContext(Dispatchers.IO) {
                if (carpeta.exists() && !carpeta.deleteRecursively()) {
                    Log.w(ETIQUETA_LOG, "No se pudieron borrar todas las fotos")
                }
            }
        }
    }
