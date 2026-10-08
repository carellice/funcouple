package com.funcouple.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// --- Geometria del cubo -------------------------------------------------------------------
// Coordinate: x a destra, y in basso, z verso chi guarda. Ogni faccia ha un asse "destra" (U),
// un asse "giù" (V) e una normale (N = U x V), così il contenuto resta dritto quando è di fronte.

private fun v(x: Float, y: Float, z: Float) = floatArrayOf(x, y, z)

private val FACE_U = arrayOf(v(1f, 0f, 0f), v(-1f, 0f, 0f), v(0f, 0f, -1f), v(0f, 0f, 1f), v(1f, 0f, 0f), v(1f, 0f, 0f))
private val FACE_V = arrayOf(v(0f, 1f, 0f), v(0f, 1f, 0f), v(0f, 1f, 0f), v(0f, 1f, 0f), v(0f, 0f, 1f), v(0f, 0f, -1f))
private val FACE_N = arrayOf(v(0f, 0f, 1f), v(0f, 0f, -1f), v(1f, 0f, 0f), v(-1f, 0f, 0f), v(0f, -1f, 0f), v(0f, 1f, 0f))

// Matrici 3x3 in ordine di riga.
private fun mul(a: FloatArray, b: FloatArray) = FloatArray(9) { i ->
    val r = i / 3
    val c = i % 3
    a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c]
}

private fun apply(m: FloatArray, p: FloatArray) = floatArrayOf(
    m[0] * p[0] + m[1] * p[1] + m[2] * p[2],
    m[3] * p[0] + m[4] * p[1] + m[5] * p[2],
    m[6] * p[0] + m[7] * p[1] + m[8] * p[2],
)

private fun rotX(a: Float) = floatArrayOf(1f, 0f, 0f, 0f, cos(a), -sin(a), 0f, sin(a), cos(a))
private fun rotY(a: Float) = floatArrayOf(cos(a), 0f, sin(a), 0f, 1f, 0f, -sin(a), 0f, cos(a))
private fun rotZ(a: Float) = floatArrayOf(cos(a), -sin(a), 0f, sin(a), cos(a), 0f, 0f, 0f, 1f)

/** Rotazione che porta la faccia [f] di fronte a chi guarda, con il contenuto dritto. */
private fun faceToFront(f: Int) = FACE_U[f] + FACE_V[f] + FACE_N[f]

private val LIGHT = v(-0.36f, -0.52f, 0.77f)

// --- Contenuto e lancio -------------------------------------------------------------------

private class DieFace(val icon: FcIcon?, val big: String?, val label: String)

private class FaceLayout(val icon: FcIcon?, val big: TextLayoutResult?, val label: TextLayoutResult)

/** Parametri casuali di un lancio: faccia finale, giri di rotazione e punto di partenza. */
private class DieThrow(
    val target: Int,
    val spinX: Float = 0f,
    val spinY: Float = 0f,
    val spinZ: Float = 0f,
    val startDx: Float = 0f,
    val restZ: Float = 0f,
)

private fun randomThrow(allowed: List<Int>): DieThrow {
    fun turns(min: Float, max: Float) =
        (min + Random.nextFloat() * (max - min)) * 2f * PI.toFloat() * (if (Random.nextBoolean()) 1 else -1)
    return DieThrow(
        target = allowed.random(),
        spinX = turns(1.6f, 3.2f),
        spinY = turns(1.2f, 2.8f),
        spinZ = turns(0.2f, 0.7f),
        startDx = Random.nextFloat() * 2f - 1f,
        restZ = (Random.nextFloat() - 0.5f) * 0.24f,
    )
}

private const val THROW_MS = 1500
private const val STAGGER_MS = 180
private const val DICE = 4
private const val TOTAL_MS = THROW_MS + STAGGER_MS * (DICE - 1)

/** Altezza dal tavolo durante il lancio: parte in aria e rimbalza quattro volte, sempre meno. */
private fun bounceHeight(t: Float) = 0.9f * (1f - t) * (1f - t) * abs(cos(3.5f * PI.toFloat() * t))

private val IMPACTS = listOf(0.143f, 0.429f, 0.714f)

