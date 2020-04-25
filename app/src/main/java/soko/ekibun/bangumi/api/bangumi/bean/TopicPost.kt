package soko.ekibun.bangumi.api.bangumi.bean

import com.chad.library.adapter.base.entity.AbstractExpandableItem
import com.chad.library.adapter.base.entity.MultiItemEntity
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.FormBody
import org.jsoup.nodes.Element
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil

/**
 * 帖子回复
 * @property pst_id String
 * @property pst_mid String
 * @property pst_uid String
 * @property pst_content String
 * @property username String
 * @property nickname String
 * @property sign String
 * @property avatar String
 * @property dateline String
 * @property is_self Boolean
 * @property isSub Boolean
 * @property editable Boolean
 * @property relate String
 * @property model String
 * @property floor Int
 * @property sub_floor Int
 * @property badge String?
 * @constructor
 */
data class TopicPost(
    var pst_id: String = "",
    var pst_mid: String = "",
    var pst_uid: String = "",
    var pst_content: String = "",
    var username: String = "",
    var nickname: String = "",
    var sign: String = "",
    var avatar: String = "",
    var dateline: String = "",
    var is_self: Boolean = false,
    var isSub: Boolean = false,
    var editable: Boolean = false,
    var relate: String = "",
    val model: String = "",
    var floor: Int = 0,
    var sub_floor: Int = 0,
    var badge: String? = null,
    var rawData: String? = null
):  AbstractExpandableItem<TopicPost>(), MultiItemEntity{
    override fun getItemType(): Int {
        return if(isSub) 1 else 0
    }

    override fun getLevel(): Int { return itemType }

    companion object {
        /**
         * 讨论
         * @param it Element
         * @return TopicPost?
         */
        fun parse(it: Element): TopicPost? {
            val user = it.selectFirst(".inner a") ?: return null
            val data = (it.selectFirst(".icons_cmt")?.attr("onclick") ?: "").split(",")
            val relate = data.getOrNull(2)?.toIntOrNull() ?: 0
            val post_id = it.selectFirst(".re_info a")?.attr("href")?.substringAfter("_")?.toIntOrNull() ?: 0
            val badge = it.selectFirst(".badgeState")?.text()
            val floor = Regex("""#(\d+)(-\d+)?""").find(it.selectFirst(".floor-anchor")?.text() ?: "")?.groupValues
            return TopicPost(
                    pst_id = (if (post_id == 0) relate else post_id).toString(),
                    pst_mid = data.getOrNull(1) ?: "",
                    pst_uid = data.getOrNull(5) ?: "",
                    pst_content = if (!badge.isNullOrEmpty()) it.selectFirst(".inner")?.ownText()
                            ?: "" else it.selectFirst(".topic_content")?.html()
                            ?: it.selectFirst(".message")?.html()
                            ?: it.selectFirst(".cmt_sub_content")?.html() ?: "",
                    username = UserInfo.getUserName(user.attr("href")) ?: "",
                    nickname = user.text() ?: "",
                    sign = if (!badge.isNullOrEmpty()) "" else it.selectFirst(".inner .tip_j")?.text() ?: "",
                    avatar = Bangumi.parseImageUrl(it.selectFirst("span.avatarNeue")),
                    dateline = if (!badge.isNullOrEmpty()) it.selectFirst(".inner .tip_j")?.text()
                            ?: "" else it.selectFirst(".re_info")?.text()?.split("/")?.get(0)?.trim()?.substringAfter(" - ")
                            ?: "",
                    is_self = it.selectFirst(".re_info")?.text()?.contains("/") == true,
                    isSub = it.selectFirst(".re_info a")?.text()?.contains("-") ?: false,
                    editable = it.selectFirst(".re_info")?.text()?.contains("/") == true,
                    relate = relate.toString(),
                    model = Regex("'([^']*)'").find(data.getOrNull(0) ?: "")?.groupValues?.get(1) ?: "",
                    floor = floor?.getOrNull(1)?.toIntOrNull() ?: 1,
                    sub_floor = floor?.getOrNull(2)?.trim('-')?.toIntOrNull() ?: 0,
                    badge = badge
            )
        }

        /**
         * 删除帖子回复
         * @param post TopicPost
         * @return Call<Boolean>
         */
        fun remove(
            post: TopicPost
        ): Observable<Boolean> {
            return ApiHelper.createHttpObservable(
                Bangumi.SERVER + when (post.model) {
                    "group" -> "/erase/group/reply/"
                    "prsn" -> "/erase/reply/person/"
                    "crt" -> "/erase/reply/character/"
                    "ep" -> "/erase/reply/ep/"
                    "subject" -> "/erase/subject/reply/"
                    "blog" -> "/erase/reply/blog/"
                    else -> ""
                } + "${post.pst_id}?gh=${HttpUtil.formhash}&ajax=1"
            ).subscribeOn(Schedulers.computation()).map { rsp ->
                rsp.body?.string()?.contains("\"status\":\"ok\"") == true
            }
        }

        /**
         * 编辑帖子回复
         * @param post TopicPost
         * @param content String
         * @return Call<Boolean>
         */
        fun edit(
            post: TopicPost,
            content: String
        ): Observable<Boolean> {
            return ApiHelper.createHttpObservable(
                Bangumi.SERVER + when (post.model) {
                    "group" -> "/group/reply/${post.pst_id}/edit"
                    "prsn" -> "/person/edit_reply/${post.pst_id}"
                    "crt" -> "/character/edit_reply/${post.pst_id}"
                    "ep" -> "/subject/ep/edit_reply/${post.pst_id}"
                    "subject" -> "/subject/reply/${post.pst_id}/edit"
                    "blog" -> "/blog/reply/edit/${post.pst_id}"
                    else -> ""
                }, body = FormBody.Builder()
                    .add("formhash", HttpUtil.formhash)
                    .add("submit", "改好了")
                    .add("content", content).build()
            ).subscribeOn(Schedulers.computation()).map { rsp ->
                rsp.code == 200
            }
        }
    }
}