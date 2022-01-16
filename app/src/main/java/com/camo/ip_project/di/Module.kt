package com.camo.ip_project.di

import android.content.Context
import androidx.room.Room
import com.camo.ip_project.database.Repository
import com.camo.ip_project.database.local.LocalAppDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Module {
    @Provides
    @Singleton
    fun getAppDb(@ApplicationContext context: Context): LocalAppDb = Room.databaseBuilder(
        context.applicationContext,
        LocalAppDb::class.java,
        "appDB.db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun getRepo(@ApplicationContext context: Context): Repository = Repository(getAppDb(context))

}