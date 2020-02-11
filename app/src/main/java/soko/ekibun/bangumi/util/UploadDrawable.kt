package soko.ekibun.bangumi.util

import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import android.widget.TextView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import okhttp3.RequestBody
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.sda1.Sda1
import soko.ekibun.bangumi.api.uploadcc.UploadCC
import java.lang.ref.WeakReference

/**
 * 上传图片 drawable
 */
class UploadDrawable(
        private val requestBody: RequestBody,
        private val fileName: String,
        container: WeakReference<TextView>,
        uri: Uri,
        private val onUploaded: (String) -> Unit
) : CollapseUrlDrawable(container) {

    init {
        this.uri = uri
    }

    var uploadCall: Call<String>? = null
    override fun loadImage() {
        if (url != null) {
            super.loadImage()
            return
        }
        val view = container.get()
        view?.post {
            val textSize = view.textSize
            val circularProgressDrawable = CircularProgressDrawable(view.context)
            circularProgressDrawable.setColorSchemeColors(
                ResourceUtil.resolveColorAttr(
                    view.context,
                    android.R.attr.textColorSecondary
                )
            )
            circularProgressDrawable.strokeWidth = 5f
            circularProgressDrawable.centerRadius = textSize / 2 - circularProgressDrawable.strokeWidth - 1f
            circularProgressDrawable.progressRotation = 0.75f
            update(circularProgressDrawable, textSize.toInt())
            circularProgressDrawable.start()
            val errorDrawable = view.context.getDrawable(R.drawable.ic_broken_image)
            val callback = object : RetrofitCallback<String>() {
                override fun onSuccess(call: Call<String>?, response: retrofit2.Response<String>) {
                    if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                    val imgUrl = response.body() ?: ""
                    Log.v("rspurl", imgUrl)
                    if (imgUrl.isNotEmpty()) {
                        url = imgUrl
                        onUploaded(imgUrl)
                        loadImage()
                    } else {
                        error = true
                        errorDrawable?.let { update(it, textSize.toInt()) }
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                    error = true
                    errorDrawable?.let { update(it, textSize.toInt()) }
                }

                override fun onLoading(total: Long, progress: Long) {
                    super.onLoading(total, progress)
                    view.post {
                        if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                        circularProgressDrawable.setStartEndTrim(0f, progress * 1f / total)
                        circularProgressDrawable.progressRotation = 0.75f
                        circularProgressDrawable.invalidateSelf()
                    }
                }
            }
            val fileRequestBody = FileRequestBody(requestBody, callback)
            uploadCall?.cancel()
            uploadCall = when (PreferenceManager.getDefaultSharedPreferences(view.context).getString(
                "image_uploader",
                "p.sda1.dev"
            )) {
                "upload.cc" -> UploadCC.uploadImage(fileRequestBody, fileName)
                else -> Sda1.uploadImage(fileRequestBody, fileName)
            }
            uploadCall?.enqueue(callback)
        }
    }
}