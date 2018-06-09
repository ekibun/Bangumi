package soko.ekibun.bangumi.api.bangumi

import retrofit2.Call
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
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

    @GET("/{subject_type}/list/{username}/{collection_status}")
    fun getCollectionList(@SubjectType.SubjectTypeName @Path("subject_type")subject_type: String,
                          @Path("username") username: String,
                          @CollectionStatusType.CollectionStatusType @Path("collection_status") collection_status: String,
                          @Query("page")page: Int = 1
    ): Call<List<SubjectCollection>>

    companion object {
        const val SERVER = "https://bgm.tv"
        private const val SERVER_API = "https://api.bgm.tv"
        const val APP_ID = "bgm2315af5554b7f887"
        const val APP_SECRET = "adaf4941f83f2fb3c4336ee80a087f75"
        const val REDIRECT_URL = "bangumi://redirect"
        fun createInstance(api: Boolean = true, converterFactory: Converter.Factory = GsonConverterFactory.create()): Bangumi{
            return Retrofit.Builder().baseUrl(if(api) SERVER_API else SERVER)
                    .addConverterFactory(converterFactory)
                    .build().create(Bangumi::class.java)
        }
    }
}