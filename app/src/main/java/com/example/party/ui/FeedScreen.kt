package com.example.party.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.party.model.Discoteca
import com.example.party.model.Usuario
import com.example.party.navigation.Details
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun FeedScreen(viewModel: PartyViewModel, navController: NavController) {
    val discotecas by viewModel.uiState.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState() // Traemos a la comunidad

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (discotecas.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¡Te has quedado sin rumbas!", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Vuelve más tarde para descubrir nuevos eventos.", color = Color.LightGray)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                discotecas.reversed().forEachIndexed { index, discoteca ->
                    val isTopCard = index == discotecas.size - 1
                    SwipeableCard(
                        discoteca = discoteca,
                        isTopCard = isTopCard,
                        users = allUsers, // Le pasamos los usuarios a la tarjeta
                        onSwipeLeft = { viewModel.onDislike(discoteca) },
                        onSwipeRight = { viewModel.onLike(discoteca) },
                        onInfoClick = { navController.navigate(Details(discoId = discoteca.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeableCard(
    discoteca: Discoteca,
    isTopCard: Boolean,
    users: List<Usuario>,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onInfoClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    val swipeThreshold = 400f
    val rightOpacity = (offsetX.value / swipeThreshold).coerceIn(0f, 1f)
    val leftOpacity = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f).takeIf { offsetX.value < 0 } ?: 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.65f)
            .graphicsLayer(
                translationX = offsetX.value,
                translationY = offsetY.value,
                rotationZ = rotation.value,
                scaleX = if (isTopCard) 1f else 0.95f,
                scaleY = if (isTopCard) 1f else 0.95f
            )
            .shadow(elevation = if (isTopCard) 16.dp else 4.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.DarkGray)
            .pointerInput(isTopCard) {
                if (!isTopCard) return@pointerInput

                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value > 300f) {
                                offsetX.animateTo(1000f, tween(300))
                                onSwipeRight()
                            }
                            else if (offsetX.value < -300f) {
                                offsetX.animateTo(-1000f, tween(300))
                                onSwipeLeft()
                            }
                            else {
                                launch { offsetX.animateTo(0f, tween(300)) }
                                launch { offsetY.animateTo(0f, tween(300)) }
                                launch { rotation.animateTo(0f, tween(300)) }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                            rotation.snapTo(offsetX.value * 0.05f)
                        }
                    }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(colors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))
        ))

        if (discoteca.fotoUrl.isNotEmpty()) {
            AsyncImage(
                model = discoteca.fotoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                    startY = 200f
                ))
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Surface(color = Color(0xFFE91E63), shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = discoteca.generoMusical.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = discoteca.nombre, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Cover: $${discoteca.precioReserva}", color = Color(0xFF00BCD4), fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(12.dp))

            // --- SOCIAL PROOF: Los avatares de la comunidad ---
            if (users.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        users.take(3).forEachIndexed { index, user ->
                            Box(
                                modifier = Modifier
                                    .offset(x = (-10 * index).dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.Black, CircleShape)
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (user.photoUrl.isNotEmpty()) {
                                    AsyncImage(model = user.photoUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    Text(text = "y ${users.size * 12} rumberos más van a ir", color = Color.LightGray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onInfoClick,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Ver Más", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("INFO VIP", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(12.dp))

                FilledIconButton(
                    onClick = {
                        val numeroDummy = "573000000000"
                        val mensaje = "¡Hola! Vi el evento de ${discoteca.nombre} en la app Party. ¿Tienen mesas disponibles?"
                        val url = "https://api.whatsapp.com/send?phone=$numeroDummy&text=${Uri.encode(mensaje)}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(50.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Message, contentDescription = "WhatsApp", tint = Color.White)
                }
            }
        }

        if (isTopCard) {
            if (rightOpacity > 0f) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Green.copy(alpha = rightOpacity * 0.4f)))
                Text("¡VAMOS!", color = Color.Green, fontSize = 48.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.TopStart).padding(32.dp).border(4.dp, Color.Green, RoundedCornerShape(12.dp)).padding(16.dp).graphicsLayer(alpha = rightOpacity, rotationZ = -15f)
                )
            }
            if (leftOpacity > 0f) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = leftOpacity * 0.4f)))
                Text("PASO", color = Color.Red, fontSize = 48.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.TopEnd).padding(32.dp).border(4.dp, Color.Red, RoundedCornerShape(12.dp)).padding(16.dp).graphicsLayer(alpha = leftOpacity, rotationZ = 15f)
                )
            }
        }
    }
}