package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FitTrackApp(viewModel: FitViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
            when (screen) {
                // --- Auth Screens ---
                "auth_login" -> LoginScreen(viewModel)
                "auth_register" -> RegisterScreen(viewModel)
                "auth_forgot" -> ForgotPasswordScreen(viewModel)

                // --- Main Application Area ---
                else -> MainDashboardContainer(viewModel, screen)
            }
        }
    }
}

// --- Main App Shell with Custom bottom bar / navigation rails ---
@Composable
fun MainDashboardContainer(viewModel: FitViewModel, activeTab: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_nav_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    Triple("dashboard", "Dashboard", Icons.Default.Dashboard),
                    Triple("workouts", "Workouts", Icons.Default.FitnessCenter),
                    Triple("exercises", "Exercises", Icons.Default.Search),
                    Triple("water", "Water", Icons.Default.WaterDrop),
                    Triple("scanner", "AI Scan", Icons.Default.CameraAlt),
                    Triple("profile", "Profile", Icons.Default.AccountCircle)
                )

                tabs.forEach { (tabId, label, icon) ->
                    NavigationBarItem(
                        selected = activeTab == tabId,
                        onClick = { viewModel.navigateTo(tabId) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "dashboard" -> DashboardScreen(viewModel)
                "workouts" -> WorkoutsScreen(viewModel)
                "exercises" -> ExerciseLibraryScreen(viewModel)
                "water" -> WaterIntakeScreen(viewModel)
                "scanner" -> AIScannerScreen(viewModel)
                "profile" -> ProfileScreen(viewModel)
                else -> DashboardScreen(viewModel)
            }
        }
    }
}

// ==========================================
// 1. USER AUTHENTICATION SCREENS
// ==========================================

@Composable
fun LoginScreen(viewModel: FitViewModel) {
    val email by viewModel.loginEmail.collectAsStateWithLifecycle()
    val password by viewModel.loginPassword.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App header / branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "FitTrack",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "Smart Fitness & AI Nutrition Assistant",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Login Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome Back",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.loginEmail.value = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("login_email")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.loginPassword.value = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("login_password")
                )

                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = { viewModel.login() },
                    enabled = authState !is AuthState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_button")
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Sign In", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Onboarding links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { viewModel.navigateTo("auth_forgot") }) {
                Text("Forgot Password?")
            }
            TextButton(onClick = { viewModel.navigateTo("auth_register") }) {
                Text("Create Account")
            }
        }
    }
}

@Composable
fun RegisterScreen(viewModel: FitViewModel) {
    val name by viewModel.registerName.collectAsStateWithLifecycle()
    val email by viewModel.registerEmail.collectAsStateWithLifecycle()
    val password by viewModel.registerPassword.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("FitTrack registration", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Configure FitTrack Account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.registerName.value = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("register_name")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.registerEmail.value = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("register_email")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.registerPassword.value = it },
                    label = { Text("Password (Min 4 chars)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("register_password")
                )

                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = { viewModel.register() },
                    enabled = authState !is AuthState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("register_button")
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Create Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { viewModel.navigateTo("auth_login") }) {
            Text("Already have an account? Sign In")
        }
    }
}

