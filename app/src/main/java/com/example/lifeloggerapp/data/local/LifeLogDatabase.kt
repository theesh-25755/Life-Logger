package com.example.lifeloggerapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        EntryEntity::class,
        TagEntity::class,
        MediaEntity::class,
        PendingOperationEntity::class,
        PendingMediaOperationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LifeLogDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao
    abstract fun tagDao(): TagDao
    abstract fun mediaDao(): MediaDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun pendingMediaOperationDao(): PendingMediaOperationDao

    companion object {
        @Volatile
        private var INSTANCE: LifeLogDatabase? = null

        fun getInstance(context: Context): LifeLogDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    LifeLogDatabase::class.java,
                    "lifelog_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}