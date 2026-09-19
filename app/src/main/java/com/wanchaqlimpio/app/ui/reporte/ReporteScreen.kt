package com.wanchaqlimpio.app.ui.reporte

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanchaqlimpio.app.R
import java.io.File

private const val ETIQUETA_LOG = "ReporteScreen"

/**
 * Lo que se pide al ciudadano. Se pide TAMBIÉN la ubicación aproximada: Android 12+ ofrece «precisa» o «aproximada»
 * en el mismo diálogo y una app debe declarar y pedir las dos. Para reportar se exige la precisa (ver [tienePermisos]).
 */
private val PERMISOS_A_PEDIR =
    arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
private val PERMISOS_INDISPENSABLES = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

/** El botón de capturar no debe estirarse de borde a borde en una tablet. */
private val ANCHO_MAXIMO_BOTON_CAPTURA = 400.dp

@Composable
fun ReporteScreen(viewModel: ReporteViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val precisionGps by viewModel.precisionGps.collectAsStateWithLifecycle()

    when (val estado = uiState) {
        ReporteUiState.Capturando ->
            CapturaScreen(
                precisionGps = precisionGps,
                crearArchivoFoto = viewModel::crearArchivoFoto,
                onFotoCapturada = viewModel::onFotoCapturada,
                onErrorDeCaptura = viewModel::onErrorDeCaptura,
                vigilarGps = viewModel::vigilarGps,
            )
        is ReporteUiState.Confirmando ->
            ReporteConfirmacionContent(
                estado = estado,
                onSeleccionar = viewModel::seleccionarCategoria,
                onGuardar = viewModel::confirmar,
                onDescartar = viewModel::descartarFoto,
            )
        else ->
            ReporteEstadoContent(
                uiState = uiState,
                onReintentar = viewModel::reintentar,
                onReintentarUbicacion = viewModel::reintentarUbicacion,
                onTomarOtraFoto = viewModel::descartarFoto,
            )
    }
}

@Composable
private fun CapturaScreen(
    precisionGps: Float?,
    crearArchivoFoto: () -> File,
    onFotoCapturada: (String) -> Unit,
    onErrorDeCaptura: () -> Unit,
    vigilarGps: suspend () -> Unit,
) {
    val context = LocalContext.current
    var permisosOtorgados by remember { mutableStateOf(tienePermisos(context)) }
    val lanzadorPermisos =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permisosOtorgados = tienePermisos(context)
        }
    // Al volver de Ajustes (donde el ciudadano pudo activar los permisos) se comprueban de nuevo.
    AlVolverALaPantalla { permisosOtorgados = tienePermisos(context) }

    if (!permisosOtorgados) {
        PermisosRequeridosContent(
            onSolicitar = { lanzadorPermisos.launch(PERMISOS_A_PEDIR) },
            onAbrirAjustes = { abrirAjustesDeLaApp(context) },
        )
        return
    }

    // Con la cámara abierta el GPS se mantiene encendido: así, al tomar la foto, ya hay una lectura buena.
    LaunchedEffect(Unit) { vigilarGps() }

    val imageCapture = remember { ImageCapture.Builder().build() }
    MantenerRotacionDeCaptura(imageCapture)

    PantallaDeCamara(
        precisionGps = precisionGps,
        imageCapture = imageCapture,
        onCapturar = { capturarFoto(context, imageCapture, crearArchivoFoto(), onFotoCapturada, onErrorDeCaptura) },
    )
}

/** La vista previa, el estado del GPS arriba y el botón de capturar abajo. */
@Composable
private fun PantallaDeCamara(
    precisionGps: Float?,
    imageCapture: ImageCapture,
    onCapturar: () -> Unit,
) {
    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            VistaPreviaDeCamara(imageCapture)
            IndicadorGps(
                precisionMetros = precisionGps,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(12.dp),
            )
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = ANCHO_MAXIMO_BOTON_CAPTURA)
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Button(onClick = onCapturar, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.reporte_boton_capturar))
                }
            }
        }
    }
}

@Composable
private fun VistaPreviaDeCamara(imageCapture: ImageCapture) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> crearVistaPrevia(ctx, lifecycleOwner, imageCapture) },
    )
}

/** Ejecuta [accion] cada vez que la pantalla vuelve a primer plano (p. ej., tras cerrar los Ajustes del sistema). */
@Composable
private fun AlVolverALaPantalla(accion: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val accionActual by rememberUpdatedState(accion)

    DisposableEffect(lifecycleOwner) {
        val observador =
            LifecycleEventObserver { _, evento -> if (evento == Lifecycle.Event.ON_RESUME) accionActual() }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }
}

/**
 * CameraX fija la rotación de la foto cuando se crea el `ImageCapture`. Si la pantalla gira 180° (horizontal ↔
 * horizontal invertido, algo normal en una tablet) Android NO recrea la actividad, y sin este aviso la foto se
 * guardaría con la orientación EXIF equivocada, es decir, boca abajo. Es el patrón que recomienda la documentación
 * de CameraX para tablets y plegables.
 */
@Composable
private fun MantenerRotacionDeCaptura(imageCapture: ImageCapture) {
    val context = LocalContext.current
    val vista = LocalView.current

    DisposableEffect(imageCapture, vista) {
        val administrador = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val escucha =
            object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = Unit

                override fun onDisplayRemoved(displayId: Int) = Unit

                override fun onDisplayChanged(displayId: Int) {
                    val pantalla = vista.display
                    if (pantalla != null &&
                        pantalla.displayId == displayId
                    ) {
                        imageCapture.targetRotation = pantalla.rotation
                    }
                }
            }
        administrador.registerDisplayListener(escucha, null)
        onDispose { administrador.unregisterDisplayListener(escucha) }
    }
}

/** La cámara y la ubicación PRECISA (la aproximada no sirve para el filtro de 15 m). */
private fun tienePermisos(context: Context): Boolean =
    PERMISOS_INDISPENSABLES.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private fun crearVistaPrevia(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    imageCapture: ImageCapture,
): PreviewView {
    val previewView = PreviewView(context)
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener(
        {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        },
        ContextCompat.getMainExecutor(context),
    )
    return previewView
}

private fun capturarFoto(
    context: Context,
    imageCapture: ImageCapture,
    archivo: File,
    onFotoCapturada: (String) -> Unit,
    onErrorDeCaptura: () -> Unit,
) {
    val opciones = ImageCapture.OutputFileOptions.Builder(archivo).build()

    imageCapture.takePicture(
        opciones,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onFotoCapturada(archivo.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                // Fallo de hardware o de espacio al guardar: no es un rechazo de negocio (ver ReporteUiState /
                // ResultadoValidacionReporte). Se registra y se le dice al ciudadano, para que no se quede
                // tocando un botón que parece no hacer nada.
                Log.e(ETIQUETA_LOG, "Error al capturar la foto", exception)
                onErrorDeCaptura()
            }
        },
    )
}
