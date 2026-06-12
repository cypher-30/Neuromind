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

// Schema is live in production installs — do not change entities without bumping version + a real migration.
@Database(entities = [Task::class, TimetableEntry::class, FeedbackLog::class], version = 8, exportSchema = false)
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
                    populateDatabase(
                        database.taskDao(),
                        database.timetableDao(),
                        database.feedbackLogDao()
                    )
                }
            }
        }

        suspend fun populateDatabase(
            taskDao: TaskDao,
            timetableDao: TimetableDao,
            feedbackLogDao: FeedbackLogDao
        ) {
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L

            // 1. Seed Timetable Entries (Full Week Schedule)
            val commonMorningRoutine = listOf(
                TimetableEntry(title = "Morning Routine", dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(9, 0), venue = "Home"),
                TimetableEntry(title = "Morning Routine", dayOfWeek = DayOfWeek.TUESDAY, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(9, 0), venue = "Home"),
                TimetableEntry(title = "Morning Routine", dayOfWeek = DayOfWeek.WEDNESDAY, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(9, 0), venue = "Home"),
                TimetableEntry(title = "Morning Routine", dayOfWeek = DayOfWeek.THURSDAY, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(9, 0), venue = "Home"),
                TimetableEntry(title = "Morning Routine", dayOfWeek = DayOfWeek.FRIDAY, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(9, 0), venue = "Home")
            )
            commonMorningRoutine.forEach { timetableDao.insertEntry(it) }

            val lectures = listOf(
                TimetableEntry(title = "Mobile App Dev", dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(12, 0), venue = "L01"),
                TimetableEntry(title = "Cloud Computing", dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(14, 0), endTime = LocalTime.of(16, 0), venue = "L05"),
                TimetableEntry(title = "Data Structures", dayOfWeek = DayOfWeek.TUESDAY, startTime = LocalTime.of(9, 30), endTime = LocalTime.of(11, 30), venue = "Lab 2"),
                TimetableEntry(title = "Algorithm Design", dayOfWeek = DayOfWeek.WEDNESDAY, startTime = LocalTime.of(11, 0), endTime = LocalTime.of(13, 0), venue = "L03"),
                TimetableEntry(title = "Deep Work", dayOfWeek = DayOfWeek.WEDNESDAY, startTime = LocalTime.of(15, 0), endTime = LocalTime.of(18, 0), venue = "Library"),
                TimetableEntry(title = "Project Lab", dayOfWeek = DayOfWeek.THURSDAY, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(13, 0), venue = "Lab 4"),
                TimetableEntry(title = "Soft Skills", dayOfWeek = DayOfWeek.FRIDAY, startTime = LocalTime.of(11, 0), endTime = LocalTime.of(12, 0), venue = "Online")
            )
            lectures.forEach { timetableDao.insertEntry(it) }

            val extracurriculars = listOf(
                TimetableEntry(title = "Gym Session", dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime.of(17, 0), endTime = LocalTime.of(18, 30), venue = "Campus Gym"),
                TimetableEntry(title = "Gym Session", dayOfWeek = DayOfWeek.WEDNESDAY, startTime = LocalTime.of(18, 30), endTime = LocalTime.of(20, 0), venue = "Campus Gym"),
                TimetableEntry(title = "Gym Session", dayOfWeek = DayOfWeek.FRIDAY, startTime = LocalTime.of(16, 0), endTime = LocalTime.of(17, 30), venue = "Campus Gym"),
                TimetableEntry(title = "Coding Club", dayOfWeek = DayOfWeek.TUESDAY, startTime = LocalTime.of(18, 0), endTime = LocalTime.of(19, 30), venue = "Innovation Hub")
            )
            extracurriculars.forEach { timetableDao.insertEntry(it) }

            // 2. Seed Tasks (Large variety)
            val tasks = listOf(
                // Overdue
                Task(title = "Submit Assignment 1", description = "Final PDF upload", dueDate = now - 2 * dayMillis, priority = Priority.HIGH, difficulty = Difficulty.HARD, isCompleted = false),
                Task(title = "Pay Internet Bill", dueDate = now - dayMillis, priority = Priority.MEDIUM, isCompleted = false),
                
                // Completed
                Task(title = "Groceries shopping", isCompleted = true, createdAt = now - 5 * dayMillis),
                Task(title = "Clean Apartment", isCompleted = true, createdAt = now - 4 * dayMillis),
                Task(title = "Read Chapter 5", isCompleted = true, createdAt = now - 3 * dayMillis),
                Task(title = "Update Resume", isCompleted = true, createdAt = now - 2 * dayMillis),
                Task(title = "Wash Car", isCompleted = true, createdAt = now - dayMillis),

                // Upcoming Today
                Task(title = "Prepare Lecture Notes", description = "Topic: Room DB", dueDate = now + 2 * 3600000L, priority = Priority.HIGH, durationMinutes = 45),
                Task(title = "Call Mom", dueDate = now + 4 * 3600000L, priority = Priority.LOW, durationMinutes = 20),
                Task(title = "Study for Midterms", description = "Focus on Sorting Algorithms", dueDate = now + 6 * 3600000L, priority = Priority.HIGH, difficulty = Difficulty.HARD, durationMinutes = 120),

                // Future
                Task(title = "Buy New Laptop Charger", dueDate = now + 2 * dayMillis, priority = Priority.MEDIUM, durationMinutes = 60),
                Task(title = "Organize Desk", dueDate = now + 3 * dayMillis, priority = Priority.LOW, durationMinutes = 30),
                Task(title = "Write Blog Post", dueDate = now + 5 * dayMillis, priority = Priority.MEDIUM, durationMinutes = 90),
                Task(title = "Doctor Appointment", dueDate = now + 7 * dayMillis, priority = Priority.HIGH, durationMinutes = 60)
            )
            tasks.forEach { taskDao.insertTask(it) }

            // 3. Seed Feedback Logs (30 days of data)
            val moods = Mood.entries
            for (i in 0 until 30) {
                val logDate = now - (i * dayMillis)
                // Randomize slightly to make charts look real
                feedbackLogDao.insertFeedbackLog(FeedbackLog(
                    date = logDate,
                    mood = moods.random(),
                    energyLevel = (3..10).random(),
                    tasksCompleted = (0..6).random(),
                    comment = if (i % 3 == 0) "Productive day $i" else "Daily check-in $i"
                ))
            }
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
