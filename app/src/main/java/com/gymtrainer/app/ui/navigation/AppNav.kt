@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.gymtrainer.app.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymtrainer.app.data.Meal
import com.gymtrainer.app.data.NutritionRepository
import com.gymtrainer.app.ui.body.BodyScreen
import com.gymtrainer.app.ui.components.ProgressRing
import com.gymtrainer.app.ui.exercises.ExerciseDetailScreen
import com.gymtrainer.app.ui.exercises.ExercisesScreen
import com.gymtrainer.app.ui.home.GuidedWorkoutScreen
import com.gymtrainer.app.ui.home.HomeScreen
import com.gymtrainer.app.ui.home.HomeWorkoutPreviewScreen
import com.gymtrainer.app.ui.home.ProgramScreen
import com.gymtrainer.app.ui.nutrition.AddFoodScreen
import com.gymtrainer.app.ui.nutrition.NutritionScreen
import com.gymtrainer.app.ui.onboarding.OnboardingScreen
import com.gymtrainer.app.ui.profile.ProfileEditScreen
import com.gymtrainer.app.ui.profile.ProfileScreen
import com.gymtrainer.app.ui.progress.ProgressScreen
import com.gymtrainer.app.ui.rememberNutrition
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.theme.Brand
import com.gymtrainer.app.ui.tools.ToolsScreen
import com.gymtrainer.app.ui.train.CategoryScreen
import com.gymtrainer.app.ui.train.GymProgramScreen
import com.gymtrainer.app.ui.train.RoutineEditorScreen
import com.gymtrainer.app.ui.train.RoutineScreen
import com.gymtrainer.app.ui.train.TrainScreen
import com.gymtrainer.app.ui.videos.VideoTutorialScreen
import com.gymtrainer.app.ui.workout.ActiveWorkoutScreen
import kotlinx.coroutines.launch
import java.util.Calendar

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TRAIN = "train"
    const val NUTRITION = "nutrition"
    const val PROGRESS = "progress"
    const val EXERCISES = "exercises"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile/edit"
    const val EXERCISE_DETAIL = "exercise/{id}"
    const val VIDEOS = "videos/{id}"
    const val ACTIVE_WORKOUT = "workout/{dayId}"
    const val ROUTINE = "routine/{dayId}"
    const val HOME_PREVIEW = "homeworkout/{key}"
    const val HOME_PLAY = "play/{key}"
    const val PROGRAM = "program/{id}"
    const val GYM_PROGRAM = "gymprogram/{id}"
    const val CATEGORY = "category/{cat}"
    const val GYM_CATEGORY = "gymcat/{cat}"
    const val GYM_PLAN = "gymplan"
    const val VIDEO_SEARCH = "videosearch/{q}"
    const val VOLUME = "volume"
    const val ROUTINE_NEW = "newroutine"
    const val TOOLS = "tools"
    const val ADD_FOOD = "food/{meal}/{day}"
    const val BODY = "body"

    fun exerciseDetail(id: String) = "exercise/${Uri.encode(id)}"
    fun videos(id: String) = "videos/${Uri.encode(id)}"
    fun activeWorkout(dayId: Long) = "workout/$dayId"
    fun routine(dayId: Long) = "routine/$dayId"
    fun homePreview(key: String) = "homeworkout/${Uri.encode(key)}"
    fun homePlay(key: String) = "play/${Uri.encode(key)}"
    fun program(id: String) = "program/${Uri.encode(id)}"
    fun challenge(id: String) = program(id)
    fun gymProgram(id: String) = "gymprogram/$id"
    fun category(cat: com.gymtrainer.app.domain.WorkoutCategory) = "category/${cat.name}"
    fun gymCategory(cat: com.gymtrainer.app.domain.GymCategory) = "gymcat/${cat.name}"
    fun videoSearch(query: String) = "videosearch/${Uri.encode(query)}"
    fun addFood(meal: Meal, day: Long) = "food/${meal.name}/$day"
}

private val dockTabs = listOf(
    DockTab(Routes.HOME, "Today", Icons.Filled.Today),
    DockTab(Routes.TRAIN, "Workouts", Icons.Filled.FitnessCenter),
    DockTab(Routes.NUTRITION, "Nutrition", Icons.Filled.Restaurant),
    DockTab(Routes.PROGRESS, "Progress", Icons.Filled.Insights)
)

/** Asks the tab pager to show [route]'s page (set from anywhere, e.g. "See all" buttons). */
object TabSwitcher {
    val request = mutableStateOf<Int?>(null)
}

fun NavController.goToTab(route: String) {
    val index = dockTabs.indexOfFirst { it.route == route }
    if (index >= 0) TabSwitcher.request.value = index
    if (currentDestination?.route != Routes.HOME) popBackStack(Routes.HOME, inclusive = false)
}

