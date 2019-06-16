package com.crislmfroes.navicamera.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "marcador_table")
data class Marcador(
    @ColumnInfo(name = "nome") val nome : String,
    @ColumnInfo(name = "descricao") val descricao : String,
    @PrimaryKey @ColumnInfo(name = "cod") val cod : Int
) {

    var distancia : Float = 0.0f
    var rotacao : Float = 0.0f

}