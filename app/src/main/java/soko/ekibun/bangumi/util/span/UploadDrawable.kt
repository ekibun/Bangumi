package soko.ekibun.bangumi.util.span

import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.sda1.Sda1
import soko.ekibun.bangumi.util.FileRequestBody
import soko.ekibun.bangumi.util.ResourceUtil

class UploadDrawable(
    private val requestBody: RequestBody,
    private val fileName: String,
    uri: Uri,
    wrapWidth: (Float) -> Float,
    sizeCache: HashMap<String, Size>,
    private val onUploaded: (String) -> Unit
) : CollapseUrlDrawable(wrapWidth, sizeCache) {
    init {
        this.uri = uri
    }

    override fun loadImage() {
        if (url != null) {
            super.loadImage()
            return
        }
        val view = container?.get()
        view?.post {
            val textSize = view.textSize
            val circularProgressDrawable = CircularProgressDrawable(view.context)
            circularProgressDrawable.setColorSchemeColors(
                ResourceUtil.resolveColorAttr(
                    view.context,
                    android.R.attr.textColorSecondary
                )
            )
            circularProgressDrawable.strokeWidth = textSize / 8f
            circularProgressDrawable.centerRadius = textSize / 2 - circularProgressDrawable.strokeWidth - 1f
            circularProgressDrawable.progressRotation = 0.75f
            update(circularProgressDrawable)
            circularProgressDrawable.start()
            val errorDrawable = view.context.getDrawable(R.drawable.ic_broken_image)
            val fileRequestBody = FileRequestBody(requestBody) { total, progress ->
                view.post {
                    if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                    circularProgressDrawable.setStartEndTrim(0f, progress * 1f / total)
                    circularProgressDrawable.progressRotation = 0.75f
                    circularProgressDrawable.invalidateSelf()
                }
            }

            scope.launch {
                try {
                    val imgUrl = when (App.app.sp.getString(
                        "image_uploader",
                        "p.sda1.dev"
                    )) {
                        "lain.bgm.tv" -> Bangumi.uploadImage(fileRequestBody, fileName)
                        else -> Sda1.uploadImage(fileRequestBody, fileName)
                    }
                    if (imgUrl.isEmpty()) throw IllegalAccessException("Image Response Empty!")
                    Log.v("rspurl", imgUrl)
                    url = imgUrl
                    onUploaded(imgUrl)
                    loadImage()
                } catch (e: Exception) {
                    error = true
                    e.printStackTrace()
                    errorDrawable?.let { drawable -> update(drawable) }
                }
                if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
            }
        }
    }

    companion object {
        private val scope = MainScope()
    }
}