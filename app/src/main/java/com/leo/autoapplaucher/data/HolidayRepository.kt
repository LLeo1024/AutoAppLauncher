package com.leo.autoapplaucher.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * 节假日数据仓库
 * 负责从 API 获取节假日数据并缓存到 Room 数据库
 *
 * API: http://tool.bitefu.net/jiari/?d=YYYY
 * 返回格式: {"2026":{"0101":2,"0102":1,"0216":2,...}}
 * 值含义: 0=工作日, 1=周末/调休, 2=法定节假日
 * 不在返回列表中的日期默认为工作日(0)
 */
class HolidayRepository(private val context: Context) {

    companion object {
        private const val TAG = "HolidayRepository"
        private const val API_URL = "http://tool.bitefu.net/jiari/?d="
        private const val CONNECT_TIMEOUT = 5000
        private const val READ_TIMEOUT = 5000
    }

    private val holidayDao = AppDatabase.getDatabase(context).holidayDao()

    /**
     * 获取指定日期的类型
     * @return 0=工作日, 1=周末/调休, 2=法定节假日, -1=获取失败
     */
    suspend fun getDayType(year: Int, month: Int, day: Int): Int {
        // 先查缓存
        var cached = holidayDao.getByYear(year)

        // 缓存不存在或超过30天，尝试从网络获取
        if (cached == null || isStale(cached.updatedAt)) {
            val freshData = fetchFromNetwork(year)
            if (freshData != null) {
                val entity = HolidayEntity(year = year, jsonData = freshData)
                holidayDao.insert(entity)
                holidayDao.deleteOlderThan(year - 1)
                cached = entity
            }
        }

        // 从缓存数据中查找
        return cached?.let { parseDayType(it.jsonData, year, month, day) } ?: -1
    }

    /**
     * 从网络获取节假日数据
     * @return JSON 字符串或 null
     */
    private suspend fun fetchFromNetwork(year: Int): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_URL$year")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AutoAppLauncher/1.0")
            }

            conn.inputStream.bufferedReader().use { reader ->
                val response = reader.readText()
                // 验证是否为有效 JSON
                JSONObject(response)
                Log.i(TAG, "成功获取 $year 年节假日数据")
                response
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取节假日数据失败: ${e.message}")
            null
        }
    }

    /**
     * 解析 JSON 数据，获取指定日期的类型
     */
    private fun parseDayType(jsonData: String, year: Int, month: Int, day: Int): Int {
        return try {
            val root = JSONObject(jsonData)
            val yearData = root.optJSONObject(year.toString()) ?: return 0
            val monthDay = String.format("%02d%02d", month, day)
            yearData.optInt(monthDay, 0) // 默认0=工作日
        } catch (e: Exception) {
            Log.e(TAG, "解析节假日数据失败: ${e.message}")
            0
        }
    }

    /**
     * 判断缓存是否过期（超过30天）
     */
    private fun isStale(updatedAt: Long): Boolean {
        return System.currentTimeMillis() - updatedAt > 30L * 24 * 60 * 60 * 1000
    }
}
