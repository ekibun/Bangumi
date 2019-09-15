package soko.ekibun.bangumi.api.trim21.bean

/**
 * 条目关系网络
 * @property edges 边（关系）
 * @property nodes 点（条目）
 * @constructor
 */
data class IpView(
        val edges: List<Edge>? = null,
        val nodes: List<Node>? = null
) {

    /**
     * 关系网络边数据
     * @property id 边id
     * @property relation 关系类型
     * @property source 源
     * @property target 目标
     * @constructor
     */
    data class Edge(
            val id: String? = null,
            val relation: String? = null,
            val source: Int = 0,
            val target: Int = 0
    )

    /**
     * 关系网络节点数据
     * @property subject_id 条目id
     * @property id 节点id
     * @property image 封面
     * @property name 条目名称
     * @property name_cn 中文名称
     * @constructor
     */
    data class Node(
            val subject_id: Int = 0,
            val id: Int = 0,
            val image: String? = null,
            val name: String? = null,
            val name_cn: String? = null
    )
}