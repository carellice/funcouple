package com.funcouple.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Set di icone dell'app, disegnate su una griglia 24x24 con tratto 2 e terminali arrotondati.
 * Ogni parte è un tracciato SVG (solo contorno), oppure:
 * "f:<tracciato>" riempito, "c:x,y,r" cerchio pieno, "o:x,y,r" cerchio a contorno.
 */
enum class FcIcon(private vararg val parts: String) {
    FLAME(
        "M12 3c0.5 3 2.2 4.6 3.7 6.3 1.4 1.6 2.3 3.2 2.3 5.2a6 6 0 0 1 -12 0c0 -1.8 0.7 -3.2 1.7 -4.4 0.4 1.3 1 2.1 2 2.6C9.3 9.3 10.2 5.8 12 3z",
        "M12 20.5a2.5 2.5 0 0 0 2.5 -2.5c0 -1.4 -1 -2.2 -2.5 -3.8 -1.5 1.6 -2.5 2.4 -2.5 3.8a2.5 2.5 0 0 0 2.5 2.5z",
    ),
    MOON("M20 14.5A8.5 8.5 0 1 1 9.5 4a8 8 0 0 0 10.5 10.5z", "M17.5 3.5v3M16 5h3"),
    DICE(
        "M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1 -3 3H6a3 3 0 0 1 -3 -3V6a3 3 0 0 1 3 -3z",
        "c:8,8,1.4", "c:16,8,1.4", "c:12,12,1.4", "c:8,16,1.4", "c:16,16,1.4",
    ),
    WHEEL(
        "o:12,12,9", "o:12,12,2",
        "M12 3v7M12 14v7M3 12h7M14 12h7M5.6 5.6l5 5M13.4 13.4l5 5M18.4 5.6l-5 5M10.6 13.4l-5 5",
    ),
    GEAR(
        "o:12,12,3", "o:12,12,6.5",
        "M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M18.7 5.3l-2.1 2.1M7.4 16.6l-2.1 2.1",
    ),
    FLOWER(
        "M12 12c-3 -2 -3 -7 0 -9 3 2 3 7 0 9z",
        "M12 12c-3 2 -3 7 0 9 3 -2 3 -7 0 -9z",
        "M12 12c-2 -3 -7 -3 -9 0 2 3 7 3 9 0z",
        "M12 12c2 -3 7 -3 9 0 -2 3 -7 3 -9 0z",
        "c:12,12,1.7",
    ),
    CHILI(
        "M8 7.5c4 -1.5 8 0.5 9 3.5 1 3.5 -1.500 7.500 -6.500 9.500 -2 0.800 -4.500 0.800 -6.500 -0.500 4 -0.500 7 -3 7.500 -6.200 0.300 -2.200 -1.300 -4.300 -3.500 -6.300z",
        "M8 7.5c-0.500 -2 0.500 -3.500 2.500 -4.500",
    ),
    DEVIL(
        "o:12,13.5,7",
        "M6.5 9C5 7.500 4.500 5.500 5 3.500c1.500 1 3 2.200 4 3.800",
        "M17.5 9C19 7.500 19.500 5.500 19 3.500c-1.500 1 -3 2.200 -4 3.800",
        "M8.500 12l2.500 1M15.500 12l-2.500 1",
        "M9 16c1.800 1.600 4.200 1.600 6 0",
    ),
    CHAT(
        "M4 5.500A2.500 2.500 0 0 1 6.500 3h11A2.500 2.500 0 0 1 20 5.500v8a2.500 2.500 0 0 1 -2.500 2.500H11l-5 4v-4.200A2.500 2.500 0 0 1 4 13.500z",
        "c:8.5,9.5,1.1", "c:12,9.5,1.1", "c:15.5,9.5,1.1",
    ),
    BOLT("M13 2.500L5 13.500h6l-1 8 8 -11h-6z"),
    HEART("f:M12 20.500l-1.300 -1.200C5.800 14.900 3 12.300 3 8.900 3 6.200 5.100 4 7.800 4c1.600 0 3.100 0.800 4.200 2 1.100 -1.200 2.600 -2 4.200 -2C18.900 4 21 6.200 21 8.900c0 3.400 -2.800 6 -7.700 10.400z"),
    HEART_OUTLINE("M12 20.500l-1.300 -1.200C5.800 14.900 3 12.300 3 8.900 3 6.200 5.100 4 7.800 4c1.600 0 3.100 0.800 4.200 2 1.100 -1.200 2.600 -2 4.200 -2C18.900 4 21 6.200 21 8.900c0 3.400 -2.800 6 -7.700 10.400z"),
    STAR("f:M12 3l2.700 5.800 6.300 0.800 -4.600 4.400 1.200 6.300L12 17.200l-5.600 3.100 1.200 -6.300L3 9.600l6.300 -0.800z"),
    SPARKLE(
        "M11 3c0.600 4.500 2 6.400 6.500 7 -4.500 0.600 -5.900 2.500 -6.500 7 -0.600 -4.500 -2 -6.400 -6.500 -7 4.500 -0.600 5.900 -2.500 6.500 -7z",
        "M18.500 15.500v5M16 18h5",
    ),
    BULB(
        "M12 3a6 6 0 0 0 -3.500 10.900c0.600 0.500 1 1.200 1 2v0.100h5v-0.100c0 -0.800 0.400 -1.500 1 -2A6 6 0 0 0 12 3z",
        "M9.500 18.700h5M10.500 21.300h3",
    ),
    SHIELD("M12 3l7 2.500v5.500c0 4.500 -3 8 -7 10 -4 -2 -7 -5.500 -7 -10V5.500z", "M9 12l2.200 2.200L15.200 10"),
    CHECK("M5 12.500l4.500 4.500L19 7.500"),
    SKIP("M6 5.500l9 6.500 -9 6.500zM18.500 5.500v13"),
    TARGET("o:12,12,9", "o:12,12,5", "c:12,12,1.7"),
    STOPWATCH("o:12,13.5,7.5", "M12 13.500V9.500M10 2.500h4M12 2.500V6M18.500 6.500l1.500 1.500"),
    LIPS(
        "M2.500 12c2.500 -2.800 4.500 -4.500 6.500 -4.500 1.200 0 2.200 0.800 3 1.300 0.800 -0.500 1.800 -1.300 3 -1.300 2 0 4 1.700 6.500 4.500 -2.500 3.500 -5.500 5.500 -9.500 5.500s-7 -2 -9.500 -5.500z",
        "M2.500 12c3.500 0.900 6 1.200 9.500 1.200s6 -0.300 9.500 -1.200",
    ),
    TONGUE(
        "M3.500 7.500c3 1 5.500 1.300 8.500 1.300s5.500 -0.300 8.500 -1.300c-0.500 2.800 -1.800 4.700 -4 6",
        "M3.500 7.500c0.500 2.800 1.800 4.700 4 6",
        "M7.500 11.500v5a4.500 4.500 0 0 0 9 0v-5",
        "M12 12.500v4",
    ),
    FANGS(
        "M3.500 7.500c2.500 1.500 5.500 2 8.500 2s6 -0.500 8.500 -2",
        "M7 9.200l1.500 5 1.500 -4.800M14 9.400l1.500 4.800 1.500 -5",
        "M5 12.500c1.500 4 4 6 7 6s5.500 -2 7 -6",
    ),
    FEATHER("M20 4C13 4 7 8 7 15v2h2c7 0 11 -6 11 -13z", "M4 20L15 9", "M10.500 13.500h4M13 11h3.500"),
    HAND(
        "M8 13V6a1.500 1.500 0 0 1 3 0v5",
        "M11 11V4.500a1.500 1.500 0 0 1 3 0V11",
        "M14 11V6a1.500 1.500 0 0 1 3 0v8.500c0 3.600 -2.500 6.500 -6 6.500 -2.500 0 -4 -1 -5.300 -3L3.500 14.500c-0.500 -0.900 -0.200 -1.900 0.600 -2.300 0.800 -0.400 1.700 -0.200 2.300 0.500L8 14.500",
    ),
    WIND(
        "M3 8.500h9.500a2.500 2.500 0 1 0 -2.500 -2.500",
        "M3 12.500h14.500a2.500 2.500 0 1 1 -2.500 2.500",
        "M3 16.500h7.500a2 2 0 1 1 -2 2",
    ),
    DROP("M12 3c3.500 4.500 6 7.500 6 11a6 6 0 0 1 -12 0c0 -3.500 2.500 -6.500 6 -11z", "M9.500 14.500a2.500 2.500 0 0 0 2.500 2.500"),
    WAVES(
        "M3 7.500c2 -2 4 -2 6 0s4 2 6 0 4 -2 6 0",
        "M3 12.500c2 -2 4 -2 6 0s4 2 6 0 4 -2 6 0",
        "M3 17.500c2 -2 4 -2 6 0s4 2 6 0 4 -2 6 0",
    ),
    HANGER(
        "M12 8.500V7.500a2.200 2.200 0 1 0 -2.200 -2.200",
        "M12 8.500l8.500 6.500c0.900 0.700 0.400 2 -0.700 2H4.200c-1.100 0 -1.600 -1.300 -0.700 -2z",
    ),
    BLINDFOLD(
        "M2.500 12c2.500 -4 5.500 -6 9.500 -6s7 2 9.500 6c-2.500 4 -5.500 6 -9.500 6s-7 -2 -9.500 -6z",
        "o:12,12,2.8",
        "M4.500 4.500l15 15",
    ),
    CROWN("M4 19h16", "M4.500 15.500L3 7l5 4 4 -6.500 4 6.500 5 -4 -1.500 8.500z"),
    SNOWFLAKE("M12 2.500v19M3.800 7.200l16.400 9.600M3.800 16.800l16.400 -9.600", "M9.500 4l2.500 2 2.500 -2M9.500 20l2.500 -2 2.500 2"),
    SHOWER("M5 11a7 7 0 0 1 14 0z", "M12 4V2.500", "M8 14.500v2M12 14.500v2M16 14.500v2M8 19.500v2M12 19.500v2M16 19.500v2"),
    JOKER(
        "M7 3h10a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2H7a2 2 0 0 1 -2 -2V5a2 2 0 0 1 2 -2z",
        "f:M12 8l1.200 2.600 2.800 0.300 -2.100 1.900 0.600 2.800L12 14.200l-2.500 1.400 0.600 -2.800L8 10.900l2.800 -0.300z",
    ),
    CHEVRON("M9 5l7 7 -7 7"),
    REPLAY("M4 12a8 8 0 1 0 2.500 -5.800", "M4 3.500v5h5"),
    TICKET(
        "M4 7h16a1 1 0 0 1 1 1v2.5a1.5 1.5 0 0 0 0 3V16a1 1 0 0 1 -1 1H4a1 1 0 0 1 -1 -1v-2.5a1.5 1.5 0 0 0 0 -3V8a1 1 0 0 1 1 -1z",
        "M14.5 7.5v2M14.5 11v2M14.5 14.5v2",
    ),
    PEN("M4 20l1 -4.5L16.5 4a2.1 2.1 0 0 1 3 3L8 18.5z", "M14.5 6l3 3"),
    LOCK(
        "M6.5 11h11a1.5 1.5 0 0 1 1.5 1.5v6a1.5 1.5 0 0 1 -1.5 1.5h-11A1.5 1.5 0 0 1 5 18.5v-6A1.5 1.5 0 0 1 6.5 11z",
        "M8 11V8a4 4 0 0 1 8 0v3",
        "c:12,15.5,1.3",
    ),
    TROPHY("M8 4h8v5a4 4 0 0 1 -8 0z", "M8 5.5H5a3 3 0 0 0 3 4M16 5.5h3a3 3 0 0 1 -3 4", "M12 13v4M8.5 20h7M10 17h4"),
    GLASS("M5 4h14l-7 8z", "M12 12v7.5M8.5 20h7", "M7.5 7h9"),
    PLUS("M12 5v14M5 12h14"),
    TRASH("M5 7h14M10 7V4.5h4V7M7 7l1 12.5h8L17 7", "M10.5 10.5v6M13.5 10.5v6"),
    SWAP("M4 8.5h14l-3.5 -3.5M20 15.5H6l3.5 3.5"),
    PAUSE("M8.5 5v14M15.5 5v14"),
    THERMO("M12 3a2 2 0 0 0 -2 2v9.3a4 4 0 1 0 4 0V5a2 2 0 0 0 -2 -2z", "c:12,17.5,1.6", "M12 16V9"),
    BACKSPACE("M9 6h10a1.5 1.5 0 0 1 1.5 1.5v9A1.5 1.5 0 0 1 19 18H9l-5.5 -6z", "M12 9.5l5 5M17 9.5l-5 5"),
    SOUND("M4 9.5h3l4.5 -4v13l-4.5 -4H4z", "M15 9a4 4 0 0 1 0 6M17.5 6.5a7.5 7.5 0 0 1 0 11"),
    FINGERPRINT(
        "M6.5 18c-0.7 -1.7 -1 -3.5 -1 -5.5a6.5 6.5 0 0 1 13 0c0 1.6 -0.1 3 -0.4 4.5",
        "M9.3 20.5c-0.6 -2.2 -0.8 -4.700 -0.8 -8a3.5 3.5 0 0 1 7 0c0 3 -0.2 5.500 -0.900 8",
        "M12 12.5c0 3 -0.2 5.500 -0.700 8",
    ),
    PLAY("M7 4.500l12 7.500 -12 7.500z"),
    ;

