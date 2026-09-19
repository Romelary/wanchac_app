package com.wanchaqlimpio.app.data.repository

import com.wanchaqlimpio.app.core.util.reducirFotoEnDisco
import com.wanchaqlimpio.app.domain.repository.ReductorDeFotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ReductorDeFotosImpl
    @Inject
    constructor() : ReductorDeFotos {
        // Decodificar y comprimir una foto de varios megapíxeles son cientos de ms de CPU: nunca en el hilo principal.
        override suspend fun reducir(fotoPath: String): Boolean =
            withContext(Dispatchers.Default) { reducirFotoEnDisco(File(fotoPath)) }
    }
