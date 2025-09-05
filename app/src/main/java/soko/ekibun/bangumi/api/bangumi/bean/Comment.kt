package soko.ekibun.bangumi.api.bangumi.bean

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil

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
        suspend fun getSubjectComment(
            subject: Subject,
            page: Int
        ): List<Comment> {
            return withContext(Dispatchers.Default) {
                val doc = Jsoup.parse(withContext(Dispatchers.IO) {
                    HttpUtil.fetch(
                        "${Bangumi.SERVER}/subject/${subject.id}/comments?page=$page"
                    ).body?.string() ?: ""
                })
                doc.select("#comment_box .item").mapNotNull {
                    val user = it.selectFirst(".text a.l")
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