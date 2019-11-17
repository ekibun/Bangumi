package soko.ekibun.bangumi.util

import android.net.Uri
import android.widget.TextView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.uploadcc.UploadCC
import soko.ekibun.bangumi.api.uploadcc.bean.Response
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

    var uploadCall: Call<Response>? = null
    override fun loadImage() {
        if (url != null) {
            super.loadImage()
            return
        }
        val view = container.get()
        view?.post {
            val textSize = view.textSize
            val circularProgressDrawable = CircularProgressDrawable(view.context)
            circularProgressDrawable.setColorSchemeColors(ResourceUtil.resolveColorAttr(view.context, android.R.attr.textColorSecondary))
            circularProgressDrawable.strokeWidth = 5f
            circularProgressDrawable.centerRadius = textSize / 2 - circularProgressDrawable.strokeWidth - 1f
            circularProgressDrawable.progressRotation = 0.75f
            update(circularProgressDrawable, textSize.toInt())
            circularProgressDrawable.start()
            val errorDrawable = view.context.getDrawable(R.drawable.ic_broken_image)
            val callback = object : RetrofitCallback<Response>() {
                override fun onSuccess(call: Call<Response>?, response: retrofit2.Response<Response>) {
                    if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                    val imgUrl = response.body()?.success_image?.firstOrNull()?.url
                    if (imgUrl != null) {
                        url = "https://upload.cc/$imgUrl"
                        onUploaded(url!!)
                        loadImage()
                    } else {
                        error = true
                        errorDrawable?.let { update(it, textSize.toInt()) }
                    }
                }

                override fun onFailure(call: Call<Response>, t: Throwable) {
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
            val body = MultipartBody.Part.createFormData("uploaded_file[]", fileName, fileRequestBody)
            uploadCall?.cancel()
            uploadCall = UploadCC.createInstance().upload(body)
            uploadCall?.enqueue(callback)
        }
    }
}