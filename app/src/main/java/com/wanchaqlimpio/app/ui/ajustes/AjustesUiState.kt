package com.wanchaqlimpio.app.ui.ajustes

/**
 * [idAnonimo] es `null` solo de forma transitoria (antes de leer DataStore o tras
 * revocar el consentimiento). [borrando] evita un segundo "borrar todo" mientras
 * el primero sigue en curso.
 */
data class AjustesUiState(
    val idAnonimo: String? = null,
    val borrando: Boolean = false,
)
