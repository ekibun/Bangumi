package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import kotlinx.android.synthetic.main.number_picker.view.*
import soko.ekibun.bangumi.R

class NumberPicker @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.number_picker, this, true)

        number_display.doAfterTextChanged {
            onValueChangeListener(value)
        }

        increment.setOnClickListener {
            value += 1
        }

        decrement.setOnClickListener {
            value -= 1
        }
    }

    private var onValueChangeListener: (Int) -> Unit = { }
    fun setValueChangedListener(listener: (Int) -> Unit) {
        onValueChangeListener = listener
    }

    var value
        get() = number_display.text.toString().toIntOrNull() ?: 0
        set(value) {
            number_display.setText(value.toString())
        }
}