package soko.ekibun.bangumi.api.trim21

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.trim21.bean.IpView
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil
import java.util.*

/**
 * 条目关联api（via @Trim21）
 */
object BgmIpViewer {

    /**
     * 套一层，防止泛型丢失
     */
    data class SeasonData(
        val ipView: IpView,
        val seasons: List<Subject>
    )

    /**
     * 获取季度数据
     * @param id [Subject.id]
     */
    suspend fun getSeason(id: Int): SeasonData {
        return withContext(Dispatchers.Main) {
            val ipView = JsonUtil.toEntity<IpView>(withContext(Dispatchers.IO) {
                HttpUtil.fetch("https://www.trim21.cn/api.v1/view_ip/subject/$id").body?.string() ?: ""
            })!!
            SeasonData(ipView, getSeasonNode(ipView, id).map {
                Subject(
                    it.subject_id,
                    name = it.name, name_cn = it.name_cn,
                    image = it.image
                )
            })
        }
    }

    /**
     * 解析季度数据
     * @param it [IpView]
     * @param subjectId [Subject.id]
     * @return List<IpView.Node>
     */
    private fun getSeasonNode(it: IpView, subjectId: Int): List<IpView.Node> {
        val ret = ArrayList<IpView.Node>()

        val bgmIp = it.nodes?.firstOrNull { it.subject_id == subjectId } ?: return ret
        val id =
            it.edges?.firstOrNull { edge -> edge.source == bgmIp.id && edge.relation == "主线故事" }?.target ?: bgmIp.id

        for (edge in it.edges?.filter { edge -> edge.target == id && edge.relation == "主线故事" }?.reversed()
            ?: ArrayList()) {
            ret.add(0, it.nodes.firstOrNull { it.id == edge.source } ?: continue)
        }
        ret.add(0, it.nodes.firstOrNull { it.id == id } ?: return ret)
        var prevId = id
        while (true) {
            prevId = it.edges?.firstOrNull { it.source == prevId && it.relation == "前传" }?.target ?: break
            for (edge in it.edges.filter { edge -> edge.target == prevId && edge.relation == "主线故事" }.reversed()) {
                ret.add(0, it.nodes.firstOrNull { it.id == edge.source } ?: continue)
            }
            ret.add(0, it.nodes.firstOrNull { it.id == prevId } ?: break)
        }
        var nextId = id
        while (true) {
            nextId = it.edges?.firstOrNull { it.source == nextId && it.relation == "续集" }?.target ?: break
            ret.add(it.nodes.firstOrNull { it.id == nextId } ?: break)
            for (edge in it.edges.filter { edge -> edge.target == nextId && edge.relation == "主线故事" }) {
                ret.add(it.nodes.firstOrNull { it.id == edge.source } ?: continue)
            }
        }
        return ret.distinct()
    }
}
