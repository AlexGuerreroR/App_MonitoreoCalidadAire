package com.example.monitoreoaire

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.monitoreoaire.ApiConfig.BASE_URL
import org.json.JSONObject
import java.util.Locale

class ConfigFragment : Fragment() {

    private lateinit var rgUnidadTemp: RadioGroup
    private lateinit var rbCelsius: RadioButton
    private lateinit var rbFahrenheit: RadioButton
    private lateinit var edtUmbralCO2: EditText
    private lateinit var edtUmbralTemp: EditText
    private lateinit var edtUmbralHum: EditText
    private lateinit var edtUmbralIndice: EditText
    private lateinit var btnGuardar: Button

    private lateinit var itemPerfil: View
    private lateinit var itemCrearCuenta: View
    private lateinit var itemCerrarSesion: View
    private lateinit var itemResetDispositivo: View

    private val idUsuario: Int
        get() = (activity as? HomeActivity)?.idUsuario ?: 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ajustes de medición
        rgUnidadTemp = view.findViewById(R.id.rgUnidadTemp)
        rbCelsius = view.findViewById(R.id.rbCelsius)
        rbFahrenheit = view.findViewById(R.id.rbFahrenheit)
        edtUmbralCO2 = view.findViewById(R.id.edtUmbralCO2)
        edtUmbralTemp = view.findViewById(R.id.edtUmbralTemp)
        edtUmbralHum = view.findViewById(R.id.edtUmbralHum)
        edtUmbralIndice = view.findViewById(R.id.edtUmbralIndice)
        btnGuardar = view.findViewById(R.id.btnGuardarConfig)

        // Items tipo menú
        itemPerfil = view.findViewById(R.id.itemPerfil)
        itemCrearCuenta = view.findViewById(R.id.itemCrearCuenta)
        itemCerrarSesion = view.findViewById(R.id.itemCerrarSesion)
        itemResetDispositivo = view.findViewById(R.id.itemResetDispositivo)

        // ==== Datos de sesión (para rol) ====
        val sessionPrefs = requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val rolUsuario = sessionPrefs.getString("rol", "SUPERVISOR") ?: "SUPERVISOR"

        if (rolUsuario == "TECNICO") {
            btnGuardar.isEnabled = false
        }

        // Ir a pantalla de perfil
        itemPerfil.setOnClickListener {
            startActivity(Intent(requireContext(), PerfilActivity::class.java))
        }

        // Solo ADMIN ve "Crear cuenta de supervisor"
        if (rolUsuario == "ADMIN") {
            itemCrearCuenta.visibility = View.VISIBLE
            itemCrearCuenta.setOnClickListener {
                startActivity(Intent(requireContext(), RegistroActivity::class.java))
            }
        } else {
            itemCrearCuenta.visibility = View.GONE
        }

        itemCerrarSesion.setOnClickListener {
            confirmarCerrarSesion()
        }

        itemResetDispositivo.setOnClickListener {
            confirmarFactoryReset()
        }

        // ==== Cargar preferencias guardadas (local) ====
        val prefs = requireContext().getSharedPreferences("app_config", Context.MODE_PRIVATE)
        val unidad = prefs.getString("unidad_temp", "C")
        val umbralCo2 = prefs.getInt("umbral_co2", 800)
        val umbralTemp = prefs.getInt("umbral_temp", 30)
        val umbralHum = prefs.getInt("umbral_hum", 65)
        val umbralIndice = prefs.getInt("umbral_indice", 60)

        if (unidad == "F") {
            rbFahrenheit.isChecked = true
        } else {
            rbCelsius.isChecked = true
        }

        edtUmbralCO2.setText(umbralCo2.toString())
        edtUmbralTemp.setText(umbralTemp.toString())
        edtUmbralHum.setText(umbralHum.toString())
        edtUmbralIndice.setText(umbralIndice.toString())

        // Cargar umbrales desde el servidor para sobreescribir si hay datos
        cargarUmbralesServidor()

