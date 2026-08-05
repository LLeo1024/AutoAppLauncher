package com.leo.autoapplaucher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 节假日缓存实体
 * 每条记录缓存某一年的节假日数据
 * 数据来源: http://tool.bitefu.net/jiari/?d=YYYY
 *
 * jsonData 格式: {"0101":2,"0102":1,...}
 * 值含义: 0=工作日, 1=周末/调休, 2=法定节假日
 * 不在 JSON 中的日期默认为工作日(0)
 */
@Entity(tableName = "holiday_cache")
data class HolidayEntity(
    @PrimaryKey
    val year: Int,
    val jsonData: String,
    val updatedAt: Long = System.currentTimeMillis()
)
