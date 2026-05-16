package com.example.party.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.party.navigation.Payment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(discoId: String, viewModel: PartyViewModel, navController: NavController) {
    val catalogo by viewModel.catalogoCompleto.collectAsState()
    val discoteca = catalogo.find { it.id == discoId }
    val context = LocalContext.current

    if (discoteca == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cargando evento...", color = Color.Gray) }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))))
            if (discoteca.fotoUrl.isNotEmpty()) {
                AsyncImage(model = discoteca.fotoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFF121212)), startY = 300f)))
            IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(top = 48.dp, start = 16.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 350.dp)
                .background(Color(0xFF121212), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .padding(24.dp).padding(bottom = 80.dp)
        ) {
            Box(modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color.DarkGray).align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = discoteca.nombre, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text(text = "$${discoteca.precioReserva}", color = Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoChip(icon = Icons.Default.Favorite, text = discoteca.generoMusical, color = Color(0xFFE91E63))
                    InfoChip(icon = Icons.Default.LocationOn, text = discoteca.direccion, color = Color(0xFF00BCD4))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoChip(icon = Icons.Default.AccessTime, text = discoteca.hora, color = Color(0xFFFFC107))
                    InfoChip(icon = Icons.Default.Groups, text = "${discoteca.confirmados} / ${discoteca.aforoMaximo} Asistentes", color = Color(0xFF4CAF50))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Acerca del Evento", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Únete a la mejor rumba de la ciudad. Tu pago está protegido por nuestro sistema Escrow. El dinero solo se liberará al organizador cuando asistas y valides el evento con el guardia en la puerta.",
                color = Color.LightGray, fontSize = 16.sp, lineHeight = 24.sp
            )
        }

        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = Color(0xFF1A1A1A), shadowElevation = 24.dp) {
            Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total a pagar", color = Color.Gray, fontSize = 14.sp)
                    Text("$${discoteca.precioReserva}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }

                Button(
                    onClick = {
                        if (discoteca.confirmados >= discoteca.aforoMaximo) {
                            Toast.makeText(context, "SOLD OUT: Evento lleno", Toast.LENGTH_LONG).show()
                        } else {
                            navController.navigate(Payment(discoId = discoteca.id))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (discoteca.confirmados >= discoteca.aforoMaximo) Color.DarkGray else Color(0xFFE91E63)),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (discoteca.confirmados >= discoteca.aforoMaximo) "SOLD OUT" else "RESERVAR VIP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}