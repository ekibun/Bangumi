package soko.ekibun.bangumi.api.bangumi.bean

import com.chad.library.adapter.base.entity.SectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil
import java.util.*

/**
 * 时间线
 */
class TimeLine(override val isHeader: Boolean) : SectionEntity {
    var header = ""
    var t: TimeLineItem? = null

    constructor(header: String) : this(true) {
        this.header = header
    }

    constructor(t: TimeLineItem) : this(false) {
        this.t = t
    }

    /**
     * 时间线条目
     * @property user UserInfo
     * @property action String
     * @property time String
     * @property content String?
     * @property contentUrl String?
     * @property collectStar Int
     * @property thumbs List<ThumbItem>
     * @property delUrl String?
     * @property say Say?
     * @constructor
     */
    data class TimeLineItem(
        val user: UserInfo,
        val action: String,
        val time: String,
        val content: String?,
        val contentUrl: String?,
        val collectStar: Int,
        val thumbs: List<ThumbItem>,
        val delUrl: String?,
        val say: Say?
    ) {
        data class ThumbItem(
            val image: String,
            val title: String,
            val url: String
        )
    }

    companion object {
        /**
         * 时间线列表
         * @param type String
         * @param page Int
         * @param usr UserInfo?
         * @param global Boolean
         * @return Call<List<TimeLine>>
         */
        suspend fun getList(
            type: String,
            page: Int,
            usr: UserInfo?,
            global: Boolean
        ): List<TimeLine> {
            return withContext(Dispatchers.IO) {
                val rsp = HttpUtil.fetch(
                    "${Bangumi.SERVER}${if (usr == null) "" else "/user/${usr.username}"}/timeline?type=$type&page=$page&ajax=1",
                    HttpUtil.RequestOption(
                        useCookie = !global
                    )
                ).body?.string() ?: ""
                val doc = Jsoup.parse(rsp)
                val ret = ArrayList<TimeLine>()
                var user = usr ?: UserInfo()
                val cssInfo = if (usr == null) ".info" else ".info_full"
                doc.selectFirst("#timeline")?.children()?.forEach { timeline ->
                    if (timeline.hasClass("Header")) {
                        ret += TimeLine(timeline.text())
                    } else timeline.select(".tml_item")?.forEach { item ->
                        item.selectFirst("a.avatar")?.let {
                            user = UserInfo.parse(
                                item.selectFirst("$cssInfo a.l") ?: it,
                                Bangumi.parseImageUrl(it.selectFirst("span.avatarNeue"))
                            )
                        }
                        val delUrl = item.selectFirst(".tml_del")?.attr("href")
                        ret += TimeLine(TimeLineItem(
                            user = user,
                            action = item.selectFirst(cssInfo)?.childNodes()?.map {
                                if (it is TextNode || (it as? Element)?.tagName() == "a" && it.selectFirst("img") == null)
                                    it.outerHtml()
                                else if ((it as? Element)?.hasClass("status") == true)
                                    (if (usr == null) "<br/>" else "") + it.html()
                                else ""
                            }?.reduce { acc, s -> acc + s } ?: "",
                            time = item.selectFirst(".date")?.text()?.trim('·', ' ', '回', '复') ?: "",
                            content = item.selectFirst(".collectInfo")?.text()
                                ?: item.selectFirst(".info_sub")?.text(),
                            contentUrl = item.selectFirst(".info_sub a")?.attr("href"),
                            collectStar = Regex("""stars([0-9]*)""").find(
                                item.selectFirst(".starlight")?.outerHtml() ?: ""
                            )?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                            thumbs = item.select("$cssInfo img").map {
                                val url = it.parent().attr("href")
                                TimeLineItem.ThumbItem(
                                    image = Bangumi.parseImageUrl(it),
                                    title = item.select("a[href=\"$url\"]")?.text() ?: "",
                                    url = url
                                )
                            },
                            delUrl = delUrl,
                            say = Say.parse(
                                item.selectFirst("a.tml_comment")?.attr("href") ?: "",
                                user = user,
                                message = item.selectFirst("$cssInfo .status")?.html(),
                                time = item.selectFirst(".date")?.ownText()?.trim('·', ' ')?.replace("·", "via") ?: ""
                            )
                        )
                        )
                    }
                }
                ret
            }
        }

        /**
         * 时间线吐槽
         * @param say_input String
         * @return Call<Boolean>
         */
        suspend fun addComment(
            say_input: String
        ): Response {
            return withContext(Dispatchers.IO) {
                HttpUtil.fetch(
                    "${Bangumi.SERVER}/update/user/say?ajax=1",
                    HttpUtil.RequestOption(
                        body = FormBody.Builder()
                            .add("say_input", say_input)
                            .add("formhash", HttpUtil.formhash)
                            .add("submit", "submit").build()
                    )
                )
            }
        }

        /**
         * 删除时间线
         * @param item TimeLine
         * @return Call<Boolean>
         */
        suspend fun removeTimeLine(
            item: TimeLine
        ): Response {
            return withContext(Dispatchers.IO) {
                HttpUtil.fetch(
                    "${item.t?.delUrl}&ajax=1"
                )
            }
        }
    }
}