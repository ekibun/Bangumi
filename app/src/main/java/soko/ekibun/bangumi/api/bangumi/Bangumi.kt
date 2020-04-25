package soko.ekibun.bangumi.api.bangumi

import android.webkit.CookieManager
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.xmlpull.v1.XmlPullParser
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.ApiHelper.subscribeOnUiThread
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.MonoInfo
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.UserInfo
import soko.ekibun.bangumi.util.HttpUtil
import java.net.URI
import java.util.*

/**
 * Bangumi API
 */
object Bangumi {
    const val SERVER = "https://bgm.tv"
    const val COOKIE_HOST = ".bgm.tv"

    /**
     * 获取完整的URL
     * @param url String
     * @return String
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
     * @param cover Element?
     * @return String
     */
    fun parseImageUrl(cover: Element?): String {
        return parseUrl(
            when {
                cover?.hasAttr("src") == true -> cover.attr("src") ?: ""
                cover?.hasAttr("data-cfsrc") == true -> cover.attr("data-cfsrc") ?: ""
                else -> Regex("""background-image:url\('([^']*)'\)""").find(
                    cover?.attr("style") ?: ""
                )?.groupValues?.get(1)
                    ?: ""
            }
        )
    }

    /**
     * 获取用户收藏列表
     * @param subject_type String
     * @param username String
     * @param collection_status String
     * @param page Int
     * @return Call<List<Subject>>
     */
    fun getCollectionList(
        @Subject.SubjectType subject_type: String,
        username: String,
        @Collection.CollectionStatus collection_status: String,
        page: Int = 1
    ): Observable<List<Subject>> {
        return ApiHelper.createHttpObservable("$SERVER/$subject_type/list/$username/$collection_status?page=$page")
            .subscribeOn(Schedulers.computation()).map { rsp ->
                val doc = Jsoup.parse(rsp.body?.string() ?: "")
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
     * @param keywords String
     * @param type String
     * @param page Int
     * @return Call<List<Subject>>
     */
    fun searchSubject(
        keywords: String,
        @Subject.SubjectType type: String = Subject.TYPE_ANY,
        page: Int
    ): Observable<List<Subject>> {
        CookieManager.getInstance()
            .setCookie(COOKIE_HOST, "chii_searchDateLine=${System.currentTimeMillis() / 1000 - 10};")
        return ApiHelper.createHttpObservable(
            "$SERVER/subject_search/${java.net.URLEncoder.encode(
                keywords,
                "utf-8"
            )}?cat=${Subject.parseTypeInt(type)}&page=$page"
        )
            .subscribeOn(Schedulers.computation()).map { rsp ->
                val doc = Jsoup.parse(rsp.body?.string() ?: "")
                if (doc.select("#colunmNotice") == null) throw error("search error")
                doc.select(".item").mapNotNull { item ->
                    val nameCN = item.selectFirst("h3")?.selectFirst("a")?.text()
                    Subject(
                        id = item.attr("id").split('_').last().toIntOrNull() ?: return@mapNotNull null,
                        type = Subject.parseType(item.selectFirst(".ico_subject_type")?.classNames()?.mapNotNull {
                            it.split('_').last().toIntOrNull()
                        }?.firstOrNull()),
                        name = item.selectFirst("h3")?.selectFirst("small")?.text() ?: nameCN,
                        name_cn = nameCN,
                        summary = item.selectFirst(".info")?.text(),
                        image = parseImageUrl(item.selectFirst("img")),
                        collect = if (item.selectFirst(".collectBlock")?.text()
                                ?.contains("修改") == true
                        ) Collection() else null
                    )
                }
            }
    }