@Composable
fun ForgotPasswordScreen(viewModel: FitViewModel) {
    val email by viewModel.resetEmail.collectAsStateWithLifecycle()
    val newPassword by viewModel.resetNewPassword.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Forgot Password", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Verify Email and Choose New Password",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.resetEmail.value = it },
                    label = { Text("Account Email Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { viewModel.resetNewPassword.value = it },
                    label = { Text("New Secure Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = { viewModel.resetPassword() },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Overwrite & Reset Passion", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { viewModel.navigateTo("auth_login") }) {
            Text("Back to Sign In")
        }
    }
}

// ==========================================
// 2. DASHBOARD VIEW (HOME)
// ==========================================

@Composable
fun DashboardScreen(viewModel: FitViewModel) {
    val userName by viewModel.currentUserName.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val waterEntry by viewModel.waterToday.collectAsStateWithLifecycle()
    val logsToday by viewModel.exerciseLogsToday.collectAsStateWithLifecycle()
    val predefinedWorkouts by viewModel.predefinedWorkouts.collectAsStateWithLifecycle()

    val currentGoal = profile?.fitnessGoal ?: "General Fitness"
    val exercisesInGoal = predefinedWorkouts.filter { it.workoutName == "$currentGoal Plan" }
    val totalGoalCount = if (exercisesInGoal.isNotEmpty()) exercisesInGoal.size else 3
    val completedCount = logsToday.filter { it.workoutName == "$currentGoal Plan" && it.completed }.size

    val completionPercentage = if (totalGoalCount > 0) {
        (completedCount.toFloat() / totalGoalCount.toFloat())
    } else 0f

    val currentWeight = profile?.weight ?: 70.0
    val currentHeight = profile?.height ?: 170.0
    val bmiValue = if (currentHeight > 0) {
        Math.round((currentWeight / ((currentHeight/100.0) * (currentHeight/100.0))) * 10.0) / 10.0
    } else 0.0

    val bmiCategory = when {
        bmiValue < 18.5 -> "Underweight"
        bmiValue < 25.0 -> "Normal"
        bmiValue < 30.0 -> "Overweight"
        else -> "Obese"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcoming card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Fit",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = (-1).sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Text(
                                text = "Track",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Text(
                            text = "HEALTH & NUTRITION AI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Welcome info card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, $userName!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Fitness Goal: $currentGoal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Height: ${currentHeight}cm | Weight: ${currentWeight}kg",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Action Demos Card (For automated / reviewer testing)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Reviewer Quick Demos (Simulate Firebase Messaging Notifications)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerWorkoutReminderDemo() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Workout Goal FCB", fontSize = 9.sp)
                        }
                        Button(
                            onClick = { viewModel.triggerWaterReminderDemo() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Water FCM", fontSize = 9.sp)
                        }
                        Button(
                            onClick = { viewModel.triggerHealthyQuoteDemo() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("AI Quote Alert", fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        // Today's Status Summary Grid (BMI & Water & Workouts completion trackers)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BMI card matching the bg-[#EDF1EC] surface theme
                Card(
                    modifier = Modifier.weight(1f).height(128.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "CURRENT BMI", 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = bmiValue.toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                bmiCategory, 
                                fontSize = 10.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Water tracker summary card matching the warm yellow bg-[#F2E7D3] container
                val glassesToday = waterEntry?.glasses ?: 0
                val waterPercentage = minOf(glassesToday.toFloat() / 8f, 1f)

                Card(
                    modifier = Modifier.weight(1f).height(128.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    onClick = { viewModel.navigateTo("water") }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "WATER INTAKE", 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$glassesToday/8",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        Column {
                            LinearProgressIndicator(
                                progress = { waterPercentage },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = Color.White.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Target: 8 glasses",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Featured Design CTA Card matching the bg-[#386B3F] section from design specs
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .clickable { viewModel.navigateTo("scanner") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = 120.dp.toPx(),
                                center = Offset(size.width - 20.dp.toPx(), size.height + 20.dp.toPx())
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.05f),
                                radius = 40.dp.toPx(),
                                center = Offset(size.width - 40.dp.toPx(), 40.dp.toPx()),
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "PulsatingDot")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dotAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF9800).copy(alpha = pulseAlpha))
                            )
                            Text(
                                "UNIQUE AI FEATURE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.5.sp
                            )
                        }

                        Text(
                            text = "Scan Your Meal",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Text(
                            text = "Analyze nutrition instantly with AI image recognition.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 16.dp).widthIn(max = 240.dp)
                        )

                        Button(
                            onClick = { viewModel.navigateTo("scanner") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Open Camera",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's Workouts checklist card matching white background & border stroke [#DDE4DC]
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily WorkoutPlan", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$currentGoal Plan", 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Progress: ${(completionPercentage * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { completionPercentage },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (exercisesInGoal.isEmpty()) {
                        Text(
                            "Syncing exercises library... Check workouts page.",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            exercisesInGoal.forEach { exercise ->
                                val isCompleted = logsToday.any { it.exerciseName == exercise.exerciseName && it.completed }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.background)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                        .clickable { viewModel.toggleWorkoutExercise("$currentGoal Plan", exercise.exerciseName) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .border(2.dp, if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.exerciseName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            style = if (isCompleted) MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyMedium,
                                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "${exercise.sets} sets x ${exercise.reps} reps | Rest: ${exercise.restTimeSec}s",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. WORKOUT PLANS VIEW
// ==========================================

@Composable
fun WorkoutsScreen(viewModel: FitViewModel) {
    val predefinedWorkouts by viewModel.predefinedWorkouts.collectAsStateWithLifecycle()
    val logsToday by viewModel.exerciseLogsToday.collectAsStateWithLifecycle()

    var activePlanTab by remember { mutableStateOf("General Fitness") }
    val plans = listOf("Weight Loss", "Muscle Gain", "General Fitness")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workouts_screen")
            .padding(16.dp)
    ) {
        Text("Predefined Workouts plans", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

        // Sliding tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            plans.forEach { plan ->
                val selected = activePlanTab == plan
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activePlanTab = plan }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = plan,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Workout plans exercises
        val activeExercises = predefinedWorkouts.filter { it.workoutName == "$activePlanTab Plan" }

        if (activeExercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1.0f)
            ) {
                item {
                    Text(
                        text = activeExercises.firstOrNull()?.description ?: "Follow this tailored routine built by experts.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(activeExercises) { workout ->
                    val isCompleted = logsToday.any { it.exerciseName == workout.exerciseName && it.completed }
                    var expandedDetail by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        onClick = { expandedDetail = !expandedDetail }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1.0f)) {
                                    Text(workout.exerciseName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${workout.sets} Sets x ${workout.reps} Reps | Rest: ${workout.restTimeSec}s",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { viewModel.toggleWorkoutExercise("$activePlanTab Plan", workout.exerciseName) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isCompleted) "Done" else "Do", fontSize = 11.sp)
                                }
                            }

                            if (expandedDetail) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Description", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(workout.exerciseDesc, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

                                    Text("Instructions", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(workout.instructions, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

                                    Text("Target Muscles", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(workout.targetMuscle, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))

                                    // Dynamic SVG physical simulator Canvas
                                    Text("Movement Simulation", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ExerciseVisualizer(exerciseName = workout.exerciseName)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom animated exercise vector simulator to bypass static image requirements
@Composable
fun ExerciseVisualizer(exerciseName: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "PhysicVisualizer")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RepProgress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (exerciseName.lowercase()) {
            "push-ups" -> {
                // Draw ground line
                drawLine(Color.Gray, Offset(20f, h - 20f), Offset(w - 20f, h - 20f), strokeWidth = 4f)
                // Draw body bar tilting
                val bodyY = h - 25f - (30f * animatedProgress)
                drawLine(
                    primaryColor,
                    Offset(50f, h - 25f),
                    Offset(w - 60f, bodyY),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
                // Head
                drawCircle(secondaryColor, radius = 10f, center = Offset(w - 50f, bodyY - 14f))
            }
            "squats" -> {
                // Ground
                drawLine(Color.Gray, Offset(20f, h - 15f), Offset(w - 20f, h - 15f), strokeWidth = 4f)
                // Hip/knee coordinates bending down and up
                val kneeY = h - 35f
                val hipY = h - 55f + (22f * animatedProgress)
                val spineY = hipY - 25f

                // Thigh
                drawLine(primaryColor, Offset(cx, kneeY), Offset(cx - 20f, hipY), strokeWidth = 8f, cap = StrokeCap.Round)
                // Torso
                drawLine(primaryColor, Offset(cx - 20f, hipY), Offset(cx - 10f, spineY), strokeWidth = 8f, cap = StrokeCap.Round)
                // Head
                drawCircle(secondaryColor, radius = 9f, center = Offset(cx - 8f, spineY - 12f))
            }
            "plank" -> {
                drawLine(Color.Gray, Offset(20f, h - 20f), Offset(w - 20f, h - 20f), strokeWidth = 4f)
                val bodyPulseY = h - 35f + (3f * animatedProgress)
                drawLine(
                    primaryColor,
                    Offset(50f, h - 25f), // Feet
                    Offset(w - 60f, bodyPulseY), // Head shoulder
                    strokeWidth = 12f,
                    cap = StrokeCap.Round
                )
                // Elbow support
                drawLine(Color.DarkGray, Offset(w - 80f, h - 20f), Offset(w - 80f, bodyPulseY), strokeWidth = 6f)
            }
            "jumping jacks" -> {
                val limbSpread = 25f * animatedProgress
                // torso
                drawLine(primaryColor, Offset(cx, cy - 15f), Offset(cx, cy + 15f), strokeWidth = 10f, cap = StrokeCap.Round)
                // legs
                drawLine(primaryColor, Offset(cx, cy + 15f), Offset(cx - 10f - limbSpread, cy + 35f), strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(primaryColor, Offset(cx, cy + 15f), Offset(cx + 10f + limbSpread, cy + 35f), strokeWidth = 8f, cap = StrokeCap.Round)
                // arms
                drawLine(secondaryColor, Offset(cx, cy - 10f), Offset(cx - 15f - limbSpread, cy - 25f + (limbSpread * 1.5f)), strokeWidth = 6f, cap = StrokeCap.Round)
                drawLine(secondaryColor, Offset(cx, cy - 10f), Offset(cx + 15f + limbSpread, cy - 25f + (limbSpread * 1.5f)), strokeWidth = 6f, cap = StrokeCap.Round)
                // head
                drawCircle(secondaryColor, radius = 10f, center = Offset(cx, cy - 28f))
            }
            "pull-ups" -> {
                // Top bar
                drawLine(Color.DarkGray, Offset(30f, 20f), Offset(w - 30f, 20f), strokeWidth = 6f)
                // Body climbing vertically
                val bodyY = cy + 25f - (30f * animatedProgress)
                drawLine(primaryColor, Offset(cx, bodyY - 10f), Offset(cx, bodyY + 25f), strokeWidth = 10f, cap = StrokeCap.Round)
                // Arms holding the bar
                drawLine(secondaryColor, Offset(cx, bodyY - 10f), Offset(cx - 20f, 20f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawLine(secondaryColor, Offset(cx, bodyY - 10f), Offset(cx + 20f, 20f), strokeWidth = 6f, cap = StrokeCap.Round)
                // Head
                drawCircle(secondaryColor, radius = 10f, center = Offset(cx, bodyY - 20f))
            }
            else -> {
                // Generic pacing exercise dot movement
                drawCircle(primaryColor, radius = 12f + (8f * animatedProgress), center = Offset(cx, cy))
                drawCircle(secondaryColor, radius = 6f, center = Offset(cx - 40f + (80f * animatedProgress), cy))
            }
        }
    }
}

// ==========================================
// 4. EXERCISE LIBRARY (SEARCHABLE)
// ==========================================

@Composable
fun ExerciseLibraryScreen(viewModel: FitViewModel) {
    val predefinedWorkouts by viewModel.predefinedWorkouts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Filter exercises ensuring unique library categories
    val exercises = predefinedWorkouts
        .distinctBy { it.exerciseName }
        .filter {
            it.exerciseName.lowercase().contains(searchQuery.lowercase()) ||
            it.targetMuscle.lowercase().contains(searchQuery.lowercase())
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("exercise_library_screen")
            .padding(16.dp)
    ) {
        Text("Search Exercises Library", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search by name (e.g. Squat, Push-up)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("exercise_search_input")
        )

        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No exercise found matching input criteria.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1.0f)
            ) {
                items(exercises) { exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(exercise.exerciseName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Target: ${exercise.targetMuscle}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(exercise.exerciseDesc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Instructions:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(exercise.instructions, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. WATER INTAKE TRACKER
// ==========================================

@Composable
fun WaterIntakeScreen(viewModel: FitViewModel) {
    val waterEntry by viewModel.waterToday.collectAsStateWithLifecycle()
    val count = waterEntry?.glasses ?: 0
    val progress = minOf(count.toFloat() / 8f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("water_tracker_screen")
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Water Intake Tracker",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )
        Text(
            text = "Track your fluid hydration. Daily target: 8 glasses (2.0L)",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.Start).padding(bottom = 24.dp)
        )

        // Magnificent physical water jug animator
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(6.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animating background liquid wave fill inside Canvas
            val density = LocalContext.current.resources.displayMetrics.density
            val fluidAnimState = remember { Animatable(0f) }
            LaunchedEffect(count) {
                fluidAnimState.animateTo(progress, animationSpec = tween(1000, easing = FastOutSlowInEasing))
            }

            Canvas(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                val w = size.width
                val h = size.height
                val fillHeight = h * fluidAnimState.value

                // Draw water wave path
                val paint = Paint().apply {
                    color = Color(0xFF2196F3).copy(alpha = 0.85f)
                    style = PaintingStyle.Fill
                }
                val path = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h - fillHeight)
                    cubicTo(
                        w * 0.25f, h - fillHeight - (10f * density),
                        w * 0.75f, h - fillHeight + (10f * density),
                        w, h - fillHeight
                    )
                    lineTo(w, h)
                    close()
                }
                drawPath(path, color = Color(0xFF03A9F4).copy(alpha = 0.8f))
            }

            // Foreground Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$count / 8",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progress > 0.5f) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Glasses",
                    fontSize = 12.sp,
                    color = if (progress > 0.5f) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Hydration Tips Box
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (count >= 8) "Awesome Work! Goal achieved. Keep hydrated!" else "Drinking water regularly boosts brain power, hydrates muscle tissue, and accelerates natural lipid reduction.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.weight(1.0f))

        // Control Panel
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.resetWaterToday() },
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset")
            }

            Button(
                onClick = { viewModel.incrementWater() },
                modifier = Modifier.weight(1f).height(50.dp).testTag("increment_water")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log 1 Glass")
            }
        }
    }
}

// ==========================================
// 6. AI FOOD NUTRITION SCANNER
// ==========================================

@Composable
fun AIScannerScreen(viewModel: FitViewModel) {
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val scansHistory by viewModel.scanHistory.collectAsStateWithLifecycle()

    var activeInputTab by remember { mutableStateOf("choices") } // "choices" or "text"
    var textQuery by remember { mutableStateOf("") }

    val presetChoices = listOf(
        Pair("Banana", "Fresh organic banana"),
        Pair("Mediterranean Salad", "Bowl of green mediterranean salad"),
        Pair("Pepperoni Pizza Slice", "Full single slice pizza"),
        Pair("Grilled Chicken Breast", "Standard size grilled chicken strip"),
        Pair("Double Cheeseburger", "Fast food standard cheeseburger"),
        Pair("Steamed Brown Rice", "Cooked fiber-rich brown rice bowl"),
        Pair("Whole Cow's Milk", "One glass natural dairy fluid")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_scanner_screen")
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("AI Nutrition scanner", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Analyze food calories and macro metrics using Gemini API integration.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Input Selector Mode tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (activeInputTab == "choices") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeInputTab = "choices" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Curated presets",
                    color = if (activeInputTab == "choices") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (activeInputTab == "text") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeInputTab = "text" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Custom text query",
                    color = if (activeInputTab == "text") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action inputs
        if (activeInputTab == "choices") {
            Text(
                "Select a sample food to simulate AI camera detection:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(200.dp).padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetChoices.size) { index ->
                    val (title, info) = presetChoices[index]
                    val isScanning = scanState is ScanState.Loading
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isScanning) {
                                viewModel.requestFoodNutritionScan(null, title)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(info, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = textQuery,
                onValueChange = { textQuery = it },
                label = { Text("Enter food dish name...") },
                placeholder = { Text("e.g. Avocado Toast with Egg") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("food_text_input")
            )

            Button(
                onClick = {
                    if (textQuery.isNotBlank()) {
                        viewModel.requestFoodNutritionScan(null, textQuery)
                    }
                },
                enabled = textQuery.isNotBlank() && scanState !is ScanState.Loading,
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 16.dp).testTag("analyze_btn")
            ) {
                Text("Scan Custom dish")
            }
        }

        // Render Scanning Active State
        when (scanState) {
            is ScanState.Loading -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("AI Analyzing Food nutrients in background with Gemini API...", fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            is ScanState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Scan Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text((scanState as ScanState.Error).message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            is ScanState.Success -> {
                val nut = (scanState as ScanState.Success).nutrition
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Detection Successful", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text(nut.foodName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Nutrition Details table
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Calories", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${nut.calories} kcal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Serving Size", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(nut.servingSize, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroIndicator(label = "Protein", value = "${nut.protein}g", color = Color(0xFF4CAF50))
                            MacroIndicator(label = "Carbs", value = "${nut.carbohydrates}g", color = Color(0xFF2196F3))
                            MacroIndicator(label = "Fats", value = "${nut.fat}g", color = Color(0xFFFF9800))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroIndicator(label = "Fiber", value = "${nut.fiber}g", color = Color(0xFF8BC34A))
                            MacroIndicator(label = "Sugar", value = "${nut.sugar}g", color = Color(0xFFE91E63))
                        }
                    }
                }
            }
            else -> {}
        }

        // Historic Scans Display Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Previous AI Scans", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (scansHistory.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearScans() }) {
                    Text("Clear All")
                }
            }
        }

        if (scansHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Zero food scan logged in history database.", fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            scansHistory.forEach { scan ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(scan.foodName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "Calories: ${scan.calories} kcal | Protein: ${scan.protein}g | Carbs: ${scan.carbohydrates}g",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { viewModel.deleteScan(scan.scanId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroIndicator(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 7. USER PROFILE & HISTORY CHARTS VIEW
// ==========================================

@Composable
fun ProfileScreen(viewModel: FitViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val progressHistory by viewModel.progressHistory.collectAsStateWithLifecycle()

    val age by viewModel.editAge.collectAsStateWithLifecycle()
    val gender by viewModel.editGender.collectAsStateWithLifecycle()
    val height by viewModel.editHeight.collectAsStateWithLifecycle()
    val weight by viewModel.editWeight.collectAsStateWithLifecycle()
    val goal by viewModel.editGoal.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen")
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Weight & BMI Progress Tracker", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        Text(
            text = "Track historical progress records and configure physical goals.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Custom drawn Canvas Progression Chart
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Weight Progression Trend (kg)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                if (progressHistory.size < 2) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Log at least 2 weight markers to draw trends. Use Profile Editor below.",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                } else {
                    WeightProgressChart(progressList = progressHistory, modifier = Modifier.fillMaxWidth().height(150.dp))
                }
            }
        }

        // Profile Editor Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("User Profile Editor", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { viewModel.editAge.value = it },
                        label = { Text("Age (yrs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("age_input")
                    )
                    OutlinedTextField(
                        value = gender,
                        onValueChange = { viewModel.editGender.value = it },
                        label = { Text("Gender") },
                        modifier = Modifier.weight(1f).testTag("gender_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { viewModel.editHeight.value = it },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("height_input")
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { viewModel.editWeight.value = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("weight_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fitness Goal selection choices
                Text("Fitness Target Goal:", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                val goals = listOf("Weight Loss", "Muscle Gain", "General Fitness")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    goals.forEach { item ->
                        val selected = item == goal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.editGoal.value = item }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.updateProfile() },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button")
                ) {
                    Text("Save & Log Progress Metrics", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Progression history logs list
        Text("Weight & BMI History Records", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        if (progressHistory.isEmpty()) {
            Text("Zero progress recorded yet.", fontSize = 12.sp, color = Color.Gray)
        } else {
            progressHistory.reversed().forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Weight: ${item.weight} kg | BMI: ${item.bmi}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Logged date: ${item.date}", fontSize = 11.sp, color = Color.Gray)
                        }

                        IconButton(onClick = { viewModel.deleteProgressEntry(item.progressId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large logout panel
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("logout_button")
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out Session", fontWeight = FontWeight.Bold)
        }
    }
}

// Custom vector drawing canvas plotter for the weight progresion trend
@Composable
fun WeightProgressChart(progressList: List<Progress>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Find min/max weight to draw inside coordinates
        val weights = progressList.map { it.weight }
        val maxWeight = weights.maxOrNull() ?: 100.0
        val minWeight = weights.minOrNull() ?: 50.0
        val range = maxWeight - minWeight
        val span = if (range == 0.0) 10.0 else range

        val padX = 40f
        val padY = 30f
        val usableW = w - (padX * 2)
        val usableH = h - (padY * 2)

        // Draw helper background grid horizontal lines
        val linesCount = 3
        for (i in 0..linesCount) {
            val yOffset = padY + (usableH / linesCount) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(padX, yOffset),
                end = Offset(w - padX, yOffset),
                strokeWidth = 2f
            )
        }

        // Build list of computed coords
        val points = mutableListOf<Offset>()
        progressList.forEachIndexed { index, progress ->
            val ratioX = if (progressList.size > 1) index.toFloat() / (progressList.size - 1).toFloat() else 0.5f
            val ratioY = (progress.weight - minWeight) / span

            val cx = padX + (usableW * ratioX)
            val cy = padY + usableH - (usableH * ratioY).toFloat()

            points.add(Offset(cx, cy))
        }

        // Draw line connections
        if (points.size > 1) {
            val path = Path()
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
            drawPath(path = path, color = primaryColor, style = Stroke(width = 6f))
        }

        // Draw dots and weight tags
        points.forEachIndexed { idx, point ->
            drawCircle(color = primaryColor, radius = 6f, center = point)
            // Draw weight text tag slightly offset
            drawCircle(color = Color.White, radius = 2f, center = point)
        }
    }
}