private fun DrawScope.drawDie(
    orientation: FloatArray,
    center: Offset,
    half: Float,
    scale: Float,
    colors: List<Color>,
    faces: List<FaceLayout>,
) {
    for (f in 0 until 6) {
        val n = apply(orientation, FACE_N[f])
        if (n[2] <= 0.02f) continue // faccia girata dall'altra parte
        val u = apply(orientation, FACE_U[f])
        val w = apply(orientation, FACE_V[f])
        // Proiezione ortografica: la faccia è un parallelogramma, quindi basta una trasformazione affine.
        val matrix = Matrix()
        matrix.values[Matrix.ScaleX] = u[0] * scale
        matrix.values[Matrix.SkewY] = u[1] * scale
        matrix.values[Matrix.SkewX] = w[0] * scale
        matrix.values[Matrix.ScaleY] = w[1] * scale
        matrix.values[Matrix.TranslateX] = center.x + n[0] * half * scale
        matrix.values[Matrix.TranslateY] = center.y + n[1] * half * scale
        val light = (n[0] * LIGHT[0] + n[1] * LIGHT[1] + n[2] * LIGHT[2]).coerceIn(0f, 1f)
        withTransform({ transform(matrix) }) {
            val corner = Offset(-half, -half)
            val full = Size(half * 2, half * 2)
            drawRect(colors.last(), corner, full)
            drawRect(Color.Black.copy(alpha = 0.35f), corner, full)
            val inset = half * 0.06f
            val inner = Size(full.width - inset * 2, full.height - inset * 2)
            val radius = CornerRadius(half * 0.3f)
            drawRoundRect(
                Brush.linearGradient(colors, Offset(-half, -half), Offset(half, half)),
                Offset(-half + inset, -half + inset),
                inner,
                radius,
            )
            drawRoundRect(
                Color.White.copy(alpha = 0.35f),
                Offset(-half + inset, -half + inset),
                inner,
                radius,
                style = Stroke(half * 0.03f),
            )
            // L'ombra va sotto al contenuto, così icone e testi restano bianchi e leggibili.
            drawRect(Color.Black.copy(alpha = (1f - light) * 0.55f), corner, full)
            val face = faces[f]
            if (face.icon != null) {
                val glyph = half * 0.82f
                drawGlyph(face.icon, Offset(-glyph / 2, -half * 0.74f), glyph, Color.White)
            }
            if (face.big != null) {
                drawText(face.big, topLeft = Offset(-face.big.size.width / 2f, -half * 0.3f - face.big.size.height / 2f))
            }
            drawText(
                face.label,
                topLeft = Offset(-face.label.size.width / 2f, half * 0.5f - face.label.size.height / 2f),
            )
        }
    }
}

