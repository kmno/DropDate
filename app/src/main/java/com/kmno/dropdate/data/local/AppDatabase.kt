package com.kmno.dropdate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kmno.dropdate.data.local.dao.ReleaseDao
import com.kmno.dropdate.data.local.entity.ReleaseEntity

@Database(entities = [ReleaseEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun releaseDao(): ReleaseDao
}
