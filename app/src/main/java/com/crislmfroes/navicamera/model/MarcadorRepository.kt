package com.crislmfroes.navicamera.model

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import java.io.IOException

class MarcadorRepository(private val marcadorDao: MarcadorDao, private val marcadorRemote: MarcadorRemote) {
    val allMarcadoresRoom = marcadorDao.getAllMarcadores()
    val allMarcadoresRemote = marcadorRemote.getAllMarcadores()

    @WorkerThread
    suspend fun insert(marcador : Marcador) {
        marcadorDao.insert(marcador)
    }

    fun get(cod : Int) : Marcador? {
        return marcadorDao.getMarcador(cod)
    }

}