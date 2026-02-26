package com.example.monitoreoaire

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class GestionCuentasActivity : AppCompatActivity() {

    private lateinit var btnCrear: Button
    private lateinit var lvUsuarios: ListView

    private val listaNombres = ArrayList<String>()
    private val listaIds = ArrayList<Int>()
    private val listaRoles = ArrayList<String>()
    private val listaEmails = ArrayList<String>()

    private val idAdmin: Int
        get() = Session.getUserId(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_cuentas)

        btnCrear = findViewById(R.id.btnIrACrearCuenta)
        lvUsuarios = findViewById(R.id.lvUsuarios)

        btnCrear.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        lvUsuarios.setOnItemClickListener { _, _, position, _ ->
            mostrarOpcionesUsuario(position)
        }
    }

    override fun onResume() {
        super.onResume()
        if (idAdmin > 0) {
            cargarUsuariosDesdeServidor()
        } else {
            Toast.makeText(this, "Error: No se detectó sesión de Administrador", Toast.LENGTH_LONG).show()
        }
    }

    private fun cargarUsuariosDesdeServidor() {
        val url = ApiConfig.BASE_URL + "listar_usuarios.php?id_admin=$idAdmin"

        val request = object : JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                listaNombres.clear()
                listaIds.clear()
                listaRoles.clear()
                listaEmails.clear()

                for (i in 0 until response.length()) {
                    val userObj = response.getJSONObject(i)
                    listaIds.add(userObj.getInt("id"))
                    listaNombres.add(userObj.getString("nombre"))
                    listaRoles.add(userObj.getString("rol"))
                    listaEmails.add(userObj.getString("email"))
                }

                val adapter = object : ArrayAdapter<String>(this, R.layout.item_usuario, R.id.txtNombreUsuario, listaNombres) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent)
                        val txtNombre = view.findViewById<TextView>(R.id.txtNombreUsuario)
                        val txtRol = view.findViewById<TextView>(R.id.txtRolUsuario)

                        txtNombre.text = listaNombres[position]
                        txtRol.text = "Rol: ${listaRoles[position]}"
                        return view
                    }
                }
                lvUsuarios.adapter = adapter
            },
            { Toast.makeText(this, "Error al cargar lista", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getHeaders(): MutableMap<String, String> = Session.authHeaders(this@GestionCuentasActivity)
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun mostrarOpcionesUsuario(pos: Int) {
        val opciones = arrayOf("Editar Usuario", "Eliminar Cuenta")
        AlertDialog.Builder(this)
            .setTitle(listaNombres[pos])
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirFormularioEdicion(pos)
                    1 -> confirmarEliminacion(pos)
                }
            }
            .show()
    }

    private fun abrirFormularioEdicion(pos: Int) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val edtNom = EditText(this).apply { hint = "Nombre"; setText(listaNombres[pos]) }
        val edtMail = EditText(this).apply { hint = "Email"; setText(listaEmails[pos]) }

        // Selector de Rol
        val spinnerRol = Spinner(this)
        val roles = arrayOf("SUPERVISOR", "TECNICO")
        val adapterRol = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        spinnerRol.adapter = adapterRol
        // Seleccionar el rol actual del usuario en el spinner
        val currentRolPos = if (listaRoles[pos] == "SUPERVISOR") 0 else 1
        spinnerRol.setSelection(currentRolPos)

        layout.addView(edtNom)
        layout.addView(edtMail)
        layout.addView(TextView(this).apply { text = "  Seleccionar Rol:"; textSize = 14f })
        layout.addView(spinnerRol)

        AlertDialog.Builder(this)
            .setTitle("Editar Usuario")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val n = edtNom.text.toString().trim()
                val m = edtMail.text.toString().trim()
                val r = spinnerRol.selectedItem.toString()
                if (n.isNotEmpty() && m.isNotEmpty()) {
                    actualizarUsuarioServidor(listaIds[pos], n, m, r)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarUsuarioServidor(id: Int, n: String, m: String, r: String) {
        val url = ApiConfig.BASE_URL + "editar_usuario.php"
        val req = object : StringRequest(Method.POST, url,
            { response ->
                val json = JSONObject(response)
                if (json.optBoolean("success")) {
                    Toast.makeText(this, "Actualizado", Toast.LENGTH_SHORT).show()
                    cargarUsuariosDesdeServidor()
                } else {
                    // AQUÍ MOSTRARÁ EL ERROR SI EL EMAIL ESTÁ DUPLICADO
                    Toast.makeText(this, json.optString("message"), Toast.LENGTH_LONG).show()
                }
            },
            { Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams() = hashMapOf(
                "id" to id.toString(),
                "nombre" to n,
                "email" to m,
                "rol" to r
            )
            override fun getHeaders() = Session.authHeaders(this@GestionCuentasActivity)
        }
        Volley.newRequestQueue(this).add(req)
    }

    private fun confirmarEliminacion(pos: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Seguro que quieres borrar a ${listaNombres[pos]}?")
            .setPositiveButton("Sí") { _, _ -> eliminarUsuarioServidor(listaIds[pos]) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun actualizarUsuarioServidor(id: Int, nuevoNombre: String, nuevoEmail: String) {
        val url = ApiConfig.BASE_URL + "editar_usuario.php"
        val request = object : StringRequest(Method.POST, url,
            { cargarUsuariosDesdeServidor() },
            { Toast.makeText(this, "Error al editar", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf(
                "id" to id.toString(),
                "nombre" to nuevoNombre,
                "email" to nuevoEmail
            )
            override fun getHeaders(): MutableMap<String, String> = Session.authHeaders(this@GestionCuentasActivity)
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun eliminarUsuarioServidor(id: Int) {
        val url = ApiConfig.BASE_URL + "eliminar_usuario.php"
        val request = object : StringRequest(Method.POST, url,
            { cargarUsuariosDesdeServidor() },
            { Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("id" to id.toString())
            override fun getHeaders(): MutableMap<String, String> = Session.authHeaders(this@GestionCuentasActivity)
        }
        Volley.newRequestQueue(this).add(request)
    }
}