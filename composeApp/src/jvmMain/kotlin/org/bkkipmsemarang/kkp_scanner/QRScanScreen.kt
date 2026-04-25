package org.bkkipmsemarang.kkp_scanner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamResolution
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import scannerkkp.composeapp.generated.resources.Res
import scannerkkp.composeapp.generated.resources.logo_bkipm
import java.awt.Dimension
import java.util.EnumMap

sealed class ProcessingState {
    object Idle : ProcessingState()
    object Loading : ProcessingState()
    data class Success(val data: ScanSuccessData) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

data class ScanSuccessData(
    val queueNumber: String,
    val activationTime: String,
    val location: String
)

@Composable
fun QRScanScreen(onBack: () -> Unit) {
    var webcamImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var processingState by remember { mutableStateOf<ProcessingState>(ProcessingState.Idle) }
    val scope = rememberCoroutineScope()

    // Handle Firestore Update when scanResult is available
    LaunchedEffect(scanResult) {
        if (scanResult != null) {
            processingState = ProcessingState.Loading
            try {
                // Update firestore status based on scanned code
                val data = updateFirestoreStatus(scanResult!!)
                processingState = ProcessingState.Success(data)
            } catch (e: Exception) {
                processingState = ProcessingState.Error(e.message ?: "Terjadi kesalahan tidak diketahui")
            }
        }
    }

    // Auto-close dialog after 60 seconds when success
    LaunchedEffect(processingState) {
        if (processingState is ProcessingState.Success) {
            delay(60000) // 60 seconds delay
            onBack()
        }
    }

    DisposableEffect(Unit) {
        var webcam: Webcam? = null
        
        val job = scope.launch(Dispatchers.IO) {
            try {
                webcam = Webcam.getDefault()
                webcam?.viewSize = WebcamResolution.VGA.size
                webcam?.open()

                val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
                hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE)
                hints[DecodeHintType.TRY_HARDER] = true
                
                val reader = MultiFormatReader()
                reader.setHints(hints)

                while (isActive && webcam != null && webcam!!.isOpen) {
                    val image = webcam!!.image
                    if (image != null) {
                        webcamImage = image.toComposeImageBitmap()

                        if (isScanning) {
                            val luminanceSource = BufferedImageLuminanceSource(image)
                            val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))
                            
                            try {
                                val result = reader.decode(binaryBitmap)
                                if (result.text.isNotEmpty()) {
                                    scanResult = result.text
                                    isScanning = false 
                                }
                            } catch (e: NotFoundException) {
                                try {
                                    val invertedSource = luminanceSource.invert()
                                    val invertedBitmap = BinaryBitmap(HybridBinarizer(invertedSource))
                                    val result = reader.decode(invertedBitmap)
                                    if (result.text.isNotEmpty()) {
                                        scanResult = result.text
                                        isScanning = false
                                    }
                                } catch (e: NotFoundException) {
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    delay(33) // ~30 FPS
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            job.cancel()
            webcam?.close()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Scan QR Code",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (webcamImage != null) {
                Image(
                    bitmap = webcamImage!!,
                    contentDescription = "Camera Feed",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(color = Color.White)
            }
            
             Box(
                modifier = Modifier
                    .size(300.dp)
                    .background(Color.Transparent, shape = RoundedCornerShape(16.dp))
            )
        }
    }

    // Dialogs based on Processing State
    when (val state = processingState) {
        is ProcessingState.Loading -> {
            Dialog(onDismissRequest = {}) {
                 Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF0056FF))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Memproses...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        is ProcessingState.Success -> {
            SuccessDialog(
                data = state.data,
                onDismiss = onBack
            )
        }
        is ProcessingState.Error -> {
            ErrorDialog(
                message = state.message,
                onRetry = {
                    scanResult = null
                    processingState = ProcessingState.Idle
                    isScanning = true
                },
                onDismiss = onBack
            )
        }
        ProcessingState.Idle -> {}
    }
}

@Composable
fun SuccessDialog(data: ScanSuccessData, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.width(400.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo
                Image(
                    painter = painterResource(Res.drawable.logo_bkipm),
                    contentDescription = "Logo",
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Success Check
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFE3F2FD), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color(0xFF0056FF),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Antrian Aktif",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
                
                Text(
                    text = "Pindai QR Berhasil Terverifikasi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Details Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nomor Antrian", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text(
                                text = data.queueNumber,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0056FF)
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Waktu Aktivasi", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(
                                text = data.activationTime,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lokasi Layanan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(
                                text = data.location,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Selesai",
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "KEMENTERIAN\nKELAUTAN DAN PERIKANAN",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
fun ErrorDialog(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.width(400.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo
                Image(
                    painter = painterResource(Res.drawable.logo_bkipm),
                    contentDescription = "Logo",
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Error Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFFEBEE), CircleShape), // Light Red
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Error",
                        tint = Color(0xFFD32F2F), // Red
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Gagal Verifikasi",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
                
                Text(
                    text = "QR Code Tidak Valid atau Tidak Terdaftar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Message Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Retry Button
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), // Red button
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Coba Scan Lagi",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                 // Cancel Button (Text Button)
                TextButton(onClick = onDismiss) {
                     Text("Tutup", color = Color.Gray)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "KEMENTERIAN\nKELAUTAN DAN PERIKANAN",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

suspend fun updateFirestoreStatus(code: String): ScanSuccessData {
    return withContext(Dispatchers.IO) {
        val db = FirestoreManager.db
        
        // 1. Cek di CustomerService
        var collectionName = "CustomerService"
        var docRef = db.collection(collectionName).document(code)
        var snapshot = docRef.get().get()
        
        // 2. Jika tidak ditemukan, Cek di SMKHP
        if (!snapshot.exists()) {
             collectionName = "SMKHP"
             docRef = db.collection(collectionName).document(code)
             snapshot = docRef.get().get()
        }
        
        if (snapshot.exists()) {
             // 3. Update status (root level) menjadi 'active'
             docRef.update("status", "active").get()
             
             // 4. Extract data for UI
             val queueNo = snapshot.get("queueNo")?.toString() ?: "-"
             val time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + " WIB"
             val location = if (collectionName == "CustomerService") "Gedung Mina Bahari IV" else if (collectionName == "SMKHP") "Gedung Mina Bahari IV" else collectionName

             return@withContext ScanSuccessData(queueNo, time, location)
        } else {
             throw Exception("Data dengan kode $code tidak ditemukan di CustomerService maupun SMKHP.")
        }
    }
}
