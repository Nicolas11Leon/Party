package com.example.party.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.party.model.Discoteca
import com.example.party.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PartyViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val databaseRef = FirebaseDatabase.getInstance().getReference("discotecas")
    private val usersRef = FirebaseDatabase.getInstance().getReference("users")
    private val storageRef = FirebaseStorage.getInstance().getReference("user_photos")

    private var allDiscotecas = listOf<Discoteca>()
    private var myDislikesIds = listOf<String>()

    private val _catalogoCompleto = MutableStateFlow<List<Discoteca>>(emptyList())
    val catalogoCompleto: StateFlow<List<Discoteca>> = _catalogoCompleto

    private val _uiState = MutableStateFlow<List<Discoteca>>(emptyList())
    val uiState: StateFlow<List<Discoteca>> = _uiState

    private val _likedDiscos = MutableStateFlow<List<Discoteca>>(emptyList())
    val likedDiscos: StateFlow<List<Discoteca>> = _likedDiscos

    private val _ticketsAgrupados = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val ticketsAgrupados: StateFlow<Map<String, List<String>>> = _ticketsAgrupados

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _ingresosHoy = MutableStateFlow(0.0)
    val ingresosHoy: StateFlow<Double> = _ingresosHoy

    private val _ticketsVendidos = MutableStateFlow(0)
    val ticketsVendidos: StateFlow<Int> = _ticketsVendidos

    private val _aforoDisponible = MutableStateFlow(0)
    val aforoDisponible: StateFlow<Int> = _aforoDisponible

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail
    private val _userPhotoUrl = MutableStateFlow<Uri?>(null)
    val userPhotoUrl: StateFlow<Uri?> = _userPhotoUrl
    private val _userRole = MutableStateFlow("Usuario")
    val userRole: StateFlow<String> = _userRole
    private val _isPrivate = MutableStateFlow(false)
    val isPrivate: StateFlow<Boolean> = _isPrivate

    private val _instagram = MutableStateFlow("")
    val instagram: StateFlow<String> = _instagram

    private val _tiktok = MutableStateFlow("")
    val tiktok: StateFlow<String> = _tiktok

    private val _allUsers = MutableStateFlow<List<Usuario>>(emptyList())
    val allUsers: StateFlow<List<Usuario>> = _allUsers

    private val _clubes = MutableStateFlow<List<Usuario>>(emptyList())
    val clubes: StateFlow<List<Usuario>> = _clubes

    private val _misSeguidos = MutableStateFlow<List<String>>(emptyList())
    val misSeguidos: StateFlow<List<String>> = _misSeguidos

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId

    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult
    private val _profileLoading = MutableStateFlow(false)
    val profileLoading: StateFlow<Boolean> = _profileLoading

    private val _profileLoaded = MutableStateFlow(false)
    val profileLoaded: StateFlow<Boolean> = _profileLoaded

    private val _sedeLat = MutableStateFlow(4.6670)
    private val _sedeLng = MutableStateFlow(-74.0560)
    private val _sedeDireccion = MutableStateFlow("")

    private var currentUid: String? = null
    private var likesListener: ValueEventListener? = null
    private var dislikesListener: ValueEventListener? = null
    private var ticketsListener: ValueEventListener? = null
    private var profileListener: ValueEventListener? = null

    init {
        loadAllUsers()
        loadDiscotecas()

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _userEmail.value = user.email ?: ""
                _userPhotoUrl.value = user.photoUrl
                _currentUserId.value = user.uid
                if (currentUid != user.uid) {
                    limpiarListenersUsuario()
                    currentUid = user.uid
                    _profileLoaded.value = false
                    cargarPerfilCompleto(user.uid)
                    cargarInteracciones(user.uid)
                }
            } else {
                limpiarListenersUsuario()
                currentUid = null
                _currentUserId.value = ""
                limpiarDatosSesion()
            }
        }
    }

    private fun limpiarListenersUsuario() {
        currentUid?.let { uid ->
            val userNode = usersRef.child(uid)
            profileListener?.let { userNode.removeEventListener(it) }
            likesListener?.let { userNode.child("likes").removeEventListener(it) }
            dislikesListener?.let { userNode.child("dislikes").removeEventListener(it) }
            ticketsListener?.let { userNode.child("tickets").removeEventListener(it) }
        }
        profileListener = null; likesListener = null; dislikesListener = null; ticketsListener = null
    }

    private fun limpiarDatosSesion() {
        _userEmail.value = ""; _userName.value = ""; _userPhotoUrl.value = null; _userRole.value = "Usuario"
        _instagram.value = ""; _tiktok.value = ""
        _likedDiscos.value = emptyList(); _ticketsAgrupados.value = emptyMap()
        _uiState.value = emptyList(); myDislikesIds = emptyList()
        _ingresosHoy.value = 0.0; _ticketsVendidos.value = 0; _aforoDisponible.value = 0
        _profileLoaded.value = false
    }

    private fun calcularStatsDiscoteca() {
        val uid = currentUid ?: return
        val misEventos = allDiscotecas.filter { it.creadorId == uid }
        _ticketsVendidos.value = misEventos.sumOf { it.confirmados }
        _ingresosHoy.value = misEventos.sumOf { it.confirmados * it.precioReserva }
        _aforoDisponible.value = misEventos.sumOf { (it.aforoMaximo - it.confirmados).coerceAtLeast(0) }
    }

    private fun cargarInteracciones(uid: String) {
        val userNode = usersRef.child(uid)
        likesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likes = mutableListOf<Discoteca>()
                for (snap in snapshot.children) { snap.getValue(Discoteca::class.java)?.let { likes.add(it) } }
                _likedDiscos.value = likes
                filtrarFeed()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        userNode.child("likes").addValueEventListener(likesListener!!)

        dislikesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dislikes = mutableListOf<String>()
                for (snap in snapshot.children) { snap.key?.let { dislikes.add(it) } }
                myDislikesIds = dislikes
                filtrarFeed()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        userNode.child("dislikes").addValueEventListener(dislikesListener!!)

        ticketsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mapaAgrupado = mutableMapOf<String, MutableList<String>>()
                for (ticket in snapshot.children) {
                    val status = ticket.child("status").getValue(String::class.java)
                    if (status != "usado") {
                        val discoId = ticket.child("discoId").getValue(String::class.java) ?: "Desconocido"
                        mapaAgrupado.getOrPut(discoId) { mutableListOf() }.add(ticket.key.toString())
                    }
                }
                _ticketsAgrupados.value = mapaAgrupado
                filtrarFeed()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        userNode.child("tickets").addValueEventListener(ticketsListener!!)
    }

    private fun loadDiscotecas() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = mutableListOf<Discoteca>()
                for (disco in snapshot.children) { disco.getValue(Discoteca::class.java)?.let { lista.add(it) } }
                allDiscotecas = lista
                _catalogoCompleto.value = allDiscotecas
                filtrarFeed()
                calcularStatsDiscoteca()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filtrarFeed() {
        val likedIds = _likedDiscos.value.map { it.id }
        val purchasedIds = _ticketsAgrupados.value.keys
        _uiState.value = allDiscotecas.filter { it.id !in likedIds && it.id !in myDislikesIds && it.id !in purchasedIds }
    }

    fun onLike(discoteca: Discoteca) { currentUid?.let { usersRef.child(it).child("likes").child(discoteca.id).setValue(discoteca) } }
    fun onDislike(discoteca: Discoteca) { currentUid?.let { usersRef.child(it).child("dislikes").child(discoteca.id).setValue(true) } }

    fun comprarBoleta(disco: Discoteca) {
        val uid = currentUid ?: return
        val codigoQR = "$uid-${disco.id}-${System.currentTimeMillis()}"
        val ticketData = mapOf("discoId" to disco.id, "nombre" to disco.nombre, "status" to "activo")
        usersRef.child(uid).child("tickets").child(codigoQR).setValue(ticketData).addOnSuccessListener {
            usersRef.child(uid).child("likes").child(disco.id).removeValue()
            databaseRef.child(disco.id).child("confirmados").setValue(disco.confirmados + 1)
        }
    }

    fun transferirBoleta(qrCode: String, amigoUid: String, onComplete: (Boolean, String) -> Unit) {
        val miUid = currentUid ?: return
        val miTicketRef = usersRef.child(miUid).child("tickets").child(qrCode)
        val amigoTicketRef = usersRef.child(amigoUid).child("tickets").child(qrCode)

        miTicketRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                amigoTicketRef.setValue(snapshot.value).addOnSuccessListener {
                    miTicketRef.removeValue().addOnSuccessListener { onComplete(true, "Boleta transferida con éxito") }
                }
            } else { onComplete(false, "Error: La boleta no existe") }
        }.addOnFailureListener { onComplete(false, "Error de red al transferir") }
    }

    fun guardarSedeClub(lat: Double, lng: Double, direccion: String) {
        val uid = currentUid ?: return
        usersRef.child(uid).updateChildren(mapOf("latitudSede" to lat, "longitudSede" to lng, "direccionSede" to direccion))
    }

    fun guardarRedesSociales(ig: String, tk: String) {
        val uid = currentUid ?: return
        usersRef.child(uid).updateChildren(mapOf(
            "instagram" to ig.trim().removePrefix("@"),
            "tiktok" to tk.trim().removePrefix("@")
        ))
    }

    fun crearNuevoEvento(nombre: String, genero: String, precio: Double, hora: String, aforo: Int, fotoUrl: String) {
        val uid = currentUid ?: return
        val eventoId = databaseRef.push().key ?: return

        val nuevoEvento = Discoteca(
            id = eventoId,
            creadorId = uid,
            nombre = nombre,
            generoMusical = genero,
            precioReserva = precio,
            latitud = _sedeLat.value,
            longitud = _sedeLng.value,
            fotoUrl = fotoUrl,
            hora = hora,
            fechaMs = System.currentTimeMillis(),
            direccion = _sedeDireccion.value,
            aforoMaximo = aforo,
            confirmados = 0
        )
        databaseRef.child(eventoId).setValue(nuevoEvento)
    }

    fun crearEmpleadoStaff(email: String, password: String, nombre: String, onComplete: (Boolean, String) -> Unit) {
        val clubId = currentUid ?: return
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val newUid = task.result?.user?.uid
                if (newUid != null) {
                    val userData = mapOf("username" to nombre, "email" to email, "rol" to "Staff", "trabajaEn" to clubId, "private" to true)
                    usersRef.child(newUid).setValue(userData).addOnCompleteListener {
                        logout()
                        onComplete(true, "Empleado creado. Inicia sesión nuevamente.")
                    }
                }
            } else { onComplete(false, task.exception?.message ?: "Error al crear cuenta") }
        }
    }

    fun toggleSeguirUsuario(amigoUid: String) {
        val miUid = currentUid ?: return
        val isFollowing = _misSeguidos.value.contains(amigoUid)

        if (isFollowing) {
            usersRef.child(miUid).child("siguiendo").child(amigoUid).removeValue()
            usersRef.child(amigoUid).child("seguidores").child(miUid).removeValue()
        } else {
            usersRef.child(miUid).child("siguiendo").child(amigoUid).setValue(true)
            usersRef.child(amigoUid).child("seguidores").child(miUid).setValue(true)
        }
    }

    private fun cargarPerfilCompleto(uid: String) {
        profileListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    _userRole.value = snapshot.child("rol").getValue(String::class.java) ?: "Usuario"
                    _userName.value = snapshot.child("username").getValue(String::class.java) ?: ""
                    _isPrivate.value = snapshot.child("private").getValue(Boolean::class.java) ?: false

                    _instagram.value = snapshot.child("instagram").getValue(String::class.java) ?: ""
                    _tiktok.value = snapshot.child("tiktok").getValue(String::class.java) ?: ""

                    _sedeLat.value = snapshot.child("latitudSede").getValue(Double::class.java) ?: 4.6670
                    _sedeLng.value = snapshot.child("longitudSede").getValue(Double::class.java) ?: -74.0560
                    _sedeDireccion.value = snapshot.child("direccionSede").getValue(String::class.java) ?: "Sin dirección configurada"

                    val seguidos = mutableListOf<String>()
                    snapshot.child("siguiendo").children.forEach { it.key?.let { k -> seguidos.add(k) } }
                    _misSeguidos.value = seguidos

                    calcularStatsDiscoteca()
                    _profileLoaded.value = true
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        usersRef.child(uid).addValueEventListener(profileListener!!)
    }

    fun togglePrivacy(isPrivate: Boolean) { currentUid?.let { usersRef.child(it).child("private").setValue(isPrivate) } }

    private fun loadAllUsers() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listaUsuarios = mutableListOf<Usuario>()
                val listaClubes = mutableListOf<Usuario>()
                for (userSnap in snapshot.children) {
                    val uid = userSnap.key ?: continue
                    val rol = userSnap.child("rol").getValue(String::class.java) ?: "Usuario"
                    val isPriv = userSnap.child("private").getValue(Boolean::class.java) ?: false
                    val email = userSnap.child("email").getValue(String::class.java) ?: ""
                    val username = userSnap.child("username").getValue(String::class.java) ?: email.substringBefore("@")
                    val photo = userSnap.child("photoUrl").getValue(String::class.java) ?: ""

                    val ig = userSnap.child("instagram").getValue(String::class.java) ?: ""
                    val tk = userSnap.child("tiktok").getValue(String::class.java) ?: ""

                    val mapSeguidores = mutableMapOf<String, Boolean>()
                    userSnap.child("seguidores").children.forEach { it.key?.let { k -> mapSeguidores[k] = true } }

                    val mapSiguiendo = mutableMapOf<String, Boolean>()
                    userSnap.child("siguiendo").children.forEach { it.key?.let { k -> mapSiguiendo[k] = true } }

                    val userObj = Usuario(
                        uid = uid, username = username, email = email, rol = rol,
                        isPrivate = isPriv, photoUrl = photo, instagram = ig, tiktok = tk,
                        seguidores = mapSeguidores, siguiendo = mapSiguiendo
                    )

                    if (rol == "Discoteca") {
                        val lat = userSnap.child("latitudSede").getValue(Double::class.java) ?: 4.6670
                        val lng = userSnap.child("longitudSede").getValue(Double::class.java) ?: -74.0560
                        val dir = userSnap.child("direccionSede").getValue(String::class.java) ?: ""
                        if (lat != 0.0) listaClubes.add(userObj.copy(latitudSede = lat, longitudSede = lng, direccionSede = dir))
                    } else if (rol == "Usuario" && uid != currentUid) {
                        listaUsuarios.add(userObj)
                    }
                }
                _allUsers.value = listaUsuarios
                _clubes.value = listaClubes
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun uploadProfilePicture(imageUri: Uri) {
        val user = auth.currentUser ?: return
        _profileLoading.value = true
        val fileRef = storageRef.child("${user.uid}/profile.jpg")
        fileRef.putFile(imageUri).addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val profileUpdates = UserProfileChangeRequest.Builder().setPhotoUri(downloadUri).build()
                user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                    _profileLoading.value = false
                    if (task.isSuccessful) {
                        _userPhotoUrl.value = downloadUri
                        usersRef.child(user.uid).child("photoUrl").setValue(downloadUri.toString())
                    }
                }
            }
        }.addOnFailureListener { _profileLoading.value = false }
    }

    fun simularEscaneoQR(qrCode: String) {
        val parts = qrCode.split("-")
        if (parts.size >= 2) {
            val userIdDuenio = parts[0]
            val ticketRef = usersRef.child(userIdDuenio).child("tickets").child(qrCode)
            ticketRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    if (snapshot.child("status").getValue(String::class.java) == "usado") {
                        _scanResult.value = "ERROR: Este ticket ya fue usado."
                    } else {
                        ticketRef.child("status").setValue("usado")
                        _scanResult.value = "ACCESO PERMITIDO."
                    }
                } else _scanResult.value = "ERROR: Ticket falso."
            }
        } else _scanResult.value = "ERROR: Formato inválido."
    }

    fun clearScanResult() { _scanResult.value = null }
    fun logout() { auth.signOut() }
}