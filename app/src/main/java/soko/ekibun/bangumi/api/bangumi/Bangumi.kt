package soko.ekibun.bangumi.api.bangumi

import android.util.Log
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.*
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.util.HttpUtil
import java.net.URI

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
    suspend fun getCollectionList(
        @Subject.SubjectType subject_type: String,
        username: String,
        @Collection.CollectionStatus collection_status: String,
        page: Int = 1
    ): List<Subject> {
        return withContext(Dispatchers.Default) {
            Jsoup.parse(withContext(Dispatchers.IO) {
                HttpUtil.getCall(
                    "$SERVER/$subject_type/list/$username/$collection_status?page=$page"
                ).execute().body?.string() ?: ""
            }).select(".item").mapNotNull { item ->
                val id = item.attr("id").split('_').getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                val nameCN = item.selectFirst("h3 a")?.text()
                val name = item.selectFirst("h3 small")?.text() ?: nameCN
                Subject(
                    id = id,
                    type = Subject.TYPE_ANY,
                    name = name,
                    name_cn = nameCN,
                    summary = item.selectFirst(".info")?.text(),
                    image = parseImageUrl(item.selectFirst("img")),
                    ep_status = -1
                )
            }
        }
    }

    /**
     * 条目搜索
     * @param keywords String
     * @param type String
     * @param page Int
     * @return Call<List<Subject>>
     */
    suspend fun searchSubject(
        keywords: String,
        @Subject.SubjectType type: String = Subject.TYPE_ANY,
        page: Int
    ): List<Subject> {
        CookieManager.getInstance()
            .setCookie(COOKIE_HOST, "chii_searchDateLine=${System.currentTimeMillis() / 1000 - 10};")
        return withContext(Dispatchers.Default) {
            val doc = Jsoup.parse(withContext(Dispatchers.IO) {
                HttpUtil.getCall(
                    "$SERVER/subject_search/${java.net.URLEncoder.encode(
                        keywords,
                        "utf-8"
                    )}?cat=${Subject.parseTypeInt(type)}&page=$page"
                ).execute().body?.string() ?: ""
            })
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
    suspend fun searchMono(
        keywords: String,
        type: String,
        page: Int
    ): List<MonoInfo> {
        CookieManager.getInstance()
            .setCookie(COOKIE_HOST, "chii_searchDateLine=${System.currentTimeMillis() / 1000 - 10};")
        return withContext(Dispatchers.Default) {
            val doc = Jsoup.parse(withContext(Dispatchers.IO) {
                HttpUtil.getCall(
                    "$SERVER/mono_search/${java.net.URLEncoder.encode(keywords, "utf-8")}?cat=$type&page=$page"
                ).execute().body?.string() ?: ""
            })
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
    suspend fun browserAirTime(
        @Subject.SubjectType subject_type: String,
        year: Int,
        month: Int,
        page: Int,
        sub_cat: String
    ): List<Subject> {
        return withContext(Dispatchers.Default) {
            val doc = Jsoup.parse(withContext(Dispatchers.IO) {
                HttpUtil.getCall(
                    "$SERVER/$subject_type/browser${if (sub_cat.isEmpty()) "" else "/$sub_cat"}/airtime/$year-$month?page=$page"
                ).execute().body?.string() ?: ""
            })
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
     * @return Observable<Pair<Int, Int>|UserInfo|List<Subject>>
     */
    suspend fun getCollectionSax(
        onUser: (UserInfo) -> Unit,
        onNotify: (Pair<Int, Int>) -> Unit
    ): List<Subject> {
        return withContext(Dispatchers.Default) {
            val cookieManager = CookieManager.getInstance()
            val rsp = withContext(Dispatchers.IO) { HttpUtil.getCall(SERVER).execute() }
            var subjectLoaded = false
            val ret = ArrayList<Subject>()
            ApiHelper.parseSaxAsync(rsp, { _, attrs ->
                when {
                    attrs.contains("id=\"columnHomeA\"") -> {
                        "user" to ApiHelper.SaxEventType.BEGIN
                    }
                    attrs.contains("id=\"home_") && !subjectLoaded -> {
                        subjectLoaded = true
                        "subject" to ApiHelper.SaxEventType.BEGIN
                    }
                    attrs.contains("id=\"subject_prg_content\"") -> {
                        "notify" to ApiHelper.SaxEventType.END
                    }
                    else -> null to ApiHelper.SaxEventType.NOTHING
                }
            }) { tag, str ->
                when (tag) {
                    "user" -> {
                        val doc = Jsoup.parse(str)
                        val user = doc.selectFirst(".idBadgerNeue a.avatar") ?: throw Exception("login failed")
                        val username = UserInfo.getUserName(user.attr("href"))
                        rsp.headers("set-cookie").forEach {
                            cookieManager.setCookie(COOKIE_HOST, it)
                        }
                        cookieManager.flush()
                        HttpUtil.formhash = Regex("""//bgm.tv/logout/([^"]+)""").find(str)?.groupValues?.getOrNull(1)
                            ?: HttpUtil.formhash
                        withContext(Dispatchers.Main) {
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
                        }
                    }
                    "subject" -> {
                        ret += Jsoup.parse(str).select(".infoWrapper").mapNotNull {
                            Subject.parseChaseCollection(it)
                        }
                    }
                    "notify" -> {
                        val doc = Jsoup.parse(str)
                        withContext(Dispatchers.Main) {
                            onNotify(
                                Pair(
                                    Regex("叮咚叮咚～你有 ([0-9]+) 条新信息!").find(
                                        doc.selectFirst("#robot_speech_js")?.text()
                                            ?: ""
                                    )?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                    doc.selectFirst("#notify_count")?.text()?.toIntOrNull() ?: 0
                                )
                            )
                        }
                    }
                }
            }
            ret
        }
    }

    /**
     * 上传图片
     * @param requestBody RequestBody
     * @param fileName String
     * @return Call<String>
     */
    suspend fun uploadImage(requestBody: RequestBody, fileName: String): String {
        return withContext(Dispatchers.IO) {
            val sid = Regex("""CHOBITS_SID = '(.*?)';""").find(
                HttpUtil.getCall("$SERVER/blog/create").execute().body!!.string()
            )!!.groupValues[1]
            Log.v("sid", sid)
            Images.large(
                Jsoup.parse(
                    HttpUtil.getCall(
                        "$SERVER/blog/upload_photo?folder=/blog/files&sid=$sid",
                        body = MultipartBody.Builder().setType(MultipartBody.FORM)
                            .addFormDataPart("Filename", fileName)
                            .addFormDataPart("Filedata", fileName, requestBody)
                            .addFormDataPart("Upload", "Submit Query")
                            .build()
                    ).execute().body!!.string()
                ).selectFirst("img").attr("src")
            )
        }
    }
}