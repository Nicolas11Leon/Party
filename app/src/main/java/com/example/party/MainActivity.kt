package com.example.party

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.party.navigation.AppNavigation
import com.example.party.ui.theme.PartyTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos Firebase desde el segundo cero
        FirebaseApp.initializeApp(this)

        setContent {
            PartyTheme {
                AppNavigation()
            }
        }
    }
}