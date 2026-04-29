package com.kmno.dropdate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kmno.dropdate.data.local.dao.ReleaseDao
import com.kmno.dropdate.data.local.entity.ReleaseEntity
import com.kmno.dropdate.data.local.entity.TrackedSeriesEntity

@Database(
    entities = [ReleaseEntity::class, TrackedSeriesEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun releaseDao(): ReleaseDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE releases ADD COLUMN seriesId TEXT NOT NULL DEFAULT ''",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `tracked_series` " +
                            "(`seriesId` TEXT NOT NULL, PRIMARY KEY(`seriesId`))",
                    )
                }
            }
    }
}
