package com.example.party.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.example.party.navigation.Details
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransportMode(val divisorVelocidadMetrosPorMinuto: Int) { WALK(80), BUS(150), CAR(250), MOTO(400) }

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(viewModel: PartyViewModel, navController: NavController) {
    val context = LocalContext.current
    val eventos by viewModel.catalogoCompleto.collectAsState()
    val clubes by viewModel.clubes.collectAsState()
    val likedDiscos by viewModel.likedDiscos.collectAsState()
    val ticketsAgrupados by viewModel.ticketsAgrupados.collectAsState()

    // --- NUEVO: ESTADOS SOCIALES ---
    val allUsers by viewModel.allUsers.collectAsState()
    val misSeguidos by viewModel.misSeguidos.collectAsState()
    val miUid by viewModel.currentUserId.collectAsState()
    val trackingActivo by viewModel.trackingActivo.collectAsState()

    val mapStyle = MapStyleOptions(MIDNIGHT_MAP_STYLE)

    var userLocation by remember { mutableStateOf(LatLng(4.6670, -74.0560)) }
    var userAzimuth by remember { mutableFloatStateOf(0f) }
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(userLocation, 14f) }

    var selectedClubLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedClubName by remember { mutableStateOf("") }
    var destinationClubUid by remember { mutableStateOf<String?>(null) }
    var destinationEventoId by remember { mutableStateOf<String?>(null) }
    var isNavigating by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(TransportMode.WALK) }

    var routeTrigger by remember { mutableIntStateOf(0) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var etaMinutes by remember { mutableIntStateOf(0) }
    var distanceKm by remember { mutableStateOf("0.0 km") }
    var arrivalTime by remember { mutableStateOf("") }

    val markerImages = remember { mutableStateMapOf<String, Bitmap>() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    var showFilters by remember { mutableStateOf(false) }
    var filterGenre by remember { mutableStateOf("Todos") }
    var filterMaxPrice by remember { mutableFloatStateOf(200000f) }
    val generosDisponibles = listOf("Todos", "Reggaeton", "Electrónica", "Crossover", "Salsa", "Techno")

    // --- EL CEREBRO DE DESCONEXIÓN INSTANTÁNEA ---
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY) {
                viewModel.desactivarTracking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.desactivarTracking()
        }
    }

    val filteredClubes by remember(clubes, eventos, filterGenre, filterMaxPrice) {
        derivedStateOf {
            if (filterGenre == "Todos" && filterMaxPrice >= 200000f) {
                clubes
            } else {
                clubes.filter { club ->
                    val clubEvents = eventos.filter { it.creadorId == club.uid }
                    clubEvents.any { ev ->
                        val matchGenre = filterGenre == "Todos" || ev.generoMusical.contains(filterGenre, ignoreCase = true)
                        val matchPrice = ev.precioReserva <= filterMaxPrice.toDouble()
                        matchGenre && matchPrice
                    }
                }
            }
        }
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { loc ->
                    if (loc.hasAccuracy() && loc.accuracy < 25f) {
                        userLocation = LatLng(loc.latitude, loc.longitude)
                        // Enviamos nuestra ubicación a Firebase SOLO si tenemos el radar encendido
                        if (trackingActivo) {
                            viewModel.actualizarMiUbicacion(loc.latitude, loc.longitude)
                        }
                    }
                }
            }
        }
    }

    var hasLocationPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { hasLocationPermission = it[Manifest.permission.ACCESS_FINE_LOCATION] == true }

    LaunchedEffect(clubes) {
        withContext(Dispatchers.IO) {
            clubes.forEach { club ->
                if (club.photoUrl.isNotEmpty() && !markerImages.containsKey(club.uid)) {
                    try {
                        val connection = URL(club.photoUrl).openConnection() as HttpURLConnection
                        connection.doInput = true
                        connection.connect()
                        val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                        withContext(Dispatchers.Main) { markerImages[club.uid] = bitmap }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = event.values
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = event.values
                if (gravity != null && geomagnetic != null) {
                    val r = FloatArray(9); val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        userAzimuth = (Math.toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        else {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000).setMinUpdateDistanceMeters(3f).build()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    LaunchedEffect(routeTrigger, selectedMode) {
        if (selectedClubLocation != null && isNavigating) {
            etaMinutes = 0
            val routeData = fetchRealRoute(userLocation, selectedClubLocation!!, selectedMode)
            if (routeData != null && routeData.points.isNotEmpty()) {
                routePoints = routeData.points
                etaMinutes = routeData.durationMins
                distanceKm = String.format(Locale.US, "%.1f km", routeData.distanceKm)
                val tiempoLlegadaMs = System.currentTimeMillis() + (etaMinutes * 60 * 1000)
                arrivalTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(tiempoLlegadaMs)).lowercase()
            } else {
                routePoints = listOf(userLocation, selectedClubLocation!!)
                val stats = calcularDistanciaYTiempo(userLocation, selectedClubLocation!!, selectedMode)
                etaMinutes = stats.minutos
                distanceKm = stats.distanciaStr
                arrivalTime = stats.horaLlegada
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false, mapStyleOptions = mapStyle),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false, compassEnabled = false, zoomControlsEnabled = false, mapToolbarEnabled = false),
            onMapClick = {
                isNavigating = false
                routePoints = emptyList()
                selectedClubLocation = null
            }
        ) {
            val userMarkerState = rememberMarkerState(position = userLocation)
            userMarkerState.position = userLocation
            val userIcon = remember { drawSleekNavArrow() }

            Marker(
                state = userMarkerState,
                icon = userIcon,
                rotation = userAzimuth,
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                flat = true,
                zIndex = 0f
            )

            // --- PINTAR DISCOTECAS ---
            val likedIds = likedDiscos.map { it.id }
            filteredClubes.forEach { club ->
                val clubPos = LatLng(club.latitudSede, club.longitudSede)
                val eventosDelClub = eventos.filter { it.creadorId == club.uid }
                val eventoActivo = eventosDelClub.firstOrNull()

                val pinStatus = when {
                    eventoActivo != null && ticketsAgrupados.containsKey(eventoActivo.id) -> 2
                    eventoActivo != null && eventoActivo.id in likedIds -> 1
                    eventoActivo != null -> 3
                    else -> 0
                }

                val markerState = rememberMarkerState(key = club.uid, position = clubPos)
                markerState.position = clubPos
                val profileImg = markerImages[club.uid]
                val cachedClubIcon = remember(pinStatus, club.username, profileImg) { drawPremiumClubMarker(pinStatus, club.username, profileImg) }

                Marker(
                    state = markerState,
                    title = if (club.username.isNotEmpty()) club.username else "Club VIP",
                    icon = cachedClubIcon,
                    anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                    zIndex = 10f,
                    onClick = {
                        routePoints = emptyList()
                        selectedClubLocation = clubPos
                        selectedClubName = if (club.username.isNotEmpty()) club.username else "Club VIP"
                        destinationClubUid = club.uid
                        destinationEventoId = eventoActivo?.id
                        isNavigating = true
                        routeTrigger++
                        coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(clubPos, 16.5f)) }
                        true
                    }
                )
            }

            // --- REQUISITO DE TIEMPO REAL: PINTAR AMIGOS VIP ---
            val amigosOnline = allUsers.filter { user ->
                misSeguidos.contains(user.uid) && user.siguiendo.containsKey(miUid) && user.isOnline
            }

            amigosOnline.forEach { amigo ->
                val amigoPos = LatLng(amigo.latitud, amigo.longitud)
                val markerState = rememberMarkerState(key = amigo.uid, position = amigoPos)
                markerState.position = amigoPos

                MarkerComposable(
                    state = markerState,
                    onClick = { true }, // No hace nada especial al clickear el amigo
                    zIndex = 20f
                ) {
                    Box(
                        modifier = Modifier
                            .size(55.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFE91E63), CircleShape) // Borde Fucsia VIP
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = amigo.photoUrl,
                            contentDescription = "Amigo VIP",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = { Text(if(amigo.username.isNotEmpty()) amigo.username.take(1).uppercase() else "?", color = Color.White, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            if (isNavigating && routePoints.isNotEmpty()) {
                Polyline(points = routePoints, color = Color(0xFF00BCD4), width = 20f, pattern = listOf(Dot(), Gap(20f)), geodesic = true, zIndex = 5f)
            }
        }

        // --- BOTÓN SUPERIOR: FILTROS DE DISCOTECA ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { showFilters = true }
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = "Filtros", tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXPLORAR: ${filteredClubes.size} CLUBES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // --- BOTÓN SUPERIOR DERECHO: RADAR DE AMIGOS (TIEMPO REAL) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "RADAR VIP",
                    color = if (trackingActivo) Color.Green else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = trackingActivo,
                    onCheckedChange = { if (it) viewModel.activarTracking() else viewModel.desactivarTracking() },
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        if (isNavigating) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth(),
                color = Color(0xFF1A1A1A).copy(alpha = 0.95f), shape = RoundedCornerShape(24.dp), shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("En ruta hacia", color = Color.Gray, fontSize = 14.sp)
                            Text(selectedClubName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(if (etaMinutes > 0) "$etaMinutes min" else "Calculando...", color = Color(0xFF4CAF50), fontSize = 28.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("($distanceKm)", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF333333)).clickable {
                            isNavigating = false; routePoints = emptyList(); selectedClubLocation = null; destinationClubUid = null; destinationEventoId = null
                        }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.background(Color.Black, CircleShape).fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TransportOption(Icons.Default.DirectionsWalk, selectedMode == TransportMode.WALK) { selectedMode = TransportMode.WALK }
                        TransportOption(Icons.Default.TwoWheeler, selectedMode == TransportMode.MOTO) { selectedMode = TransportMode.MOTO }
                        TransportOption(Icons.Default.DirectionsCar, selectedMode == TransportMode.CAR) { selectedMode = TransportMode.CAR }
                        TransportOption(Icons.Default.DirectionsBus, selectedMode == TransportMode.BUS) { selectedMode = TransportMode.BUS }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (destinationEventoId != null) {
                                navController.navigate(Details(discoId = destinationEventoId!!))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (destinationEventoId != null) Color(0xFFE91E63) else Color(0xFF333333),
                            disabledContainerColor = Color(0xFF333333)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = destinationEventoId != null
                    ) {
                        Text(
                            text = if (destinationEventoId != null) "VER EVENTO VIP" else "CLUB SIN EVENTOS HOY",
                            fontWeight = FontWeight.Bold,
                            color = if (destinationEventoId != null) Color.White else Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false }, containerColor = Color(0xFF121212), dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp).fillMaxWidth()) {
                Text("Afinar Radar", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))

                Text("GÉNERO MUSICAL", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(generosDisponibles) { genre ->
                        val isSelected = filterGenre == genre
                        Surface(
                            color = if (isSelected) Color(0xFFE91E63) else Color(0xFF1A1A1A), shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFE91E63) else Color.DarkGray),
                            modifier = Modifier.clickable { filterGenre = genre }
                        ) { Text(genre, color = if (isSelected) Color.White else Color.LightGray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text("COVER MÁXIMO: $${filterMaxPrice.toInt()}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = filterMaxPrice, onValueChange = { filterMaxPrice = it }, valueRange = 0f..200000f, steps = 19,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00BCD4), activeTrackColor = Color(0xFF00BCD4), inactiveTrackColor = Color.DarkGray)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { showFilters = false },
                    modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)
                ) { Text("APLICAR FILTRO", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 16.sp) }
            }
        }
    }
}

@Composable
fun TransportOption(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isSelected) Color(0xFF00BCD4) else Color.Transparent).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = if (isSelected) Color.Black else Color.Gray)
    }
}

private fun drawSleekNavArrow(): BitmapDescriptor {
    val size = 200
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.parseColor("#4400BCD4"); canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = android.graphics.Color.parseColor("#40000000")
    val shadowPath = Path().apply { moveTo(size / 2f, 40f); lineTo(40f, size - 50f); lineTo(size / 2f, size - 70f); lineTo(size - 40f, size - 50f); close() }
    canvas.drawPath(shadowPath, paint)
    paint.color = android.graphics.Color.WHITE
    val path = Path().apply { moveTo(size / 2f, 35f); lineTo(35f, size - 55f); lineTo(size / 2f, size - 75f); lineTo(size - 35f, size - 55f); close() }
    canvas.drawPath(path, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun drawPremiumClubMarker(status: Int, title: String, profileImage: Bitmap?): BitmapDescriptor {
    val size = 200
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val bgColor = when(status) { 2 -> android.graphics.Color.parseColor("#FFC107"); 1 -> android.graphics.Color.parseColor("#E91E63"); else -> android.graphics.Color.parseColor("#121212") }
    val strokeColor = when(status) { 2 -> android.graphics.Color.WHITE; 1 -> android.graphics.Color.WHITE; 3 -> android.graphics.Color.parseColor("#00BCD4"); else -> android.graphics.Color.DKGRAY }

    paint.color = strokeColor
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    if (profileImage != null) {
        val scaledBmp = Bitmap.createScaledBitmap(profileImage, size - 20, size - 20, false)
        val shader = BitmapShader(scaledBmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 10f, paint)
        paint.shader = null

        if (status == 0) {
            paint.color = android.graphics.Color.parseColor("#B3000000")
            canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 10f, paint)
        }
    } else {
        paint.color = bgColor
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 10f, paint)
        paint.color = if (status == 2) android.graphics.Color.BLACK else if (status == 0) android.graphics.Color.GRAY else android.graphics.Color.WHITE
        paint.textSize = 80f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val letter = if (title.isNotEmpty()) title.take(1).uppercase() else "C"
        val textY = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(letter, size / 2f, textY, paint)
    }
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

data class RouteData(val points: List<LatLng>, val durationMins: Int, val distanceKm: Double)

suspend fun fetchRealRoute(start: LatLng, end: LatLng, mode: TransportMode): RouteData? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=polyline")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 3000; connection.readTimeout = 3000
        connection.setRequestProperty("User-Agent", "PartyApp_Android_MVP")
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().readText()
            val jsonObject = JSONObject(response)
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val distanceMeters = route.getDouble("distance")
                val points = decodePolyline(route.getString("geometry"))
                return@withContext RouteData(points, (distanceMeters / mode.divisorVelocidadMetrosPorMinuto).toInt() + 1, distanceMeters / 1000.0)
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return@withContext null
}

private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0; val len = encoded.length; var lat = 0; var lng = 0
    while (index < len) {
        var b: Int; var shift = 0; var result = 0
        do { b = encoded[index++].code - 63; result = result or (b and 0x1f shl shift); shift += 5 } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1; lat += dlat
        shift = 0; result = 0
        do {
            if (index >= len) break
            b = encoded[index++].code - 63; result = result or (b and 0x1f shl shift); shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1; lng += dlng
        poly.add(LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5))
    }
    return poly
}

