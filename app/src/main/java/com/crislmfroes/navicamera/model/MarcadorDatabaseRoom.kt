package com.crislmfroes.navicamera.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Marcador::class], version = 1)
public abstract class MarcadorDatabaseRoom : RoomDatabase() {
    abstract fun marcadorDao() : MarcadorDao

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
                ).addCallback(DatabaseCallback(scope)).build()
                INSTANCE = instance
                return instance
            }
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