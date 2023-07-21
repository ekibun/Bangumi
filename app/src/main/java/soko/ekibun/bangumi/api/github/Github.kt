package soko.ekibun.bangumi.api.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.github.bean.BangumiCalendarItem
import soko.ekibun.bangumi.api.github.bean.BangumiLinkMap
import soko.ekibun.bangumi.api.github.bean.OnAirInfo
import soko.ekibun.bangumi.api.github.bean.Release
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil
import java.util.LinkedList

/**
 * GitHub API
 */
object Github {

    private const val GITHUB_SERVER_API = "https://api.github.com"

    /**
     * 获取版本列表
     */
    suspend fun releases(): List<Release> {
        return withContext(Dispatchers.IO) {
            JsonUtil.toEntity<List<Release>>(
                HttpUtil.fetch(
                    "$GITHUB_SERVER_API/repos/ekibun/Bangumi/releases"
                ).body!!.string()
            )!!
        }
    }

    private const val RAW_SERVER_API = "https://ghproxy.com/https://raw.githubusercontent.com"

    /**
     * 时间表
     */
    suspend fun bangumiCalendar(): List<BangumiCalendarItem> {
        return withContext(Dispatchers.IO) {
            JsonUtil.toEntity<List<BangumiCalendarItem>>(
                HttpUtil.fetch(
                    "$RAW_SERVER_API/ekibot/bangumi-link/master/calendar.json"
                ).body!!.string()
            )!!
        }
    }

    /**
     * 播放源
     * @param id Int
     */
    suspend fun onAirInfo(id: Int): OnAirInfo? {
        return withContext(Dispatchers.IO) {
            JsonUtil.toEntity<OnAirInfo>(
                HttpUtil.fetch(
                    "$RAW_SERVER_API/ekibot/bangumi-onair/master/onair/${id / 1000}/$id.json"
                ).body?.string() ?: ""
            )
        }
    }

    /**
     * 播放源
     * @param id Int
     */
    suspend fun getSeason(id: Int): List<Subject>? {
        return withContext(Dispatchers.IO) {
            val mapId = HttpUtil.fetch(
                "$RAW_SERVER_API/ekibot/bangumi-link/master/node/${id / 1000}/$id"
            ).body?.string()?.toIntOrNull() ?: return@withContext null
            JsonUtil.toEntity<BangumiLinkMap>(
                HttpUtil.fetch(
                    "$RAW_SERVER_API/ekibot/bangumi-link/master/map/${mapId / 1000}/$mapId.json"
                ).body?.string() ?: ""
            )?.let {
                getSeasonNode(it, id)
            }
        }
    }

    /**
     * 解析季度数据
     * @param map [BangumiLinkMap]
     * @param subjectId [Subject.id]
     * @return List<IpView.Node>
     */
    private fun getSeasonNode(map: BangumiLinkMap, subjectId: Int): List<Subject> {
        val bgmIp = map.node?.firstOrNull { it.id == subjectId } ?: return emptyList()
        val ret = ArrayList<BangumiLinkMap.BangumiLinkSubject>()
        val visitNode = { nodeId: Int ->
            map.node.firstOrNull { it.id == nodeId }?.takeIf { it.visit != true }
        }
        val id = map.relate?.firstOrNull { edge ->
            edge.src == bgmIp.id && edge.relate == "主线故事"
        }?.dst ?: bgmIp.id

        val queue = LinkedList<Int>()
        queue.add(id)
        while (true) {
            val nodeId = queue.poll() ?: break
            val node = visitNode(nodeId)?.also {
                it.visit = true
            } ?: continue
            ret.add(node)
            for (edge in map.relate?.filter { edge ->
                edge.dst == node.id && edge.relate == "主线故事"
            }?.reversed() ?: emptyList()) {
                ret.add(visitNode(edge.src) ?: continue)
            }
            if (map.relate == null) break
            queue.addAll(map.relate.filter { it.src == nodeId && it.relate == "续集" }.map { it.dst })
            queue.addAll(map.relate.filter { it.src == nodeId && it.relate == "前传" }.map { it.dst })
            queue.addAll(map.relate.filter { it.dst == nodeId && it.relate == "续集" }.map { it.src })
            queue.addAll(map.relate.filter { it.dst == nodeId && it.relate == "前传" }.map { it.src })
        }
        return ret.distinct().map {
            Subject(
                id = it.id,
                name = it.name,
                name_cn = it.nameCN,
                image = Bangumi.parseUrl(it.image ?: ""),
            )
        }.sortedBy { it.id }
    }
}