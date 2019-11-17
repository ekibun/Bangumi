package soko.ekibun.bangumi.api.bangumi.bean

import androidx.annotation.StringDef
import androidx.annotation.StringRes
import okhttp3.FormBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.xmlpull.v1.XmlPullParser
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.TextUtil
import java.util.*

/**
 * 条目类
 */
data class Subject(
        val id: Int = 0,
        @SubjectType var type: String = TYPE_ANY,
        var name: String? = null,
        var name_cn: String? = null,
        var summary: String? = null,
        var images: Images? = null,
        var air_date: String? = null,
        var air_weekday: Int = 0,
        var infobox: List<Pair<String, String>>? = null,
        var category: String? = null,

        var rating: UserRating? = null,
        var collection: UserCollection? = null,

        var eps: List<Episode>? = null,
        var eps_count: Int = 0,
        var vol_count: Int = 0,
        var ep_status: Int = 0,
        var vol_status: Int = 0,

        var crt: List<Character>? = null,
        var staff: List<Person>? = null,
        var topic: List<Topic>? = null,
        var blog: List<Blog>? = null,
        //web
        var linked: List<Subject>? = null,
        var recommend: List<Subject>? = null,
        var tags: List<Pair<String, Int>>? = null,
        var collect: Collection? = null
) {
    val url = "${Bangumi.SERVER}/subject/$id"

    /**
     * 显示用的条目名称
     */
    val displayName get() = TextUtil.html2text((if (name_cn.isNullOrEmpty()) name else name_cn) ?: "")

    /**
     * 条目评分
     */
    class UserRating(
            val rank: Int = 0,
            val total: Int = 0,
            val count: IntArray = IntArray(10),
            val score: Float = 0f,
            val friend_score: Float = 0f,
            val friend_count: Int = 0
    )

    /**
     * 条目收藏
     */
    data class UserCollection(
            val wish: Int = 0,
            val collect: Int = 0,
            val doing: Int = 0,
            val on_hold: Int = 0,
            val dropped: Int = 0
    )

    /**
     * 日志
     */
    data class Blog(
            val id: Int = 0,
            val title: String? = null,
            val summary: String? = null,
            val image: String? = null,
            val replies: Int = 0,
            val time: String? = null,
            val user: UserInfo? = null
    ) {
        val url = "${Bangumi.SERVER}/blog/$id"
    }

    /**
     * 评论
     */
    data class Topic(
            val id: Int = 0,
            val title: String = "",
            val time: String? = null,
            val replies: Int = 0,
            val user: UserInfo? = null
    ) {
        val url = "${Bangumi.SERVER}/subject/topic/$id"
    }

    @StringDef(TYPE_ANY, TYPE_BOOK, TYPE_ANIME, TYPE_MUSIC, TYPE_GAME, TYPE_REAL)
    annotation class SubjectType

    companion object {
        /**
         * 条目类型定义
         */
        const val TYPE_ANY = "any"
        const val TYPE_BOOK = "book"
        const val TYPE_ANIME = "anime"
        const val TYPE_MUSIC = "music"
        const val TYPE_GAME = "game"
        const val TYPE_REAL = "real"

        /**
         * 获取条目类型显示字符串
         */
        @StringRes
        fun getTypeRes(@SubjectType type: String): Int {
            return when (type) {
                TYPE_BOOK -> R.string.book
                TYPE_ANIME -> R.string.anime
                TYPE_MUSIC -> R.string.music
                TYPE_GAME -> R.string.game
                TYPE_REAL -> R.string.real
                else -> R.string.subject
            }
        }

        /**
         * 获取条目类型
         */
        @SubjectType
        fun parseType(type: Int?): String {
            return when (type) {
                1 -> TYPE_BOOK
                2 -> TYPE_ANIME
                3 -> TYPE_MUSIC
                4 -> TYPE_GAME
                6 -> TYPE_REAL
                else -> TYPE_ANY
            }
        }

        /**
         * 获取条目信息
         */
        fun getDetail(subject: Subject, onUpdate: (Subject, String) -> Unit = { _, _ -> }): Call<Subject> {
            return ApiHelper.buildHttpCall(subject.url) { rsp ->
                var lastTag = ""
                var tankobon: List<Subject>? = null
                val updateSubject = { str: String, newTag: String ->
                    val doc = Jsoup.parse(str)
                    doc.outputSettings().prettyPrint(false)

                    when (lastTag) {
                        "type" -> subject.type = when (doc.selectFirst("#navMenuNeue .focus").text()) {
                            "动画" -> TYPE_ANIME
                            "书籍" -> TYPE_BOOK
                            "音乐" -> TYPE_MUSIC
                            "游戏" -> TYPE_GAME
                            "三次元" -> TYPE_REAL
                            else -> TYPE_ANY
                        }
                        "name" -> {
                            subject.name = doc.selectFirst(".nameSingle> a")?.text() ?: subject.name
                            subject.name_cn = doc.selectFirst(".nameSingle> a")?.attr("title") ?: subject.name_cn
                            subject.category = doc.selectFirst(".nameSingle small")?.text() ?: subject.category
                        }
                        "summary" -> subject.summary = doc.selectFirst("#subject_summary")?.let { TextUtil.html2text(it.html()) }
                                ?: subject.summary
                        "images" -> subject.images = doc.selectFirst(".infobox img.cover")?.let { Images(Bangumi.parseImageUrl(it)) }
                                ?: subject.images
                        "infobox" -> {
                            val infobox = doc.select("#infobox li")?.map { li ->
                                val tip = li.selectFirst("span.tip")?.text() ?: ""
                                var value = ""
                                li.childNodes()?.forEach { if (it !is Element || !it.hasClass("tip")) value += it.outerHtml() }
                                Pair(tip.trim(':', ' '), value.trim())
                            }
                            subject.infobox = infobox ?: subject.infobox
                            subject.air_date = infobox?.firstOrNull { it.first in arrayOf("放送开始", "上映年度", "开始") }?.second
                                    ?: subject.air_date
                            subject.air_weekday = ("一二三四五六日".map { "星期$it" }.indexOf(infobox?.firstOrNull { it.first == "放送星期" }?.second
                                    ?: "") + 1).let { week ->
                                if (week == 0) "月火水木金土日".map { "${it}曜日" }.indexOf(infobox?.firstOrNull { it.first == "放送星期" }?.second
                                        ?: "") + 1 else week
                            }
                        }
                        "eps" -> subject.eps = Episode.parseProgressList(doc)
                        "tags" -> {
                            subject.tags = doc.select(".subject_tag_section a")?.map {
                                Pair(it.selectFirst("span")?.text()
                                        ?: "", it.selectFirst("small")?.text()?.toIntOrNull()
                                        ?: 0)
                            }
                        }
                        "collection" -> {
                            subject.collection = doc.select("#subjectPanelCollect .tip_i a")?.mapNotNull { Regex("(\\d+)人(.+)").find(it.text())?.groupValues }?.let { list ->
                                UserCollection(
                                        wish = list.firstOrNull { it[2].contains("想") }?.get(1)?.toIntOrNull() ?: 0,
                                        collect = list.firstOrNull { it[2].contains("过") }?.get(1)?.toIntOrNull() ?: 0,
                                        doing = list.firstOrNull { it[2].contains("在") }?.get(1)?.toIntOrNull() ?: 0,
                                        on_hold = list.firstOrNull { it[2] == "搁置" }?.get(1)?.toIntOrNull() ?: 0,
                                        dropped = list.firstOrNull { it[2] == "抛弃" }?.get(1)?.toIntOrNull() ?: 0
                                )
                            }
                        }
                        "collect" -> {
                            subject.rating = UserRating(
                                    rank = doc.selectFirst(".global_score .alarm")?.text()?.trim('#')?.toIntOrNull()
                                            ?: subject.rating?.rank ?: 0,
                                    total = doc.selectFirst("span[property=\"v:votes\"]")?.text()?.toIntOrNull()
                                            ?: subject.rating?.total ?: 0,
                                    count = {
                                        val counts = IntArray(10)
                                        doc.select(".horizontalChart li")?.forEach {
                                            counts[(it.selectFirst(".label")?.text()?.toIntOrNull()
                                                    ?: 0) - 1] = it.selectFirst(".count").text()?.trim('(', ')')?.toIntOrNull()
                                                    ?: 0
                                        }
                                        counts
                                    }(),
                                    score = doc.selectFirst(".global_score .number")?.text()?.toFloatOrNull()
                                            ?: subject.rating?.score ?: 0f,
                                    friend_score = doc.selectFirst(".frdScore .num")?.text()?.toFloatOrNull()
                                            ?: subject.rating?.friend_score ?: 0f,
                                    friend_count = doc.selectFirst(".frdScore a.l")?.text()?.split(" ")?.getOrNull(0)?.toIntOrNull()
                                            ?: subject.rating?.friend_count ?: 0
                            )
                            subject.collect = doc.selectFirst("#collectBoxForm")?.let {
                                Collection(
                                        status = doc.selectFirst(".collectType input[checked=checked]")?.id()
                                                ?: return@let null,
                                        rating = doc.selectFirst(".rating[checked]")?.attr("value")?.toIntOrNull() ?: 0,
                                        comment = doc.selectFirst("#comment")?.text(),
                                        private = doc.selectFirst("#privacy[checked]")?.attr("value")?.toIntOrNull()
                                                ?: 0,
                                        tag = doc.selectFirst("#tags")?.attr("value")?.split(" ")?.filter { it.isNotEmpty() }
                                                ?: ArrayList(),
                                        myTag = doc.select("div.tagList")?.firstOrNull {
                                            it.selectFirst("span.tip_j")?.text()?.contains("我的标签") ?: false
                                        }
                                                ?.select("div.inner a")?.map { it.text() }
                                )
                            }
                            subject.eps_count = doc.selectFirst("input[name=watchedeps]")?.parent()?.ownText()?.trim(' ', '/')?.toIntOrNull()
                                    ?: 0
                            subject.ep_status = doc.selectFirst("input[name=watchedeps]")?.attr("value")?.toIntOrNull()
                                    ?: 0
                            subject.vol_count = doc.selectFirst("input[name=watched_vols]")?.let {
                                it.parent()?.ownText()?.trim(' ', '/')?.toIntOrNull() ?: -1
                            } ?: 0
                            subject.vol_status = doc.selectFirst("input[name=watched_vols]")?.attr("value")?.toIntOrNull()
                                    ?: 0
                        }
                        "sections" -> {
                            val subtitle = doc.selectFirst(".subtitle")?.text()
                            when {
                                subtitle == "角色介绍" -> {
                                    lastTag = "crt"
                                    subject.crt = doc.select("li")?.map {
                                        val a = it.selectFirst("a.avatar")
                                        Character(
                                                id = Regex("""/character/([0-9]*)""").find(a?.attr("href")
                                                        ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                                name = a?.text() ?: "",
                                                name_cn = it.selectFirst(".info .tip")?.text() ?: "",
                                                role_name = it.selectFirst(".info .badge_job_tip")?.text() ?: "",
                                                images = Images(Bangumi.parseImageUrl(a.selectFirst("span.avatarNeue"))),
                                                comment = it.selectFirst("small.fade")?.text()?.trim('(', '+', ')')?.toIntOrNull()
                                                        ?: 0,
                                                actors = it.select("a[rel=\"v:starring\"]").map { psn ->
                                                    Person(
                                                            id = Regex("""/person/([0-9]*)""").find(psn.attr("href")
                                                                    ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                                            name = psn.text() ?: "")
                                                })
                                    }
                                }
                                subtitle == "讨论版" -> {
                                    lastTag = "topic"
                                    subject.topic = doc.select(".topic_list tr")?.mapNotNull {
                                        val tds = it.select("td")
                                        val td0 = tds?.get(0)?.selectFirst("a")
                                        if (td0?.attr("href").isNullOrEmpty()) null else Topic(
                                                id = Regex("""/topic/([0-9]*)""").find(td0?.attr("href")
                                                        ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                                title = td0?.text() ?: "",
                                                time = tds?.get(3)?.text(),
                                                replies = Regex("""([0-9]*)""").find(tds?.get(2)?.text()
                                                        ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                                user = UserInfo.parse(tds?.get(1)?.selectFirst("a"))
                                        )
                                    }
                                }
                                subtitle == "评论" -> {
                                    lastTag = "blog"
                                    subject.blog = doc.select("div.item")?.map {
                                        Blog(
                                                id = Regex("""/blog/([0-9]*)""").find(it.selectFirst(".title a")?.attr("href")
                                                        ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                                title = it.selectFirst(".title a")?.text() ?: "",
                                                summary = it.selectFirst(".content")?.ownText() ?: "",
                                                image = Bangumi.parseImageUrl(it.selectFirst("img")),
                                                replies = it.selectFirst("small.orange")?.text()?.trim('(', '+', ')')?.toIntOrNull()
                                                        ?: 0,
                                                time = it.selectFirst("small.time")?.text(),
                                                user = UserInfo.parse(it.selectFirst(".tip_j a"))
                                        )
                                    }
                                }
                                subtitle == "单行本" -> tankobon = doc.select("li")?.map {
                                    val avatar = it.selectFirst("a.avatar")
                                    val title = avatar?.attr("title")?.split("/ ")
                                    Subject(
                                            id = Regex("""/subject/([0-9]*)""").find(avatar?.attr("href")
                                                    ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                            name = title?.getOrNull(0),
                                            name_cn = title?.getOrNull(1),
                                            category = "单行本",
                                            images = Images(Bangumi.parseImageUrl(avatar.selectFirst("span.avatarNeue")))
                                    )
                                }
                                subtitle == "关联条目" -> {
                                    lastTag = "linked"
                                    var sub = ""
                                    val linked = doc.select("li")?.mapNotNull {
                                        val newSub = it.selectFirst(".sub").text()
                                        if (!newSub.isNullOrEmpty()) sub = newSub
                                        val avatar = it.selectFirst(".avatar")
                                        val title = it.selectFirst(".title")
                                        val id = Regex("""/subject/([0-9]*)""").find(title?.attr("href")
                                                ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0
                                        if (tankobon?.firstOrNull { b -> b.id == id } == null)
                                            Subject(id = id,
                                                    name = title?.text(),
                                                    name_cn = avatar.attr("title"),
                                                    category = sub,
                                                    images = Images(Bangumi.parseImageUrl(avatar.selectFirst("span.avatarNeue")))
                                            )
                                        else null
                                    }?.toMutableList() ?: ArrayList()
                                    linked.addAll(0, tankobon ?: ArrayList())
                                    subject.linked = linked
                                }
                                subtitle?.contains("大概会喜欢") ?: false -> {
                                    lastTag = "recommend"
                                    subject.recommend = doc.select("li")?.map {
                                        val avatar = it.selectFirst(".avatar")
                                        val title = it.selectFirst(".info a")
                                        Subject(
                                                id = Regex("""/subject/([0-9]*)""").find(title?.attr("href")
                                                        ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                                name = title?.text(),
                                                name_cn = avatar.attr("title"),
                                                images = Images(Bangumi.parseImageUrl(avatar.selectFirst("span.avatarNeue")))
                                        )
                                    }
                                }
                            }

                        }
                    }
                    onUpdate(subject, lastTag)
                    lastTag = newTag
                }
                val lastData = ApiHelper.parseWithSax(rsp) { parser, str ->
                    val attr = { name: String -> parser.getAttributeValue("", name) }
                    val hasClass = { cls: String -> attr("class")?.split(" ")?.contains(cls) ?: false }
                    when {
                        parser.eventType != XmlPullParser.START_TAG -> ApiHelper.SaxEventType.NOTHING
                        parser.name == "input" && attr("name") == "formhash" -> {
                            HttpUtil.formhash = attr("value") ?: HttpUtil.formhash
                            ApiHelper.SaxEventType.NOTHING
                        }
                        attr("id") == "navMenuNeue" -> {
                            HttpUtil.formhash = attr("value") ?: HttpUtil.formhash
                            updateSubject(str, "type")
                            ApiHelper.SaxEventType.BEGIN
                        }
                        hasClass("nameSingle") -> {
                            updateSubject(str, "name")
                            ApiHelper.SaxEventType.BEGIN
                        }
                        attr("id") == "subject_summary" -> {
                            updateSubject(str, "summary")
                            ApiHelper.SaxEventType.BEGIN
                        }
                        hasClass("infobox") -> {
                            updateSubject(str, "images")
                            ApiHelper.SaxEventType.BEGIN
                        }
                        attr("id") == "infobox" -> {
                            updateSubject(str, "infobox")
                            ApiHelper.SaxEventType.BEGIN
                        }
                        attr("id") == "subjectPanelCollect" -> {
                            updateSubject(str, "collection")
                            ApiHelper.SaxEventType.BEGIN
                        }
                        attr("id") == "panelInterestWrapper" -> {
                            updateSubject(str, "collect")
                            ApiHelper.SaxEventType.BEGIN
                        }
                        hasClass("line_list_music") || hasClass("prg_list") -> {
                            updateSubject(str, "eps")
                            ApiHelper.SaxEventType.BEGIN
                        }
                        hasClass("subject_section") -> {
                            updateSubject(str, "sections")
                            ApiHelper.SaxEventType.BEGIN
                        }
                        hasClass("subject_tag_section") -> {
                            updateSubject(str, "tags")
                            ApiHelper.SaxEventType.BEGIN
                        }
                        else -> ApiHelper.SaxEventType.NOTHING
                    }
                }
                updateSubject(lastData, "")
                subject
            }
        }

        /**
         * 更新进度
         */
        fun updateProgress(
                id: Int,
                @Episode.EpisodeStatus status: String,
                epIds: String? = null
        ): Call<Boolean> {
            return ApiHelper.buildHttpCall("${Bangumi.SERVER}/subject/ep/$id/status/$status?gh=${HttpUtil.formhash}&ajax=1", body = FormBody.Builder()
                    .add("ep_id", epIds ?: id.toString()).build()) {
                it.body?.string()?.contains("\"status\":\"ok\"") == true
            }
        }

        /**
         * 更新进度(看到)
         */
        fun updateSubjectProgress(
                subject: Subject,
                watchedeps: String,
                watched_vols: String
        ): Call<Boolean> {
            val body = FormBody.Builder()
                    .add("referer", "subject")
                    .add("submit", "更新")
                    .add("watchedeps", watchedeps)
            if (subject.vol_count != 0) body.add("watched_vols", watched_vols)
            return ApiHelper.buildHttpCall("${Bangumi.SERVER}/subject/set/watched/${subject.id}", body = body.build()) {
                it.code == 200
            }
        }
    }
}