package soko.ekibun.bangumi.util

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import soko.ekibun.bangumi.R

/**
 * 资源工具类
 */
object ResourceUtil{

    /**
     * Converts dp to px
     *
     * @param dp  the value in dp
     * @return int
     */
    fun toPixels(dp: Float): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    /**
     * Converts sp to px
     *
     * @param sp  the value in sp
     * @return int
     */
    fun toScreenPixels(sp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().displayMetrics).toInt()
    }

    /**
     * rtl
     * @param res Resources
     * @return Boolean
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun isRtl(res: Resources): Boolean {
        return res.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    }

    /**
     * 获取 drawable
     * @param context Context
     * @param resId Int
     * @return Drawable
     */
    fun getDrawable(context: Context, @DrawableRes resId: Int): Drawable {
        return context.resources.getDrawable(resId, context.theme)
    }

    /**
     * 获取颜色
     * @param context Context
     * @param colorAttr Int
     * @return Int
     */
    @ColorInt
    fun resolveColorAttr(context: Context, @AttrRes colorAttr: Int): Int {
        val resolvedAttr = resolveThemeAttr(context, colorAttr)
        val colorRes = if (resolvedAttr.resourceId != 0) resolvedAttr.resourceId else resolvedAttr.data
        return ContextCompat.getColor(context, colorRes)
    }

    private fun resolveThemeAttr(context: Context, @AttrRes attrRes: Int): TypedValue {
        val theme = context.theme
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue
    }

    fun checkMenu(context: Context, menu: Menu?, isChecked: (MenuItem) -> Boolean): Boolean {
        if (menu == null) return false
        var hasCheckedItem = false
        menu.forEach {
            val checked = isChecked(it) || checkMenu(context, it.subMenu, isChecked)
            hasCheckedItem = hasCheckedItem || checked
            it.title = SpannableString(it.title.toString()).also { span ->
                if (checked) span.setSpan(
                    ForegroundColorSpan(resolveColorAttr(context, R.attr.colorAccent)),
                    0, span.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
        }
        return hasCheckedItem
    }
}