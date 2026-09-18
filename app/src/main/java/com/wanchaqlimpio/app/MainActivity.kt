package com.wanchaqlimpio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanchaqlimpio.app.ui.misreportes.MisReportesScreen
import com.wanchaqlimpio.app.ui.onboarding.OnboardingScreen
import com.wanchaqlimpio.app.ui.onboarding.OnboardingViewModel
import com.wanchaqlimpio.app.ui.reporte.ReporteScreen
import com.wanchaqlimpio.app.ui.theme.WanchaqLimpioTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WanchaqLimpioTheme {
                WanchaqLimpioRoot()
            }
        }
    }
}

/**
 * Decide qué mostrar según el estado del consentimiento informado (Anexo 9):
 * mientras se lee DataStore, el onboarding si no se aceptó, o la app si ya se aceptó.
 * Ver docs/diseno.md §9 y docs/adr/0001-sin-login-id-anonimo.md.
 */
@Composable
private fun WanchaqLimpioRoot(onboardingViewModel: OnboardingViewModel = hiltViewModel()) {
    val estado by onboardingViewModel.estado.collectAsStateWithLifecycle()

    when {
        estado == null -> LoadingScreen()
        estado?.haConsentido == true -> HomePlaceholder()
        else -> OnboardingScreen(viewModel = onboardingViewModel)
    }
}

@Composable
private fun LoadingScreen() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

// TODO(Módulo 5): reemplazar por la navegación real (Mapa, Reportar, Mis reportes, Ajustes).
@PreviewScreenSizes
@Composable
fun HomePlaceholder() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.REPORTAR) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label,
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it },
                )
            }
        },
    ) {
        when (currentDestination) {
            AppDestinations.REPORTAR -> ReporteScreen()
            AppDestinations.MIS_REPORTES -> MisReportesScreen()
            AppDestinations.PROFILE ->
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding),
                    )
                }
        }
    }
}

// PROFILE sigue siendo un placeholder del wizard hasta los Módulos 5 y 7 (Mapa,
// Ajustes); los íconos y etiquetas de esta barra son temporales.
enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    REPORTAR("Reportar", R.drawable.ic_home),
    MIS_REPORTES("Mis reportes", R.drawable.ic_favorite),
    PROFILE("Profile", R.drawable.ic_account_box),
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WanchaqLimpioTheme {
        Greeting("Android")
    }
}
