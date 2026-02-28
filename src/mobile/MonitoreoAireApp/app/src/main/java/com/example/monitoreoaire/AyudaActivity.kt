package com.example.monitoreoaire

import android.os.Bundle
import android.graphics.PorterDuff
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat

class AyudaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ayuda)

        val toolbar: Toolbar = findViewById(R.id.toolbarAyuda)
        setSupportActionBar(toolbar)

        // Habilitar el botón de "Atrás" nativo
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // --- CAMBIO PARA LA FLECHA BLANCA ---
        // Esto busca el icono de retroceso y le aplica un filtro de color blanco
        toolbar.navigationIcon?.setColorFilter(
            ContextCompat.getColor(this, android.R.color.white),
            PorterDuff.Mode.SRC_ATOP
        )

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}