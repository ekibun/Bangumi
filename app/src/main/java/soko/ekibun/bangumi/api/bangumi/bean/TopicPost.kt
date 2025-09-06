package soko.ekibun.bangumi.api.bangumi.bean

import com.chad.library.adapter.base.entity.node.BaseExpandNode
import com.chad.library.adapter.base.entity.node.BaseNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Response
import org.jsoup.nodes.Element
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.util.HttpUtil

/**
 * 帖子回复
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
    var relate: String = "",
    val model: String = "",
    var floor: Int = 0,
    var sub_floor: Int = 0,
    var badge: String? = null,
    var likeType: Int = 0,
    var likes: List<Like>? = null
) : BaseExpandNode() {
    init {
        isExpanded = true
    }

    val editable get() = UserModel.current()?.username == username && children.size == 0

    val isSub get() = sub_floor > 0

    val children = ArrayList<TopicPost>()

    override val childNode: MutableList<BaseNode>? get() = children as MutableList<BaseNode>

    data class Like(
        val value: Int,
        val type: Int,
        val main_id: Int,
        var total: Int,
        var users: List<UserInfo>
    ) {
        companion object {
            val emojiWrap: Map<Int, String> = mapOf(
                0 to "/img/smiles/tv/44.gif",
                79 to "/img/smiles/tv/40.gif",
                54 to "/img/smiles/tv/15.gif",
                140 to "/img/smiles/tv/101.gif",
                62 to "/img/smiles/tv/23.gif",
                122 to "/img/smiles/tv/83.gif",
                104 to "/img/smiles/tv/65.gif",
                80 to "/img/smiles/tv/41.gif",
                141 to "/img/smiles/tv/102.gif",
                88 to "/img/smiles/tv/49.gif",
                85 to "/img/smiles/tv/46.gif",
                90 to "/img/smiles/tv/51.gif"
            )

            /**
             * 贴贴
             * @return Call<Boolean>
             */
            suspend fun dolike(
                type: Int, topicId: Int, id: String, value: String
            ): Response {
                return withContext(Dispatchers.IO) {
                    HttpUtil.fetch(
                        "${Bangumi.SERVER}/like?type=${type}&main_id=${topicId}&id=${id}&value=${value}&gh=${HttpUtil.formhash}&ajax=1"
                    )
                }
            }
        }
        val image get() = emojiWrap[value] ?: ""
    }

    companion object {
        /**
         * 讨论
         * @param it Element
         * @return TopicPost?
         */
        fun parse(it: Element): TopicPost? {
            val user = it.selectFirst(".inner a") ?: return null
            // <a href="javascript:void(0);" onclick="subReply('group', 380646, 2309023, 0, 428864, 650688, 0)" class="icon" title="回复">
            val data = (it.selectFirst(".icon")?.attr("onclick") ?: "").split(",")
            val relate = data.getOrNull(2)?.trim()?.toIntOrNull() ?: 0
            val post_id = it.attr("id")?.substringAfter("_")?.toIntOrNull() ?: 0
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
                            ?: "" else it.selectFirst(".re_info")?.text()?.split(".")?.get(0)?.split("回")?.get(0)?.trim()?.substringAfter(" - ")
                            ?: "",
                    relate = relate.toString(),
                    model = Regex("'([^']*)'").find(data.getOrNull(0) ?: "")?.groupValues?.get(1) ?: "",
                    floor = floor?.getOrNull(1)?.toIntOrNull() ?: 1,
                    sub_floor = floor?.getOrNull(2)?.trim('-')?.toIntOrNull() ?: 0,
                    badge = badge,
                    likeType = it.selectFirst("a.like_dropdown").attr("data-like-type")?.toIntOrNull()?: 0
            )
        }

        /**
         * 删除帖子回复
         * @param post TopicPost
         * @return Call<Boolean>
         */
        suspend fun remove(
            post: TopicPost
        ): Response {
            return withContext(Dispatchers.IO) {
                HttpUtil.fetch(
                    Bangumi.SERVER + when (post.model) {
                        "group" -> "/erase/group/reply/"
                        "prsn" -> "/erase/reply/person/"
                        "crt" -> "/erase/reply/character/"
                        "ep" -> "/erase/reply/ep/"
                        "subject" -> "/erase/subject/reply/"
                        "blog" -> "/erase/reply/blog/"
                        else -> ""
                    } + "${post.pst_id}?gh=${HttpUtil.formhash}&ajax=1"
                )
            }
        }

        /**
         * 编辑帖子回复
         * @param post TopicPost
         * @param content String
         * @return Call<Boolean>
         */
        suspend fun edit(
            post: TopicPost,
            content: String
        ): Response {
            return withContext(Dispatchers.IO) {
                HttpUtil.fetch(
                    Bangumi.SERVER + when (post.model) {
                        "group" -> "/group/reply/${post.pst_id}/edit"
                        "prsn" -> "/person/edit_reply/${post.pst_id}"
                        "crt" -> "/character/edit_reply/${post.pst_id}"
                        "ep" -> "/subject/ep/edit_reply/${post.pst_id}"
                        "subject" -> "/subject/reply/${post.pst_id}/edit"
                        "blog" -> "/blog/reply/edit/${post.pst_id}"
                        else -> ""
                    }, HttpUtil.RequestOption(
                        body = FormBody.Builder()
                            .add("formhash", HttpUtil.formhash)
                            .add("submit", "改好了")
                            .add("content", content).build()
                    )
                )
            }
        }
    }
}