package com.wanchaqlimpio.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Se ejecuta con Robolectric porque DataStore internamente ramifica en
 * android.os.Build.VERSION.SDK_INT: en un test JVM puro ese valor es 0
 * (no hay Android real) y DataStore cae a File.renameTo(), que en Windows
 * falla si el archivo destino ya existe. Con Robolectric, SDK_INT refleja
 * un valor real (>= 26, igual que minSdk) y usa Files.move(), como en
 * el dispositivo real donde correra la app.
 */
@RunWith(RobolectricTestRunner::class)
class ConsentimientoRepositoryImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun crearRepositorio(): ConsentimientoRepositoryImpl {
        // El archivo se calcula una sola vez: si el lambda de produceFile llamara a
        // tempFolder.newFolder() en cada invocacion, cada llamada devolveria una ruta
        // distinta y DataStore fallaria al renombrar su archivo temporal (justamente
        // el bug que causaba "Unable to rename ... this likely means multiple instances").
        val archivo = File(tempFolder.newFolder(), "test.preferences_pb")
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO),
            ) {
                archivo
            }
        return ConsentimientoRepositoryImpl(dataStore)
    }

    @Test
    fun `antes de aceptar no hay id anonimo`() =
        runTest {
            val estado = crearRepositorio().estado.first()

            assertNull(estado.idAnonimo)
            assertFalse(estado.haConsentido)
        }

    @Test
    fun `aceptar genera un id anonimo y marca el consentimiento`() =
        runTest {
            val repositorio = crearRepositorio()

            repositorio.aceptarConsentimiento()
            val estado = repositorio.estado.first()

            assertNotNull(estado.idAnonimo)
            assertNotNull(estado.fechaConsentimientoEpochMillis)
            assertTrue(estado.haConsentido)
        }

    @Test
    fun `aceptar dos veces conserva el mismo id anonimo`() =
        runTest {
            val repositorio = crearRepositorio()

            repositorio.aceptarConsentimiento()
            val primerId = repositorio.estado.first().idAnonimo
            repositorio.aceptarConsentimiento()
            val segundoId = repositorio.estado.first().idAnonimo

            assertEquals(primerId, segundoId)
        }

    @Test
    fun `revocar borra el id anonimo`() =
        runTest {
            val repositorio = crearRepositorio()

            repositorio.aceptarConsentimiento()
            repositorio.revocarConsentimiento()
            val estado = repositorio.estado.first()

            assertNull(estado.idAnonimo)
            assertFalse(estado.haConsentido)
        }
}
