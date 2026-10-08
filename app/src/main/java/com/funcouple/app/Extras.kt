package com.funcouple.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// --- Pezzi condivisi -------------------------------------------------------------------------

@Composable
fun Toggle(on: Boolean, colors: List<Color> = listOf(Fc.Pink, Fc.DeepViolet)) {
    val x by animateDpAsState(if (on) 22.dp else 2.dp, spring(dampingRatio = 0.6f, stiffness = 400f), label = "toggle")
    Box(
        Modifier
            .size(50.dp, 30.dp)
            .clip(CircleShape)
            .background(if (on) Brush.horizontalGradient(colors) else Brush.horizontalGradient(listOf(Fc.Glass, Fc.Glass)))
            .border(1.dp, Fc.GlassBorder, CircleShape),
    ) {
        Box(
            Modifier
                .offset(x = x, y = 2.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
fun IconTile(icon: FcIcon, colors: List<Color>, size: Dp = 48.dp) {
    Box(
        Modifier
            .size(size)
            .shadow(size * 0.12f, RoundedCornerShape(size * 0.32f), spotColor = colors.last())
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(colors))
            // Riflesso in alto e ombra in basso: la tessera sembra bombata.
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.32f),
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.22f),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) { Glyph(icon, size = size * 0.55f, tint = Color.White) }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), fontSize = 12.sp, letterSpacing = 2.sp, color = Fc.Muted, fontWeight = FontWeight.Bold)
}

/** Timer a tappe: scorre da solo da una tappa all'altra, così il telefono si può posare. */
class StepTimer(private val durations: List<Int>) {
    /** -1 prima di cominciare, uguale al numero di tappe quando è finito. */
    var index by mutableIntStateOf(-1)
        private set
    var remaining by mutableIntStateOf(0)
    var running by mutableStateOf(false)

    val count get() = durations.size
    val started get() = index >= 0
    val finished get() = index >= durations.size
    val total get() = durations.getOrElse(index) { 1 }

    fun start() = goTo(0)

    fun skip() = goTo(index + 1)

    private fun goTo(step: Int) {
        index = step
        running = step < durations.size
        if (running) remaining = durations[step]
    }

    @Composable
    fun Run() {
        val haptic = LocalHapticFeedback.current
        val sounds = LocalSounds.current
        LaunchedEffect(index, running) {
            while (running && remaining > 0) {
                delay(1000)
                remaining--
            }
            if (running && remaining == 0 && started && !finished) {
                sounds?.play(Sfx.BELL)
                repeat(2) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(160)
                }
                skip()
            }
        }
    }
}

@Composable
private fun StepClock(timer: StepTimer, colors: List<Color>) {
    val progress by animateFloatAsState(
        timer.remaining / timer.total.toFloat(),
        tween(if (timer.running) 1000 else 250, easing = LinearEasing),
        label = "step",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(formatTime(timer.remaining), fontFamily = Fc.Display, fontWeight = FontWeight.Bold, fontSize = 46.sp)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(colors)),
            )
        }
    }
}

@Composable
private fun StepDots(timer: StepTimer, colors: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(timer.count) { i ->
            Box(
                Modifier
                    .size(if (i == timer.index) 22.dp else 8.dp, 8.dp)
                    .clip(CircleShape)
                    .background(if (i <= timer.index) Brush.horizontalGradient(colors) else Brush.horizontalGradient(listOf(Fc.GlassBorder, Fc.GlassBorder))),
            )
        }
    }
}

@Composable
private fun StepControls(timer: StepTimer, colors: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GhostButton(
            if (timer.running) "Pausa" else "Riprendi",
            Modifier.weight(1f),
            if (timer.running) FcIcon.PAUSE else FcIcon.PLAY,
        ) { timer.running = !timer.running }
        GradientButton("Avanti", Modifier.weight(1f), colors, icon = FcIcon.SKIP) { timer.skip() }
    }
}

// --- Maratona del Kamasutra ------------------------------------------------------------------

