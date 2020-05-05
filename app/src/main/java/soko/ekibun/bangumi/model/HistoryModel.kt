package soko.ekibun.bangumi.model

import androidx.room.Room
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
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

    @Deprecated("TODO 升级数据用，过几个版本删掉")
    fun migrateIntoSql() {
        val oldData = JsonUtil.toEntity<List<History>>(App.app.sp.getString("history", "") ?: "")
        if (oldData.isNullOrEmpty()) return
        historyDao.insert(oldData.mapNotNull {
            History(
                when (it.type) {
                    "subject" -> JsonUtil.toEntity<Subject>(it.data)?.cacheKey
                    "topic" -> JsonUtil.toEntity<Topic>(it.data)?.cacheKey
                    "say" -> JsonUtil.toEntity<Say>(it.data)?.cacheKey
                    else -> null
                } ?: return@mapNotNull null, it.timestamp, it.type, it.thumb, it.title, it.subTitle, it.data
            )
        }).subscribeOn(Schedulers.io()).subscribe()
        App.app.sp.edit().remove("history").apply()
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
    fun getHistoryList(page: Int): Single<List<History>> {
        return historyDao.getListOffset(PAGE_SIZE, page * PAGE_SIZE).subscribeOn(Schedulers.io())
    }

    /**
     * 添加
     * @param obj History
     */
    fun addHistory(obj: Any) {
        createHistory(obj)?.let { historyDao.insert(it).subscribeOn(Schedulers.io()).subscribe() }
    }

    /**
     * 删除
     * @param data String
     * @return Boolean
     */
    fun removeHistory(data: History): Completable {
        return historyDao.delete(data).subscribeOn(Schedulers.io())
    }

    /**
     * 清除
     */
    fun clearHistory(): Completable {
        return historyDao.deleteAll().subscribeOn(Schedulers.io())
    }
}