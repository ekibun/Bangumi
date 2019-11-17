package soko.ekibun.bangumi.api.bangumi.bean

import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi

/**
 * 超展开列表
 * @property images 头像
 * @property topic 标题
 * @property group 小组
 * @property time 时间
 * @property reply 回复数
 * @property url 链接
 * @property groupUrl 小组链接
 * @constructor
 */
data class Rakuen(
        val images: Images,
        val topic: String,
        val group: String?,
        val time: String,
        val reply: Int = 0,
        val url: String,
        val groupUrl: String?
) {
    companion object {
        /**
         * 超展开
         */
        fun getList(
                type: String
        ): Call<List<Rakuen>> {
            return ApiHelper.buildHttpCall("${Bangumi.SERVER}/rakuen/topiclist" + if (type.isEmpty()) "" else "?type=$type") { rsp ->
                val doc = Jsoup.parse(rsp.body?.string() ?: "")
                doc.select(".item_list").mapNotNull {
                    val title = it.selectFirst(".title")
                    val group = it.selectFirst(".row").selectFirst("a")
                    Rakuen(
                            images = Images(Bangumi.parseImageUrl(it.selectFirst("span.avatarNeue"))),
                            topic = title.text(),
                            group = group?.text(),
                            time = it.selectFirst(".time")?.text()?.replace("...", "") ?: "",
                            reply = it.selectFirst(".grey")?.text()?.trim('(', '+', ')')?.toIntOrNull() ?: 0,
                            url = Bangumi.parseUrl(title.attr("href") ?: ""),
                            groupUrl = Bangumi.parseUrl(group?.attr("href") ?: ""))
                }
            }
        }
    }
}