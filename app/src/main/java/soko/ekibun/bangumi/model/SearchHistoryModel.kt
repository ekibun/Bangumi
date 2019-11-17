package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.reflect.TypeToken
import soko.ekibun.bangumi.util.JsonUtil

/**
 * 搜索历史
 */
class SearchHistoryModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }

    /**
     * 添加
     */
    fun addHistory(searchKey: String) {
        val newList = getHistoryList().toMutableList()
        newList.add(0, searchKey)
        sp.edit().putString(PREF_SEARCH_HISTORY, JsonUtil.toJson(newList.distinct())).apply()
    }

    /**
     * 删除
     */
    fun removeHistory(searchKey: String): Boolean {
        val newList = getHistoryList().toMutableList()
        val removed = newList.remove(searchKey)
        sp.edit().putString(PREF_SEARCH_HISTORY, JsonUtil.toJson(newList)).apply()
        return removed
    }

    /**
     * 清除
     */
    fun clearHistory() {
        sp.edit().putString(PREF_SEARCH_HISTORY, JsonUtil.toJson(ArrayList<String>())).apply()
    }

    /**
     * 获取列表
     */
    fun getHistoryList(): List<String> {
        return JsonUtil.toEntity(sp.getString(PREF_SEARCH_HISTORY, JsonUtil.toJson(ArrayList<String>()))!!, object: TypeToken<List<String>>() {}.type)?:ArrayList()
    }

    companion object {
        const val PREF_SEARCH_HISTORY="searchHistory"
    }
}