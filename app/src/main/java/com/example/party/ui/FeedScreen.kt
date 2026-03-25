package com.example.party.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.party.model.Discoteca

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: PartyViewModel, navController: NavController) {
    val lista by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "PARTY", 
                        fontWeight = FontWeight.Black, 
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Button(
                    onClick = { navController.navigate("wallet") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Mis Reservas / Billetera")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            lista.firstOrNull()?.let { disco ->
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { navController.navigate("details/${disco.id}") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = disco.imagenUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            color = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(disco.nombre, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text(disco.generoMusical, style = MaterialTheme.typography.bodyLarge)
                                Text("Toca para ver detalles", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LargeFloatingActionButton(
                        onClick = { viewModel.onDislike(disco) },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "No me interesa", modifier = Modifier.size(36.dp))
                    }

                    LargeFloatingActionButton(
                        onClick = { viewModel.onLike(disco) },
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Me gusta", modifier = Modifier.size(36.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay más eventos por hoy. ¡Vuelve más tarde!", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
