package soko.ekibun.bangumi.model

import androidx.room.Room
import org.jsoup.Jsoup
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.api.bangumi.bean.Say
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.model.history.History
import soko.ekibun.bangumi.model.history.HistoryDatabase
import soko.ekibun.bangumi.util.JsonUtil
import java.util.*

/**
 * 浏览历史
 */
object HistoryModel {
    private val historyDao by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Room.databaseBuilder(App.app, HistoryDatabase::class.java, "history.sqlite").build().historyDao()
    }

    private fun createHistory(obj: Any): History? {
        val timestamp = Calendar.getInstance().timeInMillis
        return when (obj) {
            is Say -> History(
                obj.cacheKey,
                timestamp,
                "say",
                obj.user.avatar,
                Jsoup.parse(obj.message ?: "").text(),
                obj.user.nickname,
                JsonUtil.toJson(
                    Say(
                        id = obj.id,
                        user = obj.user,
                        message = obj.message,
                        time = obj.time
                    )
                )
            )
            is Subject -> History(
                obj.cacheKey,
                timestamp,
                "subject",
                obj.image,
                obj.displayName,
                obj.name_cn,
                JsonUtil.toJson(Subject(obj.id))
            )
            is Topic -> History(
                obj.cacheKey,
                timestamp,
                "topic",
                obj.image,
                obj.title,
                obj.links?.keys?.firstOrNull(),
                JsonUtil.toJson(Topic(obj.model, obj.id))
            )
            else -> null
        }
    }

    private const val PAGE_SIZE = 50
    suspend fun getHistoryList(page: Int): List<History> {
        return historyDao.getListOffset(PAGE_SIZE, page * PAGE_SIZE)
    }

    /**
     * 添加
     * @param obj History
     */
    suspend fun addHistory(obj: Any) {
        createHistory(obj)?.let { historyDao.insert(it) }
    }

    /**
     * 删除
     * @param data String
     * @return Boolean
     */
    suspend fun removeHistory(data: History) {
        historyDao.delete(data)
    }

    /**
     * 清除
     */
    suspend fun clearHistory() {
        historyDao.deleteAll()
    }
}