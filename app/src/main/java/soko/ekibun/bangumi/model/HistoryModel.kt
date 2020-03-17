package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import soko.ekibun.bangumi.api.bangumi.bean.Say
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.ui.say.SayActivity
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

        fun getCacheKey(): String {
            return when (type) {
                "subject" -> JsonUtil.toEntity<Subject>(data)!!.cacheKey
                "topic" -> JsonUtil.toEntity<Topic>(data)!!.cacheKey
                "say" -> JsonUtil.toEntity<Say>(data)!!.cacheKey
                else -> ""
            }
        }

        fun startActivity(context: Context) {
            when (type) {
                "subject" -> SubjectActivity.startActivity(context, JsonUtil.toEntity(data)!!)
                "topic" -> TopicActivity.startActivity(context, JsonUtil.toEntity(data)!!)
                "say" -> SayActivity.startActivity(context, JsonUtil.toEntity(data)!!)
            }
        }
    }

    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    val historyList by lazy {
        JsonUtil.toEntity<ArrayList<History>>(sp.getString(PREF_HISTORY, null) ?: "[]") ?: ArrayList()
    }

    /**
     * 添加
     * @param data History
     */
    fun addHistory(data: History) {
        val cacheKey = data.getCacheKey()
        historyList.removeAll { it.getCacheKey() == cacheKey }
        historyList.add(0, data)
        historyList.sortByDescending { it.timestamp }
        save()
    }

    private fun save() {
        sp.edit().putString(PREF_HISTORY, JsonUtil.toJson(historyList.subList(0, Math.min(historyList.size, 500))))
            .apply()
    }

    /**
     * 删除
     * @param data String
     * @return Boolean
     */
    fun removeHistory(data: History): Boolean {
        val removed = historyList.removeAll { it.timestamp == data.timestamp }
        save()
        return removed
    }

    /**
     * 清除
     */
    fun clearHistory() {
        sp.edit().putString(PREF_HISTORY, "[]").apply()
    }

    companion object {
        const val PREF_HISTORY = "history"
    }
}