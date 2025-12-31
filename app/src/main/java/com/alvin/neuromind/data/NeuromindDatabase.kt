package com.alvin.neuromind.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 1. Version number is increased
@Database(entities = [Task::class, TimetableEntry::class, FeedbackLog::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NeuromindDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun timetableDao(): TimetableDao
    abstract fun feedbackLogDao(): FeedbackLogDao

    companion object {
        // 2. The Migration object is defined
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timetable_entries ADD COLUMN venue TEXT")
                db.execSQL("ALTER TABLE timetable_entries ADD COLUMN details TEXT")
            }
        }

        @Volatile private var INSTANCE: NeuromindDatabase? = null
        fun getDatabase(context: Context): NeuromindDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, NeuromindDatabase::class.java, "neuromind_database")
                    // 3. The migration is added to the builder
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}