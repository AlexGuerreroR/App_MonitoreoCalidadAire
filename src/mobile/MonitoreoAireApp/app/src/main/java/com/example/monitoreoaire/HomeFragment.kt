package com.example.monitoreoaire

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import android.view.Gravity


class HomeFragment : Fragment() {

    private lateinit var spDispositivos: Spinner

    private val nombresDispositivos = ArrayList<String>()
    private val idsDispositivos = ArrayList<Int>()
    private val tokensDispositivos = ArrayList<String>()

    private lateinit var viewCircleEstado: View
    private lateinit var txtEstado: TextView
    private lateinit var txtUltimaLectura: TextView

    private lateinit var txtCO2: TextView
    private lateinit var txtTemp: TextView
    private lateinit var txtHum: TextView

    private lateinit var txtTempConfort: TextView
    private lateinit var txtHumConfort: TextView
    private lateinit var txtCO2Objetivo: TextView
    private lateinit var txtIndiceCalidad: TextView
    private lateinit var txtIndiceNivel: TextView

    private lateinit var cardRiesgoSee: CardView
    private lateinit var txtRiesgoSee: TextView
    private lateinit var txtRecomendacion: TextView

    private lateinit var requestQueue: RequestQueue
    private var selectedToken: String? = null

    private val watchdogHandler = Handler(Looper.getMainLooper())
    private var watchdogRunnable: Runnable? = null
    private var lastMqttAt: Long = 0L
    private val MQTT_TIMEOUT_MS = 25_000L

    private val idUsuario: Int
        get() = (activity as? HomeActivity)?.idUsuario ?: 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requestQueue = Volley.newRequestQueue(context.applicationContext)

        MqttManager.init(context)
        MqttManager.setOnMessageListener { payload ->
            try {
                val obj = JSONObject(payload)

                val tokenMsg = obj.optString("token", "")
                val tokenSel = selectedToken ?: ""
                if (tokenSel.isNotEmpty() && tokenMsg.isNotEmpty() && tokenMsg != tokenSel) {
                    return@setOnMessageListener
                }

                val co2 = obj.optDouble("co2", 0.0)
                val temp = obj.optDouble("temperatura", 0.0)
                val hum = obj.optDouble("humedad", 0.0)

                val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                lastMqttAt = System.currentTimeMillis()

                activity?.runOnUiThread {
                    actualizarDashboard(co2, temp, hum, "Tiempo real $hora")
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_home, container, false)

        spDispositivos = v.findViewById(R.id.spDispositivosHome)

        viewCircleEstado = v.findViewById(R.id.viewCircleEstado)
        txtEstado = v.findViewById(R.id.txtEstado)
        txtEstado.textAlignment = View.TEXT_ALIGNMENT_CENTER
        txtEstado.gravity = Gravity.CENTER
        txtUltimaLectura = v.findViewById(R.id.txtUltimaLectura)

        txtCO2 = v.findViewById(R.id.txtCO2)
        txtTemp = v.findViewById(R.id.txtTemp)
        txtHum = v.findViewById(R.id.txtHum)

        txtTempConfort = v.findViewById(R.id.txtTempConfort)
        txtHumConfort = v.findViewById(R.id.txtHumConfort)
        txtCO2Objetivo = v.findViewById(R.id.txtCO2Objetivo)
        txtIndiceCalidad = v.findViewById(R.id.txtIndiceCalidad)
        txtIndiceNivel = v.findViewById(R.id.txtIndiceNivel)

        cardRiesgoSee = v.findViewById(R.id.cardRiesgoSee)
        txtRiesgoSee = v.findViewById(R.id.txtRiesgoSee)
        txtRecomendacion = v.findViewById(R.id.txtRecomendacion)

        ponerEstadoEsperando()
        cargarDispositivos()

        spDispositivos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position !in tokensDispositivos.indices) return

                val token = tokensDispositivos[position]
                selectedToken = token

                lastMqttAt = 0L
                ponerEstadoEsperando()

                if (token.isNotEmpty()) {
                    MqttManager.subscribeToDevice(token)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return v
    }

    override fun onResume() {
        super.onResume()
        startWatchdog()
        selectedToken?.let { if (it.isNotEmpty()) MqttManager.subscribeToDevice(it) }
    }

    override fun onPause() {
        super.onPause()
        stopWatchdog()
    }

    private fun startWatchdog() {
        if (watchdogRunnable != null) return

        watchdogRunnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()

                if (selectedToken.isNullOrEmpty()) {
                    watchdogHandler.postDelayed(this, 1000)
                    return
                }

                if (lastMqttAt == 0L) {
                    marcarSinSenal("Esperando datos (?)")
                } else if (now - lastMqttAt > MQTT_TIMEOUT_MS) {
                    marcarSinSenal("Dispositivo sin señal")
                }

                watchdogHandler.postDelayed(this, 1000)
            }
        }

