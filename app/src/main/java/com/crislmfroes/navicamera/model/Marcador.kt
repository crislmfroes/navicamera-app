package com.crislmfroes.navicamera.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.sinh

@Entity(tableName = "marcador_table")
data class Marcador(
    @ColumnInfo(name = "nome") val nome : String,
    @ColumnInfo(name = "descricao") val descricao : String,
    @PrimaryKey @ColumnInfo(name = "cod") val cod : Int
) {

    var x : Double = 0.0
    var y : Double = 0.0
    var z : Double = 0.0
    var distancia : Double = 0.0
    var rotacao : Double = 0.0
    var distThresh = 0.5

    /*override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other !is Marcador) {
            return false
        } else {
            if ((other as Marcador).cod != cod) {
                return false
            }
            if (distanceToOther(other) > distThresh) {
                return false
            }
        }
        return true
    }*/



    /*fun distanceToOther(other : Marcador) : Double {
        return Math.sqrt(Math.pow(other.x - x, 2.0) + Math.pow(other.y - y, 2.0) + Math.pow(other.z - z, 2.0))
    }

    fun distanceToCamera() : Double {
        return Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0) + Math.pow(z, 2.0))
    }

    fun rotacao() : Double {
        return sinh((x/distanceToCamera())*180/Math.PI)
    }*/

    override fun hashCode(): Int {
        var result = nome.hashCode()
        result = 31 * result + descricao.hashCode()
        result = 31 * result + cod
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Marcador

        if (nome != other.nome) return false
        if (descricao != other.descricao) return false
        if (cod != other.cod) return false

        return true
    }

}