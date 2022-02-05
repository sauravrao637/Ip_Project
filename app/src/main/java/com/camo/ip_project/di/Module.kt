package com.camo.ip_project.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
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
    fun provideAppDb(@ApplicationContext context: Context): LocalAppDb = Room.databaseBuilder(
        context.applicationContext,
        LocalAppDb::class.java,
        "appDB.db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideRepo(@ApplicationContext context: Context): Repository = Repository(provideAppDb(context))

    @Singleton
    @Provides
    fun provideSharedPreference(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}