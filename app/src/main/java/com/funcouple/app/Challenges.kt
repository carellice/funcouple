package com.funcouple.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal val cardShape = RoundedCornerShape(34.dp)

/** Selettore dei livelli: un indicatore colorato scivola sotto il livello scelto. */
@Composable
internal fun LevelSelector(selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(66.dp)
            .alpha(if (enabled) 1f else 0.55f)
            .glass(24.dp),
    ) {
        val cell = maxWidth / levels.size
        val x by animateDpAsState(cell * selected, spring(dampingRatio = 0.7f, stiffness = 320f), label = "level")
        Box(
            Modifier
                .offset(x = x)
                .width(cell)
                .fillMaxHeight()
                .padding(5.dp)
                .shadow(10.dp, RoundedCornerShape(19.dp), spotColor = levels[selected].colors.last())
                .clip(RoundedCornerShape(19.dp))
                .background(Brush.linearGradient(levels[selected].colors)),
        )
        Row(Modifier.fillMaxSize()) {
            levels.forEachIndexed { i, level ->
                val color = if (i == selected) Color.White else Fc.Muted
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .bounceClick(enabled) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Glyph(level.icon, size = 22.dp, tint = color)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        level.name,
                        fontSize = 12.sp,
                        fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Medium,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Avatar del giocatore con iniziale e punteggio; quello di turno si ingrandisce e pulsa. */
@Composable
private fun PlayerBadge(name: String, score: Int, colors: List<Color>, active: Boolean, pulse: Float) {
    val scale by animateFloatAsState(if (active) 1f else 0.82f, spring(dampingRatio = 0.55f, stiffness = 260f), label = "badge")
    Column(
        Modifier
            .width(84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (active) 1f else 0.6f
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                if (active) {
                    // Anello che si allarga e svanisce, come un battito.
                    drawCircle(colors.last().copy(alpha = 0.55f * (1f - pulse)), radius * (0.86f + 0.3f * pulse))
                }
                drawCircle(Brush.linearGradient(colors), radius * 0.86f)
                drawCircle(Fc.Bg0.copy(alpha = 0.35f), radius * 0.74f)
            }
            Text(
                name.take(1).uppercase(),
                fontFamily = Fc.Display,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White,
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Fc.Bg0)
                    .border(1.5.dp, Fc.Gold, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = score,
                    transitionSpec = { (scaleIn(spring(dampingRatio = 0.4f)) + fadeIn()) togetherWith fadeOut() },
                    label = "score",
                ) { Text("$it", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Fc.Gold) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(name, fontSize = 12.sp, color = Fc.Muted, maxLines = 1)
    }
}

@Composable
private fun Scoreboard(state: AppState, turn: Int, pulse: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerBadge(state.name1, state.todScores[0], listOf(Fc.Rose, Fc.Pink), turn == 0, pulse)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TOCCA A", fontSize = 11.sp, color = Fc.Muted, letterSpacing = 3.sp)
            AnimatedContent(
                targetState = turn,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "turn",
            ) { t ->
                Text(
                    state.name(t),
                    fontFamily = Fc.Display,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 30.sp,
                    color = if (t == 0) Fc.Rose else Fc.Violet,
                    maxLines = 1,
                )
            }
        }
        PlayerBadge(state.name2, state.todScores[1], listOf(Fc.Violet, Fc.DeepViolet), turn == 1, pulse)
    }
}

/** Doppia cornice e icone agli angoli, come su una carta da gioco. */
private fun DrawScope.drawCardFrame(icon: FcIcon, color: Color) {
    val outer = 12.dp.toPx()
    val inner = 17.dp.toPx()
    drawRoundRect(
        color.copy(alpha = 0.55f),
        Offset(outer, outer),
        Size(size.width - outer * 2, size.height - outer * 2),
        CornerRadius(24.dp.toPx()),
        style = Stroke(1.5.dp.toPx()),
    )
    drawRoundRect(
        color.copy(alpha = 0.22f),
        Offset(inner, inner),
        Size(size.width - inner * 2, size.height - inner * 2),
        CornerRadius(20.dp.toPx()),
        style = Stroke(1.dp.toPx()),
    )
    val glyph = 20.dp.toPx()
    val pad = 28.dp.toPx()
    drawGlyph(icon, Offset(pad, pad), glyph, color.copy(alpha = 0.9f))
    rotate(180f) { drawGlyph(icon, Offset(pad, pad), glyph, color.copy(alpha = 0.9f)) }
}

@Composable
private fun CardBack(level: Level, shimmer: Float, ring: Float, onTruth: () -> Unit, onDare: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .shadow(26.dp, cardShape, ambientColor = level.colors.last(), spotColor = level.colors.last())
            .clip(cardShape)
            .background(Fc.Bg1)
            .background(Brush.linearGradient(level.colors.map { it.copy(alpha = 0.62f) })),
    ) {
        // Trama del dorso: l'icona del livello ripetuta a scacchiera.
        Canvas(Modifier.fillMaxSize()) {
            val step = 46.dp.toPx()
            val glyph = 20.dp.toPx()
            var row = 0
            var y = -glyph / 2
            while (y < size.height) {
                var x = if (row % 2 == 0) step * 0.1f else step * 0.6f
                while (x < size.width) {
                    drawGlyph(level.icon, Offset(x, y), glyph, Color.White.copy(alpha = 0.075f))
                    x += step
                }
                y += step * 0.78f
                row++
            }
            drawCardFrame(level.icon, Color.White)
        }
        // Riflesso di luce che attraversa la carta.
        Canvas(Modifier.fillMaxSize()) {
            val x = size.width * (-0.6f + shimmer * 2.2f)
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.13f), Color.Transparent),
                    start = Offset(x, 0f),
                    end = Offset(x + size.width * 0.45f, size.height * 0.5f),
                ),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    rotate(ring) {
                        drawCircle(
                            Color.White.copy(alpha = 0.7f),
                            radius - 2.dp.toPx(),
                            style = Stroke(
                                2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 9.dp.toPx())),
                            ),
                        )
                    }
                    drawCircle(Color.White.copy(alpha = 0.16f), radius * 0.8f)
                    drawCircle(Color.White.copy(alpha = 0.5f), radius * 0.8f, style = Stroke(1.5.dp.toPx()))
                }
                Glyph(level.icon, size = 62.dp, tint = Color.White)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Verità o Obbligo?",
                fontFamily = Fc.Display,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
            )
            Text("LIVELLO ${level.name.uppercase()}", color = Fc.Muted, fontSize = 12.sp, letterSpacing = 3.sp)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChoiceTile("Verità", FcIcon.CHAT, Modifier.weight(1f), filled = false, accent = Color.White, onClick = onTruth)
                ChoiceTile("Obbligo", FcIcon.BOLT, Modifier.weight(1f), filled = true, accent = level.colors.last(), onClick = onDare)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ChoiceTile(text: String, icon: FcIcon, modifier: Modifier, filled: Boolean, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val content = if (filled) lerp(accent, Fc.Bg0, 0.25f) else Color.White
    Column(
        modifier
            .height(104.dp)
            .bounceClick(onClick = onClick)
            .shadow(if (filled) 14.dp else 0.dp, shape, spotColor = Color.Black)
            .clip(shape)
            .background(if (filled) Color.White.copy(alpha = 0.94f) else Color.Black.copy(alpha = 0.3f))
            .border(1.5.dp, Color.White.copy(alpha = if (filled) 0f else 0.45f), shape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Glyph(icon, size = 32.dp, tint = content)
        Spacer(Modifier.height(6.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = content)
    }
}

/** Interruttore tra partita libera ed Escalation. */
@Composable
private fun ModeSwitch(escalation: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip("Scelgo io il livello", !escalation, listOf(Fc.Pink, Fc.DeepViolet), Modifier.weight(1f)) {
            if (enabled) onChange(false)
        }
        Chip("Escalation", escalation, listOf(Fc.Orange, Fc.Crimson), Modifier.weight(1f), FcIcon.THERMO) {
            if (enabled) onChange(true)
        }
    }
}

/** Termometro dell'Escalation: quattro tacche che si riempiono a mano a mano che si gioca. */
@Composable
private fun EscalationBar(played: Int, levelIndex: Int) {
    val level = levels[levelIndex]
    val left = ESCALATION_STEP - played % ESCALATION_STEP
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(66.dp)
            .glass(24.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Glyph(level.icon, size = 18.dp, tint = level.colors.first())
            Spacer(Modifier.width(8.dp))
            Text(level.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    levelIndex == levels.lastIndex -> "Temperatura massima"
                    left == 1 -> "Si sale dopo questa carta"
                    else -> "Si sale tra $left carte"
                },
                fontSize = 12.sp,
                color = Fc.Muted,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            levels.forEachIndexed { i, l ->
                val target = ((played - i * ESCALATION_STEP) / ESCALATION_STEP.toFloat()).coerceIn(0f, 1f)
                val fill by animateFloatAsState(if (i < levelIndex) 1f else target, tween(500), label = "heat")
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(if (i == levelIndex && levelIndex == levels.lastIndex) 1f else fill)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(l.colors)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFront(
    challenge: Challenge?,
    isTruth: Boolean,
    level: Level,
    text: String,
    modifier: CardModifier,
    modifierNote: String,
    penalty: String?,
    onSkip: () -> Unit,
    onDone: () -> Unit,
    onPenaltyDone: () -> Unit,
) {
    val isPenalty = penalty != null
    val accent = when {
        isPenalty -> Fc.Gold
        isTruth -> Fc.Violet
        else -> level.colors.first()
    }
    val accentColors = when {
        isPenalty -> listOf(Fc.Gold, Fc.Orange)
        isTruth -> listOf(Fc.Violet, Fc.DeepViolet)
        else -> level.colors
    }
    val icon = when {
        isPenalty -> FcIcon.SKIP
        isTruth -> FcIcon.CHAT
        else -> FcIcon.BOLT
    }
    Box(
        Modifier
            .fillMaxSize()
            .shadow(26.dp, cardShape, ambientColor = accent, spotColor = accent)
            .clip(cardShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF3B1136), Color(0xFF180620)))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Bagliore in alto e filigrana al centro
            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(size.width / 2, 0f),
                    radius = size.width * 0.9f,
                ),
                size.width * 0.9f,
                Offset(size.width / 2, 0f),
            )
            val mark = size.width * 0.72f
            drawGlyph(icon, Offset((size.width - mark) / 2, (size.height - mark) / 2), mark, accent.copy(alpha = 0.06f))
            drawCardFrame(icon, accent)
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when {
                    isPenalty -> "PENITENZA"
                    isTruth -> "VERITÀ"
                    else -> "OBBLIGO"
                },
                Modifier
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(accentColors))
                    .padding(horizontal = 18.dp, vertical = 7.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
            )
            if (!isPenalty && modifier != CardModifier.NONE) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Fc.Gold.copy(alpha = 0.16f))
                        .border(1.dp, Fc.Gold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Glyph(modifier.icon, size = 18.dp, tint = Fc.Gold)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(modifier.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp, color = Fc.Gold)
                        Text(modifierNote, fontSize = 12.sp, lineHeight = 15.sp)
                    }
                }
            } else if (!isPenalty && challenge?.custom == true) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Glyph(FcIcon.PEN, size = 12.dp, tint = Fc.Muted)
                    Spacer(Modifier.width(5.dp))
                    Text("Scritta da voi", fontSize = 12.sp, color = Fc.Muted)
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.height(14.dp))
                AnimatedContent(
                    targetState = penalty ?: text,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() },
                    label = "cardText",
                ) { shown ->
                    Text(
                        shown,
                        Modifier.fillMaxWidth(),
                        fontFamily = Fc.Display,
                        fontSize = 25.sp,
                        lineHeight = 35.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                if (!isPenalty && challenge != null && challenge.seconds > 0) {
                    Spacer(Modifier.height(22.dp))
                    CountdownTimer(challenge.seconds, accent)
                }
                Spacer(Modifier.height(14.dp))
            }
            if (isPenalty) {
                GradientButton("Penitenza fatta", Modifier.fillMaxWidth(), accentColors, icon = FcIcon.CHECK, onClick = onPenaltyDone)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostButton("Passo", Modifier.weight(1f), FcIcon.SKIP, onClick = onSkip)
                    GradientButton("Fatto", Modifier.weight(1.3f), accentColors, icon = FcIcon.CHECK, onClick = onDone)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

private class Spark(val angle: Float, val distance: Float, val size: Float, val color: Color)

/** Esplosione di cuori quando una sfida viene completata. */
@Composable
internal fun HeartBurst(trigger: Int, colors: List<Color>, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    val sparks = remember(trigger) {
        List(22) {
            Spark(
                angle = Random.nextFloat() * 2f * PI.toFloat(),
                distance = 0.35f + Random.nextFloat() * 0.65f,
                size = 7f + Random.nextFloat() * 13f,
                color = if (it % 3 == 0) Fc.Gold else colors[it % colors.size],
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing))
        }
    }
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val reach = size.minDimension * 0.75f
        sparks.forEach { s ->
            val x = center.x + cos(s.angle) * reach * s.distance * p
            val y = center.y + sin(s.angle) * reach * s.distance * p + size.height * 0.12f * p * p
            val scale = s.size.dp.toPx() * (0.5f + 0.5f * p)
            withTransform({
                translate(x, y)
                scale(scale, scale, Offset.Zero)
            }) { drawPath(unitHeart, s.color.copy(alpha = (1f - p) * 0.95f)) }
        }
    }
}

