package soko.ekibun.bangumi.api.trim21

import android.util.Log
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
        return withContext(Dispatchers.Default) {
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
    private fun getSeasonNode(ipView: IpView, subjectId: Int): List<IpView.Node> {
        val ret = ArrayList<IpView.Node>()

        val bgmIp = ipView.nodes?.firstOrNull { it.subject_id == subjectId } ?: return ret
        val visitNode = { nodeId: Int ->
            ipView.nodes.firstOrNull { it.id == nodeId }?.takeIf { it.visit != true }
        }
        val id = ipView.edges?.firstOrNull { edge ->
            edge.source == bgmIp.id && edge.relation == "主线故事"
        }?.target ?: bgmIp.id

        val queue = LinkedList<Int>()
        queue.add(id + 1)
        while (true) {
            val nodeId = queue.poll() ?: break
            val node = visitNode(nodeId)?.also {
                Log.v("node", it.name_cn ?: it.name)
                it.visit = true
            } ?: continue
            ret.add(node)
            for (edge in ipView.edges?.filter { edge ->
                edge.target == node.id && edge.relation == "主线故事"
            }?.reversed() ?: emptyList()) {
                ret.add(visitNode(edge.source) ?: continue)
            }
            if (ipView.edges == null) break
            queue.addAll(ipView.edges.filter { it.source == nodeId && it.relation == "续集" }.map { it.target })
            queue.addAll(ipView.edges.filter { it.source == nodeId && it.relation == "前传" }.map { it.target })
            queue.addAll(ipView.edges.filter { it.target == nodeId && it.relation == "续集" }.map { it.source })
            queue.addAll(ipView.edges.filter { it.target == nodeId && it.relation == "前传" }.map { it.source })
        }
        return ret.distinct().sortedBy { it.subject_id }
    }
}