@Composable
fun MarathonScreen(state: AppState, onBack: () -> Unit) {
    var picks by remember { mutableStateOf(positions.shuffled().take(4)) }
    val timer = remember { StepTimer(List(picks.size) { 60 }) }
    val colors = listOf(Fc.Violet, Fc.DeepViolet)
    timer.Run()
    LaunchedEffect(timer.finished) {
        if (timer.finished) {
            state.completeMarathon(picks.map { it.id })
            state.sounds.play(Sfx.WIN)
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopBar("Maratona", onBack)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                !timer.started -> {
                    Text(
                        "Quattro posizioni, un minuto ciascuna. Se una non vi convince, cambiatela. " +
                            "Poi il timer avanza da solo: posate il telefono e seguite il ritmo.",
                        color = Fc.Muted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    picks.forEachIndexed { i, p ->
                        Entrance(i) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .glass(22.dp)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PosePictogram(
                                    p,
                                    Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.22f)),
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, fontFamily = Fc.Display, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(difficultyNames[p.difficulty], color = Fc.Muted, fontSize = 13.sp)
                                }
                                // Cambia solo questa posizione con un'altra non ancora in scaletta.
                                Box(
                                    Modifier
                                        .bounceClick { picks = picks.toMutableList().also { it[i] = (positions - picks.toSet()).random() } }
                                        .padding(12.dp),
                                ) { Glyph(FcIcon.REPLAY, size = 20.dp, tint = Fc.Muted) }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GhostButton("Rimescola", Modifier.weight(1f), FcIcon.REPLAY) {
                            picks = (positions - picks.toSet()).shuffled().take(picks.size)
                        }
                        GradientButton("Inizia", Modifier.weight(1f), colors, icon = FcIcon.PLAY) { timer.start() }
                    }
                }

                timer.finished -> {
                    Spacer(Modifier.weight(1f))
                    IconTile(FcIcon.TROPHY, listOf(Fc.Gold, Fc.Orange), 110.dp)
                    Spacer(Modifier.height(20.dp))
                    Text("Maratona completata!", fontFamily = Fc.Display, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Le quattro posizioni sono state segnate come provate.",
                        color = Fc.Muted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.weight(1f))
                    GradientButton("Torna al Kamasutra", Modifier.fillMaxWidth(), colors, onClick = onBack)
                }

                else -> {
                    val p = picks[timer.index]
                    StepDots(timer, colors)
                    Spacer(Modifier.height(12.dp))
                    AnimatedContent(
                        targetState = p,
                        modifier = Modifier.weight(1f),
                        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() },
                        label = "position",
                    ) { shown ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.weight(1f).glass(32.dp), contentAlignment = Alignment.Center) {
                                PosePictogram(shown, Modifier.fillMaxHeight().padding(10.dp), animated = true)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(shown.name, fontFamily = Fc.Display, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text(shown.tagline, fontStyle = FontStyle.Italic, fontFamily = Fc.Display, color = Fc.Rose, fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    StepClock(timer, colors)
                    Spacer(Modifier.height(14.dp))
                    StepControls(timer, colors)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// --- Roulette dei preliminari ----------------------------------------------------------------

private class ForeplayStep(val who: Int, val text: String, val icon: FcIcon, val seconds: Int)

private fun generateForeplay(state: AppState): List<ForeplayStep> {
    val actions = diceActions.filter { state.allows(it.verb) }.ifEmpty { diceActions }
    val modes = diceModes.filter { state.allows("${it.label} ${it.phrase}") }
    val parts = diceParts.shuffled()
    val first = Random.nextInt(2)
    return List(5) { i ->
        val who = (first + i) % 2
        // "Succhia" sta bene solo dove ha senso: altrove si pesca un'altra azione.
        val awkward = parts[i].label in setOf("Schiena", "Fondoschiena")
        val action = actions.filter { !(awkward && it.verb == "Succhia") }.ifEmpty { actions }.random()
        // Le prime tappe sono semplici; dalla terza può aggiungersi un "come".
        val mode = if (i >= 2 && modes.isNotEmpty() && Random.nextBoolean()) modes.random() else null
        val text = "${action.verb} ${parts[i].phrase} di ${state.name(1 - who)}" + (mode?.let { ", ${it.phrase}" } ?: "")
        ForeplayStep(who, text, action.icon, listOf(45, 60, 60, 75, 90)[i])
    }
}

@Composable
fun ForeplayScreen(state: AppState, onBack: () -> Unit) {
    var steps by remember { mutableStateOf(generateForeplay(state)) }
    val timer = remember(steps) { StepTimer(steps.map { it.seconds }) }
    val colors = listOf(Fc.Orange, Fc.Crimson)
    timer.Run()
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopBar("Roulette dei Preliminari", onBack)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                !timer.started -> {
                    Text(
                        "Cinque tappe a sorpresa, a turno. Il timer passa da una all'altra da solo, con una vibrazione a ogni cambio.",
                        color = Fc.Muted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        steps.forEachIndexed { i, step ->
                            Entrance(i) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .glass(22.dp)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconTile(step.icon, colors, 44.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            state.name(step.who),
                                            color = if (step.who == 0) Fc.Rose else Fc.Violet,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                        )
                                        Text(step.text, fontSize = 15.sp, lineHeight = 20.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(formatTime(step.seconds), color = Fc.Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GhostButton("Cambia", Modifier.weight(1f), FcIcon.REPLAY) { steps = generateForeplay(state) }
                        GradientButton("Si comincia", Modifier.weight(1.4f), colors, icon = FcIcon.PLAY) { timer.start() }
                    }
                }

                timer.finished -> {
                    Spacer(Modifier.weight(1f))
                    IconTile(FcIcon.FLAME, colors, 110.dp)
                    Spacer(Modifier.height(20.dp))
                    Text("Riscaldamento finito", fontFamily = Fc.Display, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Da qui in poi non servono più istruzioni.", color = Fc.Muted, textAlign = TextAlign.Center)
                    Spacer(Modifier.weight(1f))
                    GradientButton("Un altro giro", Modifier.fillMaxWidth(), colors, icon = FcIcon.REPLAY) {
                        steps = generateForeplay(state)
                    }
                }

                else -> {
                    val step = steps[timer.index]
                    StepDots(timer, colors)
                    AnimatedContent(
                        targetState = step,
                        modifier = Modifier.weight(1f),
                        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.85f)) togetherWith fadeOut() },
                        label = "step",
                    ) { shown ->
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            IconTile(shown.icon, colors, 104.dp)
                            Spacer(Modifier.height(22.dp))
                            Text("TOCCA A", fontSize = 11.sp, color = Fc.Muted, letterSpacing = 3.sp)
                            Text(
                                state.name(shown.who),
                                fontFamily = Fc.Display,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = if (shown.who == 0) Fc.Rose else Fc.Violet,
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                shown.text,
                                fontFamily = Fc.Display,
                                fontSize = 25.sp,
                                lineHeight = 34.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    StepClock(timer, colors)
                    Spacer(Modifier.height(14.dp))
                    StepControls(timer, colors)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// --- Non ho mai ------------------------------------------------------------------------------

@Composable
fun NeverScreen(state: AppState, onBack: () -> Unit) {
    LaunchedEffect(Unit) {
        if (state.neverCurrent == null) state.neverDraw()
    }
    val statement = state.neverCurrent.orEmpty()
    var did by remember(statement) { mutableStateOf(listOf(false, false)) }
    val level = levels[state.neverLevel]
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopBar("Non ho mai", onBack)
        LevelSelector(state.neverLevel, enabled = true) {
            state.neverLevel = it
            state.neverDraw()
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 18.dp)
                .tilt3d(0.8f)
                .shadow(24.dp, cardShape, ambientColor = level.colors.last(), spotColor = level.colors.last())
                .clip(cardShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF3B1136), Color(0xFF180620))))
                .background(Brush.verticalGradient(listOf(level.colors.first().copy(alpha = 0.28f), Color.Transparent)))
                .border(1.5.dp, level.colors.first().copy(alpha = 0.5f), cardShape)
                .padding(26.dp),
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                IconTile(FcIcon.GLASS, level.colors, 54.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Non ho mai…",
                    fontFamily = Fc.Display,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = level.colors.first(),
                )
                AnimatedContent(
                    targetState = statement,
                    modifier = Modifier.weight(1f),
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() },
                    label = "never",
                ) { shown ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "…" + shown.removePrefix("Non ho mai "),
                            fontFamily = Fc.Display,
                            fontSize = 26.sp,
                            lineHeight = 36.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Text(
                    "Chi l'ha fatto toglie un indumento. O racconta com'è andata.",
                    color = Fc.Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) { player ->
                val on = did[player]
                val colors = if (player == 0) listOf(Fc.Rose, Fc.Pink) else listOf(Fc.Violet, Fc.DeepViolet)
                Column(
                    Modifier
                        .weight(1f)
                        .bounceClick { did = did.mapIndexed { i, v -> if (i == player) !v else v } }
                        .then(if (on) Modifier.clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(colors)) else Modifier.glass(22.dp))
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.name(player), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    Text(
                        if (on) "L'ho fatto!" else "Tocca se l'hai fatto",
                        fontSize = 12.sp,
                        color = if (on) Color.White else Fc.Muted,
                    )
                    Text(state.neverCounts[player].let { if (it == 1) "1 pegno" else "$it pegni" }, fontSize = 11.sp, color = if (on) Color.White else Fc.Gold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GradientButton(
            "Prossima frase",
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            level.colors,
            icon = FcIcon.SKIP,
        ) {
            state.neverNext(did)
            state.sounds.play(Sfx.FLIP)
        }
        if (state.neverCounts.any { it > 0 }) {
            Text(
                "Azzera il conteggio",
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .bounceClick { state.neverReset() }
                    .padding(12.dp),
                color = Fc.Muted,
                fontSize = 14.sp,
            )
        } else {
            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- Buoni del piacere -----------------------------------------------------------------------

@Composable
fun CouponsScreen(state: AppState, onBack: () -> Unit) {
    var player by remember { mutableIntStateOf(0) }
    var used by remember { mutableStateOf<String?>(null) }
    val mine = state.owned[player].mapNotNull { id -> coupons.find { it.id == id } }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopBar("Buoni del Piacere", onBack)
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) { p ->
                val colors = if (p == 0) listOf(Fc.Rose, Fc.Pink) else listOf(Fc.Violet, Fc.DeepViolet)
                Column(
                    Modifier
                        .weight(1f)
                        .bounceClick {
                            player = p
                            used = null
                        }
                        .then(if (p == player) Modifier.clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(colors)) else Modifier.glass(24.dp))
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.name(p), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Glyph(FcIcon.STAR, size = 18.dp, tint = Fc.Gold)
                        Spacer(Modifier.width(6.dp))
                        Text("${state.wallet[p]}", fontFamily = Fc.Display, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    }
                    Text("punti da spendere", fontSize = 11.sp, color = if (p == player) Color.White else Fc.Muted)
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Ogni sfida completata a Verità o Obbligo vale un punto. Qui diventano buoni da usare quando vuoi.",
                color = Fc.Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            used?.let {
                Text(
                    "Buono usato: $it. Buon divertimento!",
                    Modifier
                        .fillMaxWidth()
                        .glass(18.dp, Fc.Gold.copy(alpha = 0.14f))
                        .padding(14.dp),
                    color = Fc.Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            if (mine.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                SectionTitle("I buoni di ${state.name(player)}")
                mine.forEach { coupon ->
                    CouponRow(coupon, listOf(Fc.Gold, Fc.Orange), "Usa", FcIcon.CHECK, true) {
                        state.useCoupon(player, coupon.id)
                        used = coupon.title
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            SectionTitle("Negozio")
            coupons.forEach { coupon ->
                CouponRow(coupon, listOf(Fc.Pink, Fc.DeepViolet), "${coupon.cost}", FcIcon.STAR, state.wallet[player] >= coupon.cost) {
                    state.buyCoupon(player, coupon)
                    state.sounds.play(Sfx.SUCCESS)
                    used = null
                }
            }
        }
    }
}

@Composable
private fun CouponRow(coupon: Coupon, colors: List<Color>, action: String, actionIcon: FcIcon, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(22.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(coupon.icon, colors, 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(coupon.title, fontFamily = Fc.Display, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(coupon.text, color = Fc.Muted, fontSize = 13.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier
                .alpha(if (enabled) 1f else 0.35f)
                .bounceClick(enabled, onClick)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(colors))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Glyph(actionIcon, size = 14.dp, tint = Color.White)
            Spacer(Modifier.width(5.dp))
            Text(action, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// --- Le vostre carte -------------------------------------------------------------------------

@Composable
fun CustomCardsScreen(state: AppState, onBack: () -> Unit) {
    var author by remember { mutableIntStateOf(0) }
    var truth by remember { mutableStateOf(false) }
    var level by remember { mutableIntStateOf(1) }
    var text by remember { mutableStateOf("") }
    var added by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(emptySet<Long>()) }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding(),
    ) {
        TopBar("Le vostre carte", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Scrivete di nascosto le vostre verità e i vostri obblighi: finiranno nel mazzo e usciranno a sorpresa.",
                Modifier.padding(horizontal = 20.dp),
                color = Fc.Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { p ->
                    Chip(
                        "Scrive ${state.name(p)}",
                        author == p,
                        if (p == 0) listOf(Fc.Rose, Fc.Pink) else listOf(Fc.Violet, Fc.DeepViolet),
                        Modifier.weight(1f),
                    ) { author = p }
                }
            }
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Verità", truth, listOf(Fc.Violet, Fc.DeepViolet), Modifier.weight(1f), FcIcon.CHAT) { truth = true }
                Chip("Obbligo", !truth, listOf(Fc.Pink, Fc.Crimson), Modifier.weight(1f), FcIcon.BOLT) { truth = false }
            }
            LevelSelector(level, enabled = true) { level = it }
            OutlinedTextField(
                value = text,
                onValueChange = {
                    if (it.length <= 160) text = it
                    added = false
                },
                placeholder = { Text(if (truth) "La tua domanda…" else "La tua sfida…", color = Fc.Muted) },
                minLines = 3,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Fc.Pink,
                    unfocusedBorderColor = Fc.GlassBorder,
                    cursorColor = Fc.Pink,
                    focusedTextColor = Fc.Text,
                    unfocusedTextColor = Fc.Text,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            GradientButton(
                if (added) "Aggiunta in segreto" else "Aggiungi al mazzo",
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = text.trim().length >= 8,
                icon = if (added) FcIcon.CHECK else FcIcon.PLUS,
            ) {
                state.addCustomCard(author, level, truth, text)
                text = ""
                added = true
            }
            if (state.customCards.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Box(Modifier.padding(horizontal = 20.dp)) { SectionTitle("Nel mazzo: ${state.customCards.size}") }
                state.customCards.forEach { card ->
                    val shown = card.id in revealed
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .glass(20.dp)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(levels[card.level].icon, levels[card.level].colors, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(
                            Modifier
                                .weight(1f)
                                .bounceClick { revealed = if (shown) revealed - card.id else revealed + card.id },
                        ) {
                            Text(
                                "${if (card.truth) "Verità" else "Obbligo"} · ${levels[card.level].name} · di ${state.name(card.author)}",
                                fontSize = 12.sp,
                                color = Fc.Muted,
                            )
                            Text(
                                if (shown) card.text else "Carta segreta: tocca per sbirciare",
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                fontStyle = if (shown) FontStyle.Normal else FontStyle.Italic,
                            )
                        }
                        Box(
                            Modifier
                                .size(40.dp)
                                .bounceClick { state.removeCustomCard(card.id) },
                            contentAlignment = Alignment.Center,
                        ) { Glyph(FcIcon.TRASH, size = 20.dp, tint = Fc.Muted) }
                    }
                }
            }
        }
    }
}

// --- Preferenze e limiti ---------------------------------------------------------------------

@Composable
fun LimitsScreen(state: AppState, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopBar("Preferenze e limiti", onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Spegnete ciò che non vi va: quelle sfide non usciranno più, in nessun gioco. Basta il no di uno dei due.",
                color = Fc.Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Limit.entries.forEach { limit ->
                val allowed = limit !in state.limits
                Row(
                    Modifier
                        .fillMaxWidth()
                        .bounceClick { state.toggleLimit(limit) }
                        .glass(22.dp)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.alpha(if (allowed) 1f else 0.4f)) {
                        IconTile(limit.icon, listOf(Fc.Pink, Fc.DeepViolet), 44.dp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(limit.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(if (allowed) limit.hint else "Escluso da tutti i giochi", color = Fc.Muted, fontSize = 13.sp)
                    }
                    Toggle(allowed)
                }
            }
        }
    }
}

// --- Blocco con PIN --------------------------------------------------------------------------

@Composable
private fun PinPad(title: String, subtitle: String, error: String?, onComplete: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    val shake = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val sounds = LocalSounds.current
    LaunchedEffect(error) {
        if (error != null) {
            sounds?.play(Sfx.ERROR)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            shake.snapTo(1f)
            shake.animateTo(0f, spring(dampingRatio = 0.2f, stiffness = 500f))
        }
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        IconTile(FcIcon.LOCK, listOf(Fc.Pink, Fc.DeepViolet), 72.dp)
        Spacer(Modifier.height(18.dp))
        Text(title, fontFamily = Fc.Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(error ?: subtitle, color = if (error != null) Fc.Gold else Fc.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        Row(
            Modifier.graphicsLayer { translationX = shake.value * 18.dp.toPx() },
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (i < pin.length) Fc.Pink else Color.Transparent)
                        .border(1.5.dp, if (i < pin.length) Fc.Pink else Fc.GlassBorder, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        listOf("123", "456", "789", " 0<").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .size(74.dp)
                            .then(
                                when (key) {
                                    ' ' -> Modifier
                                    '<' -> Modifier.bounceClick { pin = pin.dropLast(1) }
                                    else -> Modifier
                                        .bounceClick {
                                            val next = pin + key
                                            if (next.length == 4) {
                                                pin = ""
                                                onComplete(next)
                                            } else {
                                                pin = next
                                            }
                                        }
                                        .glass(37.dp)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (key) {
                            ' ' -> Unit
                            '<' -> Glyph(FcIcon.BACKSPACE, size = 26.dp, tint = Fc.Muted)
                            else -> Text("$key", fontSize = 28.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
fun LockScreen(state: AppState) {
    var attempts by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val biometric = remember { state.biometric && Biometrics.available(context) }
    fun askBiometric() = Biometrics.prompt(context, "Sblocca FunCouple") { state.unlockWithBiometric() }
    // Se è attiva, la richiesta biometrica parte da sola; il tastierino resta sotto come riserva.
    LaunchedEffect(Unit) {
        if (biometric) askBiometric()
    }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f)) {
            PinPad(
                "FunCouple è bloccata",
                "Inserisci il PIN per entrare",
                if (attempts > 0) "PIN errato, riprova ($attempts)" else null,
            ) {
                if (!state.unlock(it)) attempts++
            }
        }
        if (biometric) {
            GhostButton("Usa impronta o volto", icon = FcIcon.FINGERPRINT) { askBiometric() }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun PinSetupScreen(state: AppState, onBack: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopBar("Blocco con PIN", onBack)
        Box(Modifier.weight(1f).padding(horizontal = 24.dp)) {
            when {
                state.hasPin -> PinPad("Rimuovi il blocco", "Inserisci il PIN attuale", error) {
                    if (state.checkPin(it)) {
                        state.setPin(null)
                        onBack()
                    } else {
                        error = "PIN errato, riprova"
                    }
                }

                first == null -> PinPad("Scegli un PIN", error ?: "Quattro cifre, da ricordare bene", null) {
                    first = it
                    error = null
                }

                else -> PinPad("Ripeti il PIN", "Ancora una volta, per conferma", error) {
                    if (it == first) {
                        state.setPin(it)
                        onBack()
                    } else {
                        first = null
                        error = "I due PIN non coincidono: ricominciamo"
                    }
                }
            }
        }
        Text(
            "Il PIN verrà chiesto a ogni apertura. Se lo dimentichi, dovrai reinstallare l'app e perderai i dati salvati.",
            Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
            color = Fc.Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
        )
    }
}
