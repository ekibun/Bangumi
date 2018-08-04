package soko.ekibun.bangumi.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.support.annotation.AttrRes
import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.util.TypedValue
import java.text.SimpleDateFormat
import java.util.*

object ResourceUtil{
    fun getTintDrawable(context: Context, @DrawableRes resId: Int, colors: ColorStateList?): Drawable{
        val icon = context.resources.getDrawable(resId, context.theme)
        icon.setTintList(colors)
        return icon
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