    /** Tracciati pronti da disegnare: true se la parte va riempita. */
    val shapes: List<Pair<Path, Boolean>> by lazy {
        parts.map { part ->
            when {
                part.startsWith("c:") || part.startsWith("o:") -> {
                    val (x, y, r) = part.drop(2).split(",").map { it.toFloat() }
                    Path().apply { addOval(Rect(Offset(x, y), r)) } to part.startsWith("c:")
                }
                part.startsWith("f:") -> PathParser().parsePathString(part.drop(2)).toPath() to true
                else -> PathParser().parsePathString(part).toPath() to false
            }
        }
    }
}

private val glyphStroke = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Disegna l'icona in un quadrato di lato [sizePx] con angolo in alto a sinistra in [topLeft]. */
fun DrawScope.drawGlyph(icon: FcIcon, topLeft: Offset, sizePx: Float, color: Color) {
    withTransform({
        translate(topLeft.x, topLeft.y)
        scale(sizePx / 24f, sizePx / 24f, Offset.Zero)
    }) {
        icon.shapes.forEach { (path, filled) ->
            if (filled) drawPath(path, color) else drawPath(path, color, style = glyphStroke)
        }
    }
}

@Composable
fun Glyph(icon: FcIcon, modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = LocalContentColor.current) {
    Canvas(modifier.size(size)) { drawGlyph(icon, Offset.Zero, this.size.width, tint) }
}
