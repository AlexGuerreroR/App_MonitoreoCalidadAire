package com.example.monitoreoaire

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class PerfilActivity : AppCompatActivity() {

    private lateinit var txtRol: TextView
    private lateinit var edtNombre: EditText
    private lateinit var edtEmail: EditText
    private lateinit var btnGuardar: Button

    private var idUsuario: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        txtRol = findViewById(R.id.txtPerfilRol)
        edtNombre = findViewById(R.id.edtPerfilNombre)
        edtEmail = findViewById(R.id.edtPerfilEmail)
        btnGuardar = findViewById(R.id.btnGuardarPerfil)

        // Cargar datos de sesión
        val prefs = getSharedPreferences("sesion", MODE_PRIVATE)
        idUsuario = prefs.getInt("id_usuario", 0)
        val nombre = prefs.getString("nombre", "") ?: ""
        val email = prefs.getString("email", "") ?: ""
        val rol = prefs.getString("rol", "SUPERVISOR") ?: "SUPERVISOR"

        edtNombre.setText(nombre)
        edtEmail.setText(email)
        txtRol.text = if (rol == "ADMIN") "Administrador" else "Supervisor"

        btnGuardar.setOnClickListener {
            guardarPerfil()
        }
    }

    private fun guardarPerfil() {
        val nuevoNombre = edtNombre.text.toString().trim()
        val nuevoEmail = edtEmail.text.toString().trim()

        if (nuevoNombre.isEmpty() || nuevoEmail.isEmpty()) {
            Toast.makeText(this, "Completa nombre y correo", Toast.LENGTH_SHORT).show()
            return
        }

        val url = ApiConfig.BASE_URL + "actualizar_perfil.php"

        val request = object : StringRequest(
            Request.Method.POST,
            url,
            { response ->
                // Puedes parsear JSON si quieres, por ahora solo mensaje simple
                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()

                // Actualizar sesión local
                val prefs = getSharedPreferences("sesion", MODE_PRIVATE)
                prefs.edit()
                    .putString("nombre", nuevoNombre)
                    .putString("email", nuevoEmail)
                    .apply()

                finish() // volver atrás
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ){
            override fun getHeaders(): MutableMap<String, String> {
                return Session.authHeaders(this@PerfilActivity)
            }

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["id_usuario"] = idUsuario.toString()
                params["nombre"] = nuevoNombre
                params["email"] = nuevoEmail
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}
