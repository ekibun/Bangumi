package soko.ekibun.bangumi.api.bangumi

import android.util.Log
import android.webkit.CookieManager
import com.google.gson.reflect.TypeToken
import okhttp3.FormBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.xmlpull.v1.XmlPullParser
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.*
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.util.TextUtil
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

object Bangumi {
    const val SERVER = "https://bgm.tv"

    /**
     * 获取完整的URL
     */
    fun parseUrl(url: String): String {
        if (url in arrayOf("/img/info_only.png", "/img/info_only_m.png", "/img/no_icon_subject.png")) return ""
        return try {
            URI.create(SERVER)?.resolve(url)?.toASCIIString() ?: URI.create(url).toASCIIString()
        } catch (e: Exception) {
            url
        }
    }

    /**
     * 获取图片的URL
     */
    fun parseImageUrl(cover: Element?): String {
        return parseUrl(if (cover?.hasAttr("src") == true) cover.attr("src") ?: "" else cover?.attr("data-cfsrc") ?: "")
    }

    private fun parseUserInfo(user: Element?, avatar: Element? = null): UserInfo {
        val username = Regex("""/user/([^/]*)""").find(user?.attr("href") ?: "")?.groupValues?.get(1)
        return UserInfo(
                id = username?.toIntOrNull() ?: 0,
                username = username,
                nickname = user?.text(),
                avatar = Images(parseUrl(Regex("""background-image:url\('([^']*)'\)""").find(avatar?.html()
                        ?: "")?.groupValues?.get(1) ?: ""))
        )
    }

    /**
     * 剧集和曲目列表
     */
    private fun parseLineList(doc: Element): List<Episode> {
        var cat = "本篇"
        return doc.select("ul.line_list>li").mapNotNull { li ->
            if (li.hasClass("cat")) cat = li.text()
            val h6a = li.selectFirst("h6>a") ?: return@mapNotNull null
            val values = Regex("^\\D*(\\d+\\.?\\d?)\\.(.*)").find(h6a.text() ?: "")?.groupValues
                    ?: " ${h6a.text()}".split(" ", limit = 3)
            val epInfo = li.select("small.grey")?.text()?.split("/")
            Episode(
                    id = Regex("""/ep/([0-9]*)""").find(h6a.attr("href") ?: "")?.groupValues?.get(1)?.toIntOrNull()
                            ?: return@mapNotNull null,
                    type = if (cat.startsWith("Disc")) Episode.TYPE_MUSIC else when (cat) {
                        "本篇" -> Episode.TYPE_MAIN
                        "特别篇" -> Episode.TYPE_SP
                        "OP" -> Episode.TYPE_OP
                        "ED" -> Episode.TYPE_ED
                        "PV" -> Episode.TYPE_PV
                        "MAD" -> Episode.TYPE_MAD
                        else -> Episode.TYPE_OTHER
                    },
                    sort = values.getOrNull(1)?.toFloatOrNull() ?: 0f,
                    name = values.getOrNull(2) ?: h6a.text(),
                    name_cn = li.selectFirst("h6>span.tip")?.text()?.substringAfter(" "),
                    duration = epInfo?.firstOrNull { it.trim().startsWith("时长") }?.substringAfter(":"),
                    airdate = epInfo?.firstOrNull { it.trim().startsWith("首播") }?.substringAfter(":"),
                    comment = epInfo?.firstOrNull { it.trim().startsWith("讨论") }?.trim()?.substringAfter("+")?.toIntOrNull()
                            ?: 0,
                    status = if (cat.startsWith("Disc")) Episode.STATUS_AIR else li.selectFirst(".epAirStatus span")?.className(),
                    progress = li.selectFirst(".listEpPrgManager>span")?.let {
                        when {
                            it.hasClass("statusWatched") -> Episode.PROGRESS_WATCH
                            it.hasClass("statusQueue") -> Episode.PROGRESS_QUEUE
                            it.hasClass("statusDrop") -> Episode.PROGRESS_DROP
                            else -> null
                        }
                    },
                    category = if (cat.startsWith("Disc")) cat else null)
        }

    }

