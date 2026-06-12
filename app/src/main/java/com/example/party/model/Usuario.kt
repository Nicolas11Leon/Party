package com.example.party.model

import com.google.firebase.database.PropertyName

data class Usuario(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val rol: String = "Usuario",
    val isPrivate: Boolean = false,
    val photoUrl: String = "",
    val reputacion: Double = 5.0,

    val instagram: String = "",
    val tiktok: String = "",

    val direccionSede: String = "",
    val latitudSede: Double = 0.0,
    val longitudSede: Double = 0.0,

    val seguidores: Map<String, Boolean> = emptyMap(),
    val siguiendo: Map<String, Boolean> = emptyMap(),

    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = false,
    var latitud: Double = 0.0,
    var longitud: Double = 0.0
)