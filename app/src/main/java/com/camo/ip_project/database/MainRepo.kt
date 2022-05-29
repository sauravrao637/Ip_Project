/****************************************************************************************
 * Copyright (c) 2022 Saurav Rao <sauravrao637@gmail.com>                               *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.camo.ip_project.database

import androidx.lifecycle.LiveData
import com.camo.ip_project.database.local.LocalAppDb
import com.camo.ip_project.database.local.model.UserHRV
import javax.inject.Inject

class Repository @Inject constructor(private val db: LocalAppDb) {
    suspend fun addData(data: UserHRV): Long = db.userHrvDao().addData(data)

    fun getData(): LiveData<List<UserHRV>> = db.userHrvDao().getData()
}