/** Meal that fits the current time of day. */
private fun mealNow(): Meal = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..10 -> Meal.BREAKFAST
    in 11..15 -> Meal.LUNCH
    in 18..22 -> Meal.DINNER
    else -> Meal.SNACK
}

@Composable
fun AppRoot() {
    val repo = rememberRepository()
    val nutrition = rememberNutrition()
    var loaded by remember { mutableStateOf(false) }
    var hasProfile by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        repo.ensureSeeded()
        hasProfile = repo.getProfile() != null
        loaded = true
    }

    // Quiet launch check for a newer release on GitHub (at most every 12 h).
    val context = androidx.compose.ui.platform.LocalContext.current
    var update by remember { mutableStateOf<com.gymtrainer.app.data.AppUpdate?>(null) }
    LaunchedEffect(Unit) {
        if (com.gymtrainer.app.ui.components.MotionPrefs.allowNetwork && com.gymtrainer.app.data.Updates.dueForCheck(context)) {
            update = com.gymtrainer.app.data.Updates.check(context)?.takeIf { !com.gymtrainer.app.data.Updates.isSkipped(context, it.version) }
        }
    }
    update?.let { com.gymtrainer.app.ui.components.UpdateDialog(it) { update = null } }

    if (!loaded) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            ProgressRing(0.35f, size = 48.dp)
        }
        return
    }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showDock = currentRoute == Routes.HOME
    val pager = rememberPagerState { dockTabs.size }
    val tabSpec = tween<Float>(durationMillis = 380, easing = FastOutSlowInEasing)
    val requested by TabSwitcher.request
    LaunchedEffect(requested) {
        requested?.let { pager.animateScrollToPage(it, animationSpec = tabSpec); TabSwitcher.request.value = null }
    }
    // Back on another tab goes to Today first, like most Android apps.
    BackHandler(enabled = showDock && pager.currentPage != 0) { scope.launch { pager.animateScrollToPage(0, animationSpec = tabSpec) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(
                visible = showDock,
                enter = slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(180))
            ) {
                FloatingDock(dockTabs, pager.currentPage + pager.currentPageOffsetFraction,
                    onTab = { i -> scope.launch { pager.animateScrollToPage(i, animationSpec = tabSpec) } },
                    onAction = { showActions = true })
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (hasProfile) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn() }, exitTransition = { fadeOut() }
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = { nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } } })
            }
            // The four main tabs live in one pager: swipe between them or tap the dock.
            composable(Routes.HOME) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize(),
                    key = { dockTabs[it].route },
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pager,
                        snapAnimationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    )
                ) { page ->
                    Box(Modifier.fillMaxSize().graphicsLayer {
                        // Gentle depth: the page being left fades and shrinks a little.
                        val offset = abs((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
                        alpha = 1f - 0.25f * offset
                        val sc = 1f - 0.04f * offset
                        scaleX = sc; scaleY = sc
                    }) {
                        when (dockTabs[page].route) {
                            Routes.HOME -> HomeScreen(contentPadding = padding, nav = nav)
                            Routes.TRAIN -> TrainScreen(contentPadding = padding, nav = nav)
                            Routes.NUTRITION -> NutritionScreen(contentPadding = padding, onAddFood = { meal, day -> nav.navigate(Routes.addFood(meal, day)) })
                            else -> ProgressScreen(contentPadding = padding, onBody = { nav.navigate(Routes.BODY) }, onVolume = { nav.navigate(Routes.VOLUME) })
                        }
                    }
                }
            }
            composable(Routes.EXERCISES) { ExercisesScreen(contentPadding = padding, nav = nav, onBack = { nav.popBackStack() }) }
            composable(Routes.PROFILE) {
                ProfileScreen(contentPadding = padding, onEditProfile = { nav.navigate(Routes.PROFILE_EDIT) },
                    onTools = { nav.navigate(Routes.TOOLS) }, onBack = { nav.popBackStack() })
            }
            composable(Routes.PROFILE_EDIT) { ProfileEditScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() }) }
            composable(Routes.EXERCISE_DETAIL) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                ExerciseDetailScreen(exerciseId = id, onBack = { nav.popBackStack() }, onVideos = { nav.navigate(Routes.videos(id)) })
            }
            composable(Routes.VIDEOS) { entry ->
                val picked by entry.savedStateHandle.getStateFlow<String?>("picked", null).collectAsState()
                VideoTutorialScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() },
                    onSearch = { nav.navigate(Routes.videoSearch(it)) },
                    picked = picked, onPickedHandled = { entry.savedStateHandle["picked"] = null })
            }
            composable(Routes.VIDEO_SEARCH) { entry ->
                com.gymtrainer.app.ui.videos.VideoSearchScreen(entry.arguments?.getString("q").orEmpty(), onBack = { nav.popBackStack() },
                    onPicked = { id ->
                        nav.previousBackStackEntry?.savedStateHandle?.set("picked", id)
                        nav.popBackStack()
                    })
            }
            composable(Routes.ROUTINE) { entry ->
                val dayId = entry.arguments?.getString("dayId")?.toLongOrNull() ?: -1L
                RoutineScreen(dayId, onBack = { nav.popBackStack() },
                    onStart = { nav.navigate(Routes.activeWorkout(dayId)) { popUpTo(Routes.ROUTINE) { inclusive = true } } },
                    onExercise = { nav.navigate(Routes.exerciseDetail(it)) })
            }
            composable(Routes.ACTIVE_WORKOUT) { entry ->
                val dayId = entry.arguments?.getString("dayId")?.toLongOrNull() ?: -1L
                ActiveWorkoutScreen(dayId = dayId, onFinish = { nav.popBackStack() }, onVideo = { nav.navigate(Routes.videos(it)) })
            }
            composable(Routes.HOME_PREVIEW) { entry ->
                val key = entry.arguments?.getString("key").orEmpty()
                HomeWorkoutPreviewScreen(
                    workoutKey = key, onBack = { nav.popBackStack() },
                    onStart = { nav.navigate(Routes.homePlay(key)) },
                    onExercise = { nav.navigate(Routes.exerciseDetail(it)) }
                )
            }
            composable(Routes.HOME_PLAY) { entry ->
                val key = entry.arguments?.getString("key").orEmpty()
                GuidedWorkoutScreen(workoutKey = key, onClose = { nav.popBackStack(Routes.HOME_PREVIEW, inclusive = true) },
                    onVideo = { nav.navigate(Routes.videos(it)) })
            }
            composable(Routes.PROGRAM) { entry ->
                ProgramScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() },
                    onOpenDay = { key -> nav.navigate(Routes.homePreview(key)) })
            }
            composable(Routes.GYM_PROGRAM) { entry ->
                GymProgramScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }, onAdopted = { nav.popBackStack() })
            }
            composable(Routes.GYM_CATEGORY) { entry ->
                com.gymtrainer.app.ui.train.GymCategoryScreen(entry.arguments?.getString("cat").orEmpty(), onBack = { nav.popBackStack() },
                    onOpen = { nav.navigate(Routes.routine(it)) })
            }
            composable(Routes.VOLUME) {
                com.gymtrainer.app.ui.train.VolumeScreen(onBack = { nav.popBackStack() },
                    onCategory = { nav.navigate(Routes.gymCategory(it)) })
            }
            composable(Routes.GYM_PLAN) {
                com.gymtrainer.app.ui.train.GymPlanScreen(onBack = { nav.popBackStack() },
                    onStart = { nav.navigate(Routes.routine(it)) }, onPrograms = { nav.popBackStack() },
                    onVolume = { nav.navigate(Routes.VOLUME) })
            }
            composable(Routes.CATEGORY) { entry ->
                CategoryScreen(entry.arguments?.getString("cat").orEmpty(), onBack = { nav.popBackStack() },
                    onOpen = { nav.navigate(Routes.homePreview(it)) })
            }
            composable(Routes.ADD_FOOD) { entry ->
                AddFoodScreen(entry.arguments?.getString("meal").orEmpty(),
                    entry.arguments?.getString("day")?.toLongOrNull() ?: NutritionRepository.today(),
                    onDone = { nav.popBackStack() })
            }
            composable(Routes.BODY) { BodyScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.ROUTINE_NEW) { RoutineEditorScreen(onDone = { nav.popBackStack() }) }
            composable(Routes.TOOLS) { ToolsScreen(onBack = { nav.popBackStack() }) }
        }
    }

    if (showActions) {
        QuickActionSheet(onDismiss = { showActions = false }, actions = listOf(
            QuickAction("7-min workout", "No equipment", QuickIcons.workout, Brand.Blue) { nav.navigate(Routes.homePreview("seven")) },
            QuickAction("Log a meal", mealNow().label, QuickIcons.meal, Brand.Orange) { nav.navigate(Routes.addFood(mealNow(), NutritionRepository.today())) },
            QuickAction("Add water", "+250 ml", QuickIcons.water, Brand.Teal) {
                scope.launch {
                    nutrition.addWater(NutritionRepository.today(), 250)
                    snackbar.showSnackbar("Added 250 ml of water 💧")
                }
            },
            QuickAction("Body stats", "Weight & sizes", QuickIcons.weight, Brand.Purple) { nav.navigate(Routes.BODY) },
            QuickAction("Exercises", "880+ moves", QuickIcons.library, Brand.Green) { nav.navigate(Routes.EXERCISES) },
            QuickAction("New routine", "Build your own", QuickIcons.routine, Brand.Pink) { nav.navigate(Routes.ROUTINE_NEW) }
        ))
    }
}

@Suppress("unused")
private val transparent = Color.Transparent
