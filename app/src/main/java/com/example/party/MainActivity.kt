package com.example.party

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.party.navigation.AppNavigation
import com.example.party.ui.theme.PartyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Asegúrate de usar el nombre de tu tema generado (generalmente PartyTheme)
            PartyTheme {
                AppNavigation()
            }
        }
    }
}