package soko.ekibun.bangumi.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object JsonUtil{
    private val GSON = Gson()
    private val JSON_PARSER = JsonParser()

    fun toJson(src: Any): String {
        return GSON.toJson(src)
    }

    fun <T> toEntity(json: String, classOfT: Class<T>): T? {
        return try{
            GSON.fromJson(json, classOfT)
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }

    fun <T> toEntity(json: String, type: Type): T? {
        return try{ GSON.fromJson(json, type)
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }

    fun toJsonObject(json: String): JsonObject {
        return JSON_PARSER.parse(json).asJsonObject
    }
}