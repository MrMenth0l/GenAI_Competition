package com.example.genai_competition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.genai_competition.ui.chat.ChatScreen
import com.example.genai_competition.ui.chat.TutorViewModel
import com.example.genai_competition.ui.overview.CourseOverviewScreen
import com.example.genai_competition.ui.theme.GenAI_CompetitionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GenAI_CompetitionTheme {
                TutorApp()
            }
        }
    }
}

@Composable
fun TutorApp(viewModel: TutorViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.Overview
        ) {
            composable(Routes.Overview) {
                CourseOverviewScreen(
                    courses = uiState.courses,
                    onCourseSelected = { courseState ->
                        viewModel.selectCourse(courseState.course.id)
                        navController.navigate("${Routes.Chat}/${courseState.course.id}") {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = "${Routes.Chat}/{courseId}",
                arguments = listOf(navArgument("courseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId")
                val courseState = uiState.courses.firstOrNull { it.course.id == courseId }
                if (courseId == null || courseState == null) {
                    LaunchedEffect(courseId) {
                        navController.popBackStack()
                    }
                } else {
                    ChatScreen(
                        state = courseState,
                        onBack = { navController.popBackStack() },
                        onSendMessage = { message, attachments ->
                            viewModel.sendMessage(courseId, message, attachments)
                        },
                        onDismissError = { viewModel.clearError(courseId) }
                    )
                }
            }
        }
    }
}

private object Routes {
    const val Overview = "overview"
    const val Chat = "chat"
}
