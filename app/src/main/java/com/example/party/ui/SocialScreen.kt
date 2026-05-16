package com.example.party.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border // <-- ¡ESTE ERA EL IMPORT QUE FALTABA!
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.party.model.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(viewModel: PartyViewModel, navController: NavController) {
    val allUsers by viewModel.allUsers.collectAsState()
    val misSeguidos by viewModel.misSeguidos.collectAsState()
    val miUid by viewModel.currentUserId.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val misAmigos = allUsers.filter { user ->
        misSeguidos.contains(user.uid) && user.siguiendo.containsKey(miUid)
    }

    val filteredUsers = allUsers.filter { user ->
        user.username.contains(searchText, ignoreCase = true) ||
                user.email.contains(searchText, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Zona Social VIP", fontWeight = FontWeight.Black, color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C29))
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F0C29),
                    contentColor = Color(0xFF00BCD4),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFE91E63),
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Descubrir", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) Color.White else Color.Gray) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Amigos", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) Color.White else Color.Gray)
                                if (misAmigos.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(color = Color(0xFFE91E63), shape = CircleShape) {
                                        Text("${misAmigos.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp)) {

            AnimatedVisibility(
                visible = selectedTab == 0,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar rumberos por alias o correo...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00BCD4)) },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE91E63), unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color(0xFFE91E63)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (filteredUsers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron resultados.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(filteredUsers) { user ->
                                UserSocialCard(user, miUid, misSeguidos, viewModel, context)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedTab == 1,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                    if (misAmigos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Handshake, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Aún no tienes amigos VIP.", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Vuelve a 'Descubrir', envía solicitudes y espera a que te sigan de vuelta para hacer Match.", color = Color.DarkGray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(misAmigos) { amigo ->
                                UserSocialCard(amigo, miUid, misSeguidos, viewModel, context)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSocialCard(user: Usuario, miUid: String, misSeguidos: List<String>, viewModel: PartyViewModel, context: Context) {
    val loSigo = misSeguidos.contains(user.uid)
    val meSigue = user.siguiendo.containsKey(miUid)
    val somosAmigos = loSigo && meSigue

    val mostrarInfoPrivada = !user.isPrivate || somosAmigos

    val (btnText, btnColor, contentColor) = when {
        somosAmigos -> Triple("Amigos 🤝", Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF4CAF50))
        loSigo -> Triple("Siguiendo", Color.DarkGray.copy(alpha = 0.3f), Color.White)
        else -> Triple("Seguir", Color(0xFFE91E63), Color.White)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                if (mostrarInfoPrivada && user.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.photoUrl, contentDescription = "Foto de perfil",
                        modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp, if (somosAmigos) Color(0xFF4CAF50) else Color(0xFF00BCD4), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF2C2C2C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (user.isPrivate && !somosAmigos) Icons.Default.Lock else Icons.Default.Person,
                            contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (user.username.isNotEmpty()) user.username else user.email.substringBefore("@"),
                        fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (user.isPrivate && !somosAmigos) "Perfil Privado 🔒" else if (somosAmigos) "Conexión VIP" else "Rumbero de la comunidad",
                        color = if (user.isPrivate && !somosAmigos) Color.Gray else if (somosAmigos) Color(0xFF4CAF50) else Color(0xFF00BCD4),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { viewModel.toggleSeguirUsuario(user.uid) },
                    colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(if (loSigo) 0.dp else 4.dp)
                ) {
                    Text(btnText, fontWeight = FontWeight.Black, color = contentColor, fontSize = 13.sp)
                }
            }

            if (mostrarInfoPrivada && (user.instagram.isNotEmpty() || user.tiktok.isNotEmpty())) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (user.instagram.isNotEmpty()) {
                        SocialActionChip("IG: @${user.instagram}", Color(0xFFE1306C)) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/${user.instagram}"))
                            context.startActivity(intent)
                        }
                    }
                    if (user.tiktok.isNotEmpty()) {
                        SocialActionChip("TK: @${user.tiktok}", Color.White) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tiktok.com/@${user.tiktok}"))
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialActionChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}