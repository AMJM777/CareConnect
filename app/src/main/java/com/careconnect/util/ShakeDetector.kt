package com.careconnect.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

// Rileva lo scuotimento del telefono tramite l'accelerometro ,il sensore lavora solo quando la schermata e' visibile.
class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometro: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // istanti (ms) degli "strattoni" recenti sopra soglia: servono a distinguere
    // uno scuotimento voluto (piu' strattoni ravvicinati) da un urto singolo
    private val strattoni = ArrayDeque<Long>()

    // evita di far ripartire l'SOS subito dopo un rilevamento
    private var ultimoTrigger = 0L

    // registra il listener: da chiamare in onResume()
    fun avvia() {
        accelerometro?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    // rimuove il listener: da chiamare in onPause() (risparmia batteria)
    fun ferma() {
        sensorManager.unregisterListener(this)
        strattoni.clear()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // forza dell'accelerazione in "g", tolta la gravita' terrestre:
        // ~1 da fermo, cresce quando il telefono viene mosso con forza
        val forzaG = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

        val ora = System.currentTimeMillis()

        if (forzaG > SOGLIA_G) {
            // ignora strattoni troppo ravvicinati (rimbalzi dello stesso movimento)
            if (strattoni.isNotEmpty() && ora - strattoni.last() < INTERVALLO_MIN_MS) return

            strattoni.addLast(ora)

            // tiene solo gli strattoni dentro la finestra temporale
            while (strattoni.isNotEmpty() && ora - strattoni.first() > FINESTRA_MS) {
                strattoni.removeFirst()
            }

            // abbastanza strattoni ravvicinati + passato il cooldown -> scuotimento voluto
            if (strattoni.size >= STRATTONI_RICHIESTI && ora - ultimoTrigger > COOLDOWN_MS) {
                ultimoTrigger = ora
                strattoni.clear()
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* non serve */ }

    private companion object {
        // forza minima (in g) perche' un movimento conti come "strattone"
        const val SOGLIA_G = 2.7f
        // quanti strattoni servono dentro la finestra per far scattare l'SOS
        const val STRATTONI_RICHIESTI = 3
        // finestra temporale entro cui contare gli strattoni
        const val FINESTRA_MS = 1500L
        // distanza minima tra due strattoni (scarta i rimbalzi dello stesso colpo)
        const val INTERVALLO_MIN_MS = 100L
        // pausa dopo un rilevamento, per non ripartire subito
        const val COOLDOWN_MS = 3000L
    }
}