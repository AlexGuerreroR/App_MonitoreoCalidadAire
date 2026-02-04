package com.example.monitoreoaire

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtRegistro: TextView
    private lateinit var txtOlvide: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔹 Revisar si ya hay sesión guardada
        val prefs = getSharedPreferences("sesion", MODE_PRIVATE)
        val logged = prefs.getBoolean("isLoggedIn", false)

        if (logged) {
            // Recupero datos de la sesión
            val idUsuario = prefs.getInt("id_usuario", 0)
            val nombre = prefs.getString("nombre", "") ?: ""
            val rol = prefs.getString("rol", "SUPERVISOR") ?: "SUPERVISOR"   // 👈 nuevo

            // Los paso como extras igual que cuando haces login
            val i = Intent(this, HomeActivity::class.java)
            i.putExtra("id_usuario", idUsuario)
            i.putExtra("nombre", nombre)
            i.putExtra("rol", rol)  // 👈 enviar rol al Home
            startActivity(i)
            finish()
            return
        }

        // Si no hay sesión, muestro login
        setContentView(R.layout.activity_main)

        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        txtRegistro = findViewById(R.id.txtIrRegistro)
        txtOlvide = findViewById(R.id.txtOlvide)

        btnLogin.setOnClickListener { hacerLogin() }

        txtRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        txtOlvide.setOnClickListener {
            startActivity(Intent(this, RecuperarActivity::class.java))
        }
    }

    private fun hacerLogin() {
        val email = edtEmail.text.toString().trim()
        val pass = edtPassword.text.toString().trim()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val url = ApiConfig.BASE_URL + "login.php"

        val json = JSONObject()
        json.put("email", email)
        json.put("password", pass)

        val request = JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val idUsuario = response.getInt("id_usuario")
                        val nombre = response.getString("nombre")
                        val rol = response.getString("rol")
                        val apiToken = response.optString("api_token", "")   // 👈 leer rol del PHP

                        // 🔹 Guardar la sesión (incluyendo rol)
                        guardarSesion(idUsuario, nombre, email, rol, apiToken)

                        // Ir al Home
                        val i = Intent(this, HomeActivity::class.java)
                        i.putExtra("id_usuario", idUsuario)
                        i.putExtra("nombre", nombre)
                        i.putExtra("rol", rol)   // 👈 mandar rol
                        startActivity(i)
                        finish()
                    } else {
                        Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error red: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    // ⬇️ Ahora guarda también el rol
    private fun guardarSesion(idUsuario: Int, nombre: String, email: String, rol: String, apiToken: String) {
        val prefs = getSharedPreferences("sesion", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putInt("id_usuario", idUsuario)
        editor.putString("nombre", nombre)
        editor.putString("email", email)
        editor.putString("rol", rol)
        editor.putString("api_token", apiToken)
        editor.apply()
    }
}
