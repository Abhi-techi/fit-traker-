package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Success(val user: User) : AuthState
    data class Error(val message: String) : AuthState
}

sealed interface ScanState {
    object Idle : ScanState
    object Loading : ScanState
    data class Success(val nutrition: NutritionResult) : ScanState
    data class Error(val message: String) : ScanState
}

class FitViewModel(private val repository: Repository, private val context: Context) : ViewModel() {
    private val TAG = "FitViewModel"

    // Authentication states
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Screen Navigation
    private val _currentScreen = MutableStateFlow("auth_login")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Exercise Library Search text
    val searchQuery = MutableStateFlow("")

    // Scan State
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // Interactive inputs
    val registerName = MutableStateFlow("")
    val registerEmail = MutableStateFlow("")
    val registerPassword = MutableStateFlow("")

    val loginEmail = MutableStateFlow("")
    val loginPassword = MutableStateFlow("")

    val resetEmail = MutableStateFlow("")
    val resetNewPassword = MutableStateFlow("")

    val editAge = MutableStateFlow("25")
    val editGender = MutableStateFlow("Male")
    val editHeight = MutableStateFlow("170.0")
    val editWeight = MutableStateFlow("70.0")
    val editGoal = MutableStateFlow("General Fitness")

    // Session-dependent Flows
    val currentUserId: StateFlow<Int?> = repository.currentUserId