    /**
     * 主页和概览的剧集信息
     */
    private fun parseProgressList(item: Element, doc: Element? = null): List<Episode> {
        if (item.selectFirst("ul.line_list_music") != null) return parseLineList(item)
        var cat = "本篇"
        val now = Date().time
        return item.select("ul.prg_list li").mapNotNull { li ->
            if (li.hasClass("subtitle")) cat = li.text()
            val it = li.selectFirst("a") ?: return@mapNotNull null
            val rel = doc?.selectFirst(it.attr("rel"))
            val epInfo = rel?.selectFirst(".tip")?.textNodes()?.map { it.text() }
            val airdate = epInfo?.firstOrNull { it.startsWith("首播") }?.substringAfter(":")
            Episode(
                    id = it.id().substringAfter("_").toIntOrNull() ?: return@mapNotNull null,
                    type = when (cat) {
                        "本篇" -> Episode.TYPE_MAIN
                        "SP" -> Episode.TYPE_SP
                        "OP" -> Episode.TYPE_OP
                        "ED" -> Episode.TYPE_ED
                        "PV" -> Episode.TYPE_PV
                        "MAD" -> Episode.TYPE_MAD
                        else -> Episode.TYPE_OTHER
                    },
                    sort = Regex("""\d*(\.\d*)?""").find(it.text())?.groupValues?.getOrNull(0)?.toFloatOrNull() ?: 0f,
                    name = it.attr("title")?.substringAfter(" "),
                    name_cn = epInfo?.firstOrNull { it.startsWith("中文标题") }?.substringAfter(":"),
                    duration = epInfo?.firstOrNull { it.startsWith("时长") }?.substringAfter(":"),
                    airdate = airdate,
                    comment = rel?.selectFirst(".cmt .na")?.text()?.trim('(', ')', '+')?.toIntOrNull() ?: 0,
                    status = when {
                        it.hasClass("epBtnToday") -> Episode.STATUS_TODAY
                        it.hasClass("epBtnAir") || it.hasClass("epBtnWatched") || (rel != null && (try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(airdate ?: "")
                        } catch (e: Exception) {
                            null
                        }?.time ?: 0L < now)) -> Episode.STATUS_AIR
                        else -> Episode.STATUS_NA
                    },
                    progress = when {
                        it.hasClass("epBtnWatched") -> Episode.PROGRESS_WATCH
                        it.hasClass("epBtnQueue") -> Episode.PROGRESS_QUEUE
                        it.hasClass("epBtnDrop") -> Episode.PROGRESS_DROP
                        else -> null
                    })
        }
    }

    /**
     * 获取用户收藏列表
     */
    fun getCollectionList(
            @Subject.SubjectType subject_type: String,
            username: String,
            @Collection.CollectionStatusType collection_status: String,
            page: Int = 1
    ): Call<List<Subject>> {
        return ApiHelper.buildHttpCall("$SERVER/$subject_type/list/$username/$collection_status?page=$page") {
            val doc = Jsoup.parse(it.body()?.string() ?: "")
            val ret = ArrayList<Subject>()
            for (item in doc.select(".item")) {
                val id = item.attr("id").split('_').getOrNull(1)?.toIntOrNull() ?: continue
                val nameCN = item.selectFirst("h3 a")?.text()
                val name = item.selectFirst("h3 small")?.text() ?: nameCN
                ret += Subject(
                        id = id,
                        type = Subject.TYPE_ANY,
                        name = name,
                        name_cn = nameCN,
                        summary = item.selectFirst(".info")?.text(),
                        images = Images(parseImageUrl(item.selectFirst("img"))),
                        ep_status = -1
                )
            }
            ret
        }
    }

    /**
     * 获取条目信息
     * @param subject 条目
     */
    fun getSubject(
            subject: Subject
    ): Call<Subject> {
        return ApiHelper.buildHttpCall(subject.url) { response ->
            val doc = Jsoup.parse(response.body()?.string() ?: "")

            val infobox = doc.select("#infobox li")?.map { li ->
                val tip = li.selectFirst("span.tip")?.text() ?: ""
                var value = ""
                li.childNodes()?.forEach { if (it !is Element || !it.hasClass("tip")) value += it.outerHtml() }
                Pair(tip.trim(':', ' '), value.trim())
            }
            HttpUtil.formhash = doc.selectFirst("input[name=formhash]")?.attr("value") ?: HttpUtil.formhash
            Subject(id = subject.id,
                    type = when (doc.selectFirst("#navMenuNeue .focus").text()) {
                        "动画" -> Subject.TYPE_ANIME
                        "书籍" -> Subject.TYPE_BOOK
                        "音乐" -> Subject.TYPE_MUSIC
                        "游戏" -> Subject.TYPE_GAME
                        "三次元" -> Subject.TYPE_REAL
                        else -> Subject.TYPE_ANY
                    },
                    name = doc.selectFirst(".nameSingle> a")?.text() ?: subject.name,
                    name_cn = doc.selectFirst(".nameSingle> a")?.attr("title") ?: subject.name_cn,
                    summary = doc.selectFirst("#subject_summary")?.let { TextUtil.html2text(it.html()) }
                            ?: subject.summary,
                    images = Images(parseImageUrl(doc.selectFirst(".infobox img.cover"))),
                    air_date = infobox?.firstOrNull { it.first in arrayOf("放送开始", "上映年度", "开始") }?.second ?: "",
                    air_weekday = ("一二三四五六日".map { "星期$it" }.indexOf(infobox?.firstOrNull { it.first == "放送星期" }?.second
                            ?: "") + 1).let {
                        if (it == 0) "月火水木金土日".map { "${it}曜日" }.indexOf(infobox?.firstOrNull { it.first == "放送星期" }?.second
                                ?: "") + 1 else it
                    },
                    infobox = infobox,
                    category = doc.selectFirst(".nameSingle small")?.text() ?: "",
                    rating = Subject.UserRating(
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
                    ),
                    collection = doc.select("#subjectPanelCollect .tip_i a")?.mapNotNull { Regex("(\\d+)人(.+)").find(it.text())?.groupValues }?.let { list ->
                        Subject.UserCollection(
                                wish = list.firstOrNull { it[2].contains("想") }?.get(1)?.toIntOrNull() ?: 0,
                                collect = list.firstOrNull { it[2].contains("过") }?.get(1)?.toIntOrNull() ?: 0,
                                doing = list.firstOrNull { it[2].contains("在") }?.get(1)?.toIntOrNull() ?: 0,
                                on_hold = list.firstOrNull { it[2] == "搁置" }?.get(1)?.toIntOrNull() ?: 0,
                                dropped = list.firstOrNull { it[2] == "抛弃" }?.get(1)?.toIntOrNull() ?: 0
                        )
                    },
                    eps = parseProgressList(doc, doc),
                    eps_count = doc.selectFirst("input[name=watchedeps]")?.parent()?.ownText()?.trim(' ', '/')?.toIntOrNull()
                            ?: 0,
                    ep_status = doc.selectFirst("input[name=watchedeps]")?.attr("value")?.toIntOrNull() ?: 0,
                    vol_count = doc.selectFirst("input[name=watched_vols]")?.let {
                        it.parent()?.ownText()?.trim(' ', '/')?.toIntOrNull() ?: -1
                    } ?: 0,
                    vol_status = doc.selectFirst("input[name=watched_vols]")?.attr("value")?.toIntOrNull() ?: 0,
                    crt = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "角色介绍" }.getOrNull(0)?.select("li")?.map {
                        val a = it.selectFirst("a.avatar")
                        Character(
                                id = Regex("""/character/([0-9]*)""").find(a?.attr("href")
                                        ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                name = a?.text() ?: "",
                                name_cn = it.selectFirst(".info .tip")?.text() ?: "",
                                role_name = it.selectFirst(".info .badge_job_tip")?.text() ?: "",
                                images = Images(parseUrl(Regex("""background-image:url\('([^']*)'\)""").find(a?.html()
                                        ?: "")?.groupValues?.get(1) ?: "")),
                                comment = it.selectFirst("small.fade")?.text()?.trim('(', '+', ')')?.toIntOrNull() ?: 0,
                                actors = it.select("a[rel=\"v:starring\"]").map { psn ->
                                    Person(
                                            id = Regex("""/person/([0-9]*)""").find(psn.attr("href")
                                                    ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                            name = psn.text() ?: "")
                                })
                    },
                    topic = doc.select(".topic_list tr")?.mapNotNull {
                        val tds = it.select("td")
                        val td0 = tds?.get(0)?.selectFirst("a")
                        if (td0?.attr("href").isNullOrEmpty()) null else Subject.Topic(
                                id = Regex("""/topic/([0-9]*)""").find(td0?.attr("href")
                                        ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                title = td0?.text() ?: "",
                                time = tds?.get(3)?.text(),
                                replies = Regex("""([0-9]*)""").find(tds?.get(2)?.text()
                                        ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                user = parseUserInfo(tds?.get(1)?.selectFirst("a"))
                        )
                    },
                    blog = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "评论" }.getOrNull(0)?.select("div.item")?.map {
                        Subject.Blog(
                                id = Regex("""/blog/([0-9]*)""").find(it.selectFirst(".title a")?.attr("href")
                                        ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                title = it.selectFirst(".title a")?.text() ?: "",
                                summary = it.selectFirst(".content")?.ownText() ?: "",
                                image = parseImageUrl(it.selectFirst("img")),
                                replies = it.selectFirst("small.orange")?.text()?.trim('(', '+', ')')?.toIntOrNull()
                                        ?: 0,
                                time = it.selectFirst("small.time")?.text(),
                                user = parseUserInfo(it.selectFirst(".tip_j a"))
                        )
                    },
                    linked = {
                        val tankobon = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "单行本" }.getOrNull(0)?.select("li")?.map {
                            val avatar = it.selectFirst(".avatar")
                            val title = avatar?.attr("title")?.split("/ ")
                            Subject(
                                    id = Regex("""/subject/([0-9]*)""").find(avatar?.attr("href")
                                            ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                    name = title?.getOrNull(0),
                                    name_cn = title?.getOrNull(1),
                                    category = "单行本",
                                    images = Images(parseUrl(Regex("""background-image:url\('([^']*)'\)""").find(avatar?.html()
                                            ?: "")?.groupValues?.get(1) ?: ""))
                            )
                        }
                        var sub = ""
                        val linked = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "关联条目" }.getOrNull(0)?.select("li")?.mapNotNull {
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
                                        images = Images(parseUrl(Regex("""background-image:url\('([^']*)'\)""").find(avatar?.html()
                                                ?: "")?.groupValues?.get(1) ?: ""))
                                )
                            else null
                        }?.toMutableList() ?: ArrayList()
                        linked.addAll(0, tankobon ?: ArrayList())
                        linked
                    }(),
                    recommend = doc.select(".subject_section").filter { it.select(".subtitle")?.text()?.contains("大概会喜欢") == true }.getOrNull(0)?.select("li")?.map {
                        val avatar = it.selectFirst(".avatar")
                        val title = it.selectFirst(".info a")
                        Subject(
                                id = Regex("""/subject/([0-9]*)""").find(title?.attr("href")
                                        ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                name = title?.text(),
                                name_cn = avatar.attr("title"),
                                images = Images(parseUrl(Regex("""background-image:url\('([^']*)'\)""").find(avatar?.html()
                                        ?: "")?.groupValues?.get(1) ?: ""))
                        )
                    },
                    tags = doc.select(".subject_tag_section a")?.map {
                        Pair(it.selectFirst("span")?.text() ?: "", it.selectFirst("small")?.text()?.toIntOrNull() ?: 0)
                    },
                    collect = doc.selectFirst("#collectBoxForm")?.let {
                        Collection(
                                status = it.selectFirst(".collectType input[checked=checked]")?.id() ?: return@let null,
                                rating = it.selectFirst(".rating[checked]")?.attr("value")?.toIntOrNull() ?: 0,
                                comment = it.selectFirst("#comment")?.text(),
                                private = it.selectFirst("#privacy[checked]")?.attr("value")?.toIntOrNull() ?: 0,
                                tag = it.selectFirst("#tags")?.attr("value")?.split(" ")?.filter { it.isNotEmpty() }
                                        ?: ArrayList(),
                                myTag = it.select("div.tagList")?.firstOrNull { it.selectFirst("span.tip_j").text().contains("我的标签") }
                                        ?.select("div.inner a")?.map { it.text() }
                        )
                    })
        }
    }

    /**
     * 条目搜索
     */
    fun searchSubject(
            keywords: String,
            @Subject.SubjectType type: String = Subject.TYPE_ANY,
            page: Int
    ): Call<List<Subject>> {
        CookieManager.getInstance().setCookie(SERVER, "chii_searchDateLine=${System.currentTimeMillis() / 1000};")
        return ApiHelper.buildHttpCall("$SERVER/subject_search/${java.net.URLEncoder.encode(keywords, "utf-8")}?cat=${when (type) {
            Subject.TYPE_BOOK -> 1
            Subject.TYPE_ANIME -> 2
            Subject.TYPE_MUSIC -> 3
            Subject.TYPE_GAME -> 4
            Subject.TYPE_REAL -> 6
            else -> 0
        }}&page=$page") { rsp ->
            val doc = Jsoup.parse(rsp.body()?.string() ?: "")
            if (doc.select("#colunmNotice") == null) throw Exception("search error")
            doc.select(".item").mapNotNull { item ->
                val nameCN = item.selectFirst("h3")?.selectFirst("a")?.text()
                Subject(
                        id = item.attr("id").split('_').last().toIntOrNull() ?: return@mapNotNull null,
                        type = Subject.parseType(item.selectFirst(".ico_subject_type")?.classNames()?.mapNotNull { it.split('_').last().toIntOrNull() }?.firstOrNull()),
                        name = item.selectFirst("h3")?.selectFirst("small")?.text() ?: nameCN,
                        name_cn = nameCN,
                        summary = item.selectFirst(".info")?.text(),
                        images = Images(parseImageUrl(item.selectFirst("img"))),
                        collect = if (item.selectFirst(".collectBlock")?.text()?.contains("修改") == true) Collection() else null)
            }
        }
    }

    /**
     * 人物搜索
     */
    fun searchMono(
            keywords: String,
            type: String,
            page: Int
    ): Call<List<MonoInfo>> {
        CookieManager.getInstance().setCookie(SERVER, "chii_searchDateLine=${System.currentTimeMillis() / 1000};")
        return ApiHelper.buildHttpCall("$SERVER/mono_search/${java.net.URLEncoder.encode(keywords, "utf-8")}?cat=$type&page=$page") { rsp ->
            val doc = Jsoup.parse(rsp.body()?.string() ?: "")
            if (doc.select("#colunmNotice") == null) throw Exception("search error")
            doc.select(".light_odd").map {
                val a = it.selectFirst("h2 a")
                MonoInfo(
                        name = a?.ownText()?.trim('/', ' '),
                        name_cn = a?.selectFirst("span.tip")?.text(),
                        images = Images(parseImageUrl(it.selectFirst("img"))),
                        summary = it.selectFirst(".prsn_info")?.text(),
                        url = parseUrl(a?.attr("href") ?: ""))
            }
        }
    }

    /**
     * 吐槽箱
     */
    fun getComments(
            subject: Subject,
            page: Int
    ): Call<List<Comment>> {
        return ApiHelper.buildHttpCall("$SERVER/subject/${subject.id}/comments?page=$page") { rsp ->
            val doc = Jsoup.parse(rsp.body()?.string() ?: "")
            doc.select("#comment_box .item").mapNotNull {
                val user = it.selectFirst(".text a")
                val username = Regex("""/user/([^/]*)""").find(user?.attr("href") ?: "")?.groupValues?.get(1)
                Comment(
                        user = UserInfo(
                                id = username?.toIntOrNull() ?: 0,
                                username = username,
                                nickname = user?.text(),
                                avatar = Images(parseUrl(Regex("""background-image:url\('([^']*)'\)""").find(it.selectFirst(".avatar")?.html()
                                        ?: "")?.groupValues?.get(1) ?: ""))),
                        time = it.selectFirst(".grey")?.text()?.replace("@", "")?.trim(),
                        comment = it.selectFirst("p")?.text(),
                        rate = Regex("""stars([0-9]*)""").find(it.selectFirst(".text")?.selectFirst(".starlight")?.outerHtml()
                                ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0)
            }
        }
    }

    /**
     * 索引
     */
    fun browserAirTime(
            @Subject.SubjectType subject_type: String,
            year: Int,
            month: Int,
            page: Int,
            sub_cat: String
    ): Call<List<Subject>> {
        return ApiHelper.buildHttpCall("$SERVER/$subject_type/browser${if (sub_cat.isEmpty()) "" else "/$sub_cat"}/airtime/$year-$month?page=$page") {
            val doc = Jsoup.parse(it.body()?.string() ?: "")
            doc.select(".item").mapNotNull {
                val nameCN = it.selectFirst("h3 a")?.text()
                Subject(
                        id = it.attr("id").split('_').last().toIntOrNull() ?: return@mapNotNull null,
                        name = it.selectFirst("h3 small")?.text() ?: nameCN,
                        name_cn = nameCN,
                        summary = it.selectFirst(".info")?.text(),
                        images = Images(parseImageUrl(it.selectFirst("img"))),
                        collect = if (it.selectFirst(".collectBlock")?.text()?.contains("修改") == true) Collection() else null)
            }
        }
    }

    /**
     * 超展开
     */
    fun getRakuen(
            type: String
    ): Call<List<Rakuen>> {
        return ApiHelper.buildHttpCall("$SERVER/rakuen/topiclist" + if (type.isEmpty()) "" else "?type=$type") { rsp ->
            val doc = Jsoup.parse(rsp.body()?.string() ?: "")
            doc.select(".item_list").mapNotNull {
                val title = it.selectFirst(".title")
                val group = it.selectFirst(".row").selectFirst("a")
                Rakuen(
                        images = Images(parseUrl(Regex("""background-image:url\('([^']*)'\)""").find(it.selectFirst(".avatar")?.html()
                                ?: "")?.groupValues?.get(1) ?: "")),
                        topic = title.text(),
                        group = group?.text(),
                        time = it.selectFirst(".time")?.text()?.replace("...", "") ?: "",
                        reply = it.selectFirst(".grey")?.text()?.trim('(', '+', ')')?.toIntOrNull() ?: 0,
                        url = parseUrl(title.attr("href") ?: ""),
                        groupUrl = parseUrl(group?.attr("href") ?: ""))
            }
        }
    }

    /**
     * 时间线
     */
    fun getTimeLine(
            type: String,
            page: Int,
            usr: UserInfo?,
            global: Boolean
    ): Call<List<TimeLine>> {
        return ApiHelper.buildHttpCall("$SERVER${if (usr == null) "" else "/user/${usr.username}"}/timeline?type=$type&page=$page&ajax=1", useCookie = !global) { rsp ->
            val doc = Jsoup.parse(rsp.body()?.string() ?: "")
            val ret = ArrayList<TimeLine>()
            var user = usr ?: UserInfo()
            val cssInfo = if (usr == null) ".info" else ".info_full"
            doc.selectFirst("#timeline")?.children()?.forEach { timeline ->
                if (timeline.hasClass("Header")) {
                    ret += TimeLine(true, timeline.text())
                } else timeline.select(".tml_item")?.forEach { item ->
                    item.selectFirst("a.avatar")?.let {
                        user = parseUserInfo(it, it)
                    }
                    ret += TimeLine(TimeLine.TimeLineItem(
                            user = user,
                            action = item.selectFirst(cssInfo)?.childNodes()?.map {
                                if (it is TextNode || (it as? Element)?.tagName() == "a" && it.selectFirst("img") == null)
                                    it.outerHtml()
                                else if ((it as? Element)?.hasClass("status") == true)
                                    "<br/>" + it.html()
                                else ""
                            }?.reduce { acc, s -> acc + s } ?: "",
                            time = item.selectFirst(".date")?.text()?.trim('·', ' ', '回', '复') ?: "",
                            content = item.selectFirst(".collectInfo")?.text() ?: item.selectFirst(".info_sub")?.text(),
                            contentUrl = item.selectFirst(".info_sub a")?.attr("href"),
                            collectStar = Regex("""stars([0-9]*)""").find(item.selectFirst(".starlight")?.outerHtml()
                                    ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                            thumbs = item.select("$cssInfo img").map {
                                val url = it.parent().attr("href")
                                TimeLine.TimeLineItem.ThumbItem(
                                        images = Images(parseImageUrl(it)),
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
     * 讨论
     */
    private fun parseTopicPost(it: Element): TopicPost {
        val user = it.selectFirst(".inner a")
        val data = (it.selectFirst(".icons_cmt")?.attr("onclick") ?: "").split(",")
        val relate = data.getOrNull(2)?.toIntOrNull() ?: 0
        val post_id = data.getOrNull(3)?.toIntOrNull() ?: 0
        val badge = it.selectFirst(".badgeState")?.text()
        return TopicPost(
                pst_id = (if (post_id == 0) relate else post_id).toString(),
                pst_mid = data.getOrNull(1) ?: "",
                pst_uid = data.getOrNull(5) ?: "",
                pst_content = if (!badge.isNullOrEmpty()) it.selectFirst(".inner")?.ownText() ?: ""
                else it.selectFirst(".topic_content")?.html()
                        ?: it.selectFirst(".message")?.html()
                        ?: it.selectFirst(".cmt_sub_content")?.html() ?: "",
                username = Regex("""/user/([^/]*)""").find(user?.attr("href")
                        ?: "")?.groupValues?.get(1) ?: "",
                nickname = user?.text() ?: "",
                sign = if (!badge.isNullOrEmpty()) "" else it.selectFirst(".inner .tip_j")?.text() ?: "",
                avatar = Regex("""background-image:url\('([^']*)'\)""").find(it.selectFirst(".avatar")?.html()
                        ?: "")?.groupValues?.get(1) ?: "",
                dateline = if (!badge.isNullOrEmpty()) it.selectFirst(".inner .tip_j")?.text() ?: ""
                else it.selectFirst(".re_info")?.text()?.split("/")?.get(0)?.trim()?.substringAfter(" - ")
                        ?: "",
                is_self = it.selectFirst(".re_info")?.text()?.contains("/") == true,
                isSub = it.selectFirst(".re_info a")?.text()?.contains("-") ?: false,
                editable = it.selectFirst(".re_info")?.text()?.contains("/") == true,
                relate = relate.toString(),
                model = Regex("'([^']*)'").find(data.getOrNull(0) ?: "")?.groupValues?.get(1) ?: "",
                badge = badge
        )
    }


    fun getTopicSax(url: String, onBeforePost: (data: String) -> Unit, onNewPost: (post: TopicPost) -> Unit): Call<Topic> {
        return ApiHelper.buildHttpCall(url) { rsp ->
            var beforeData = ""
            val replies = ArrayList<TopicPost>()
            val updateReply = { str: String ->
                val it = Jsoup.parse(str)
                it.outputSettings().prettyPrint(false)

                val post = parseTopicPost(it)
                replies += post
                onNewPost(post)
            }
            val lastData = ApiHelper.parseWithSax(rsp) { parser, str ->
                when {
                    parser.eventType != XmlPullParser.START_TAG -> ApiHelper.SaxEventType.NOTHING
                    parser.getAttributeValue("", "id")?.startsWith("post_") == true -> {
                        Log.v("POST", str)
                        if (beforeData.isEmpty()) {
                            beforeData = str
                            onBeforePost(str)
                        } else {
                            updateReply(str)
                        }
                        ApiHelper.SaxEventType.BEGIN
                    }
                    parser.getAttributeValue("", "id")?.contains("reply_wrapper") == true -> {
                        updateReply(str)
                        ApiHelper.SaxEventType.BEGIN
                    }
                    else -> ApiHelper.SaxEventType.NOTHING
                }
            }

            val doc = Jsoup.parse(beforeData + lastData)
            val error = doc.selectFirst("#reply_wrapper")?.selectFirst(".tip")
            val form = doc.selectFirst("#ReplyForm")
            Topic(
                    group = doc.selectFirst("#pageHeader span")?.text() ?: "",
                    title = doc.selectFirst("#pageHeader h1")?.ownText() ?: "",
                    images = Images(parseImageUrl(doc.selectFirst("#pageHeader img"))),
                    replies = replies,
                    post = parseUrl("${form?.attr("action")}?ajax=1"),
                    lastview = form?.selectFirst("input[name=lastview]")?.attr("value"),
                    links = LinkedHashMap<String, String>().let { links ->
                        doc.selectFirst("#pageHeader")?.select("a")?.filter { !it.text().isNullOrEmpty() }?.forEach {
                            links[it.text()] = parseUrl(it.attr("href") ?: "")
                        }
                        links
                    },
                    error = error?.text(),
                    errorLink = parseUrl(error?.selectFirst("a")?.attr("href") ?: ""))
        }
    }

    fun getTopic(url: String): Call<Topic> {
        return ApiHelper.buildHttpCall(url) { rsp ->
            val doc = Jsoup.parse(rsp.body()?.string() ?: "")
            doc.outputSettings().prettyPrint(false)
            val error = doc.selectFirst("#reply_wrapper")?.selectFirst(".tip")
            val form = doc.selectFirst("#ReplyForm")
            HttpUtil.formhash = doc.selectFirst("input[name=formhash]")?.attr("value") ?: HttpUtil.formhash
            Topic(
                    group = doc.selectFirst("#pageHeader span")?.text() ?: "",
                    title = doc.selectFirst("#pageHeader h1")?.ownText() ?: "",
                    images = Images(parseImageUrl(doc.selectFirst("#pageHeader img"))),
                    replies = doc.select("div[id^=post_]")?.mapNotNull {
                        parseTopicPost(it)
                    } ?: ArrayList(),
                    post = parseUrl("${form?.attr("action")}?ajax=1"),
                    lastview = form?.selectFirst("input[name=lastview]")?.attr("value"),
                    links = LinkedHashMap<String, String>().let { links ->
                        doc.selectFirst("#pageHeader")?.select("a")?.filter { !it.text().isNullOrEmpty() }?.forEach {
                            links[it.text()] = parseUrl(it.attr("href") ?: "")
                        }
                        links
                    },
                    error = error?.text(),
                    errorLink = parseUrl(error?.selectFirst("a")?.attr("href") ?: ""))
        }
    }

    /**
     * 用户信息
     */
    fun getUserInfo(reload: () -> Unit): Call<UserInfo> {
        val cookieManager = CookieManager.getInstance()
        return ApiHelper.buildHttpCall("$SERVER/settings") {
            val doc = Jsoup.parse(it.body()?.string() ?: "")
            val user = doc.selectFirst(".idBadgerNeue a.avatar") ?: throw Exception("login failed")
            val username = Regex("""/user/([^/]*)""").find(user.attr("href") ?: "")?.groupValues?.get(1)
            it.headers("set-cookie").forEach {
                cookieManager.setCookie(SERVER, it)
            }
            if (it.headers("set-cookie").size > 0) reload()
            HttpUtil.formhash = doc.selectFirst("input[name=formhash]")?.attr("value") ?: HttpUtil.formhash
            UserInfo(
                    id = username?.toIntOrNull() ?: 0,
                    username = username,
                    nickname = doc.selectFirst("input[name=nickname]")?.attr("value"),
                    avatar = Images(parseUrl(Regex("""background-image:url\('([^']*)'\)""").find(user.html()
                            ?: "")?.groupValues?.get(1) ?: "")),
                    sign = doc.selectFirst("input[name=sign_input]")?.attr("value"),
                    notify = Pair(
                            Regex("叮咚叮咚～你有 ([0-9]+) 条新信息!").find(doc.selectFirst("#robot_speech_js")?.text()
                                    ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                            doc.selectFirst("#notify_count")?.text()?.toIntOrNull() ?: 0)
            )
        }
    }


    /**
     * 获取剧集列表
     */
    fun getSubjectEps(
            subject: Subject
    ): Call<List<Episode>> {
        return ApiHelper.buildHttpCall("$SERVER/subject/${subject.id}/ep") {
            parseLineList(Jsoup.parse(it.body()?.string() ?: ""))
        }
    }

    /**
     * 移动版的进度管理
     */
    fun getMobileCollection(): Call<List<Subject>> {
        return ApiHelper.buildHttpCall("$SERVER/m/prg") { rsp ->
            val doc = Jsoup.parse(rsp.body()?.string() ?: "")
            doc.select("#cloumnSubjectInfo .subjectItem").mapNotNull {
                var cat = "本篇"
                Subject(
                        id = it.id().split("_").getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null,
                        name = it.selectFirst(".header a[title]")?.attr("title"),
                        images = Images(parseUrl(Regex("""background-image:url\('([^']*)'\)""").find(it.selectFirst(".cover").attr("style")
                                ?: "")?.groupValues?.get(1) ?: "")),
                        eps = it.selectFirst(".prg_list").children().mapNotNull eps@{ ep ->
                            if (ep.hasClass("cat")) cat = ep.text()
                            Episode(
                                    id = ep.id().split("_").getOrNull(1)?.toIntOrNull() ?: return@eps null,
                                    type = when (cat) {
                                        "本篇" -> Episode.TYPE_MAIN
                                        "SP" -> Episode.TYPE_SP
                                        "OP" -> Episode.TYPE_OP
                                        "ED" -> Episode.TYPE_ED
                                        "PV" -> Episode.TYPE_PV
                                        "MAD" -> Episode.TYPE_MAD
                                        else -> Episode.TYPE_OTHER
                                    },
                                    name = ep.attr("title"),
                                    sort = ep.text().toFloatOrNull() ?: 0f,
                                    status = when {
                                        it.hasClass("epBtnToday") -> Episode.STATUS_TODAY
                                        it.hasClass("epBtnAir") || it.hasClass("epBtnWatched") -> Episode.STATUS_AIR
                                        else -> Episode.STATUS_NA
                                    },
                                    progress = when {
                                        it.hasClass("epBtnWatched") -> Episode.PROGRESS_WATCH
                                        it.hasClass("epBtnQueue") -> Episode.PROGRESS_QUEUE
                                        it.hasClass("epBtnDrop") -> Episode.PROGRESS_DROP
                                        else -> null
                                    }
                            )
                        },
                        eps_count = it.selectFirst(".prgBatchManagerForm .grey")?.text()?.trim(' ', '/')?.toIntOrNull()
                                ?: it.selectFirst("input[name=watchedeps]")?.parent()?.ownText()?.trim(' ', '/')?.toIntOrNull()
                                ?: 0,
                        vol_count = it.selectFirst("input[name=watched_vols]")?.parent()?.let {
                            it.ownText().trim(' ', '/').toIntOrNull() ?: -1
                        } ?: 0,
                        ep_status = it.selectFirst("input[name=watchedeps]")?.attr("value")?.toIntOrNull() ?: 0,
                        vol_status = it.selectFirst("input[name=watched_vols]")?.attr("value")?.toIntOrNull() ?: 0
                )
            }
        }
    }

    fun getCollectionSax(onNewSubject: (Subject) -> Unit): Call<List<Subject>> {
        return ApiHelper.buildHttpCall(SERVER) { rsp ->
            val ret = ArrayList<Subject>()

            val addSubject = addSubject@{ str: String ->
                val it = Jsoup.parse(str).selectFirst(".infoWrapper") ?: return@addSubject
                val data = it.selectFirst(".headerInner a.textTip") ?: return@addSubject
                val subject = Subject(
                        id = data.attr("data-subject-id")?.toIntOrNull() ?: return@addSubject,
                        type = Subject.parseType(it.attr("subject_type")?.toIntOrNull()),
                        name = data.attr("data-subject-name"),
                        name_cn = data.attr("data-subject-name-cn"),
                        images = Images(parseImageUrl(it.selectFirst("img"))),
                        eps = parseProgressList(it),
                        eps_count = it.selectFirst(".prgBatchManagerForm .grey")?.text()?.trim(' ', '/')?.toIntOrNull()
                                ?: it.selectFirst("input[name=watchedeps]")?.parent()?.ownText()?.trim(' ', '/')?.toIntOrNull()
                                ?: 0,
                        vol_count = it.selectFirst("input[name=watched_vols]")?.parent()?.let {
                            it.ownText().trim(' ', '/').toIntOrNull() ?: -1
                        } ?: 0,
                        ep_status = it.selectFirst("input[name=watchedeps]")?.attr("value")?.toIntOrNull() ?: 0,
                        vol_status = it.selectFirst("input[name=watched_vols]")?.attr("value")?.toIntOrNull() ?: 0)

                ret += subject
                onNewSubject(subject)
            }
            ApiHelper.parseWithSax(rsp) { parser, str ->
                when {
                    parser.eventType != XmlPullParser.START_TAG -> ApiHelper.SaxEventType.NOTHING
                    parser.getAttributeValue("", "id")?.startsWith("subjectPanel_") == true -> {
                        addSubject(str)
                        ApiHelper.SaxEventType.BEGIN
                    }
                    parser.getAttributeValue("", "id")?.contains("columnHomeB") == true -> {
                        addSubject(str)
                        ApiHelper.SaxEventType.END
                    }
                    else -> ApiHelper.SaxEventType.NOTHING
                }
            }
            ret
        }
    }

    /**
     * 主页的进度管理
     */
    fun getCollection(): Call<List<Subject>> {
        return ApiHelper.buildHttpCall(SERVER) {


            val doc = Jsoup.parse(it.body()?.string() ?: "")
            if (doc.selectFirst(".idBadgerNeue a.avatar") == null) throw Exception("no login")
            doc.select("#cloumnSubjectInfo .infoWrapper").mapNotNull {
                val data = it.selectFirst(".headerInner a.textTip") ?: return@mapNotNull null
                Subject(
                        id = data.attr("data-subject-id")?.toIntOrNull() ?: return@mapNotNull null,
                        type = Subject.parseType(it.attr("subject_type")?.toIntOrNull()),
                        name = data.attr("data-subject-name"),
                        name_cn = data.attr("data-subject-name-cn"),
                        images = Images(parseImageUrl(it.selectFirst("img"))),
                        eps = parseProgressList(it, doc),
                        eps_count = it.selectFirst(".prgBatchManagerForm .grey")?.text()?.trim(' ', '/')?.toIntOrNull()
                                ?: it.selectFirst("input[name=watchedeps]")?.parent()?.ownText()?.trim(' ', '/')?.toIntOrNull()
                                ?: 0,
                        vol_count = it.selectFirst("input[name=watched_vols]")?.parent()?.let {
                            it.ownText().trim(' ', '/').toIntOrNull() ?: -1
                        } ?: 0,
                        ep_status = it.selectFirst("input[name=watchedeps]")?.attr("value")?.toIntOrNull() ?: 0,
                        vol_status = it.selectFirst("input[name=watched_vols]")?.attr("value")?.toIntOrNull() ?: 0)
            }
        }
    }

    /**
     * 更新收藏
     */
    fun updateCollectionStatus(
            subject: Subject,
            newCollection: Collection): Call<Collection> {
        return ApiHelper.buildHttpCall("$SERVER/subject/${subject.id}/interest/update?gh=${HttpUtil.formhash}", body = FormBody.Builder()
                .add("referer", "ajax")
                .add("interest", newCollection.statusId.toString())
                .add("rating", newCollection.rating.toString())
                .add("tags", if (newCollection.tag?.isNotEmpty() == true) newCollection.tag.reduce { acc, s -> "$acc $s" } else "")
                .add("comment", newCollection.comment ?: "")
                .add("privacy", newCollection.private.toString())
                .add("update", "保存").build()) {
            newCollection
        }
    }

    /**
     * 删除收藏
     */
    fun removeCollection(
            subject: Subject
    ): Call<Boolean> {
        return ApiHelper.buildHttpCall("$SERVER/subject/${subject.id}/remove?gh=${HttpUtil.formhash}") {
            it.code() == 200
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
        return ApiHelper.buildHttpCall("$SERVER/subject/ep/$id/status/$status?gh=${HttpUtil.formhash}&ajax=1", body = FormBody.Builder()
                .add("ep_id", epIds ?: id.toString()).build()) {
            it.body()?.string()?.contains("\"status\":\"ok\"") == true
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
        return ApiHelper.buildHttpCall("$SERVER/subject/set/watched/${subject.id}", body = body.build()) {
            it.code() == 200
        }
    }

    /**
     * 时间线吐槽
     */
    fun addTimeLineComment(
            say_input: String
    ): Call<Boolean> {
        return ApiHelper.buildHttpCall("$SERVER/update/user/say?ajax=1", body = FormBody.Builder()
                .add("say_input", say_input)
                .add("formhash", HttpUtil.formhash)
                .add("submit", "submit").build()) {
            it.body()?.string()?.contains("\"status\":\"ok\"") == true
        }
    }

    /**
     * 注销
     */
    fun logout(): Call<Int> {
        return ApiHelper.buildHttpCall("$SERVER/logout/${HttpUtil.formhash}") {
            val cookieManager = CookieManager.getInstance()
            it.headers("set-cookie").forEach { cookieManager.setCookie(SERVER, it) }
            it.code()
        }
    }

    /**
     * 删除帖子
     * TODO 时间线
     */
    fun removeTopic(
            topic: Topic
    ): Call<Boolean> {
        return ApiHelper.buildHttpCall(topic.post.replace(SERVER, "$SERVER/erase").replace("/new_reply", "?gh=${HttpUtil.formhash}&ajax=1")) {
            it.code() == 200
        }
    }

    /**
     * 删除帖子回复
     * TODO 时间线
     */
    fun removeTopicReply(
            post: TopicPost
    ): Call<Boolean> {
        return ApiHelper.buildHttpCall(SERVER + when (post.model) {
            "group" -> "/erase/group/reply/"
            "prsn" -> "/erase/reply/person/"
            "crt" -> "/erase/reply/character/"
            "ep" -> "/erase/reply/ep/"
            "subject" -> "/erase/subject/reply/"
            else -> ""
        } + "${post.pst_id}?gh=${HttpUtil.formhash}&ajax=1") {
            it.body()?.string()?.contains("\"status\":\"ok\"") == true
        }
    }

    /**
     * 编辑帖子回复
     * TODO 时间线
     */
    fun editTopicReply(
            topic: Topic,
            post: TopicPost,
            content: String
    ): Call<Boolean> {
        return ApiHelper.buildHttpCall(if (post.floor == 1)
            topic.post.replace("/new_reply", "/edit")
        else SERVER + when (post.model) {
            "group" -> "/group/reply/${post.pst_id}/edit"
            "prsn" -> "/person/edit_reply/${post.pst_id}"
            "crt" -> "/character/edit_reply/${post.pst_id}"
            "ep" -> "/subject/ep/edit_reply/${post.pst_id}"
            "subject" -> "/subject/reply/${post.pst_id}/edit"
            else -> ""
        }, body = FormBody.Builder()
                .add("formhash", HttpUtil.formhash)
                .add("title", topic.title)
                .add("submit", "改好了")
                .add("content", content).build()) {
            it.code() == 200
        }
    }

    /**
     * 回复帖子
     * TODO 时间线
     */
    fun replyTopic(
            topic: Topic,
            post: TopicPost?,
            content: String
    ): Call<List<TopicPost>> {
        val comment = if (post?.isSub == true)
            "[quote][b]${post.nickname}[/b] 说: ${Jsoup.parse(post.pst_content).let { doc ->
                doc.select("div.quote").remove()
                TextUtil.html2text(doc.html()).let {
                    if (it.length > 100) it.substring(0, 100) + "..." else it
                }
            }}[/quote]\n" else ""

        val data = FormBody.Builder()
                .add("lastview", topic.lastview!!)
                .add("formhash", HttpUtil.formhash)
                .add("content", comment + content)
                .add("submit", "submit")
        if (post != null) {
            data.add("topic_id", post.pst_mid)
                    .add("related", post.relate)
                    .add("post_uid", post.pst_uid)
        }

        return ApiHelper.buildHttpCall(topic.post, body = data.build()) { rsp ->
            val replies = ArrayList(topic.replies)
            replies.removeAll { it.sub_floor > 0 }
            replies.sortedBy { it.floor }
            val posts = JsonUtil.toJsonObject(rsp.body()?.string() ?: "").getAsJsonObject("posts")
            val main = JsonUtil.toEntity<Map<String, TopicPost>>(posts.get("main")?.toString()
                    ?: "", object : TypeToken<Map<String, TopicPost>>() {}.type) ?: HashMap()
            main.forEach {
                it.value.floor = (replies.last()?.floor ?: 0) + 1
                it.value.relate = it.key
                it.value.isExpanded = replies.firstOrNull { o -> o.pst_id == it.value.pst_id }?.isExpanded ?: true
                replies.removeAll { o -> o.pst_id == it.value.pst_id }
                replies.add(it.value)
            }
            replies.toTypedArray().forEach { replies.addAll(it.subItems ?: return@forEach) }
            replies.sortedBy { it.floor + it.sub_floor * 1.0f / replies.size }
            val sub = JsonUtil.toEntity<Map<String, List<TopicPost>>>(posts.get("sub")?.toString()
                    ?: "", object : TypeToken<Map<String, List<TopicPost>>>() {}.type) ?: HashMap()
            sub.forEach {
                replies.lastOrNull { old -> old.pst_id == it.key }?.isExpanded = true
                var relate = replies.lastOrNull { old -> old.relate == it.key } ?: return@forEach
                it.value.forEach { topicPost ->
                    topicPost.isSub = true
                    topicPost.floor = relate.floor
                    topicPost.sub_floor = relate.sub_floor + 1
                    topicPost.editable = topicPost.is_self
                    topicPost.relate = relate.relate
                    replies.removeAll { o -> o.pst_id == topicPost.pst_id }
                    replies.add(topicPost)
                    relate = topicPost
                }
            }
            replies.sortedBy { it.floor + it.sub_floor * 1.0f / replies.size }
        }
    }


}