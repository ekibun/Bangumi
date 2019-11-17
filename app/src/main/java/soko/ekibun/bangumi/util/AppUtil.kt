package soko.ekibun.bangumi.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.bumptech.glide.load.resource.gif.GifDrawable
import soko.ekibun.bangumi.BuildConfig
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.github.Github
import soko.ekibun.bangumi.ui.web.WebActivity
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * App工具库
 */
object AppUtil {
    /**
     * 分享字符串
     */
    fun shareString(context: Context, str: String){
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, str)
        intent.type = "text/plain"
        context.startActivity(Intent.createChooser(intent, context.resources.getString(R.string.share)))
    }

    /**
     * 分享图片
     */
    fun shareDrawable(context: Context, drawable: Drawable){
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // don't forget to make the directory
            val stream = FileOutputStream("$cachePath/image", false) // overwrites this image every time
            if(drawable is GifDrawable){
                val newGifDrawable = (drawable.constantState!!.newDrawable().mutate()) as GifDrawable
                val byteBuffer = newGifDrawable.buffer
                val bytes = ByteArray(byteBuffer.capacity())
                (byteBuffer.duplicate().clear() as ByteBuffer).get(bytes)
                stream.write(bytes, 0 ,bytes.size)
            }else if(drawable is BitmapDrawable){
                drawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            stream.close()

            val imageFile = File(cachePath, "image")
            val contentUri = FileProvider.getUriForFile(context, "soko.ekibun.bangumi.fileprovider", imageFile)

            if (contentUri != null) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
                shareIntent.setDataAndType(contentUri, "image/*")
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                context.startActivity(Intent.createChooser(shareIntent, "分享图片"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 打开浏览器
     */
    fun openBrowser(context: Context, url: String){
        try{
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }catch(e: Exception){ e.printStackTrace() }
    }

    /**
     * 检测更新
     */
    fun checkUpdate(activity: Activity, checkIgnore: Boolean = true, onLatest: () -> Unit = {}) {
        when (BuildConfig.FLAVOR) {
            "github" -> {
                val sp = PreferenceManager.getDefaultSharedPreferences(activity)
                Github.createInstance().releases().enqueue(ApiHelper.buildCallback({
                    if (activity.isFinishing) return@buildCallback
                    val release = it.firstOrNull() ?: return@buildCallback
                    val checkNew = { tag: String? ->
                        val version = tag?.split("-")
                        val versionName = version?.getOrNull(0) ?: ""
                        val versionCode = version?.getOrNull(1)?.toIntOrNull() ?: 0
                        versionName > BuildConfig.VERSION_NAME || versionCode > BuildConfig.VERSION_CODE
                    }
                    if (checkNew(release.tag_name) && (checkIgnore || sp.getString("ignore_tag", "") != release.tag_name))
                        AlertDialog.Builder(activity)
                                .setTitle(activity.getString(R.string.parse_new_version, release.tag_name))
                                .setMessage(it.filter { checkNew(it.tag_name) }.map { "${it.tag_name}\n${it.body}" }.reduce { acc, s -> "$acc\n$s" })
                                .setPositiveButton(R.string.download) { _, _ ->
                                    WebActivity.launchUrl(activity, release.assets?.firstOrNull()?.browser_download_url, "")
                                }.setNegativeButton(R.string.ignore) { _, _ ->
                                    sp.edit().putString("ignore_tag", release.tag_name).apply()
                                }.show()
                    else {
                        onLatest()
                    }
                }) {})
            }
            "coolapk" -> {
                WebActivity.launchUrl(activity, "https://www.coolapk.com/apk/soko.ekibun.bangumi", "")
            }
        }
    }
}