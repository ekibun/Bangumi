package soko.ekibun.bangumi.util

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import soko.ekibun.bangumi.api.bangumi.Bangumi

/**
 * 防止Glide崩溃
 */
object GlideUtil {

    const val TYPE_RESOURCE = 0
    const val TYPE_PLACEHOLDER = 1
    const val TYPE_ERROR = 2

    private val refererHost = mapOf(
        ".sinaimg.cn" to "https://sina.cn"
    )

    private fun wrapReferer(url: String): String {
        val httpUrl = url.toHttpUrlOrNull() ?: return Bangumi.SERVER
        refererHost.forEach {
            if (httpUrl.host.endsWith(it.key)) return it.value
        }
        return Bangumi.SERVER
    }

    /**
     * Glide进度
     * @param url String
     * @param view View
     * @param options RequestOptions
     * @param circularProgressDrawable CircularProgressDrawable
     * @param uri Uri?
     * @param callback Function2<Int, Drawable?, Unit>
     * @return Target<Drawable>?
     */
    fun loadWithProgress(url: String, view: View, options: RequestOptions, circularProgressDrawable: CircularProgressDrawable, uri: Uri? = null, callback: (Int, Drawable?) -> Unit): Target<Drawable>? {
        val request = with(view) ?: return null
        circularProgressDrawable.start()
        ProgressAppGlideModule.expect(url, object : ProgressAppGlideModule.UIonProgressListener {
            override fun onProgress(bytesRead: Long, expectedLength: Long) {
                if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                circularProgressDrawable.setStartEndTrim(0f, bytesRead * 1f / expectedLength)
                circularProgressDrawable.progressRotation = 0.75f
                circularProgressDrawable.invalidateSelf()
            }

            override fun getGranualityPercentage(): Float {
                return 1.0f
            }
        })
        return request.asDrawable().let {
            if (uri != null) {
                it.load(uri)
            } else {
                it.load(
                    if (url.isEmpty()) null else GlideUrl(
                        url,
                        Headers {
                            mapOf("referer" to wrapReferer(url), "user-agent" to HttpUtil.ua)
                                .filterNot { it.value.isEmpty() }
                        })
                )
            }
        }.apply(options).into(object : CustomTarget<Drawable>() {

            override fun onLoadStarted(placeholder: Drawable?) {
                callback(TYPE_PLACEHOLDER, placeholder)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                callback(TYPE_ERROR, errorDrawable)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                ProgressAppGlideModule.forget(url)
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                callback(TYPE_RESOURCE, resource)
            }

            override fun onDestroy() {
                ProgressAppGlideModule.forget(url)
            }
        })
    }

    fun with(context: Context): RequestManager? {
        return try {
            Glide.with(context)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(activity: Activity): RequestManager? {
        return try {
            Glide.with(activity)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(activity: FragmentActivity): RequestManager? {
        return try {
            Glide.with(activity)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(fragment: Fragment): RequestManager? {
        return try {
            Glide.with(fragment)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(view: View): RequestManager? {
        return try {
            Glide.with(view)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}