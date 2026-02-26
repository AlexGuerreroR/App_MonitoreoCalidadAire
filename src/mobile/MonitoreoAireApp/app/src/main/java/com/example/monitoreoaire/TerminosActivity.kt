package com.example.monitoreoaire

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TerminosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminos)

        val txtContenido = findViewById<TextView>(R.id.txtContenidoTerminos)
        val btnCerrar = findViewById<Button>(R.id.btnCerrarTerminos)

        val textoLegal = """
            TÉRMINOS Y CONDICIONES DE USO
            
            • Propósito: El software "Monitoreo de Aire" es un prototipo educativo/experimental diseñado para la visualización de datos de sensores CO2, temperatura y humedad.
            
            • Precisión: Los valores son estimaciones basadas en sensores de bajo costo (MQ135 y DHT11); no deben usarse para decisiones médicas o de seguridad industrial.
            
            • Responsabilidad: Las recomendaciones son sugerencias automáticas basadas en estándares ASHRAE e ISO; la ejecución es responsabilidad exclusiva del usuario.
            
            POLÍTICAS DE SEGURIDAD Y PRIVACIDAD
            
            • Resguardo: El sistema exige contraseñas seguras (mínimo 8 caracteres, números y símbolos) para mitigar accesos no autorizados.
            
            • Tratamiento de Datos: Los datos se almacenan en un servidor privado bajo un modelo de aislamiento multi-empresa, garantizando privacidad por infraestructura.
            
            • Alertas: Al registrarse, el usuario autoriza el envío de correos automáticos cuando los sensores detecten niveles fuera de los rangos saludables.
            
            • Gestión de Sesiones Seguras: El uso de tokens de API y preferencias compartidas cifradas en Android asegura que la sesión del usuario sea persistente y protegida contra el secuestro de sesiones.
            
            • Derecho de Acceso: El usuario tiene derecho a modificar sus datos o eliminar su cuenta a través del administrador asignado.
        """.trimIndent()

        txtContenido.text = textoLegal

        btnCerrar.setOnClickListener {
            finish() // Regresa a la pantalla anterior
        }
    }
}