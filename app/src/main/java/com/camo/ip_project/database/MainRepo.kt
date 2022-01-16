package com.camo.ip_project.database

import com.camo.ip_project.database.local.LocalAppDb
import com.camo.ip_project.database.local.model.Coin
import javax.inject.Inject

class Repository @Inject constructor(private val db: LocalAppDb) {

    suspend fun addCoins(coins: ArrayList<Coin>) {
        db.coinDao().addCoins(coins)
    }
}