package soko.ekibun.bangumi.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken

/**
 * Json工具类
 */
object JsonUtil {
    val GSON = Gson()
    private val JSON_PARSER = JsonParser()

    /**
     * 转换为JSON
     * @param src Any
     * @return String
     */
    fun toJson(src: Any): String {
        return GSON.toJson(src)
    }

    /**
     * 转换为实例T
     * @param json String
     * @return T?
     */
    inline fun <reified T> toEntity(json: String): T? {
        return try {
            GSON.fromJson(json, object : TypeToken<T>() {}.type)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 转为JsonObject
     * @param json String
     * @return JsonObject
     */
    fun toJsonObject(json: String): JsonObject {
        return JSON_PARSER.parse(json).asJsonObject
    }
}