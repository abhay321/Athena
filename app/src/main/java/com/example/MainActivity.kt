package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrainViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: BrainViewModel = viewModel()
                val activeTab by viewModel.activeTab.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                                        )
                                    }
                                    Text(
                                        text = "Athena",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            letterSpacing = (-0.5).sp
                                        )
                                    )
                                }
                            },
                            actions = {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "JD",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.testTag("app_top_bar")
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .testTag("app_bottom_nav_bar")
                                .navigationBarsPadding()
                        ) {
                            NavigationBarItem(
                                selected = activeTab == "dashboard",
                                onClick = { viewModel.activeTab.value = "dashboard" },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == "dashboard") Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                        contentDescription = null
                                    )
                                },
                                label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_dashboard_tab")
                            )
                            NavigationBarItem(
                                selected = activeTab == "capture",
                                onClick = { viewModel.activeTab.value = "capture" },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == "capture") Icons.Filled.CenterFocusStrong else Icons.Outlined.CenterFocusWeak,
                                        contentDescription = null
                                    )
                                },
                                label = { Text("Capture", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_capture_tab")
                            )
                            NavigationBarItem(
                                selected = activeTab == "library",
                                onClick = { viewModel.activeTab.value = "library" },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == "library") Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                                        contentDescription = null
                                    )
                                },
                                label = { Text("Library", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_library_tab")
                            )
                            NavigationBarItem(
                                selected = activeTab == "chat",
                                onClick = { viewModel.activeTab.value = "chat" },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == "chat") Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null
                                    )
                                },
                                label = { Text("Brain Chat", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_chat_tab")
                            )
                            NavigationBarItem(
                                selected = activeTab == "settings",
                                onClick = { viewModel.activeTab.value = "settings" },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == "settings") Icons.Filled.SettingsInputComponent else Icons.Outlined.SettingsInputComponent,
                                        contentDescription = null
                                    )
                                },
                                label = { Text("Architecture", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_settings_tab")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (activeTab) {
                            "dashboard" -> DashboardScreen(viewModel = viewModel)
                            "capture" -> CaptureScreen(viewModel = viewModel)
                            "library" -> LibraryScreen(viewModel = viewModel)
                            "chat" -> ChatScreen(viewModel = viewModel)
                            "settings" -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