    @OptIn(ExperimentalCoroutinesApi::class)
    val userProfile: StateFlow<Profile?> = repository.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) repository.getProfileFlow(userId) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val progressHistory: StateFlow<List<Progress>> = repository.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) repository.getProgressHistoryFlow(userId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val waterToday: StateFlow<WaterTracker?> = repository.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                repository.getWaterTodayFlow(userId, repository.getCurrentFormattedDate())
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val scanHistory: StateFlow<List<NutritionScan>> = repository.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) repository.getNutritionScansFlow(userId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val predefinedWorkouts: StateFlow<List<Workout>> = repository.getPredefinedWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val exerciseLogsToday: StateFlow<List<WorkoutLog>> = repository.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                repository.getLogsByDateFlow(userId, repository.getCurrentFormattedDate())
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val exerciseLogsHistory: StateFlow<List<WorkoutLog>> = repository.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) repository.getWorkoutHistoryFlow(userId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentUserName = MutableStateFlow("User")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    init {
        // Setup simple notifications channel
        createNotificationChannel()

        viewModelScope.launch {
            repository.currentUserId.collect { id ->
                if (id != null) {
                    _currentUserName.value = repository.getUserName(id)
                    // Pre-fill profile editors
                    val profile = repository.getProfile(id)
                    if (profile != null) {
                        editAge.value = profile.age.toString()
                        editGender.value = profile.gender
                        editHeight.value = profile.height.toString()
                        editWeight.value = profile.weight.toString()
                        editGoal.value = profile.fitnessGoal
                    }
                    _currentScreen.value = "dashboard"
                } else {
                    _currentScreen.value = "auth_login"
                }
            }
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // --- Authentication Actions ---

    fun login() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val email = loginEmail.value.trim()
            val pass = loginPassword.value.trim()
            val result = repository.loginUser(email, pass)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                _authState.value = AuthState.Success(user)
                _currentUserName.value = user.name
                _currentScreen.value = "dashboard"

                // Clear login fields
                loginPassword.value = ""
                // Simulated onboarding login notification
                sendNotification(
                    "Welcome back to FitTrack!",
                    "Let's track your water intake and complete today's workout plan: ${editGoal.value}!"
                )
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun register() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val name = registerName.value.trim()
            val email = registerEmail.value.trim()
            val pass = registerPassword.value.trim()

            if (pass.length < 4) {
                _authState.value = AuthState.Error("Password must be at least 4 characters long")
                return@launch
            }

            val result = repository.registerUser(name, email, pass)
            if (result.isSuccess) {
                // Successfully registered, auto login next
                val resLogin = repository.loginUser(email, pass)
                if (resLogin.isSuccess) {
                    _authState.value = AuthState.Success(resLogin.getOrThrow())
                    _currentScreen.value = "dashboard"
                    sendNotification(
                        "Welcome to FitTrack, $name!",
                        "Your smart workout & nutrition diary is now ready. Set your daily water and workout goals!"
                    )
                } else {
                    _authState.value = AuthState.Error("Account created, please log in.")
                    _currentScreen.value = "auth_login"
                }
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun resetPassword() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val email = resetEmail.value.trim()
            val newPass = resetNewPassword.value.trim()

            if (newPass.length < 4) {
                _authState.value = AuthState.Error("Password must be at least 4 characters long")
                return@launch
            }

            val result = repository.resetPassword(email, newPass)
            if (result.isSuccess) {
                _authState.value = AuthState.Idle
                resetEmail.value = ""
                resetNewPassword.value = ""
                _currentScreen.value = "auth_login"
                sendNotification("Password Reset Successful", "Please log in using your new password.")
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Reset failed")
            }
        }
    }

    fun logout() {
        repository.logout()
        _currentScreen.value = "auth_login"
        _authState.value = AuthState.Idle
    }

    // --- Profile & Habits Actions ---

    fun updateProfile() {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            val age = editAge.value.toIntOrNull() ?: 25
            val gender = editGender.value
            val height = editHeight.value.toDoubleOrNull() ?: 170.0
            val weight = editWeight.value.toDoubleOrNull() ?: 70.0
            val goal = editGoal.value

            val updatedProfile = Profile(
                userId = userId,
                age = age,
                gender = gender,
                height = height,
                weight = weight,
                fitnessGoal = goal
            )
            repository.saveProfile(updatedProfile)
            _currentScreen.value = "dashboard"
            sendNotification(
                "Profile Updated Successfully",
                "Your details have been updated. BMI recalculation generated a value of ${repository.calculateBmiValue(weight, height)}!"
            )
        }
    }

    fun addManualWeightRecord(weight: Double, height: Double) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            repository.addManualProgress(userId, weight, height)
            editWeight.value = weight.toString()
            editHeight.value = height.toString()
        }
    }

    fun deleteProgressEntry(progressId: Int) {
        viewModelScope.launch {
            repository.deleteProgressEntry(progressId)
        }
    }

    fun incrementWater() {
        val id = currentUserId.value ?: return
        viewModelScope.launch {
            repository.incrementWater(id, repository.getCurrentFormattedDate())

            val checkWater = repository.getWaterToday(id, repository.getCurrentFormattedDate())
            if (checkWater != null && checkWater.glasses == 7) {
                // Pre-notify when approaching daily goal
                sendNotification(
                    "Almost there! 💧",
                    "You've logged 7 glasses today! Just one more glass to crush your active hydration goal!"
                )
            } else if (checkWater != null && checkWater.glasses == 8) {
                sendNotification(
                    "Water Goal Crushed! 🎉",
                    "Awesome! You've officially achieved your target of 8 glasses today. Keep drinking to stay vitalized!"
                )
            }
        }
    }

    fun decrementWater() {
        val id = currentUserId.value ?: return
        viewModelScope.launch {
            repository.decrementWater(id, repository.getCurrentFormattedDate())
        }
    }

    fun resetWaterToday() {
        val id = currentUserId.value ?: return
        viewModelScope.launch {
            repository.resetWater(id, repository.getCurrentFormattedDate())
        }
    }

    // --- Workout completion Actions ---

    fun toggleWorkoutExercise(workoutName: String, exerciseName: String) {
        val id = currentUserId.value ?: return
        viewModelScope.launch {
            repository.toggleExerciseCompletion(id, workoutName, exerciseName, repository.getCurrentFormattedDate())

            // Celebrate completion on finish of a workout plan
            val logs = repository.getLogsByDate(id, repository.getCurrentFormattedDate())
            val matchedLogs = logs.filter { it.workoutName == workoutName }
            if (matchedLogs.size == 3) {
                sendNotification(
                    "Workout Complete! 🏆",
                    "Dynamic stats logged: You successfully crushed every single routine in $workoutName! Great progress!"
                )
            }
        }
    }

    // --- AI Nutrition Scanner Actions ---

    fun requestFoodNutritionScan(bitmap: Bitmap?, foodTextInput: String?) {
        val id = currentUserId.value ?: return
        viewModelScope.launch {
            _scanState.value = ScanState.Loading
            val queryText = foodTextInput?.trim()
            val result = GeminiService.analyzeFood(bitmap, queryText)

            if (result != null) {
                _scanState.value = ScanState.Success(result)
                repository.saveNutritionScan(id, result.foodName, result)
                sendNotification(
                    "New nutrition scan logged!",
                    "Recognized food: ${result.foodName} (${result.calories} kcal, ${result.protein}g protein)."
                )
            } else {
                _scanState.value = ScanState.Error("Could not analysis nutritional facts. Please verify your query.")
            }
        }
    }

    fun deleteScan(scanId: Int) {
        viewModelScope.launch {
            repository.deleteScanEntry(scanId)
        }
    }

    fun clearScans() {
        val id = currentUserId.value ?: return
        viewModelScope.launch {
            repository.clearScanHistory(id)
        }
    }

    // --- Simulated FCM push Notifications trigger triggers ---

    fun triggerWorkoutReminderDemo() {
        sendNotification(
            "🏋️ FitTrack Daily Workout Alert",
            "Hey $currentUserName, it's peak energy hour! Squeeze in your active ${editGoal.value} routine to keep your streak burning!"
        )
    }

    fun triggerWaterReminderDemo() {
        sendNotification(
            "💧 Hydration Check reminder",
            "Time to take several sips of fresh water! Log a glass on the Water Tracker to protect your wellness."
        )
    }

    fun triggerHealthyQuoteDemo() {
        val tips = listOf(
            "Consistency beats intensity. Achieve small fitness tasks every day!",
            "Did you know? Building muscle mass naturally burns more daily calories at rest.",
            "Combine hydration with fresh fruits to fast track natural muscular recovery.",
            "Tracking your nutrition with FitTrack AI increases nutritional discipline up to 40%!"
        )
        sendNotification(
            "💡 Daily Fitness Advice",
            tips.random()
        )
    }

    // --- Android Notification Service core logic ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "FitTrack Alerts"
            val descriptionText = "Handles workout streaks and hydration notification updates"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("fittrack_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, body: String) {
        try {
            val builder = NotificationCompat.Builder(context, "fittrack_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Notification trigger exception: ${e.message}")
        }
    }
}

// Simple Factory for ViewModel instantiation in Activity
class FitViewModelFactory(private val repository: Repository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FitViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
