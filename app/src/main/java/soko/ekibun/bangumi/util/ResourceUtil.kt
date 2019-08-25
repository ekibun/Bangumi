package soko.ekibun.bangumi.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

object ResourceUtil{

    fun dip2px(context: Context, dpValue: Float): Int{
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }


    fun getDrawable(context: Context, @DrawableRes resId: Int): Drawable {
        return context.resources.getDrawable(resId, context.theme)
    }

    @ColorInt
    fun resolveColorAttr(context: Context, @AttrRes colorAttr: Int): Int {
        val resolvedAttr = resolveThemeAttr(context, colorAttr)
        val colorRes = if (resolvedAttr.resourceId != 0) resolvedAttr.resourceId else resolvedAttr.data
        return ContextCompat.getColor(context, colorRes)
    }

    fun resolveFloatAttr(context: Context, @AttrRes colorAttr: Int): Float {
        val resolvedAttr = resolveThemeAttr(context, colorAttr)
        return resolvedAttr.float
    }

    private fun resolveThemeAttr(context: Context, @AttrRes attrRes: Int): TypedValue {
        val theme = context.theme
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue
    }

    fun getTimeInterval(timeStamp: Long): String {
        val interval = System.currentTimeMillis()/1000 - timeStamp
        return when(interval){
            in 0..60-> "刚刚"
            in 0..(60 * 60)->"${interval/60}分钟前"
            in 0..(24 * 60 * 60)->"${interval/60/60}小时前"
            else-> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(timeStamp * 1000)
        }
    }
}