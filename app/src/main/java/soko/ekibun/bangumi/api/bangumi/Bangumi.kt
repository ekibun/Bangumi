package soko.ekibun.bangumi.api.bangumi

import android.annotation.SuppressLint
import android.webkit.CookieManager
import okhttp3.FormBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.*
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.util.HttpUtil
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

interface Bangumi {

    @GET("/user/{username}/collection")
    fun collection(@Path("username") username: String,
                   @Query("cat") cat: String = "all_watching"
    ): Call<List<SubjectCollection>>

    companion object {
        const val SERVER = "https://bgm.tv"
        private const val SERVER_API = "https://api.bgm.tv"
        fun createInstance(): Bangumi{
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(Bangumi::class.java)
        }

        fun getCollectionList(@SubjectType.SubjectTypeName subject_type: String,
                              username: String, ua: String,
                              @CollectionStatusType.CollectionStatusType collection_status: String,
                              page: Int = 1
        ): Call<List<SubjectCollection>>{
            return ApiHelper.buildHttpCall("$SERVER/$subject_type/list/$username/$collection_status?page=$page", mapOf("User-Agent" to ua)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<SubjectCollection>()
                doc.select(".item").forEach {
                    it.attr("id").split('_').getOrNull(1)?.toIntOrNull()?.let{id->
                        val nameCN = it.selectFirst("h3")?.selectFirst("a")?.text()
                        val name = it.selectFirst("h3")?.selectFirst("small")?.text()?:nameCN
                        val img = getImageUrl(it.selectFirst("img"))
                        val info = it.selectFirst(".info")?.text()
                        val subject = Subject(id,
                                HttpUtil.getUrl(it.selectFirst("a")?.attr("href")?:"", URI.create(Bangumi.SERVER)),
                                0,
                                name,
                                nameCN,
                                info,
                                images = Images(img.replace("/s/", "/l/"),
                                        img.replace("/s/", "/c/"),
                                        img.replace("/s/", "/m/"), img,
                                        img.replace("/s/", "/g/"))
                        )
                        ret += SubjectCollection(name, id, -1, -1, subject = subject)
                    }
                }
                return@buildHttpCall ret
            }
        }

        private fun getImageUrl(cover: Element?): String{
            return HttpUtil.getUrl(if(cover?.hasAttr("src") == true) cover.attr("src")?:"" else cover?.attr("data-cfsrc")?:"", URI.create(Bangumi.SERVER))
        }

        @SuppressLint("UseSparseArrays")
        fun getSubject(subject: Subject, ua: String): Call<Subject>{
            return ApiHelper.buildHttpCall(subject.url?:"", mapOf("User-Agent" to ua)){ response ->
                val doc = Jsoup.parse(response.body()?.string()?:"")
                val type = when(doc.selectFirst("#navMenuNeue .focus").text()){
                    "动画" -> SubjectType.ANIME
                    "书籍" -> SubjectType.BOOK
                    "音乐" -> SubjectType.MUSIC
                    "游戏" -> SubjectType.GAME
                    "三次元" -> SubjectType.REAL
                    else -> SubjectType.ALL
                }
                //name
                val name = doc.selectFirst(".nameSingle> a")?.text()?:subject.name
                val name_cn = doc.selectFirst(".nameSingle> a")?.attr("title")?:subject.name_cn
                //summary
                val summary = Jsoup.parse(doc.selectFirst("#subject_summary")?.html()?.replace("<br>", "$$$$$")?:"")?.text()?.replace("$$$$$", "\n")?:subject.summary

                val infobox = doc.select("#infobox li")?.map{
                    val tip = it.selectFirst("span.tip")?.text()?:""
                    var value = ""
                    it.childNodes()?.forEach { if(it !is Element || !it.hasClass("tip")) value += it.outerHtml() }
                    Pair(tip.trim(':',' '), value.trim())
                }
                var eps_count = infobox?.firstOrNull { it.first == "话数" }?.second?.toIntOrNull()?:subject.eps_count
                //air-date
                val air_date = infobox?.firstOrNull { it.first in arrayOf("放送开始", "上映年度", "开始") }?.second?.replace("/", "-")?.
                        replace("年", "-")?.replace("月", "-")?.replace("日", "")?:""
                var air_weekday = "一二三四五六日".map { "星期$it" }.indexOf(infobox?.firstOrNull { it.first == "放送星期" }?.second?:"") + 1
                if(air_weekday == 0)
                    air_weekday = "月火水木金土日".map { "${it}曜日" }.indexOf(infobox?.firstOrNull { it.first == "放送星期" }?.second?:"") + 1

                val counts = HashMap<Int, Int>()
                doc.select(".horizontalChart li")?.forEach {
                    counts[it.selectFirst(".label")?.text()?.toIntOrNull()?:0] =
                            it.selectFirst(".count").text()?.trim('(',')')?.toIntOrNull()?:0
                }

                val rating = Subject.RatingBean(
                    doc.selectFirst("span[property=\"v:votes\"]")?.text()?.toIntOrNull()?:subject.rating?.total?:0,
                        Subject.RatingBean.CountBean(counts[10]?:0, counts[9]?:0, counts[8]?:0, counts[7]?:0,
                                counts[6]?:0, counts[5]?:0, counts[4]?:0, counts[3]?:0, counts[2]?:0, counts[1]?:0),
                        doc.selectFirst(".global_score .number")?.text()?.toDoubleOrNull()?:subject.rating?.score?:0.0
                )
                val rank = doc.selectFirst(".global_score .alarm")?.text()?.trim('#')?.toIntOrNull()?:subject.rank
                val img = getImageUrl(doc.selectFirst(".infobox img.cover"))
                val images = Images(
                        img.replace("/c/", "/l/"),img,
                        img.replace("/c/", "/m/"),
                        img.replace("/c/", "/s/"),
                        img.replace("/c/", "/g/"))
                //TODO Collection
                //prg
                val ep_status = doc.selectFirst("input[name=watchedeps]")?.attr("value")?.toIntOrNull()?:0
                val vol_status = doc.selectFirst("input[name=watched_vols]")?.attr("value")?.toIntOrNull()?:0
                var vol_count = 0
                var has_vol = false
                doc.select(".prgText")?.forEach {
                    when(it.selectFirst(".type")?.text()){
                        "Vol." -> {
                            has_vol = true
                            vol_count = it.ownText()?.trim(' ', '/')?.toIntOrNull()?:0
                        }
                        else -> eps_count = it.ownText()?.trim(' ', '/')?.toIntOrNull()?:0
                    }
                }
                //crt
                val crt = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "角色介绍" }.getOrNull(0)?.select("li")?.map {
                    val a = it.selectFirst("a.avatar")
                    val crt_img = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(a?.html()?:"")?.groupValues?.get(1)
                            ?: "", URI.create(Bangumi.SERVER))
                    val stars = it.select("a[rel=\"v:starring\"]").map { psn ->
                        Person(Regex("""/person/([0-9]*)""").find(psn.attr("href")
                                ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                HttpUtil.getUrl(psn.attr("href") ?: "", URI.create(Bangumi.SERVER)),
                                psn.text() ?: "")
                    }
                    Character(Regex("""/character/([0-9]*)""").find(a?.attr("href")?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                            HttpUtil.getUrl(a?.attr("href")?: "", URI.create(Bangumi.SERVER)),
                            a?.text() ?: "",
                            it.selectFirst(".info .tip")?.text() ?: "",
                            it.selectFirst(".info .badge_job_tip")?.text() ?: "",
                            Images(crt_img.replace("/s/", "/l/"),
                                    crt_img.replace("/s/", "/c/"),
                                    crt_img.replace("/s/", "/m/"), crt_img,
                                    crt_img.replace("/s/", "/g/")),
                            it.selectFirst("small.fade")?.text()?.trim('(', '+', ')')?.toIntOrNull() ?: 0,
                            actors = stars)
                }
                //TODO staff
                //topic
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val topic = doc.select(".topic_list tr")?.map {
                    val tds = it.select("td")
                    val td0 = tds?.get(0)?.selectFirst("a")
                    val topic_id = Regex("""/topic/([0-9]*)""").find(td0?.attr("href")?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val user = tds?.get(1)?.selectFirst("a")
                    val user_id = Regex("""/user/([^/]*)""").find(user?.attr("href")?:"")?.groupValues?.get(1)
                    val time  = try{
                        dateFormat.parse(tds?.get(3)?.text()?:"").time /1000
                    }catch (e: Exception){ 0L }
                    if(td0?.attr("href").isNullOrEmpty()) null else
                    Subject.TopicBean(topic_id,
                            HttpUtil.getUrl(td0?.attr("href")?: "", URI.create(Bangumi.SERVER)),
                            td0?.text() ?: "", topic_id, time, 0,
                            Regex("""([0-9]*)""").find(tds?.get(2)?.text()?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                            UserInfo(user_id?.toIntOrNull()?:0, HttpUtil.getUrl(user?.attr("href")?:"", URI.create(SERVER)), user_id, user?.text())
                    )
                }?.filterNotNull()
                //blog
                val datetimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val blog = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "评论" }.getOrNull(0)?.select("div.item")?.map {
                    val a = it.selectFirst(".title a")
                    val user = it.selectFirst(".tip_j a")
                    val user_id = Regex("""/user/([^/]*)""").find(user?.attr("href")?:"")?.groupValues?.get(1)
                    val time  = try{
                        datetimeFormat.parse(it.selectFirst("small.time")?.text()?:"").time /1000
                    }catch (e: Exception){
                        e.printStackTrace()
                        0L }
                    Subject.BlogBean(Regex("""/blog/([0-9]*)""").find(a?.attr("href")?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                            HttpUtil.getUrl(a?.attr("href")?: "", URI.create(Bangumi.SERVER)),
                            a?.text() ?: "",
                            it.selectFirst(".content")?.ownText()?:"",
                            getImageUrl(it.selectFirst("img")),
                            it.selectFirst("small.orange")?.text()?.trim('(', '+', ')')?.toIntOrNull() ?: 0, time,
                            it.selectFirst("small.time")?.text()?:"",
                            UserInfo(user_id?.toIntOrNull()?:0, HttpUtil.getUrl(user?.attr("href")?:"", URI.create(SERVER)), user_id, user?.text())
                    )
                }
                //linked
                val tankobon = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "单行本" }.getOrNull(0)?.select("li")?.map {
                    val avatar = it.selectFirst(".avatar")
                    val subjectImg = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(avatar?.html()?:"")?.groupValues?.get(1)?:"", URI.create(Bangumi.SERVER))
                    val title = avatar?.attr("title")?.split("/ ")
                    val url = HttpUtil.getUrl(avatar?.attr("href")?:"", URI.create(Bangumi.SERVER))
                    val id = Regex("""/subject/([0-9]*)""").find(url)?.groupValues?.get(1)?.toIntOrNull()?:0
                    Subject(id, url, 0, title?.getOrNull(0), title?.getOrNull(1), typeString = "单行本",
                            images = Images(subjectImg.replace("/g/", "/l/"),
                                    subjectImg.replace("/g/", "/m/"),
                                    subjectImg.replace("/g/", "/c/"),
                                    subjectImg.replace("/g/", "/s/"), subjectImg))
                }
                var sub = ""
                val linked = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "关联条目" }.getOrNull(0)?.select("li")?.map {
                    val newSub = it.selectFirst(".sub").text()
                    if(!newSub.isNullOrEmpty()) sub = newSub
                    val avatar = it.selectFirst(".avatar")
                    val subjectImg = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(avatar?.html()?:"")?.groupValues?.get(1)?:"", URI.create(Bangumi.SERVER))
                    val title = it.selectFirst(".title")
                    val url = HttpUtil.getUrl(title?.attr("href")?:"", URI.create(Bangumi.SERVER))
                    val id = Regex("""/subject/([0-9]*)""").find(url)?.groupValues?.get(1)?.toIntOrNull()?:0
                    if(tankobon?.firstOrNull { b -> b.id == id } == null)
                        Subject(id, url, 0, title?.text(), avatar.attr("title"), typeString = sub,
                            images = Images(subjectImg.replace("/m/", "/l/"),
                                    subjectImg.replace("/m/", "/c/"), subjectImg,
                                    subjectImg.replace("/m/", "/s/"),
                                    subjectImg.replace("/m/", "/g/")))
                    else null
                }?.filterNotNull()?.toMutableList()?:ArrayList()
                linked.addAll(0, tankobon?:ArrayList())
                //commend
                val commend = doc.select(".subject_section").filter { it.select(".subtitle")?.text()?.contains("大概会喜欢") == true }.getOrNull(0)?.select("li")?.map {
                    val avatar = it.selectFirst(".avatar")
                    val subjectImg = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(avatar?.html()?:"")?.groupValues?.get(1)?:"", URI.create(Bangumi.SERVER))
                    val title = it.selectFirst(".info a")
                    val url = HttpUtil.getUrl(title?.attr("href")?:"", URI.create(Bangumi.SERVER))
                    val id = Regex("""/subject/([0-9]*)""").find(url)?.groupValues?.get(1)?.toIntOrNull()?:0
                    Subject(id, url, 0, title?.text(), avatar.attr("title"),
                            images = Images(subjectImg.replace("/m/", "/l/"),
                                    subjectImg.replace("/m/", "/c/"), subjectImg,
                                    subjectImg.replace("/m/", "/s/"),
                                    subjectImg.replace("/m/", "/g/")))
                }
                //tags
                val tags = doc.select(".subject_tag_section a")?.map {
                    Pair(it.selectFirst("span")?.text()?:"", it.selectFirst("small")?.text()?.toIntOrNull()?:0)
                }
                //typeString
                val typeString = doc.selectFirst(".nameSingle small")?.text()?:""
                //collection
                val interest = doc.selectFirst("#collectBoxForm")?.let{
                    val collectType = it.selectFirst(".collectType input[checked=checked]")
                    val collectStatus = Collection.StatusBean(collectType?.attr("value")?.toIntOrNull()?:return@let null, collectType.id(), collectType.parent()?.text())
                    val rate = it.selectFirst(".rating[checked]")?.attr("value")?.toIntOrNull()?:0
                    val collectTags = it.selectFirst("#tags")?.attr("value")?.split(" ")?.filter { it.isNotEmpty() }?:ArrayList()
                    val collectComment = it.selectFirst("#comment")?.text()
                    val private = it.selectFirst("#privacy[checked]")?.attr("value")?.toIntOrNull()?:0
                    return@let Collection(collectStatus, rate, collectComment, private, collectTags)
                }
                //formhash
                val formhash = if(doc.selectFirst(".guest") != null) "" else doc.selectFirst("input[name=formhash]")?.attr("value")
                Subject(subject.id, subject.url, type, name, name_cn, summary, eps_count, air_date, air_weekday, rating, rank, images, infobox = infobox,
                        ep_status = ep_status, vol_count = vol_count, vol_status = vol_status, has_vol = has_vol,
                        crt=crt, topic = topic, blog = blog, linked = linked, commend = commend, tags = tags, typeString = typeString, formhash = formhash, interest = interest)
            }
        }

        fun searchSubject(keywords: String, @SubjectType.SubjectType @Query("type") type: Int = SubjectType.ALL, page: Int, ua: String): Call<List<Subject>>{
            return ApiHelper.buildHttpCall("$SERVER/subject_search/${java.net.URLEncoder.encode(keywords, "utf-8")}?cat=$type&page=$page", mapOf("User-Agent" to ua)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Subject>()
                doc.select(".item")?.forEach{
                    it.attr("id").split('_').getOrNull(1)?.toIntOrNull()?.let{id->
                        val subjectType = it.selectFirst(".ico_subject_type")?.classNames()?.mapNotNull { it.split('_').last().toIntOrNull() }?.firstOrNull()?:0
                        val nameCN = it.selectFirst("h3")?.selectFirst("a")?.text()
                        val name = it.selectFirst("h3")?.selectFirst("small")?.text()?:nameCN
                        val img = getImageUrl(it.selectFirst("img"))
                        val info = it.selectFirst(".info")?.text()
                        ret += Subject(id,
                                HttpUtil.getUrl(it.selectFirst("a")?.attr("href")?:"", URI.create(SERVER)),
                                subjectType, name, nameCN, info,
                                images = Images(img.replace("/s/", "/l/"),
                                        img.replace("/s/", "/c/"),
                                        img.replace("/s/", "/m/"), img,
                                        img.replace("/s/", "/g/")),
                                collect = (it.selectFirst(".collectBlock")?.text()?.contains("修改") == true))
                    }
                }
                return@buildHttpCall ret
            }
        }

        fun searchMono(keywords: String, type: String, page: Int, ua: String): Call<List<MonoInfo>>{
            return ApiHelper.buildHttpCall("$SERVER/mono_search/${java.net.URLEncoder.encode(keywords, "utf-8")}?cat=$type&page=$page", mapOf("User-Agent" to ua)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<MonoInfo>()
                doc.select(".light_odd")?.forEach{
                    val a = it.selectFirst("h2 a")
                    val url =  HttpUtil.getUrl(a?.attr("href")?:"", URI.create(SERVER))
                    val name = a?.ownText()?.trim('/', ' ')
                    val nameCN = a?.selectFirst("span.tip")?.text()?:""
                    val img = getImageUrl(it.selectFirst("img"))
                    val summary = it.selectFirst(".prsn_info")?.text()
                    ret += MonoInfo(nameCN, name = name, url = url, img = img, summary = summary)
                }
                return@buildHttpCall ret
            }
        }

        fun getComments(subject: Subject, page: Int, ua: String): Call<List<Comment>>{
            return ApiHelper.buildHttpCall("${subject.url?:""}/comments?page=$page", mapOf("User-Agent" to ua)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Comment>()
                doc.selectFirst("#comment_box")?.let{
                    it.select(".item").forEach {
                        val img = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(it.selectFirst(".avatar")?.html()?:"")?.groupValues?.get(1)?:"", URI.create(Bangumi.SERVER))
                        val user = it.selectFirst(".text")?.selectFirst("a")
                        val id = Regex("""/user/([^/]*)""").find(user?.attr("href")?:"")?.groupValues?.get(1)
                        val userInfo = UserInfo(id?.toIntOrNull()?:0, HttpUtil.getUrl(user?.attr("href")?:"", URI.create(SERVER)), id, user?.text(), Images(img.replace("/s/", "/l/"),
                                img.replace("/s/", "/l/"),
                                img.replace("/s/", "/m/"), img,
                                img.replace("/s/", "/m/")))
                        val time = it.selectFirst(".grey")?.text()?.replace("@", "")?.trim()
                        val rate = Regex("""sstars([0-9]*)""").find(it.selectFirst(".text")?.selectFirst("span").toString())?.groupValues?.get(1)?.toIntOrNull()?:0
                        val comment = it.selectFirst("p")?.text()
                        ret += Comment(userInfo, time, comment, rate)
                    }
                }
                return@buildHttpCall ret
            }
        }

        fun browserAirTime(@SubjectType.SubjectTypeName subject_type: String,
                            year: Int, month: Int,
                            page: Int = 1, ua: String, sub_cat: String): Call<List<Subject>>{
            return ApiHelper.buildHttpCall("$SERVER/$subject_type/browser${if(sub_cat.isEmpty())"" else "/$sub_cat"}/airtime/$year-$month?page=$page", mapOf("User-Agent" to ua)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Subject>()
                doc.select(".item")?.forEach{
                    it.attr("id").split('_').getOrNull(1)?.toIntOrNull()?.let{id->
                        val nameCN = it.selectFirst("h3")?.selectFirst("a")?.text()
                        val name = it.selectFirst("h3")?.selectFirst("small")?.text()?:nameCN
                        val img = getImageUrl(it.selectFirst("img"))
                        val info = it.selectFirst(".info")?.text()
                        ret += Subject(id,
                                HttpUtil.getUrl(it.selectFirst("a")?.attr("href")?:"", URI.create(SERVER)),
                                0, name, nameCN, info,
                                images = Images(img.replace("/s/", "/l/"),
                                        img.replace("/s/", "/c/"),
                                        img.replace("/s/", "/m/"), img,
                                        img.replace("/s/", "/g/")),
                                collect = (it.selectFirst(".collectBlock")?.text()?.contains("修改") == true))
                    }
                }
                return@buildHttpCall ret
            }
        }

        //超展开
        fun getRakuen(type: String, ua: String): Call<List<Rakuen>>{
            val url = "$SERVER/m" + if(type.isEmpty()) "" else "?type=$type"
            return ApiHelper.buildHttpCall(url, mapOf("User-Agent" to ua)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Rakuen>()
                doc.select(".item_list")?.forEach{
                    val img = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(it.selectFirst(".avatar")?.html()?:"")?.groupValues?.get(1)?:"", URI.create(SERVER))
                    val title = it.selectFirst(".title")
                    val plus =  it.selectFirst(".grey").text()
                    val time = it.selectFirst(".time").text()?.replace("...", "")?:""
                    val group = it.selectFirst(".row").selectFirst("a")
                    ret+= Rakuen(img, title.text(), group?.text(), time, plus,
                            HttpUtil.getUrl(title.attr("href")?:"", URI.create(SERVER)),
                            HttpUtil.getUrl(group?.attr("href")?:"", URI.create(SERVER)))
                }
                return@buildHttpCall ret
            }
        }

        //timeline
        //type: global
        fun getTimeLine(type: String, page: Int, ua: String, usr: UserInfo?): Call<List<TimeLine>>{
            return ApiHelper.buildHttpCall("$SERVER${if(usr == null) "" else "/user/${usr.username}"}/timeline?type=$type&page=$page&ajax=1", if(ua.isEmpty()) mapOf() else mapOf("User-Agent" to ua), useCookie = ua.isNotEmpty()){rsp ->
                val doc = Jsoup.parse(rsp.body()?.string()?:"")
                val ret = ArrayList<TimeLine>()
                var usrImg = usr?.avatar?.large?:""
                var userUrl = usr?.url?:""
                val cssInfo = if(usr == null) ".info" else ".info_full"
                doc.selectFirst("#timeline")?.children()?.forEach{ timeline ->
                    if(timeline.hasClass("Header")){
                        ret += TimeLine(true, timeline.text())
                    }else timeline.select(".tml_item")?.forEach { item ->
                        //user
                        val user = item.selectFirst("a.avatar")
                        val img = Regex("""background-image:url\('([^']*)'\)""").find(user?.html()?:"")?.groupValues?.get(1)?:""
                        if(img.isNotEmpty()){
                            usrImg = HttpUtil.getUrl(img, URI.create(SERVER))
                            userUrl = HttpUtil.getUrl(user?.attr("href")?:"", URI.create(SERVER))
                        }
                        //action
                        val action = item.selectFirst(cssInfo)?.childNodes()?.map {
                            if(it is TextNode || (it as? Element)?.tagName() == "a" && it.selectFirst("img") == null)
                                it.outerHtml()
                            else if((it as? Element)?.hasClass("status") == true)
                                "<br/>"+it.html()
                            else ""
                        }?.reduce { acc, s -> acc + s }?:""
                        //time
                        val time = item.selectFirst(".date")?.text()?.trim('·', ' ', '回', '复')?:""
                        val content = item.selectFirst(".collectInfo")?.text()?:
                                item.selectFirst(".info_sub")?.text()
                        val contentUrl = item.selectFirst(".info_sub a")?.attr("href")
                        val collectStar = Regex("""sstars([0-9]*)""").find(item.selectFirst(".starsinfo")?.outerHtml()?:"")?.groupValues?.get(1)?.toIntOrNull()?:0
                        //thumb
                        val thumbs = ArrayList<TimeLine.TimeLineItem.ThumbItem>()
                        item.select("$cssInfo img")?.forEach {
                            val url = it.parent().attr("href")
                            val text = item.select("a[href=\"$url\"]")?.text()?:""
                            val src = getImageUrl(it)
                            thumbs += TimeLine.TimeLineItem.ThumbItem(src, text, url)
                        }
                        val delUrl: String? = item.selectFirst(".tml_del")?.attr("href")
                        val sayUrl: String? = item.selectFirst("a.tml_comment")?.attr("href")
                        ret += TimeLine(TimeLine.TimeLineItem(usrImg, userUrl, action, time, content, contentUrl, collectStar, thumbs, delUrl, sayUrl))
                    }
                }
                return@buildHttpCall ret
            }
        }

        //讨论
        fun getTopic(url: String, ua: String): Call<Topic>{
            return ApiHelper.buildHttpCall(url, mapOf("User-Agent" to ua)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val replies = ArrayList<TopicPost>()
                doc.select(".re_info")?.map{ it.parent() }?.forEach{
                    val img = Regex("""background-image:url\('([^']*)'\)""").find(it.selectFirst(".avatar")?.html()?:"")?.groupValues?.get(1)?:""
                    val user = it.selectFirst(".inner")?.selectFirst("a")
                    val userId = Regex("""/user/([^/]*)""").find(user?.attr("href")?:"")?.groupValues?.get(1)?:""
                    val userName = user?.text()?:""
                    val message = it.selectFirst(".topic_content")?.html()
                            ?:it.selectFirst(".message")?.html()
                            ?:it.selectFirst(".cmt_sub_content")?.html()?:""
                    val isSubReply = it.selectFirst(".re_info")?.selectFirst("a")?.text()?.contains("-")?:false
                    val info = it.selectFirst(".re_info")?.text()?.split("/")?.get(0)?.trim()?:""
                    val isSelf = it.selectFirst(".re_info")?.text()?.contains("/") == true
                    val data = (it.selectFirst(".icons_cmt")?.attr("onclick")?:"").split(",")
                    val topic_id = data.getOrNull(1)?:""
                    val relate = data.getOrNull(2)?.toIntOrNull()?:0
                    val post_id = data.getOrNull(3)?.toIntOrNull()?:0
                    val post_uid = data.getOrNull(5)?:""
                    val model = Regex("'([^']*)'").find(data.getOrNull(0)?:"")?.groupValues?.get(1)?:""
                    replies += TopicPost(
                            (if(post_id == 0) relate else post_id).toString(), //pst_id
                            topic_id, //pst_mid
                            post_uid, //pst_uid
                            message, //pst_content
                            userId, //username
                            userName, //nickName
                            img,  //avatar
                            info.substring(max(min(info.indexOf(" - ")+3, info.length-1), 0)), //dateline
                            isSelf,
                            isSubReply,
                            isSelf,
                            relate.toString(),
                            model
                    )
                }
                val user_id = Regex("""/user/([^/]*)""").find(doc.selectFirst("#header")?.selectFirst(".avatar")?.attr("href")?:"")?.groupValues?.get(1)?:""
                val error = doc.selectFirst("#reply_wrapper")?.selectFirst(".tip")
                val errorLink = HttpUtil.getUrl(error?.selectFirst("a")?.attr("href")?:"", URI.create(SERVER))
                val group = doc.selectFirst("#pageHeader")?.selectFirst("span")?.text()?:""
                val title = doc.selectFirst("#pageHeader")?.selectFirst("h1")?.ownText()?:""
                val form = doc.selectFirst("#ReplyForm")
                val post = HttpUtil.getUrl("${form?.attr("action")}?ajax=1", URI.create(SERVER))
                val formhash = form?.selectFirst("input[name=formhash]")?.attr("value")
                val lastview = form?.selectFirst("input[name=lastview]")?.attr("value")
                val links = LinkedHashMap<String, String>()
                doc.selectFirst("#pageHeader")?.select("a")?.filter { !it.text().isNullOrEmpty() }?.forEach {
                    links[it.text()]= HttpUtil.getUrl(it.attr("href")?:"", URI.create(SERVER)) }
                return@buildHttpCall Topic(user_id, group, title, replies, post, formhash, lastview, links, error?.text(), errorLink)
            }
        }

        //通知
        fun getNotify(ua: String): Call<List<Notify>>{
            return ApiHelper.buildHttpCall("$SERVER/notify", mapOf("User-Agent" to ua)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Notify>()
                doc.select(".tml_item")?.forEach {
                    val user = it.selectFirst("a.avatar")
                    val userId = Regex("""/user/([^/]*)""").find(user?.attr("href")?:"")?.groupValues?.get(1)
                    val img = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(user?.html()?:"")?.groupValues?.get(1)?:"", URI.create(Bangumi.SERVER))
                    val userName = it.selectFirst(".inner")?.selectFirst("strong")?.text()
                    val content = it.selectFirst(".inner")?.text()?:""
                    val url = it.selectFirst(".nt_link")?.attr("href")?:""
                    ret += Notify(url, content, UserInfo(0, "$SERVER/user/$userId", userId, userName, Images(img.replace("/s/", "/l/"),
                            img.replace("/s/", "/l/"),
                            img.replace("/s/", "/m/"), img,
                            img.replace("/s/", "/m/"))))
                }
                ret
            }
        }

        //userInfo
        fun getUserInfo(ua: String): Call<UserInfo>{
            val cookieManager = CookieManager.getInstance()
            return ApiHelper.buildHttpCall("$SERVER/settings", mapOf("User-Agent" to ua)){
                var needReload = false
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val user = doc.selectFirst(".idBadgerNeue a.avatar")?: throw Exception("login failed")
                val userName = doc.selectFirst("input[name=nickname]")?.attr("value")//doc.selectFirst("#header a")?.text()
                val img = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(user.html()?:"")?.groupValues?.get(1)?:"", URI.create(Bangumi.SERVER))
                val id = Regex("""/user/([^/]*)""").find(user.attr("href")?:"")?.groupValues?.get(1)
                it.headers("set-cookie").forEach {
                    needReload = true
                    cookieManager.setCookie(Bangumi.SERVER, it) }
                val formhash = doc.selectFirst("input[name=formhash]")?.attr("value")
                val inbox = Regex("叮咚叮咚～你有 ([0-9]+) 条新信息!").find(doc.selectFirst("#robot_speech_js")?.text()?:"")?.groupValues?.get(1)?.toIntOrNull()?:0
                val notify = doc.selectFirst("#notify_count")?.text()?.toIntOrNull()?:0
                UserInfo(id?.toIntOrNull()?:0, HttpUtil.getUrl(user.attr("href")?:"", URI.create(SERVER)), id, userName,
                        Images(img.replace("/s/", "/l/"),
                                img.replace("/s/", "/l/"),
                                img.replace("/s/", "/m/"), img,
                                img.replace("/s/", "/m/")), sign = formhash, needReload = needReload, notify = Pair(inbox, notify))
            }
        }

        fun updateCollectionStatus(subject: Subject, formhash: String, ua: String, status: String, tags: String, comment: String, rating: Int, privacy: Int = 0): Call<Collection>{
            val index = CollectionStatusType.status.indexOf(status)
            return ApiHelper.buildHttpCall("$SERVER/subject/${subject.id}/interest/update?gh=$formhash", mapOf("User-Agent" to ua), FormBody.Builder()
                    .add("referer", "ajax")
                    .add("interest", (index + 1).toString())
                    .add("rating", rating.toString())
                    .add("tags", tags)
                    .add("comment", comment)
                    .add("privacy", privacy.toString())
                    .add("update", "保存").build()){
                Collection(Collection.StatusBean(index + 1, status, when(subject.type){
                    SubjectType.BOOK -> listOf("想读", "读过", "在读", "搁置", "抛弃").getOrNull(index)
                    SubjectType.MUSIC -> listOf("想听", "听过", "在听", "搁置", "抛弃").getOrNull(index)
                    SubjectType.GAME -> listOf("想玩", "玩过", "在玩", "搁置", "抛弃").getOrNull(index)
                    else -> listOf("想看", "看过", "在看", "搁置", "抛弃").getOrNull(index)
                }), rating, comment, privacy, tags.split(" ").filter { it.isNotEmpty() })
            }
        }

        //prg
        fun getCollection(ua: String): Call<List<SubjectCollection>>{
            return ApiHelper.buildHttpCall(SERVER, mapOf("User-Agent" to ua)){
                val ret = ArrayList<SubjectCollection>()
                val doc = Jsoup.parse(it.body()?.string()?:"")
                if(doc.selectFirst(".idBadgerNeue a.avatar") == null) throw Exception("no login")
                doc.select("#cloumnSubjectInfo .infoWrapper")?.forEach {
                    val data = it.selectFirst(".headerInner a.textTip")?:return@forEach
                    val id = data.attr("data-subject-id")?.toIntOrNull()?:return@forEach
                    val type = it.attr("subject_type")?.toIntOrNull()?:return@forEach
                    val name = data.attr("data-subject-name")
                    val name_cn = data.attr("data-subject-name-cn")
                    val img = getImageUrl(it.selectFirst("img"))
                    val ep_status = it.selectFirst("input[name=watchedeps]")?.attr("value")?.toIntOrNull()?:0
                    var eps_count = it.selectFirst(".prgBatchManagerForm .grey")?.text()?.trim(' ', '/')?.toIntOrNull()?:0
                    val vol_status = it.selectFirst("input[name=watched_vols]")?.attr("value")?.toIntOrNull()?:0
                    var vol_count = 0
                    var has_vol = false
                    it.select(".prgText")?.forEach {
                        when(it.selectFirst(".type")?.text()){
                            "Chap." -> eps_count = it.ownText()?.trim(' ', '/')?.toIntOrNull()?:0
                            "Vol." -> {
                                has_vol = true
                                vol_count = it.ownText()?.trim(' ', '/')?.toIntOrNull()?:0
                            }
                        }
                    }
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val now = Date().time
                    var cat = "MAIN"
                    val eps = it.select("ul.prg_list>li")?.mapNotNull {li->
                        if(li.hasClass("subtitle")) cat = li.text()
                        val it = li.selectFirst("a")?:return@mapNotNull null
                        val epInfo = doc.selectFirst(it.attr("rel"))?.selectFirst(".tip")?.textNodes()?.map { it.text() }
                        val ep_name_cn = epInfo?.firstOrNull { it.startsWith("中文标题") }?.substringAfter(":")
                        val air_date = epInfo?.firstOrNull { it.startsWith("首播") }?.substringAfter(":")
                        val duration = epInfo?.firstOrNull { it.startsWith("时长") }?.substringAfter(":")
                        val status  = if(it.hasClass("epBtnToday")) "Today" else if(it.hasClass("epBtnAir") || try{ dateFormat.parse(air_date).time }catch (e: Exception){ 0L } < now) "Air" else "NA"
                        val epId = it.id().substringAfter("_").toIntOrNull()?:return@mapNotNull null
                        val cmt = doc.selectFirst(it.attr("rel"))?.selectFirst(".cmt .na")?.text()?.trim('(', ')', '+')?.toIntOrNull()?:0
                        Episode(epId, HttpUtil.getUrl(it.attr("href")?:"", URI.create(Bangumi.SERVER)), when(cat){
                            "MAIN" -> Episode.TYPE_MAIN
                            "SP" -> Episode.TYPE_SP
                            "OP" -> Episode.TYPE_OP
                            "ED" -> Episode.TYPE_ED
                            "PV" -> Episode.TYPE_PV
                            "MAD" -> Episode.TYPE_MAD
                            else -> Episode.TYPE_OTHER
                        },
                                it.text().toFloat(), it.attr("title")?.substringAfter(" "), ep_name_cn, duration, air_date, cmt, status =  status, progress =  when{
                                    it.hasClass("epBtnWatched") -> SubjectProgress.EpisodeProgress(epId, SubjectProgress.EpisodeProgress.EpisodeStatus(SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH_ID, url_name = SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH, cn_name = "看过"))
                                    it.hasClass("epBtnQueue") -> SubjectProgress.EpisodeProgress(epId, SubjectProgress.EpisodeProgress.EpisodeStatus(SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE_ID, url_name = SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE, cn_name = "想看"))
                                    it.hasClass("epBtnDrop") -> SubjectProgress.EpisodeProgress(epId, SubjectProgress.EpisodeProgress.EpisodeStatus(SubjectProgress.EpisodeProgress.EpisodeStatus.DROP_ID, url_name = SubjectProgress.EpisodeProgress.EpisodeStatus.DROP, cn_name = "抛弃"))
                                    else -> null
                                })
                    }
                    val watched_eps = it.selectFirst("input[name=watchedeps]")?.attr("value")?.toIntOrNull()?:0
                    val watched_vols = it.selectFirst("input[name=watched_vols]")?.attr("value")?.toIntOrNull()?:0
                    ret += SubjectCollection(name, id, watched_eps, watched_vols, 0, Subject(
                            id, "$SERVER/subject/$id", type, name, name_cn, eps = eps,
                            eps_count = eps_count, ep_status = ep_status, vol_count = vol_count, vol_status = vol_status, has_vol = has_vol,
                            images = Images(img.replace("/s/", "/l/"),
                                    img.replace("/s/", "/m/"),
                                    img.replace("/s/", "/c/"), img,
                                    img.replace("/s/", "/s/"))))
                }
                return@buildHttpCall ret
            }
        }

        fun updateProgress(id: Int,
                           @SubjectProgress.EpisodeProgress.EpisodeStatus.Companion.EpStatusType status: String,
                           formhash: String, ua: String,
                           epIds: String? = null): Call<Boolean>{
            return ApiHelper.buildHttpCall("$SERVER/subject/ep/$id/status/$status?gh=$formhash&ajax=1", mapOf("User-Agent" to ua), FormBody.Builder().add("ep_id", epIds?:id.toString()).build() ){
                return@buildHttpCall it.body()?.string()?.contains("\"status\":\"ok\"") == true
            }
        }

        //eps
        fun getSubjectEps(subject: Int, ua: String): Call<List<Episode>>{
            return ApiHelper.buildHttpCall("$SERVER/subject/$subject/ep", mapOf("User-Agent" to ua)){
                var cat = ""
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val type = when(doc.selectFirst("#navMenuNeue .focus").text()){
                    "动画" -> SubjectType.ANIME
                    "书籍" -> SubjectType.BOOK
                    "音乐" -> SubjectType.MUSIC
                    "游戏" -> SubjectType.GAME
                    "三次元" -> SubjectType.REAL
                    else -> SubjectType.ALL
                }
                doc.select("ul.line_list>li")?.mapNotNull {
                    if(it.hasClass("cat")){
                        cat = it.text()
                        null
                    }else{
                        //val epId = it.selectFirst(".checkbox")?.attr("value")?.toIntOrNull()?:return@mapNotNull null
                        val epId = Regex("""/ep/([0-9]*)""").find(it.selectFirst("h6>a")?.attr("href")?: "")?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null

                        val values = Regex("^\\D*(\\d+\\.?\\d?)\\.(.*)").find(it.selectFirst("h6>a")?.text()?:"")?.groupValues
                        val sort = values?.getOrNull(1)?.toFloatOrNull()?:0f
                        val progress = it.selectFirst(".listEpPrgManager>span")
                        val status = if(type == SubjectType.MUSIC) "Air" else it.selectFirst(".epAirStatus span")?.className()
                        val ep_name = values?.getOrNull(2)?:it.selectFirst("h6>a")?.text()?.substringAfter(".")
                        val ep_name_cn = it.selectFirst("h6>span.tip")?.text()?.substringAfter(" ")
                        val epInfo = it.select("small.grey")?.text()?.split("/")
                        val air_date = epInfo?.firstOrNull { it.trim().startsWith("首播") }?.substringAfter(":")
                        val duration = epInfo?.firstOrNull { it.trim().startsWith("时长") }?.substringAfter(":")
                        val comment = epInfo?.firstOrNull { it.trim().startsWith("讨论") }?.trim()?.substringAfter("+")?.toIntOrNull()?:0

                        Episode(epId, "$SERVER/ep/$epId", if(type == SubjectType.MUSIC) Episode.TYPE_MUSIC else when(cat){
                            "本篇" -> Episode.TYPE_MAIN
                            "特别篇" -> Episode.TYPE_SP
                            "OP" -> Episode.TYPE_OP
                            "ED" -> Episode.TYPE_ED
                            "PV" -> Episode.TYPE_PV
                            "MAD" -> Episode.TYPE_MAD
                            else -> Episode.TYPE_OTHER
                        }, sort, ep_name, ep_name_cn, duration, air_date, comment, status = status, progress =  when{
                            progress?.hasClass("statusWatched") == true -> SubjectProgress.EpisodeProgress(epId, SubjectProgress.EpisodeProgress.EpisodeStatus(SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH_ID, url_name = SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH, cn_name = "看过"))
                            progress?.hasClass("statusQueue") == true -> SubjectProgress.EpisodeProgress(epId, SubjectProgress.EpisodeProgress.EpisodeStatus(SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE_ID, url_name = SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE, cn_name = "想看"))
                            progress?.hasClass("statusDrop") == true -> SubjectProgress.EpisodeProgress(epId, SubjectProgress.EpisodeProgress.EpisodeStatus(SubjectProgress.EpisodeProgress.EpisodeStatus.DROP_ID, url_name = SubjectProgress.EpisodeProgress.EpisodeStatus.DROP, cn_name = "抛弃"))
                            else -> null
                        }, cat = cat)
                    }
                }?:ArrayList()
            }
        }
    }
}