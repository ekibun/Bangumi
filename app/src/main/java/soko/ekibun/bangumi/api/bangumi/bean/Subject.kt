package soko.ekibun.bangumi.api.bangumi.bean

import androidx.annotation.StringDef
import androidx.annotation.StringRes
import io.reactivex.rxjava3.core.Observable
import okhttp3.FormBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.xmlpull.v1.XmlPullParser
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.github.bean.OnAirInfo
import soko.ekibun.bangumi.util.HtmlUtil
import soko.ekibun.bangumi.util.HttpUtil
import java.util.*

/**
 * 条目类
 * @property id Int
 * @property type String
 * @property name String?
 * @property name_cn String?
 * @property summary String?
 * @property image String?
 * @property air_date String?
 * @property air_weekday Int
 * @property infobox List<Pair<String, String>>?
 * @property category String?
 * @property rating UserRating?
 * @property collection UserCollection?
 * @property eps List<Episode>?
 * @property eps_count Int
 * @property vol_count Int
 * @property ep_status Int
 * @property vol_status Int
 * @property crt List<Character>?
 * @property staff List<Person>?
 * @property topic List<Topic>?
 * @property blog List<Topic>?
 * @property linked List<Subject>?
 * @property recommend List<Subject>?
 * @property tags List<Pair<String, Int>>?
 * @property collect Collection?
 * @property season List<Subject>?
 * @property onair OnAirInfo?
 * @property cacheKey String
 * @property url String
 * @property displayName String
 * @constructor
 */
