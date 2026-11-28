package com.example.mobilefinalproject.strength.ui

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.mobilefinalproject.R
import com.example.mobilefinalproject.strength.StrengthViewModel
import com.example.mobilefinalproject.strength.domain.StrengthData
import com.example.mobilefinalproject.strength.domain.exerciseKey
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

abstract class StrengthFragment(@LayoutRes layout: Int = R.layout.strength_screen) : Fragment(layout) {
    // Leaving the feature through the app navigation still lets the user resume the draft.
    protected val model: StrengthViewModel by activityViewModels()
    protected val content: LinearLayout get() = requireView().findViewById(R.id.strength_content)
    private val scroll: ScrollView get() = requireView().findViewById(R.id.strength_scroll)
    protected val data get() = model.data.value

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        content.textLine("Loading strength training…")
        model.data.observe(viewLifecycleOwner) { redraw(it) }
        model.error.observe(viewLifecycleOwner) { message ->
            if (message == null) return@observe
            if (data == null) {
                content.removeAllViews()
                content.textLine(message)
                content.button("Retry") { model.refresh() }
            } else {
                message(message)
            }
            model.clearError()
        }
    }

    protected abstract fun render(data: StrengthData)

    protected fun redraw(data: StrengthData? = this.data) {
        if (data == null) return
        val viewport = scroll
        val y = viewport.scrollY
        content.removeAllViews()
        render(data)
        viewport.post { if (viewport.isAttachedToWindow) viewport.scrollTo(0, y) }
    }

    protected fun go(destination: Int, args: Bundle? = null) {
        if (!isResumed) return
        hideKeyboard()
        findNavController().navigate(destination, args)
    }

    protected fun back() {
        if (!isResumed) return
        hideKeyboard()
        findNavController().popBackStack()
    }

    protected fun whenVisible(action: () -> Unit): () -> Unit {
        val owner = viewLifecycleOwner
        return { if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) action() }
    }

    protected fun validate(action: () -> Unit) {
        try {
            action()
        } catch (error: IllegalArgumentException) {
            message(error.message ?: "Check the values you entered.")
        }
    }

    protected fun message(text: String) {
        MaterialAlertDialogBuilder(requireContext()).setMessage(text)
            .setPositiveButton("OK", null).show()
    }

    protected fun confirm(title: String, text: String, actionLabel: String, action: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext()).setTitle(title).setMessage(text)
            .setNegativeButton("Cancel", null).setPositiveButton(actionLabel) { _, _ -> action() }.show()
    }

    protected fun LinearLayout.exerciseEntry(
        current: String, onChange: (String) -> Unit, showFavorites: Boolean = true, onAdd: (String) -> Unit
    ) {
        val name = field("Exercise name", current, limit = 160)
        name.doAfterTextChanged { onChange(it.toString()) }
        button("Add Exercise") {
            validate {
                onAdd(name.text.toString())
                hideKeyboard()
                redraw()
            }
        }
        if (!showFavorites) return
        button("Favorite Exercises") {
            val favorites = data?.favorites.orEmpty()
            val dialog = MaterialAlertDialogBuilder(context).setTitle("Favorite Exercises")
            if (favorites.isEmpty()) {
                dialog.setMessage("Your saved favorites will appear here. Add an exercise, then tap Add to Favorites.")
            } else {
                dialog.setItems(favorites.toTypedArray()) { _, position ->
                    validate {
                        onAdd(favorites[position])
                        hideKeyboard()
                        redraw()
                    }
                }
            }
            dialog.setNegativeButton("Close", null).show()
        }
    }

    protected fun LinearLayout.favoriteButton(name: String, favorites: List<String>) {
        val favorite = favorites.any { exerciseKey(it) == exerciseKey(name) }
        button(if (favorite) "Remove from Favorites" else "Add to Favorites") {
            model.setFavorite(name, !favorite)
        }.contentDescription =
            if (favorite) "Remove $name from favorites" else "Add $name to favorites"
    }

    protected fun hideKeyboard() {
        val input = requireContext().getSystemService(InputMethodManager::class.java)
        input.hideSoftInputFromWindow(requireView().windowToken, 0)
    }
}

internal fun Context.dp(value: Int): Int = (resources.displayMetrics.density * value).roundToInt()

internal fun LinearLayout.heading(value: String, size: Float = 18f): TextView =
    textLine(value).apply {
        textSize = size
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        ViewCompat.setAccessibilityHeading(this, true)
    }

internal fun LinearLayout.textLine(value: String): TextView = TextView(context).apply {
    text = value
    textSize = 16f
    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = context.dp(8) }
    this@textLine.addView(this)
}

internal fun LinearLayout.button(
    title: String, primary: Boolean = false, action: () -> Unit
): MaterialButton = MaterialButton(
    context, null, if (primary) com.google.android.material.R.attr.materialButtonStyle
    else com.google.android.material.R.attr.materialButtonOutlinedStyle
).apply {
    text = title
    isAllCaps = false
    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = context.dp(4) }
    setOnClickListener { action() }
    this@button.addView(this)
}

internal fun LinearLayout.card(build: LinearLayout.() -> Unit) {
    val card = MaterialCardView(context).apply {
        radius = context.dp(8).toFloat()
        cardElevation = context.dp(2).toFloat()
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = context.dp(12) }
    }
    val body = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(context.dp(12), context.dp(12), context.dp(12), context.dp(8))
    }
    card.addView(body, ViewGroup.LayoutParams(-1, -2))
    addView(card)
    body.build()
}

internal fun LinearLayout.field(
    label: String, value: String = "", type: Int = InputType.TYPE_CLASS_TEXT,
    limit: Int = 80, multiline: Boolean = false
): EditText {
    val wrapper = TextInputLayout(context).apply {
        hint = label
        boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = context.dp(8) }
    }
    val edit = TextInputEditText(context).apply {
        setSingleLine(!multiline)
        if (type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_NUMBER) {
            // Request a numeric keyboard and keep pasted input intact for validation.
            setRawInputType(type)
        } else {
            inputType = if (multiline) type or InputType.TYPE_TEXT_FLAG_MULTI_LINE else type
        }
        filters = arrayOf(InputFilter.LengthFilter(limit))
        setText(value)
    }
    wrapper.addView(edit, LinearLayout.LayoutParams(-1, -2))
    addView(wrapper)
    return edit
}

internal fun LinearLayout.formRow(): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = android.view.Gravity.CENTER_VERTICAL
    this@formRow.addView(this, LinearLayout.LayoutParams(-1, -2))
}

internal fun LinearLayout.weighted(child: View, weight: Float = 1f) {
    child.layoutParams = LinearLayout.LayoutParams(0, -2, weight).apply {
        marginEnd = context.dp(6)
    }
}

internal fun LinearLayout.weightedField(
    label: String, value: String, type: Int, limit: Int, weight: Float = 1f
): EditText {
    val field = field(label, value, type, limit)
    weighted(getChildAt(childCount - 1), weight)
    return field
}

internal fun displayDate(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))

internal fun displayWeight(weight: Double): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 3
}.format(weight)

internal fun duration(millis: Long): String {
    val seconds = millis / 1000
    return if (seconds >= 3600) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
    }
}

internal const val RECORD_ID = "id"
internal const val IS_SUMMARY = "summary"

internal fun recordArgs(id: String, summary: Boolean = false): Bundle = Bundle().apply {
    putString(RECORD_ID, id)
    if (summary) putBoolean(IS_SUMMARY, true)
}
