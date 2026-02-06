package com.example.monitoreoaire

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment

class HomeActivity : AppCompatActivity() {

    var idUsuario: Int = 0

    // CAMBIO: este id representa el dispositivo actualmente seleccionado en el Home (Spinner).
    // Lo actualiza HomeFragment llamando a setDispositivoSeleccionado(id).
    var idDispositivo: Int = 0
        private set

    private lateinit var txtBienvenida: TextView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        idUsuario = intent.getIntExtra("id_usuario", 0)
        val nombre = intent.getStringExtra("nombre") ?: ""

        txtBienvenida = findViewById(R.id.txtBienvenida)
        bottomNav = findViewById(R.id.bottomNav)

        txtBienvenida.text = "Hola, $nombre"

        // Fragment inicial
        abrirFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> abrirFragment(HomeFragment())
                R.id.nav_dispositivos -> abrirFragment(DispositivosFragment())
                R.id.nav_registros -> abrirFragment(RegistrosFragment())
                R.id.nav_config -> abrirFragment(ConfigFragment())
            }
            true
        }
    }

    // CAMBIO método para que los fragments HomeFragmen, DispositivosFragment informen
    // cuál dispositivo está seleccionado. ConfigFragment leerá este valor para cargar/guardar
    // umbrales por dispositivo.
    fun setDispositivoSeleccionado(id: Int) {
        idDispositivo = if (id > 0) id else 0
    }

    private fun abrirFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
