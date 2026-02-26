package com.example.monitoreoaire



import android.content.Intent

import android.os.Bundle

import android.util.Patterns

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



// 1. Validar formato de correo

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            Toast.makeText(this, "Formato de correo inválido", Toast.LENGTH_SHORT).show()

            return

        }



// NUEVA REGEX:

// ^(?=.*[A-Za-z]) -> Al menos una letra

// (?=.*\d) -> Al menos un número

// (?=.*[@$!%*?&_ \-]) -> Al menos un carácter especial (incluye _ y -)

// [A-Za-z\d@$!%*?&_ \-]{8,}$ -> Mínimo 8 caracteres en total

        val passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&_\\-])[A-Za-z\\d@$!%*?&_\\-]{8,}$".toRegex()



        if (!pass.matches(passwordRegex)) {

// En lugar de Toast, usamos error en el campo para que no se borre rápido

            edtPass.error = "Mínimo 8 caracteres: letras, números y símbolos (@$!%*?& _ -)"

            edtPass.requestFocus()

            return

        }



        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty() || pass2.isEmpty() || pregunta.isEmpty() || respuesta.isEmpty()) {

            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()

            return

        }

        if (pass != pass2) {

            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()

            return

        }



        val rol = Session.getRole(this)

        val isLoggedIn = Session.isLoggedIn(this)



// Si el que está usando la app es un ADMIN logueado, crea técnicos/supervisores

        if (isLoggedIn && rol == "ADMIN") {

            seleccionarRolYCrear(nombre, email, pass, pregunta, respuesta)

        } else {

// Si no hay nadie logueado, es el registro inicial de un nuevo ADMIN

            registrarPrimerAdmin(nombre, email, pass, pregunta, respuesta)

        }

    }



    private fun seleccionarRolYCrear(nombre: String, email: String, pass: String, pregunta: String, respuesta: String) {

        val roles = arrayOf("SUPERVISOR", "TECNICO")

        AlertDialog.Builder(this)

            .setTitle("Seleccionar Rol para el nuevo usuario")

            .setItems(roles) { _, which ->

                val rolSeleccionado = roles[which]

                crearUsuarioComoAdmin(nombre, email, pass, pregunta, respuesta, rolSeleccionado)

            }

            .setNegativeButton("Cancelar", null)

            .show()

    }



    private fun crearUsuarioComoAdmin(

        nombre: String,

        email: String,

        pass: String,

        pregunta: String,

        respuesta: String,

        rol: String

    ) {

        val url = ApiConfig.BASE_URL + "crear_usuario.php"



// USAMOS EL OBJETO SESSION PARA EVITAR EL ID EN 0

        val idAdmin = Session.getUserId(this)



        if (idAdmin <= 0) {

            Toast.makeText(this, "Sesión inválida. Por favor, inicia sesión nuevamente.", Toast.LENGTH_LONG).show()

            return

        }



        val json = JSONObject().apply {

            put("nombre", nombre)

            put("email", email)

            put("password", pass)

            put("pregunta_seguridad", pregunta)

            put("respuesta_seguridad", respuesta)

            put("rol", rol)

            put("id_admin_creador", idAdmin)

        }



        val req = object : JsonObjectRequest(

            Request.Method.POST, url, json,

            { response ->

                val ok = response.optBoolean("success", false)

                val msg = response.optString("message", "Usuario creado")

                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

                if (ok) finish()

            },

            { error ->

                Toast.makeText(this, "Error de red: ${error.message}", Toast.LENGTH_LONG).show()

            }

        ) {

            override fun getHeaders(): MutableMap<String, String> = Session.authHeaders(this@RegistroActivity)

        }



        Volley.newRequestQueue(this).add(req)

    }



    private fun registrarPrimerAdmin(nombre: String, email: String, pass: String, pregunta: String, respuesta: String) {

        val url = ApiConfig.BASE_URL + "registro.php"



        val json = JSONObject().apply {

            put("nombre", nombre)

            put("email", email)

            put("password", pass)

            put("pregunta_seguridad", pregunta)

            put("respuesta_seguridad", respuesta)

// No enviamos id_admin_creador aquí

        }



        val req = JsonObjectRequest(Request.Method.POST, url, json,

            { response ->

                val ok = response.optBoolean("success")

                val msg = response.optString("message")

                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

                if (ok) finish()

            },

            { error ->

                Toast.makeText(this, "Error de red: ${error.message}", Toast.LENGTH_SHORT).show()

            }

        )

// IMPORTANTE: Aquí NO usamos Session.authHeaders() porque no hay sesión

        Volley.newRequestQueue(this).add(req)

    }

}