@Composable
fun ChallengesScreen(state: AppState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    // Punteggi, turno e carta scoperta vivono in AppState: uscire dalla schermata non li azzera.
    val turn = state.todTurn
    val current = state.todCurrent
    val levelIndex = state.todLevel
    val level = levels[levelIndex]
    var busy by remember { mutableStateOf(false) }
    var confirmNewGame by remember { mutableStateOf(false) }
    var burst by remember { mutableIntStateOf(0) }
    var levelUp by remember { mutableStateOf(false) }
    var seenLevel by remember { mutableIntStateOf(levelIndex) }
    val rotation = remember { Animatable(if (state.todCurrent != null) 180f else 0f) }
    val tilt = LocalTilt.current

    val idle = rememberInfiniteTransition(label = "idle")
    val float by idle.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float",
    )
    val shimmer by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "shimmer")
    val ring by idle.animateFloat(0f, 360f, infiniteRepeatable(tween(26_000, easing = LinearEasing)), label = "ring")
    val pulse by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearOutSlowInEasing)), label = "pulse")

    // In Escalation, quando il livello sale lo si annuncia sopra la carta.
    LaunchedEffect(levelIndex) {
        if (state.escalation && levelIndex > seenLevel) {
            state.sounds.play(Sfx.LEVEL)
            levelUp = true
            delay(2200)
            levelUp = false
        }
        seenLevel = levelIndex
    }

    fun draw(truth: Boolean) {
        if (busy) return
        state.todDraw(truth)
        state.sounds.play(Sfx.FLIP)
        busy = true
        scope.launch {
            rotation.animateTo(180f, tween(650, easing = FastOutSlowInEasing))
            busy = false
        }
    }

    fun endTurn(done: Boolean) {
        if (busy) return
        busy = true
        if (done) {
            state.todComplete()
            state.sounds.play(Sfx.SUCCESS)
            burst++
        }
        scope.launch {
            if (done) delay(250)
            state.sounds.play(Sfx.FLIP)
            rotation.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
            state.todEndTurn()
            busy = false
        }
    }

    fun newGame() {
        if (busy) return
        if (!confirmNewGame) {
            confirmNewGame = true
            scope.launch {
                delay(3000)
                confirmNewGame = false
            }
            return
        }
        confirmNewGame = false
        busy = true
        scope.launch {
            rotation.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
            state.todNewGame()
            seenLevel = state.todLevel
            busy = false
        }
    }

    val partner = state.name(1 - turn)
    val me = state.name(turn)
    // Con "Scambio" la sfida la esegue il partner, quindi nel testo il partner diventa chi era di turno.
    val swapped = state.todModifier == CardModifier.SWAP
    val modifierNote = when (state.todModifier) {
        CardModifier.DOUBLE -> "Vale due punti."
        CardModifier.SWAP -> "La sfida passa a $partner, e il punto pure."
        CardModifier.TOGETHER -> "La fate tutti e due: un punto a testa."
        CardModifier.NONE -> ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopBar("Verità o Obbligo", onBack)
        val idleTable = current == null && !busy
        ModeSwitch(state.escalation, enabled = idleTable && !state.todInProgress) { state.updateEscalation(it) }
        Spacer(Modifier.height(10.dp))
        if (state.escalation) {
            EscalationBar(state.todPlayed, levelIndex)
        } else {
            LevelSelector(state.level, enabled = idleTable) { state.updateLevel(it) }
        }
        Spacer(Modifier.height(12.dp))
        Scoreboard(state, turn, pulse)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .padding(top = 4.dp, bottom = 18.dp),
        ) {
            // Le altre carte del mazzo che spuntano da sotto
            val deck = lerp(level.colors.last(), Fc.Bg0, 0.5f)
            listOf(-5f to 22f, 4f to 12f).forEach { (angle, drop) ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Il mazzo sotto si sposta meno della carta in cima: parallasse.
                            translationX = -(tilt?.x ?: 0f) * 10.dp.toPx()
                            rotationZ = angle
                            translationY = drop.dp.toPx()
                            scaleX = 0.95f
                            scaleY = 0.95f
                        }
                        .clip(cardShape)
                        .background(deck)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), cardShape),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = rotation.value - (tilt?.x ?: 0f) * 9f
                        rotationX = -(tilt?.y ?: 0f) * 9f
                        translationX = (tilt?.x ?: 0f) * 6.dp.toPx()
                        translationY = (float - 0.5f) * 10.dp.toPx()
                        rotationZ = (float - 0.5f) * 1.2f
                        cameraDistance = 16f * density
                    },
            ) {
                if (rotation.value <= 90f) {
                    CardBack(level, shimmer, ring, onTruth = { draw(true) }, onDare = { draw(false) })
                } else {
                    Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                        CardFront(
                            challenge = current,
                            isTruth = state.todIsTruth,
                            level = level,
                            text = current?.text?.forPartner(if (swapped) me else partner).orEmpty(),
                            modifier = state.todModifier,
                            modifierNote = modifierNote,
                            penalty = state.todPenalty?.forPartner(partner),
                            onSkip = {
                                if (!busy) {
                                    state.todSkip()
                                    state.sounds.play(Sfx.ERROR)
                                }
                            },
                            onDone = { endTurn(true) },
                            onPenaltyDone = { endTurn(false) },
                        )
                    }
                }
            }
            HeartBurst(burst, level.colors, Modifier.fillMaxSize())
            androidx.compose.animation.AnimatedVisibility(
                levelUp,
                Modifier.align(Alignment.TopCenter),
                enter = fadeIn() + scaleIn(spring(dampingRatio = 0.5f), initialScale = 0.6f),
                exit = fadeOut(),
            ) {
                Row(
                    Modifier
                        .offset(y = (-6).dp)
                        .shadow(18.dp, CircleShape, spotColor = level.colors.last())
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(level.colors))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Glyph(FcIcon.THERMO, size = 18.dp, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("La temperatura sale: ${level.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        if (state.todInProgress) {
            Row(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .bounceClick { newGame() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = if (confirmNewGame) Fc.Gold else Fc.Muted
                Glyph(FcIcon.REPLAY, size = 16.dp, tint = color)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (confirmNewGame) "Tocca di nuovo per azzerare i punti" else "Nuova partita",
                    color = color,
                    fontSize = 14.sp,
                )
            }
        } else {
            Spacer(Modifier.height(20.dp))
        }
    }
}
