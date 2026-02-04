package com.example.monitoreoaire

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class RecuperarActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtRespuesta: EditText
    private lateinit var edtNuevaPass: EditText
    private lateinit var btnRecuperar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar)

        edtEmail = findViewById(R.id.edtEmailRec)
        edtRespuesta = findViewById(R.id.edtRespuestaRec)
        edtNuevaPass = findViewById(R.id.edtNuevaPass)
        btnRecuperar = findViewById(R.id.btnRecuperar)

        btnRecuperar.setOnClickListener { recuperar() }
    }

    private fun recuperar() {
        val email = edtEmail.text.toString().trim()
        val resp = edtRespuesta.text.toString().trim()
        val nueva = edtNuevaPass.text.toString().trim()

        if (email.isEmpty() || resp.isEmpty() || nueva.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val url = ApiConfig.BASE_URL + "recuperar_password.php"

        val json = JSONObject()
        json.put("email", email)
        json.put("respuesta", resp)
        json.put("nueva_password", nueva)

        val request = JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                if (response.optBoolean("success", false)) {
                    Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, response.optString("message","Error"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error red: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}
