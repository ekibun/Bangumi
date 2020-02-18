package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.topic.TopicActivity
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.util.TimeUtil
import java.util.*
import kotlin.collections.ArrayList

/**
 * 浏览历史
 * @property sp SharedPreferences
 * @constructor
 */
class HistoryModel(context: Context) {

    data class History(
        var type: String,
        var thumb: String?,
        var title: String?,
        var subTitle: String?,
        var data: String,
        var timestamp: Long
    ) {
        val timeString: String get() = TimeUtil.timeFormat.format(Date(timestamp))
        val dateString: String get() = TimeUtil.dateFormat.format(Date(timestamp))

        fun startActivity(context: Context) {
            when (type) {
                "subject" -> SubjectActivity.startActivity(context, JsonUtil.toEntity(data)!!)
                "topic" -> TopicActivity.startActivity(context, JsonUtil.toEntity(data)!!)
            }
        }
    }

    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    /**
     * 添加
     * @param data History
     */
    fun addHistory(data: History) {
        val newList = getHistoryList().toMutableList()
        newList.removeAll { it.type == data.type && it.data == data.data }
        newList.add(0, data)
        sp.edit().putString(PREF_HISTORY, JsonUtil.toJson(newList.sortedByDescending { it.timestamp })).apply()
    }

    /**
     * 删除
     * @param searchKey String
     * @return Boolean
     */
    fun removeHistory(data: History): Boolean {
        val newList = getHistoryList().toMutableList()
        val removed = newList.removeAll { it.timestamp == data.timestamp }
        sp.edit().putString(PREF_HISTORY, JsonUtil.toJson(newList)).apply()
        return removed
    }

    /**
     * 清除
     */
    fun clearHistory() {
        sp.edit().putString(PREF_HISTORY, "[]").apply()
    }

    /**
     * 获取列表
     * @return List<String>
     */
    fun getHistoryList(): List<History> {
        return JsonUtil.toEntity<List<History>>(sp.getString(PREF_HISTORY, null) ?: "[]") ?: ArrayList()
    }

    companion object {
        const val PREF_HISTORY = "history"
    }
}