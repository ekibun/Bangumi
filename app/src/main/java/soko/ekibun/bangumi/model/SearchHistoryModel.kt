package soko.ekibun.bangumi.model

import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.util.JsonUtil

/**
 * 搜索历史
 */
object SearchHistoryModel {

    /**
     * 添加
     * @param searchKey String
     */
    fun addHistory(searchKey: String) {
        val newList = getHistoryList().toMutableList()
        newList.add(0, searchKey)
        App.app.sp.edit().putString(PREF_SEARCH_HISTORY, JsonUtil.toJson(newList.distinct())).apply()
    }

    /**
     * 删除
     * @param searchKey String
     * @return Boolean
     */
    fun removeHistory(searchKey: String): Boolean {
        val newList = getHistoryList().toMutableList()
        val removed = newList.remove(searchKey)
        App.app.sp.edit().putString(PREF_SEARCH_HISTORY, JsonUtil.toJson(newList)).apply()
        return removed
    }

    /**
     * 清除
     */
    fun clearHistory() {
        App.app.sp.edit().putString(PREF_SEARCH_HISTORY, JsonUtil.toJson(ArrayList<String>())).apply()
    }

    /**
     * 获取列表
     * @return List<String>
     */
    fun getHistoryList(): List<String> {
        return JsonUtil.toEntity<List<String>>(
            App.app.sp.getString(PREF_SEARCH_HISTORY, JsonUtil.toJson(ArrayList<String>()))
                ?: ""
        ) ?: ArrayList()
    }

    const val PREF_SEARCH_HISTORY = "searchHistory"
}