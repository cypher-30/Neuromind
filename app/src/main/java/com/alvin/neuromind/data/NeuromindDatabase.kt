package com.alvin.neuromind.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Database(entities = [Task::class, TimetableEntry::class, FeedbackLog::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NeuromindDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun timetableDao(): TimetableDao
    abstract fun feedbackLogDao(): FeedbackLogDao

    // Callback to populate database on creation
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
            // 1. Add Sample Timetable Entries (Classes/Events)
            val lecture = TimetableEntry(
                title = "Mobile App Dev Lecture",
                dayOfWeek = DayOfWeek.MONDAY, // Adjust this to match TODAY if you want to see it immediately
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(12, 0),
                venue = "Room 304",
                details = "Topic: Jetpack Compose"
            )
            timetableDao.insertEntry(lecture)

            timetableDao.insertEntry(TimetableEntry(
                title = "Gym",
                dayOfWeek = DayOfWeek.WEDNESDAY,
                startTime = LocalTime.of(18, 0),
                endTime = LocalTime.of(19, 30),
                venue = "Campus Gym"
            ))

            // 2. Add Sample Tasks
            taskDao.insertTask(Task(
                title = "Finish Neuromind V3",
                description = "Implement demo data and fix lag issues.",
                dueDate = System.currentTimeMillis() + 86400000, // Due tomorrow
                priority = Priority.HIGH,
                difficulty = Difficulty.HARD,
                durationMinutes = 120
            ))

            taskDao.insertTask(Task(
                title = "Buy Groceries",
                description = "Milk, Eggs, Bread",
                dueDate = System.currentTimeMillis() - 86400000, // Overdue (Yesterday)
                priority = Priority.MEDIUM,
                durationMinutes = 45
            ))
        }
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timetable_entries ADD COLUMN venue TEXT")
                db.execSQL("ALTER TABLE timetable_entries ADD COLUMN details TEXT")
            }
        }

        @Volatile private var INSTANCE: NeuromindDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): NeuromindDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, NeuromindDatabase::class.java, "neuromind_database")
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(NeuromindDatabaseCallback(scope)) // Attach the callback here
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}