package com.funcouple.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Illustrazione della posizione; con [animated] le due figure "respirano". */
@Composable
fun PosePictogram(position: Position, modifier: Modifier = Modifier, animated: Boolean = false) {
    val phase = if (animated) {
        val transition = rememberInfiniteTransition(label = "breath")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "phase",
        )
        v
    } else {
        0f
    }
    Image(
        painterResource(position.image),
        contentDescription = position.name,
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = 1f + phase * 0.03f
                scaleY = 1f + phase * 0.03f
            },
    )
}

@Composable
fun KamasutraScreen(state: AppState, onBack: () -> Unit, onMarathon: () -> Unit, onOpen: (String) -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val filters = listOf("Tutte", "Facili", "Medie", "Difficili", "Preferite")
    val list = when (filter) {
        0 -> positions
        4 -> positions.filter { it.id in state.favorites }
        else -> positions.filter { it.difficulty == filter }
    }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopBar("Kamasutra", onBack)
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filters.forEachIndexed { i, label ->
                Chip(label, filter == i, listOf(Fc.Pink, Fc.DeepViolet), icon = if (i == 4) FcIcon.HEART else null) { filter = i }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientButton("Sorprendimi", Modifier.weight(1f), listOf(Fc.Pink, Fc.DeepViolet), icon = FcIcon.SPARKLE) {
                onOpen(positions.random().id)
            }
            GradientButton("Maratona", Modifier.weight(1f), listOf(Fc.Violet, Fc.DeepViolet), icon = FcIcon.STOPWATCH, onClick = onMarathon)
        }
        Spacer(Modifier.height(12.dp))
        Medals(state)
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Nessuna preferita, per ora.\nTocca il cuore su una posizione per salvarla qui.",
                    textAlign = TextAlign.Center,
                    color = Fc.Muted,
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(list, key = { _, p -> p.id }) { index, position ->
                    Entrance(index.coerceAtMost(6)) {
                        PositionCard(position, position.id in state.favorites, position.id in state.tried) {
                            onOpen(position.id)
                        }
                    }
                }
            }
        }
    }
}

private class Medal(val title: String, val goal: String, val done: Boolean)

private fun medals(state: AppState): List<Medal> {
    val tried = state.tried
    fun all(difficulty: Int) = positions.filter { it.difficulty == difficulty }.all { it.id in tried }
    return listOf(
        Medal("Primi passi", "Prova una posizione", tried.isNotEmpty()),
        Medal("Esploratori", "Provane 10", tried.size >= 10),
        Medal("Instancabili", "Provane 25", tried.size >= 25),
        Medal("Senza pensieri", "Tutte le Facili", all(1)),
        Medal("Equilibristi", "Tutte le Medie", all(2)),
        Medal("Acrobati", "Tutte le Difficili", all(3)),
        Medal("Maratoneti", "Completa una Maratona", state.marathons > 0),
        Medal("Collezionisti", "5 preferite", state.favorites.size >= 5),
        Medal("Maestri", "Provatele tutte", tried.size >= positions.size),
    )
}

/** Fila di traguardi: quelli raggiunti si accendono d'oro. */
@Composable
private fun Medals(state: AppState) {
    val list = medals(state)
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        list.sortedByDescending { it.done }.forEach { medal ->
            Row(
                Modifier
                    .glass(18.dp, if (medal.done) Fc.Gold.copy(alpha = 0.16f) else Fc.Glass)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Glyph(FcIcon.TROPHY, Modifier.alpha(if (medal.done) 1f else 0.35f), 20.dp, if (medal.done) Fc.Gold else Fc.Muted)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(medal.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (medal.done) Fc.Gold else Fc.Text)
                    Text(if (medal.done) "Raggiunto" else medal.goal, fontSize = 11.sp, color = Fc.Muted)
                }
            }
        }
    }
}

