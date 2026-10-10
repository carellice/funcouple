package com.funcouple.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor

class MainActivity : ComponentActivity() {
    private val state by lazy { AppState(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        TogetherWidget.refresh(applicationContext)
        setContent { FunCoupleApp(state) }
    }

    override fun onResume() {
        super.onResume()
        if (state.depth) state.tilt.start()
    }

    override fun onPause() {
        super.onPause()
        state.tilt.stop()
    }

    override fun onStop() {
        super.onStop()
        // Con il PIN attivo, l'app si blocca ogni volta che va in secondo piano.
        state.lock()
    }
}

sealed interface Screen {
    data object Home : Screen
    data object Challenges : Screen
    data object Kamasutra : Screen
    data class Detail(val id: String) : Screen
    data object Dice : Screen
    data object Wheel : Screen
    data object Settings : Screen
    data object Never : Screen
    data object Foreplay : Screen
    data object Coupons : Screen
    data object CustomCards : Screen
    data object Limits : Screen
    data object PinSetup : Screen
    data object Marathon : Screen
}

@Composable
fun FunCoupleApp(state: AppState) {
    FunCoupleTheme {
        CompositionLocalProvider(LocalSounds provides state.sounds, LocalTilt provides state.tilt) {
        Box(Modifier.fillMaxSize()) {
            AnimatedBackground()
            // La pila delle schermate vive qui, così sopravvive al blocco con PIN.
            val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
            val root = when {
                state.locked -> 0
                state.onboarded -> 2
                else -> 1
            }
            Crossfade(root, animationSpec = tween(500), label = "root") { shown ->
                when (shown) {
                    0 -> LockScreen(state)
                    1 -> OnboardingScreen(state)
                    else -> MainNavigation(state, stack)
                }
            }
            // "Luci basse": un velo scuro e caldo sopra tutto, che non intercetta i tocchi.
            if (state.dim) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(ComposeColor(0xFF12000A).copy(alpha = 0.4f))
                    drawRect(ComposeColor(0xFFFF2040).copy(alpha = 0.06f))
                }
            }
        }
        }
    }
}

@Composable
private fun MainNavigation(state: AppState, stack: MutableList<Screen>) {
    var forward by remember { mutableStateOf(true) }
    fun push(s: Screen) {
        forward = true
        stack.add(s)
    }
    fun pop() {
        if (stack.size > 1) {
            forward = false
            stack.removeAt(stack.lastIndex)
        }
    }
    fun replace(s: Screen) {
        forward = true
        stack[stack.lastIndex] = s
    }
    BackHandler(stack.size > 1) { pop() }

    AnimatedContent(
        targetState = stack.last(),
        transitionSpec = {
            val dir = if (forward) 1 else -1
            (slideInHorizontally(tween(380)) { it * dir / 3 } + fadeIn(tween(380)) +
                scaleIn(tween(380), initialScale = 0.94f)) togetherWith
                (slideOutHorizontally(tween(300)) { -it * dir / 3 } + fadeOut(tween(220)))
        },
        label = "nav",
    ) { screen ->
        when (screen) {
            Screen.Home -> HomeScreen(state, ::push)
            Screen.Challenges -> ChallengesScreen(state, ::pop)
            Screen.Kamasutra -> KamasutraScreen(state, ::pop, { push(Screen.Marathon) }) { push(Screen.Detail(it)) }
            is Screen.Detail -> PositionDetailScreen(state, screen.id, ::pop) { replace(Screen.Detail(it)) }
            Screen.Dice -> DiceScreen(state, ::pop)
            Screen.Wheel -> WheelScreen(state, ::pop) { push(Screen.Detail(it)) }
            Screen.Settings -> SettingsScreen(state, ::pop, ::push)
            Screen.Never -> NeverScreen(state, ::pop)
            Screen.Foreplay -> ForeplayScreen(state, ::pop)
            Screen.Coupons -> CouponsScreen(state, ::pop)
            Screen.CustomCards -> CustomCardsScreen(state, ::pop)
            Screen.Limits -> LimitsScreen(state, ::pop)
            Screen.PinSetup -> PinSetupScreen(state, ::pop)
            Screen.Marathon -> MarathonScreen(state, ::pop)
        }
    }
}