@Composable
fun DiceScreen(state: AppState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val turn = state.diceTurn
    val progress = remember { Animatable(1f) }
    val tiltSensor = LocalTilt.current
    var throws by remember { mutableStateOf(List(DICE) { DieThrow(target = it) }) }
    // Facce che ogni dado può mostrare: quelle escluse dai limiti della coppia non escono mai.
    val allowedFaces = remember(state.limits) {
        listOf(
            diceActions.map { it.verb },
            diceParts.map { it.phrase },
            diceDurations.map { "" },
            diceModes.map { "${it.label} ${it.phrase}" },
        ).map { faces -> faces.indices.filter { state.allows(faces[it]) }.ifEmpty { faces.indices.toList() } }
    }
    var rolling by remember { mutableStateOf(false) }
    var rolled by remember { mutableStateOf(false) }

    fun roll() {
        if (rolling) return
        rolling = true
        if (rolled) state.diceTurn = 1 - state.diceTurn
        rolled = false
        throws = List(DICE) { randomThrow(allowedFaces[it]) }
        scope.launch {
            progress.snapTo(0f)
            launch {
                // Un colpetto a ogni rimbalzo del primo dado.
                var elapsed = 0L
                IMPACTS.forEach { impact ->
                    val at = (impact * THROW_MS).toLong()
                    delay(at - elapsed)
                    elapsed = at
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    state.sounds.play(Sfx.DICE)
                }
            }
            progress.animateTo(1f, tween(TOTAL_MS, easing = LinearEasing))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            state.sounds.play(Sfx.SUCCESS)
            rolled = true
            rolling = false
        }
    }

    val action = diceActions[throws[0].target]
    val part = diceParts[throws[1].target]
    val seconds = diceDurations[throws[2].target]
    val mode = diceModes[throws[3].target]
    val count = if (state.fourthDie) DICE else DICE - 1
    val diceColors = listOf(
        listOf(Fc.Pink, Fc.Crimson),
        listOf(Fc.Violet, Fc.DeepViolet),
        listOf(Fc.Gold, Fc.Orange),
        listOf(Color(0xFF37D6C0), Color(0xFF1D7FA8)),
    )

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar("Dadi del Piacere", onBack)
        Spacer(Modifier.height(8.dp))
        TurnBanner(state, turn)
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            // Quattro dadi stanno su due file da due, tre su una fila sola.
            val grid = count == DICE
            val slot = with(density) { maxWidth.toPx() } / (if (grid) 2f else 3f)
            val half = slot * (if (grid) 0.24f else 0.33f)
            val layouts = remember(half) {
                val labelStyle = TextStyle(
                    fontFamily = Fc.Body,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = with(density) { (half * 0.27f).toSp() },
                    lineHeight = with(density) { (half * 0.3f).toSp() },
                    textAlign = TextAlign.Center,
                )
                val bigStyle = labelStyle.copy(fontSize = with(density) { (half * 0.78f).toSp() })
                val width = Constraints(maxWidth = (half * 1.8f).toInt())
                val dice = listOf(
                    diceActions.map { DieFace(it.icon, null, it.verb) },
                    diceParts.map { DieFace(FcIcon.TARGET, null, it.label) },
                    diceDurations.map { DieFace(null, "$it", "secondi") },
                    diceModes.map { DieFace(it.icon, null, it.label) },
                )
                dice.map { faces ->
                    faces.map { face ->
                        FaceLayout(
                            face.icon,
                            face.big?.let { measurer.measure(AnnotatedString(it), bigStyle) },
                            measurer.measure(AnnotatedString(face.label), labelStyle, constraints = width),
                        )
                    }
                }
            }
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(with(density) { (half * (if (grid) 7.2f else 4.2f)).toDp() })
                    .bounceClick(enabled = !rolling) { roll() },
            ) {
                // Il "tavolo" su cui cadono i dadi
                drawRoundRect(Color.White.copy(alpha = 0.05f), cornerRadius = CornerRadius(28.dp.toPx()))
                drawRoundRect(Fc.GlassBorder, cornerRadius = CornerRadius(28.dp.toPx()), style = Stroke(1.dp.toPx()))

                class Frame(val index: Int, val center: Offset, val scale: Float, val height: Float, val o: FloatArray)

                val frames = throws.take(count).mapIndexed { i, th ->
                    val t = ((progress.value * TOTAL_MS - i * STAGGER_MS) / THROW_MS).coerceIn(0f, 1f)
                    val ease = 1f - (1f - t) * (1f - t) * (1f - t)
                    val left = 1f - ease
                    val height = bounceHeight(t)
                    val rest = if (grid) {
                        Offset(slot * (i % 2 + 0.5f), size.height * (0.25f + 0.49f * (i / 2)))
                    } else {
                        Offset(slot * (i + 0.5f), size.height * 0.47f)
                    }
                    // Parte da sotto lo schermo, come lanciato dalla mano, e scivola verso il suo posto.
                    val center = rest + Offset(th.startDx * slot * 0.8f, size.height * 1.6f) * left
                    // I dadi sono veri cubi: inclinando il telefono si vedono un po' di più le altre facce.
                    val tilt = mul(
                        rotZ(th.restZ),
                        mul(rotX(-0.34f + (tiltSensor?.y ?: 0f) * 0.18f), rotY(0.4f + (tiltSensor?.x ?: 0f) * 0.22f)),
                    )
                    val spin = mul(rotX(th.spinX * left), mul(rotY(th.spinY * left), rotZ(th.spinZ * left)))
                    Frame(i, center, 1f + 0.45f * height, height, mul(tilt, mul(spin, faceToFront(th.target))))
                }
                frames.forEach { f ->
                    val w = half * 2.5f * (1f + f.height * 0.4f)
                    val h = half * 0.6f
                    val c = f.center + Offset(half * 0.15f, half * (1.3f + f.height * 1.1f))
                    drawOval(
                        Brush.radialGradient(
                            listOf(Color.Black.copy(alpha = 0.55f * (1f - f.height * 0.6f)), Color.Transparent),
                            center = c,
                            radius = w / 2,
                        ),
                        topLeft = Offset(c.x - w / 2, c.y - h / 2),
                        size = Size(w, h),
                    )
                }
                frames.sortedBy { it.scale }.forEach { f ->
                    drawDie(f.o, f.center, half, f.scale, diceColors[f.index], layouts[f.index])
                }
            }
        }
        Row(
            Modifier
                .padding(top = 10.dp)
                .alpha(if (rolling) 0.5f else 1f)
                .bounceClick(enabled = !rolling) {
                    state.updateFourthDie(!state.fourthDie)
                    rolled = false
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Toggle(state.fourthDie, listOf(Color(0xFF37D6C0), Color(0xFF1D7FA8)))
            Spacer(Modifier.width(10.dp))
            Text("Quarto dado: come farlo", fontSize = 14.sp, color = if (state.fourthDie) Fc.Text else Fc.Muted)
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                rolled,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${action.verb} ${part.phrase} di ${state.name(1 - turn)} per ${formatSeconds(seconds)}" +
                            if (state.fourthDie) ", ${mode.phrase}" else "",
                        fontFamily = Fc.Display,
                        fontSize = 21.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    CountdownTimer(seconds, Fc.Gold, size = 92.dp)
                }
            }
            if (!rolled && !rolling) {
                Text(
                    "Lancia i dadi e lascia decidere al destino…",
                    color = Fc.Muted,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        GradientButton(
            if (rolled) "Tocca a ${state.name(1 - turn)}" else "Lancia i dadi",
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            listOf(Fc.Gold, Fc.Orange),
            enabled = !rolling,
            icon = FcIcon.DICE,
        ) { roll() }
        Spacer(Modifier.height(24.dp))
    }
}
