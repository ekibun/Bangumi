package soko.ekibun.bangumi.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
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

    fun getVersionName(context: Context): String {
        return getPackageInfo(context)?.versionName?:""
    }

    fun getVersionCode(context: Context): Int {
        return getPackageInfo(context)?.versionCode?:0
    }

    private fun getPackageInfo(context: Context): PackageInfo? {
        return context.packageManager.getPackageInfo(context.packageName,
                PackageManager.GET_CONFIGURATIONS)
    }
}