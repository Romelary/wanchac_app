package com.wanchaqlimpio.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.ui.common.ContenidoCentrado

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    OnboardingContent(onAceptar = viewModel::aceptar)
}

/**
 * Contenido "stateless" del onboarding, sin dependencia de Hilt/ViewModel,
 * para poder probarlo con Compose UI Testing sin infraestructura de Hilt.
 */
@Composable
fun OnboardingContent(onAceptar: () -> Unit) {
    var aceptado by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        ContenidoCentrado(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = stringResource(R.string.onboarding_titulo), style = MaterialTheme.typography.headlineSmall)
                Text(text = stringResource(R.string.onboarding_intro))

                OnboardingSeccion(
                    titulo = stringResource(R.string.onboarding_que_se_registra_titulo),
                    texto = stringResource(R.string.onboarding_que_se_registra_texto),
                )
                OnboardingSeccion(
                    titulo = stringResource(R.string.onboarding_para_que_se_usa_titulo),
                    texto = stringResource(R.string.onboarding_para_que_se_usa_texto),
                )
                OnboardingSeccion(
                    titulo = stringResource(R.string.onboarding_que_se_pide_titulo),
                    texto = stringResource(R.string.onboarding_que_se_pide_texto),
                )
                OnboardingSeccion(
                    titulo = stringResource(R.string.onboarding_derechos_titulo),
                    texto = stringResource(R.string.onboarding_derechos_texto),
                )
                Text(text = stringResource(R.string.onboarding_voluntaria_texto))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = aceptado, onCheckedChange = { aceptado = it })
                    Text(text = stringResource(R.string.onboarding_checkbox_texto))
                }

                Button(
                    onClick = onAceptar,
                    enabled = aceptado,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.onboarding_boton_continuar))
                }
            }
        }
    }
}

@Composable
private fun OnboardingSeccion(
    titulo: String,
    texto: String,
) {
    Column {
        Text(text = titulo, style = MaterialTheme.typography.titleMedium)
        Text(text = texto)
    }
}
