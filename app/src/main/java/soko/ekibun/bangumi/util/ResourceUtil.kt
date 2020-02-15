package soko.ekibun.bangumi.util

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * 资源工具类
 */
object ResourceUtil{

    /**
     * Converts dp to px
     *
     * @param res Resources
     * @param dp  the value in dp
     * @return int
     */
    fun toPixels(res: Resources, dp: Float): Int {
        return (dp * res.displayMetrics.density).toInt()
    }

    /**
     * Converts sp to px
     *
     * @param res Resources
     * @param sp  the value in sp
     * @return int
     */
    fun toScreenPixels(res: Resources, sp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, res.displayMetrics).toInt()
    }

    /**
     * rtl
     * @param res Resources
     * @return Boolean
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun isRtl(res: Resources): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && res.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
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
}