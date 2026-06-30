package com.taleson2wheels.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's local store. Currently just a single key→JSON response cache that
 * backs offline reads; bump `version` and add a migration when entities change.
 */
@Database(entities = [CachedResponse::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cachedResponses(): CachedResponseDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "t2w-cache.db",
            )
                // The cache is disposable — on a schema change, drop and refill
                // from the network rather than ship migrations for cached blobs.
                .fallbackToDestructiveMigration()
                .build()
    }
}
