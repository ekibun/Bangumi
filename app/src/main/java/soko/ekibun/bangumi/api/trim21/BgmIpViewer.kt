package soko.ekibun.bangumi.api.trim21

import android.os.Build
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import soko.ekibun.bangumi.BuildConfig
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.trim21.bean.IpView
import java.util.ArrayList

interface BgmIpViewer {
    @GET("/api.v1/view_ip/subject/{id}")
    fun subject(@Path("id") id: Int,
                @Header("user-agent") ua: String = "Bangumi-ekibun/${BuildConfig.VERSION_NAME} (${Build.MODEL}; Android:${Build.VERSION.RELEASE})"): Call<IpView>

    companion object {
        private const val SERVER_API = "https://www.trim21.cn/"
        fun createInstance(): BgmIpViewer {
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(BgmIpViewer::class.java)
        }

        fun getSeason(it: IpView, subject: Subject): List<IpView.Node>{
            val ret = ArrayList<IpView.Node>()

            val bgmIp = it.nodes?.firstOrNull { it.subject_id == subject.id }?:return ret
            val id = it.edges?.firstOrNull{edge-> edge.source == bgmIp.id && edge.relation == "主线故事"}?.target?:bgmIp.id

            it.edges?.filter { edge-> edge.target == id && edge.relation == "主线故事" }?.reversed()?.forEach { edge->
                ret.add(0, it.nodes.firstOrNull{it.id == edge.source}?:return@forEach)
            }
            ret.add(0,it.nodes.firstOrNull { it.id == id }?:return ret)
            var prevId = id
            while(true){
                prevId = it.edges?.firstOrNull { it.source == prevId && it.relation == "前传"}?.target?:break
                it.edges.filter { edge-> edge.target == prevId && edge.relation == "主线故事" }.reversed().forEach {edge->
                    ret.add(0, it.nodes.firstOrNull{it.id == edge.source}?:return@forEach)
                }
                ret.add(0, it.nodes.firstOrNull{it.id == prevId}?:break)
            }
            var nextId = id
            while(true){
                nextId = it.edges?.firstOrNull { it.source == nextId && it.relation == "续集"}?.target?:break
                ret.add(it.nodes.firstOrNull{it.id == nextId}?:break)
                it.edges.filter { edge-> edge.target == nextId && edge.relation == "主线故事" }.forEach {edge->
                    ret.add(it.nodes.firstOrNull{it.id == edge.source}?:return@forEach)
                }
            }
            return ret.distinct()
        }
    }
}