package soko.ekibun.bangumi.api.github

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import soko.ekibun.bangumi.api.github.bean.Release

/**
 * GitHub API
 */
interface Github{

    /**
     * 获取版本列表
     */
    @GET("/repos/ekibun/Bangumi/releases")
    fun releases(): Call<List<Release>>

    companion object {
        private const val SERVER_API = "https://api.github.com"
        /**
         * 创建retrofit实例
         */
        fun createInstance(): Github{
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(Github::class.java)
        }
    }
}