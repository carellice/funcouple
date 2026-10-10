package com.funcouple.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Widget della home con i giorni insieme. Un widget non può usare Compose né i font dell'app,
 * quindi la card viene ridisegnata su una bitmap con le stesse misure di [TogetherCard].
 */
class TogetherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context, manager.getAppWidgetOptions(it))) }
        scheduleMidnight(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        manager.updateAppWidget(id, views(context, options))
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(midnightIntent(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> refresh(context)
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.funcouple.app.REFRESH_TOGETHER_WIDGET"

        private fun component(context: Context) = ComponentName(context, TogetherWidget::class.java)

        /** Ridisegna tutti i widget: va chiamata quando cambia la data o inizia un nuovo giorno. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(component(context))
            if (ids.isNotEmpty()) TogetherWidget().onUpdate(context, manager, ids)
            // Da Android 15 l'anteprima nell'elenco dei widget può essere quella vera.
            if (Build.VERSION.SDK_INT >= 35) {
                runCatching {
                    manager.setWidgetPreview(component(context), AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN, views(context, null))
                }
            }
        }

        fun canPin(context: Context) = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

        /** Chiede al launcher di aggiungere il widget alla home. */
        fun requestPin(context: Context) {
            AppWidgetManager.getInstance(context).requestPinAppWidget(component(context), null, null)
        }

        private fun midnightIntent(context: Context) = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, TogetherWidget::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /** Il numero cambia a mezzanotte: una sveglia non esatta basta, senza permessi. */
        private fun scheduleMidnight(context: Context) {
            val midnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            context.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, midnight + 5_000, midnightIntent(context))
        }

        private fun views(context: Context, options: android.os.Bundle?): RemoteViews {
            val density = context.resources.displayMetrics.density
            // In verticale il launcher dà al widget la larghezza minima e l'altezza massima.
            val widthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)?.takeIf { it > 0 } ?: 320
            val heightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)?.takeIf { it > 0 } ?: 180
            val prefs = context.getSharedPreferences("funcouple", Context.MODE_PRIVATE)
            val since = if (prefs.contains("since")) LocalDate.ofEpochDay(prefs.getLong("since", 0)) else null
            val bitmap = Bitmap.createBitmap((widthDp * density).toInt(), (heightDp * density).toInt(), Bitmap.Config.ARGB_8888)
            TogetherDrawing(context).draw(Canvas(bitmap), bitmap.width.toFloat(), bitmap.height.toFloat(), since)
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return RemoteViews(context.packageName, R.layout.widget_together).apply {
                setImageViewBitmap(R.id.widget_image, bitmap)
                setOnClickPendingIntent(R.id.widget_image, open)
            }
        }
    }
}

/** La card dei giorni insieme disegnata a mano: misure e colori sono quelli di [TogetherCard]. */
private class TogetherDrawing(private val context: Context) {
    private val dp = context.resources.displayMetrics.density
    private val sp = dp * context.resources.configuration.fontScale

