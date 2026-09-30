package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.components.CinematicCard
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioRed
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun SettingsScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customApiKey by viewModel.customApiKey.collectAsState()
    val customBackendUrl by viewModel.customBackendUrl.collectAsState()
    val forceDemoMode by viewModel.forceDemoMode.collectAsState()
    val scrollState = rememberScrollState()

    var apiKeyInput by remember(customApiKey) { mutableStateOf(customApiKey) }
    var backendUrlInput by remember(customBackendUrl) { mutableStateOf(customBackendUrl) }
    var showSavedMessage by remember { mutableStateOf(false) }

    val buildConfigKeyExists = try {
        BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    } catch (_: Exception) {
        false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = StudioCyan
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings & AI Configuration",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StudioTextPrimary
            )
        }

        // Demo Mode / Backend Mode Card
        CinematicCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Science, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Synthetic Demo Analysis Mode",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = "Use offline synthetic motion curves without cloud API",
                                fontSize = 11.sp,
                                color = StudioTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = forceDemoMode,
                        onCheckedChange = { viewModel.setForceDemoMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioAmber,
                            checkedTrackColor = StudioAmber.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("demo_mode_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurfaceHighlight, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (forceDemoMode) {
                            "DEMO MODE ACTIVE: Videos are analyzed locally using synthetic motion curves. No external API calls are made."
                        } else if (buildConfigKeyExists || customApiKey.isNotBlank()) {
                            "CLOUD AI ACTIVE: Connected to Gemini Multimodal AI for live video motion and optical flow analysis."
                        } else {
                            "DEMO FALLBACK: No Gemini API Key detected. Analysis defaults to Demo Mode until an API key or backend is provided."
                        },
                        fontSize = 11.sp,
                        color = if (forceDemoMode) StudioAmber else if (buildConfigKeyExists || customApiKey.isNotBlank()) StudioEmerald else StudioCyan,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // AI Engine Credentials
        CinematicCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI API Configuration",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                }

                Text(
                    text = "Configure Gemini API key or a custom AI motion-extraction server. Secrets are stored securely in app private storage or via BuildConfig.",
                    fontSize = 11.sp,
                    color = StudioTextSecondary
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Gemini API Key (AI_API_KEY)") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                )

                OutlinedTextField(
                    value = backendUrlInput,
                    onValueChange = { backendUrlInput = it },
                    label = { Text("Custom Backend URL (Optional)") },
                    placeholder = { Text("https://my-motion-ai-service.com/api") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("backend_url_input")
                )

                StudioPrimaryButton(
                    text = if (showSavedMessage) "SAVED!" else "SAVE CONFIGURATION",
                    onClick = {
                        viewModel.setCustomApiKey(apiKeyInput)
                        viewModel.setCustomBackendUrl(backendUrlInput)
                        showSavedMessage = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_settings_button"
                )
            }
        }

        // Privacy & Storage Management
        CinematicCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy & Local Storage",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                }

                Text(
                    text = "Your videos are private by default. All keyframe calculations and rendering occur locally on your device hardware.",
                    fontSize = 11.sp,
                    color = StudioTextSecondary
                )

                StudioSecondaryButton(
                    text = "Clear Temporary Render Cache",
                    icon = Icons.Default.CleaningServices,
                    onClick = { viewModel.clearTemporaryCache() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "clear_cache_button"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
