package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.StageProgressStepper
import com.example.ui.components.StudioStage
import com.example.ui.screens.AiAnalysisScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.FineTuneScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KeyframeTimelineScreen
import com.example.ui.screens.PreviewScreen
import com.example.ui.screens.ReferenceVideoScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TargetVideoScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                StudioApp()
            }
        }
    }
}

@Composable
fun StudioApp(viewModel: StudioViewModel = viewModel()) {
    val currentStage by viewModel.currentStage.collectAsState()
    val completedStages by viewModel.completedStages.collectAsState()
    val projectName by viewModel.projectName.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var inSettings by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    BackHandler(enabled = inSettings || currentStage != StudioStage.HOME) {
        if (inSettings) {
            inSettings = false
        } else {
            when (currentStage) {
                StudioStage.REFERENCE, StudioStage.TARGET -> viewModel.setStage(StudioStage.HOME)
                StudioStage.ANALYSIS -> viewModel.setStage(StudioStage.TARGET)
                StudioStage.TIMELINE, StudioStage.PREVIEW -> viewModel.setStage(StudioStage.ANALYSIS)
                StudioStage.FINE_TUNE -> viewModel.setStage(StudioStage.PREVIEW)
                StudioStage.RENDER_EXPORT -> viewModel.setStage(StudioStage.PREVIEW)
                StudioStage.HOME -> { /* Exit app */ }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBackground),
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!inSettings) {
                Surface(
                    color = StudioSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StudioCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Logo",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "AI Keyframe Studio",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioTextPrimary
                                    )
                                    Text(
                                        text = projectName,
                                        fontSize = 11.sp,
                                        color = StudioCyan
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentStage != StudioStage.HOME) {
                                    IconButton(
                                        onClick = { viewModel.setStage(StudioStage.HOME) },
                                        modifier = Modifier.testTag("topbar_home_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "Home",
                                            tint = StudioTextSecondary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { inSettings = true },
                                    modifier = Modifier.testTag("topbar_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = StudioTextSecondary
                                    )
                                }
                            }
                        }

                        // Stepper row when inside edit workflow
                        if (currentStage != StudioStage.HOME) {
                            Spacer(modifier = Modifier.height(8.dp))
                            StageProgressStepper(
                                currentStage = currentStage,
                                completedStages = completedStages,
                                onStageSelected = { viewModel.setStage(it) }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!inSettings) {
                NavigationBar(
                    containerColor = StudioSurface,
                    contentColor = StudioTextPrimary,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentStage == StudioStage.HOME,
                        onClick = { viewModel.setStage(StudioStage.HOME) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Projects", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = StudioCyan,
                            indicatorColor = StudioCyan,
                            unselectedIconColor = StudioTextSecondary,
                            unselectedTextColor = StudioTextSecondary
                        )
                    )

                    val refMeta by viewModel.referenceMetadata.collectAsState()
                    val targetMeta by viewModel.targetMetadata.collectAsState()

                    NavigationBarItem(
                        selected = currentStage == StudioStage.REFERENCE || currentStage == StudioStage.TARGET || currentStage == StudioStage.ANALYSIS,
                        onClick = {
                            if (refMeta == null) {
                                viewModel.setStage(StudioStage.REFERENCE)
                            } else if (targetMeta == null) {
                                viewModel.setStage(StudioStage.TARGET)
                            } else {
                                viewModel.setStage(StudioStage.ANALYSIS)
                            }
                        },
                        icon = { Icon(Icons.Default.Movie, contentDescription = "Workflow") },
                        label = { Text("Workflow", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = StudioCyan,
                            indicatorColor = StudioCyan,
                            unselectedIconColor = StudioTextSecondary,
                            unselectedTextColor = StudioTextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentStage == StudioStage.TIMELINE || currentStage == StudioStage.FINE_TUNE,
                        onClick = { viewModel.setStage(StudioStage.TIMELINE) },
                        icon = { Icon(Icons.Default.Diamond, contentDescription = "Timeline") },
                        label = { Text("Keyframes", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = StudioAmber,
                            indicatorColor = StudioAmber,
                            unselectedIconColor = StudioTextSecondary,
                            unselectedTextColor = StudioTextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentStage == StudioStage.PREVIEW,
                        onClick = { viewModel.setStage(StudioStage.PREVIEW) },
                        icon = { Icon(Icons.Default.PlayCircle, contentDescription = "Preview") },
                        label = { Text("Preview", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = StudioCyan,
                            indicatorColor = StudioCyan,
                            unselectedIconColor = StudioTextSecondary,
                            unselectedTextColor = StudioTextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentStage == StudioStage.RENDER_EXPORT,
                        onClick = { viewModel.setStage(StudioStage.RENDER_EXPORT) },
                        icon = { Icon(Icons.Default.FileDownload, contentDescription = "Export") },
                        label = { Text("Export", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = StudioEmerald,
                            indicatorColor = StudioEmerald,
                            unselectedIconColor = StudioTextSecondary,
                            unselectedTextColor = StudioTextSecondary
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
            if (inSettings) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { inSettings = false }
                )
            } else {
                when (currentStage) {
                    StudioStage.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { inSettings = true }
                    )
                    StudioStage.REFERENCE -> ReferenceVideoScreen(viewModel = viewModel)
                    StudioStage.TARGET -> TargetVideoScreen(viewModel = viewModel)
                    StudioStage.ANALYSIS -> AiAnalysisScreen(viewModel = viewModel)
                    StudioStage.TIMELINE -> KeyframeTimelineScreen(viewModel = viewModel)
                    StudioStage.PREVIEW -> PreviewScreen(viewModel = viewModel)
                    StudioStage.FINE_TUNE -> FineTuneScreen(viewModel = viewModel)
                    StudioStage.RENDER_EXPORT -> ExportScreen(viewModel = viewModel)
                }
            }
        }
    }
}
