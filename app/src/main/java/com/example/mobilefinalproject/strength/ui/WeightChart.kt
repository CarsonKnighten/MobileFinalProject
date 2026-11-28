package com.example.mobilefinalproject.strength.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import com.example.mobilefinalproject.strength.domain.ProgressPoint
import com.google.android.material.color.MaterialColors
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** Draws daily bests at their actual spacing on the calendar. */
internal class WeightChart(
    context: Context,
    private val points: List<ProgressPoint>,
    private val onSelection: (ProgressPoint) -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val foreground = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)
    private val background = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface)
    private val accent = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
    private val labelColor = ColorUtils.setAlphaComponent(foreground, 190)
    private val gridColor = ColorUtils.setAlphaComponent(foreground, 38)
    private val areaColor = ColorUtils.setAlphaComponent(accent, 30)
    private val labelSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    private val ticks = weightTicks(points.minOf { it.weight }, points.maxOf { it.weight })
    private val tickLabels = ticks.map(::displayWeight)
    private val plot = RectF()
    private val line = Path()
    private val area = Path()
    private var positions = emptyList<PointF>()
    private var selected = points.lastIndex

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isClickable = true
        contentDescription = "Line chart of highest added weight per day. " + points.joinToString(". ") {
            "${it.date.format(dateFormat)}, ${displayWeight(it.weight)} pounds"
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        paint.textSize = labelSize
        plot.set(tickLabels.maxOf { paint.measureText(it) } + context.dp(12),
            context.dp(10).toFloat(), width - context.dp(6).toFloat(),
            height - labelSize * 2 - context.dp(10))
        line.reset()
        area.reset()
        if (plot.width() <= 0 || plot.height() <= 0) {
            positions = emptyList()
            return
        }

        val firstDay = points.first().date.toEpochDay()
        val totalDays = points.last().date.toEpochDay() - firstDay
        positions = points.map { point ->
            val day = point.date.toEpochDay() - firstDay
            PointF(plot.left + plot.width() * day / totalDays, weightY(point.weight))
        }
        line.moveTo(positions.first().x, positions.first().y)
        positions.drop(1).forEach { line.lineTo(it.x, it.y) }
        area.set(line)
        area.lineTo(plot.right, plot.bottom)
        area.lineTo(plot.left, plot.bottom)
        area.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (positions.isEmpty()) return

        paint.style = Paint.Style.FILL
        paint.textSize = labelSize
        paint.color = areaColor
        canvas.drawPath(area, paint)

        paint.strokeWidth = context.dp(1).toFloat()
        ticks.forEachIndexed { index, tick ->
            val y = weightY(tick)
            paint.color = gridColor
            canvas.drawLine(plot.left, y, plot.right, y, paint)
            paint.color = labelColor
            val label = tickLabels[index]
            canvas.drawText(label, plot.left - context.dp(8) - paint.measureText(label),
                y - (paint.ascent() + paint.descent()) / 2, paint)
        }
        paint.color = ColorUtils.setAlphaComponent(foreground, 90)
        canvas.drawLine(plot.left, plot.top, plot.left, plot.bottom, paint)
        canvas.drawLine(plot.left, plot.bottom, plot.right, plot.bottom, paint)
        drawDates(canvas)

        val marker = positions[selected]
        paint.color = ColorUtils.setAlphaComponent(accent, 85)
        canvas.drawLine(marker.x, plot.top, marker.x, plot.bottom, paint)
        paint.color = accent
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = context.dp(3).toFloat()
        paint.strokeJoin = Paint.Join.ROUND
        canvas.drawPath(line, paint)
        paint.style = Paint.Style.FILL
        positions.forEach { canvas.drawCircle(it.x, it.y, context.dp(3).toFloat(), paint) }
        paint.color = background
        canvas.drawCircle(marker.x, marker.y, context.dp(6).toFloat(), paint)
        paint.color = accent
        canvas.drawCircle(marker.x, marker.y, context.dp(4).toFloat(), paint)
    }

    private fun drawDates(canvas: Canvas) {
        val first = points.first().date.format(dateFormat)
        val last = points.last().date.format(dateFormat)
        val lastX = width - paint.measureText(last)
        val baseline = plot.bottom + labelSize * 1.55f
        val gap = context.dp(8)
        if (lastX < paint.measureText(first) + gap) return
        paint.color = labelColor
        canvas.drawText(first, 0f, baseline, paint)
        canvas.drawText(last, lastX, baseline, paint)
        if (points.size > 2) {
            val index = (1 until points.lastIndex).minBy { abs(positions[it].x - plot.centerX()) }
            val label = points[index].date.format(dateFormat)
            val labelWidth = paint.measureText(label)
            val x = positions[index].x - labelWidth / 2
            if (x >= paint.measureText(first) + gap && x + labelWidth + gap <= lastX) {
                canvas.drawText(label, x, baseline, paint)
            }
        }
    }

    private fun weightY(weight: Double): Float =
        plot.bottom - plot.height() * ((weight - ticks.first()) / (ticks.last() - ticks.first())).toFloat()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (positions.isEmpty()) return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) return plot.contains(event.x, event.y)
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            selected = positions.indices.minBy { abs(positions[it].x - event.x) }
            onSelection(points[selected])
            invalidate()
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

private fun weightTicks(minimum: Double, maximum: Double): List<Double> {
    val padding = maxOf((maximum - minimum) * 0.15, maximum * 0.03, 0.5)
    val low = (minimum - padding).coerceAtLeast(0.0)
    val high = maximum + padding
    val rawStep = (high - low) / 4
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * magnitude >= rawStep } * magnitude
    val first = floor(low / step) * step
    val intervals = ((ceil(high / step) * step - first) / step).roundToInt()
    return (0..intervals).map { first + it * step }
}
