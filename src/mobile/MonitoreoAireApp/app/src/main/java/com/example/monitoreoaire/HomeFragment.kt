package com.example.monitoreoaire

import android.content.Intent // ESTA ES VITAL
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class HomeFragment : Fragment() {

    private lateinit var spDispositivos: Spinner
    private lateinit var btnAyuda: FloatingActionButton // NUEVO
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
    private val idDispositivo: Int
        get() = (activity as? HomeActivity)?.idDispositivo ?: 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requestQueue = Volley.newRequestQueue(context.applicationContext)
        MqttManager.init(context)
        MqttManager.setOnMessageListener { payload ->
            try {
                val obj = JSONObject(payload)
                val tokenMsg = obj.optString("token", "")
                val tokenSel = selectedToken ?: ""

                if (tokenSel.isNotEmpty() && tokenMsg.isNotEmpty() && tokenMsg != tokenSel) return@setOnMessageListener

                val co2 = obj.optDouble("co2", 0.0)
                val temp = obj.optDouble("temperatura", 0.0)
                val hum = obj.optDouble("humedad", 0.0)
                val indiceESP32 = obj.optDouble("calidad_aire_indice", 100.0).toInt()

                val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                lastMqttAt = System.currentTimeMillis()

                activity?.runOnUiThread {
                    actualizarDashboard(co2, temp, hum, "Tiempo real $hora", indiceESP32)
                }
            } catch (_: Exception) {}
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_home, container, false)
        spDispositivos = v.findViewById(R.id.spDispositivosHome)
        btnAyuda = v.findViewById(R.id.btnAyudaHome) // NUEVO

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

        // Lógica para abrir la nueva pantalla de Ayuda
        btnAyuda.setOnClickListener {
            val intent = Intent(requireContext(), AyudaActivity::class.java)
            startActivity(intent)
        }

        ponerEstadoEsperando()
        cargarDispositivos()

        spDispositivos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos !in tokensDispositivos.indices) return

                MqttManager.clearAllSubscriptions()

                val idSel = idsDispositivos[pos]
                (activity as? HomeActivity)?.setDispositivoSeleccionado(idSel)
                selectedToken = tokensDispositivos[pos]
                lastMqttAt = 0L

                ponerEstadoEsperando()

                selectedToken?.let { if (it.isNotEmpty()) MqttManager.subscribeToDevice(it) }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {
                (activity as? HomeActivity)?.setDispositivoSeleccionado(0)
            }
        }
        return v
    }

    override fun onResume() {
        super.onResume()
        startWatchdog()
        selectedToken?.let { if (it.isNotEmpty()) MqttManager.subscribeToDevice(it) }
    }

    override fun onPause() { super.onPause(); stopWatchdog() }

    private fun startWatchdog() {
        if (watchdogRunnable != null) return
        watchdogRunnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                if (selectedToken.isNullOrEmpty()) { watchdogHandler.postDelayed(this, 1000); return }
                if (lastMqttAt == 0L) marcarSinSenal("Esperando datos (?)")
                else if (now - lastMqttAt > MQTT_TIMEOUT_MS) marcarSinSenal("Sin señal")
                watchdogHandler.postDelayed(this, 1000)
            }
        }
        watchdogHandler.post(watchdogRunnable!!)
    }

    private fun stopWatchdog() { watchdogRunnable?.let { watchdogHandler.removeCallbacks(it) }; watchdogRunnable = null }

    private fun ponerEstadoEsperando() {
        txtEstado.text = "Sincronizando..."
        txtUltimaLectura.text = "Esperando lectura..."
        txtCO2.text = "--"; txtTemp.text = "--"; txtHum.text = "--"
        txtIndiceCalidad.text = "--"; txtIndiceNivel.text = "--"
        txtRiesgoSee.text = "--"
        txtRecomendacion.text = "Conectando con el sensor..."
        viewCircleEstado.setBackgroundResource(R.drawable.bg_circle_warn)
    }

    private fun marcarSinSenal(m: String) {
        txtEstado.text = m; txtUltimaLectura.text = "Offline"
        viewCircleEstado.setBackgroundResource(R.drawable.bg_circle_warn)
    }

    private fun cargarDispositivos() {
        if (idUsuario == 0) return
        val rol = Session.getRole(requireContext())
        val idC = if (rol == "ADMIN" || rol == "SUPERVISOR") 0 else idUsuario
        val url = ApiConfig.BASE_URL + "listar_dispositivos.php?id_usuario=$idC&ts=${System.currentTimeMillis()}"
        val request = object : JsonArrayRequest(Request.Method.GET, url, null, { response ->
            nombresDispositivos.clear(); idsDispositivos.clear(); tokensDispositivos.clear()
            for (i in 0 until response.length()) {
                val obj = response.getJSONObject(i)
                idsDispositivos.add(obj.getInt("id"))
                tokensDispositivos.add(obj.optString("token_dispositivo", ""))
                val label = if (obj.optString("ubicacion", "").isNotEmpty()) "${obj.getString("nombre_dispositivo")} - ${obj.getString("ubicacion")}" else obj.getString("nombre_dispositivo")
                nombresDispositivos.add(label)
            }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nombresDispositivos)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spDispositivos.adapter = adapter

            if (idsDispositivos.isNotEmpty()) {
                (activity as? HomeActivity)?.setDispositivoSeleccionado(idsDispositivos[0])
                MqttManager.clearAllSubscriptions()
                selectedToken = tokensDispositivos[0]
                selectedToken?.let { if (it.isNotEmpty()) MqttManager.subscribeToDevice(it) }
            }
        }, { }) { override fun getHeaders() = Session.authHeaders(requireContext()) }
        requestQueue.add(request)
    }

    private fun actualizarDashboard(co2: Double, temp: Double, hum: Double, fecha: String, indiceReal: Int) {
        txtCO2.text = "${co2.roundToInt()} ppm"
        txtTemp.text = formatearTempConUnidad(temp)
        txtHum.text = "${hum.roundToInt()} %"
        txtUltimaLectura.text = "Lectura: $fecha"
        txtIndiceCalidad.text = "$indiceReal / 100"

        val (nivel, bgRes) = when {
            indiceReal >= 85 -> "Excelente" to R.drawable.bg_circle_good
            indiceReal >= 70 -> "Buena" to R.drawable.bg_circle_good
            indiceReal >= 50 -> "Moderada" to R.drawable.bg_circle_warn
            else -> "Crítica" to R.drawable.bg_circle_bad
        }
        txtEstado.text = nivel
        txtIndiceNivel.text = nivel
        viewCircleEstado.setBackgroundResource(bgRes)

        txtTempConfort.text = when {
            temp < 20 -> "Frío"
            temp <= 26 -> "Confortable"
            else -> "Caluroso"
        }
        txtHumConfort.text = when {
            hum < 30 -> "Muy Seco"
            hum <= 60 -> "Óptimo"
            else -> "Húmedo"
        }
        txtCO2Objetivo.text = if (co2 <= 800) "Seguro (<800 ppm)" else "Alerta (>800 ppm)"

        val (nivelSee, colorSee) = calcularRiesgoSalud(co2, temp, hum)
        txtRiesgoSee.text = nivelSee
        cardRiesgoSee.setCardBackgroundColor(colorSee)
        txtRecomendacion.text = generarRecomendacionTecnica(co2, temp, hum)
    }

    private fun formatearTempConUnidad(t: Double): String {
        val p = requireContext().getSharedPreferences("app_config", Context.MODE_PRIVATE)
        val u = p.getString(if (idDispositivo > 0) "unidad_temp_$idDispositivo" else "unidad_temp", "C") ?: "C"
        return if (u == "F") "${(t * 9.0 / 5.0 + 32.0).roundToInt()} °F" else "${t.roundToInt()} °C"
    }

    private fun calcularRiesgoSalud(co2: Double, t: Double, h: Double): Pair<String, Int> {
        var s = 0
        if (co2 > 1000) s += 2 else if (co2 > 800) s += 1
        if (t < 18 || t > 28) s += 1
        if (h < 30 || h > 70) s += 1
        return when {
            s == 0 -> "Bajo" to Color.parseColor("#E8F5E9")
            s <= 2 -> "Medio" to Color.parseColor("#FFFDE7")
            else -> "Alto" to Color.parseColor("#FFEBEE")
        }
    }

    private fun generarRecomendacionTecnica(co2: Double, t: Double, h: Double): String {
        return when {
            co2 > 1500 -> "Crítico: Active ventilación forzada o evacue el área hasta renovar el aire."
            co2 > 1000 -> "Aire deficiente: Incremente la ventilación abriendo puertas y ventanas totalmente."
            co2 > 800 -> "Moderado: Realice una apertura parcial de ventanas para renovar el flujo de aire."
            h > 70 -> "Humedad Crítica: Use deshumidificadores o aire acondicionado para reducir riesgo de moho."
            h < 30 -> "Humedad Muy Baja: Use un humidificador o coloque recipientes con agua para evitar sequedad."
            t > 30 -> "Calor Excesivo: Reduzca la carga térmica; use ventiladores o sistemas de refrigeración."
            t < 16 -> "Frío Intenso: Incremente la calefacción o mejore el aislamiento térmico del recinto."
            else -> "Condiciones Óptimas: Calidad de aire estable. No se requieren acciones correctivas."
        }
    }
}