package com.funcouple.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Inclinazione del telefono, da -1 a 1 sui due assi, per l'effetto di profondità.
 * Il punto neutro segue lentamente la posizione in cui si tiene il telefono, così l'effetto
 * risponde ai movimenti e poi torna da solo al centro.
 */
class TiltSensor(context: Context) : SensorEventListener {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var running = false
    private var primed = false
    private var restX = 0f
    private var restY = 0f

    var x by mutableFloatStateOf(0f)
        private set
    var y by mutableFloatStateOf(0f)
        private set

    fun start() {
        if (sensor == null || running) return
        manager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        running = true
    }

    fun stop() {
        if (!running) return
        manager?.unregisterListener(this)
        running = false
        primed = false
        x = 0f
        y = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        val ax = event.values[0]
        val ay = event.values[1]
        if (!primed) {
            restX = ax
            restY = ay
            primed = true
        }
        restX += (ax - restX) * 0.004f
        restY += (ay - restY) * 0.004f
        // Circa 20 gradi di inclinazione bastano per arrivare a fondo corsa.
        x += (((ax - restX) / 3.5f).coerceIn(-1f, 1f) - x) * 0.15f
        y += (((ay - restY) / 3.5f).coerceIn(-1f, 1f) - y) * 0.15f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

val LocalTilt = staticCompositionLocalOf<TiltSensor?> { null }

/** Inclina l'elemento in 3D seguendo il telefono; [strength] 1 corrisponde a circa 7 gradi. */
fun Modifier.tilt3d(strength: Float = 1f): Modifier = composed {
    val tilt = LocalTilt.current
    graphicsLayer {
        rotationY = -(tilt?.x ?: 0f) * 7f * strength
        rotationX = -(tilt?.y ?: 0f) * 7f * strength
        cameraDistance = 14f * density
    }
}
