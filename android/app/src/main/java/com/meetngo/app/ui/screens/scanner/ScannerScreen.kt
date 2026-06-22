@file:OptIn(ExperimentalMaterial3Api::class)

package com.meetngo.app.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.meetngo.app.data.api.ApiService
import com.meetngo.app.data.model.ApiError
import com.meetngo.app.ui.theme.MeetNGoColors
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.util.concurrent.Executors

/** Extrahiert eine lesbare Fehlermeldung aus der Check-in-API-Antwort, sonst wird [fallback] verwendet. */
private fun Throwable.toCheckinError(fallback: String): String {
    if (this is HttpException) {
        val body = response()?.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            val parsed = runCatching { Gson().fromJson(body, ApiError::class.java) }.getOrNull()
            if (parsed?.error?.isNotBlank() == true) return parsed.error
        }
    }
    return fallback
}

/**
 * QR-Scanner für Veranstalter: zeigt eine Kamera-Vorschau, dekodiert den
 * Ticket-QR-Code (CameraX + ZXing) und löst das Ticket über das Backend ein.
 */
@Composable
fun ScannerScreen(navController: NavHostController, apiService: ApiService) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    // Fordert die Kamera-Berechtigung direkt beim Öffnen des Screens an, falls noch nicht erteilt.
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var scanned by remember { mutableStateOf(false) }
    var resultSuccess by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    /** Verarbeitet den dekodierten QR-Inhalt: liest die Ticket-ID aus dem JSON und löst das Ticket per API-Check-in ein. */
    fun handleQr(raw: String) {
        if (scanned) return
        scanned = true
        val ticketId = runCatching { JSONObject(raw).getInt("ticketId") }.getOrNull()
        if (ticketId == null) {
            resultSuccess = false
            resultMessage = "Ungültiger QR-Code"
            return
        }
        scope.launch {
            try {
                val res = apiService.checkinTicket(ticketId)
                resultSuccess = true
                resultMessage = "Eingelöst: ${res.eventName} (Ticket #${res.ticketId})"
            } catch (e: Exception) {
                resultSuccess = false
                resultMessage = e.toCheckinError("Check-in fehlgeschlagen")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                enabled = !scanned,
                onQr = { handleQr(it) },
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Kamerazugriff wird benötigt, um Ticket-QR-Codes zu scannen.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MeetNGoColors.BrandCoral),
                ) { Text("Kamera erlauben") }
            }
        }

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück", tint = Color.White)
        }

        if (!scanned && hasPermission) {
            Text(
                "Ticket-QR-Code in den Rahmen halten",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
            )
        }

        resultMessage?.let { message ->
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (resultSuccess) "✓ Gültig" else "✗ Fehler",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (resultSuccess) MeetNGoColors.BrandTeal else MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = message,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    OutlinedButton(
                        onClick = {
                            resultMessage = null
                            scanned = false
                        },
                        modifier = Modifier.padding(top = 16.dp),
                    ) { Text("Weiteren Code scannen") }
                }
            }
        }
    }
}

/**
 * Bindet eine CameraX-Vorschau (Live-Bild) zusammen mit einer Bildanalyse-Pipeline,
 * die jeden Frame auf einen QR-Code untersucht ([QrAnalyzer]).
 */
@Composable
private fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    enabled: Boolean,
    onQr: (String) -> Unit,
) {
    // Eigener Hintergrund-Thread für die Bildanalyse, damit der Haupt-Thread (UI) nicht blockiert wird.
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { analysisExecutor.shutdown() } }

    // Der Analyzer wird in factory{} nur einmal aufgebaut und würde sonst den
    // enabled-Wert von damals festhalten. rememberUpdatedState sorgt dafür, dass
    // das Lambda immer den aktuellen Wert liest (true → wieder scanbereit).
    val currentEnabled by rememberUpdatedState(enabled)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        // factory erzeugt die PreviewView einmalig und bindet asynchron den CameraProvider,
        // sobald dieser verfügbar ist (ProcessCameraProvider.getInstance liefert ein Future).
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                // onQr wird nur weitergeleitet, solange enabled true ist (z. B. nicht mehr nach einem erfolgreichen Scan).
                analysis.setAnalyzer(analysisExecutor, QrAnalyzer { code -> if (currentEnabled) onQr(code) })
                runCatching {
                    // unbindAll verhindert, dass bei mehrfachem Aufruf mehrere Kamera-Sessions parallel laufen.
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

/** Decodes QR codes from CameraX frames using ZXing (no Play Services needed). */
private class QrAnalyzer(private val onQr: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    override fun analyze(image: ImageProxy) {
        // ZXing benötigt nur die Luminanz (Helligkeit); Plane 0 des YUV_420_888-Formats enthält genau diese (Y-Ebene).
        if (image.format != ImageFormat.YUV_420_888) {
            image.close()
            return
        }
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            // rowStride kann breiter sein als die tatsächliche Bildbreite (Padding); daher auf cropWidth/-height begrenzen.
            val rowStride = plane.rowStride
            val dataHeight = data.size / rowStride
            val cropWidth = minOf(image.width, rowStride)
            val cropHeight = minOf(image.height, dataHeight)

            val source = PlanarYUVLuminanceSource(
                data, rowStride, dataHeight, 0, 0, cropWidth, cropHeight, false,
            )
            val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
            onQr(result.text)
        } catch (_: Exception) {
            // Kein QR-Code in diesem Frame gefunden (oder vorübergehender Decode-Fehler) — Scan läuft einfach weiter.
        } finally {
            reader.reset()
            image.close()
        }
    }
}
