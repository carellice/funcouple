package com.funcouple.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.runtime.staticCompositionLocalOf

/** Effetti sonori: i file sono generati da tools/gen_sounds.py. */
enum class Sfx(val res: Int) {
    TAP(R.raw.sfx_tap),
    FLIP(R.raw.sfx_flip),
    SUCCESS(R.raw.sfx_success),
    WIN(R.raw.sfx_win),
    LEVEL(R.raw.sfx_level),
    DICE(R.raw.sfx_dice),
    TICK(R.raw.sfx_tick),
    BELL(R.raw.sfx_bell),
    ERROR(R.raw.sfx_error),
}

class Sounds(context: Context, private val enabled: () -> Boolean, private val soft: () -> Boolean) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = Sfx.entries.associateWith { pool.load(context, it.res, 1) }

    /** Con "Luci basse" attivo i suoni si abbassano insieme allo schermo. */
    fun play(sfx: Sfx) {
        if (!enabled()) return
        val volume = if (soft()) 0.4f else 1f
        pool.play(ids.getValue(sfx), volume, volume, 1, 0, 1f)
    }
}

val LocalSounds = staticCompositionLocalOf<Sounds?> { null }

/** Sblocco con impronta o volto tramite il sistema di Android; il PIN resta sempre come riserva. */
object Biometrics {
    fun available(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            context.getSystemService(BiometricManager::class.java)
                ?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q ->
            @Suppress("DEPRECATION")
            context.getSystemService(BiometricManager::class.java)?.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS

        Build.VERSION.SDK_INT == Build.VERSION_CODES.P ->
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)

        else -> false
    }

    fun prompt(context: Context, title: String, onSuccess: () -> Unit) {
        val activity = context.findActivity() ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val executor = activity.mainExecutor
        BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle("Usa l'impronta o il volto")
            .setNegativeButton("Usa il PIN", executor) { _, _ -> }
            .build()
            .authenticate(
                CancellationSignal(),
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onSuccess()
                },
            )
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
