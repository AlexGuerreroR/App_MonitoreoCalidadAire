package com.example.monitoreoaire

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class RegistroActivity : AppCompatActivity() {

    private lateinit var edtNombre: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPass: EditText
    private lateinit var edtPass2: EditText
    private lateinit var edtPregunta: EditText
    private lateinit var edtRespuesta: EditText
    private lateinit var btnRegistrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        edtNombre = findViewById(R.id.edtNombre)
        edtEmail = findViewById(R.id.edtEmailReg)
        edtPass = findViewById(R.id.edtPass)
        edtPass2 = findViewById(R.id.edtPass2)
        edtPregunta = findViewById(R.id.edtPregunta)
        edtRespuesta = findViewById(R.id.edtRespuesta)
        btnRegistrar = findViewById(R.id.btnRegistrar)

        btnRegistrar.setOnClickListener { ejecutarRegistro() }
    }

    private fun ejecutarRegistro() {
        val nombre = edtNombre.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val pass = edtPass.text.toString().trim()
        val pass2 = edtPass2.text.toString().trim()
        val pregunta = edtPregunta.text.toString().trim()
        val respuesta = edtRespuesta.text.toString().trim()

        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty() || pass2.isEmpty() || pregunta.isEmpty() || respuesta.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (pass != pass2) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        val rol = Session.getRole(this)
        val isAdmin = (rol == "ADMIN" && Session.isLoggedIn(this))

        if (isAdmin) {
            seleccionarRolYCrear(nombre, email, pass, pregunta, respuesta)
        } else {
            registrarPrimerAdmin(nombre, email, pass, pregunta, respuesta)
        }
    }

    private fun seleccionarRolYCrear(nombre: String, email: String, pass: String, pregunta: String, respuesta: String) {
        val roles = arrayOf("SUPERVISOR", "TECNICO")
        AlertDialog.Builder(this)
            .setTitle("Rol del usuario")
            .setItems(roles) { _, which ->
                val rolSeleccionado = roles[which]
                crearUsuarioComoAdmin(nombre, email, pass, pregunta, respuesta, rolSeleccionado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearUsuarioComoAdmin(nombre: String, email: String, pass: String, pregunta: String, respuesta: String, rol: String) {
        val url = ApiConfig.BASE_URL + "crear_usuario.php"

        val json = JSONObject().apply {
            put("nombre", nombre)
            put("email", email)
            put("password", pass)
            put("pregunta", pregunta)
            put("respuesta", respuesta)
            put("rol", rol)
        }

        val req = object : JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                val ok = response.optBoolean("success", false)
                val msg = response.optString("message", "Respuesta del servidor")
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                if (ok) finish()
            },
            { error ->
                Toast.makeText(this, "Error red: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return Session.authHeaders(this@RegistroActivity)
            }
        }

        Volley.newRequestQueue(this).add(req)
    }

    private fun registrarPrimerAdmin(nombre: String, email: String, pass: String, pregunta: String, respuesta: String) {
        val url = ApiConfig.BASE_URL + "registro.php"

        val json = JSONObject().apply {
            put("nombre", nombre)
            put("email", email)
            put("password", pass)
            put("pregunta", pregunta)
            put("respuesta", respuesta)
        }

        val req = JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                val ok = response.optBoolean("success", false)
                val msg = response.optString("message", "Respuesta del servidor")
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                if (ok) finish()
            },
            { error ->
                Toast.makeText(this, "Error red: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        Volley.newRequestQueue(this).add(req)
    }
}