data class Subject(
    val id: Int = 0,
    @SubjectType var type: String = TYPE_ANY,
    var name: String? = null,
    var name_cn: String? = null,
    var summary: String? = null,
    var image: String? = null,
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
    var blog: List<Topic>? = null,
    //web
    var linked: List<Subject>? = null,
    var recommend: List<Subject>? = null,
    var tags: List<Pair<String, Int>>? = null,
    var collect: Collection? = null,
    // other
    var season: List<Subject>? = null,
    var airInfo: String? = null, // 首页放送日期
    var onair: OnAirInfo? = null
) {
    val cacheKey get() = "subject_$id"

    val url get() = "${Bangumi.SERVER}/subject/$id"
    val displayName get() = HtmlUtil.html2text((if (name_cn.isNullOrEmpty()) name else name_cn) ?: "")

    /**
     * 条目评分
     * @property rank Int
     * @property total Int
     * @property count IntArray
     * @property score Float
     * @property friend_score Float
     * @property friend_count Int
     * @constructor
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
     * @property wish Int
     * @property collect Int
     * @property doing Int
     * @property on_hold Int
     * @property dropped Int
     * @constructor
     */
    data class UserCollection(
        val wish: Int = 0,
        val collect: Int = 0,
        val doing: Int = 0,
        val on_hold: Int = 0,
        val dropped: Int = 0
    )

    /**
     * 条目类型
     */
    @StringDef(TYPE_ANY, TYPE_BOOK, TYPE_ANIME, TYPE_MUSIC, TYPE_GAME, TYPE_REAL)
    annotation class SubjectType

    /**
     * Sax tag
     */
    enum class SaxTag {
        NONE, TYPE, NAME, SUMMARY, IMAGES, INFOBOX, EPISODES, TAGS, COLLECTION, COLLECT, SECTIONS, CHARACTOR, TOPIC, BLOG, LINKED, RECOMMEND, SEASON, ONAIR
    }

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
         * @param type String
         * @return Int
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
         * @param type Int?
         * @return String
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
         * 获取条目类型Int
         * @param type String?
         * @return Int
         */
        @SubjectType
        fun parseTypeInt(type: String?): Int {
            return when (type) {
                TYPE_BOOK -> 1
                TYPE_ANIME -> 2
                TYPE_MUSIC -> 3
                TYPE_GAME -> 4
                TYPE_REAL -> 6
                else -> 0
            }
        }

        fun parseChaseCollection(it: Element): Subject? {
            val data = it.selectFirst(".headerInner a.textTip") ?: return null
            return Subject(
                id = data.attr("data-subject-id")?.toIntOrNull() ?: return null,
                type = parseType(it.attr("subject_type")?.toIntOrNull()),
                name = data.attr("data-subject-name"),
                name_cn = data.attr("data-subject-name-cn"),
                image = Bangumi.parseImageUrl(it.selectFirst("img")),
                eps = Episode.parseProgressList(it),
                eps_count = it.selectFirst(".prgBatchManagerForm .grey")?.text()?.trim(' ', '/')?.toIntOrNull()
                    ?: it.selectFirst("input[name=watchedeps]")?.parent()?.ownText()?.trim(' ', '/')?.toIntOrNull()
                    ?: 0,
                vol_count = it.selectFirst("input[name=watched_vols]")?.parent()?.let {
                    it.ownText().trim(' ', '/').toIntOrNull() ?: -1
                } ?: 0,
                ep_status = it.selectFirst("input[name=watchedeps]")?.attr("value")?.toIntOrNull() ?: 0,
                vol_status = it.selectFirst("input[name=watched_vols]")?.attr("value")?.toIntOrNull() ?: 0)
        }

        /**
         * 获取条目信息
         * @param subject Subject
         * @param onUpdate Function2<Subject, SaxTag, Unit>
         * @return Call<Subject>
         */
        fun getDetail(subject: Subject, onUpdate: (Subject, SaxTag) -> Unit = { _, _ -> }): Observable<Subject> {
            return ApiHelper.createHttpObservable(subject.url).map { rsp ->
                var lastTag = SaxTag.NONE
                var tankobon: List<Subject>? = null
                val updateSubject = { str: String, newTag: SaxTag ->
                    val doc = Jsoup.parse(str)
                    doc.outputSettings().prettyPrint(false)

                    when (lastTag) {
                        SaxTag.TYPE -> subject.type = when (doc.selectFirst("#navMenuNeue .focus").text()) {
                            "动画" -> TYPE_ANIME
                            "书籍" -> TYPE_BOOK
                            "音乐" -> TYPE_MUSIC
                            "游戏" -> TYPE_GAME
                            "三次元" -> TYPE_REAL
                            else -> TYPE_ANY
                        }
                        SaxTag.NAME -> {
                            subject.name = doc.selectFirst(".nameSingle> a")?.text() ?: subject.name
                            subject.name_cn = doc.selectFirst(".nameSingle> a")?.attr("title") ?: subject.name_cn
                            subject.category = doc.selectFirst(".nameSingle small")?.text() ?: subject.category
                        }
                        SaxTag.SUMMARY -> subject.summary =
                            doc.selectFirst("#subject_summary")?.let { HtmlUtil.html2text(it.html()) }
                                ?: subject.summary
                        SaxTag.IMAGES -> subject.image =
                            doc.selectFirst(".infobox img.cover")?.let { Bangumi.parseImageUrl(it) }
                                ?: subject.image
                        SaxTag.INFOBOX -> {
                            val infobox = doc.select("#infobox li")?.map { li ->
                                val tip = li.selectFirst("span.tip")?.text() ?: ""
                                var value = ""
                                li.childNodes()?.forEach {
                                    if (it !is Element || !it.hasClass("tip")) value += it.outerHtml()
                                }
                                Pair(tip.trim(':', ' '), value.trim())
                            }
                            subject.infobox = infobox ?: subject.infobox
                            subject.air_date =
                                infobox?.firstOrNull { it.first in arrayOf("放送开始", "上映年度", "开始") }?.second
                                    ?: subject.air_date
                            subject.air_weekday = ("一二三四五六日".map { "星期$it" }.indexOf(
                                infobox?.firstOrNull { it.first == "放送星期" }?.second ?: ""
                            ) + 1).let { week ->
                                if (week == 0) "月火水木金土日".map { "${it}曜日" }.indexOf(
                                    infobox?.firstOrNull { it.first == "放送星期" }?.second ?: ""
                                ) + 1 else week
                            }
                        }
                        SaxTag.EPISODES -> subject.eps = Episode.parseProgressList(doc)
                        SaxTag.TAGS -> {
                            subject.tags = doc.select(".subject_tag_section a")?.map {
                                Pair(
                                    it.selectFirst("span")?.text()
                                        ?: "", it.selectFirst("small")?.text()?.toIntOrNull() ?: 0
                                )
                            }
                        }
                        SaxTag.COLLECTION -> {
                            subject.collection = doc.select("#subjectPanelCollect .tip_i a")
                                ?.mapNotNull { Regex("(\\d+)人(.+)").find(it.text())?.groupValues }?.let { list ->
                                    UserCollection(
                                        wish = list.firstOrNull { it[2].contains("想") }?.get(1)?.toIntOrNull() ?: 0,
                                        collect = list.firstOrNull { it[2].contains("过") }?.get(1)?.toIntOrNull() ?: 0,
                                        doing = list.firstOrNull { it[2].contains("在") }?.get(1)?.toIntOrNull() ?: 0,
                                        on_hold = list.firstOrNull { it[2] == "搁置" }?.get(1)?.toIntOrNull() ?: 0,
                                        dropped = list.firstOrNull { it[2] == "抛弃" }?.get(1)?.toIntOrNull() ?: 0
                                    )
                                }
                        }
                        SaxTag.COLLECT -> {
                            subject.rating = UserRating(
                                rank = doc.selectFirst(".global_score .alarm")?.text()?.trim('#')?.toIntOrNull()
                                    ?: subject.rating?.rank ?: 0,
                                total = doc.selectFirst("span[property=\"v:votes\"]")?.text()?.toIntOrNull()
                                    ?: subject.rating?.total ?: 0,
                                count = {
                                    val counts = IntArray(10)
                                    doc.select(".horizontalChart li")?.forEach {
                                        counts[(it.selectFirst(".label")?.text()?.toIntOrNull() ?: 0) - 1] =
                                            it.selectFirst(".count").text()?.trim('(', ')')?.toIntOrNull() ?: 0
                                    }
                                    counts
                                }(),
                                score = doc.selectFirst(".global_score .number")?.text()?.toFloatOrNull()
                                    ?: subject.rating?.score ?: 0f,
                                friend_score = doc.selectFirst(".frdScore .num")?.text()?.toFloatOrNull()
                                    ?: subject.rating?.friend_score ?: 0f,
                                friend_count = doc.selectFirst(".frdScore a.l")?.text()?.split(" ")?.getOrNull(0)
                                    ?.toIntOrNull()
                                    ?: subject.rating?.friend_count ?: 0
                            )
                            subject.collect = doc.selectFirst("#collectBoxForm")?.let {
                                Collection(
                                    status = doc.selectFirst(".collectType input[checked=checked]")
                                        ?.id() ?: return@let null,
                                    rating = doc.selectFirst(".rating[checked]")
                                        ?.attr("value")?.toIntOrNull() ?: 0,
                                    comment = doc.selectFirst("#comment")?.text(),
                                    private = doc.selectFirst("#privacy[checked]")
                                        ?.attr("value")?.toIntOrNull() ?: 0,
                                    tag = doc.selectFirst("#tags")?.attr("value")
                                        ?.split(" ")?.filter { it.isNotEmpty() } ?: ArrayList(),
                                    myTag = doc.select("div.tagList")?.firstOrNull {
                                            it.selectFirst("span.tip_j")?.text()?.contains("我的标签") ?: false
                                        }
                                        ?.select("div.inner a")?.map { it.text() }
                                )
                            } ?: Collection()
                            subject.eps_count = doc.selectFirst("input[name=watchedeps]")
                                ?.parent()?.ownText()?.trim(' ', '/')?.toIntOrNull() ?: 0
                            subject.ep_status = doc.selectFirst("input[name=watchedeps]")
                                ?.attr("value")?.toIntOrNull() ?: 0
                            subject.vol_count = doc.selectFirst("input[name=watched_vols]")?.let {
                                it.parent()?.ownText()?.trim(' ', '/')?.toIntOrNull() ?: -1
                            } ?: 0
                            subject.vol_status = doc.selectFirst("input[name=watched_vols]")
                                ?.attr("value")?.toIntOrNull() ?: 0
                        }
                        SaxTag.SECTIONS -> {
                            val subtitle = doc.selectFirst(".subtitle")?.text()
                            when {
                                subtitle == "角色介绍" -> {
                                    lastTag = SaxTag.CHARACTOR
                                    subject.crt = doc.select("li")?.map {
                                        val a = it.selectFirst("a.avatar")
                                        Character(
                                            id = Regex("""/character/([0-9]*)""").find(
                                                a?.attr("href") ?: ""
                                            )?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                            name = a?.text() ?: "",
                                            name_cn = it.selectFirst(".info .tip")?.text() ?: "",
                                            role_name = it.selectFirst(".info .badge_job_tip")?.text() ?: "",
                                            image = Bangumi.parseImageUrl(a.selectFirst("span.avatarNeue")),
                                            comment = it.selectFirst("small.fade")?.text()
                                                ?.trim('(', '+', ')')?.toIntOrNull() ?: 0,
                                            actors = it.select("a[rel=\"v:starring\"]").map { psn ->
                                                Person(
                                                    id = Regex("""/person/([0-9]*)""").find(
                                                        psn.attr("href") ?: ""
                                                    )?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                                    name = psn.text() ?: ""
                                                )
                                            })
                                    }
                                }
                                subtitle == "讨论版" -> {
                                    lastTag = SaxTag.TOPIC
                                    subject.topic = doc.select(".topic_list tr")?.mapNotNull {
                                        val tds = it.select("td")
                                        val td0 = tds?.get(0)?.selectFirst("a")
                                        if (td0?.attr("href").isNullOrEmpty()) null else Topic(
                                            model = "subject",
                                            id = Regex("""/topic/([0-9]*)""").find(
                                                td0?.attr("href") ?: ""
                                            )?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                            title = td0?.text() ?: "",
                                            time = tds?.get(3)?.text(),
                                            replyCount = Regex("""([0-9]*)""").find(
                                                tds?.get(2)?.text() ?: ""
                                            )?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                            user = UserInfo.parse(tds?.get(1)?.selectFirst("a"))
                                        )
                                    }
                                }
                                subtitle == "评论" -> {
                                    lastTag = SaxTag.BLOG
                                    subject.blog = doc.select("div.item")?.map {
                                        val user = UserInfo.parse(it.selectFirst(".tip_j a"))
                                        Topic(
                                            model = "blog",
                                            id = Regex("""/blog/([0-9]*)""").find(
                                                it.selectFirst(".title a")?.attr("href") ?: ""
                                            )?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                            title = it.selectFirst(".title a")?.text() ?: "",
                                            image = Bangumi.parseImageUrl(it.selectFirst("img")),
                                            replyCount = it.selectFirst("small.orange")?.text()
                                                ?.trim('(', '+', ')')?.toIntOrNull() ?: 0,
                                            time = it.selectFirst("small.time")?.text(),
                                            blog = TopicPost(
                                                "", "",
                                                pst_uid = user.username ?: "",
                                                username = user.username ?: "",
                                                nickname = user.nickname ?: "",
                                                pst_content = it.selectFirst(".content")?.ownText() ?: "",
                                                dateline = it.selectFirst("small.time")?.text() ?: "",
                                                model = "blog"
                                            ),
                                            user = user
                                        )
                                    }
                                }
                                subtitle == "单行本" -> tankobon = doc.select("li")?.map {
                                    val avatar = it.selectFirst("a.avatar")
                                    val title = avatar?.attr("title")?.split("/ ")
                                    Subject(
                                        id = Regex("""/subject/([0-9]*)""").find(
                                            avatar?.attr("href") ?: ""
                                        )?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                        name = title?.getOrNull(0),
                                        name_cn = title?.getOrNull(1),
                                        category = "单行本",
                                        image = Bangumi.parseImageUrl(avatar.selectFirst("span.avatarNeue"))
                                    )
                                }
                                subtitle == "关联条目" -> {
                                    lastTag = SaxTag.LINKED
                                    var sub = ""
                                    val linked = doc.select("li")?.mapNotNull {
                                        val newSub = it.selectFirst(".sub").text()
                                        if (!newSub.isNullOrEmpty()) sub = newSub
                                        val avatar = it.selectFirst(".avatar")
                                        val title = it.selectFirst(".title")
                                        val id = Regex("""/subject/([0-9]*)""").find(
                                            title?.attr("href")
                                                ?: ""
                                        )?.groupValues?.get(1)?.toIntOrNull() ?: 0
                                        if (tankobon?.firstOrNull { b -> b.id == id } == null)
                                            Subject(
                                                id = id,
                                                name = title?.text(),
                                                name_cn = avatar.attr("title"),
                                                category = sub,
                                                image = Bangumi.parseImageUrl(avatar.selectFirst("span.avatarNeue"))
                                            )
                                        else null
                                    }?.toMutableList() ?: ArrayList()
                                    linked.addAll(0, tankobon ?: ArrayList())
                                    subject.linked = linked
                                }
                                subtitle?.contains("大概会喜欢") ?: false -> {
                                    lastTag = SaxTag.RECOMMEND
                                    subject.recommend = doc.select("li")?.map {
                                        val avatar = it.selectFirst(".avatar")
                                        val title = it.selectFirst(".info a")
                                        Subject(
                                            id = Regex("""/subject/([0-9]*)""").find(
                                                title?.attr("href") ?: ""
                                            )?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                            name = title?.text(),
                                            name_cn = avatar.attr("title"),
                                            image = Bangumi.parseImageUrl(avatar.selectFirst("span.avatarNeue"))
                                        )
                                    }
                                }
                            }

                        }
                        else -> {
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
                            updateSubject(str(), SaxTag.TYPE)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        hasClass("nameSingle") -> {
                            updateSubject(str(), SaxTag.NAME)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        attr("id") == "subject_summary" -> {
                            updateSubject(str(), SaxTag.SUMMARY)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        hasClass("infobox") -> {
                            updateSubject(str(), SaxTag.IMAGES)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        attr("id") == "infobox" -> {
                            updateSubject(str(), SaxTag.INFOBOX)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        attr("id") == "subjectPanelCollect" -> {
                            updateSubject(str(), SaxTag.COLLECTION)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        attr("id") == "panelInterestWrapper" -> {
                            updateSubject(str(), SaxTag.COLLECT)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        hasClass("line_list_music") || hasClass("prg_list") -> {
                            updateSubject(str(), SaxTag.EPISODES)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        hasClass("subject_section") -> {
                            updateSubject(str(), SaxTag.SECTIONS)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        hasClass("subject_tag_section") -> {
                            updateSubject(str(), SaxTag.TAGS)
                            ApiHelper.SaxEventType.BEGIN
                        }
                        else -> ApiHelper.SaxEventType.NOTHING
                    }
                }
                updateSubject(lastData, SaxTag.NONE)
                subject
            }
        }

        /**
         * 更新进度
         * @param id Int
         * @param status String
         * @param epIds String?
         * @return Call<Boolean>
         */
        fun updateProgress(
            id: Int,
            @Episode.EpisodeStatus status: String,
            epIds: String? = null
        ): Observable<Boolean> {
            return ApiHelper.createHttpObservable(
                "${Bangumi.SERVER}/subject/ep/$id/status/$status?gh=${HttpUtil.formhash}&ajax=1",
                body = FormBody.Builder()
                    .add("ep_id", epIds ?: id.toString()).build()
            ).map { rsp ->
                rsp.body?.string()?.contains("\"status\":\"ok\"") == true
            }
        }

        /**
         * 更新进度(看到)
         * @param subject Subject
         * @param watchedeps String
         * @param watched_vols String
         * @return Call<Boolean>
         */
        fun updateSubjectProgress(
            subject: Subject,
            watchedeps: String,
            watched_vols: String
        ): Observable<Boolean> {
            val body = FormBody.Builder()
                .add("referer", "subject")
                .add("submit", "更新")
                .add("watchedeps", watchedeps)
            if (subject.vol_count != 0) body.add("watched_vols", watched_vols)
            return ApiHelper.createHttpObservable(
                "${Bangumi.SERVER}/subject/set/watched/${subject.id}",
                body = body.build()
            ).map { rsp ->
                rsp.code == 200
            }
        }
    }
}