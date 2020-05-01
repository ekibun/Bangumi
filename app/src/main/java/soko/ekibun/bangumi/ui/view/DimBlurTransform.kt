package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import jp.wasabeef.glide.transformations.BitmapTransformation
import jp.wasabeef.glide.transformations.internal.FastBlur
import java.security.MessageDigest

class DimBlurTransform(private val radius: Int, private val sampling: Int, private val dim: Int) :
    BitmapTransformation() {
    companion object {
        private val ID = DimBlurTransform::class.java.name
    }

    override fun equals(other: Any?): Boolean {
        return other is DimBlurTransform && other.radius == radius && other.sampling == sampling
    }

    override fun hashCode(): Int {
        return ID.hashCode() + dim + radius * 1000 + sampling * 10
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((ID + radius + sampling + dim).toByteArray(Key.CHARSET))
    }

    override fun transform(
        context: Context, pool: BitmapPool,
        toTransform: Bitmap, outWidth: Int, outHeight: Int
    ): Bitmap? {
        val width = toTransform.width
        val height = toTransform.height
        val scaledWidth = width / sampling
        val scaledHeight = height / sampling
        var bitmap: Bitmap = pool[scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888]
        val canvas = Canvas(bitmap)
        canvas.scale(1 / sampling.toFloat(), 1 / sampling.toFloat())
        val paint = Paint()
        paint.flags = Paint.FILTER_BITMAP_FLAG
        canvas.drawBitmap(toTransform, 0f, 0f, paint)
        paint.alpha = dim
        canvas.drawRect(Rect(0, 0, width, height), paint)
        bitmap = FastBlur.blur(bitmap, radius, true)
        return bitmap
    }
}