package com.example.party.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.party.model.Discoteca

@Composable
fun FeedScreen(viewModel: PartyViewModel, navController: NavController) {
    val lista by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Party Feed", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(8.dp))

        lista.firstOrNull()?.let { disco ->
            Card(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp)) {
                Column {
                    AsyncImage(
                        model = disco.imagenUrl,
                        contentDescription = null,
                        modifier = Modifier.height(250.dp).fillMaxWidth(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Text(disco.nombre, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.titleLarge)
                    Text(disco.generoMusical, modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(onClick = { viewModel.onLike(disco) }, modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Me Gusta")
                    }
                }
            }
        } ?: Text("¡No hay más eventos por hoy!", modifier = Modifier.padding(vertical = 32.dp))

        Button(onClick = { navController.navigate("wallet") }, modifier = Modifier.fillMaxWidth()) {
            Text("Ir a mi Billetera")
        }
    }
}
