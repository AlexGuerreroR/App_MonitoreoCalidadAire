package com.example.monitoreoaire

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class DispositivosFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var btnAgregar: Button

    private val listaTexto = ArrayList<String>()
    private val idsDispositivos = ArrayList<Int>()
    private val tokensDispositivos = ArrayList<String>()

    private val idUsuario: Int
        get() = (activity as? HomeActivity)?.idUsuario ?: 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_dispositivos, container, false)
        listView = v.findViewById(R.id.lvDispositivos)
        btnAgregar = v.findViewById(R.id.btnAgregarDispositivo)

        val rolUsuario = Session.getRole(requireContext())
        if (rolUsuario == "TECNICO") {
            btnAgregar.visibility = View.GONE
        }

        // Botón para agregar dispositivo (igual que antes)
        btnAgregar.setOnClickListener { mostrarDialogoAgregar() }

        // Al tocar un dispositivo ahora mostramos menú: Configurar / Eliminar
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position in idsDispositivos.indices) {
                mostrarOpcionesDispositivo(position)
            }
        }

        cargarDispositivos()

        return v
    }

    // ========== CARGAR DISPOSITIVOS DESDE PHP ==========

    private fun cargarDispositivos() {
        if (idUsuario == 0) return

        val rol = Session.getRole(requireContext())
        val idConsulta = if (rol == "ADMIN" || rol == "SUPERVISOR") 0 else idUsuario
        val url = ApiConfig.BASE_URL + "listar_dispositivos.php?id_usuario=$idConsulta"

        val request = object : JsonArrayRequest(
            Request.Method.GET, url, null,
            { response: JSONArray ->
                listaTexto.clear()
                idsDispositivos.clear()
                tokensDispositivos.clear()

                for (i in 0 until response.length()) {
                    val obj = response.getJSONObject(i)
                    val id = obj.getInt("id")
                    val nombre = obj.getString("nombre_dispositivo")
                    val ubicacion = obj.optString("ubicacion", "")
                    val token = obj.optString("token_dispositivo", "")

                    idsDispositivos.add(id)
                    tokensDispositivos.add(token)

                    val linea = buildString {
                        append(nombre)
                        if (ubicacion.isNotEmpty()) append("\nLugar: $ubicacion")
                    }

                    listaTexto.add(linea)
                }

                // Usamos el layout con tarjeta
                val adapter = ArrayAdapter(
                    requireContext(),
                    R.layout.item_dispositivo,
                    R.id.txtLineaDispositivo,
                    listaTexto
                )
                listView.adapter = adapter
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ){
            override fun getHeaders(): MutableMap<String, String> {
                return Session.authHeaders(requireContext())
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    // ========== MENÚ: CONFIGURAR / ELIMINAR ==========

    private fun mostrarOpcionesDispositivo(position: Int) {
        val nombre = listaTexto[position].substringBefore("\n")
        val opciones = arrayOf("Configurar red", "Eliminar dispositivo")

        AlertDialog.Builder(requireContext())
            .setTitle(nombre)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> mostrarDialogoConfigurar(position)
                    1 -> confirmarEliminar(position)
                }
            }
            .show()
    }

    private fun confirmarEliminar(position: Int) {
        val nombre = listaTexto[position].substringBefore("\n")
        val id = idsDispositivos[position]

        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar dispositivo")
            .setMessage("¿Seguro que deseas eliminar \"$nombre\"?\n\n" +
                    "Se borrarán también todas las lecturas y umbrales asociados a este dispositivo.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarDispositivo(id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarDispositivo(idDispositivo: Int) {
        val url = ApiConfig.BASE_URL + "eliminar_dispositivo.php"

        val request = object : StringRequest(
            Method.POST, url,
            { resp ->
                try {
                    val obj = JSONObject(resp)
                    if (obj.optBoolean("success", false)) {
                        Toast.makeText(requireContext(), "Dispositivo eliminado", Toast.LENGTH_SHORT).show()
                        cargarDispositivos()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            obj.optString("message", "Error al eliminar"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Respuesta inválida del servidor", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return Session.authHeaders(requireContext())
            }

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["id_dispositivo"] = idDispositivo.toString()
                return params
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    // ========== DIALOGO PARA AGREGAR DISPOSITIVO (NOMBRE + LUGAR) ==========

    private fun mostrarDialogoAgregar() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Nuevo dispositivo")

        val v = layoutInflater.inflate(R.layout.dialog_nuevo_dispositivo, null)
        val edtNombre = v.findViewById<EditText>(R.id.edtNombreDispositivo)
        val edtLugar = v.findViewById<EditText>(R.id.edtLugarDispositivo)

        builder.setView(v)
        builder.setPositiveButton("Guardar") { _, _ ->
            val nombre = edtNombre.text.toString().trim()
            val lugar = edtLugar.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa un nombre", Toast.LENGTH_SHORT).show()
            } else {
                crearDispositivo(nombre, lugar)
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun crearDispositivo(nombre: String, lugar: String) {
        val url = ApiConfig.BASE_URL + "crear_dispositivo.php"

        val json = JSONObject().apply {
            put("id_usuario", idUsuario)
            put("nombre_dispositivo", nombre)
            put("ubicacion", lugar)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                if (response.optBoolean("success", false)) {
                    Toast.makeText(requireContext(), "Dispositivo creado", Toast.LENGTH_SHORT).show()
                    cargarDispositivos()
                } else {
                    Toast.makeText(
                        requireContext(),
                        response.optString("message", "Error"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return Session.authHeaders(requireContext())
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    // ========== DIALOGO PARA CONFIGURAR ESP32 (SSID + PASSWORD) ==========

    private fun mostrarDialogoConfigurar(position: Int) {
        if (position !in tokensDispositivos.indices) {
            Toast.makeText(requireContext(), "Datos del dispositivo inválidos", Toast.LENGTH_SHORT).show()
            return
        }

        val token = tokensDispositivos[position]

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Configurar dispositivo")

        val v = layoutInflater.inflate(R.layout.dialog_config_dispositivo, null)
        val edtSsid = v.findViewById<EditText>(R.id.edtSsidConfig)
        val edtPass = v.findViewById<EditText>(R.id.edtPassConfig)
        val txtInfo  = v.findViewById<TextView>(R.id.txtInfoToken)

        txtInfo.text = "Token del dispositivo (usado internamente):\n$token"

        builder.setView(v)
        builder.setPositiveButton("Enviar config") { _, _ ->
            val ssid = edtSsid.text.toString().trim()
            val pass = edtPass.text.toString().trim()

            if (ssid.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa el SSID de la red", Toast.LENGTH_SHORT).show()
            } else {
                enviarConfigAESP32(token, ssid, pass)
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun enviarConfigAESP32(token: String, ssid: String, pass: String) {
        // IMPORTANTE: el celular debe estar conectado a la WiFi del ESP32 (SensorAire-ESP32)
        val url = "http://192.168.4.1/config"

        val request = object : StringRequest(
            Method.POST, url,
            {
                Toast.makeText(requireContext(), "Config enviada a ESP32", Toast.LENGTH_SHORT).show()
            },
            { error ->
                Toast.makeText(requireContext(), "Error al enviar config: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["ssid"] = ssid
                params["password"] = pass
                params["token"] = token
                params["serverUrl"] = ApiConfig.SERVER_ESP_URL
                return params
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }
}