    /**
     * 人物搜索
     * @param keywords String
     * @param type String
     * @param page Int
     * @return Call<List<MonoInfo>>
     */
    fun searchMono(
        keywords: String,
        type: String,
        page: Int
    ): Observable<List<MonoInfo>> {
        CookieManager.getInstance()
            .setCookie(COOKIE_HOST, "chii_searchDateLine=${System.currentTimeMillis() / 1000 - 10};")
        return ApiHelper.createHttpObservable(
            "$SERVER/mono_search/${java.net.URLEncoder.encode(
                keywords,
                "utf-8"
            )}?cat=$type&page=$page"
        )
            .subscribeOn(Schedulers.computation()).map { rsp ->
                val doc = Jsoup.parse(rsp.body?.string() ?: "")
                if (doc.select("#colunmNotice") == null) throw Exception("search error")
                doc.select(".light_odd").map {
                    val a = it.selectFirst("h2 a")
                    MonoInfo(
                        name = a?.ownText()?.trim('/', ' '),
                        name_cn = a?.selectFirst("span.tip")?.text(),
                        image = parseImageUrl(it.selectFirst("img")),
                        summary = it.selectFirst(".prsn_info")?.text(),
                        url = parseUrl(a?.attr("href") ?: "")
                    )
                }
            }
    }

    /**
     * 索引
     * @param subject_type String
     * @param year Int
     * @param month Int
     * @param page Int
     * @param sub_cat String
     * @return Call<List<Subject>>
     */
    fun browserAirTime(
        @Subject.SubjectType subject_type: String,
        year: Int,
        month: Int,
        page: Int,
        sub_cat: String
    ): Observable<List<Subject>> {
        return ApiHelper.createHttpObservable("$SERVER/$subject_type/browser${if (sub_cat.isEmpty()) "" else "/$sub_cat"}/airtime/$year-$month?page=$page")
            .subscribeOn(Schedulers.computation()).map { rsp ->
                val doc = Jsoup.parse(rsp.body?.string() ?: "")
                doc.select(".item").mapNotNull {
                    val nameCN = it.selectFirst("h3 a")?.text()
                    Subject(
                        id = it.attr("id").split('_').last().toIntOrNull() ?: return@mapNotNull null,
                        name = it.selectFirst("h3 small")?.text() ?: nameCN,
                        name_cn = nameCN,
                        summary = it.selectFirst(".info")?.text(),
                        image = parseImageUrl(it.selectFirst("img")),
                        collect = if (it.selectFirst(".collectBlock")?.text()
                                ?.contains("修改") == true
                        ) Collection() else null
                    )
                }
            }
    }

    /**
     * 进度管理 + 用户信息
     * @param onUser Function1<UserInfo, Unit>
     * @param onNotify Function1<Pair<Int, Int>, Unit>
     * @return Call<List<Subject>>
     */
    fun getCollectionSax(
        onUser: (UserInfo) -> Unit = {},
        onNotify: (Pair<Int, Int>) -> Unit = {}
    ): Observable<List<Subject>> {
        val cookieManager = CookieManager.getInstance()

        return ApiHelper.createHttpObservable(SERVER).map { rsp ->
            val ret = ArrayList<Subject>()
            var subjectLoaded = false
            ApiHelper.parseWithSax(rsp) { parser, str ->
                when {
                    parser.eventType != XmlPullParser.START_TAG -> ApiHelper.SaxEventType.NOTHING
                    parser.getAttributeValue("", "id")?.contains("columnHomeA") == true -> {
                        val s = str()
                        val doc = Jsoup.parse(s)
                        val user = doc.selectFirst(".idBadgerNeue a.avatar") ?: throw Exception("login failed")
                        val username = UserInfo.getUserName(user.attr("href"))
                        rsp.headers("set-cookie").forEach {
                            cookieManager.setCookie(COOKIE_HOST, it)
                        }
                        cookieManager.flush()
                        HttpUtil.formhash = Regex("""//bgm.tv/logout/([^"]+)""").find(s)?.groupValues?.getOrNull(1)
                            ?: HttpUtil.formhash
                        onUser(
                            UserInfo(
                                id = Regex("""CHOBITS_UID = (\d+)""").find(
                                    doc.select("script").html()
                                )?.groupValues?.get(1)?.toIntOrNull() ?: username?.toIntOrNull() ?: 0,
                                username = username,
                                nickname = doc.selectFirst("#header a")?.text(),
                                avatar = parseImageUrl(user.selectFirst("span.avatarNeue")),
                                sign = doc.selectFirst("input[name=sign_input]")?.attr("value")
                            )
                        )
                        ApiHelper.SaxEventType.BEGIN
                    }
                    parser.getAttributeValue("", "id")?.startsWith("subjectPanel_") == true -> {
                        Subject.parseChaseCollection(str())?.let { ret += it }
                        ApiHelper.SaxEventType.BEGIN
                    }
                    parser.getAttributeValue("", "id")?.startsWith("home_") == true && !subjectLoaded -> {
                        subjectLoaded = true
                        Subject.parseChaseCollection(str())?.let { ret += it }
                        ApiHelper.SaxEventType.BEGIN
                    }
                    parser.getAttributeValue("", "id")?.contains("subject_prg_content") == true -> {
                        val doc = Jsoup.parse(str())
                        Observable.just(
                            Pair(
                                Regex("叮咚叮咚～你有 ([0-9]+) 条新信息!").find(
                                    doc.selectFirst("#robot_speech_js")?.text()
                                        ?: ""
                                )?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                doc.selectFirst("#notify_count")?.text()?.toIntOrNull() ?: 0
                            )
                        ).subscribeOnUiThread({
                            onNotify(it)
                        })
                        ApiHelper.SaxEventType.END
                    }
                    else -> ApiHelper.SaxEventType.NOTHING
                }
            }
            ret
        }
    }
}