@Composable
private fun PositionCard(position: Position, favorite: Boolean, tried: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .bounceClick(onClick = onClick)
            .glass(24.dp)
            .padding(10.dp),
    ) {
        Box {
            PosePictogram(
                position,
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.22f)),
            )
            if (favorite) Glyph(FcIcon.HEART, Modifier.align(Alignment.TopEnd).padding(8.dp), 16.dp, Fc.Pink)
            if (tried) {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Fc.Pink.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Glyph(FcIcon.CHECK, size = 10.dp, tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Provata", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            position.name,
            fontFamily = Fc.Display,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (position.difficulty > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(difficultyNames[position.difficulty], fontSize = 12.sp, color = Fc.Muted)
                Spacer(Modifier.width(6.dp))
                repeat(position.intensity) { Glyph(FcIcon.FLAME, size = 11.dp, tint = Fc.Orange) }
            }
        }
    }
}

@Composable
fun PositionDetailScreen(state: AppState, id: String, onBack: () -> Unit, onOpenOther: (String) -> Unit) {
    val position = remember(id) { positions.first { it.id == id } }
    val favorite = id in state.favorites
    val tried = id in state.tried
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopBar(position.name, onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Entrance(0) {
                Box(Modifier.tilt3d(1f).glass(32.dp)) {
                    PosePictogram(position, Modifier.fillMaxWidth().padding(12.dp), animated = true)
                    val heartScale by animateFloatAsState(
                        if (favorite) 1.2f else 1f,
                        spring(dampingRatio = 0.35f, stiffness = 300f),
                        label = "fav",
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                            .size(46.dp)
                            .bounceClick { state.toggleFavorite(id) }
                            .glass(23.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Glyph(
                            if (favorite) FcIcon.HEART else FcIcon.HEART_OUTLINE,
                            Modifier.graphicsLayer {
                                scaleX = heartScale
                                scaleY = heartScale
                            },
                            22.dp,
                            if (favorite) Fc.Pink else Fc.Text,
                        )
                    }
                }
            }
            // Le posizioni senza scheda mostrano solo l'illustrazione.
            if (position.description.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Entrance(1) {
                    Column {
                        Text(
                            position.tagline,
                            fontFamily = Fc.Display,
                            fontStyle = FontStyle.Italic,
                            fontSize = 20.sp,
                            color = Fc.Rose,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .glass(22.dp)
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Stat("Difficoltà", FcIcon.STAR, Fc.Gold, position.difficulty, 3)
                            Stat("Intimità", FcIcon.HEART, Fc.Pink, position.intimacy, 5)
                            Stat("Intensità", FcIcon.FLAME, Fc.Orange, position.intensity, 5)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Entrance(2) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Legend(Fc.Pink, "A")
                            Spacer(Modifier.width(14.dp))
                            Legend(Fc.Violet, "B")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(position.description, fontSize = 16.sp, lineHeight = 24.sp)
                        Spacer(Modifier.height(16.dp))
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .glass(22.dp, Fc.Gold.copy(alpha = 0.10f))
                                .padding(16.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Glyph(FcIcon.BULB, size = 16.dp, tint = Fc.Gold)
                                Spacer(Modifier.width(6.dp))
                                Text("Il consiglio", fontWeight = FontWeight.Bold, color = Fc.Gold, fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(position.tip, fontSize = 15.sp, lineHeight = 22.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GhostButton("Un'altra", Modifier.weight(1f), FcIcon.DICE) {
                onOpenOther(positions.filter { it.id != id }.random().id)
            }
            GradientButton(
                if (tried) "Provata!" else "L'abbiamo provata",
                Modifier.weight(1.4f),
                colors = if (tried) listOf(Fc.DeepViolet, Fc.Pink) else listOf(Fc.Pink, Fc.Crimson),
                icon = if (tried) FcIcon.CHECK else null,
            ) { state.toggleTried(id) }
        }
    }
}

@Composable
private fun Stat(label: String, icon: FcIcon, color: Color, value: Int, max: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(max) { i ->
                Glyph(icon, Modifier.alpha(if (i < value) 1f else 0.2f), 14.dp, color)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = Fc.Muted)
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text("Partner $label", fontSize = 12.sp, color = Fc.Muted)
    }
}
