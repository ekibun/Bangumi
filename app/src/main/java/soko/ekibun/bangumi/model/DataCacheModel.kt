package soko.ekibun.bangumi.model

import android.content.Context
import android.os.Environment
import com.jakewharton.disklrucache.DiskLruCache
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.util.JsonUtil
import java.io.File
import java.util.*

/**
 * 数据离线缓存
 */
open class DataCacheModel(context: Context) {
    val memoryCache = WeakHashMap<String, Any>()
    val diskLruCache: DiskLruCache by lazy {
        val cacheDir = getDiskCacheDir(context, "data")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        DiskLruCache.open(cacheDir, 1, 1, 10 * 1024 * 1024)
    }

    /**
     * 读取缓存
     */
    inline fun <reified T> get(key: String): T? {
        return (memoryCache[key] as? T) ?: diskLruCache.get(key)?.let { snapshot ->
            try {
                JsonUtil.toEntity<T>(String(snapshot.getInputStream(0).readBytes()))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 保存缓存
     */
    inline fun <reified T : Any> set(key: String, data: T) {
        memoryCache[key] = data
        diskLruCache.edit(key)?.let { editor ->
            editor.newOutputStream(0).write(JsonUtil.toJson(data).toByteArray())
            editor.commit()
        }
    }

    companion object {
        /**
         * 获取缓存位置
         */
        fun getDiskCacheDir(context: Context, uniqueName: String): File {
            return File(if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
                context.externalCacheDir!!.path
            } else {
                context.cacheDir.path
            } + File.separator + uniqueName)
        }

        /**
         * 混合数据
         */
        inline fun <reified T : Any> merge(a: T, b: T?) {
            if (b == null) return
            T::class.java.declaredFields.forEach { field ->
                if (field.modifiers != 2) return@forEach
                field.isAccessible = true
                val nval = field.get(b) ?: return@forEach
                when (field.type) {
                    String::class.java -> if ((nval as? String)?.isNotEmpty() == true) field.set(a, nval)
                    List::class.java -> if ((nval as? List<*>)?.isNotEmpty() == true) {
                        val oval = (field.get(a) as? List<*>)?.filterIsInstance<Episode>()
                        if (!oval.isNullOrEmpty()) nval.filterIsInstance<Episode>().forEach mergeEp@{ ep ->
                            ep.merge(oval.firstOrNull { it.id == ep.id } ?: return@mergeEp)
                        }
                        field.set(a, nval)
                    }
                    else -> field.set(a, nval)
                }
            }
        }
    }
}