package soko.ekibun.bangumi.model.history

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import soko.ekibun.bangumi.ui.say.SayActivity
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.topic.TopicActivity
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.util.TimeUtil
import java.util.*

@Entity(tableName = "history")
data class History(
    @PrimaryKey
    val cacheKey: String,
    val timestamp: Long,
    val type: String,
    val thumb: String? = null,
    val title: String? = null,
    val subTitle: String? = null,
    val data: String
) {
    fun startActivity(context: Context) {
        when (type) {
            "subject" -> SubjectActivity.startActivity(context, JsonUtil.toEntity(data)!!)
            "topic" -> TopicActivity.startActivity(context, JsonUtil.toEntity(data)!!)
            "say" -> SayActivity.startActivity(context, JsonUtil.toEntity(data)!!)
        }
    }

    val timeString: String get() = TimeUtil.timeFormat.format(Date(timestamp))
    val dateString: String get() = TimeUtil.dateFormat.format(Date(timestamp))
}