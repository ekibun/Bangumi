package soko.ekibun.bangumi.api.bangumi

import android.webkit.CookieManager
import org.jsoup.Jsoup
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.*
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.util.HttpUtil
import java.net.URI
import kotlin.math.max
import kotlin.math.min

interface Bangumi {

    @GET("/search/subject/{keywords}")
    fun search(@Path("keywords") keywords: String,
               @SubjectType.SubjectType @Query("type") type: Int = SubjectType.ALL,
               @Query("start")start: Int = 0,
               @Query("max_results")max_results: Int = 10,
               @Header("cookie") cookie: String = "chii_searchDateLine=0"
    ): Call<SearchResult>

    @GET("/subject/{id}")
    fun subject(@Path("id") id: Int,
                @ResponseGroup.ResponseGroup @Query("responseGroup") responseGroup: String = ResponseGroup.LARGE
    ): Call<Subject>

    @GET("/user/{username}")
    fun user(@Path("username") id: String
    ): Call<UserInfo>

    @GET("/user/{username}/collection")
    fun collection(@Path("username") username: String,
                   @Query("cat") cat: String = "all_watching"
    ): Call<List<SubjectCollection>>

    @GET("/collection/{subject_id}")
    fun collectionStatus(@Path("subject_id") subject_id: Int,
                         @Query("access_token") access_token: String
    ): Call<Collection>

    @GET("/ep/{id}/status/{status}")
    fun updateProgress(@Path("id") id: Int,
                       @SubjectProgress.EpisodeProgress.EpisodeStatus.Companion.EpStatusType
                       @Path("status") status: String,
                       @Query("access_token") access_token: String
    ): Call<StatusCode>

    @GET("/user/{username}/progress")
    fun progress(@Path("username") username: String,
                 @Query("subject_id") subject_id: Int,
                 @Query("access_token") access_token: String
    ): Call<SubjectProgress>

    @FormUrlEncoded
    @POST("/collection/{subject_id}/update")
    fun updateCollectionStatus(@Path("subject_id") subject_id: Int,
                               @Field("access_token") access_token: String,
                               @Field("status") status: String,
                               @Field("comment") comment: String?,
                               @Field("rating") rating: Int,
                               @Field("privacy") privacy: Int = 0
    ): Call<Collection>

    @FormUrlEncoded
    @POST("/oauth/access_token")
    fun accessToken(@Field("code") code : String,
                    @Field("redirect_uri") redirect_uri: String = REDIRECT_URL,
                    @Field("grant_type") grant_type : String = "authorization_code",
                    @Field("client_id") client_id : String = APP_ID,
                    @Field("client_secret") client_secret : String = APP_SECRET
    ): Call<AccessToken>

    @FormUrlEncoded
    @POST("/oauth/access_token")
    fun refreshToken(@Field("refresh_token") refresh_token : String,
                     @Field("redirect_uri") redirect_uri: String = REDIRECT_URL,
                     @Field("grant_type") grant_type : String = "refresh_token",
                     @Field("client_id") client_id : String = APP_ID,
                     @Field("client_secret") client_secret : String = APP_SECRET
    ): Call<AccessToken>

    /*
    @FormUrlEncoded
    @POST("/oauth/token_status")
    fun tokenStatus(@Field("access_token") access_token : String
    ): Call<AccessToken>
    */

    @GET("/calendar")
    fun calendar(): Call<List<Calendar>>

    /*
    @GET("/{subject_type}/list/{username}/{collection_status}")
    fun getCollectionList(@SubjectType.SubjectTypeName @Path("subject_type")subject_type: String,
                          @Path("username") username: String,
                          @CollectionStatusType.CollectionStatusType @Path("collection_status") collection_status: String,
                          @Query("page")page: Int = 1
    ): Call<List<SubjectCollection>>
    */

