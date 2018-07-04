package soko.ekibun.bangumi.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import soko.ekibun.bangumi.R

object AppUtil {
    fun shareString(context: Context, str: String){
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, str)
        intent.type = "text/plain"
        context.startActivity(Intent.createChooser(intent, context.resources.getString(R.string.share)))
    }

    fun openBrowser(context: Context, url: String){
        try{
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }catch(e: Exception){ e.printStackTrace() }
    }
}