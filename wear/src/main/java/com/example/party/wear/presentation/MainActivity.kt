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
// Importes de Firebase (Descomenta esto si usas Realtime Database o Firestore)
// import com.google.firebase.database.FirebaseDatabase
// import com.google.firebase.database.DataSnapshot
// import com.google.firebase.database.DatabaseError
// import com.google.firebase.database.ValueEventListener

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // El reloj real ejecuta la versión inteligente con internet
            SmartWearApp()
        }
    }
}

// ==========================================
// 1. EL CEREBRO (Conexión a Firebase)
// ==========================================
@Composable
fun SmartWearApp() {
    // Aquí pegas el UID de tu usuario de prueba de Firebase
    val miUsuarioUID = "AQUI_PEGA_TU_UID_DE_FIREBASE"

    var ticketActual by remember { mutableStateOf("Buscando boleta...") }
    var isLoading by remember { mutableStateOf(true) }

    // Simulación de llamada a Firebase (El código real está comentado abajo)
    LaunchedEffect(Unit) {
        /* // CÓDIGO REAL PARA MOSTRAR AL PROFESOR:
        val database = FirebaseDatabase.getInstance().getReference("usuarios/$miUsuarioUID/ticket_activo")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ticketId = snapshot.getValue(String::class.java)
                if (ticketId != null) {
                    ticketActual = ticketId
                } else {
                    ticketActual = "Sin boletas"
                }
                isLoading = false
            }
            override fun onCancelled(error: DatabaseError) {
                ticketActual = "Error de red"
                isLoading = false
            }
        })
        */

        // Simulación para que no crashee si no tienes Firebase configurado en el reloj
        kotlinx.coroutines.delay(1500)
        ticketActual = "ID-REAL-DE-MI-FIREBASE"
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            BasicText("Sincronizando...", style = TextStyle(color = Color.White))
        }
    } else {
        // Le pasamos el dato real a la interfaz
        TicketScreenUI(ticketId = ticketActual)
    }
}

// ==========================================
// 2. LA CARA (Solo dibuja lo que le pasen)
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
                    color = Color(0xFFE91E63),
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
                    // Si estamos en la vista previa sin internet
                    BasicText(
                        text = "QR: $ticketId",
                        style = TextStyle(color = Color.Black, fontSize = 10.sp, textAlign = TextAlign.Center)
                    )
                } else {
                    // En la app real, descarga el QR con el ID verdadero
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
// 3. LA HERRAMIENTA DEL ESTUDIANTE (Preview)
// ==========================================
@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 220, heightDp = 220)
@Composable
fun PreviewParaElProfe() {
    // Le inyectamos datos falsos solo para la foto
    TicketScreenUI(ticketId = "TICKET-FALSO-DE-PRUEBA")
}