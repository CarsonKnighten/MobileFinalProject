package com.example.mobilefinalproject.strength.domain

import java.math.BigDecimal
import java.util.Locale

internal const val MAX_NAME_LENGTH = 80
internal const val MAX_WEIGHT = 100_000.0
internal const val MAX_PLANNED_SETS = 50

private val whitespace = Regex("[\\s\\p{Z}]+")
private val decimal = Regex("[0-9]+(\\.[0-9]+)?|\\.[0-9]+")

private fun cleanName(name: String) = name.trim().replace(whitespace, " ")

fun exerciseKey(name: String): String = cleanName(name).lowercase(Locale.ROOT)

fun validateExerciseName(name: String): String = validateName(name, "an exercise")

fun validateRoutineName(name: String): String = validateName(name, "a routine")

fun validateWorkoutName(name: String): String =
    if (name.isBlank()) "Strength Workout" else validateName(name, "a workout")

private fun validateName(name: String, label: String): String {
    val clean = cleanName(name)
    require(clean.isNotBlank()) { "Enter $label name." }
    require(clean.length <= MAX_NAME_LENGTH) { "Use $MAX_NAME_LENGTH characters or fewer for the name." }
    require(clean.none { it.isISOControl() }) { "Remove control characters from the name." }
    return clean
}

data class SetValues(val weight: Double, val reps: Int?)

fun parseSet(weightText: String, repsText: String, completed: Boolean): SetValues {
    // Accept a decimal comma from localized keyboards as well as a decimal point.
    val weight = if (weightText.isBlank()) 0.0 else {
        val input = weightText.trim().replace(',', '.')
        require(input.matches(decimal)) { "Enter a non-negative weight in pounds." }
        val value = BigDecimal(input)
        require(value <= BigDecimal.valueOf(MAX_WEIGHT)) { "Weight must be 100,000 lb or less." }
        require(value.stripTrailingZeros().scale() <= 3) { "Use up to three decimal places for weight." }
        value.toDouble()
    }
    val reps = if (repsText.isBlank() && !completed) null else {
        val value = repsText.trim().toIntOrNull()
        require(value != null && value > 0) { "Enter a positive whole number of reps." }
        value
    }
    return SetValues(weight, reps)
}
