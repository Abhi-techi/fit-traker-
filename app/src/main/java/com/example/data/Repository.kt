package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Repository(private val context: Context) {
    private val TAG = "Repository"

    // Initialize Room Database
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "fittrack_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val userDao = database.userDao()
    private val profileDao = database.profileDao()
    private val workoutDao = database.workoutDao()
    private val workoutLogDao = database.workoutLogDao()
    private val progressDao = database.progressDao()
    private val waterTrackerDao = database.waterTrackerDao()
    private val nutritionScanDao = database.nutritionScanDao()

    // Preferences for session caching
    private val prefs = context.getSharedPreferences("fittrack_prefs", Context.MODE_PRIVATE)

    // Current Session State
    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    init {
        // Restore active user session from preferences
        val savedUserId = prefs.getInt("logged_in_user_id", -1)
        if (savedUserId != -1) {
            _currentUserId.value = savedUserId
        }

        // Run seed check in background IO thread
        CoroutineScope(Dispatchers.IO).launch {
            seedPredefinedWorkoutsIfNeeded()
        }
    }

    // --- Authentication ---

    suspend fun registerUser(name: String, email: String, passwordRaw: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            if (name.isBlank() || email.isBlank() || passwordRaw.isBlank()) {
                return@withContext Result.failure(Exception("All fields are required"))
            }
            val existing = userDao.getUserByEmail(email)
            if (existing != null) {
                return@withContext Result.failure(Exception("An account with this email already exists"))
            }

            // In a production app you'd use BCrypt or PBKDF2. For prototype we'll save simple hash
            val passwordHash = passwordRaw.hashCode().toString()
            val newUser = User(name = name, email = email, passwordHash = passwordHash)
            val insertedId = userDao.insertUser(newUser).toInt()

            val createdUser = newUser.copy(id = insertedId)

            // Seed a default profile for the user
            val defaultProfile = Profile(
                userId = insertedId,
                age = 25,
                gender = "Not Specified",
                height = 175.0,
                weight = 70.0,
                fitnessGoal = "General Fitness"
            )
            profileDao.insertOrUpdateProfile(defaultProfile)

            // Auto-log the first weight entry to populate progress charts
            val initialBmi = 70.0 / ((175.0 / 100.0) * (175.0 / 100.0))
            progressDao.insertProgress(Progress(
                userId = insertedId,
                weight = 70.0,
                bmi = Math.round(initialBmi * 10.0) / 10.0,
                date = getCurrentFormattedDate()
            ))

            Result.success(createdUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, passwordRaw: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank() || passwordRaw.isBlank()) {
                return@withContext Result.failure(Exception("Please enter email and password"))
            }
            val user = userDao.getUserByEmail(email)
            if (user == null) {
                return@withContext Result.failure(Exception("No account registered with this email"))
            }

            val expectedHash = passwordRaw.hashCode().toString()
            if (user.passwordHash != expectedHash) {
                return@withContext Result.failure(Exception("Incorrect password"))
            }

            // Save session
            _currentUserId.value = user.id
            prefs.edit().putInt("logged_in_user_id", user.id).apply()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        _currentUserId.value = null
        prefs.edit().remove("logged_in_user_id").apply()
    }

    suspend fun resetPassword(email: String, newPasswordRaw: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserByEmail(email)
            if (user == null) {
                return@withContext Result.failure(Exception("No account exists with this email"))
            }
            val newHash = newPasswordRaw.hashCode().toString()
            userDao.insertUser(user.copy(passwordHash = newHash)) // Re-inserts (REPLACE)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserName(userId: Int): String {
        return withContext(Dispatchers.IO) {
            userDao.getUserById(userId)?.name ?: "User"
        }
    }

    // --- Profile & Progress ---

    fun getProfileFlow(userId: Int): Flow<Profile?> {
        return profileDao.getProfileByUserIdFlow(userId)
    }

    suspend fun getProfile(userId: Int): Profile? = withContext(Dispatchers.IO) {
        profileDao.getProfileByUserId(userId)
    }

    suspend fun saveProfile(profile: Profile) = withContext(Dispatchers.IO) {
        profileDao.insertOrUpdateProfile(profile)

        // Log an automatic progress snapshot for weight and BMI tracking
        val bmiVal = calculateBmiValue(profile.weight, profile.height)
        progressDao.insertProgress(Progress(
            userId = profile.userId,
            weight = profile.weight,
            bmi = bmiVal,
            date = getCurrentFormattedDate()
        ))
    }

    fun getProgressHistoryFlow(userId: Int): Flow<List<Progress>> {
        return progressDao.getProgressByUserIdFlow(userId)
    }

    suspend fun addManualProgress(userId: Int, weight: Double, height: Double) = withContext(Dispatchers.IO) {
        val bmiVal = calculateBmiValue(weight, height)
        val progress = Progress(
            userId = userId,
            weight = weight,
            bmi = bmiVal,
            date = getCurrentFormattedDate()
        )
        progressDao.insertProgress(progress)

        // Sync weight to profile
        val profile = profileDao.getProfileByUserId(userId)
        if (profile != null) {
            profileDao.insertOrUpdateProfile(profile.copy(weight = weight, height = height))
        }
    }

    suspend fun deleteProgressEntry(progressId: Int) = withContext(Dispatchers.IO) {
        progressDao.deleteProgress(progressId)
    }

    // --- Predefined Workouts library ---

    fun getPredefinedWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllPredefinedWorkouts()
    }

    private suspend fun seedPredefinedWorkoutsIfNeeded() {
        val count = workoutDao.getCount()
        if (count == 0) {
            Log.d(TAG, "Seeding predefined workout plans inside empty db...")
            val list = listOf(
                // ---- Weight Loss Plan ----
                Workout(
                    workoutName = "Weight Loss Plan",
                    description = "High-Intensity Interval training with core engagement suitable to burn fat quickly.",
                    exerciseName = "Jumping Jacks",
                    exerciseDesc = "Full-body dynamic aerobic exercise that increases cardiovascular performance and burns fat.",
                    instructions = "Start standing with feet together and hands at sides. Jump splitting feet outward and sweeping arms overhead. Jump back to baseline.",
                    targetMuscle = "Full Body Cardio",
                    sets = 3,
                    reps = 30,
                    restTimeSec = 30
                ),
                Workout(
                    workoutName = "Weight Loss Plan",
                    description = "High-Intensity Interval training with core engagement suitable to burn fat quickly.",
                    exerciseName = "Burpees",
                    exerciseDesc = "An intense full-body calorie-burning exercises testing cardiorespiratory conditioning.",
                    instructions = "Drop into dry squat, kick feet backwards to high push-up. Jump legs back inward to squat, and explode upwards with hands raised.",
                    targetMuscle = "Full Body / Cardio Boost",
                    sets = 3,
                    reps = 12,
                    restTimeSec = 45
                ),
                Workout(
                    workoutName = "Weight Loss Plan",
                    description = "High-Intensity Interval training with core engagement suitable to burn fat quickly.",
                    exerciseName = "Squats",
                    exerciseDesc = "Lower body compound resistance strength standard to activate massive leg and core calorie burns.",
                    instructions = "Bend at hips and knees, sitting back into lower squat till thighs parallel floor. Press back through heels to standing.",
                    targetMuscle = "Quadriceps & Glutes",
                    sets = 3,
                    reps = 20,
                    restTimeSec = 30
                ),

                // ---- Muscle Gain Plan ----
                Workout(
                    workoutName = "Muscle Gain Plan",
                    description = "Heavy concentration on skeletal resistance targeting muscle fibers hypertrophy and raw gains.",
                    exerciseName = "Pull-ups",
                    exerciseDesc = "Advanced upper pull exercise strengthening the complete posterior back chains.",
                    instructions = "Hang from overhead grip bar. Pull collar bone upward to touch bar, squeezing shoulder blades. Lower slowly back.",
                    targetMuscle = "Back & Biceps",
                    sets = 4,
                    reps = 8,
                    restTimeSec = 60
                ),
                Workout(
                    workoutName = "Muscle Gain Plan",
                    description = "Heavy concentration on skeletal resistance targeting muscle fibers hypertrophy and raw gains.",
                    exerciseName = "Push-ups",
                    exerciseDesc = "Compound chest pushing exercise building solid arms, chest, shoulders, and triceps.",
                    instructions = "Align in flat plank pose, arms vertical. Bend elbows to lower target chest inches off floor. Push strongly to restart.",
                    targetMuscle = "Chest & Triceps",
                    sets = 4,
                    reps = 15,
                    restTimeSec = 45
                ),
                Workout(
                    workoutName = "Muscle Gain Plan",
                    description = "Heavy concentration on skeletal resistance targeting muscle fibers hypertrophy and raw gains.",
                    exerciseName = "Squats",
                    exerciseDesc = "Master lower structural strengthener adding dense thick muscle fibers across the hips and quads.",
                    instructions = "With heavy focus on flat back, bend hips deep to full stretch parallel posture. Explode to stand, squeeze glutes.",
                    targetMuscle = "Quads, Glutes & Hamstrings",
                    sets = 4,
                    reps = 12,
                    restTimeSec = 60
                ),

                // ---- General Fitness Plan ----
                Workout(
                    workoutName = "General Fitness Plan",
                    description = "Balanced movement patterns enhancing daily life mobility, core strength, and general conditioning.",
                    exerciseName = "Lunges",
                    exerciseDesc = "Single leg focal developer targeting physical symmetry and hip mobility.",
                    instructions = "Step forward with one leg. Lower hips until both rear and front knees bend exactly 90 degrees. Push back up.",
                    targetMuscle = "Thighs & Balance Core",
                    sets = 3,
                    reps = 12,
                    restTimeSec = 45
                ),
                Workout(
                    workoutName = "General Fitness Plan",
                    description = "Balanced movement patterns enhancing daily life mobility, core strength, and general conditioning.",
                    exerciseName = "Plank",
                    exerciseDesc = "Static isometric spine stabilizer building massive deep core shielding and endurance.",
                    instructions = "Rest upper weight onto forearms and toes. Form and lock rigid horizontal board pose. Hold tightly, breathe slowly.",
                    targetMuscle = "Abdominals & Core Stability",
                    sets = 3,
                    reps = 60, // reps represent 60 seconds
                    restTimeSec = 30
                ),
                Workout(
                    workoutName = "General Fitness Plan",
                    description = "Balanced movement patterns enhancing daily life mobility, core strength, and general conditioning.",
                    exerciseName = "Push-ups",
                    exerciseDesc = "Classic foundational horizontal press matching arms and shoulder stamina.",
                    instructions = "Hold straight torso plank. Press up and down to build general functional stamina across torso and scapular zones.",
                    targetMuscle = "Pectoral & Deltoids",
                    sets = 3,
                    reps = 10,
                    restTimeSec = 45
                )
            )
            workoutDao.insertWorkouts(list)
        }
    }

    // --- Workout completion logs ---

    fun getLogsByDateFlow(userId: Int, date: String): Flow<List<WorkoutLog>> {
        return workoutLogDao.getLogsByDateFlow(userId, date)
    }

    suspend fun getLogsByDate(userId: Int, date: String): List<WorkoutLog> {
        return workoutLogDao.getLogsByDate(userId, date)
    }

    fun getWorkoutHistoryFlow(userId: Int): Flow<List<WorkoutLog>> {
        return workoutLogDao.getWorkoutHistoryFlow(userId)
    }

    suspend fun toggleExerciseCompletion(userId: Int, workoutName: String, exerciseName: String, date: String) = withContext(Dispatchers.IO) {
        val currentLogs = workoutLogDao.getLogsByDate(userId, date)
        val matched = currentLogs.find { it.workoutName == workoutName && it.exerciseName == exerciseName }
        if (matched != null) {
            // Delete log means toggled off
            workoutLogDao.deleteLog(userId, date, exerciseName)
        } else {
            // Create completed log
            val log = WorkoutLog(
                userId = userId,
                workoutName = workoutName,
                exerciseName = exerciseName,
                date = date,
                completed = true
            )
            workoutLogDao.insertOrUpdateLog(log)
        }
    }

    // --- Water Intake Tracker ---

    fun getWaterTodayFlow(userId: Int, date: String): Flow<WaterTracker?> {
        return waterTrackerDao.getWaterTodayFlow(userId, date)
    }

    suspend fun getWaterToday(userId: Int, date: String): WaterTracker? = withContext(Dispatchers.IO) {
        waterTrackerDao.getWaterToday(userId, date)
    }

    suspend fun incrementWater(userId: Int, date: String, goalCount: Int = 8) = withContext(Dispatchers.IO) {
        val today = waterTrackerDao.getWaterToday(userId, date)
        if (today == null) {
            waterTrackerDao.insertOrUpdateWater(WaterTracker(userId = userId, glasses = 1, date = date))
        } else {
            waterTrackerDao.insertOrUpdateWater(today.copy(glasses = today.glasses + 1))
        }
    }

    suspend fun decrementWater(userId: Int, date: String) = withContext(Dispatchers.IO) {
        val today = waterTrackerDao.getWaterToday(userId, date)
        if (today != null && today.glasses > 0) {
            waterTrackerDao.insertOrUpdateWater(today.copy(glasses = today.glasses - 1))
        }
    }

    suspend fun resetWater(userId: Int, date: String) = withContext(Dispatchers.IO) {
        val today = waterTrackerDao.getWaterToday(userId, date)
        if (today != null) {
            waterTrackerDao.insertOrUpdateWater(today.copy(glasses = 0))
        }
    }

    // --- AI Nutrition Scans ---

    fun getNutritionScansFlow(userId: Int): Flow<List<NutritionScan>> {
        return nutritionScanDao.getAllScansFlow(userId)
    }

    suspend fun saveNutritionScan(userId: Int, foodName: String, facts: NutritionResult) = withContext(Dispatchers.IO) {
        val scan = NutritionScan(
            userId = userId,
            foodName = facts.foodName,
            calories = facts.calories,
            protein = facts.protein,
            carbohydrates = facts.carbohydrates,
            fat = facts.fat,
            fiber = facts.fiber,
            sugar = facts.sugar,
            imagePath = getPresetImageNameForFood(foodName)
        )
        nutritionScanDao.insertScan(scan)
    }

    suspend fun deleteScanEntry(scanId: Int) = withContext(Dispatchers.IO) {
        nutritionScanDao.deleteScan(scanId)
    }

    suspend fun clearScanHistory(userId: Int) = withContext(Dispatchers.IO) {
        nutritionScanDao.clearHistory(userId)
    }

    // --- Helpers ---

    fun getCurrentFormattedDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    fun calculateBmiValue(weight: Double, height: Double): Double {
        if (height <= 0) return 0.0
        val heightM = height / 100.0
        val rawBmi = weight / (heightM * heightM)
        return Math.round(rawBmi * 10.0) / 10.0
    }

    private fun getPresetImageNameForFood(foodName: String): String {
        val lower = foodName.lowercase()
        return when {
            lower.contains("banana") -> "ic_food_banana"
            lower.contains("apple") -> "ic_food_apple"
            lower.contains("pizza") -> "ic_food_pizza"
            lower.contains("burger") -> "ic_food_burger"
            lower.contains("salad") -> "ic_food_salad"
            lower.contains("egg") -> "ic_food_egg"
            lower.contains("chicken") -> "ic_food_chicken"
            lower.contains("rice") -> "ic_food_rice"
            lower.contains("milk") -> "ic_food_milk"
            else -> "ic_food_generic"
        }
    }
}
