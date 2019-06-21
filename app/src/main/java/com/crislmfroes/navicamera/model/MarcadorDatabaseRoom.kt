package com.crislmfroes.navicamera.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomMasterTable.TABLE_NAME
import androidx.room.TypeConverter
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Marcador::class, Dicionario::class], version = 2, exportSchema = true)
public abstract class MarcadorDatabaseRoom : RoomDatabase() {
    abstract fun marcadorDao() : MarcadorDao
    abstract fun dicionarioDao() : DicionarioDao

    companion object {
        @Volatile
        private var INSTANCE : MarcadorDatabaseRoom? = null

        fun getDatabase(context : Context, scope: CoroutineScope) : MarcadorDatabaseRoom {
            val tmpInstance = INSTANCE
            if (tmpInstance != null) {
                return tmpInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    MarcadorDatabaseRoom::class.java,
                    "Marcador_database"
                )
                    .addMigrations(DatabaseMigration())
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }

    private class DatabaseMigration : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `dicionario_table` (`cod` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `markerSize` INTEGER NOT NULL, `nMarkers` INTEGER NOT NULL, `maxCorrectionBits` INTEGER NOT NULL, `bytesList` BLOB NOT NULL)")
        }
    }

    private class DatabaseCallback(private val scope : CoroutineScope) : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let {
                scope.launch(Dispatchers.IO) {
                    populateDatabase(it.marcadorDao())
                }
            }
        }
        fun populateDatabase(dao : MarcadorDao) {

        }
    }
}