        btnGuardar.setOnClickListener {
            guardarConfig()
        }
    }

    // ================== GUARDAR CONFIG APP + UMBRALES ==================
    private fun guardarConfig() {
        val unidadSeleccionada = if (rbFahrenheit.isChecked) "F" else "C"

        val txtCo2 = edtUmbralCO2.text.toString().trim()
        val txtTemp = edtUmbralTemp.text.toString().trim()
        val txtHum = edtUmbralHum.text.toString().trim()
        val txtIndice = edtUmbralIndice.text.toString().trim()

        if (txtCo2.isEmpty() || txtTemp.isEmpty() || txtHum.isEmpty() || txtIndice.isEmpty()) {
            Toast.makeText(requireContext(), "Completa todos los umbrales", Toast.LENGTH_SHORT).show()
            return
        }

        val umbralCo2 = txtCo2.toIntOrNull()
        val umbralTemp = txtTemp.toIntOrNull()
        val umbralHum = txtHum.toIntOrNull()
        val umbralIndice = txtIndice.toIntOrNull()

        if (umbralCo2 == null || umbralCo2 <= 0) {
            Toast.makeText(requireContext(), "Umbral de CO₂ no válido", Toast.LENGTH_SHORT).show()
            return
        }
        if (umbralTemp == null) {
            Toast.makeText(requireContext(), "Umbral de temperatura no válido", Toast.LENGTH_SHORT).show()
            return
        }
        if (umbralHum == null) {
            Toast.makeText(requireContext(), "Umbral de humedad no válido", Toast.LENGTH_SHORT).show()
            return
        }
        if (umbralIndice == null || umbralIndice < 0 || umbralIndice > 100) {
            Toast.makeText(requireContext(), "El índice debe estar entre 0 y 100", Toast.LENGTH_SHORT).show()
            return
        }

        // Guardar en SharedPreferences
        val prefs = requireContext().getSharedPreferences("app_config", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("unidad_temp", unidadSeleccionada)
            .putInt("umbral_co2", umbralCo2)
            .putInt("umbral_temp", umbralTemp)
            .putInt("umbral_hum", umbralHum)
            .putInt("umbral_indice", umbralIndice)
            .apply()

        // Enviar al servidor para actualizar tabla umbrales
        guardarUmbralesServidor(umbralCo2, umbralTemp, umbralHum, umbralIndice)
    }

    private fun guardarUmbralesServidor(
        co2: Int,
        temp: Int,
        hum: Int,
        indice: Int
    ) {
        if (idUsuario <= 0) {
            Toast.makeText(requireContext(), "No se pudo obtener id de usuario", Toast.LENGTH_SHORT).show()
            return
        }

        val url = BASE_URL + "guardar_umbrales.php" // AJUSTA ESTA URL

        val queue = Volley.newRequestQueue(requireContext())
        val request = object : StringRequest(
            Request.Method.POST,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val ok = json.optBoolean("success", false)
                    val msg = json.optString("message", "")
                    Toast.makeText(
                        requireContext(),
                        if (ok) "Umbrales guardados" else "Error: $msg",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error al procesar respuesta", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(requireContext(), "Error de red al guardar umbrales", Toast.LENGTH_SHORT).show()
            }
        ){
            override fun getHeaders(): MutableMap<String, String> {
                return Session.authHeaders(requireContext())
            }

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["id_usuario"] = idUsuario.toString()
                params["umbral_co2"] = co2.toString()
                params["umbral_temp"] = temp.toString()
                params["umbral_hum"] = hum.toString()
                params["umbral_indice"] = indice.toString()
                return params
            }
        }

        queue.add(request)
    }

    private fun cargarUmbralesServidor() {
        if (idUsuario <= 0) return

        val url = BASE_URL + "obtener_umbrales.php"

        val queue = Volley.newRequestQueue(requireContext())
        val request = object : StringRequest(
            Request.Method.POST,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optBoolean("success", false)) {
                        val data = json.getJSONObject("data")

                        val co2 = data.optDouble("CO2", edtUmbralCO2.text.toString().toDoubleOrNull() ?: 1000.0)
                        val temp = data.optDouble("TEMP", edtUmbralTemp.text.toString().toDoubleOrNull() ?: 27.0)
                        val hum = data.optDouble("HUM", edtUmbralHum.text.toString().toDoubleOrNull() ?: 65.0)
                        val indice = data.optDouble("INDICE", edtUmbralIndice.text.toString().toDoubleOrNull() ?: 60.0)

                        edtUmbralCO2.setText(co2.toInt().toString())
                        edtUmbralTemp.setText(temp.toInt().toString())
                        edtUmbralHum.setText(hum.toInt().toString())
                        edtUmbralIndice.setText(indice.toInt().toString())
                    }
                } catch (_: Exception) {
                }
            },
            {
                // si falla, dejamos lo que está en prefs
            }
        ){
            override fun getHeaders(): MutableMap<String, String> {
                return Session.authHeaders(requireContext())
            }

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["id_usuario"] = idUsuario.toString()
                return params
            }
        }

        queue.add(request)
    }

    // ================== CERRAR SESIÓN ==================
    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ -> cerrarSesion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        val sessionPrefs =
            requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE)
        sessionPrefs.edit().clear().apply()

        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // ================== FACTORY RESET ESP32 ==================
    private fun confirmarFactoryReset() {
        AlertDialog.Builder(requireContext())
            .setTitle("Restablecer dispositivo")
            .setMessage(
                "Esto borrará la configuración WiFi/token del sensor y lo pondrá en modo AP.\n\n" +
                        "¿Deseas continuar?"
            )
            .setPositiveButton("Sí") { _, _ -> enviarFactoryReset() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarFactoryReset() {
        val prefs = requireContext().getSharedPreferences("app_config", Context.MODE_PRIVATE)

        val ipFromArgs = arguments?.getString("espIp")
        val ipFromPrefs = prefs.getString("esp_ip", null)
        val ipEsp = ipFromArgs ?: ipFromPrefs ?: "192.168.4.1"

        val url = "http://$ipEsp/factory_reset"

        val queue = Volley.newRequestQueue(requireContext())
        val request = StringRequest(
            Request.Method.GET,
            url,
            { _ ->
                Toast.makeText(
                    requireContext(),
                    "Factory reset enviado a $ipEsp. El dispositivo se reiniciará y entrará en modo AP.",
                    Toast.LENGTH_LONG
                ).show()
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error al contactar al dispositivo en $ipEsp",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        queue.add(request)
    }


    // ===== Helpers para usar desde otras pantallas (unidades) =====
    companion object {
        fun getUnidadTemperatura(context: Context): String {
            val prefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
            return prefs.getString("unidad_temp", "C") ?: "C"
        }

        fun getUmbralCo2(context: Context): Int {
            val prefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
            return prefs.getInt("umbral_co2", 1000)
        }

        fun formatearTemperatura(celsius: Float, context: Context): String {
            val unidad = getUnidadTemperatura(context)
            return if (unidad == "F") {
                val f = celsius * 9f / 5f + 32f
                String.format(Locale.getDefault(), "%.1f °F", f)
            } else {
                String.format(Locale.getDefault(), "%.1f °C", celsius)
            }
        }
    }
}
