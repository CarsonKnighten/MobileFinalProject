package com.example.mobilefinalproject.strength.ui

import android.os.Bundle
import android.widget.LinearLayout
import com.example.mobilefinalproject.strength.domain.ProgressPoint
import com.example.mobilefinalproject.strength.domain.StrengthData
import com.example.mobilefinalproject.strength.domain.exerciseKey
import com.example.mobilefinalproject.strength.domain.personalRecords
import com.example.mobilefinalproject.strength.domain.progressPoints
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ProgressFragment : StrengthFragment() {
    private var selectedId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedId = savedInstanceState?.getString("exercise")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("exercise", selectedId)
    }

    override fun render(data: StrengthData) = with(content) {
        heading("Strength Progress", 24f)
        val options = data.workouts.flatMap { it.exercises }.map { it.name }
            .distinctBy(::exerciseKey).sortedBy(::exerciseKey)
        if (options.isEmpty()) {
            textLine("Complete a workout to see progress for its exercises.")
            return@with
        }
        val exercise = options.firstOrNull { exerciseKey(it) == selectedId } ?: options.first()
        button("Exercise · $exercise") {
            MaterialAlertDialogBuilder(context).setTitle("Choose an Exercise")
                .setItems(options.toTypedArray()) { _, index ->
                    selectedId = exerciseKey(options[index])
                    redraw()
                }.setNegativeButton("Cancel", null).show()
        }
        val points = progressPoints(data.workouts, exercise, ZoneId.systemDefault())
        if (points.isNotEmpty()) {
            card {
                heading("Heaviest Lift by Date")
                val metrics = formRow()
                metrics.metric("Latest", points.last().weight)
                metrics.metric("Best", points.maxOf { it.weight })

                if (points.size == 1) {
                    textLine(describe(points.single()))
                    textLine("Record this exercise on another date to see the graph.")
                } else {
                    textLine("Weight (lb)")
                    val selection = android.widget.TextView(context).apply {
                        textSize = 16f
                        text = describe(points.last())
                    }
                    addView(WeightChart(context, points) { selection.text = describe(it) },
                        LinearLayout.LayoutParams(-1, context.dp(224)))
                    addView(selection)
                }
            }
        }
        heading("Personal Records")
        for (record in personalRecords(data.workouts)) {
            textLine("${record.name} · ${displayWeight(record.weight)} lb")
        }
        if (points.size >= 2) {
            heading("Latest Sessions by Day")
            points.takeLast(5).asReversed().forEach { textLine(describe(it)) }
        }
    }

    private fun LinearLayout.metric(label: String, weight: Double) {
        val metric = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        addView(metric)
        weighted(metric)
        metric.heading("${displayWeight(weight)} lb", 20f)
        metric.textLine(label)
    }

    private fun describe(point: ProgressPoint) =
        "${point.date.format(mediumDate)} · ${displayWeight(point.weight)} lb"

    private companion object {
        val mediumDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
}
