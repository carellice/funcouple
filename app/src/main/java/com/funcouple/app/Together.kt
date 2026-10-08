package com.funcouple.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private const val DAY_MS = 86_400_000L
private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN)

fun formatDate(date: LocalDate): String = date.format(longDate)

private fun count(n: Int, one: String, many: String) = if (n == 1) "1 $one" else "$n $many"

private fun thousands(n: Int) = "%,d".format(Locale.ITALIAN, n)

/** "3 anni, 4 mesi e 12 giorni", saltando le parti a zero. */
private fun elapsed(period: Period): String {
    val parts = listOfNotNull(
        count(period.years, "anno", "anni").takeIf { period.years > 0 },
        count(period.months, "mese", "mesi").takeIf { period.months > 0 },
        count(period.days, "giorno", "giorni").takeIf { period.days > 0 },
    )
    return if (parts.size > 1) parts.dropLast(1).joinToString(", ") + " e " + parts.last() else parts.firstOrNull().orEmpty()
}

/** La ricorrenza di oggi, se c'è, altrimenti quanto manca al prossimo anniversario. */
private fun milestone(since: LocalDate, today: LocalDate): String {
    val period = Period.between(since, today)
    val days = ChronoUnit.DAYS.between(since, today).toInt()
    val left = ChronoUnit.DAYS.between(today, since.plusYears(period.years + 1L)).toInt()
    return when {
        days == 0 -> "Oggi inizia la vostra storia"
        period.months == 0 && period.days == 0 -> "Buon anniversario: ${count(period.years, "anno", "anni")} oggi!"
        // Chi si è messo insieme il 29 febbraio festeggia il 28 negli anni non bisestili.
        left == 0 -> "Buon anniversario: ${count(period.years + 1, "anno", "anni")} oggi!"
        days % 100 == 0 -> "Traguardo: ${thousands(days)} giorni insieme!"
        period.days == 0 -> "Buon mesiversario: ${count(period.toTotalMonths().toInt(), "mese", "mesi")} oggi!"
        else -> "${if (left == 1) "Manca 1 giorno" else "Mancano $left giorni"} al ${period.years + 1}° anniversario"
    }
}

/** In cima alla home: da quanti giorni la coppia sta insieme. Toccandola si sceglie la data. */
@Composable
fun TogetherCard(state: AppState) {
    var picking by remember { mutableStateOf(false) }
    if (picking) SinceDialog(state) { picking = false }
    val since = state.since
    if (since == null) {
        Row(
            Modifier
                .bounceClick { picking = true }
                .glass(20.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Glyph(FcIcon.HEART_OUTLINE, size = 16.dp, tint = Fc.Rose)
            Spacer(Modifier.width(8.dp))
            Text("Da quando state insieme?", color = Fc.Muted, fontSize = 14.sp)
        }
        return
    }
    val today = remember { LocalDate.now() }
    val days = ChronoUnit.DAYS.between(since, today).toInt()
    val shown = remember { Animatable(0f) }
    LaunchedEffect(days) { shown.animateTo(days.toFloat(), tween(1400, 300, FastOutSlowInEasing)) }
    Column(
        Modifier
            .fillMaxWidth()
            .tilt3d(0.5f)
            .bounceClick { picking = true }
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Fc.Pink.copy(alpha = 0.24f), Fc.DeepViolet.copy(alpha = 0.24f))))
            .glass(28.dp, Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            thousands(shown.value.roundToInt()),
            style = TextStyle(
                brush = Brush.horizontalGradient(listOf(Fc.Rose, Fc.Gold)),
                fontFamily = Fc.Display,
                fontWeight = FontWeight.Bold,
                fontSize = 46.sp,
                lineHeight = 50.sp,
            ),
        )
        Text(if (days == 1) "giorno insieme" else "giorni insieme", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            // Sotto il mese gli anni e i mesi non aggiungono nulla al numero qui sopra.
            if (days > 31) "${elapsed(Period.between(since, today))}, dal ${formatDate(since)}" else "Dal ${formatDate(since)}",
            color = Fc.Muted,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .clip(CircleShape)
                .background(Fc.Gold.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Glyph(FcIcon.SPARKLE, size = 14.dp, tint = Fc.Gold)
            Spacer(Modifier.width(6.dp))
            Text(milestone(since, today), color = Fc.Gold, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Calendario per scegliere, cambiare o togliere la data di fidanzamento. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SinceDialog(state: AppState, onDismiss: () -> Unit) {
    val today = remember { LocalDate.now() }
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = (state.since ?: today).toEpochDay() * DAY_MS,
        yearRange = 1950..today.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DAY_MS <= today.toEpochDay()
            override fun isSelectableYear(year: Int) = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = picker.selectedDateMillis != null,
                onClick = {
                    picker.selectedDateMillis?.let { state.updateSince(LocalDate.ofEpochDay(it / DAY_MS)) }
                    onDismiss()
                },
            ) { Text("Salva") }
        },
        dismissButton = {
            Row {
                if (state.since != null) {
                    TextButton({
                        state.updateSince(null)
                        onDismiss()
                    }) { Text("Rimuovi", color = Fc.Muted) }
                }
                TextButton(onDismiss) { Text("Annulla", color = Fc.Muted) }
            }
        },
    ) {
        DatePicker(
            picker,
            title = { Text("Da quando state insieme?", Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) },
        )
    }
}
