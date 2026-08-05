package com.leo.autoapplaucher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holiday_cache WHERE year = :year")
    suspend fun getByYear(year: Int): HolidayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HolidayEntity)

    @Query("DELETE FROM holiday_cache WHERE year < :year")
    suspend fun deleteOlderThan(year: Int)
}
