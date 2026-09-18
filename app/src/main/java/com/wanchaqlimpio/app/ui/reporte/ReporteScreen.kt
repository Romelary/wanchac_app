package com.wanchaqlimpio.app.ui.reporte

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanchaqlimpio.app.R
import com.wanchaqlimpio.app.domain.model.CategoriaResiduo
import com.wanchaqlimpio.app.ui.common.etiqueta
import java.io.File

private const val ETIQUETA_LOG = "ReporteScreen"
private val PERMISOS_REQUERIDOS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

@Composable
fun ReporteScreen(viewModel: ReporteViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState is ReporteUiState.Capturando) {
        CapturaScreen(
            onFotoCapturada = viewModel::onFotoCapturada,
            onCategoriaSeleccionada = viewModel::seleccionarCategoria,
        )
    } else {
        ReporteEstadoContent(uiState = uiState, onReintentar = viewModel::reintentar)
    }
}

@Composable
private fun CapturaScreen(
    onFotoCapturada: (String) -> Unit,
    onCategoriaSeleccionada: (CategoriaResiduo) -> Unit,
) {
    val context = LocalContext.current
    var permisosOtorgados by remember { mutableStateOf(tienePermisos(context)) }
    val lanzadorPermisos =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultados ->
            permisosOtorgados = resultados.values.all { it }
        }

    if (!permisosOtorgados) {
        PermisosRequeridosContent(onSolicitar = { lanzadorPermisos.launch(PERMISOS_REQUERIDOS) })
        return
    }

    var categoriaSeleccionada by remember { mutableStateOf(CategoriaResiduo.MIXTO) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val lifecycleOwner = LocalLifecycleOwner.current

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> crearVistaPrevia(ctx, lifecycleOwner, imageCapture) },
            )

            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                SelectorCategoria(
                    seleccionada = categoriaSeleccionada,
                    onSeleccionar = {
                        categoriaSeleccionada = it
                        onCategoriaSeleccionada(it)
                    },
                )
                Button(
                    onClick = { capturarFoto(context, imageCapture, onFotoCapturada) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.reporte_boton_capturar))
                }
            }
        }
    }
}

private fun tienePermisos(context: Context): Boolean =
    PERMISOS_REQUERIDOS.all {
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
    onFotoCapturada: (String) -> Unit,
) {
    val carpeta = File(context.filesDir, "reportes").apply { mkdirs() }
    val archivo = File(carpeta, "reporte_${System.currentTimeMillis()}.jpg")
    val opciones = ImageCapture.OutputFileOptions.Builder(archivo).build()

    imageCapture.takePicture(
        opciones,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onFotoCapturada(archivo.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                // Fallo de hardware/E-S de la cámara: no es un rechazo de negocio
                // (ver ReporteUiState/ResultadoValidacionReporte), se registra para
                // no perderlo silenciosamente.
                Log.e(ETIQUETA_LOG, "Error al capturar la foto", exception)
            }
        },
    )
}

@Composable
private fun PermisosRequeridosContent(onSolicitar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.reporte_permisos_titulo))
        Text(stringResource(R.string.reporte_permisos_texto))
        Button(onClick = onSolicitar) {
            Text(stringResource(R.string.reporte_permisos_boton))
        }
    }
}

@Composable
private fun SelectorCategoria(
    seleccionada: CategoriaResiduo,
    onSeleccionar: (CategoriaResiduo) -> Unit,
) {
    Column {
        Text(stringResource(R.string.reporte_categoria_titulo))
        Row {
            CategoriaResiduo.entries.forEach { categoria ->
                FilterChip(
                    selected = categoria == seleccionada,
                    onClick = { onSeleccionar(categoria) },
                    label = { Text(categoria.etiqueta()) },
                )
            }
        }
    }
}
