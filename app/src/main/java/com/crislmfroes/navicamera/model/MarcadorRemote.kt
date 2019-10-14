package com.crislmfroes.navicamera.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MarcadorRemote {
    private val client = OkHttpClient()
    private val TAG = "MarcadorRemote"

    fun getAllMarcadores() : MutableLiveData<List<Marcador>> {
        var marcadores = listOf<Marcador>()
        val liveData = MutableLiveData<List<Marcador>>()
        try {
            val request = Request.Builder()
                .url("https://navicamera-web.herokuapp.com/api/marcadores")
                .build()
            val response = client.newCall(request).execute()
            //Log.i(TAG, "Response: %s".format(response.body!!.string()))
            marcadores = Klaxon().parseArray(response.body().byteStream())!!
            /*for (json in jsonArray!!) {
                val marcador = Marcador(
                    nome = json["nome"] as String,
                    descricao = json["descricao"] as String,
                    cod = json["cod"] as Int
                )
                marcadores.add(marcador)
            }*/
            Log.i(TAG, marcadores.toString())
        } catch (e : IOException) {
            Log.e(TAG, "Erro ao conectar com a api ...", e)
        }
        liveData.value = marcadores
        return liveData
    }
}