package com.funcouple.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Cuore che pulsa come un battito. */
@Composable
fun BeatingHeart(size: Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "heart")
    val beat by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(620, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beat",
    )
    Canvas(modifier.size(size)) {
        val c = Offset(this.size.width / 2, this.size.height / 2)
        drawCircle(
            Brush.radialGradient(listOf(Fc.Pink.copy(alpha = 0.55f), Color.Transparent), c, this.size.width * 0.6f * beat),
            this.size.width * 0.6f * beat,
            c,
        )
        val s = this.size.width * 0.3f * beat
        withTransform({
            translate(c.x, c.y + s * 0.2f)
            scale(s, s, Offset.Zero)
        }) {
            drawPath(unitHeart, Brush.verticalGradient(listOf(Fc.Rose, Fc.Crimson), startY = -1f, endY = 0.8f))
        }
    }
}

@Composable
private fun Logo(fontSize: Int) {
    Text(
        "FunCouple",
        style = TextStyle(
            brush = Brush.horizontalGradient(listOf(Fc.Rose, Fc.Pink, Fc.Gold)),
            fontFamily = Fc.Display,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSize = fontSize.sp,
        ),
    )
}

@Composable
private fun NameField(value: String, label: String, accent: Color, last: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 16) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = if (last) ImeAction.Done else ImeAction.Next,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = Fc.GlassBorder,
            focusedLabelColor = accent,
            unfocusedLabelColor = Fc.Muted,
            cursorColor = accent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = Fc.Text,
            unfocusedTextColor = Fc.Text,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun OnboardingScreen(state: AppState) {
    var first by rememberSaveable { mutableStateOf(state.name1) }
    var second by rememberSaveable { mutableStateOf(state.name2) }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Entrance(0) { BeatingHeart(130.dp) }
        Entrance(1) { Logo(46) }
        Entrance(2) {
            Text(
                "Il gioco che accende la coppia",
                color = Fc.Muted,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(36.dp))
        Entrance(3) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NameField(first, "Il tuo nome", Fc.Pink, last = false) { first = it }
                NameField(second, "Il nome del tuo partner", Fc.Violet, last = true) { second = it }
            }
        }
        Spacer(Modifier.height(20.dp))
        Entrance(4) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .bounceClick { confirmed = !confirmed }
                    .glass(20.dp)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (confirmed) Fc.Pink else Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (confirmed) Glyph(FcIcon.CHECK, size = 16.dp, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Siamo entrambi maggiorenni e giochiamo di comune accordo.",
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Entrance(5) {
            GradientButton(
                "Iniziamo a giocare",
                Modifier.fillMaxWidth(),
                enabled = confirmed && first.isNotBlank() && second.isNotBlank(),
                icon = FcIcon.FLAME,
            ) { state.saveNames(first, second) }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Contenuti per adulti (18+). Tutto resta sul tuo dispositivo.",
            fontSize = 12.sp,
            color = Fc.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

private data class Mode(
    val icon: FcIcon,
    val title: String,
    val subtitle: String,
    val colors: List<Color>,
    val screen: Screen,
)

@Composable
fun HomeScreen(state: AppState, onOpen: (Screen) -> Unit) {
    val modes = listOf(
        Mode(FcIcon.FLAME, "Verità o Obbligo", "Quattro livelli, da dolce a estremo", listOf(Fc.Pink, Fc.Crimson), Screen.Challenges),
        Mode(
            FcIcon.MOON,
            "Kamasutra",
            "${state.tried.size} di ${positions.size} posizioni provate",
            listOf(Fc.Violet, Fc.DeepViolet),
            Screen.Kamasutra,
        ),
        Mode(FcIcon.DICE, "Dadi del Piacere", "Cosa, dove, quanto e come", listOf(Fc.Gold, Fc.Orange), Screen.Dice),
        Mode(FcIcon.WHEEL, "Ruota del Desiderio", "Gira e scopri il tuo premio", listOf(Fc.Rose, Fc.DeepViolet), Screen.Wheel),
    )
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Entrance(0) { BeatingHeart(96.dp, Modifier.tilt3d(1.6f)) }
        Entrance(1) { Box(Modifier.tilt3d(1.2f)) { Logo(40) } }
        Entrance(2) {
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(state.name1, fontWeight = FontWeight.SemiBold, color = Fc.Rose, fontSize = 16.sp)
                Glyph(FcIcon.HEART, Modifier.padding(horizontal = 10.dp), 14.dp, Fc.Pink)
                Text(state.name2, fontWeight = FontWeight.SemiBold, color = Fc.Violet, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Entrance(3) { TogetherCard(state) }
        Spacer(Modifier.height(16.dp))
        modes.forEachIndexed { index, mode ->
            Entrance(index + 4) { ModeCard(mode) { onOpen(mode.screen) } }
            Spacer(Modifier.height(12.dp))
        }
        val extras = listOf(
            Mode(FcIcon.GLASS, "Non ho mai", "Chi l'ha fatto paga pegno", listOf(Fc.Pink, Fc.Orange), Screen.Never),
            Mode(FcIcon.STOPWATCH, "Preliminari", "Cinque tappe a tempo", listOf(Fc.Orange, Fc.Crimson), Screen.Foreplay),
            Mode(
                FcIcon.TICKET,
                "Buoni",
                "${state.wallet[0]} e ${state.wallet[1]} punti",
                listOf(Fc.Gold, Fc.Orange),
                Screen.Coupons,
            ),
            Mode(
                FcIcon.PEN,
                "Le vostre carte",
                if (state.customCards.isEmpty()) "Scrivetele in segreto" else "${state.customCards.size} nel mazzo",
                listOf(Fc.Violet, Fc.Pink),
                Screen.CustomCards,
            ),
        )
        extras.chunked(2).forEachIndexed { row, pair ->
            Entrance(modes.size + 4 + row) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { mode -> ModeTile(mode, Modifier.weight(1f)) { onOpen(mode.screen) } }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Entrance(modes.size + 6) {
            Row(
                Modifier
                    .bounceClick { onOpen(Screen.Settings) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Glyph(FcIcon.GEAR, size = 18.dp, tint = Fc.Muted)
                Spacer(Modifier.width(8.dp))
                Text("Impostazioni", color = Fc.Muted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ModeCard(mode: Mode, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .tilt3d(0.5f)
            .bounceClick(onClick = onClick)
            .clip(shape)
            .background(Brush.horizontalGradient(mode.colors.map { it.copy(alpha = 0.30f) }))
            .glass(28.dp, Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(mode.icon, mode.colors, 62.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(mode.title, fontFamily = Fc.Display, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text(mode.subtitle, color = Fc.Muted, fontSize = 13.sp)
        }
        Glyph(FcIcon.CHEVRON, size = 20.dp, tint = Fc.Muted)
    }
}

@Composable
private fun ModeTile(mode: Mode, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .tilt3d(0.5f)
            .bounceClick(onClick = onClick)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(mode.colors.map { it.copy(alpha = 0.26f) }))
            .glass(26.dp, Color.Transparent)
            .padding(14.dp),
    ) {
        IconTile(mode.icon, mode.colors, 46.dp)
        Spacer(Modifier.height(10.dp))
        Text(mode.title, fontFamily = Fc.Display, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
        Text(mode.subtitle, color = Fc.Muted, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun SettingsRow(icon: FcIcon, title: String, subtitle: String, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .glass(22.dp)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, listOf(Fc.Pink, Fc.DeepViolet), 42.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = Fc.Muted, fontSize = 13.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit, onOpen: (Screen) -> Unit) {
    var first by remember { mutableStateOf(state.name1) }
    var second by remember { mutableStateOf(state.name2) }
    var resetDone by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var pickingSince by remember { mutableStateOf(false) }
    if (pickingSince) SinceDialog(state) { pickingSince = false }
    val context = androidx.compose.ui.platform.LocalContext.current
    val biometricAvailable = remember { Biometrics.available(context) }
    if (confirmReset) {
        ConfirmDialog(
            "Azzerare i progressi?",
            "Verranno cancellate le preferite, le posizioni provate e i traguardi del Kamasutra. Non si può annullare.",
            "Sì, azzera",
            onConfirm = {
                state.resetProgress()
                resetDone = true
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding(),
    ) {
        TopBar("Impostazioni", onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NameField(first, "Il tuo nome", Fc.Pink, last = false) { first = it }
            NameField(second, "Il nome del tuo partner", Fc.Violet, last = true) { second = it }
            GradientButton(
                "Salva i nomi",
                Modifier.fillMaxWidth(),
                enabled = first.isNotBlank() && second.isNotBlank() &&
                    (first.trim() != state.name1 || second.trim() != state.name2),
            ) { state.saveNames(first, second) }
            Spacer(Modifier.height(8.dp))
            SettingsRow(
                FcIcon.HEART,
                "Insieme dal",
                state.since?.let(::formatDate) ?: "Scegliete la data: la home conterà i vostri giorni",
                { pickingSince = true },
            ) { Glyph(FcIcon.CHEVRON, size = 18.dp, tint = Fc.Muted) }
            val updater = state.updater
            val update = updater.status
            val scope = rememberCoroutineScope()
            SettingsRow(
                FcIcon.DOWNLOAD,
                when (update) {
                    is Updater.Status.Available -> "Aggiornamento disponibile"
                    is Updater.Status.Downloading -> "Scarico la ${update.version}…"
                    is Updater.Status.Ready -> "Aggiornamento pronto"
                    else -> "Aggiornamenti"
                },
                when (update) {
                    Updater.Status.Idle -> "Versione ${updater.currentVersion}: tocca per controllare"
                    Updater.Status.Checking -> "Controllo in corso…"
                    Updater.Status.UpToDate -> "Versione ${updater.currentVersion}: hai già l'ultima"
                    is Updater.Status.Available -> "Versione ${update.version}: tocca per scaricarla e installarla"
                    is Updater.Status.Downloading -> "${(update.progress * 100).toInt()}% completato"
                    is Updater.Status.Ready -> "Versione ${update.version}: tocca per installarla"
                    is Updater.Status.Failed -> update.message
                },
                {
                    when (update) {
                        Updater.Status.Checking, is Updater.Status.Downloading -> Unit
                        is Updater.Status.Available -> scope.launch { updater.download(update) }
                        is Updater.Status.Ready -> updater.install(update)
                        else -> scope.launch { updater.check() }
                    }
                },
            ) {
                when (update) {
                    is Updater.Status.Available, is Updater.Status.Ready -> Glyph(FcIcon.DOWNLOAD, size = 22.dp, tint = Fc.Gold)
                    Updater.Status.UpToDate -> Glyph(FcIcon.CHECK, size = 20.dp, tint = Fc.Gold)
                    else -> Glyph(FcIcon.REPLAY, size = 18.dp, tint = Fc.Muted)
                }
            }
            SettingsRow(
                FcIcon.SHIELD,
                "Preferenze e limiti",
                if (state.limits.isEmpty()) "Tutto ammesso: scegliete cosa escludere" else if (state.limits.size == 1) "1 categoria esclusa" else "${state.limits.size} categorie escluse",
                { onOpen(Screen.Limits) },
            ) { Glyph(FcIcon.CHEVRON, size = 18.dp, tint = Fc.Muted) }
            SettingsRow(
                FcIcon.MOON,
                "Luci basse",
                "Schermo più scuro e caldo, per non rovinare l'atmosfera",
                { state.updateDim(!state.dim) },
            ) { Toggle(state.dim) }
            SettingsRow(
                FcIcon.DICE,
                "Effetto 3D",
                "L'app segue l'inclinazione del telefono",
                { state.updateDepth(!state.depth) },
            ) { Toggle(state.depth) }
            SettingsRow(
                FcIcon.SOUND,
                "Suoni",
                "Effetti sonori per carte, dadi, ruota e timer",
                { state.updateSound(!state.soundOn) },
            ) { Toggle(state.soundOn) }
            SettingsRow(
                FcIcon.LOCK,
                "Blocco con PIN",
                if (state.hasPin) "Attivo: tocca per rimuoverlo" else "Chiedi un PIN a ogni apertura",
                { onOpen(Screen.PinSetup) },
            ) { Glyph(FcIcon.CHEVRON, size = 18.dp, tint = Fc.Muted) }
            SettingsRow(
                FcIcon.FINGERPRINT,
                "Sblocco biometrico",
                when {
                    !biometricAvailable -> "Registra prima un'impronta o il volto nelle impostazioni del telefono"
                    !state.hasPin -> "Imposta prima un PIN: resta come riserva"
                    state.biometric -> "Attivo: impronta o volto al posto del PIN"
                    else -> "Entra con l'impronta o il volto invece del PIN"
                },
                {
                    when {
                        !biometricAvailable || !state.hasPin -> Unit
                        state.biometric -> state.updateBiometric(false)
                        // Si attiva solo dopo una verifica riuscita.
                        else -> Biometrics.prompt(context, "Attiva lo sblocco biometrico") { state.updateBiometric(true) }
                    }
                },
            ) {
                Box(Modifier.alpha(if (biometricAvailable && state.hasPin) 1f else 0.35f)) { Toggle(state.biometric) }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .glass(22.dp)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Glyph(FcIcon.SHIELD, size = 18.dp, tint = Fc.Rose)
                    Spacer(Modifier.width(8.dp))
                    Text("La parola di sicurezza", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Prima di giocare scegliete insieme una parola: se uno dei due la pronuncia, ci si ferma subito. " +
                        "Ogni sfida si può sempre saltare, senza spiegazioni.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Fc.Muted,
                )
            }
            GhostButton("Rivedi l'introduzione", Modifier.fillMaxWidth(), FcIcon.PLAY) {
                state.replayOnboarding()
            }
            GhostButton(
                if (resetDone) "Progressi azzerati" else "Azzera preferite e posizioni provate",
                Modifier.fillMaxWidth(),
                if (resetDone) FcIcon.CHECK else FcIcon.REPLAY,
            ) { confirmReset = true }
        }
    }
}
