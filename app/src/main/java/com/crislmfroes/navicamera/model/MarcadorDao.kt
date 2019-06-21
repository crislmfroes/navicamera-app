package com.crislmfroes.navicamera.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MarcadorDao {
    @Query("SELECT * FROM marcador_table ORDER BY cod ASC")
    suspend fun getAllMarcadores() : List<Marcador>

    @Query("SELECT * FROM marcador_table WHERE cod = :cod")
    fun getMarcador(cod : Int) : Marcador?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(marcador : Marcador)

    @Query("DELETE FROM marcador_table")
    fun deleteAll()
}