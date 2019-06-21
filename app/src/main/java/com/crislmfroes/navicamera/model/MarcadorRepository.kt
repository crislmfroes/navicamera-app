package com.crislmfroes.navicamera.model

import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MarcadorRepository(private val marcadorDao: MarcadorDao, private val marcadorRemote: MarcadorRemote, private val dicionarioDao: DicionarioDao) {

    private val TAG = "MarcadorRepository"

    @WorkerThread
    suspend fun insertMarcador(marcador : Marcador) {
        Log.i(TAG, "Inserindo marcador ...")
        marcadorDao.insert(marcador)
    }

    @WorkerThread
    suspend fun insertDicionario(dicionario: Dicionario) {
        dicionarioDao.insert(dicionario)
    }

    @WorkerThread
    suspend fun getAllMarcador() : List<Marcador> {
        return marcadorDao.getAllMarcadores()
    }

    fun getAllMarcadorRemote() : MutableLiveData<List<Marcador>> {
        return marcadorRemote.getAllMarcadores()
    }

    @WorkerThread
    suspend fun getFirstDicionario() : Dicionario? {
        return dicionarioDao.getFirst()
    }

}