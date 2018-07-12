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
            val cookie = CookieManager.getInstance().getCookie(SERVER)
            return ApiHelper.buildHttpCall("$SERVER/$subject_type/list/$username/$collection_status?page=$page", mapOf("cookie" to cookie)){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<SubjectCollection>()
                doc.select(".item").forEach {
                    it.attr("id").split('_').getOrNull(1)?.toIntOrNull()?.let{id->
                        val nameCN = it.selectFirst("h3")?.selectFirst("a")?.text()
                        val name = it.selectFirst("h3")?.selectFirst("small")?.text()?:nameCN
                        val img = "http:" + it.selectFirst("img")?.attr("src")?.replace("cover/s/", "cover/m/")
                        val info = it.selectFirst(".info")?.text()
                        val subject = Subject(id,
                                Bangumi.SERVER + it.selectFirst("a")?.attr("href"),
                                0,
                                name,
                                nameCN,
                                info,
                                images = Images(img, img, img, img, img)
                        )
                        ret += SubjectCollection(name, id, -1, -1, subject = subject)
                    }
                }
                return@buildHttpCall ret
            }
        }

        fun getSubject(subject: Subject): Call<List<Subject>>{
            return ApiHelper.buildHttpCall(subject.url?:""){
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
                        val url = SERVER + title.attr("href")
                        val id = Regex("""/subject/([0-9]*)""").find(url)?.groupValues?.get(1)?.toIntOrNull()?:0
                        val name = title.text()
                        ret += Subject(id, url, 0, name, summary = sub, images = Images(img, img, img, img, img))
                    }
                }
                return@buildHttpCall ret
            }
        }

        fun getComments(subject: Subject, page: Int): Call<List<Comment>>{
            return ApiHelper.buildHttpCall("${subject.url?:""}/comments?page=$page"){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Comment>()
                doc.selectFirst("#comment_box")?.let{
                    it.select(".item").forEach {
                        val img = "http:" + Regex("""background-image:url\('([^']*)'\)""").find(it.selectFirst(".avatar")?.html()?:"")?.groupValues?.get(1)
                        val user = it.selectFirst(".text")?.selectFirst("a")
                        val id = Regex("""/user/([^/]*)""").find(user?.attr("href")?:"")?.groupValues?.get(1)
                        val userInfo = UserInfo(id?.toIntOrNull()?:0, SERVER + user?.attr("href"), id, user?.text(), Images(img, img, img, img, img))
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
            return ApiHelper.buildHttpCall("$SERVER/$subject_type/browser/airtime/$year-$month?page=$page"){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = ArrayList<Subject>()
                doc.select(".item")?.forEach{
                    it.attr("id").split('_').getOrNull(1)?.toIntOrNull()?.let{id->
                        val nameCN = it.selectFirst("h3")?.selectFirst("a")?.text()
                        val name = it.selectFirst("h3")?.selectFirst("small")?.text()?:nameCN
                        val img = "http:" + it.selectFirst("img")?.attr("src")?.replace("cover/s/", "cover/m/")
                        val info = it.selectFirst(".info")?.text()
                        ret += Subject(id,
                                Bangumi.SERVER + it.selectFirst("a")?.attr("href"),
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
    }
}