    /** Testo con il mezzo sp di spaziatura che Material dà a ogni testo dell'app. */
    private fun text(fontId: Int, weight: Int, size: Float, color: Color) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        // I font sono variabili: il peso si sceglie sull'asse "wght".
        typeface = context.resources.getFont(fontId)
        fontVariationSettings = "'wght' $weight"
        textSize = size * sp
        this.color = color.toArgb()
        letterSpacing = 0.5f / size
    }

    /** Disegna una riga centrata in orizzontale dentro una fascia alta [lineHeight] che parte da [top]. */
    private fun Canvas.line(text: String, paint: Paint, centerX: Float, top: Float, lineHeight: Float) {
        val metrics = paint.fontMetrics
        val baseline = top + (lineHeight - (metrics.descent - metrics.ascent)) / 2 - metrics.ascent
        drawText(text, centerX - paint.measureText(text) / 2, baseline, paint)
    }

    fun draw(canvas: Canvas, width: Float, height: Float, since: LocalDate?) {
        val card = RectF(0f, 0f, width, height)
        val radius = 28 * dp
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fun gradient(x1: Float, y1: Float, from: Color, to: Color) =
            LinearGradient(0f, 0f, x1, y1, from.toArgb(), to.toArgb(), Shader.TileMode.CLAMP)

        // Nell'app la card è una lastra trasparente sullo sfondo scuro: qui lo sfondo va disegnato.
        fill.shader = gradient(0f, height, Fc.Bg1, Color(0xFF3A0B2E))
        canvas.drawRoundRect(card, radius, radius, fill)
        fill.shader = gradient(width, height, Fc.Pink.copy(alpha = 0.24f), Fc.DeepViolet.copy(alpha = 0.24f))
        canvas.drawRoundRect(card, radius, radius, fill)
        fill.shader = gradient(0f, height, Color.White.copy(alpha = 0.07f), Color.Transparent)
        canvas.drawRoundRect(card, radius, radius, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp
            shader = gradient(0f, height, Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.06f))
        }
        val inset = dp / 2
        canvas.drawRoundRect(RectF(inset, inset, width - inset, height - inset), radius - inset, radius - inset, border)

        if (since == null) {
            val paint = text(R.font.outfit, 400, 14f, Fc.Muted)
            canvas.line("Da quando state insieme?", paint, width / 2, height / 2 - 12 * sp, 24 * sp)
            return
        }

        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(since, today).toInt()
        val number = thousands(days)
        val numberPaint = text(R.font.playfair, 700, 46f, Fc.Rose).apply { letterSpacing = 0f }
        val labelPaint = text(R.font.outfit, 600, 16f, Fc.Text)
        val detailPaint = text(R.font.outfit, 400, 13f, Fc.Muted)
        val pillPaint = text(R.font.outfit, 500, 13f, Fc.Gold)
        val detail = if (days > 31) "${elapsed(Period.between(since, today))}, dal ${formatDate(since)}" else "Dal ${formatDate(since)}"
        val pillText = milestone(since, today)
        val pillWidth = 12 * dp + 14 * dp + 6 * dp + pillPaint.measureText(pillText) + 12 * dp

        // Se il widget è più piccolo della card dell'app, il contenuto si rimpicciolisce tutto insieme.
        fun detailLayout(available: Float): StaticLayout {
            val metrics = detailPaint.fontMetricsInt
            return StaticLayout.Builder.obtain(detail, 0, detail.length, detailPaint, max(1, available.toInt()))
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .setLineSpacing(17 * sp - (metrics.descent - metrics.ascent), 1f)
                .build()
        }
        fun contentHeight(layout: StaticLayout) = 50 * sp + 24 * sp + 4 * dp + layout.height + 10 * dp + 24 * sp + 12 * dp
        var scale = min(1f, width / (pillWidth + 32 * dp))
        var layout = detailLayout(width / scale - 32 * dp)
        val fit = height / (contentHeight(layout) + 28 * dp)
        if (fit < scale) {
            scale = fit
            layout = detailLayout(width / scale - 32 * dp)
        }
        val w = width / scale
        val h = height / scale
        canvas.scale(scale, scale)

        val center = w / 2
        var y = (h - contentHeight(layout)) / 2
        val numberWidth = numberPaint.measureText(number)
        numberPaint.shader = LinearGradient(
            center - numberWidth / 2, 0f, center + numberWidth / 2, 0f, Fc.Rose.toArgb(), Fc.Gold.toArgb(), Shader.TileMode.CLAMP,
        )
        canvas.line(number, numberPaint, center, y, 50 * sp)
        y += 50 * sp
        canvas.line(if (days == 1) "giorno insieme" else "giorni insieme", labelPaint, center, y, 24 * sp)
        y += 24 * sp + 4 * dp
        canvas.save()
        canvas.translate(16 * dp, y)
        layout.draw(canvas)
        canvas.restore()
        y += layout.height + 10 * dp

        val pillHeight = 24 * sp + 12 * dp
        val pill = RectF(center - pillWidth / 2, y, center + pillWidth / 2, y + pillHeight)
        fill.shader = null
        fill.color = Fc.Gold.copy(alpha = 0.14f).toArgb()
        canvas.drawRoundRect(pill, pillHeight / 2, pillHeight / 2, fill)
        val icon = 14 * dp
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Fc.Gold.toArgb()
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.save()
        canvas.translate(pill.left + 12 * dp, pill.centerY() - icon / 2)
        canvas.scale(icon / 24f, icon / 24f)
        FcIcon.SPARKLE.shapes.forEach { (path, filled) ->
            stroke.style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
            canvas.drawPath(Path(path.asAndroidPath()), stroke)
        }
        canvas.restore()
        val textLeft = pill.left + 12 * dp + icon + 6 * dp
        canvas.line(pillText, pillPaint, textLeft + pillPaint.measureText(pillText) / 2, y + 6 * dp, 24 * sp)
    }
}
