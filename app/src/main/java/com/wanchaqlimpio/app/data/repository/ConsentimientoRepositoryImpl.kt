package com.wanchaqlimpio.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wanchaqlimpio.app.domain.model.ConsentimientoEstado
import com.wanchaqlimpio.app.domain.repository.ConsentimientoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

private val KEY_ID_ANONIMO = stringPreferencesKey("id_anonimo")
private val KEY_FECHA_CONSENTIMIENTO = longPreferencesKey("fecha_consentimiento")

class ConsentimientoRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ConsentimientoRepository {
        override val estado: Flow<ConsentimientoEstado> =
            dataStore.data.map { prefs ->
                ConsentimientoEstado(
                    idAnonimo = prefs[KEY_ID_ANONIMO],
                    fechaConsentimientoEpochMillis = prefs[KEY_FECHA_CONSENTIMIENTO],
                )
            }

        override suspend fun aceptarConsentimiento() {
            dataStore.edit { prefs ->
                if (prefs[KEY_ID_ANONIMO] == null) {
                    prefs[KEY_ID_ANONIMO] = UUID.randomUUID().toString()
                }
                prefs[KEY_FECHA_CONSENTIMIENTO] = System.currentTimeMillis()
            }
        }

        override suspend fun revocarConsentimiento() {
            dataStore.edit { prefs ->
                prefs.remove(KEY_ID_ANONIMO)
                prefs.remove(KEY_FECHA_CONSENTIMIENTO)
            }
        }
    }
