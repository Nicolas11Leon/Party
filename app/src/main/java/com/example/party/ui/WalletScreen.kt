package com.example.party.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.party.model.Discoteca

@Composable
fun WalletScreen(viewModel: PartyViewModel, navController: NavController) {
    val misLikes by viewModel.likedDiscos.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mis Reservas", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(8.dp))

        if (misLikes.isEmpty()) {
            Text("No tienes reservas aún.", modifier = Modifier.padding(vertical = 16.dp))
        } else {
            misLikes.forEach { disco ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(disco.nombre, style = MaterialTheme.typography.titleMedium)
                            Text("QR Pendiente", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { /* Próxima entrega: Hardware QR */ }) {
                            Text("Ver QR")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al Feed")
        }
    }
}
