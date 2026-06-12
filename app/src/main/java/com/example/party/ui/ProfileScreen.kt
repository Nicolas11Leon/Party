package com.example.party.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.party.navigation.Login
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: PartyViewModel, navController: NavController) {
    val email by viewModel.userEmail.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val photoUrl by viewModel.userPhotoUrl.collectAsState()
    val rol by viewModel.userRole.collectAsState()
    val isPrivate by viewModel.isPrivate.collectAsState()

    val instagram by viewModel.instagram.collectAsState()
    val tiktok by viewModel.tiktok.collectAsState()

    val ingresos by viewModel.ingresosHoy.collectAsState()
    val tickets by viewModel.ticketsVendidos.collectAsState()
    val aforo by viewModel.aforoDisponible.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showStaffDialog by remember { mutableStateOf(false) }
    var showSocialsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadProfilePicture(it) }
    }

    val backgroundBrush = Brush.verticalGradient(colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)))

    Scaffold(
        topBar = { TopAppBar(title = { Text("Panel de Control", fontWeight = FontWeight.Black, color = Color.White) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C29))) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().background(backgroundBrush).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                if (photoUrl != null) {
                    AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(120.dp).clip(CircleShape).border(4.dp, Color(0xFF00BCD4), CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.DarkGray), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.LightGray)
                    }
                }
                FilledIconButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.size(40.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE91E63))) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(if (userName.isNotEmpty()) userName.uppercase() else email.substringBefore("@").uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(email, color = Color.Gray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // --- BLOQUE DE REDES SOCIALES ---
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (instagram.isNotEmpty()) {
                    SocialChip("IG: @$instagram", Color(0xFFE1306C)) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$instagram"))
                        context.startActivity(intent)
                    }
                }
                if (tiktok.isNotEmpty()) {
                    SocialChip("TK: @$tiktok", Color.White) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tiktok.com/@$tiktok"))
                        context.startActivity(intent)
                    }
                }

                IconButton(onClick = { showSocialsDialog = true }, modifier = Modifier.size(36.dp).background(Color.DarkGray, CircleShape)) {
                    Icon(Icons.Default.AddLink, contentDescription = "Editar Redes", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (rol) {
                "Usuario" -> {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Perfil Privado", fontWeight = FontWeight.Bold, color = Color.White)
                                Text(if (isPrivate) "Oculto en búsquedas" else "Visible en la comunidad", color = Color.Gray, fontSize = 12.sp)
                            }
                            Switch(checked = isPrivate, onCheckedChange = { viewModel.togglePrivacy(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00BCD4), checkedTrackColor = Color(0xFF00BCD4).copy(alpha = 0.5f)))
                        }
                    }
                }
                "Staff" -> {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE91E63))) {
                        Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("EL ESCÁNER ESTÁ EN LA BARRA INFERIOR", fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
                "Discoteca" -> {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), elevation = CardDefaults.cardElevation(8.dp)) {
                        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, tint = Color.Green, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ingresos Acumulados", color = Color.Gray)
                            }
                            Text("$ ${String.format(Locale.US, "%,.0f", ingresos)} COP", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Tickets Vendidos", color = Color.Gray, fontSize = 12.sp)
                                    Text("$tickets", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF00BCD4))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Aforo Libre Total", color = Color.Gray, fontSize = 12.sp)
                                    Text("$aforo", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFE91E63))
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CREAR NUEVO EVENTO", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showLocationPicker = true },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00BCD4)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00BCD4).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CONFIGURAR SEDE FÍSICA")
                            }

                            OutlinedButton(
                                onClick = { showStaffDialog = true },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("REGISTRAR GUARDIA (STAFF)")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    navController.navigate(Login) { popUpTo(0) { inclusive = true } }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
            ) {
                Text("CERRAR SESIÓN", fontWeight = FontWeight.Bold)
            }
        }
    }

    // --- DIALOG: REDES SOCIALES ---
    if (showSocialsDialog) {
        Dialog(onDismissRequest = { showSocialsDialog = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1A1A1A)) {
                var igInput by remember { mutableStateOf(instagram) }
                var tkInput by remember { mutableStateOf(tiktok) }

                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Conecta tus Redes", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Deja que la comunidad te encuentre", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = igInput, onValueChange = { igInput = it },
                        label = { Text("Usuario de Instagram") }, placeholder = { Text("@tu_usuario") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tkInput, onValueChange = { tkInput = it },
                        label = { Text("Usuario de TikTok") }, placeholder = { Text("@tu_usuario") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            viewModel.guardarRedesSociales(igInput, tkInput)
                            showSocialsDialog = false
                            Toast.makeText(context, "Redes actualizadas", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                    ) { Text("GUARDAR", fontWeight = FontWeight.Black, color = Color.White) }
                }
            }
        }
    }

    // --- OTROS DIALOGS EXISTENTES (Crear Evento, Staff, Mapa) ---

    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1A1A1A), modifier = Modifier.fillMaxWidth()) {
                var newNombre by remember { mutableStateOf("") }
                var newPrecio by remember { mutableStateOf("") }
                var newAforo by remember { mutableStateOf("") }
                var newHora by remember { mutableStateOf("10:00 PM") }
                var newFoto by remember { mutableStateOf("") }

                val generos = listOf("Reggaeton", "Electrónica", "Crossover", "Salsa", "Techno")
                var selectedGenero by remember { mutableStateOf(generos[0]) }

                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LANZAR EVENTO VIP", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(value = newNombre, onValueChange = { newNombre = it }, label = { Text("Nombre del Evento") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Género Principal", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(generos) { genre ->
                            val isSel = selectedGenero == genre
                            Surface(
                                color = if (isSel) Color(0xFFE91E63) else Color.DarkGray, shape = RoundedCornerShape(16.dp), modifier = Modifier.clickable { selectedGenero = genre }
                            ) { Text(genre, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = newPrecio, onValueChange = { newPrecio = it }, label = { Text("Precio Cover (COP)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = newAforo, onValueChange = { newAforo = it }, label = { Text("Aforo Máximo (Personas)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = newHora, onValueChange = { newHora = it }, label = { Text("Hora de Inicio") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = newFoto, onValueChange = { newFoto = it }, label = { Text("URL de la Foto / Flyer") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val p = newPrecio.toDoubleOrNull() ?: 0.0
                            val a = newAforo.toIntOrNull() ?: 100
                            if (newNombre.isNotEmpty()) {
                                viewModel.crearNuevoEvento(newNombre, selectedGenero, p, newHora, a, newFoto)
                                showCreateDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
                    ) { Text("PUBLICAR EVENTO", fontWeight = FontWeight.Black, color = Color.Black) }
                }
            }
        }
    }

    if (showLocationPicker) {
        Dialog(onDismissRequest = { showLocationPicker = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
                Column(modifier = Modifier.fillMaxWidth().height(500.dp).padding(16.dp)) {
                    Text("UBICA TU DISCOTECA", fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
                    Text("Mueve el mapa hasta centrar el local", color = Color.Gray, fontSize = 12.sp)

                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp).clip(RoundedCornerShape(20.dp))) {
                        val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(4.6670, -74.0560), 15f) }
                        GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState, properties = MapProperties(mapStyleOptions = MapStyleOptions(MIDNIGHT_MAP_STYLE_PROFILE)))
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.align(Alignment.Center).size(40.dp))
                        Button(
                            onClick = {
                                val centro = cameraPositionState.position.target
                                viewModel.guardarSedeClub(centro.latitude, centro.longitude, "Sede en Zona T")
                                showLocationPicker = false
                            },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).fillMaxWidth(0.8f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                        ) { Text("GUARDAR ESTA UBICACIÓN", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    if (showStaffDialog) {
        Dialog(onDismissRequest = { showStaffDialog = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1A1A1A)) {
                var staffNombre by remember { mutableStateOf("") }
                var staffEmail by remember { mutableStateOf("") }
                var staffPassword by remember { mutableStateOf("") }

                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NUEVO GUARDIA", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Genera credenciales de acceso", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(value = staffNombre, onValueChange = { staffNombre = it }, label = { Text("Nombre del Empleado") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = staffEmail, onValueChange = { staffEmail = it }, label = { Text("Correo (Ej: puerta@miclub.com)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = staffPassword, onValueChange = { staffPassword = it }, label = { Text("Contraseña (Min 6 chars)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (staffEmail.isNotEmpty() && staffPassword.isNotEmpty() && staffNombre.isNotEmpty()) {
                                viewModel.crearEmpleadoStaff(staffEmail, staffPassword, staffNombre) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) {
                                        showStaffDialog = false
                                        navController.navigate(Login) { popUpTo(0) { inclusive = true } }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                    ) { Text("CREAR CUENTA", fontWeight = FontWeight.Black, color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun SocialChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private const val MIDNIGHT_MAP_STYLE_PROFILE = """ [ { "elementType": "geometry", "stylers": [{"color": "#121212"}] }, { "elementType": "labels.icon", "stylers": [{"visibility": "off"}] }, { "elementType": "labels.text.fill", "stylers": [{"color": "#757575"}] }, { "elementType": "labels.text.stroke", "stylers": [{"color": "#212121"}] }, { "featureType": "road", "elementType": "geometry.fill", "stylers": [{"color": "#2c2c2c"}] } ] """