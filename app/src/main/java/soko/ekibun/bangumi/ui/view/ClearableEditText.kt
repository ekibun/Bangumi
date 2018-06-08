package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v7.widget.AppCompatEditText
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.util.ResourceUtil

class ClearableEditText constructor(context: Context, attrs: AttributeSet) : AppCompatEditText(context, attrs) {
    private val clearDrawable: Drawable by lazy{
        ResourceUtil.getDrawable(context, R.drawable.ic_clear)
    }
    private var onBackPress: (()->Unit) = {}

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && text.isEmpty()) {
            onBackPress()
        }
        return false
    }

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        setClearIconVisible(hasFocus() && text.isNotEmpty())
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        setClearIconVisible(focused && length() > 0)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                val drawable = compoundDrawables[DRAWABLE_RIGHT]
                if (drawable != null && event.x <= width - paddingRight
                        && event.x >= width - paddingRight - drawable.bounds.width()) {
                    setText("")
                }
            }
            MotionEvent.ACTION_DOWN -> performClick()
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun setClearIconVisible(visible: Boolean) {
        setCompoundDrawablesWithIntrinsicBounds(
                compoundDrawables[DRAWABLE_LEFT],
                compoundDrawables[DRAWABLE_TOP],
                if (visible) clearDrawable else null,
                compoundDrawables[DRAWABLE_BOTTOM])
    }

    companion object {
        private const val DRAWABLE_LEFT = 0
        private const val DRAWABLE_TOP = 1
        private const val DRAWABLE_RIGHT = 2
        private const val DRAWABLE_BOTTOM = 3
    }
}