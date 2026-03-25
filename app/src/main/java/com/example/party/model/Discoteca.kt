package com.example.party.model

data class Discoteca(
    val id: String,
    val nombre: String,
    val generoMusical: String,
    val precioReserva: Double,
    val imagenUrl: String
)

// Datos simulados (Mocks) para la entrega
val mockDiscotecas = listOf(
    Discoteca("1", "Neon Club", "Electrónica", 50000.0, "https://images.unsplash.com/photo-1514525253361-bee8a18744ad?q=80&w=1000&auto=format&fit=crop"),
    Discoteca("2", "La Terraza", "Reggaeton", 35000.0, "https://images.unsplash.com/photo-1545128485-c400e7702796?q=80&w=1000&auto=format&fit=crop"),
    Discoteca("3", "Underground VIP", "Crossover", 60000.0, "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=1000&auto=format&fit=crop")
)
