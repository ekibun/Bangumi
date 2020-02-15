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
import java.util.*

/**
 * 条目关联api（via @Trim21）
 */
interface BgmIpViewer {

    /**
     * 获取条目关联的条目链
     * @param id Int
     * @param ua String
     * @return Call<IpView>
     */
    @GET("/api.v1/view_ip/subject/{id}")
    fun subject(@Path("id") id: Int,
                @Header("user-agent") ua: String = "Bangumi-ekibun/${BuildConfig.VERSION_NAME} (${Build.MODEL}; Android:${Build.VERSION.RELEASE})"): Call<IpView>

    companion object {
        private const val SERVER_API = "https://www.trim21.cn/"
        /**
         * 创建retrofit实例
         * @return BgmIpViewer
         */
        fun createInstance(): BgmIpViewer {
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(BgmIpViewer::class.java)
        }

        /**
         * 获取条目的季度条目链（OVA & 续集）
         * @param it IpView
         * @param subject Subject
         * @return List<Subject>
         */
        fun getSeason(it: IpView, subject: Subject): List<Subject> {
            return getSeasonNode(it, subject).map {
                Subject(
                    it.subject_id,
                    name = it.name, name_cn = it.name_cn,
                    image = it.image
                )
            }
        }

        /**
         * 解析季度数据
         * @param it IpView
         * @param subject Subject
         * @return List<IpView.Node>
         */
        private fun getSeasonNode(it: IpView, subject: Subject): List<IpView.Node> {
            val ret = ArrayList<IpView.Node>()

            val bgmIp = it.nodes?.firstOrNull { it.subject_id == subject.id } ?: return ret
            val id =
                it.edges?.firstOrNull { edge -> edge.source == bgmIp.id && edge.relation == "主线故事" }?.target ?: bgmIp.id

            for (edge in it.edges?.filter { edge -> edge.target == id && edge.relation == "主线故事" }?.reversed()
                ?: ArrayList()) {
                ret.add(0, it.nodes.firstOrNull { it.id == edge.source } ?: continue)
            }
            ret.add(0,it.nodes.firstOrNull { it.id == id }?:return ret)
            var prevId = id
            while(true){
                prevId = it.edges?.firstOrNull { it.source == prevId && it.relation == "前传"}?.target?:break
                for (edge in it.edges.filter { edge -> edge.target == prevId && edge.relation == "主线故事" }.reversed()) {
                    ret.add(0, it.nodes.firstOrNull { it.id == edge.source } ?: continue)
                }
                ret.add(0, it.nodes.firstOrNull{it.id == prevId}?:break)
            }
            var nextId = id
            while(true){
                nextId = it.edges?.firstOrNull { it.source == nextId && it.relation == "续集"}?.target?:break
                ret.add(it.nodes.firstOrNull{it.id == nextId}?:break)
                for (edge in it.edges.filter { edge -> edge.target == nextId && edge.relation == "主线故事" }) {
                    ret.add(it.nodes.firstOrNull { it.id == edge.source } ?: continue)
                }
            }
            return ret.distinct()
        }
    }
}