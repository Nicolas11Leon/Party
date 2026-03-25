package com.example.party.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.party.model.Discoteca

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: PartyViewModel, navController: NavController) {
    val misLikes by viewModel.likedDiscos.collectAsState()
    var showQuoteDialog by remember { mutableStateOf<Discoteca?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Billetera Digital", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Tus Próximos Eventos", 
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (misLikes.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No tienes reservas activas.", color = Color.Gray)
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Explorar Eventos")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(misLikes) { disco ->
                        TicketCard(
                            disco = disco,
                            onVerQR = { /* Próxima entrega: Hardware QR */ },
                            onCotizar = { showQuoteDialog = disco }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    // Diálogo de Cotización
    showQuoteDialog?.let { disco ->
        QuoteDialog(
            disco = disco,
            onDismiss = { showQuoteDialog = null }
        )
    }
}

@Composable
fun TicketCard(disco: Discoteca, onVerQR: () -> Unit, onCotizar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(disco.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(disco.generoMusical, style = MaterialTheme.typography.bodyMedium)
                }
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "PENDIENTE", 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFF4CAF50),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCotizar,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cotizar")
                }
                Button(
                    onClick = onVerQR,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Ver QR")
                }
            }
        }
    }
}

@Composable
fun QuoteDialog(disco: Discoteca, onDismiss: () -> Unit) {
    var personCount by remember { mutableIntStateOf(1) }
    val total = disco.precioReserva * personCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cotización: ${disco.nombre}") },
        text = {
            Column {
                Text("Precio base por persona: $${disco.precioReserva}")
                Spacer(modifier = Modifier.height(16.dp))
                Text("¿Cuántas personas?", fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    IconButton(onClick = { if (personCount > 1) personCount-- }) {
                        Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        personCount.toString(), 
                        modifier = Modifier.padding(horizontal = 24.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { personCount++ }) {
                        Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL A PAGAR:", fontWeight = FontWeight.Bold)
                    Text("$${total}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }
                Text("* Los precios incluyen impuestos y reserva de mesa.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}
