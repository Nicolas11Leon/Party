package com.example.party.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScannerScreen(viewModel: PartyViewModel) {
    var manualCode by remember { mutableStateOf("") }
    val scanResult by viewModel.scanResult.collectAsState()

    // =========================================================
    // EL MOTOR REAL DEL ESCÁNER (Pide permisos y abre la cámara)
    // =========================================================
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // Cuando la cámara lee el QR, captura el texto y se lo envía al ViewModel
            viewModel.simularEscaneoQR(result.contents)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control de Acceso VIP", fontWeight = FontWeight.Black, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C29))
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFF0F0C29))) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Validación de seguridad en puerta", color = Color.Gray, fontSize = 16.sp)

                Spacer(modifier = Modifier.height(48.dp))

                // BOTÓN GIGANTE PARA ABRIR LA CÁMARA REAL
                Button(
                    onClick = {
                        val options = ScanOptions()
                        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        options.setPrompt("Apunta al QR del reloj")
                        options.setCameraId(0) // Usar cámara trasera
                        options.setBeepEnabled(true) // Que suene "Bip" al leer
                        options.setOrientationLocked(false)
                        scanLauncher.launch(options)
                    },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("ABRIR CÁMARA Y ESCANEAR", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Entrada manual de respaldo
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("O ingresa el ID manual", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00BCD4), unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (manualCode.isNotEmpty()) viewModel.simularEscaneoQR(manualCode)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("VALIDAR CÓDIGO MANUAL", fontWeight = FontWeight.Bold, color = Color.Black)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Tarjeta de Resultado del Escaneo reactiva
                if (scanResult != null) {
                    val isSuccess = scanResult!!.contains("PERMITIDO")
                    val resultColor = if (isSuccess) Color(0xFF4CAF50) else Color.Red
                    val icon = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = resultColor.copy(alpha = 0.2f)),
                            border = BorderStroke(2.dp, resultColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = resultColor, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(scanResult!!, color = resultColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = {
                            viewModel.clearScanResult()
                            manualCode = ""
                            viewModel.clearScanResult()
                            manualCode = ""
                        }) {
                            Text("ESCANEAR SIGUIENTE CLIENTE", color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}