    companion object {
        const val SERVER = "https://bgm.tv"
        private const val SERVER_API = "https://api.bgm.tv"
        const val APP_ID = "bgm2315af5554b7f887"
        const val APP_SECRET = "adaf4941f83f2fb3c4336ee80a087f75"
        const val REDIRECT_URL = "bangumi://redirect"
        fun createInstance(api: Boolean = true): Bangumi{
            return Retrofit.Builder().baseUrl(if(api) SERVER_API else SERVER)
                    .addConverterFactory( GsonConverterFactory.create())
                    .build().create(Bangumi::class.java)
        }

        fun getCollectionList(@SubjectType.SubjectTypeName subject_type: String,
                              username: String,
                              @CollectionStatusType.CollectionStatusType collection_status: String,
                              page: Int = 1
        ): Call<List<SubjectCollection>>{
            val cookie = CookieManager.getInstance().getCookie(Bangumi.SERVER)?:""
            return ApiHelper.buildHttpCall("$SERVER/$subject_type/list/$username/$collection_status?page=$page", mapOf("cookie" to cookie)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<SubjectCollection>()
                doc.select(".item").forEach {
                    it.attr("id").split('_').getOrNull(1)?.toIntOrNull()?.let{id->
                        val nameCN = it.selectFirst("h3")?.selectFirst("a")?.text()
                        val name = it.selectFirst("h3")?.selectFirst("small")?.text()?:nameCN
                        val img = "http:" + it.selectFirst("img")?.attr("src")
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

        fun getSubject(subject: Subject): Call<List<Subject>>{
            val cookie = CookieManager.getInstance().getCookie(Bangumi.SERVER)?:""
            return ApiHelper.buildHttpCall(subject.url?:"", mapOf("cookie" to cookie)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Subject>()
                doc.select(".subject_section").filter { it.select(".subtitle").text() == "关联条目" }.getOrNull(0)?.let{
                    var sub = ""
                    it.select("li").forEach {
                        val newSub = it.selectFirst(".sub").text()
                        if(!newSub.isNullOrEmpty()) sub = newSub
                        val avatar = it.selectFirst(".avatar")
                        val img = "http:" + Regex("""background-image:url\('([^']*)'\)""").find(avatar.html())?.groupValues?.get(1)
                        val title = it.selectFirst(".title")
                        val url = HttpUtil.getUrl(title.attr("href")?:"", URI.create(Bangumi.SERVER))
                        val id = Regex("""/subject/([0-9]*)""").find(url)?.groupValues?.get(1)?.toIntOrNull()?:0
                        val name = title.text()
                        ret += Subject(id, url, 0, name, summary = sub,
                                images = Images(img.replace("/m/", "/l/"),
                                        img.replace("/m/", "/c/"), img,
                                        img.replace("/m/", "/s/"),
                                        img.replace("/m/", "/g/")))
                    }
                }
                return@buildHttpCall ret
            }
        }

        fun getComments(subject: Subject, page: Int): Call<List<Comment>>{
            val cookie = CookieManager.getInstance().getCookie(Bangumi.SERVER)?:""
            return ApiHelper.buildHttpCall("${subject.url?:""}/comments?page=$page", mapOf("cookie" to cookie)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Comment>()
                doc.selectFirst("#comment_box")?.let{
                    it.select(".item").forEach {
                        val img = "http:" + Regex("""background-image:url\('([^']*)'\)""").find(it.selectFirst(".avatar")?.html()?:"")?.groupValues?.get(1)
                        val user = it.selectFirst(".text")?.selectFirst("a")
                        val id = Regex("""/user/([^/]*)""").find(user?.attr("href")?:"")?.groupValues?.get(1)
                        val userInfo = UserInfo(id?.toIntOrNull()?:0, HttpUtil.getUrl(user?.attr("href")?:"", URI.create(SERVER)), id, user?.text(), Images(img, img, img, img, img))
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
                            page: Int = 1): Call<List<Subject>>{
            val cookie = CookieManager.getInstance().getCookie(Bangumi.SERVER)?:""
            return ApiHelper.buildHttpCall("$SERVER/$subject_type/browser/airtime/$year-$month?page=$page", mapOf("cookie" to cookie)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Subject>()
                doc.select(".item")?.forEach{
                    it.attr("id").split('_').getOrNull(1)?.toIntOrNull()?.let{id->
                        val nameCN = it.selectFirst("h3")?.selectFirst("a")?.text()
                        val name = it.selectFirst("h3")?.selectFirst("small")?.text()?:nameCN
                        val img = "http:" + it.selectFirst("img")?.attr("src")?.replace("cover/s/", "cover/m/")
                        val info = it.selectFirst(".info")?.text()
                        ret += Subject(id,
                                HttpUtil.getUrl(it.selectFirst("a")?.attr("href")?:"", URI.create(SERVER)),
                                0,
                                name,
                                nameCN,
                                info,
                                images = Images(img, img, img, img, img)
                        )
                    }
                }
                return@buildHttpCall ret
            }
        }

        //超展开
        fun getRakuen(type: String): Call<List<Rakuen>>{
            val url = "$SERVER/m" + if(type.isEmpty()) "" else "?type=$type"
            return ApiHelper.buildHttpCall(url){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Rakuen>()
                doc.select(".item_list")?.forEach{
                    val img = "http:" + Regex("""background-image:url\('([^']*)'\)""").find(it.selectFirst(".avatar")?.html()?:"")?.groupValues?.get(1)
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

        //讨论
        fun getTopic(url: String): Call<Topic>{
            val cookie = CookieManager.getInstance().getCookie(Bangumi.SERVER)?:""
            return ApiHelper.buildHttpCall(url, mapOf("cookie" to cookie)){
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
                val group = doc.selectFirst("#pageHeader")?.selectFirst("span")?.text()?:""
                val title = doc.selectFirst("#pageHeader")?.selectFirst("h1")?.ownText()?:""
                val form = doc.selectFirst("#ReplyForm")
                val post = HttpUtil.getUrl("${form?.attr("action")}?ajax=1", URI.create(SERVER))
                val formhash = form?.selectFirst("input[name=formhash]")?.attr("value")
                val lastview = form?.selectFirst("input[name=lastview]")?.attr("value")
                val links = LinkedHashMap<String, String>()
                doc.selectFirst("#pageHeader")?.select("a")?.filter { !it.text().isNullOrEmpty() }?.forEach {
                    links[it.text()]= HttpUtil.getUrl(it.attr("href")?:"", URI.create(SERVER)) }
                links[title]= url
                return@buildHttpCall Topic(group, title, replies, post, formhash, lastview, links)
            }
        }

        //通知
        fun getNotify(): Call<List<Notify>>{
            val cookie = CookieManager.getInstance().getCookie(Bangumi.SERVER)?:""
            return ApiHelper.buildHttpCall("$SERVER/notify", mapOf("cookie" to cookie)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Notify>()
                doc.select(".tml_item")?.forEach {
                    val user = it.selectFirst("a.avatar")
                    val userId = Regex("""/user/([^/]*)""").find(user?.attr("href")?:"")?.groupValues?.get(1)
                    val img = "http:" + Regex("""background-image:url\('([^']*)'\)""").find(user?.html()?:"")?.groupValues?.get(1)
                    val userName = it.selectFirst(".inner")?.selectFirst("strong")?.text()
                    val content = it.selectFirst(".inner")?.text()?:""
                    val url = it.selectFirst(".nt_link")?.attr("href")?:""
                    ret += Notify(url, content, UserInfo(0, "$SERVER/user/$userId", userId, userName, Images(img, img, img, img, img)))
                }
                ret
            }
        }
    }
}