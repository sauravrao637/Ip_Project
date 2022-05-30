/*****************************************************************************************
 * Copyright <2022> <Saurav Rao> <sauravrao637@gmail.com>                                *
 *                                                                                       *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this  *
 * software and associated documentation files (the "Software"), to deal in the Software *
 * without restriction, including without limitation the rights to use, copy, modify,    *
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to    *
 * permit persons to whom the Software is furnished to do so, subject to the following   *
 * conditions:                                                                           *
 *                                                                                       *
 * The above copyright notice and this permission notice shall be included in all copies *
 * or substantial portions of the Software.                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,   *
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A         *
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT    *
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF  *
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE  *
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                                         *
 *****************************************************************************************/

package com.camo.ip_project.database.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.camo.ip_project.database.local.model.UserHRV

/**
 * This is data access object for hrv analysis result saved on the device. It is responsible for
 * interacting with the UserHRVTable of the room database.
 */
@Dao
interface UserHRVDao {
    /**
     * Adds a row in UserHRV table
     * @param data: HRV analysis data to be inserted
     * @return id of inserted data
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addData(data: UserHRV): Long

    /**
     * Clears all data from the UserHRV table
     */
    @Query("DELETE FROM UserHRV")
    suspend fun deleteAllData()

    /**
     * Get all data stored in the UserHRV table as live data
     */
    @Query("Select * FROM UserHRV")
    fun getData(): LiveData<List<UserHRV>>

    /**
     * Get all data for a particular username stored in UserHRV as live data
     * @param name: username of the patient
     */
    @Query("Select * From UserHRV Where userName like :name ORDER BY id")
    fun getDataFilterByName(name: String): LiveData<List<UserHRV>>
}