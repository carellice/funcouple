package com.funcouple.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup per cambiare telefono: tutti i dati dell'app stanno nelle preferenze "funcouple",
 * che qui vengono copiate così come sono in un file JSON, ognuna con il suo tipo.
 */
object Backup {
    private const val FORMAT = "funcouple-backup"
    private const val VERSION = 1

    fun export(context: Context, uri: Uri): Boolean = runCatching {
        val data = JSONObject()
        prefs(context).all.forEach { (key, value) ->
            val entry = when (value) {
                is Boolean -> JSONObject().put("type", "boolean").put("value", value)
                is Int -> JSONObject().put("type", "int").put("value", value)
                is Long -> JSONObject().put("type", "long").put("value", value)
                is Float -> JSONObject().put("type", "float").put("value", value.toDouble())
                is String -> JSONObject().put("type", "string").put("value", value)
                is Set<*> -> JSONObject().put("type", "set").put("value", JSONArray(value.map { it.toString() }))
                else -> null
            }
            if (entry != null) data.put(key, entry)
        }
        val json = JSONObject().put("format", FORMAT).put("version", VERSION).put("data", data)
        context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toString(2).toByteArray()) }
    }.isSuccess

    /** Sostituisce tutti i dati con quelli del file; false se il file non è un backup valido. */
    fun import(context: Context, uri: Uri): Boolean = runCatching {
        val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        val json = JSONObject(text)
        require(json.optString("format") == FORMAT && json.optInt("version") in 1..VERSION)
        val data = json.getJSONObject("data")
        // Si legge tutto il file prima di toccare i dati attuali: se è rovinato non si perde nulla.
        val editor = prefs(context).edit().clear()
        data.keys().forEach { key ->
            val entry = data.getJSONObject(key)
            when (entry.getString("type")) {
                "boolean" -> editor.putBoolean(key, entry.getBoolean("value"))
                "int" -> editor.putInt(key, entry.getInt("value"))
                "long" -> editor.putLong(key, entry.getLong("value"))
                "float" -> editor.putFloat(key, entry.getDouble("value").toFloat())
                "string" -> editor.putString(key, entry.getString("value"))
                "set" -> entry.getJSONArray("value").let { array -> editor.putStringSet(key, List(array.length()) { array.getString(it) }.toSet()) }
                else -> error("tipo sconosciuto")
            }
        }
        check(editor.commit())
        TogetherWidget.refresh(context)
    }.isSuccess

    private fun prefs(context: Context) = context.getSharedPreferences("funcouple", Context.MODE_PRIVATE)
}