data class RouteStats(val minutos: Int, val distanciaStr: String, val horaLlegada: String)
private fun calcularDistanciaYTiempo(start: LatLng, end: LatLng, mode: TransportMode): RouteStats {
    val results = FloatArray(1); Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
    val metros = results[0] * 1.3
    val km = metros / 1000.0
    val minutos = (metros / mode.divisorVelocidadMetrosPorMinuto).toInt() + 1
    val tiempoLlegadaMs = System.currentTimeMillis() + (minutos * 60 * 1000)
    return RouteStats(minutos, String.format(Locale.US, "%.1f km", km), SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(tiempoLlegadaMs)).lowercase(Locale.getDefault()))
}

private const val MIDNIGHT_MAP_STYLE = """ [ { "elementType": "geometry", "stylers": [{"color": "#121212"}] }, { "elementType": "labels.icon", "stylers": [{"visibility": "off"}] }, { "elementType": "labels.text.fill", "stylers": [{"color": "#757575"}] }, { "elementType": "labels.text.stroke", "stylers": [{"color": "#212121"}] }, { "featureType": "administrative", "elementType": "geometry", "stylers": [{"color": "#757575"}] }, { "featureType": "administrative.country", "elementType": "labels.text.fill", "stylers": [{"color": "#9e9e9e"}] }, { "featureType": "administrative.land_parcel", "stylers": [{"visibility": "off"}] }, { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{"color": "#bdbdbd"}] }, { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#757575"}] }, { "featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#181818"}] }, { "featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{"color": "#616161"}] }, { "featureType": "poi.park", "elementType": "labels.text.stroke", "stylers": [{"color": "#1b1b1b"}] }, { "featureType": "road", "elementType": "geometry.fill", "stylers": [{"color": "#2c2c2c"}] }, { "featureType": "road", "elementType": "labels.text.fill", "stylers": [{"color": "#8a8a8a"}] }, { "featureType": "road.arterial", "elementType": "geometry", "stylers": [{"color": "#373737"}] }, { "featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#3c3c3c"}] }, { "featureType": "road.highway.controlled_access", "elementType": "geometry", "stylers": [{"color": "#4e4e4e"}] }, { "featureType": "road.local", "elementType": "labels.text.fill", "stylers": [{"color": "#616161"}] }, { "featureType": "transit", "elementType": "labels.text.fill", "stylers": [{"color": "#757575"}] }, { "featureType": "water", "elementType": "geometry", "stylers": [{"color": "#000000"}] }, { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#3d3d3d"}] } ] """