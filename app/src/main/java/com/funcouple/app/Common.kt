package com.funcouple.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Cuore unitario (largo circa 2, alto circa 1.6) centrato sull'origine. */
val unitHeart: Path = Path().apply {
    moveTo(0f, 0.75f)
    cubicTo(-1.5f, -0.2f, -0.75f, -1.25f, 0f, -0.45f)
    cubicTo(0.75f, -1.25f, 1.5f, -0.2f, 0f, 0.75f)
    close()
}

private class FloatingHeart(
    val x: Float,
    val phase: Float,
    val speed: Int,
    val size: Float,
    val sway: Float,
    val alpha: Float,
    val violet: Boolean,
)

@Composable
fun AnimatedBackground() {
    val transition = rememberInfiniteTransition(label = "bg")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(48_000, easing = LinearEasing)),
        label = "t",
    )
    val hearts = remember {
        val rnd = Random(7)
        List(18) {
            FloatingHeart(
                x = rnd.nextFloat(),
                phase = rnd.nextFloat(),
                speed = 1 + rnd.nextInt(3),
                size = 6f + rnd.nextFloat() * 16f,
                sway = 8f + rnd.nextFloat() * 22f,
                alpha = 0.06f + rnd.nextFloat() * 0.14f,
                violet = rnd.nextInt(4) == 0,
            )
        }
    }
    val tilt = LocalTilt.current
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val wave = sin(t * 2f * PI.toFloat())
        // Parallasse: i bagliori lontani si spostano poco, i cuori più grandi (più vicini) di più.
        val tx = tilt?.x ?: 0f
        val ty = tilt?.y ?: 0f
        drawRect(Brush.verticalGradient(listOf(Fc.Bg0, Fc.Bg1, Fc.Bg2)))
        val far = Offset(tx * 14.dp.toPx(), -ty * 14.dp.toPx())
        val c1 = Offset(w * (0.2f + 0.12f * wave), h * 0.14f) + far
        drawCircle(Brush.radialGradient(listOf(Fc.Pink.copy(alpha = 0.30f), Color.Transparent), c1, w * 0.85f), w * 0.85f, c1)
        val c2 = Offset(w * (0.9f - 0.12f * wave), h * 0.82f) + far
        drawCircle(Brush.radialGradient(listOf(Fc.DeepViolet.copy(alpha = 0.28f), Color.Transparent), c2, w * 0.9f), w * 0.9f, c2)
        hearts.forEach { p ->
            val progress = (t * p.speed + p.phase) % 1f
            val near = (p.size * 2.2f).dp.toPx()
            val y = h * (1.08f - progress * 1.16f) + ty * near
            val x = w * p.x + sin(progress * 4f * PI.toFloat() + p.phase * 6f) * p.sway.dp.toPx() - tx * near
            val s = p.size.dp.toPx()
            val fade = sin(progress * PI.toFloat())
            withTransform({
                translate(x, y)
                scale(s, s, Offset.Zero)
            }) {
                drawPath(unitHeart, (if (p.violet) Fc.Violet else Fc.Pink).copy(alpha = p.alpha * fade))
            }
        }
    }
}

fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "bounce",
    )
    val haptic = LocalHapticFeedback.current
    val sounds = LocalSounds.current
    // Mentre è premuto, l'elemento si piega in 3D verso il punto toccato.
    var press by remember { mutableStateOf(Offset.Zero) }
    var bounds by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    LaunchedEffect(source) {
        source.interactions.collect {
            if (it is androidx.compose.foundation.interaction.PressInteraction.Press) press = it.pressPosition
        }
    }
    val lean by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "lean")
    onSizeChanged { bounds = it }.graphicsLayer {
        scaleX = scale
        scaleY = scale
        if (bounds.width > 0 && bounds.height > 0) {
            rotationY = (press.x / bounds.width - 0.5f) * 12f * lean
            rotationX = -(press.y / bounds.height - 0.5f) * 12f * lean
            cameraDistance = 12f * density
        }
    }.clickable(interactionSource = source, indication = null, enabled = enabled) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        sounds?.play(Sfx.TAP)
        onClick()
    }
}

