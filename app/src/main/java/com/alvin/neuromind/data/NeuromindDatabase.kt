package com.alvin.neuromind.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

@Database(entities = [Task::class, TimetableEntry::class, FeedbackLog::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NeuromindDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun timetableDao(): TimetableDao
    abstract fun feedbackLogDao(): FeedbackLogDao

    private class NeuromindDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.taskDao(), database.timetableDao())
                }
            }
        }

        suspend fun populateDatabase(taskDao: TaskDao, timetableDao: TimetableDao) {
            // 1. Add Sample Timetable Entries
            val lecture = TimetableEntry(
                title = "Mobile App Dev Lecture",
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(12, 0),
                venue = "Room 304",
                details = "Topic: Jetpack Compose"
            )
            timetableDao.insert(lecture) // Fixed: calls insert()

            timetableDao.insert(TimetableEntry( // Fixed: calls insert()
                title = "Gym",
                dayOfWeek = DayOfWeek.WEDNESDAY,
                startTime = LocalTime.of(18, 0),
                endTime = LocalTime.of(19, 30),
                venue = "Campus Gym"
            ))

            // 2. Add Sample Tasks
            taskDao.insert(Task(
                title = "Finish Neuromind V3",
                description = "Implement demo data and fix lag issues.",
                dueDate = System.currentTimeMillis() + 86400000,
                priority = Priority.HIGH,
                difficulty = Difficulty.HARD,
                durationMinutes = 120
            ))

            taskDao.insert(Task(
                title = "Buy Groceries",
                description = "Milk, Eggs, Bread",
                dueDate = System.currentTimeMillis() - 86400000,
                priority = Priority.MEDIUM,
                durationMinutes = 45,
                difficulty = Difficulty.EASY // FIXED: Added missing parameter
            ))
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: NeuromindDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): NeuromindDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NeuromindDatabase::class.java,
                    "neuromind_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(NeuromindDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}