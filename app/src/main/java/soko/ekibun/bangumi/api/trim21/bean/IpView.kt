package soko.ekibun.bangumi.api.trim21.bean

data class IpView(
        val edges: List<Edge>? = null,
        val nodes: List<Node>? = null
) {

    data class Edge(
            val id: String? = null,
            val relation: String? = null,
            val source: Int = 0,
            val target: Int = 0
    ) {
        /**
         * _id : 115908-115909
         * relation : 书籍
         * source : 0
         * target : 1
         */
    }

    data class Node(
            val subject_id: Int = 0,
            val cover: String? = null,
            val id: Int = 0,
            val image: String? = null,
            val name: String? = null,
            val name_cn: String? = null
    ) {
        /**
         * NSFW : false
         * _id : 115908
         * cover : lain.bgm.tv/pic/cover/g/37/df/115908_6n60U.jpg
         * id : 0
         * image : http://lain.bgm.tv/pic/cover/g/37/df/115908_6n60U.jpg
         * name : 響け！ユーフォニアム TV
         * name_cn : 吹响！悠风号
         */
    }
}