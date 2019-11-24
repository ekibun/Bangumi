package soko.ekibun.bangumi.api.bangumi

import android.webkit.CookieManager
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.xmlpull.v1.XmlPullParser
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.MonoInfo
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.HttpUtil
import java.net.URI
import java.util.*

/**
 * Bangumi API
 */
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
        return parseUrl(when {
            cover?.hasAttr("src") == true -> cover.attr("src") ?: ""
            cover?.hasAttr("data-cfsrc") == true -> cover.attr("data-cfsrc") ?: ""
            else -> Regex("""background-image:url\('([^']*)'\)""").find(cover?.attr("style") ?: "")?.groupValues?.get(1)
                    ?: ""
        })
    }

    /**
     * 获取用户收藏列表
     */
    fun getCollectionList(
            @Subject.SubjectType subject_type: String,
            username: String,
            @Collection.CollectionStatus collection_status: String,
            page: Int = 1
    ): Call<List<Subject>> {
        return ApiHelper.buildHttpCall("$SERVER/$subject_type/list/$username/$collection_status?page=$page") {
            val doc = Jsoup.parse(it.body?.string() ?: "")
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
                        image = parseImageUrl(item.selectFirst("img")),
                        ep_status = -1
                )
            }
            ret
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
        CookieManager.getInstance().setCookie(SERVER, "chii_searchDateLine=${System.currentTimeMillis() / 1000 - 10};")
        return ApiHelper.buildHttpCall("$SERVER/subject_search/${java.net.URLEncoder.encode(keywords, "utf-8")}?cat=${Subject.parseTypeInt(type)}&page=$page") { rsp ->
            val doc = Jsoup.parse(rsp.body?.string() ?: "")
            if (doc.select("#colunmNotice") == null) throw Exception("search error")
            doc.select(".item").mapNotNull { item ->
                val nameCN = item.selectFirst("h3")?.selectFirst("a")?.text()
                Subject(
                        id = item.attr("id").split('_').last().toIntOrNull() ?: return@mapNotNull null,
                        type = Subject.parseType(item.selectFirst(".ico_subject_type")?.classNames()?.mapNotNull { it.split('_').last().toIntOrNull() }?.firstOrNull()),
                        name = item.selectFirst("h3")?.selectFirst("small")?.text() ?: nameCN,
                        name_cn = nameCN,
                        summary = item.selectFirst(".info")?.text(),
                        image = parseImageUrl(item.selectFirst("img")),
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
        CookieManager.getInstance().setCookie(SERVER, "chii_searchDateLine=${System.currentTimeMillis() / 1000 - 10};")
        return ApiHelper.buildHttpCall("$SERVER/mono_search/${java.net.URLEncoder.encode(keywords, "utf-8")}?cat=$type&page=$page") { rsp ->
            val doc = Jsoup.parse(rsp.body?.string() ?: "")
            if (doc.select("#colunmNotice") == null) throw Exception("search error")
            doc.select(".light_odd").map {
                val a = it.selectFirst("h2 a")
                MonoInfo(
                        name = a?.ownText()?.trim('/', ' '),
                        name_cn = a?.selectFirst("span.tip")?.text(),
                        image = parseImageUrl(it.selectFirst("img")),
                        summary = it.selectFirst(".prsn_info")?.text(),
                        url = parseUrl(a?.attr("href") ?: ""))
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
        return ApiHelper.buildHttpCall("$SERVER/$subject_type/browser${if (sub_cat.isEmpty()) "" else "/$sub_cat"}/airtime/$year-$month?page=$page") { rsp ->
            val doc = Jsoup.parse(rsp.body?.string() ?: "")
            doc.select(".item").mapNotNull {
                val nameCN = it.selectFirst("h3 a")?.text()
                Subject(
                        id = it.attr("id").split('_').last().toIntOrNull() ?: return@mapNotNull null,
                        name = it.selectFirst("h3 small")?.text() ?: nameCN,
                        name_cn = nameCN,
                        summary = it.selectFirst(".info")?.text(),
                        image = parseImageUrl(it.selectFirst("img")),
                        collect = if (it.selectFirst(".collectBlock")?.text()?.contains("修改") == true) Collection() else null)
            }
        }
    }

    /**
     * 进度管理
     */
    fun getCollectionSax(onNewSubject: (Subject) -> Unit = {}): Call<List<Subject>> {
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
                        image = parseImageUrl(it.selectFirst("img")),
                        eps = Episode.parseProgressList(it),
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
     * 注销
     */
    fun logout(): Call<Int> {
        return ApiHelper.buildHttpCall("$SERVER/logout/${HttpUtil.formhash}") {
            val cookieManager = CookieManager.getInstance()
            it.headers("set-cookie").forEach { cookieManager.setCookie(SERVER, it) }
            it.code
        }
    }
}