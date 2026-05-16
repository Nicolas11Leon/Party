package com.example.party.model

data class Discoteca(
    val id: String = "",
    val creadorId: String = "",
    val nombre: String = "",
    val generoMusical: String = "",
    val precioReserva: Double = 0.0,
    val latitud: Double = 4.6675,
    val longitud: Double = -74.0582,
    val fotoUrl: String = "",
    val hora: String = "10:00 PM",
    val fechaMs: Long = System.currentTimeMillis(), // <-- NUEVO: Para expiración
    val direccion: String = "Zona de Rumba",
    val aforoMaximo: Int = 100,
    val confirmados: Int = 0
)