fun Modifier.glass(radius: Dp = 28.dp, tint: Color = Fc.Glass): Modifier {
    val shape = RoundedCornerShape(radius)
    // Luce dall'alto: bordo e superficie più chiari in cima danno spessore alla lastra.
    return clip(shape)
        .background(tint)
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.06f))), shape)
}

/** Fa entrare il contenuto dal basso con un leggero ritardo in base all'indice. */
@Composable
fun Entrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(60L + index * 80L)
        progress.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 180f))
    }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value.coerceIn(0f, 1f)
            translationY = (1f - progress.value) * 48.dp.toPx()
            val s = 0.9f + 0.1f * progress.value
            scaleX = s
            scaleY = s
        },
    ) { content() }
}

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Fc.Pink, Fc.Crimson),
    enabled: Boolean = true,
    icon: FcIcon? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(29.dp)
    Box(
        modifier
            .height(58.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .bounceClick(enabled, onClick)
            .shadow(16.dp, shape, ambientColor = colors.first(), spotColor = colors.first())
            // Bordo inferiore più scuro: il pulsante sembra avere uno spessore.
            .drawBehind {
                drawRoundRect(
                    lerp(colors.last(), Color.Black, 0.45f),
                    topLeft = Offset(0f, 4.dp.toPx()),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                )
            }
            .clip(shape)
            .background(Brush.horizontalGradient(colors))
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.30f),
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.18f),
                ),
            )
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)), shape),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Glyph(icon, size = 20.dp, tint = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, icon: FcIcon? = null, onClick: () -> Unit) {
    Box(
        modifier
            .height(58.dp)
            .bounceClick(onClick = onClick)
            .glass(29.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Glyph(icon, size = 18.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .bounceClick(onClick = onBack)
                .glass(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(16.dp)) {
                val stroke = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
                val path = Path().apply {
                    moveTo(size.width * 0.65f, size.height * 0.1f)
                    lineTo(size.width * 0.25f, size.height * 0.5f)
                    lineTo(size.width * 0.65f, size.height * 0.9f)
                }
                drawPath(path, Fc.Text, style = stroke)
            }
        }
        Text(
            title,
            Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontFamily = Fc.Display,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            maxLines = 1,
        )
        Spacer(Modifier.width(44.dp))
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    icon: FcIcon? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val base = modifier
        .bounceClick(onClick = onClick)
        .height(40.dp)
    Box(
        if (selected) base.clip(shape).background(Brush.horizontalGradient(colors)) else base.glass(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        val color = if (selected) Color.White else Fc.Muted
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Glyph(icon, size = 14.dp, tint = color)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = color,
                maxLines = 1,
            )
        }
    }
}

fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/** Timer circolare: tocca per avviare o mettere in pausa, tocca a fine corsa per azzerare. */
@Composable
fun CountdownTimer(seconds: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 104.dp) {
    var remaining by remember(seconds) { mutableIntStateOf(seconds) }
    var running by remember(seconds) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val sounds = LocalSounds.current
    LaunchedEffect(running, seconds) {
        while (running && remaining > 0) {
            delay(1000)
            remaining--
        }
        if (running && remaining == 0) {
            running = false
            sounds?.play(Sfx.BELL)
            repeat(3) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(180)
            }
        }
    }
    val progress by animateFloatAsState(
        targetValue = remaining / seconds.toFloat(),
        animationSpec = tween(if (running) 1000 else 300, easing = LinearEasing),
        label = "timer",
    )
    Box(
        modifier
            .size(size)
            .bounceClick {
                if (remaining == 0) remaining = seconds else running = !running
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.12f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatTime(remaining), fontSize = (size.value * 0.22f).sp, fontWeight = FontWeight.Bold)
            Text(
                when {
                    remaining == 0 -> "fine!"
                    running -> "pausa"
                    else -> "avvia"
                },
                fontSize = 11.sp,
                color = Fc.Muted,
                modifier = Modifier.clip(CircleShape),
            )
        }
    }
}

/** Finestra di conferma per le azioni che non si possono annullare. */
@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(Fc.Bg1)
                .border(1.dp, Fc.GlassBorder, RoundedCornerShape(30.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontFamily = Fc.Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(text, color = Fc.Muted, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                GhostButton("Annulla", Modifier.weight(1f), onClick = onDismiss)
                GradientButton(confirm, Modifier.weight(1f), onClick = onConfirm)
            }
        }
    }
}
