package soko.ekibun.bangumi.util

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.Snackbar

object CustomTabsUtil {
    fun launchUrl(context: Context, url : String?){
        try{
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
        }catch (e: Exception){
            (context as? Activity)?.let{
                Snackbar.make(it.window.decorView, "出错啦\n" + e.toString(), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}