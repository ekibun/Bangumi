package soko.ekibun.bangumi.api.bangumi.bean

import com.chad.library.adapter.base.entity.SectionEntity
import okhttp3.FormBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil
import java.util.*

class TimeLine: SectionEntity<TimeLine.TimeLineItem> {
    constructor(isHeader: Boolean, header: String): super(isHeader, header)
    constructor(t: TimeLineItem): super(t)

    data class TimeLineItem(
            val user: UserInfo,
            val action: String,
            val time: String,
            val content: String?,
            val contentUrl: String?,
            val collectStar: Int,
            val thumbs: List<ThumbItem>,
            val delUrl: String?,
            val sayUrl: String?
    ){
        data class ThumbItem(
                val images: Images,
                val title: String,
                val url: String
        )
    }

    companion object {
        /**
         * 时间线列表
         */
        fun getList(
                type: String,
                page: Int,
                usr: UserInfo?,
                global: Boolean
        ): Call<List<TimeLine>> {
            return ApiHelper.buildHttpCall("${Bangumi.SERVER}${if (usr == null) "" else "/user/${usr.username}"}/timeline?type=$type&page=$page&ajax=1", useCookie = !global) { rsp ->
                val doc = Jsoup.parse(rsp.body?.string() ?: "")
                val ret = ArrayList<TimeLine>()
                var user = usr ?: UserInfo()
                val cssInfo = if (usr == null) ".info" else ".info_full"
                doc.selectFirst("#timeline")?.children()?.forEach { timeline ->
                    if (timeline.hasClass("Header")) {
                        ret += TimeLine(true, timeline.text())
                    } else timeline.select(".tml_item")?.forEach { item ->
                        item.selectFirst("a.avatar")?.let {
                            user = UserInfo.parse(it, Bangumi.parseImageUrl(it.selectFirst("span.avatarNeue")))
                        }
                        ret += TimeLine(TimeLineItem(
                                user = user,
                                action = item.selectFirst(cssInfo)?.childNodes()?.map {
                                    if (it is TextNode || (it as? Element)?.tagName() == "a" && it.selectFirst("img") == null)
                                        it.outerHtml()
                                    else if ((it as? Element)?.hasClass("status") == true)
                                        "<br/>" + it.html()
                                    else ""
                                }?.reduce { acc, s -> acc + s } ?: "",
                                time = item.selectFirst(".date")?.text()?.trim('·', ' ', '回', '复') ?: "",
                                content = item.selectFirst(".collectInfo")?.text()
                                        ?: item.selectFirst(".info_sub")?.text(),
                                contentUrl = item.selectFirst(".info_sub a")?.attr("href"),
                                collectStar = Regex("""stars([0-9]*)""").find(item.selectFirst(".starlight")?.outerHtml()
                                        ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                thumbs = item.select("$cssInfo img").map {
                                    val url = it.parent().attr("href")
                                    TimeLineItem.ThumbItem(
                                            images = Images(Bangumi.parseImageUrl(it)),
                                            title = item.select("a[href=\"$url\"]")?.text() ?: "",
                                            url = url)
                                },
                                delUrl = item.selectFirst(".tml_del")?.attr("href"),
                                sayUrl = item.selectFirst("a.tml_comment")?.attr("href")))
                    }
                }
                ret
            }
        }

        /**
         * 时间线吐槽
         */
        fun addComment(
                say_input: String
        ): Call<Boolean> {
            return ApiHelper.buildHttpCall("${Bangumi.SERVER}/update/user/say?ajax=1", body = FormBody.Builder()
                    .add("say_input", say_input)
                    .add("formhash", HttpUtil.formhash)
                    .add("submit", "submit").build()) {
                it.body?.string()?.contains("\"status\":\"ok\"") == true
            }
        }
    }
}