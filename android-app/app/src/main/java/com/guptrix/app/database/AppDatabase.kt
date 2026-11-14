package com.guptrix.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [NoteEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, passphrase: ByteArray? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "guptrix_notes.db"
                ).fallbackToDestructiveMigration()

                if (passphrase != null) {
                    val factory = SupportFactory(passphrase)
                    builder.openHelperFactory(factory)
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
