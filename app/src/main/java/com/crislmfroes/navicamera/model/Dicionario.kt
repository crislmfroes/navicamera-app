package com.crislmfroes.navicamera.model

import androidx.annotation.Nullable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.opencv.aruco.Aruco
import org.opencv.aruco.Dictionary
import java.util.*

@Entity(tableName = "dicionario_table")
data class Dicionario(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "cod") val cod : Int = 0,
    @ColumnInfo(name = "markerSize") val markerSize : Int,
    @ColumnInfo(name = "nMarkers") val nMarkers : Int,
    @ColumnInfo(name = "maxCorrectionBits") val maxCorretionBits : Int,
    @ColumnInfo(name = "bytesList") val bytesList : ByteArray
    ) {

    @Ignore
    val cvDictionary : Dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_1000)

    init {
        cvDictionary._markerSize = markerSize
        cvDictionary._maxCorrectionBits = maxCorretionBits
        cvDictionary._bytesList.put(0, 0, bytesList)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as Dicionario
        if (!Arrays.equals(bytesList, other.bytesList)) return false
        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(bytesList)
    }

}