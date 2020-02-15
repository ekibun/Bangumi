package soko.ekibun.bangumi.api.bangumi.bean

import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi

/**
 * 吐槽
 * @property user UserInfo?
 * @property time String?
 * @property comment String?
 * @property rate Int
 * @constructor
 */
data class Comment(
    val user: UserInfo? = null,
    val time: String? = null,
    val comment: String? = null,
    val rate: Int = 0
) {
    companion object {
        /**
         * 吐槽箱
         * @param subject Subject
         * @param page Int
         * @return Call<List<Comment>>
         */
        fun getSubjectComment(
            subject: Subject,
            page: Int
        ): Call<List<Comment>> {
            return ApiHelper.buildHttpCall("${Bangumi.SERVER}/subject/${subject.id}/comments?page=$page") { rsp ->
                val doc = Jsoup.parse(rsp.body?.string() ?: "")
                doc.select("#comment_box .item").mapNotNull {
                    val user = it.selectFirst(".text a")
                    val username = UserInfo.getUserName(user?.attr("href"))
                    Comment(
                        user = UserInfo(
                            id = username?.toIntOrNull() ?: 0,
                            username = username,
                            nickname = user?.text(),
                            avatar = Bangumi.parseImageUrl(it.selectFirst(".avatarNeue"))
                        ),
                        time = it.selectFirst(".grey")?.text()?.replace("@", "")?.trim(),
                        comment = it.selectFirst("p")?.text(),
                        rate = Regex("""stars([0-9]*)""").find(
                            it.selectFirst(".text")?.selectFirst(".starlight")?.outerHtml()
                                ?: ""
                        )?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    )
                }
            }
        }
    }
}