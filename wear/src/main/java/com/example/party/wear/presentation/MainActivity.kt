package com.example.party.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Importes de Firebase REALES
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartWearApp()
        }
    }
}

// ==========================================
// 1. EL CEREBRO (Conexión Real a Firebase con Auth Anónima)
// ==========================================
@Composable
fun SmartWearApp() {
    // Asegúrate de tener el UID correcto aquí:
    val miUsuarioUID = "PEGA_AQUI_EL_UID_DE_TU_USUARIO_DE_PRUEBA"

    var ticketActual by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf("Conectando a la base de datos...") }

    // Conexión Directa a Firebase (Sin pasar por Auth)
    LaunchedEffect(Unit) {
        try {
            val database = FirebaseDatabase.getInstance().getReference("users/$miUsuarioUID/tickets")

            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var ticketEncontrado: String? = null

                    // Recorremos los tickets del usuario
                    for (ticketSnapshot in snapshot.children) {
                        val status = ticketSnapshot.child("status").getValue(String::class.java)

                        if (status == "activo") {
                            ticketEncontrado = ticketSnapshot.key
                            break
                        }
                    }

                    ticketActual = ticketEncontrado
                    if (ticketEncontrado == null) {
                        mensajeError = "No hay tickets activos"
                    }
                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    // Si Firebase rechaza la lectura, aquí te dirá exactamente por qué
                    mensajeError = "Error Firebase: ${error.message}"
                    isLoading = false
                }
            })
        } catch (e: Exception) {
            mensajeError = "Error de inicio: ${e.localizedMessage}"
            isLoading = false
        }
    }

    // Dibujo de pantallas
    if (isLoading) {
        PantallaMensaje(mensajeError)
    } else if (ticketActual != null) {
        TicketScreenUI(ticketId = ticketActual!!)
    } else {
        PantallaMensaje(mensajeError)
    }
}

// ==========================================
// 2. PANTALLA DE CARGA / ERRORES
// ==========================================
@Composable
fun PantallaMensaje(mensaje: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp), // Un poco de padding para que el texto no se corte en los bordes
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = mensaje,
            style = TextStyle(color = Color.White, textAlign = TextAlign.Center, fontSize = 12.sp)
        )
    }
}

// ==========================================
// 3. LA CARA (Dibuja el QR real)
// ==========================================
@Composable
fun TicketScreenUI(ticketId: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            BasicText(
                text = "PARTY VIP",
                style = TextStyle(
                    color = Color(0xFF9D00FF), // Morado Neón oficial del proyecto
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (LocalInspectionMode.current) {
                    // Vista previa en Android Studio
                    BasicText(
                        text = "QR: $ticketId",
                        style = TextStyle(color = Color.Black, fontSize = 10.sp, textAlign = TextAlign.Center)
                    )
                } else {
                    // App real: Se conecta a la API y genera el dibujo del QR usando el Hash de Firebase
                    AsyncImage(
                        model = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=$ticketId",
                        contentDescription = "QR de acceso",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            BasicText("Acerca al escáner", style = TextStyle(color = Color.LightGray, fontSize = 10.sp))
        }
    }
}

// ==========================================
// 4. PREVIEW PARA ANDROID STUDIO
// ==========================================
@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 220, heightDp = 220)
@Composable
fun PreviewParaElProfe() {
    TicketScreenUI(ticketId = "12345-ABCDE-MOCK")
}