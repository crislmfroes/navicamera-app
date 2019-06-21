package com.crislmfroes.navicamera.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DicionarioDao {
    @Query("SELECT * FROM dicionario_table WHERE cod = :cod")
    fun get(cod : Int) : Dicionario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dicionario: Dicionario)

    @Query("SELECT * FROM dicionario_table ORDER BY cod ASC LIMIT 1")
    suspend fun getFirst() : Dicionario?
}