package soko.ekibun.bangumi.api.trim21

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import soko.ekibun.bangumi.api.trim21.bean.IpView

interface BgmIpViewer {
    @GET("/subject/{id}.json")
    fun subject(@Path("id") id: Int): Call<IpView>

    companion object {
        private const val SERVER_API = "http://bgm-ip-viewer.trim21.cn"
        fun createInstance(): BgmIpViewer {
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(BgmIpViewer::class.java)
        }
    }
}