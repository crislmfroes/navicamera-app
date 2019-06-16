package com.crislmfroes.navicamera.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MarcadorRemote {
    private val client = OkHttpClient()
    private val TAG = "MarcadorRemote"

    fun getAllMarcadores() : LiveData<List<Marcador>> {
        val marcadores = mutableListOf<Marcador>()
        val liveData = MutableLiveData<List<Marcador>>()
        try {
            val request = Request.Builder()
                .url("https://navicamera-web.herokuapp.com/api/marcadores")
                .build()
            val response = client.newCall(request).execute()
            val parser = Parser.default()
            val jsonArray = parser.parse(response.body!!.charStream()) as JsonArray<JsonObject>
            for (json in jsonArray) {
                val marcador = Marcador(
                    nome = json["nome"] as String,
                    descricao = json["descricao"] as String,
                    cod = json["cod"] as Int
                )
                marcadores.add(marcador)
            }
        } catch (e : IOException) {
            Log.e(TAG, "Erro ao conectar com a api ...", e)
        }
        liveData.value = marcadores
        return liveData
    }
}