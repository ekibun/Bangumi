package soko.ekibun.bangumi.api.github

import io.reactivex.rxjava3.core.Observable
import retrofit2.http.GET
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.github.bean.Release

/**
 * GitHub API
 */
interface Github {

    /**
     * 获取版本列表
     * @return Call<List<Release>>
     */
    @GET("/repos/ekibun/Bangumi/releases")
    fun releases(): Observable<List<Release>>

    companion object {
        private const val SERVER_API = "https://api.github.com"

        /**
         * 创建retrofit实例
         * @return Github
         */
        fun createInstance(): Github {
            return ApiHelper.createRetrofitBuilder(SERVER_API)
                .build().create(Github::class.java)
        }
    }
}