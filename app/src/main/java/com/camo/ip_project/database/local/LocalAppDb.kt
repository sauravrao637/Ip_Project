package com.camo.ip_project.database.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.camo.ip_project.database.local.dao.UserHRVDao
import com.camo.ip_project.database.local.model.UserHRV


// Increase version every time you make changes to room database structure
@Database(entities = [UserHRV::class], version = 3)
//@TypeConverters(DateTypeConverter::class)
abstract class LocalAppDb : RoomDatabase() {
    abstract fun userHrvDao(): UserHRVDao
}