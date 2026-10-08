package com.funcouple.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// Un colore per spicchio: due vicini (compresi il primo e l'ultimo) non sono mai uguali.
private val wheelColors = listOf(
    Color(0xFFFF3D81), Color(0xFF7B2FF7), Color(0xFFFF8A3D), Color(0xFFC2185B),
    Color(0xFF9D4EDD), Color(0xFFE11D48), Color(0xFFFFB03B), Color(0xFF5B21B6),
    Color(0xFFFF5C9A), Color(0xFF8E24AA), Color(0xFFF4511E), Color(0xFF4A2FBD),
)

private val goldLight = Color(0xFFFFE9A8)
private val goldDark = Color(0xFFB8791F)
private const val BULBS = 24

// Proporzioni rispetto al raggio esterno: il disco che gira sta dentro al bordo dorato.
private const val DISC = 0.88f
private const val HUB = 0.2f

@Composable
fun WheelScreen(state: AppState, onBack: () -> Unit, onOpenPosition: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val rotation = remember { Animatable(0f) }
    val kick = remember { Animatable(0f) }
    val turn = state.wheelTurn
    var spinning by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<Int?>(null) }
    var burst by remember { mutableIntStateOf(0) }
    // I premi esclusi dai limiti della coppia spariscono dalla ruota, ognuno con il suo colore.
    val slices = remember(state.limits) {
        wheelPrizes.withIndex().filter { state.allows("${it.value.label} ${it.value.text}") }.ifEmpty { wheelPrizes.withIndex().toList() }
    }
    val prizes = slices.map { it.value }
    val colors = slices.map { wheelColors[it.index] }
    val segment = 360f / prizes.size

    val idle = rememberInfiniteTransition(label = "wheel")
    val blink by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "blink")
    val pulse by idle.animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )
    val dim by animateFloatAsState(if (winner != null) 1f else 0f, tween(450), label = "dim")

    // A ogni piolo che passa sotto la linguetta: scatto e vibrazione.
    LaunchedEffect(Unit) {
        snapshotFlow { floor(rotation.value / segment).toInt() }.drop(1).collect {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            state.sounds.play(Sfx.TICK)
            launch {
                kick.snapTo(-20f)
                kick.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 500f))
            }
        }
    }

    fun spin() {
        if (spinning) return
        if (winner != null) state.wheelTurn = 1 - state.wheelTurn
        winner = null
        spinning = true
        scope.launch {
            // Si sceglie prima lo spicchio e un punto al suo interno, lontano dai bordi, poi si
            // calcola di quanto girare perché finisca sotto la linguetta (che è in alto).
            val prize = Random.nextInt(prizes.size)
            val landing = prize * segment + segment * (0.18f + Random.nextFloat() * 0.64f)
            val delta = (((-landing - rotation.value) % 360f) + 360f) % 360f
            val target = rotation.value + 360f * (4 + Random.nextInt(3)) + delta
            rotation.animateTo(target, tween(4800, easing = CubicBezierEasing(0.1f, 0.7f, 0.1f, 1f)))
            winner = prize
            state.sounds.play(Sfx.WIN)
            burst++
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            spinning = false
        }
    }

    val prize = winner?.let { prizes[it] }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar("Ruota del Desiderio", onBack)
        Spacer(Modifier.height(4.dp))
        TurnBanner(state, turn)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .aspectRatio(1f)
                .tilt3d(1.4f)
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = !spinning) { spin() },
            contentAlignment = Alignment.Center,
        ) {
            // Bagliore dietro la ruota
            Canvas(Modifier.fillMaxSize()) {
                val glow = size.minDimension * (0.62f + 0.03f * pulse)
                drawCircle(
                    Brush.radialGradient(listOf(Fc.Pink.copy(alpha = 0.5f), Color.Transparent), center, glow),
                    glow,
                )
            }
            // Disco che gira
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation.value },
            ) {
                val radius = size.minDimension / 2 * DISC
                val arcTopLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                val labelStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = Fc.Body)
                val won = winner
                prizes.forEachIndexed { i, item ->
                    val start = -90f + i * segment
                    val color = colors[i]
                    drawArc(
                        Brush.radialGradient(
                            0f to lerp(color, Color.Black, 0.45f),
                            0.55f to color,
                            1f to lerp(color, Color.White, 0.22f),
                            center = center,
                            radius = radius,
                        ),
                        start, segment, useCenter = true, topLeft = arcTopLeft, size = arcSize,
                    )
                    if (won != null) {
                        if (i == won) {
                            drawArc(
                                Color.White.copy(alpha = 0.10f + 0.16f * pulse),
                                start, segment, useCenter = true, topLeft = arcTopLeft, size = arcSize,
                            )
                        } else {
                            drawArc(
                                Color.Black.copy(alpha = 0.55f * dim),
                                start, segment, useCenter = true, topLeft = arcTopLeft, size = arcSize,
                            )
                        }
                    }
                    rotate(start + segment / 2, center) {
                        val label = measurer.measure(AnnotatedString(item.label), labelStyle)
                        drawText(label, topLeft = Offset(center.x + radius * 0.27f, center.y - label.size.height / 2f))
                        val badge = Offset(center.x + radius * 0.78f, center.y)
                        drawCircle(Color.White.copy(alpha = 0.2f), radius * 0.105f, badge)
                        val glyph = radius * 0.135f
                        // L'icona è ruotata in modo che la sua parte alta guardi verso l'esterno.
                        rotate(90f, badge) {
                            drawGlyph(item.icon, Offset(badge.x - glyph / 2, badge.y - glyph / 2), glyph, Color.White)
                        }
                    }
                }
                // Raggi dorati e pioli tra uno spicchio e l'altro
                repeat(prizes.size) { i ->
                    rotate(-90f + i * segment, center) {
                        drawLine(
                            goldLight.copy(alpha = 0.75f),
                            Offset(center.x + radius * HUB, center.y),
                            Offset(center.x + radius, center.y),
                            strokeWidth = 1.5.dp.toPx(),
                        )
                        val peg = Offset(center.x + radius * 0.955f, center.y)
                        drawCircle(goldDark, radius * 0.034f, peg)
                        drawCircle(goldLight, radius * 0.022f, peg)
                    }
                }
            }
            // Bordo dorato con le luci e mozzo centrale: restano fermi
            Canvas(Modifier.fillMaxSize()) {
                val outer = size.minDimension / 2
                val rim = outer * (1f - DISC)
                drawCircle(Color.Black.copy(alpha = 0.35f), outer * DISC, style = Stroke(rim * 0.5f))
                drawCircle(
                    Brush.sweepGradient(
                        listOf(goldLight, goldDark, goldLight, goldDark, goldLight, goldDark, goldLight),
                        center,
                    ),
                    outer - rim / 2,
                    style = Stroke(rim),
                )
                drawCircle(goldLight.copy(alpha = 0.9f), outer - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                drawCircle(goldDark, outer * DISC, style = Stroke(1.5.dp.toPx()))
                repeat(BULBS) { i ->
                    val angle = (i / BULBS.toFloat()) * 2f * PI.toFloat()
                    val p = Offset(center.x + cos(angle) * (outer - rim / 2), center.y + sin(angle) * (outer - rim / 2))
                    // Ferma: le luci si alternano lente. In movimento: si rincorrono lungo il bordo.
                    val lit = if (spinning) (i + (blink * 12).toInt()) % 3 == 0 else (i + (blink * 2).toInt()) % 2 == 0
                    if (lit) {
                        drawCircle(
                            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), Color.Transparent), p, rim * 0.85f),
                            rim * 0.85f,
                            p,
                        )
                    }
                    drawCircle(if (lit) Color.White else Color(0xFF7A4A12), rim * 0.2f, p)
                }
                val hub = outer * DISC * HUB
                drawCircle(Brush.sweepGradient(listOf(goldLight, goldDark, goldLight, goldDark, goldLight), center), hub)
                drawCircle(Brush.radialGradient(listOf(Fc.Bg2, Fc.Bg0), center, hub * 0.8f), hub * 0.8f)
                val heart = hub * (0.36f + 0.05f * pulse)
                withTransform({
                    translate(center.x, center.y + heart * 0.2f)
                    scale(heart, heart, Offset.Zero)
                }) { drawPath(unitHeart, Brush.verticalGradient(listOf(Fc.Rose, Fc.Crimson), -1f, 0.8f)) }
            }
            // Linguetta: oscilla quando un piolo la colpisce
            Canvas(
                Modifier
                    .align(Alignment.TopCenter)
                    .size(34.dp, 46.dp)
                    .graphicsLayer {
                        rotationZ = kick.value
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.22f)
                    },
            ) {
                val w = size.width
                val pivot = Offset(w / 2, size.height * 0.22f)
                val path = Path().apply {
                    moveTo(w * 0.14f, pivot.y)
                    cubicTo(w * 0.14f, -size.height * 0.08f, w * 0.86f, -size.height * 0.08f, w * 0.86f, pivot.y)
                    lineTo(w / 2, size.height)
                    close()
                }
                drawPath(path, Color.Black.copy(alpha = 0.35f))
                val lift = 2.dp.toPx()
                withTransform({ translate(0f, -lift) }) {
                    drawPath(path, Brush.verticalGradient(listOf(Color.White, Fc.Rose, Fc.Crimson)))
                    drawPath(path, Color.White, style = Stroke(1.5.dp.toPx()))
                    drawCircle(Color.White, w * 0.12f, pivot)
                }
            }
            HeartBurst(burst, listOf(Fc.Pink, Fc.Violet), Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = winner,
                transitionSpec = {
                    (fadeIn() + scaleIn(spring(dampingRatio = 0.6f), initialScale = 0.8f)) togetherWith fadeOut()
                },
                label = "prize",
            ) { index ->
                if (index == null) {
                    Text(
                        if (spinning) "Gira, gira…" else "Tocca la ruota e scopri cosa ti aspetta",
                        Modifier.fillMaxWidth(),
                        color = Fc.Muted,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    PrizeCard(prizes[index], colors[index], state.name(1 - turn))
                }
            }
        }
        if (prize != null && prize.randomPosition) {
            GradientButton(
                "Scopri la posizione",
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                listOf(Fc.Violet, Fc.DeepViolet),
                icon = FcIcon.MOON,
            ) { onOpenPosition(positions.random().id) }
        } else {
            GradientButton(
                if (prize != null) "Tocca a ${state.name(1 - turn)}" else "Gira la ruota",
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                listOf(Fc.Pink, Fc.DeepViolet),
                enabled = !spinning,
                icon = FcIcon.WHEEL,
            ) { spin() }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PrizeCard(prize: WheelPrize, color: Color, partner: String) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(20.dp, shape, ambientColor = color, spotColor = color)
            .clip(shape)
            .background(Fc.Bg1)
            .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.12f))))
            .glass(28.dp, Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(lerp(color, Color.White, 0.25f), color))),
            contentAlignment = Alignment.Center,
        ) { Glyph(prize.icon, size = 30.dp, tint = Color.White) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(prize.label.uppercase(), fontWeight = FontWeight.Bold, color = Fc.Gold, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(2.dp))
            Text(prize.text.forPartner(partner), fontFamily = Fc.Display, fontSize = 18.sp, lineHeight = 25.sp)
        }
        if (prize.seconds > 0) {
            Spacer(Modifier.width(10.dp))
            CountdownTimer(prize.seconds, Fc.Gold, size = 78.dp)
        }
    }
}
