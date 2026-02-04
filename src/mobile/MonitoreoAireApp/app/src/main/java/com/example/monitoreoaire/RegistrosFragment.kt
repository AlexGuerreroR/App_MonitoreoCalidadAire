package com.example.monitoreoaire

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.monitoreoaire.ApiConfig.BASE_URL
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegistrosFragment : Fragment() {

    private lateinit var lvRegistros: ListView
    private lateinit var btnExportar: Button
    private lateinit var btnLimpiar: Button
    private lateinit var txtResumenRegistros: TextView
    private lateinit var txtFechaSeleccionada: TextView
    private lateinit var btnCambiarFecha: Button

    private val listaEventos = ArrayList<EventoRegistro>()
    private lateinit var adapter: EventosAdapter

    private val formatoServidor = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val formatoMostrar = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var fechaSeleccionada: String =
        formatoServidor.format(Calendar.getInstance().time)

    private val idUsuario: Int
        get() = (activity as? HomeActivity)?.idUsuario ?: 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val v = inflater.inflate(R.layout.fragment_registros, container, false)

        lvRegistros = v.findViewById(R.id.lvRegistros)
        btnExportar = v.findViewById(R.id.btnExportar)
        btnLimpiar = v.findViewById(R.id.btnLimpiar)
        txtResumenRegistros = v.findViewById(R.id.txtResumenRegistros)
        txtFechaSeleccionada = v.findViewById(R.id.txtFechaSeleccionada)
        btnCambiarFecha = v.findViewById(R.id.btnCambiarFecha)

        adapter = EventosAdapter(requireContext(), listaEventos)
        lvRegistros.adapter = adapter

        actualizarResumen()

        btnCambiarFecha.setOnClickListener {
            mostrarDatePicker()
        }

        btnExportar.setOnClickListener {
            exportarEventos()
        }

        btnLimpiar.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Limpiar lista")
                .setMessage(
                    "Esto solo limpia la lista en la app, " +
                            "los datos quedan en la base.\n\n¿Continuar?"
                )
                .setPositiveButton("Sí") { _, _ ->
                    listaEventos.clear()
                    adapter.notifyDataSetChanged()
                    actualizarResumen()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Cargar eventos del día seleccionado (hoy al inicio)
        cargarEventos()

        return v
    }

    // ================== UI helpers ==================
    private fun actualizarResumen() {
        val fechaMostrar = try {
            val date = formatoServidor.parse(fechaSeleccionada)
            if (date != null) formatoMostrar.format(date) else fechaSeleccionada
        } catch (e: Exception) {
            fechaSeleccionada
        }

        txtFechaSeleccionada.text = "Fecha: $fechaMostrar"
        txtResumenRegistros.text =
            "Fecha: $fechaMostrar · Total eventos: ${listaEventos.size}"
    }

    private fun mostrarDatePicker() {
        val cal = Calendar.getInstance()
        try {
            val date = formatoServidor.parse(fechaSeleccionada)
            if (date != null) cal.time = date
        } catch (_: Exception) {
        }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val dp = DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val calSel = Calendar.getInstance()
                calSel.set(y, m, d)
                fechaSeleccionada = formatoServidor.format(calSel.time)
                cargarEventos()
            },
            year, month, day
        )
        dp.show()
    }

    // ================== Cargar eventos desde PHP ==================
    private fun cargarEventos() {
        if (idUsuario <= 0) {
            Toast.makeText(
                requireContext(),
                "No se encontró id de usuario",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val url = BASE_URL + "listar_eventos.php"

        val queue = Volley.newRequestQueue(requireContext())
        val request = object : StringRequest(
            Request.Method.POST,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val ok = json.optBoolean("success", false)

                    if (!ok) {
                        Toast.makeText(
                            requireContext(),
                            json.optString("message", "Error al obtener eventos"),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val arr = json.getJSONArray("eventos")
                        listaEventos.clear()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val titulo = obj.optString("titulo")
                            val detalle = obj.optString("detalle")
                            val fechaHora = obj.optString("fecha_hora")
                            listaEventos.add(
                                EventoRegistro(
                                    titulo = titulo,
                                    detalle = detalle,
                                    fechaHora = fechaHora
                                )
                            )
                        }
                        adapter.notifyDataSetChanged()
                        actualizarResumen()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Error procesando datos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error de red al cargar eventos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        ){
            override fun getHeaders(): MutableMap<String, String> {
                return Session.authHeaders(requireContext())
            }

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                val rol = Session.getRole(requireContext())
            val idConsulta = if (rol == "ADMIN" || rol == "SUPERVISOR") 0 else idUsuario
            params["id_usuario"] = idConsulta.toString()
                params["fecha"] = fechaSeleccionada   // yyyy-MM-dd
                return params
            }
        }

        queue.add(request)
    }

    // ================== Exportar CSV usando DownloadManager ==================
    private fun exportarEventos() {
        if (idUsuario <= 0) {
            Toast.makeText(
                requireContext(),
                "No se encontró id de usuario",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val urlCompleta =
            BASE_URL + "exportar_eventos.php?id_usuario=$idUsuario&fecha=$fechaSeleccionada&api_token=${Session.getToken(requireContext())}"

        try {
            val fileName = "eventos_$fechaSeleccionada.csv"

            val request = DownloadManager.Request(Uri.parse(urlCompleta))
                .setTitle(fileName)
                .setDescription("Descargando reporte de eventos")
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalFilesDir(
                    requireContext(),
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = requireContext()
                .getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            Toast.makeText(
                requireContext(),
                "Descarga iniciada...",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Error iniciando descarga: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ================== Clases internas ==================
    data class EventoRegistro(
        val titulo: String,
        val detalle: String,
        val fechaHora: String
    )

    class EventosAdapter(
        context: Context,
        private val eventos: List<EventoRegistro>
    ) : ArrayAdapter<EventoRegistro>(context, 0, eventos) {

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_registro, parent, false)

            val evento = eventos[position]

            val txtTitulo = view.findViewById<TextView>(R.id.txtEventoTitulo)
            val txtDetalle = view.findViewById<TextView>(R.id.txtEventoDetalle)
            val txtFecha = view.findViewById<TextView>(R.id.txtEventoFecha)

            txtTitulo.text = evento.titulo
            txtDetalle.text = evento.detalle
            txtFecha.text = evento.fechaHora

            return view
        }
    }
}