        watchdogHandler.post(watchdogRunnable!!)
    }

    private fun stopWatchdog() {
        watchdogRunnable?.let { watchdogHandler.removeCallbacks(it) }
        watchdogRunnable = null
    }

    private fun ponerEstadoEsperando() {
        txtEstado.text = "Esperando datos (?)"
        txtUltimaLectura.text = "Última lectura: --"
        txtCO2.text = "--"
        txtTemp.text = "--"
        txtHum.text = "--"
        txtIndiceCalidad.text = "--"
        txtIndiceNivel.text = "--"
        txtRiesgoSee.text = "--"
        txtRecomendacion.text = "Cuando llegue un dato, se actualizará aquí."
        viewCircleEstado.setBackgroundResource(R.drawable.bg_circle_warn)
    }

    private fun marcarSinSenal(mensaje: String) {
        txtEstado.text = mensaje
        txtUltimaLectura.text = "Última lectura: --"
        txtCO2.text = "--"
        txtTemp.text = "--"
        txtHum.text = "--"
        txtIndiceCalidad.text = "--"
        txtIndiceNivel.text = "--"
        txtRiesgoSee.text = "--"
        txtRecomendacion.text = "No hay datos recientes del dispositivo."
        viewCircleEstado.setBackgroundResource(R.drawable.bg_circle_warn)
    }

    private fun cargarDispositivos() {
        if (idUsuario == 0) {
            txtEstado.text = "Sin usuario"
            return
        }

        val rol = Session.getRole(requireContext())
        val idConsulta = if (rol == "ADMIN" || rol == "SUPERVISOR") 0 else idUsuario

        val url = ApiConfig.BASE_URL +
                "listar_dispositivos.php?id_usuario=$idConsulta&ts=${System.currentTimeMillis()}"

        val request = object : JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                nombresDispositivos.clear()
                idsDispositivos.clear()
                tokensDispositivos.clear()

                for (i in 0 until response.length()) {
                    val obj = response.getJSONObject(i)
                    val id = obj.getInt("id")
                    val token = obj.optString("token_dispositivo", "")
                    val nombre = obj.getString("nombre_dispositivo")
                    val ubicacion = obj.optString("ubicacion", "")

                    idsDispositivos.add(id)
                    tokensDispositivos.add(token)

                    val label = if (ubicacion.isNotEmpty()) "$nombre - $ubicacion" else nombre
                    nombresDispositivos.add(label)
                }

                if (nombresDispositivos.isEmpty()) {
                    nombresDispositivos.add("Sin dispositivos")
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    nombresDispositivos
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spDispositivos.adapter = adapter

                if (tokensDispositivos.isNotEmpty()) {
                    val firstToken = tokensDispositivos[0]
                    selectedToken = firstToken
                    if (firstToken.isNotEmpty()) {
                        MqttManager.subscribeToDevice(firstToken)
                    }
                }

                lastMqttAt = 0L
                ponerEstadoEsperando()
            },
            { error ->
                txtEstado.text = "Error al cargar dispositivos: ${error.message}"
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return Session.authHeaders(requireContext())
            }
        }

        request.setShouldCache(false)
        request.retryPolicy = DefaultRetryPolicy(5000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        requestQueue.add(request)
    }

    private fun actualizarDashboard(co2: Double, temp: Double, hum: Double, fecha: String) {
        txtCO2.text = "${co2.roundToInt()} ppm"
        txtTemp.text = formatearTempConUnidad(temp)
        txtHum.text = "${hum.roundToInt()} %"
        txtUltimaLectura.text = "Última lectura: $fecha"

        val (estado, bgRes) = when {
            co2 <= 800 -> "Buena" to R.drawable.bg_circle_good
            co2 <= 1200 -> "Aceptable" to R.drawable.bg_circle_warn
            else -> "Mala" to R.drawable.bg_circle_bad
        }
        txtEstado.text = estado
        viewCircleEstado.setBackgroundResource(bgRes)

        val confortTemp = when {
            temp < 21 -> "Frío"
            temp <= 27 -> "Óptimo"
            else -> "Cálido"
        }
        txtTempConfort.text = confortTemp

        val confortHum = when {
            hum < 40 -> "Seco"
            hum <= 60 -> "Óptimo"
            else -> "Húmedo"
        }
        txtHumConfort.text = confortHum

        txtCO2Objetivo.text = if (co2 <= 800) "Objetivo: OK (< 800 ppm)" else "Objetivo: bajar a < 800 ppm"

        val (indice, nivelIndice) = calcularIndiceCalidad(co2)
        txtIndiceCalidad.text = "$indice / 100"
        txtIndiceNivel.text = nivelIndice

        val (nivelSee, colorSee) = calcularRiesgoSee(co2, temp, hum)
        txtRiesgoSee.text = nivelSee
        cardRiesgoSee.setCardBackgroundColor(colorSee)

        txtRecomendacion.text = generarRecomendacion(co2, temp, hum)
    }

    private fun formatearTempConUnidad(tempC: Double): String {
        val prefs = requireContext().getSharedPreferences("app_config", Context.MODE_PRIVATE)
        val unidad = prefs.getString("unidad_temp", "C")
        return if (unidad == "F") {
            val tempF = tempC * 9.0 / 5.0 + 32.0
            "${tempF.roundToInt()} °F"
        } else {
            "${tempC.roundToInt()} °C"
        }
    }

    private fun calcularIndiceCalidad(co2: Double): Pair<Int, String> {
        val min = 400.0
        val max = 2000.0
        val clamped = co2.coerceIn(min, max)
        val score = ((max - clamped) / (max - min) * 100.0).coerceIn(0.0, 100.0).roundToInt()

        val nivel = when {
            score >= 70 -> "Bueno"
            score >= 40 -> "Moderado"
            else -> "Malo"
        }
        return score to nivel
    }

    private fun calcularRiesgoSee(co2: Double, temp: Double, hum: Double): Pair<String, Int> {
        var puntos = 0
        if (co2 > 800) puntos++
        if (temp < 20 || temp > 27) puntos++
        if (hum < 40 || hum > 60) puntos++

        return when {
            puntos <= 0 -> "Bajo" to Color.parseColor("#E8F5E9")
            puntos == 1 || puntos == 2 -> "Medio" to Color.parseColor("#FFFDE7")
            else -> "Alto" to Color.parseColor("#FFEBEE")
        }
    }

    private fun generarRecomendacion(co2: Double, temp: Double, hum: Double): String {
        val aireOK = co2 <= 800
        val tempOK = temp in 21.0..27.0
        val humOK = hum in 40.0..60.0

        return when {
            aireOK && tempOK && humOK -> "Todo OK. Mantén ventilación natural."
            co2 <= 1200 -> "Ventila 5–10 minutos abriendo puertas/ventanas."
            else -> "Ventila urgentemente la zona (apertura total o extracción)."
        }
    }
}
