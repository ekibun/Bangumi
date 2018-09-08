package soko.ekibun.bangumi.util

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import java.security.MessageDigest

class SizeTransformation(private val scale: (Int, Int)->Float) : BitmapTransformation() {
    companion object {
        private val ID = "SizeTransformation"
        private val ID_BYTES = ID.toByteArray()
    }
    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val scale = scale(toTransform.width, toTransform.height)
        return if (scale == 1f) {
            toTransform
        } else TransformationUtils.centerCrop(pool, toTransform,
                (toTransform.width * scale).toInt(), (toTransform.height * scale).toInt())
    }

    override fun equals(other: Any?): Boolean {
        return other is SizeTransformation
    }

    override fun hashCode(): Int {
        return ID.hashCode()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }
}