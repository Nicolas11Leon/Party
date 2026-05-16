package com.example.party.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.party.model.Discoteca
import com.example.party.navigation.Payment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: PartyViewModel, navController: NavController) {
    val misLikes by viewModel.likedDiscos.collectAsState()
    val ticketsAgrupados by viewModel.ticketsAgrupados.collectAsState()
    val catalogo by viewModel.catalogoCompleto.collectAsState()

    val allUsers by viewModel.allUsers.collectAsState()
    val misSeguidos by viewModel.misSeguidos.collectAsState()
    val miUid by viewModel.currentUserId.collectAsState()

    val context = LocalContext.current

    var showTransferDialog by remember { mutableStateOf(false) }
    var qrToTransfer by remember { mutableStateOf("") }
    var eventNameToTransfer by remember { mutableStateOf("") }

    val ahora = System.currentTimeMillis()

    val boletasActivas = ticketsAgrupados.filter { (id, _) ->
        val disco = catalogo.find { it.id == id }
        val fechaEvento = disco?.fechaMs ?: System.currentTimeMillis()
        fechaEvento >= (ahora - 86400000)
    }

    val likesActivos = misLikes.filter { disco ->
        val fechaEvento = disco.fechaMs
        fechaEvento >= (ahora - 86400000)
    }

    val misAmigos = allUsers.filter { user ->
        misSeguidos.contains(user.uid) && user.siguiendo.containsKey(miUid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Billetera VIP", fontWeight = FontWeight.Black, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C29))
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFF0F0C29)),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (boletasActivas.isNotEmpty()) {
                item { Text("TUS PASES ACTIVOS", fontWeight = FontWeight.Black, color = Color(0xFF00BCD4), letterSpacing = 2.sp) }

                items(boletasActivas.toList()) { (discoId, qrs) ->
                    val discoInfo = catalogo.find { it.id == discoId }
                    val nombreEvento = discoInfo?.nombre ?: "Evento VIP"
                    val primerQr = qrs.first()
                    val qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=$primerQr"

                    Column {
                        TicketProUI(nombreEvento = nombreEvento, qrImageUrl = qrImageUrl, ticketsCount = qrs.size, qrCodeText = primerQr)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { navController.navigate(Payment(discoId = discoId)) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00BCD4)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00BCD4).copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COMPRAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    qrToTransfer = primerQr
                                    eventNameToTransfer = nombreEvento
                                    showTransferDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE91E63)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("TRANSFERIR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (likesActivos.isNotEmpty()) {
                item { Text("LISTA DE DESEOS (PENDIENTES DE PAGO)", fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp) }

                items(likesActivos) { disco ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE91E63))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(disco.nombre, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(disco.generoMusical, color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("COVER: $${disco.precioReserva}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00BCD4))
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { navController.navigate(Payment(discoId = disco.id)) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("PAGAR AHORA", fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                    }
                }
            }

            if (likesActivos.isEmpty() && boletasActivas.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tu wallet está vacía o tus eventos ya pasaron.", color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("¡Ve al Feed y reserva una nueva rumba!", color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showTransferDialog) {
        Dialog(onDismissRequest = { showTransferDialog = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1A1A1A)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Transferir Boleta", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(eventNameToTransfer, color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    if (misAmigos.isEmpty()) {
                        Text("No tienes amigos en tu red VIP.", color = Color.Gray, fontSize = 14.sp)
                        Text("Ve al buscador y haz match con otros rumberos.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        Text("Elige un amigo:", color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(misAmigos) { amigo ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .background(Color.DarkGray, RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.transferirBoleta(qrToTransfer, amigo.uid) { success, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                if (success) showTransferDialog = false
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (amigo.photoUrl.isNotEmpty()) {
                                        AsyncImage(model = amigo.photoUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                    } else {
                                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(if (amigo.username.isNotEmpty()) amigo.username else amigo.email.substringBefore("@"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = { showTransferDialog = false }) { Text("CANCELAR", color = Color.Gray) }
                }
            }
        }
    }
}

@Composable
fun TicketProUI(nombreEvento: String, qrImageUrl: String, ticketsCount: Int, qrCodeText: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF5F5F5), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE91E63)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("PARTY PASS", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("x$ticketsCount VIP", color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                }
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(nombreEvento.uppercase(), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Entrada General + Fila Rápida", color = Color.DarkGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    AsyncImage(model = qrImageUrl, contentDescription = "Código QR de acceso", modifier = Modifier.size(180.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Escanea en la puerta", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ID: $qrCodeText", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) { drawLine(color = Color.Gray, start = Offset(0f, 0f), end = Offset(size.width, 0f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)) }
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Text("||||| ||| || |||||| || | |||| |||", fontSize = 32.sp, color = Color.Black, letterSpacing = 4.sp) }
            }
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = 30f; val yOffset = size.height * 0.75f
            drawCircle(color = Color(0xFF0F0C29), radius = radius, center = Offset(0f, yOffset))
            drawCircle(color = Color(0xFF0F0C29), radius = radius, center = Offset(size.width, yOffset))
        }
    }
}