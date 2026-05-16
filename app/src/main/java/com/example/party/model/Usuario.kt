package com.example.party.model

data class Usuario(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val rol: String = "Usuario",
    val isPrivate: Boolean = false,
    val photoUrl: String = "",
    val reputacion: Double = 5.0,

    // --- NUEVO: REDES SOCIALES ---
    val instagram: String = "",
    val tiktok: String = "",

    // --- CAMPOS DE SEDE FÍSICA PARA CLUBES ---
    val direccionSede: String = "",
    val latitudSede: Double = 0.0,
    val longitudSede: Double = 0.0,

    val seguidores: Map<String, Boolean> = emptyMap(),
    val siguiendo: Map<String, Boolean> = emptyMap()
)