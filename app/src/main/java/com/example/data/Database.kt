package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. User Entity ---
@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val passwordHash: String, // Securely hashes simple passwords
    val createdAt: Long = System.currentTimeMillis()
)

// --- 2. Profile Entity ---
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val age: Int,
    val gender: String,
    val height: Double, // in cm
    val weight: Double, // in kg
    val fitnessGoal: String // "Weight Loss", "Muscle Gain", "General Fitness"
)

// --- 3. Workout Plan Entity ---
@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val workoutId: Int = 0,
    val workoutName: String, // e.g. "Weight Loss Plan", "Muscle Gain Plan", "General Fitness Plan"
    val description: String,
    val exerciseName: String,
    val exerciseDesc: String,
    val instructions: String,
    val targetMuscle: String,
    val sets: Int,
    val reps: Int,
    val restTimeSec: Int
)

// --- 4. Workout Completed Log Entity ---
@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val logId: Int = 0,
    val userId: Int,
    val workoutName: String,
    val exerciseName: String,
    val date: String, // "YYYY-MM-DD"
    val completed: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// --- 5. Progress Entity ---
@Entity(tableName = "progress")
data class Progress(
    @PrimaryKey(autoGenerate = true) val progressId: Int = 0,
    val userId: Int,
    val weight: Double,
    val bmi: Double,
    val date: String, // "YYYY-MM-DD"
    val timestamp: Long = System.currentTimeMillis()
)

// --- 6. Water Tracker Entity ---
@Entity(tableName = "water_tracker")
data class WaterTracker(
    @PrimaryKey(autoGenerate = true) val trackerId: Int = 0,
    val userId: Int,
    val glasses: Int, // Number of glasses (e.g. 250ml each)
    val date: String // "YYYY-MM-DD"
)

// --- 7. Nutrition Scans Entity ---
@Entity(tableName = "nutrition_scans")
data class NutritionScan(
    @PrimaryKey(autoGenerate = true) val scanId: Int = 0,
    val userId: Int,
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val carbohydrates: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val imagePath: String, // Can store simulated drawables or local temp URIs
    val scanDate: Long = System.currentTimeMillis()
)

// --- DAOs ---

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    fun getProfileByUserIdFlow(userId: Int): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfileByUserId(userId: Int): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: Profile)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts")
    fun getAllPredefinedWorkouts(): Flow<List<Workout>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<Workout>)

    @Query("SELECT COUNT(*) FROM workouts")
    suspend fun getCount(): Int
}

@Dao
interface WorkoutLogDao {
    @Query("SELECT * FROM workout_logs WHERE userId = :userId AND date = :date")
    fun getLogsByDateFlow(userId: Int, date: String): Flow<List<WorkoutLog>>

    @Query("SELECT * FROM workout_logs WHERE userId = :userId AND date = :date")
    suspend fun getLogsByDate(userId: Int, date: String): List<WorkoutLog>

    @Query("SELECT * FROM workout_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getWorkoutHistoryFlow(userId: Int): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: WorkoutLog)

    @Query("DELETE FROM workout_logs WHERE userId = :userId AND date = :date AND exerciseName = :exerciseName")
    suspend fun deleteLog(userId: Int, date: String, exerciseName: String)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE userId = :userId ORDER BY timestamp ASC")
    fun getProgressByUserIdFlow(userId: Int): Flow<List<Progress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: Progress)

    @Query("DELETE FROM progress WHERE progressId = :progressId")
    suspend fun deleteProgress(progressId: Int)
}

@Dao
interface WaterTrackerDao {
    @Query("SELECT * FROM water_tracker WHERE userId = :userId AND date = :date LIMIT 1")
    fun getWaterTodayFlow(userId: Int, date: String): Flow<WaterTracker?>

    @Query("SELECT * FROM water_tracker WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getWaterToday(userId: Int, date: String): WaterTracker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWater(water: WaterTracker)
}

@Dao
interface NutritionScanDao {
    @Query("SELECT * FROM nutrition_scans WHERE userId = :userId ORDER BY scanDate DESC")
    fun getAllScansFlow(userId: Int): Flow<List<NutritionScan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: NutritionScan)

    @Query("DELETE FROM nutrition_scans WHERE scanId = :scanId")
    suspend fun deleteScan(scanId: Int)

    @Query("DELETE FROM nutrition_scans WHERE userId = :userId")
    suspend fun clearHistory(userId: Int)
}

// --- AppDatabase ---

@Database(
    entities = [
        User::class,
        Profile::class,
        Workout::class,
        WorkoutLog::class,
        Progress::class,
        WaterTracker::class,
        NutritionScan::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun profileDao(): ProfileDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun progressDao(): ProgressDao
    abstract fun waterTrackerDao(): WaterTrackerDao
    abstract fun nutritionScanDao(): NutritionScanDao
}
