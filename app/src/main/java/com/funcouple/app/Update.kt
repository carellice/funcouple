package com.funcouple.app

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Aggiornamenti dalle Releases di GitHub: l'app va in rete solo quando lo si chiede da qui. */
class Updater(private val context: Context) {
    sealed interface Status {
        data object Idle : Status
        data object Checking : Status
        data object UpToDate : Status
        data class Available(val version: String, val url: String) : Status
        data class Downloading(val version: String, val progress: Float) : Status
        data class Ready(val version: String, val file: File) : Status
        data class Failed(val message: String) : Status
    }

    var status by mutableStateOf<Status>(Status.Idle)
        private set

    val currentVersion: String = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 20_000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "FunCouple")
    }

    suspend fun check() {
        status = Status.Checking
        status = runCatching {
            withContext(Dispatchers.IO) {
                val connection = open(LATEST_RELEASE)
                try {
                    val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val version = json.getString("tag_name").removePrefix("v")
                    val assets = json.getJSONArray("assets")
                    val apk = (0 until assets.length()).map(assets::getJSONObject)
                        .firstOrNull { it.getString("name").endsWith(".apk") }
                        ?.getString("browser_download_url")
                    // Si scarica solo dalla repository ufficiale.
                    if (apk != null && apk.startsWith(DOWNLOAD_PREFIX) && isNewer(version, currentVersion)) {
                        Status.Available(version, apk)
                    } else {
                        Status.UpToDate
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }.getOrElse { Status.Failed("Controllo non riuscito: verifica la connessione") }
    }

    suspend fun download(update: Status.Available) {
        status = Status.Downloading(update.version, 0f)
        status = runCatching {
            withContext(Dispatchers.IO) {
                val folder = File(context.cacheDir, "updates").apply {
                    deleteRecursively()
                    mkdirs()
                }
                val file = File(folder, "FunCouple-${update.version}.apk")
                val connection = open(update.url)
                try {
                    val total = connection.contentLengthLong
                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var done = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                done += read
                                if (total > 0) status = Status.Downloading(update.version, done / total.toFloat())
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
                Status.Ready(update.version, file)
            }
        }.getOrElse { Status.Failed("Download non riuscito: riprova") }
        (status as? Status.Ready)?.let(::install)
    }

    /** Passa l'APK all'installatore di Android, che chiede conferma (e verifica la firma). */
    fun install(ready: Status.Ready) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", ready.file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    companion object {
        private const val LATEST_RELEASE = "https://api.github.com/repos/carellice/funcouple/releases/latest"
        private const val DOWNLOAD_PREFIX = "https://github.com/carellice/funcouple/releases/download/"

        fun isNewer(candidate: String, current: String): Boolean {
            val a = candidate.split(".").map { it.toIntOrNull() ?: 0 }
            val b = current.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
                if (diff != 0) return diff > 0
            }
            return false
        }
    }
}
