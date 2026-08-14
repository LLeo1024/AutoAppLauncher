package com.leo.autoapplaucher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class, ExecutionLogEntity::class, HolidayEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun executionLogDao(): ExecutionLogDao
    abstract fun holidayDao(): HolidayDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v3 → v4：tasks 表新增 returnDelaySeconds 字段（拉起后延时返回）。
         * 该迁移必须保留（历史库升级到 v4 需要它）。
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN returnDelaySeconds INTEGER NOT NULL DEFAULT 120"
                )
            }
        }

        /**
         * v4 → v5：移除 returnDelaySeconds 字段（延时返回功能已在 v1.8 移除）。
         *
         * Android 11 (API 30) 自带 SQLite 3.28，不支持 ALTER TABLE DROP COLUMN，
         * 因此采用标准做法：建新表 → 拷贝数据 → 删旧表 → 重命名。
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 创建新表（不含 returnDelaySeconds 列）
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `targetPackage` TEXT NOT NULL,
                        `targetAppName` TEXT NOT NULL,
                        `hour` INTEGER NOT NULL,
                        `minute` INTEGER NOT NULL,
                        `repeatMode` INTEGER NOT NULL,
                        `weekDays` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `useRandomTime` INTEGER NOT NULL,
                        `timeRangeStartHour` INTEGER NOT NULL,
                        `timeRangeStartMinute` INTEGER NOT NULL,
                        `timeRangeEndHour` INTEGER NOT NULL,
                        `timeRangeEndMinute` INTEGER NOT NULL
                    )
                    """
                )
                // 2. 拷贝数据（跳过 returnDelaySeconds 列）
                db.execSQL(
                    """
                    INSERT INTO `tasks_new` (
                        `id`, `targetPackage`, `targetAppName`, `hour`, `minute`,
                        `repeatMode`, `weekDays`, `enabled`, `createdAt`,
                        `useRandomTime`, `timeRangeStartHour`, `timeRangeStartMinute`,
                        `timeRangeEndHour`, `timeRangeEndMinute`
                    )
                    SELECT
                        `id`, `targetPackage`, `targetAppName`, `hour`, `minute`,
                        `repeatMode`, `weekDays`, `enabled`, `createdAt`,
                        `useRandomTime`, `timeRangeStartHour`, `timeRangeStartMinute`,
                        `timeRangeEndHour`, `timeRangeEndMinute`
                    FROM `tasks`
                    """
                )
                // 3. 删除旧表并重命名
                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auto_app_launcher_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
