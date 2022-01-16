package com.camo.ip_project.database.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("name"), Index("symbol")])
class Coin( @PrimaryKey
            val id: String,
            val name: String,
            val symbol: String)